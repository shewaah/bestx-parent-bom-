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
package it.softsolutions.bestx.connections.amosgateway;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.services.DateService;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-cs 
* First created by: davide.rossoni 
* Creation date: 15/ott/2012 
* 
**/
public class AmosFixRevokeReportLazyBean extends FixRevokeReportLazyBean {

    public AmosFixRevokeReportLazyBean(String fixSessionId, Operation operation, boolean accept, String comment) {
    	super(fixSessionId, operation, accept, comment);
        SimpleDateFormat amosDateTimeFormatter = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
        msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(orderId));
        msg.setValue(FixMessageFields.FIX_CumQty, orderQty.doubleValue());
        msg.setValue(FixMessageFields.FIX_ExecID, "0");
        msg.setValue(FixMessageFields.FIX_IDSource, "1");
        msg.setValue(FixMessageFields.FIX_LastPx, 0.0);
        msg.setValue(FixMessageFields.FIX_LastShares, 0.0);
        msg.setValue(FixMessageFields.FIX_OrderID, AmosUtility.getInstance().unwrapString(orderId));
        msg.setValue(FixMessageFields.FIX_Symbol, operation.getOrder().getInstrument().getDescription());
        if (accept) {
            msg.setValue(FixMessageFields.FIX_Text, "Revoca accettata");
        } else {
            msg.setValue(FixMessageFields.FIX_Text, "Revoca non accettata - Order not in a revocable state");
        }
        msg.setValue(FixMessageFields.FIX_TimeInForce, "0");
        amosDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        msg.setValue(FixMessageFields.FIX_TransactTime, amosDateTimeFormatter.format(transactTime));
        
        msg.setValue(FixMessageFields.FIX_FutSettDate, DateService.format(DateService.dateISO, futSettDate));
    }

}
