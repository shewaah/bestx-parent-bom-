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

package it.softsolutions.bestx.connections.ib4j;

import it.softsolutions.bestx.SystemStateSelector;
import it.softsolutions.ib4j.clientserver.IBcsMessage;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-protocol-api 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class SystemStatusReplyMessage extends IBcsMessage {

    private static final long serialVersionUID = -9057121035753495436L;

    /**
     * Instantiates a new akros system status reply message.
     *
     * @param sessionId the session id
     * @param systemStatusSelector the system status selector
     */
    public SystemStatusReplyMessage(String sessionId, SystemStateSelector systemStatusSelector) {    
        setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
        setStringProperty(IB4JOperatorConsoleMessage.TYPE_STATUS, IB4JOperatorConsoleMessage.STATUS_OK);
        setStringProperty(IB4JOperatorConsoleMessage.RESP_TYPE, IB4JOperatorConsoleMessage.RESP_TYPE_QUERY);
        setStringProperty(IB4JOperatorConsoleMessage.TYPE_QUERY, IB4JOperatorConsoleMessage.QUERY_GET_SYSTEM_ACTIVITY_STATUS);
        setIntProperty(IB4JOperatorConsoleMessage.FLD_SYSTEM_STATUS, (systemStatusSelector.isOrderEnabled() && systemStatusSelector.isRfqEnabled()) ? 1 : 0);
    }
}
