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
package it.softsolutions.bestx;

import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 19/feb/2013 
* 
**/
public class SimpleOperatorConsoleNotifier implements OperationStateListener {
    private OperatorConsoleConnection operatorConsoleConnection;

    public void init() throws BestXException {
        checkPreRequisites();
    }

    public void setOperatorConsoleConnection(OperatorConsoleConnection operatorConsoleConnection) {
        this.operatorConsoleConnection = operatorConsoleConnection;
    }

    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) {
        if (operatorConsoleConnection.isConnected())
            operatorConsoleConnection.publishOperationStateChange(operation, oldState); // Was newState. First time oldState is null
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operatorConsoleConnection == null) {
            throw new ObjectNotInitializedException("Operation console connection not set");
        }
    }
}
