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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Attempt.AttemptState;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.RejectedState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;

/**
 * 
 * Purpose: manage TW order rejection
 * 
 * Project Name : bestx-tradeweb-market 
 * First created by: ruggero.rizzo 
 * Creation date: 27/gen/2015
 * 
 **/
public class TW_RejectedEventHandler extends BaseOperationEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TW_RejectedEventHandler.class);
    private final SerialNumberService serialNumberService;
    private BestXConfigurationDao bestXConfigurationDao;

	public TW_RejectedEventHandler(Operation operation, SerialNumberService serialNumberService, BestXConfigurationDao bestXConfigurationDao) {
		super(operation);
        this.serialNumberService = serialNumberService;
        this.bestXConfigurationDao = bestXConfigurationDao;
	}

    @Override
    public void onNewState(OperationState currentState) {

        boolean mustSendAutoNotExecution = false;
        Order order = operation.getOrder();
        Attempt lastAttempt = operation.getLastAttempt();
        String rejectReason = ""; 
        if (lastAttempt != null) {
        	lastAttempt.setAttemptState(AttemptState.REJECTED);
        	List<ClassifiedProposal> currentProposals = lastAttempt.getSortedBook().getValidSideProposals(order.getSide());
        	double[] resultValues = new double[2];
        	try {
        		if (BookHelper.isSpreadOverTheMax(currentProposals, order, resultValues)) {
        			rejectReason = Messages.getString("BestBook.21", resultValues[0], resultValues[1]);

        			mustSendAutoNotExecution = true;
        		}
        	} catch (BestXException e) {
        		LOGGER.error("Order {}, error while verifying quote spread.", operation.getOrder().getFixOrderId(), e);
        		String errorMessage = e.getMessage();
        		operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);

        		return;
        	}

        	if (mustSendAutoNotExecution) {
        		// auto not execution
        		try {
        			LOGGER.info("Order {} : spread between best and second best too wide, sending not execution report", operation.getOrder().getFixOrderId());
        			ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
        			operation.setStateResilient(new SendAutoNotExecutionReportState(rejectReason), ErrorState.class);
        		} catch (BestXException e) {
        			LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
        			String errorMessage = e.getMessage();
        			operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
        		}
        	}
        	else {
        		operation.setStateResilient(new RejectedState(Messages.getString("GoingInRejectedOrTimeoutStateMessage")), ErrorState.class);
        	}
        }
    }

}
