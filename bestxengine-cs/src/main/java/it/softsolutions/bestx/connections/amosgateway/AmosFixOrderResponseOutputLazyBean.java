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

import it.softsolutions.bestx.connections.CustomerConnection.ErrorCode;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.model.Order;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class AmosFixOrderResponseOutputLazyBean extends
FixOrderResponseOutputLazyBean {

    public AmosFixOrderResponseOutputLazyBean(String fixSessionId, Order order,
                    ErrorCode errorCode, String errorMsg) {
        super(fixSessionId, order, errorCode, errorMsg);
        String orderQty = order.getQty().toPlainString();

        msg.setValue(FixMessageFields.FIX_OrdStatus, errorCode.getCode() == 0? "0" : "8");
        msg.setValue(FixMessageFields.FIX_AvgPx, "0.0");
        msg.setValue(FixMessageFields.FIX_LastPx, "0.0");
        msg.setValue(FixMessageFields.FIX_CumQty, orderQty);
        msg.setValue(FixMessageFields.FIX_ExecTransType, "0");
        msg.setValue(FixMessageFields.FIX_ExecType, errorCode.getCode() == 0? "0" : "8");
        msg.setValue(FixMessageFields.FIX_LastShares, orderQty);
        msg.setValue(FixMessageFields.FIX_LeavesQty, "0.0");

        //20120521 - STEFANO - Verificare se deve essere passato 0
        //msg.setValue(FixMessageFields.FIX_OrderID, "0");
        msg.setValue(FixMessageFields.FIX_OrderID, AmosUtility.getInstance().unwrapString(order.getFixOrderId()));
        msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(order.getFixOrderId()));
    }


}
