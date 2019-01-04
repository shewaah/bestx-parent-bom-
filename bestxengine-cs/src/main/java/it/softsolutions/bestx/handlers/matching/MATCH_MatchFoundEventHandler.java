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
package it.softsolutions.bestx.handlers.matching;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.TitoliIncrociabiliService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.matching.MATCH_ExecutedState;
import it.softsolutions.bestx.states.matching.MATCH_StartExecutionState;
import it.softsolutions.jsscommon.Money;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class MATCH_MatchFoundEventHandler extends CSBaseOperationEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MATCH_MatchFoundEventHandler.class);

    private final SerialNumberService serialNumberService;
    private final MarketBuySideConnection matchingConnection;
    private final String matchingMMcode;
    private final TitoliIncrociabiliService titoliIncrociabiliService;

    /**
     * @param operation
     */
    public MATCH_MatchFoundEventHandler(Operation operation, MarketBuySideConnection matchingConnection, SerialNumberService serialNumberService, TitoliIncrociabiliService titoliIncrociabiliService,
            String matchingMMcode) {
        super(operation);
        this.matchingConnection = matchingConnection;
        this.serialNumberService = serialNumberService;
        this.matchingMMcode = matchingMMcode;
        this.titoliIncrociabiliService = titoliIncrociabiliService;
    }

    @Override
    public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
        try {
            matchingConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), comment);
            titoliIncrociabiliService.resetMatchingOperation(operation);
            operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
        } catch (BestXException exc) {
            operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
        }
    }

    @Override
    public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {
        if (source.getMarketCode() != MarketCode.MATCHING) {
            return;
        }

        operation.setMatchingOperation(null);
        operation.setStateResilient(new MATCH_StartExecutionState(), ErrorState.class);
    }

    @Override
    public void onOperatorMatchOrders(OperatorConsoleConnection source, Money ownPrice, Money matchingPrice, String comment) {
        try {
            matchingConnection.matchOperations(operation, operation.getMatchingOperation(), ownPrice, matchingPrice);
        } catch (BestXException exc) {
            operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
        }
    }

    @Override
    public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
        Attempt currentAttempt = operation.getLastAttempt();
        if (currentAttempt == null) {
            LOGGER.error("No current Attempt found");
            operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("MATCHMarketNoCurrentAttempFound.0")), ErrorState.class);
            return;
        }
        List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
        if (marketExecutionReports == null) {
            marketExecutionReports = new ArrayList<MarketExecutionReport>();
            currentAttempt.setMarketExecutionReports(marketExecutionReports);
        }
        marketExecutionReport.setTipoConto(ExecutionReport.CONTO_PROPRIO);
        marketExecutionReports.add(marketExecutionReport);
        // Create Customer Execution Report from Market Execution Report
        ExecutionReport executionReport;
        try {
            executionReport = marketExecutionReport.clone();
        } catch (CloneNotSupportedException e1) {
            LOGGER.error("Error while trying to create Execution Report from Market Execution Report");
            operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
            return;
        }
        long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
        executionReport.setExecBroker(matchingMMcode);
        executionReport.setCounterPart(matchingMMcode);
        executionReport.setTipoConto(ExecutionReport.CONTO_PROPRIO);
        executionReport.setSequenceId(Long.toString(executionReportId));
        LOGGER.info("new exec report received from MATCHING market: {}", executionReport.toString());
        operation.getExecutionReports().add(executionReport);
        operation.setStateResilient(new MATCH_ExecutedState(), ErrorState.class);
    }

    @Override
    public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
        // Passaggio allo stato manuale
        try {
            matchingConnection.revokeOrder(operation, operation.getLastAttempt().getMarketOrder(), comment);
            titoliIncrociabiliService.resetMatchingOperation(operation);
            operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
        } catch (BestXException exc) {
            operation.setStateResilient(new WarningState(operation.getState(), exc, Messages.getString("MATCHMarketOperatorCommandError.0")), ErrorState.class);
        }
    }

    @Override
    public void onMarketMatchFound(MarketBuySideConnection source, Operation matching) {
        // DO NOTHING - For restore purpose
    }

    @Override
    public void onStateRestore(OperationState currentState) {
        operation.setStateResilient(new MATCH_StartExecutionState(), ErrorState.class);
    }
}