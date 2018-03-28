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
 * Purpose: message sent by Bestx engine when the command has been successfully received  
 *
 * Project Name : bestxengine-protocol-api 
 * First created by: anna cochetti 
 * Creation date: 12/06/2016 
 * 
 **/
public class CommandOkReplyMessage extends IBcsMessage {

    private static final long serialVersionUID = -8027874499629763814L;

    /**
     * Instantiates a new command ok reply message.
     *
     * @param sessionId the session id
     */
    public CommandOkReplyMessage(String sessionId) {
        setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
        setStringProperty(IB4JOperatorConsoleMessage.TYPE_STATUS, IB4JOperatorConsoleMessage.STATUS_OK);
        setStringProperty(IB4JOperatorConsoleMessage.RESP_TYPE, IB4JOperatorConsoleMessage.RESP_TYPE_COMMAND);
    }
}
