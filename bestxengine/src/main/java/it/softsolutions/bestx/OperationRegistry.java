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

import java.util.Collection;
import java.util.List;

import it.softsolutions.bestx.exceptions.OperationAlreadyExistingException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;

/**
 * Purpose: Main interface for interface components for communicating with operations.
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public interface OperationRegistry {
    
    /**
     * Fill the registry with the active operations.
     *
     * @param operations a Collection of active operations
     */
    void insertActiveOperations(Collection<Operation> operations);

    /**
     * Creates a new operation and bind it to the given id. If an operation already exists bound to the given id, an exception is thrown.
     * 
     * @param idType
     *            Type of operation id
     * @param id
     *            Specific id
     * @return An operation
     * @throws OperationAlreadyExistingException
     *             If an operation is already bound to the id
     * @throws BestXException
     *             If an unexpected error occurs while processing
     */
    Operation getNewOperationById(OperationIdType idType, String id, boolean isVolatile) throws OperationAlreadyExistingException, BestXException;

    /**
     * Retrieves an existing operation bound to the given id. If no operation exists bound to this id, an exception id thrown
     * 
     * @param idType
     *            Type of operation id
     * @param id
     *            Specific id
     * @return An operation
     * @throws OperationNotExistingException
     *             If no operation is bound to the id
     * @throws BestXException
     *             If an unexpected error occurs while processing
     */
    Operation getExistingOperationById(OperationIdType idType, String id) throws OperationNotExistingException, BestXException;

    /**
     * Retrieves an operation bound to the given id, or creates a new one if it doesn't exist already
     * 
     * @param idType
     *            Type of operation id
     * @param id
     *            Specific id
     * @return An operation
     * @throws BestXException
     *             If an unexpected error occurs while processing
     */
    Operation getExistingOrNewOperationById(OperationIdType idType, String id) throws BestXException;

    /**
     * Iteratively run a command on every operation in the registry. If any command run throws an exception, the method exits throwing the
     * same exception.
     * 
     * @param operationCommand
     *            {@link OperationCommand} object
     * @throws BestXException
     *             In case the command throws an exception
     */
    void processOperations(OperationCommand operationCommand) throws BestXException;

    /**
     * Binds an existing operation to the given idType/id. If the operation is already bound to an id, the old one is discarded.
     * 
     * @param operation
     *            The operation to be bound
     * @param idType
     *            The id type
     * @param id
     *            The id for this operation
     * @throws BestXException
     */
    void bindOperation(Operation operation, OperationIdType idType, String id) throws BestXException;

    /**
     * Removes the binding of an existing operation for the given idType. the old one is discarded.
     * 
     * @param operation
     *            The operation to be bound
     * @param idType
     *            The id type
     * @param id
     *            The id for this operation
     * @throws BestXException
     */
    public void removeOperationBinding(Operation operation, OperationIdType idType);

    /**
     * Remove operation from the active operations registry
     * 
     * @param operation
     *            The operation to be removed
     */
    void removeOperation(Operation operation) throws BestXException;

    /**
     * Verify if operation has already been created, by associated id.
     *
     * @param idType the id type
     * @param id the id (order, rfq, etc), associated to the operation
     * @param isVolatile if the operation does not need to be persisted on DB
     * @return true, if present
     */
    boolean operationExistsById(OperationIdType idType, String id, boolean isVolatile);
    
    /**
     * Explicit need to update operation's persisted fields.
     * @param operation the operation that must be updated
     */
    public void updateOperation(Operation operation);
    
    /**
     * Given an operation state, this method returns a compatible list of active operations currently having the required states
     * @param operationStateClasses
     * @return
     */
    public List<Operation> getOperationsByStates(List<Class<? extends BaseState>> operationStateClasses);
}
