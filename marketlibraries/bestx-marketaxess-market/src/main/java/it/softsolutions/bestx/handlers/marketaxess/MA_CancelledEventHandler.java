package it.softsolutions.bestx.handlers.marketaxess;
/*
 * Copyright 1997-2015 SoftSolutions! srl 
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectedState;

/**
 * 
 * Purpose: manage TW order cancel
 * 
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 28/gen/2015
 * 
 **/
public class MA_CancelledEventHandler extends BaseOperationEventHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MA_CancelledEventHandler.class);
    private final SerialNumberService serialNumberService;

	public MA_CancelledEventHandler(Operation operation, SerialNumberService serialNumberService) {
		super(operation);	
        this.serialNumberService = serialNumberService;
	}

    @Override
    public void onNewState(OperationState currentState) {
//		try {
			
	      if (!checkCustomerRevoke(operation.getOrder())) {
	         int size = operation.getLastAttempt().getMarketExecutionReports().size();
	         String rejReason = "";
	         if(size > 0) {
	            rejReason = operation.getLastAttempt().getMarketExecutionReports().get(size -1).getText();
	         }
           	operation.getLastAttempt().setAttemptState(AttemptState.EXPIRED);
           	operation.setStateResilient(new RejectedState(rejReason), ErrorState.class);
	      }
//			ExecutionReportHelper.prepareForAutoNotExecution(operation,serialNumberService, ExecutionReportState.CANCELLED);
//			operation.setStateResilient(new SendNotExecutionReportState(rejReason == null? currentState.getComment() : rejReason), ErrorState.class);
//		} catch (BestXException e) {
//            LOGGER.error("Order {}, error while starting not execution sending", operation.getOrder().getFixOrderId(), e);
//            String errorMessage = e.getMessage();
//            operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
		}
}
