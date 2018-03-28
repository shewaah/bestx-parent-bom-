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
package it.softsolutions.bestx.states;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;

/**
 * 
 * Purpose: An Operation can go into this state only when the customer has enabled the auto unexecution. Instead of going to the curando
 * state we come here, but we arrive from states that didn't save the eventual book on the persistence database. This is the difference from
 * the SendNotExecutionReportState. That's why we need to save the book overriding the supeclass' method "mustSaveBook"
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public class SendAutoNotExecutionReportState extends BaseState implements Cloneable {

    private SendAutoNotExecutionReportState() {
        super(OperationState.Type.SendAutoNotExecutionReport, null);

    }

    public SendAutoNotExecutionReportState(String comment) {
        super(OperationState.Type.SendAutoNotExecutionReport, null);
        setComment(comment);
    }

    @Override
    public OperationState clone() throws CloneNotSupportedException {
        return new SendAutoNotExecutionReportState(getComment());
    }

    @Override
    public void validate() throws BestXException {
    }

    @Override
    public boolean isRevocable() {
        return false;
    }

    @Override
    public boolean mustSaveBook() {
        return true;
    }

}
