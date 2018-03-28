
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
 
package it.softsolutions.bestx.handlers;

import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
  
/**
 *
 * Purpose: this class is mainly for helping with execution report creation and Operation management of execution reports
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 07/ago/2015
 * 
 **/

public class ExecutionReportHelper {

    /**
     * Utility method to create a basic execution report. Public and static because it is an utility needed in various places.
     * 
     * @param instrument
     *            The order instrument
     * @param side
     *            The order side
     * @param serialNumberService
     *            a {@link SerialNumberService} object to be queried for execution report unique number
     * @return An {@link ExecutionReport} object
     * @throws BestXException
     *             In case execution report unique number generation would fail
     */
 	public static ExecutionReport createExecutionReport(Instrument instrument,
			OrderSide side, SerialNumberService serialNumberService){
        return createExecutionReport(instrument, side, serialNumberService, DateService.newLocalDate());
	}
 	
 	public static ExecutionReport createExecutionReport(Instrument instrument,
			OrderSide side, SerialNumberService serialNumberService, Date transTime){
        ExecutionReport executionReport = new ExecutionReport();
        executionReport.setInstrument(instrument);
        executionReport.setSide(side);
        executionReport.setTransactTime(transTime);
        long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");
        executionReport.setSequenceId(Long.toString(executionReportId));
        return executionReport;
	}
 	
    /**
     * Method used to prepare the operation for the automatic not execution of the order. We must reset all timers because the execution is ended and we must build a rejection execution report and add
     * it to the operation ones.
     * 
     * Public and static because it is an utility needed in various places.
     * 
     * @throws BestXException
     *             if the execution report creation fails
     */
    public static void prepareForAutoNotExecution(Operation operation, SerialNumberService serialNumberService, ExecutionReportState executionReportState) throws BestXException {
        // build the report of not execution that will be persisted
        ExecutionReport newExecution = createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(), serialNumberService);
        newExecution.setState(executionReportState);
        List<ExecutionReport> executionReports = operation.getExecutionReports();
        executionReports.add(newExecution);
        operation.setExecutionReports(executionReports);
    }


}
