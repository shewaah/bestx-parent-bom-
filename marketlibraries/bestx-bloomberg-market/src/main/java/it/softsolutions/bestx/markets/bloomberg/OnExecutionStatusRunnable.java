/*
* Copyright 1997-2020 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.markets.bloomberg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutablePriceAskComparator;
import it.softsolutions.bestx.model.ExecutablePriceBidComparator;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.component.TSCompDealersGrpComponent;
import quickfix.DoubleField;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.MessageComponent;
import quickfix.StringField;
import quickfix.field.CompDealerID;
import quickfix.field.CompDealerQuote;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: stefano.pontillo 
* Creation date: 25-ago-2020 
* 
**/
public class OnExecutionStatusRunnable implements Runnable {

   private static final Logger LOGGER = LoggerFactory.getLogger(OnExecutionStatusRunnable.class);
   
   private final Operation operation;
   private final TSExecutionReport tsExecutionReport;
   private final Market executionMarket;
   private final MarketMakerFinder marketMakerFinder;
   
   
   public OnExecutionStatusRunnable(Operation operation, TSExecutionReport tsExecutionReport, Market executionMarket, 
         MarketMakerFinder marketMakerFinder){
      this.operation = operation;
      this.tsExecutionReport = tsExecutionReport;
      this.executionMarket = executionMarket;
      this.marketMakerFinder = marketMakerFinder;
   }


   @Override
   public void run() {
      LOGGER.info("On Execution Status Runnable {}, {}", tsExecutionReport.getClOrdID(), tsExecutionReport.getExecType() );
      Order order = this.operation.getLastAttempt().getMarketOrder();
      Attempt attempt = operation.getLastAttempt();
      if (order == null) {
         operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
         return;
      }
      
      //if a second message with POBEX information come from the market it will be ignored
      if (attempt.getExecutablePrices() != null && attempt.getExecutablePrices().size() > 0) {
         LOGGER.info("OrderID: {} - Discarded Execution report staus message for operation with executable prices already present", operation.getOrder().getFixOrderId());
         return;
      }
      
      if (!(this.operation.getState() instanceof BBG_SendRfqState)) {
         LOGGER.info("OrderID: {} - Discarded Execution report staus message for operation not in correct state", operation.getOrder().getFixOrderId());
         return;
      }
      
      // get competing dealer quotes feedback. Does not contain the executed proposal for a cancel
      List<MessageComponent> customComp = tsExecutionReport.getCustomComponents();
      if (customComp != null) {
         for (MessageComponent comp : customComp) {
            List<ExecutablePrice> prices = new ArrayList<ExecutablePrice>();
            if (comp instanceof TSCompDealersGrpComponent) {
               try {
                  quickfix.field.NoCompDealers compDealerGrp = ((TSCompDealersGrpComponent) comp).get(new quickfix.field.NoCompDealers());
                  List<Group> groups = ((TSCompDealersGrpComponent) comp).getGroups(compDealerGrp.getField());

                  for (int i = 0; i < groups.size(); i++) {
                     ExecutablePrice price = new ExecutablePrice();
                     price.setMarket(this.executionMarket);
                     MarketMarketMaker tempMM = null;
                     if (groups.get(i).isSetField(CompDealerID.FIELD)) {
                        String quotingDealer = groups.get(i).getField(new StringField(CompDealerID.FIELD)).getValue();

                        //tempMM = marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), quotingDealer);
                        tempMM = marketMakerFinder.getMarketMarketMakerByTSOXCode(quotingDealer);
                        if (tempMM == null) {
                           LOGGER.info("IMPORTANT! Bloomberg returned dealer {} not configured in BestX!. Please configure it", quotingDealer);
                           price.setOriginatorID(quotingDealer);
                        }
                        else {
                           price.setOriginatorID(quotingDealer);
                           price.setMarketMarketMaker(tempMM);
                        }
                     }
                     if (groups.get(i).isSetField(CompDealerQuote.FIELD)) {
                        Double compDealerQuote = groups.get(i).getField(new DoubleField(CompDealerQuote.FIELD)).getValue();
                        price.setPrice(new Money(operation.getOrder().getCurrency(), Double.toString(compDealerQuote)));
                     }
                     else price.setPrice(new Money(operation.getOrder().getCurrency(), "0.0"));
                     price.setPriceType(Proposal.PriceType.PRICE);
                     price.setQty(operation.getOrder().getQty());
                     // calculate status
                     price.setTimestamp(tsExecutionReport.getTransactTime());
                     price.setType(ProposalType.COUNTER);
                     price.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
                     price.setQuoteReqId(attempt.getMarketOrder().getFixOrderId());
                     
                     //Calculate status from 22606 tag can contains: Expired, Canceled, Passed, Rejected, or Other
                     if ("PASSED".equalsIgnoreCase(tsExecutionReport.getCustomFieldString(22606).toString())) {
                        price.setAuditQuoteState("Passed");
                     } else if ("EXPIRED".equalsIgnoreCase((tsExecutionReport.getCustomFieldString(22606).toString()))) {
                        price.setAuditQuoteState("Expired");
                     } else if ("Canceled".equalsIgnoreCase((tsExecutionReport.getCustomFieldString(22606).toString()))) {
                        price.setAuditQuoteState("Cancelled");
                     } else if ("Rejected".equalsIgnoreCase((tsExecutionReport.getCustomFieldString(22606).toString()))) {
                        price.setAuditQuoteState("Missed");
                     } else {
                        price.setAuditQuoteState("Expired");
                     }
                     
                     if (tempMM == null) {
                        LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", operation.getOrder().getFixOrderId(), operation.getAttemptNo(),
                              price.getOriginatorID(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
                     }
                     else {
                        LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", operation.getOrder().getFixOrderId(), operation.getAttemptNo(),
                              price.getMarketMarketMaker().getMarketMaker().getName(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
                     }
                     prices.add(price);
                  }
               }
               catch (FieldNotFound | BestXException e) {
                  LOGGER.warn("[MktMsg] Field not found in component dealers", e);
               }
            }
            // sort the executable prices
            Comparator<ExecutablePrice> comparator;
            comparator = operation.getOrder().getSide() == OrderSide.BUY ? new ExecutablePriceAskComparator() : new ExecutablePriceBidComparator();
            Collections.sort(prices, comparator);
            // give them their rank
            for (int i = 0; i < prices.size(); i++)
               prices.get(i).setRank(i + 1);
            // add the sorted, ranked executable prices list to the attempt
            attempt.setExecutablePrices(prices);
         }
      }
      else {
         LOGGER.info("[MktMsg] No custom component found in execution report {}", tsExecutionReport.getClOrdID());
      }
   }
}
