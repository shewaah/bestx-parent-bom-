/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.handlers.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.handlers.CurandoEventHandler;
import it.softsolutions.bestx.services.serial.SerialNumberService;

public class INT_InternalInCurandoEventHandler extends CurandoEventHandler {
    private static final long serialVersionUID = 1011111470326200697L;
    private static final Logger LOGGER = LoggerFactory.getLogger(INT_InternalInCurandoEventHandler.class);

    public INT_InternalInCurandoEventHandler(Operation operation, SerialNumberService serialNumberService, int curandoRetrySec) {
        super(operation, serialNumberService, curandoRetrySec);
    }

    /**
     * We must override the onNewState because in the CurandoEventHandler we redirect the operation in the state that leads to this handler.
     */
    @Override
    public void onNewState(it.softsolutions.bestx.OperationState currentState) {
        // nothing to do
    };

    /**
     * Testing it happened that the timer set up on internal execution start, expired when the order was already in internalized manual
     * curando. We have only to reset it, nothing else must be done.
     */
    @Override
    public void onTimerExpired(String jobName, String groupName) {
		String handlerJobName = super.getDefaultTimerJobName();
        if (jobName.equals(handlerJobName)) {
            LOGGER.debug("Timer: " + jobName + " expired.");
            LOGGER.info("The order " + operation.getOrder().getFixOrderId() + ", is an internalized order in manual curando, nothing to do.");
        }
        else {
            super.onTimerExpired(jobName, groupName);
        }
    }

    /**
     * Testing it happened that we received an ack from the internal market while the order was already in internalized manual curando. It
     * is only a notification and we must do nothing.
     */
    @Override
    public void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
        LOGGER.info("Trade Ack received for the order " + operation.getOrder().getFixOrderId() + ", it is an internalized order in manual curando, nothing to do.");
    }
}
