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
package it.softsolutions.bestx.services.executionstrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.PriceController;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: this class implements the execution strategy for Credit Suisse
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 07/set/2012
 * 
 **/
public class CSNormalExecutionStrategyService extends CSExecutionStrategyService{

    private static final Logger LOGGER = LoggerFactory.getLogger(CSNormalExecutionStrategyService.class);
    /**
     * Instantiate the Credit Suisse execution strategy service.
     * 
     * @param executionStrategyServiceCallback
     *            : callback that manages the result
     * @param priceResult
     *            : the result of the price discovery, can be null especially when there are no markets available.
     * @param rejectOrderWhenBloombergIsBest
     *            : flag to reject or not orders if the best execution is on Bloomberg
     */
    @Deprecated
    public CSNormalExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
    	super(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest);  
    }

    public CSNormalExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
    	super(operation, priceResult, rejectOrderWhenBloombergIsBest);  
    }

    public void onUnexecutionResult(Result result, String message) {
        switch (result) {
        case CustomerAutoNotExecution:
        case MaxDeviationLimitViolated:
            try {
            	ExecutionReportHelper.prepareForAutoNotExecution(this.operation, SerialNumberServiceProvider.getSerialNumberService(), ExecutionReportState.REJECTED);

                // [RR20120910] The MaxDeviationLimitViolated case in the old implementation
                // required the following lines :
                //
                // Attempt currentAttempt = operation.getLastAttempt();
                // currentAttempt.setSortedBook(priceResult.getSortedBook());
                //
                // this operation has already been performed in the onPricesResult method
                // in a piece of code shared by all the callers, we can avoid to reput it
                // here.

            	this.operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
            } catch (BestXException e) {
                LOGGER.error("Order {}, error while starting automatic not execution.", this.operation.getOrder().getFixOrderId(), e);
                String errorMessage = e.getMessage();
                this.operation.setStateResilient(new WarningState(this.operation.getState(), null, errorMessage), ErrorState.class);
            }
            break;
        case Failure:
            LOGGER.error("Order {} : ", operation.getOrder().getFixOrderId(), message);
            operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
            break;
        case LimitFileNoPrice:
            operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
            break;
        case LimitFile:
            //Update the BestANdLimitDelta field on the TabHistoryOrdini table
            Order order = this.operation.getOrder();
            OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
            this.operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
            break;
        default:
            LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", this.operation.getOrder().getFixOrderId());
            this.operation.setStateResilient(new WarningState(this.operation.getState(), null, message), ErrorState.class);
            break;
        }
    }

    /** 
     * Validates the options available when the order cannot be further executed.
     * When a case exists which tells the order to be migrated on a specific state, calls onUnexecutionResult of the executionStrategyServiceCallback
     * When there is no such case calls onSuccess of the executionStrategyServiceCallback
     */
    @Deprecated
    public void manageAutomaticUnexecution(Order order, Customer customer) throws BestXException {
        if (order == null) {
            throw new IllegalArgumentException("order is null");
        }

        if (customer == null) {
            throw new IllegalArgumentException("customer is null");
        }

        if (customer.isAutoUnexecEnabled()) {
            LOGGER.info("Order {}, the customer requested the automatic not execution.");
//            executionStrategyServiceCallback.onUnexecutionResult(ExecutionStrategyService.Result.CustomerAutoNotExecution, Messages.getString("WaitingPrices.0"));
            return;
        }

        CustomerAttributes customerAttr = (CustomerAttributes) customer.getCustomerAttributes();

        if (customerAttr == null) {
            LOGGER.error("Error while loading customer attributes, none found!");
//            executionStrategyServiceCallback.onUnexecutionResult(ExecutionStrategyService.Result.Failure, Messages.getString("WaitingPrices.1", customer.getName()));
            return;
        }
        
        if (priceResult != null) {
            if (PriceController.INSTANCE.isMaxDeviationEnabled(customer) && PriceController.INSTANCE.isDeviationFromLimitOverTheMax(priceResult.getSortedBook(), order)) {
                LOGGER.info("Order {}, maximum deviation limit not respected, going into automatic not execution.", order.getFixOrderId());
//                executionStrategyServiceCallback.onUnexecutionResult(ExecutionStrategyService.Result.MaxDeviationLimitViolated,
//                                Messages.getString("LimitPriceTooFarFromBest.0", customer.getName(), customerAttr.getLimitPriceOffMarket()));
                return;
            }
        } else {
            LOGGER.warn("Order {}, priceResult is null, cannot perform check on the maximum deviation between best price and limit price", order.getFixOrderId());
        }

        LOGGER.info("Order {}, normal order flow.", order.getFixOrderId());
        // Success, null parameter because there is no need to choose a market to execute on
//        executionStrategyServiceCallback.onUnexecutionResult(ExecutionStrategyService.Result.CustomerAutoNotExecution, Messages.getString("WaitingPrices.0"));
    }
}
