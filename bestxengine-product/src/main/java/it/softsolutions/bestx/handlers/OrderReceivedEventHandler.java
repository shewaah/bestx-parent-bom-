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

package it.softsolutions.bestx.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.OrderValidationService;
import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.customservice.CustomServiceException;
import it.softsolutions.bestx.services.ordervalidation.OrderResult;
import it.softsolutions.bestx.services.ordervalidation.OrderResultBean;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.FormalValidationKOState;
import it.softsolutions.bestx.states.FormalValidationOKState;
import it.softsolutions.bestx.states.OrderNotExecutedState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class OrderReceivedEventHandler extends BaseOperationEventHandler {
	
    private static final long serialVersionUID = -3159103227533355474L;
    
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderReceivedEventHandler.class);
    private final OrderValidationService orderValidationService;
    private long customServiceLoadResponseTimeout;
    private CustomService customService;
    private InstrumentFinder instrumentFinder;
    
    //[RR20150324] the two flags and the customer connection help to manage a revoke received while
    //waiting for the custom service response
//    private boolean customServiceRequestPending = false;
//    private boolean revokeReceived = false;
//    private CustomerConnection revokeSourceConnection;
    //////////////////////////////////////////////////
    
    /**
     * Instantiates a new order received event handler.
     * 
     * @param operation
     *            the operation
     * @param orderValidationService
     *            the order validation service
     */
    public OrderReceivedEventHandler(Operation operation, OrderValidationService orderValidationService) {
        super(operation);
        this.orderValidationService = orderValidationService;
    }

    public OrderReceivedEventHandler(Operation operation, long customServiceLoadResponseTimeout, CustomService customService, InstrumentFinder instrumentFinder,
                    OrderValidationService orderValidationService) {
        super(operation);
        this.customServiceLoadResponseTimeout = customServiceLoadResponseTimeout;
        this.customService = customService;
        this.instrumentFinder = instrumentFinder;
        this.orderValidationService = orderValidationService;
    }

    @Override
    public void onNewState(OperationState currentState) {
        Order order = operation.getOrder();
        if (order.getInstrument() == null || !order.getInstrument().isInInventory()) {
            
            if (customService == null || !customService.isAvailable() || !customService.isActive()) {
                try {
                    customerConnection.sendOrderResponseNack(operation, 
                            operation.getOrder(), 
                            operation.getIdentifier(OperationIdType.ORDER_ID), 
                            "Order not accepted - " + Messages.getString("CustomService.NotAvailable"));
                    operation.setStateResilient(new OrderNotExecutedState(Messages.getString("CustomServiceNotAvailable")), ErrorState.class);
                } catch (BestXException e) {
                	LOGGER.warn("{}", e.getMessage(), e);
                    operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventFixNoConfirm.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
                    return;
                }  
            } else {
                String isin = order.getInstrumentCode();
                LOGGER.info("Order {}, the instrument {} is not available in the database, send a load request to Custom Instrument Loader Service.", order.getFixOrderId(), isin);
                try {
//                	customServiceRequestPending = true;
                    customService.sendRequest(order.getFixOrderId(), false, isin);
                    setupDefaultTimer(customServiceLoadResponseTimeout, false);
                } catch (CustomServiceException e) {
                    LOGGER.error("Custom Instrument Loader Service error while starting connection and sending load request to Custom Instrument Loader Service {}", e.getMessage(), e);
                    operation.setStateResilient(new OrderNotExecutedState(Messages.getString("CustomService.NotAvailable")), ErrorState.class);
                }
            }
        } else {
            OrderResult orderResult = orderValidationService.validateOrderFormally(operation, operation.getOrder());

            if (orderResult.isValid()) {
                operation.setStateResilient(new FormalValidationOKState(), ErrorState.class);
            } else {
                operation.setStateResilient(new FormalValidationKOState(Messages.getString("FormalValidationKO") + orderResult.getReason()), ErrorState.class);
            }
        }
    }

    @Override
    public void onFixRevoke(CustomerConnection source) {
    	//BXMNT-428 AMC 20160309
        stopDefaultTimer();  // while change the status to revoked, the timer is no more useful.
        LOGGER.info("Order {}, revoke received", operation.getOrder().getFixOrderId());
//        if (customServiceRequestPending) {
//    		LOGGER.info("Order {}, revoke received, but we're still waiting for a Custom Instrument Loader Service reply, wait to manage it", operation.getOrder().getFixOrderId());
//        	revokeReceived = true;
//        	revokeSourceConnection = source;
//        } else {
        super.onFixRevoke(source);
//        }
    }
    
    
    @Override
    public void onCustomServiceResponse(boolean error, String securityId) {
        LOGGER.info("Order {} , Security ID {}: Custom Service reply received - Error: {}.", operation.getOrder().getFixOrderId(), securityId, error);
//        customServiceRequestPending = false;
        stopDefaultTimer();
        if (!error) {
        	//update the order instrument with the info uploaded by Custom Service
        	Instrument instrument = instrumentFinder.getInstrumentByIsin(securityId);
        	operation.getOrder().setInstrument(instrument);
        	//BXMNT-428 AMC 20160309
//        	if (revokeReceived) {
//        		LOGGER.info("Order {}, there is a pending customer revoke, now we can manage it", operation.getOrder().getFixOrderId());
//        		revokeReceived = false;
//        		super.onFixRevoke(revokeSourceConnection);
//        	} else {
            OrderResult orderResult;
            // BESTX-296 AMC 20170505 when GRDLite answers OK but the instrument has not been created in the DB BestX! must not go in exception
			try {
				orderResult = orderValidationService.validateOrderFormally(operation, operation.getOrder());
			} catch (Exception e) {
				OrderResultBean orderResultB = new OrderResultBean();
				orderResultB.setValid(false);
				orderResultB.setOrder(operation.getOrder());
				orderResultB.setReason("Exception while validating formally: " + e.getMessage());
				orderResult = orderResultB;
			}
            if (orderResult.isValid()) {
                operation.setStateResilient(new FormalValidationOKState(), ErrorState.class);
            } else {
                operation.setStateResilient(new FormalValidationKOState(Messages.getString("FormalValidationKO") + orderResult.getReason()), ErrorState.class);
//	            }
        	}
        } else { //Custom Service replied with an error
            try {
                customerConnection.sendOrderResponseNack(operation, 
                        operation.getOrder(), 
                        operation.getIdentifier(OperationIdType.ORDER_ID), 
                        Messages.getString("CustomServiceErrorReceived"));
                operation.setStateResilient(new OrderNotExecutedState(Messages.getString("RequestInstrument.Error.LoadResponseError")), ErrorState.class);
            } catch (BestXException e) {
            	LOGGER.warn("{}", e.getMessage(), e);
                operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventFixNoConfirm.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
            }
        }
    }
    
    @Override
    public void onTimerExpired(String jobName, String groupName) {
    	String handlerJobName = super.getDefaultTimerJobName();
        if (jobName.equals(handlerJobName)) {
            LOGGER.debug("Order {} : Timer: {} expired.", operation.getOrder().getFixOrderId(), jobName);

            String securityID = operation.getOrder().getInstrumentCode();
            LOGGER.error("Instrument Loader Custom Service did not answer to our load request for the instrument {}, orderID = {}", securityID, operation.getOrder().getFixOrderId());

            // Clean up the callbacksMap
            customService.resetRequest(securityID, operation.getOrder().getFixOrderId());

            // [DR20130326] CRSBXIGR-70 - No request sent to GRDLite by BestX!
            try {
                customerConnection.sendOrderResponseNack(operation, 
                        operation.getOrder(), 
                        operation.getIdentifier(OperationIdType.ORDER_ID), 
                        Messages.getString("CustomService.Timeout"));
                operation.setStateResilient(new OrderNotExecutedState(Messages.getString("RequestInstrument.Error.TimedOut")), ErrorState.class);
            } catch (BestXException e) {
            	LOGGER.warn("{}", e.getMessage(), e);
                operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventFixNoConfirm.0", operation.getIdentifier(OperationIdType.CUSTOMER_CHANNEL))), ErrorState.class);
            }
        } else {
            super.onTimerExpired(jobName, groupName);
        }
    }
}
