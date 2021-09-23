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
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.PriceController;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;

/**
 * 
 * Purpose: this class implements the execution strategy for Credit Suisse
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 07/set/2012
 * 
 **/
public class CSNormalExecutionStrategyService extends CSExecutionStrategyService{

    private static final Logger LOGGER = LoggerFactory.getLogger(CSNormalExecutionStrategyService.class);

    public CSNormalExecutionStrategyService(Operation operation, PriceResult priceResult, ApplicationStatus applicationStatus, MifidConfig mifidConfig) {
    	super(operation, priceResult, applicationStatus, mifidConfig);  
    }

    /** 
     * Validates the options available when the order cannot be further executed.
     */
    @Override
    public void manageAutomaticUnexecution(Order order, Customer customer, String message) throws BestXException {
        if (order == null) {
            throw new IllegalArgumentException("order is null");
        }

        if (customer == null) {
            throw new IllegalArgumentException("customer is null");
        }

        if (!operation.isNotAutoExecute() && customer.isAutoUnexecEnabled()) {
            LOGGER.info("Order {}, the customer requested the automatic not execution.");
            onUnexecutionResult(ExecutionStrategyService.Result.CustomerAutoNotExecution,  message + Messages.getString("WaitingPrices.0"));
            return;
        }

        if(BondTypesService.isUST(operation.getOrder().getInstrument()) 
        		&& operation.getLastAttempt().getMarketOrder() != null
				&& operation.getLastAttempt().getMarketOrder().getMarket().getMarketCode() == MarketCode.TW) { // have got a rejection on the single attempt on TW
        	onUnexecutionResult(Result.USSingleAttemptNotExecuted, message + Messages.getString("UnexecutionReason.0"));
        	return;
        }
        CustomerAttributes customerAttr = (CustomerAttributes) customer.getCustomerAttributes();

        if (customerAttr == null) {
            LOGGER.error("Error while loading customer attributes, none found!");
            onUnexecutionResult(ExecutionStrategyService.Result.Failure, message + Messages.getString("WaitingPrices.1", customer.getName()));
            return;
        }
        
        if (priceResult != null) {
            if (PriceController.INSTANCE.isMaxDeviationEnabled(customer) && PriceController.INSTANCE.isDeviationFromLimitOverTheMax(priceResult.getSortedBook(), order)) {
                LOGGER.info("Order {}, maximum deviation limit not respected, going into automatic not execution.", order.getFixOrderId());
                onUnexecutionResult(ExecutionStrategyService.Result.MaxDeviationLimitViolated,
                		 message + Messages.getString("LimitPriceTooFarFromBest.0", customer.getName(), customerAttr.getLimitPriceOffMarket()));
                return;
            }
        } else {
            LOGGER.warn("Order {}, priceResult is null, cannot perform check on the maximum deviation between best price and limit price", order.getFixOrderId());
        }

        LOGGER.info("Order {}, normal order flow.", order.getFixOrderId());
        // Success, null parameter because there is no need to choose a market to execute on
    	onUnexecutionResult(ExecutionStrategyService.Result.Success,  message + Messages.getString("WaitingPrices.0"));
    }
}
