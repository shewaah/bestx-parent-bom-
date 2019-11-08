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
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.OrderHelper;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;

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
   public CSLimitFileExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus, int minimumRequiredBookDepth){
      super(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest, applicationStatus, minimumRequiredBookDepth);
   }

   public CSLimitFileExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus, int minimumRequiredBookDepth){
      super(operation, priceResult, rejectOrderWhenBloombergIsBest, applicationStatus, minimumRequiredBookDepth);
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
         if (this.operation.getState().getType() == OperationState.Type.Rejected) {
            // If the operation state is rejected, then it comes from an execution attempt that failed.
            // The customer asked to not retry anymore in such a situation and reject back to OMS the order.
            onUnexecutionResult(Result.Success, Messages.getString("WaitingPrices.0"));
         }
         else {
            onUnexecutionResult(Result.LimitFileNoPrice, Messages.getString("LimitFile.NoPrices"));
         }
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
   
   
   
   
	@Override
	// [BESTX-570] Method implemented to intercept LF UST orders
	public void startExecution(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService) {
		if (BondTypesService.isUST(operation.getOrder().getInstrument())) {
			
			 LOGGER.debug("Executing LF start execution strategy for UST bonds for order {}", operation.getOrder().getFixOrderId());
			 Order order = operation.getOrder();
			 SortedBook sortedBook = currentAttempt.getSortedBook();
	         
	         //List<ProposalSubState> wantedSubStates = Arrays.asList(ProposalSubState.NONE /*, ProposalSubState.PRICE_WORST_THAN_LIMIT*/);
	         boolean emptyOriginalBook = sortedBook == null || sortedBook.getSideProposals(order.getSide()).isEmpty();
	         boolean onePriceWorse = sortedBook != null && !sortedBook.getProposalBySubState(Arrays.asList(ProposalSubState.PRICE_WORST_THAN_LIMIT), order.getSide()).isEmpty();
	         LOGGER.info("OrderId: {}. Original book is empty: {}. There is at least one price discarded because of being worse than limit: {}", operation.getOrder().getFixOrderId(), emptyOriginalBook, onePriceWorse);
	         
	         if (priceResult.getState() == PriceResult.PriceResultState.COMPLETE) {
	            //this.operation.getLastAttempt().setSortedBook(sortedBook);
	        	 LOGGER.debug("OrderId: {}. Going to execution because there is a not empty consolidated book", operation.getOrder().getFixOrderId());
	             super.startExecution(operation, currentAttempt, serialNumberService);
	         } else if (emptyOriginalBook && this.applicationStatus.getType() == ApplicationStatus.Type.EXECUTION) {
	        	 LOGGER.debug("OrderId: {}. Going to execution because there is a completely empty original book", operation.getOrder().getFixOrderId());
	        	 super.startExecution(operation, currentAttempt, serialNumberService);
	         } else if (!onePriceWorse && this.applicationStatus.getType() == ApplicationStatus.Type.EXECUTION) {
	        	 LOGGER.debug("OrderId: {}. Going to execution because there are no prices discarded for being worse than limit", operation.getOrder().getFixOrderId());
	        	 super.startExecution(operation, currentAttempt, serialNumberService);
	         } else {
	        	try {
	        		LOGGER.debug("OrderId: {}. Managing unexecution", operation.getOrder().getFixOrderId());
	        		this.manageAutomaticUnexecution(order, order.getCustomer());
	        	} catch (BestXException e) {
					LOGGER.error("Order {}, error while managing {} price result state {}", order.getFixOrderId(), priceResult.getState().name(), e.getMessage(), e);
					operation.removeLastAttempt();
					operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
				}
	         }
		} else {
			super.startExecution(operation, currentAttempt, serialNumberService);
		}
	}

   
}