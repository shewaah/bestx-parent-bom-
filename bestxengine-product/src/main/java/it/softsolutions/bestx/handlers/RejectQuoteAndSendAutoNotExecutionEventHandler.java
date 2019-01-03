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
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;

/**  
 *
 * Purpose: 
 *
 * Project Name : bestxengine-product
 * First created by: paolo.midali
 * Creation date: 05/ott/2012 
 * 
 **/
public class RejectQuoteAndSendAutoNotExecutionEventHandler extends BaseOperationEventHandler {
    private static final long serialVersionUID = 2891618599873587829L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RejectQuoteAndSendAutoNotExecutionEventHandler.class);

    private final SerialNumberService serialNumberService;
    private final MarketBuySideConnection mktConnection;

    //FIXME there is no timeout on the rejectQuote response
    /**
     * @param operation
     */
    public RejectQuoteAndSendAutoNotExecutionEventHandler(Operation operation, MarketBuySideConnection bbgConnection, SerialNumberService serialNumberService) {
        super(operation);
        this.mktConnection = bbgConnection;
        this.serialNumberService = serialNumberService;
    }

    @Override
    public void onNewState(OperationState currentState) {
        try {
            String externalBroker = operation.getLastAttempt().getMarketOrder().getMarketMarketMaker().getMarketSpecificCode();
            String quoteId = null;
            String rejectReason = currentState.getComment();
            if (operation.getLastAttempt().getExecutablePrice(0) != null && operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal() != null) {
                quoteId = operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getSenderQuoteId();
            }
            
            LOGGER.info("Order {} : rejecting quote from broker {}, quoteId={} : {}", operation.getOrder().getFixOrderId(), externalBroker, quoteId, rejectReason);
            mktConnection.rejectProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal());
            
            // auto not execution
            try {
                LOGGER.info("Order {} : sending not execution report, quoteId={}", operation.getOrder().getFixOrderId(), quoteId);
                ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.CANCELLED);
                operation.setStateResilient(new SendAutoNotExecutionReportState(rejectReason), ErrorState.class);
            } catch (BestXException e) {
                LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
                String errorMessage = e.getMessage();
                operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
            }
        } catch (BestXException exc) {
            LOGGER.error("Error while rejecting quote from {} : {}", currentState.getMarketCode(), exc.getMessage(), exc);
            operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("RTFIMarketSendQuoteRejectError.0")), ErrorState.class);
        }
    }
}
