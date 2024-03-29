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
package it.softsolutions.bestx.handlers.tradeweb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: manage TW order cancel
 * 
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 28/gen/2015
 * 
 **/
public class TW_CancelledEventHandler extends BaseOperationEventHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TW_CancelledEventHandler.class);
    private final SerialNumberService serialNumberService;

	public TW_CancelledEventHandler(Operation operation, SerialNumberService serialNumberService) {
		super(operation);	
        this.serialNumberService = serialNumberService;
	}

    @Override
    public void onNewState(OperationState currentState) {
    	if(customerSpecificHandler != null) {
    		customerSpecificHandler.onNewState(currentState);
    	}
		try {
			String rejReason = operation.getExecutionReports().get(operation.getExecutionReports().size()-1).getText();
			ExecutionReportHelper.prepareForAutoNotExecution(operation,serialNumberService, ExecutionReportState.CANCELLED);
			
         operation.getLastAttempt().setAttemptState(AttemptState.REJECTED);

			   // AMC 20190325 if has been cancelled, go to cancelled status
		      if (!checkCustomerRevoke(operation.getOrder())) {
		    	  operation.setStateResilient(new RejectedState(rejReason), ErrorState.class);
		      }
		} catch (BestXException e) {
            LOGGER.error("Order {}, error while starting not execution sending", operation.getOrder().getFixOrderId(), e);
            String errorMessage = e.getMessage();
            operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
		}
    }

}
