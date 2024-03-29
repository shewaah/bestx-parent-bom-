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
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder.BuilderType;
import it.softsolutions.bestx.handlers.WaitingPriceEventHandler;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;

/**
 * 
 * Purpose: this is the execution strategy for Credit Suisse Limit File Orders
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date:
 * 23/ott/2013
 * 
 **/
public class CSLimitFileExecutionStrategyService extends CSExecutionStrategyService {

	static final Logger LOGGER = LoggerFactory.getLogger(CSLimitFileExecutionStrategyService.class);

	public CSLimitFileExecutionStrategyService(Operation operation, PriceResult priceResult,
			ApplicationStatus applicationStatus, MifidConfig mifidConfig) {
		super(operation, priceResult, applicationStatus, mifidConfig);
	}

	@Override
	public void manageAutomaticUnexecution(Order order, Customer customer, String message) throws BestXException {
		if (order == null) {
			throw new IllegalArgumentException("order is null");
		}

		if (customer == null) {
			throw new IllegalArgumentException("customer is null");
		}

		if (BondTypesService.isUST(operation.getOrder().getInstrument())
				&& operation.getLastAttempt().getMarketOrder() != null
				&& operation.getLastAttempt().getMarketOrder().getMarket().getMarketCode() == MarketCode.TW) {
			// have got a rejection on the single attempt on TW
			onUnexecutionResult(Result.USSingleAttemptNotExecuted, message + Messages.getString("UnexecutionReason.0"));
			return;
		}
		// when there are no markets available, the execution service is called with a
		// null price result, thus
		// we can go in the LimitPriceNoFileState
		if (priceResult == null) {
			if (this.operation.getState().getType() == OperationState.Type.Rejected) {
				// If the operation state is rejected, then it comes from an execution attempt
				// that failed.
				// The customer asked to not retry anymore in such a situation and reject back
				// to OMS the order.
				onUnexecutionResult(Result.Success, message + Messages.getString("WaitingPrices.0"));
			} else {
				onUnexecutionResult(Result.LimitFileNoPrice, message + WaitingPriceEventHandler.defaultStrategyName + Messages.getString("LimitFile.NoPrices"));
			}
		} else {
			// if the sorted book contains at least one proposal in substate NONE, it means
			// that we received a price that
			// will not allow us to consider the book empty. A book is empty, thus sending
			// the order to the Limit File No Price
			// state, only if the proposals are all in the substate PRICE_OR_QTY_NOT_VALID
			// or REJECTED_BY_MARKET, or the book
			// is null
			List<ProposalSubState> wantedSubStates = new ArrayList<ProposalSubState>(1);
			wantedSubStates.add(ProposalSubState.NONE);
         wantedSubStates.add(ProposalSubState.MARKET_TRIED);
			wantedSubStates.add(ProposalSubState.PRICE_WORST_THAN_LIMIT);
			SortedBook sortedBook = priceResult.getSortedBook();
			boolean emptyBook = sortedBook == null
					|| sortedBook.getProposalBySubState(wantedSubStates, order.getSide()).isEmpty();

			if (emptyBook) {
				this.operation.getLastAttempt().setSortedBook(sortedBook);
				onUnexecutionResult(Result.LimitFileNoPrice, message + Messages.getString("LimitFile.NoPrices"));
			} else {
				// time to update the delta between the order limit price and the best proposal
				// one

		      if (operation.getLastAttempt().getMarketOrder() != null && 
			         operation.getLastAttempt().getMarketOrder().getBuilderType() == BuilderType.CUSTOM) {
	            onUnexecutionResult(Result.LimitFile, message + Messages.getString("LimitFile.Rest", 
                     (operation.getLastAttempt().getMarketOrder().getLimit() != null ? operation.getLastAttempt().getMarketOrder().getLimit().getAmount() : "NA"),
	                  (operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice() != null ? operation.getLastAttempt().getMarketOrder().getLimitMonitorPrice().getAmount() : "NA"),
	                  operation.getOrder().getLimit().getAmount(),
                     ExecutionStrategyServiceFactory.getInstance().getCentsLFTolerance()));
			   } else {
               onUnexecutionResult(Result.LimitFile, message + Messages.getString("LimitFile"));
			   }
			}

		}
	}
}