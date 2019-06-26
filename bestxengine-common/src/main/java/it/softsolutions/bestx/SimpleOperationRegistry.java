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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationAlreadyExistingException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.management.ConfigurableOperationRegistryMBean;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 19/feb/2013 
* 
**/
public class SimpleOperationRegistry implements OperationRegistry, TimerEventListener, ConfigurableOperationRegistryMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOperationRegistry.class);

    private OperationFactory operationFactory;
    private volatile long statExceptions;
    private OperationPersistenceManager operationPersistenceManager;

    private final List<Operation> activeOperations = new CopyOnWriteArrayList<Operation>();

    // TODO Monitoring-BX
    private int totalNumberOfOperations;
    
    public void init() throws BestXException {
        JobExecutionDispatcher.INSTANCE.addTimerEventListener(OperationRegistry.class.getSimpleName(), this);
        
        checkPreRequisites();
    }
    
    public void setOperationFactory(OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    public Operation getExistingOperationById(OperationIdType idType, String id) throws OperationNotExistingException, BestXException {
    	LOGGER.debug("idType = {}, id = {}", idType, id);
        for (int i = 0; i < activeOperations.size(); i++) {
            Operation operation = activeOperations.get(i);
            String tmpId = operation.getIdentifier(idType);
            if (tmpId != null && tmpId.equals(id)) {
                return operation;
            }
        }
        throw new OperationNotExistingException("Id type: " + idType + ", id: " + id);
    }

    public Operation getExistingOrNewOperationById(OperationIdType idType, String id) throws BestXException {
        LOGGER.debug("idType = {}, id = {}", idType, id);
        for (int i = 0; i < activeOperations.size(); i++) {
            Operation operation = activeOperations.get(i);
            String tmpId = operation.getIdentifier(idType);
            if (tmpId != null && tmpId.equals(id)) {
                return operation;
            }
        }
        return createNewOperationAndBinding(idType, id, false);
    }

    public Operation getNewOperationById(OperationIdType idType, String id, boolean isVolatile) throws OperationAlreadyExistingException, BestXException {
        for (int i = 0; i < activeOperations.size(); i++) {
            Operation operation = activeOperations.get(i);
            String tmpId = operation.getIdentifier(idType);
            if (tmpId != null && tmpId.equals(id)) {
                throw new OperationAlreadyExistingException("Id type: " + idType + ", id: " + id);
            }
        }
        return createNewOperationAndBinding(idType, id, isVolatile);
    }

    public void processOperations(OperationCommand operationCommand) throws BestXException {
        for (int i = 0; i < activeOperations.size(); i++) {
            Operation operation = activeOperations.get(i);
            operationCommand.processOperation(operation);
        }
    }

    @Override
    public void bindOperation(Operation operation, OperationIdType idType, String id) throws BestXException {
        LOGGER.debug("[INT-TRACE] Bind operation to type/id: {}/{}, operationID = {}", idType, id, operation.getId());
        operation.addIdentifier(idType, id);
        operationPersistenceManager.saveOperation(operation);
    }

    @Override
    public void removeOperationBinding(Operation operation, OperationIdType idType) {
        LOGGER.debug("operationID = {}, idType = {}", operation.getId(), idType);
        operation.removeIdentifier(idType);
        operationPersistenceManager.saveOperation(operation);
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationFactory == null) {
            throw new ObjectNotInitializedException("Operation factory not set");
        }
    }

    @Override
    public int getNumberOfActiveOperations() {
        checkPreRequisites();
        return activeOperations.size();
    }

    @Override
    public long getNumberOfExceptions() {
        return statExceptions;
    }

    @Override
    public void insertActiveOperations(Collection<Operation> operations) {
        for (Operation operation : operations) {
            activeOperations.add(operation);
            // TODO Monitoring-BX
            incTotalNumberOfOperations();
        }
    }

    public void removeOperation(Operation operation) throws BestXException {
        activeOperations.remove(operation);
    }

    private Operation createNewOperationAndBinding(OperationIdType idType, String id, boolean isVolatile) throws BestXException {
        Operation operation = operationFactory.createNewOperation(isVolatile);
        bindOperation(operation, idType, id);
        activeOperations.add(operation);
        // TODO Monitoring-BX
        incTotalNumberOfOperations();
        return operation;
    }

    @Override
    public String putOperationInVisibleState(String orderId) {
        try {
            Operation operation = this.getExistingOrNewOperationById(OperationIdType.ORDER_ID, orderId);
            return operation.putInVisibleState();

        } catch (BestXException e) {
            return (Messages.getString("PUT_IN_VISIBLE_STATE.1", e.getMessage()));
        }

    }

    @Override
    public int updateOrderText(String orderID, String newText) {
        try {
            Operation operation = this.getExistingOrNewOperationById(OperationIdType.ORDER_ID, orderID);
            if (operation == null) {
                LOGGER.error("Operation not found with orderID {} whle trying to update order text [{}]", orderID, newText);
                return 0;
            } else {
                LOGGER.debug("Order {}, update text: {}", orderID, newText);
                operation.getOrder().setText(newText);
                return 1;
            }
        } catch (BestXException e) {
            LOGGER.error("Error while trying to update text for order {}", orderID, e);            
            return 0;
        }
    }
    
    public OperationPersistenceManager getOperationPersistenceManager() {
        return operationPersistenceManager;
    }

    public void setOperationPersistenceManager(OperationPersistenceManager operationPersistenceManager) {
        this.operationPersistenceManager = operationPersistenceManager;
    }

    @Override
    public boolean operationExistsById(OperationIdType idType, String id, boolean isVolatile) {
        try {
            getExistingOperationById(idType, id);
            return true;
        } catch (OperationNotExistingException e) {
            return false;
        } catch (BestXException e) {
            return false;
        }
    }

    @Override
    public int getTotalNumberOfOperations() {
		// TODO Monitoring-BX
		return totalNumberOfOperations;
	}
	
	/**
	 * Increment totalNumberOfOperations number.
	 * 
	 * @return the int
	 */
	private int incTotalNumberOfOperations() {
		// TODO Monitoring-BX
		return ++totalNumberOfOperations;
	}

    @Override
    public void timerExpired(String jobName, String groupName) {
        Operation operation;
        try {
            operation = getExistingOperationById(OperationIdType.ORDER_ID, jobName);
            operation.onTimerExpired(jobName, groupName);
        } catch (BestXException e) {
            LOGGER.error("Could not notify timer expiration, no operation found for {}-{}", jobName, groupName);
        }
        
    }

	@Override
	public void updateOperation(Operation operation) {
    	if (operation == null) {
    		throw new IllegalArgumentException("operation cannot be null");
    	}
    	
    	operationPersistenceManager.saveOperation(operation);		
	}

   @Override
   public List<Operation> getOperationsByStates(List<Class<? extends BaseState>> operationStateClasses) {
      List<Operation> operations = new ArrayList<>();
      for (Operation activeOperation:activeOperations) {
         if (operationStateClasses.contains(activeOperation.getState().getClass())) {
            operations.add(activeOperation);
         }
      }
      return operations;
   }

@Override
public String getSimpleOrderOperationById(String id) throws OperationNotExistingException, BestXException {
	// TODO Auto-generated method stub
	return null;
}
   
   
}


