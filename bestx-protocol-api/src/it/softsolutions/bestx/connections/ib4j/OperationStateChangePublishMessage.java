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
 * First created by: paolo.midali 
 * Creation date: 19-ott-2012 
 * 
 **/
public class OperationStateChangePublishMessage extends IBcsMessage {

    private static final long serialVersionUID = -7897794123530019148L;

    // new style message, for the new GUI based on DF2/Extreme technology
    /**
     * Instantiates a new akros operation state change publish message.
     *
     * @param rfqId the rfq id
     * @param oldStateName the old state name
     * @param newStateName the new state name
     */
    public OperationStateChangePublishMessage(String rfqId, String oldStateName, String newStateName) {
        this.setIBPubSubSubject(IB4JOperatorConsoleMessage.NOTIFY_OP_STATUS_CHG);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_RQF_ID, rfqId);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_OLD_STATE_NAME, oldStateName);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_STATE_NAME, newStateName);
    }

    // old style message, never really used
    /**
     * Instantiates a new akros operation state change publish message.
     *
     * @param rfqId the rfq id
     * @param newStateName the new state name
     */
    public OperationStateChangePublishMessage(String rfqId, String newStateName) {
        this.setIBPubSubSubject(IB4JOperatorConsoleMessage.NOTIFY_OP_STATUS_CHG);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_RQF_ID, rfqId);
        this.setStringProperty(IB4JOperatorConsoleMessage.FLD_STATE_NAME, newStateName);
    }
}
