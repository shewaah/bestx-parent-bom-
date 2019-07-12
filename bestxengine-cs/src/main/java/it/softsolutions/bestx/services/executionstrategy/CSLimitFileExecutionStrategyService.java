/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.services.executionstrategy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OrderHelper;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;

/**
 * 
 * Purpose: this is the execution strategy for Credit Suisse Limit File Orders
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 23/ott/2013
 * 
 **/
public class CSLimitFileExecutionStrategyService extends CSExecutionStrategyService {

   static final Logger LOGGER = LoggerFactory.getLogger(CSLimitFileExecutionStrategyService.class);

   @Deprecated
   public CSLimitFileExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus){
      super(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest, applicationStatus);
   }

   public CSLimitFileExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus){
      super(operation, priceResult, rejectOrderWhenBloombergIsBest, applicationStatus);
   }

   @Override
   public void manageAutomaticUnexecution(Order order, Customer customer) throws BestXException {
      if (order == null) {
         throw new IllegalArgumentException("order is null");
      }

      if (customer == null) {
         throw new IllegalArgumentException("customer is null");
      }

      if(BondTypesService.isUST(operation.getOrder().getInstrument()) 
            && operation.getLastAttempt().getMarketOrder() != null
            && operation.getLastAttempt().getMarketOrder().getMarket().getMarketCode() == MarketCode.TW) { // have got a rejection on the single attempt on TW
         onUnexecutionResult(Result.USSingleAttemptNotExecuted, Messages.getString("UnexecutionReason.0"));
         return;
      }
      //when there are no markets available, the execution service is called with a null price result, thus
      //we can go in the LimitPriceNoFileState
      if (priceResult == null) {
         onUnexecutionResult(Result.LimitFileNoPrice, Messages.getString("LimitFile.NoPrices"));
      } else {
         //if the sorted book contains at least one proposal in substate NONE, it means that we received a price that
         //will not allow us to consider the book empty. A book is empty, thus sending the order to the Limit File No Price
         //state, only if the proposals are all in the substate PRICE_OR_QTY_NOT_VALID or REJECTED_BY_MARKET, or the book
         //is null
         List<ProposalSubState> wantedSubStates = new ArrayList<ProposalSubState>(1);
         wantedSubStates.add(ProposalSubState.NONE);
         wantedSubStates.add(ProposalSubState.PRICE_WORST_THAN_LIMIT);
         SortedBook sortedBook = priceResult.getSortedBook();
         boolean emptyBook =  sortedBook == null || sortedBook.getProposalBySubState(wantedSubStates, order.getSide()).isEmpty();

         if (emptyBook) {
            this.operation.getLastAttempt().setSortedBook(sortedBook);
            onUnexecutionResult(Result.LimitFileNoPrice, Messages.getString("LimitFile.NoPrices"));
         } else {
            //time to update the delta between the order limit price and the best proposal one

            OrderHelper.setOrderBestPriceDeviationFromLimit(operation);
            OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
            onUnexecutionResult(Result.LimitFile, Messages.getString("LimitFile"));
         }

      }
   }
}