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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.ValidateByPunctualFilterState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 27/set/2012 
* 
**/
public class OrderRejectableEventHandler extends BaseOperationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderRejectableEventHandler.class);
    private final SerialNumberService serialNumberService;

    /**
     * @param operation
     */
    public OrderRejectableEventHandler(Operation operation, SerialNumberService serialNumberService) {
        super(operation);
        this.serialNumberService = serialNumberService;
    }

    @Override
    public void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment) {
        operation.setStateResilient(new WaitingPriceState(comment), ErrorState.class);
    }

    @Override
    public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
        operation.setStateResilient(new ManualExecutionWaitingPriceState(comment), ErrorState.class);
    }

    @Override
    public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
    	ExecutionReport newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), this.serialNumberService);
    	newExecution.setState(ExecutionReportState.REJECTED);
    	List<ExecutionReport> executionReports = operation.getExecutionReports();
    	executionReports.add(newExecution);
    	operation.setExecutionReports(executionReports);
    	operation.setStateResilient(new SendNotExecutionReportState(comment), ErrorState.class);
    }

    @Override
    public void onOperatorExceedFilterRecalculate(OperatorConsoleConnection source, String comment) {
        operation.setStateResilient(new ValidateByPunctualFilterState(operation.getState().getComment()), ErrorState.class);
    }

    @Override
    public void onStateRestore(OperationState currentState) {
        // DO NOTHING
    }

    @Override
    public void onOperatorSendDESCommand(OperatorConsoleConnection source, String comment) {
        throw new UnsupportedOperationException();
    }
}
