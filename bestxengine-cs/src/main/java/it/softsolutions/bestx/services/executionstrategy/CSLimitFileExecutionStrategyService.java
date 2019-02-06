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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.PriceController;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
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
    public CSLimitFileExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
    	super(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest);  
    }
    
    public CSLimitFileExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
    	super(operation, priceResult, rejectOrderWhenBloombergIsBest);  
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
				&& operation.getLastAttempt().getMarketOrder().getLimit() == null 
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
           SortedBook sortedBook = priceResult.getSortedBook();
            boolean emptyBook =  sortedBook == null || sortedBook.getProposalBySubState(wantedSubStates, order.getSide()).isEmpty();
            
            if (emptyBook) {
                onUnexecutionResult(Result.LimitFileNoPrice, Messages.getString("LimitFile.NoPrices"));
            } else {
                //time to update the delta between the order limit price and the best proposal one
                if (order.getLimit() != null) {
                    List<ClassifiedProposal> bookProposals = sortedBook.getSideProposals(order.getSide());
                    BigDecimal limitPrice = order.getLimit().getAmount();
                    LOGGER.debug("Order {} limit price {}, starting calculating delta from best proposal price.", order.getFixOrderId(), limitPrice.doubleValue());
                    order.setBestPriceDeviationFromLimit(PriceController.INSTANCE.getBestProposalDelta(limitPrice.doubleValue() > 0.0 ? limitPrice : BigDecimal.ZERO, bookProposals, customer));
                }
                onUnexecutionResult(Result.LimitFile, Messages.getString("LimitFile"));
            }
            
        }
    }
    
    public void onUnexecutionResult(Result result, String message) {
        switch (result) {
        case USSingleAttemptNotExecuted:
        case CustomerAutoNotExecution:
        case MaxDeviationLimitViolated:
            try {
            	ExecutionReportHelper.prepareForAutoNotExecution(this.operation, SerialNumberServiceProvider.getSerialNumberService(), ExecutionReportState.REJECTED);
            	this.operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
            } catch (BestXException e) {
                LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
                String errorMessage = e.getMessage();
                this.operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
            }
            break;
        case Failure:
            LOGGER.error(message);
            this.operation.setStateResilient(new WarningState(this.operation.getState(), null, message), ErrorState.class);
            break;
        case LimitFileNoPrice:
        	this.operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
            break;
        case LimitFile:
            //Update the BestANdLimitDelta field on the TabHistoryOrdini table
            Order order = operation.getOrder();
            OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
            this.operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
            break;            
        default:
            LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", operation.getOrder().getFixOrderId());
            operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
            break;
        }
    }
}