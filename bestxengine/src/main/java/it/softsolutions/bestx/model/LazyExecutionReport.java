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
package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class LazyExecutionReport extends ExecutionReport {
    
    private ExecutionReport executionReport;
    private Operation operation;
    
    public LazyExecutionReport(Operation operation) {
        this.operation = operation;
    }
    
    private void loadExecutionReportIfNull() {
        if (executionReport == null) {
            List<ExecutionReport> executionReportList = operation.getExecutionReports();
            
            if (executionReportList != null) {
                executionReport = executionReportList.get(executionReportList.size() - 1);
            }
        }
    }
    @Override
	public BigDecimal getActualQty() {
        loadExecutionReportIfNull();
        return executionReport.getActualQty();
    }

    @Override
	public Instrument getInstrument() {
        loadExecutionReportIfNull();
        return executionReport.getInstrument();
    }

    @Override
	public Market getMarket() {
        loadExecutionReportIfNull();
        return executionReport.getMarket();
    }

    @Override
	public BigDecimal getOrderQty() {
        loadExecutionReportIfNull();
        return executionReport.getOrderQty();
    }

    @Override
	public Money getPrice() {
        loadExecutionReportIfNull();
        return executionReport.getPrice();
    }

    @Override
	public Rfq.OrderSide getSide() {
        loadExecutionReportIfNull();
        return executionReport.getSide();
    }

    @Override
	public ExecutionReport.ExecutionReportState getState() {
        loadExecutionReportIfNull();
        return executionReport.getState();
    }

    @Override
	public Date getTransactTime() {
        loadExecutionReportIfNull();
        return executionReport.getTransactTime();
    }

    @Override
	public String getSequenceId() {
        loadExecutionReportIfNull();
        return executionReport.getSequenceId();
    }

    @Override
	public String getTicket() {
        loadExecutionReportIfNull();
        return executionReport.getTicket();
    }
}
