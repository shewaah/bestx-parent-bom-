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
 
package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.Money;


/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: stefano.pontillo 
* Creation date: 27 lug 2021 
* 
**/
public class BestXMarketOrderBuilder implements MarketOrderBuilder {

   private static final Logger LOGGER = LoggerFactory.getLogger(BestXMarketOrderBuilder.class);

   private int targetPriceMaxLevel;
   private String acceptableSubstates;
   
   @Override
   public void buildMarketOrder(Operation operation) {
      // Build MarketOrder
      MarketOrder marketOrder = new MarketOrder();
      Attempt currentAttempt = operation.getLastAttempt();

      Money limitPrice = calculateTargetPrice(operation);
      if (currentAttempt.getExecutionProposal() != null) {
         marketOrder.setValues(operation.getOrder());
         marketOrder.setTransactTime(DateService.newUTCDate());
         marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());

         marketOrder.setMarketMarketMaker(currentAttempt.getExecutionProposal().getMarketMarketMaker());
         marketOrder.setLimit(limitPrice);
         LOGGER.info("Order={}, Selecting for execution market market maker: {} and price {}", operation.getOrder().getFixOrderId(), marketOrder.getMarketMarketMaker(), limitPrice == null? "null":limitPrice.getAmount().toString());
         marketOrder.setVenue(currentAttempt.getExecutionProposal().getVenue());
      }
      operation.onMarketOrderBuilt(this, marketOrder);
   }
   
   
   /**
    * @param order client order 
    * @param currentAttempt for which the target price needs to be calculated. Contains the sorted book, the execution proposal, the market order.
    * @return
    */
   private Money calculateTargetPrice(Operation operation) {
      Money limitPrice = null;
      Money ithBest = null;
      ClassifiedProposal ithBestProp = null;
      Money best = null;
      Attempt currentAttempt = operation.getLastAttempt();
      List<ProposalSubState> wantedSubStates = loadConfiguredSubstates();
      
      try {
         best = currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()).getPrice();
         ithBestProp = BookHelper.getIthProposal(currentAttempt.getSortedBook().getProposalBySubState(wantedSubStates, operation.getOrder().getSide()), this.targetPriceMaxLevel);
         ithBest = ithBestProp.getPrice();
      } catch(NullPointerException e) {
         LOGGER.warn("NullPointerException trying to manage widen best or get the {}-th best for order {}", this.targetPriceMaxLevel, operation.getOrder().getFixOrderId());
         LOGGER.warn("NullPointerException trace", e);
      }
      try {
         double spread = BookHelper.getQuoteSpread(currentAttempt.getSortedBook().getProposalBySubState(wantedSubStates, operation.getOrder().getSide()), this.targetPriceMaxLevel);
         CustomerAttributes custAttr = (CustomerAttributes) operation.getOrder().getCustomer().getCustomerAttributes();
         BigDecimal customerMaxWideSpread = custAttr.getWideQuoteSpread();
         if(customerMaxWideSpread != null && customerMaxWideSpread.doubleValue() < spread) { // must use the spread, not the i-th best
            limitPrice = BookHelper.widen(best, customerMaxWideSpread, operation.getOrder().getSide(), operation.getOrder().getLimit() == null ? null : operation.getOrder().getLimit().getAmount());
            LOGGER.info("Order {}: widening market order limit price {}. Max wide spread is {} and spread between best {} and i-th best {} has been calculated as {}",
                  operation.getOrder().getFixOrderId(),
                  limitPrice == null?" N/A":limitPrice.getAmount().toString(),
                        customerMaxWideSpread == null?" N/A":customerMaxWideSpread.toString(),
                              best == null?" N/A":best.getAmount().toString(),
                                    ithBest == null?" N/A":ithBest.getAmount().toString(),
                                          spread
                  );
         } else {// use i-th best
            limitPrice = ithBest;
         }
         if(limitPrice == null) { // necessary to avoid null limit price. See the book depth minimum for execution 
            if (currentAttempt.getExecutionProposal().getWorstPriceUsed() != null) {
               limitPrice = currentAttempt.getExecutionProposal().getWorstPriceUsed();
               LOGGER.debug("Use worst price of consolidated proposal as market order limit price: {}", limitPrice == null? "null":limitPrice.getAmount().toString());
            } else {
               limitPrice = currentAttempt.getExecutionProposal() == null ? null : currentAttempt.getExecutionProposal().getPrice();
               LOGGER.debug("No i-th best - Use proposal as market order limit price: {}", limitPrice == null? "null":limitPrice.getAmount().toString());
            }                 
         } else {
            if(operation.getOrder().getLimit() != null && isWorseThan(limitPrice, operation.getOrder().getLimit(), operation.getOrder().getSide())) {
               LOGGER.debug("Found price is {}, which is worse than client order limit price: {}. Will use client order limit price", limitPrice.getAmount().toString(), operation.getOrder().getLimit().getAmount().toString());               
               limitPrice = operation.getOrder().getLimit();
            } else
               LOGGER.debug("Use less wide between i-th best proposal and best widened by {} as market order limit price: {}", customerMaxWideSpread, limitPrice == null? "null":limitPrice.getAmount().toString());
         }
      } catch (Exception e) {
         LOGGER.warn("Problem appeared while calculating target price", e);
      }
      return limitPrice;
   }
   
   private boolean isWorseThan(Money p1, Money p2, OrderSide side) {
      if(side == null) return false;
      if(side == OrderSide.BUY) return p1.compareTo(p2) > 0;
      else return p1.compareTo(p2) < 0;
   }
   
   private List<ProposalSubState> loadConfiguredSubstates() {
      String[] substateList = this.acceptableSubstates.split(",");
      List<ProposalSubState> wantedSubStates = new ArrayList<>();
      
      for (String substate : substateList) {
         wantedSubStates.add(ProposalSubState.valueOf(substate));
      }
      return wantedSubStates;
   }
   
   public int getTargetPriceMaxLevel() {
      return targetPriceMaxLevel;
   }

   public void setTargetPriceMaxLevel(int targetPriceMaxLevel) {
      this.targetPriceMaxLevel = targetPriceMaxLevel;
   }
   
   public String getAcceptableSubstates() {
      return acceptableSubstates;
   }
   
   public void setAcceptableSubstates(String acceptableSubstates) {
      this.acceptableSubstates = acceptableSubstates;
   }
}
