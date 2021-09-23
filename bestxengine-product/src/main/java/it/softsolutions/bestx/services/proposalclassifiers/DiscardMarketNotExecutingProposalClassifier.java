/*
* Copyright 1997-2021 SoftSolutions! srl 
* All Rights Reserved. 
* 
* NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
* The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
* may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
* are protected by trade secret or copyright law. 
* Dissemination of this information or reproduction of this material is strictly forbidden 
* unless prior written permission is obtained from SoftSolutions! srl.
* Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
* which may be part of this software package.
*/
 
package it.softsolutions.bestx.services.proposalclassifiers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**  
*
* Purpose: this class is mainly for discard proposal coming from market tried in previous attempts 
* that rejected or cancel the order without competing quotes in the execution report
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 23 set 2021 
* 
**/
public class DiscardMarketNotExecutingProposalClassifier implements ProposalClassifier {
   private static final Logger LOGGER = LoggerFactory.getLogger(DiscardMarketNotExecutingProposalClassifier.class);

   @Override
   public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
      if (proposal.getVenue() != null && previousAttempts != null && !previousAttempts.isEmpty()) {
         LOGGER.debug("Order {}, check markets not executiong in previous attempts : {}", order.getFixOrderId(), proposal);
         for (Attempt a : previousAttempts) {
            if (a.getMarketOrder() != null && a.getMarketOrder().getMarket().getEffectiveMarket().equals(proposal.getMarket().getEffectiveMarket())) {
               LOGGER.debug("Order {}, proposal {}, already tried market {}, check execution reports", order.getFixOrderId(), proposal.getMarketMarketMaker(), proposal.getMarket());
               List<MarketExecutionReport> execReports = a.getMarketExecutionReports();
               // The earlier attempt has a market execution report rejected or without pobex prices
               if (execReports != null && !execReports.isEmpty()) {
                  for (MarketExecutionReport report : execReports) {
                     if (ExecutionReportState.REJECTED.equals(report.getState()) || 
                           (ExecutionReportState.CANCELLED.equals(report.getState()) && 
                                 (a.getExecutablePrices() == null || a.getExecutablePrices().isEmpty()))) {
                        
                        LOGGER.debug("Proposal {}:{} discarded due to a previous execution rejected or without pobex prices ", proposal.getMarket(), proposal.getMarketMarketMaker());
                        proposal.setProposalState(Proposal.ProposalState.REJECTED);
                        proposal.setProposalSubState(Proposal.ProposalSubState.NOT_TRADING);
                        proposal.setReason(Messages.getString("DiscardMarketNotExecutingProposalClassifier.0"));
                     }
                  }
               }
            }
         }
      }
      return proposal;
   }

   @Override
   public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, OrderSide orderSide, BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
      throw new UnsupportedOperationException();
   }

   @Override
   public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues) {
      throw new UnsupportedOperationException();
   }

}
