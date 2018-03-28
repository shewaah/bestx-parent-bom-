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

/**
 * 
 * Purpose: interface for all the bestx strategies, the core of the state machine
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 09/ott/2012
 * 
 **/
public interface Strategy {
    /**
     * Retrieve the handler of the operation for the given state
     * 
     * @param operation
     *            : the operation that changed state
     * @param state
     *            : the new staet
     * @return a new handler
     * @throws BestXException
     *             : if something goes wrong
     */
    OperationEventListener getHandler(Operation operation, OperationState state) throws BestXException;
}
