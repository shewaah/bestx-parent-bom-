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

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.management.SimpleOperationRestorerMBean;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class SimpleOperationRestorer implements SimpleOperationRestorerMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOperationRestorer.class);

    private Initializer initializer;
    private OperationRegistry operationRegistry;
    private OperationFactory operationFactory;

    public void setInitializer(Initializer initializer) {
        this.initializer = initializer;
    }

    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    public void setOperationFactory(OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    public void init() throws ObjectNotInitializedException {
        checkPreRequisites();
    }

    public void restoreActiveOperations() throws BestXException {
        List<Operation> activeOperations = initializer.getSystemState();
        
        LOGGER.debug("Restore {} active operations from persistent layer", (activeOperations != null ? activeOperations.size() : 0));
        operationRegistry.insertActiveOperations(activeOperations);
        
        LOGGER.debug("Initialize {} operations restored from persistent layer", (activeOperations != null ? activeOperations.size() : 0));
        operationRegistry.processOperations(new OperationInitAgent(operationFactory));
    }

    public void restoreOperationStates() throws BestXException {
        LOGGER.debug("Restore operation state");
        operationRegistry.processOperations(new OperationStateRestoreAgent());
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (initializer == null) {
            throw new ObjectNotInitializedException("Operation initializer not set");
        }
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
        if (operationFactory == null) {
            throw new ObjectNotInitializedException("Operation factory not set");
        }
    }
}
