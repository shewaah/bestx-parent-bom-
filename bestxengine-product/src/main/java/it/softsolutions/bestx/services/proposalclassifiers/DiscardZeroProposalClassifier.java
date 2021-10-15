/*
 * Copyright 1997-2012 SoftSolutions! srl 
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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class DiscardZeroProposalClassifier extends BaseMarketMakerClassifier implements ProposalClassifier {

   private String acceptableStatesList;
   
   
   @Override
   public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
      if (!isCompositePriceMarketMaker(proposal) && proposal.getQty().compareTo(BigDecimal.ZERO) < 0) {
         proposal.setProposalState(Proposal.ProposalState.DROPPED);
         proposal.setReason(Messages.getString("DiscardZeroProposalClassifier.1"));
         proposal.setQty(BigDecimal.ZERO);
      }
      else if (!isCompositePriceMarketMaker(proposal)
            && (proposal.getQty().compareTo(BigDecimal.ZERO) == 0 || (proposal.getMarket().getMarketCode() == MarketCode.BLOOMBERG && proposal.getQty().compareTo(BigDecimal.ONE) <= 0))) {
               
               if (proposal.getMarket() != null && proposal.getMarket().isHistoric() && acceptableStatesList.indexOf(proposal.getAuditQuoteState()) >= 0) {
                  proposal.setProposalState(Proposal.ProposalState.ACCEPTABLE);
               } else {
                  proposal.setProposalState(Proposal.ProposalState.REJECTED);
               }
               proposal.setProposalSubState(ProposalSubState.ZERO_QUANTITY);
               proposal.setReason(Messages.getString("DiscardZeroProposalClassifier.1"));
            }
      else if (proposal.getPrice().getAmount().compareTo(BigDecimal.ZERO) == 0
            || (proposal.getMarket().getMarketCode() == MarketCode.BLOOMBERG && proposal.getPrice().getAmount().compareTo(BigDecimal.ONE) <= 0)) {
        
               if (proposal.getMarket() != null && proposal.getMarket().isHistoric() && acceptableStatesList.indexOf(proposal.getAuditQuoteState()) >= 0) {
                  proposal.setProposalState(Proposal.ProposalState.ACCEPTABLE);
               } else {
                  proposal.setProposalState(Proposal.ProposalState.REJECTED);
               }
               proposal.setProposalSubState(ProposalSubState.PRICE_NOT_VALID);
               proposal.setReason(Messages.getString("DiscardZeroProposalClassifier.0"));
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

   public String getAcceptableStatesList() {
      return acceptableStatesList;
   }
   
   public void setAcceptableStatesList(String acceptableStatesList) {
      this.acceptableStatesList = acceptableStatesList;
   }
}