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
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.Operation;

import java.util.Collection;

import org.springframework.dao.DataAccessException;


public interface OperationDao {
    /**
     * Save OperationState to DB. If Operation has not been persisted yet to DB, save Operation before saving OperationState
     * @param operation The Operation to be persisted
     * @throws DataAccessException
     */
    void saveOperationState(Operation operation);
    /**
     * Delete all OperationBindings from DB. The Operation has become detached from outside world. Usually because in terminal state
     * @param operation The Operation for which bindings should be deleted from DB
     * @throws DataAccessException
     */
    void deleteOperationBindings(Operation operation);
    /**
     * Delete all Operations from DB. Usually because in terminal state
     * @param operation The Operation to be deleted from DB
     * @throws DataAccessException
     */
    void deleteOperation(Operation operation);
    /**
     * Returns all Operations with all their identifiers. Used to re-initialize the system from DB at startup.
     * @return A Collection of OperationIdentifier
     * @throws DataAccessException
     */
    Collection<Operation> getAllActiveOperations();
}
