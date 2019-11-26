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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.management.SimpleOperationRestorerMBean;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;

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
   private boolean operationStatesCompleted = false;
   private boolean activeOperationsCompleted = false;

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
	   activeOperationsCompleted = false;
	   LOGGER.info("Find active operations from persistent layer...");
	   
	   List<Operation> activeOperations = initializer.getSystemState();

	   LOGGER.info("Restore {} active operations", (activeOperations != null ? activeOperations.size() : 0));
	   operationRegistry.insertActiveOperations(activeOperations);

	   LOGGER.info("Initialize {} operations", (activeOperations != null ? activeOperations.size() : 0));
	   operationRegistry.processOperations(new OperationInitAgent(operationFactory));
	   activeOperationsCompleted = true;
    }

   public void restoreOperationStates() throws BestXException {
      operationStatesCompleted = false;
      LOGGER.debug("Restore operation state");
      operationRegistry.processOperations(new OperationStateRestoreAgent());
      operationStatesCompleted = true;
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

   public boolean isRestoreActiveOperationsCompleted() {
      return activeOperationsCompleted;
   }

   public boolean isRestoreOperationStatesCompleted() {
      return operationStatesCompleted;
   }

	@Override
	public String killAndRestoreOperation(String orderId) {
		// [BESTX-545] AA 21-11-2019 adding this method in order to kill (from memory but not from db) an existing operation and restore it from the last good image 
		// in the db and put it after in warning state, so the operator can manage it from the web interface
		try {
			if (operationRegistry.killOperation(orderId)) {
				Operation operation = operationRegistry.loadOperationById(orderId);								
				if (operation != null) {
					operation.setStateResilient(new WarningState(operation.getState(),null, Messages.getString("RetrievedWarningState")), ErrorState.class);
				}
			}
		} catch (OperationNotExistingException e) {
			return "ERROR " + e.getMessage();
		} catch (BestXException e) {
			return "ERROR " + e.getMessage();
		}
		return null;
	}
}
