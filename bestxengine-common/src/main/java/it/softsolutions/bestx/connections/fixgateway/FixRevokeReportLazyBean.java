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
package it.softsolutions.bestx.connections.fixgateway;

import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Order;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 15/ott/2012 
* 
**/
public class FixRevokeReportLazyBean extends FixExecutionReportOutputLazyBean {
    
    public FixRevokeReportLazyBean(String fixSessionId, Operation operation, boolean accept, String comment) {
        super(fixSessionId, operation.getOrder(), operation.getOrder().getFixOrderId());
        Order order = operation.getOrder();
        //[20150116] CRSBXTEM-146: on revoke accepted the OrdStatus is CANCELLED, on revoke rejected we must look for
        //the last execution report status
        String ordStatus = null;
        if (accept) {
            msg.setName(FixMessageFields.FIX_OrderRevokeAccept);
            msg.setValue(FixMessageFields.FIX_ExecID, order.getFixOrderId() + "_CANCEL");
            ordStatus = "" + ExecutionReportState.CANCELLED.getValue();
        } else {
            msg.setName(FixMessageFields.FIX_OrderRevokeReject);
            ordStatus = "" + ExecutionReportState.NEW.getValue();
            List<ExecutionReport> execReports = operation.getExecutionReports();
            if (execReports != null && !execReports.isEmpty())  {
            	ExecutionReport lastExecReport = execReports.get(execReports.size() - 1);
            	ordStatus = lastExecReport.getState() != null? lastExecReport.getState().getValue() : "" + ExecutionReportState.NEW.getValue();
            }
        }
        
        msg.setValue(FixMessageFields.FIX_Text, comment == null ? "" : comment);
        msg.setValue(FixMessageFields.FIX_OrdStatus, ordStatus);
        msg.setValue(FixMessageFields.FIX_LastPx, order.getLimit() == null ? 0.0 : order.getLimit().getAmount().doubleValue());
        msg.setValue(FixMessageFields.FIX_AvgPx, 0f);
        msg.setValue(FixMessageFields.FIX_CumQty, 0f);
        msg.setValue(FixMessageFields.FIX_LeavesQty, 0f);
        msg.setValue(FixMessageFields.FIX_Commission, 0f);
        msg.setValue(FixMessageFields.FIX_CommType, "3");
        msg.setValue(FixMessageFields.FIX_TimeInForce, "6");
    }
}
