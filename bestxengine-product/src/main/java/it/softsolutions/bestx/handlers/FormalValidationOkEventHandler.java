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
package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Order.TimeInForce;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.bestx.states.BusinessValidationState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: formal validation success state event handler
 * 
 * Project Name : bestxengine-product First created by: Ruggero Rizzo Creation date: 06-feb-2012
 * 
 **/
public class FormalValidationOkEventHandler extends BaseOperationEventHandler {
    private static final long serialVersionUID = 1566625005552569161L;
    private static final Logger LOGGER = LoggerFactory.getLogger(FormalValidationOkEventHandler.class);
    
    public FormalValidationOkEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onNewState(OperationState currentState) {
		//Inviare l'OrderResp a TAS
		try {
	        //[RR20131021] Depending on the order TimeInForce value we set the price discovery type
		    // The formal validator allows through only Limit File orders with valid price and order type, we must only check
		    // the timeInForce
		    Order order = operation.getOrder();
	        if (order.getTimeInForce() == TimeInForce.GOOD_TILL_CANCEL) {
	            order.setPriceDiscoveryType(PriceDiscoveryType.LIMIT_FILE_PRICEDISCOVERY);
	        } else {
	            order.setPriceDiscoveryType(PriceDiscoveryType.NORMAL_PRICEDISCOVERY);
	        }
	        LOGGER.info("Order {}, price discovery type {}", order.getFixOrderId(), order.getPriceDiscoveryType());
			customerConnection.sendOrderResponseAck(operation, 
					operation.getOrder(), 
					operation.getIdentifier(OperationIdType.ORDER_ID));
			operation.setStateResilient(new BusinessValidationState(), ErrorState.class);
		} catch (BestXException e) {
			LOGGER.warn("{}", e.getMessage(), e);
			operation.setStateResilient(new WarningState(currentState, null, Messages.getString("EventFixNoConfirm.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
		}
	}
}
