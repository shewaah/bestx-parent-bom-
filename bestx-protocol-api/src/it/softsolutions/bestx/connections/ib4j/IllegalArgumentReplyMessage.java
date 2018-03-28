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
public class IllegalArgumentReplyMessage extends IBcsMessage {

    private static final long serialVersionUID = 5466831633262867653L;

    /**
     * Instantiates a new akros illegal argument reply message.
     *
     * @param sessionId the session id
     * @param reason the reason
     */
    public IllegalArgumentReplyMessage(String sessionId, String reason) {
        setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
        this.setStringProperty(IB4JOperatorConsoleMessage.TYPE_STATUS, IB4JOperatorConsoleMessage.STATUS_ILLEGAL_ARGUMENTS);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_STATUS_MESSAGE, reason);
    }
}
