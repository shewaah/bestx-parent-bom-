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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.fixgateway.FixGatewayConnector;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationAlreadyExistingException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.management.ConfigurableOperationRegistryMBean;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class CachedOperationRegistry implements OperationRegistry, ConfigurableOperationRegistryMBean, TimerEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedOperationRegistry.class);

    private final ConcurrentMap<String, Long> idToOperationIdMap = new ConcurrentHashMap<String, Long>();
    
    // <operationID, semaphore>
    private static ConcurrentMap<Long, Semaphore> semaphores = new ConcurrentHashMap<Long, Semaphore>();

    private OperationFactory operationFactory;
    private volatile long statExceptions;
    private OperationPersistenceManager operationPersistenceManager;
    // TODO Monitoring-BX
    private AtomicInteger totalNumberOfOperations = new AtomicInteger();
    private HashMap<Long, Operation> cache;
    private int limitFileCommentMaxLen;
    
    public void init() throws BestXException {
        cache = new HashMap<Long, Operation>();
        JobExecutionDispatcher.INSTANCE.addTimerEventListener(OperationRegistry.class.getSimpleName(), this);
    }

    public void setOperationFactory(OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    @Override
	public Operation getExistingOperationById(OperationIdType idType, String id) throws OperationNotExistingException, BestXException {
        LOGGER.debug("Retrieve existing operation by type/id: {}/{}", idType, id);
        checkPreRequisites();

        Operation operation = getCachedOperation(idType, id);

        if (operation != null) {
            return operation;
        } else {
            throw new OperationNotExistingException("Id type: " + idType + ", id: " + id);
        }
    }
    

    @Override
	public Operation getExistingOrNewOperationById(OperationIdType idType, String id) throws BestXException {
        LOGGER.debug("Retrieve existing or new operation by type/id: {}/{}", idType, id);
        checkPreRequisites();

        Operation operation = getCachedOperation(idType, id);
        if (operation == null) {
            operation = createNewOperationAndBinding(idType, id, false);
        }

        return operation;
    }

    @Override
	public Operation getNewOperationById(OperationIdType idType, String id, boolean isVolatile) throws OperationAlreadyExistingException, BestXException {
        LOGGER.debug("Getting new operation by type/id: {}/{}", idType, id);
        checkPreRequisites();
        
        if (operationExistsById(idType, id, isVolatile)) {
            LOGGER.warn("exception, operation already existing : " + idType + "/" + id);
            throw new OperationAlreadyExistingException("Id type: " + idType + ", id: " + id);
        }

        return createNewOperationAndBinding(idType, id, isVolatile);
    }

    @Override
	public void processOperations(OperationCommand operationCommand) throws BestXException {
    	LOGGER.debug("operationCommand = {}", operationCommand);
        checkPreRequisites();
        
        // convert to set, so duplicates are removed
        Set<Long> noDupOperationIds = new HashSet<Long>(idToOperationIdMap.values());
        LOGGER.debug("Processing {} operations", noDupOperationIds.size());
        
        for (Long operationId : noDupOperationIds) {
            Operation op = getCachedOperation(operationId);
            if (op != null) {
                operationCommand.processOperation(op);
                LOGGER.trace("operation = {} > {}", op.getOrder().getFixOrderId(), op.getState().getClass());
            }
        }
    }

    @Override
    public void bindOperation(Operation operation, OperationIdType idType, String id) throws BestXException {
        LOGGER.debug("[INT-TRACE] operationID={}, Bind operation to type/id: {}/{}", operation.getId(), idType, id);
        checkPreRequisites();

        operation.addIdentifier(idType, id);
        String key = idType + "#" + id; // key is, e.g., 'SESSION_ID#xxxyyy'
        idToOperationIdMap.put(key, operation.getId());

        operationPersistenceManager.saveOperation(operation);
    }

	@Override
	public void removeOperationBinding(Operation operation, OperationIdType idType) {
		if(operation == null) {
        	LOGGER.error("operation variable is null");
        	return;
        }
		LOGGER.debug("OperationID={}, removing operation bind for type: {}", operation.getId(), idType);
		
		operation.removeIdentifier(idType);

		// remove binding from map, too
		Iterator<Map.Entry<String, Long>> iter = idToOperationIdMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Long> entry = iter.next();
			if ((entry != null && operation != null && entry.getValue().equals(operation.getId())) && (entry.getKey().startsWith(idType + "#"))) {
				iter.remove();
			}
		}

		operationPersistenceManager.saveOperation(operation);
	}

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationFactory == null) {
            throw new ObjectNotInitializedException("Operation factory not set");
        }

        if (cache == null) {
            cache = new HashMap<Long, Operation>();
        }
    }

    @Override
    public int getNumberOfActiveOperations() {
        return operationPersistenceManager.getNumberOfDailyOperations();
    }

    @Override
    public long getNumberOfExceptions() {
        return statExceptions;
    }

    @Override
    public void insertActiveOperations(Collection<Operation> operations) {
        for (Operation operation : operations) {
            addOperation(operation);
        }
    }

    @Override
	public void removeOperation(Operation operation) throws BestXException {
        if (operation == null) {
            throw new BestXException("Null operation, cannot remove");
        }

        cache.remove(operation.getId());
    }

    private Operation createNewOperationAndBinding(OperationIdType idType, String id, boolean isVolatile) throws BestXException {
        LOGGER.trace("idType = {}, id = {}", idType, id);
        
        Operation operation = operationFactory.createNewOperation(isVolatile);
        if (operation.getId()==null) {
        	operation.setId(DateService.currentTimeMillis());
        } else {
        	bindOperation(operation, idType, id);
        }

        addOperation(operation);
        LOGGER.info("operationId {} CREATED and BOUND", operation.getId());

        return operation;
    }

    @Override
    public String putOperationInVisibleState(String orderId) {
        try {
            Operation operation = getExistingOrNewOperationById(OperationIdType.ORDER_ID, orderId);
            return operation.putInVisibleState();

        } catch (BestXException e) {
            return (Messages.getString("PUT_IN_VISIBLE_STATE.1", e.getMessage()));
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
        String key = idType + "#" + id;
        if (idToOperationIdMap.containsKey(key)) {
            return true;
        } else if (!isVolatile) {
            return operationPersistenceManager.operationExistsById(idType, id);
        } else {
        	return false;
        }
    }

    private void addOperation(Operation operation) {
        if (operation != null) {
            for (Map.Entry<OperationIdType, String> mapEntry : operation.getIdentifiers().entrySet()) {
                String key = mapEntry.getKey() + "#" + mapEntry.getValue(); // key is, e.g., 'ORDER_ID#A1372866975445'
                idToOperationIdMap.put(key, operation.getId());
                totalNumberOfOperations.incrementAndGet();
            }

            cache.put(operation.getId(), operation);

        }
    }

    private Operation getCachedOperation(OperationIdType idType, String id) {
    	LOGGER.trace("idType = {}, id = {}", idType, id);
    	
        // convert from idType + id to operationd
//        if (operationExistsById(idType, id)) {
            String key = idType + "#" + id;
            Long opId = idToOperationIdMap.get(key);
            // not available in memory, try on db
            if (opId == null && operationPersistenceManager.operationExistsById(idType, id)) {
                opId = operationPersistenceManager.getOperationIdFromBinding(idType, id);
            }
            if (opId != null) {
                return getCachedOperation(opId);
            } else {
                return null;
            }
//        }

//        return null;
    }

    private Operation getCachedOperation(long operationID) {
    	LOGGER.trace("orderID = {}", operationID);
    	
    	Operation operation = null;

    	Semaphore semaphore = null;
    	if (semaphores.containsKey(operationID)) {
    		semaphore = semaphores.get(operationID);
    	} else {
    		semaphore = new Semaphore(1);
    		semaphores.put(operationID, semaphore);
    	}

    	try {
    		// lock the operationID-related resource
    		semaphore.acquire();

    		operation = cache.get(operationID);

    		if (operation == null) {
    			// TODO PM remove
    			//LOGGER.info("Antani operationId {} NOT found in cache, searching db", operationId);
    			try {
    				operation = operationPersistenceManager.getOperationForCurrentDate(operationID);
    				if (operation != null) {
    					cache.put(operation.getId(), operation);
    				}
    			} catch (BestXException e) {
    				return null;
    			}
    		}

    	} catch (InterruptedException e1) {
    		LOGGER.error("{}", e1.getMessage(), e1);
    	} finally {
    		// release the operationID-related resource
    		semaphore.release();
    	}
    	return operation;
    }


    @Override
    public int getTotalNumberOfOperations() {
    	return operationPersistenceManager.getTotalNumberOfDailyOperations();
	}

    /**
     * @return the limitFileCommentMaxLen
     */
    public int getLimitFileCommentMaxLen() {
        return limitFileCommentMaxLen;
    }

    /**
     * @param limitFileCommentMaxLen the limitFileCommentMaxLen to set
     */
    public void setLimitFileCommentMaxLen(int limitFileCommentMaxLen) {
        this.limitFileCommentMaxLen = limitFileCommentMaxLen;
    }

    @Override
    public void timerExpired(String jobName, String groupName) {
        LOGGER.debug("jobName={}, groupName={}", jobName, groupName);
        
        // -- operation 
        String operationID = jobName;
        //check for a composite jobName coming from some handlers
        if (jobName.indexOf('#') >= 0) {
        	operationID = jobName.split("#")[0];   
        } 
        
        Operation operation = getCachedOperation(Long.parseLong(operationID));
            
        if (operation != null) {
        	operation.onTimerExpired(jobName, groupName);
        } else {
        	LOGGER.error("Could not notify timer expiration, no operation found for {}.{}", groupName, jobName);
        }
            
    }
    
    @Override
    public int updateOrderText(String orderID, String newText) {
        int returnValue = 0;
        Operation operation = getCachedOperation(OperationIdType.ORDER_ID, orderID);
        if (operation == null) {
            LOGGER.error("Operation not found with orderID {} whle trying to update order text [{}]", orderID, newText);
            return -1;
        } else {
            LOGGER.debug("Order {}, update text: {}", orderID, newText);
            /*
            if (newText != null && newText.length() > limitFileCommentMaxLen) {
                LOGGER.debug("Comment [{}]longer than {}, truncated to [{}]", newText, limitFileCommentMaxLen, newText.substring(0, limitFileCommentMaxLen));
                newText = newText.substring(0, limitFileCommentMaxLen);
                returnValue = limitFileCommentMaxLen;
            }
            */
            operation.getOrder().setText(newText);
            try {
                operationPersistenceManager.saveOperation(operation);
            } catch (Exception e) {
                LOGGER.error("Error while saving order {} with new message [{}]", orderID, newText, e);
                return -1;
            }
            return returnValue;
        }
    }

    @Override
    public void updateOperation(Operation operation) {
    	if (operation == null) {
    		throw new IllegalArgumentException("operation cannot be null");
    	}
    	
    	try {
    		operationPersistenceManager.saveOperation(operation);
    	} catch (Exception e) {
    		LOGGER.error("Error while updating the operation {}", operation.getOrder().getFixOrderId(), e);
    	}
    }

   @Override
   public List<Operation> getOperationsByStates(List<Class<? extends BaseState>> operationStateClasses) {
      ArrayList<String> opStateCanonicalNames = new ArrayList<>();
      for (Class clazz:operationStateClasses)
         opStateCanonicalNames.add(clazz.getCanonicalName());
      
      //Initially we looked for operations using the persistence manager but some components, such as market order with their session ids are not immediately persisted so we may have uncomplete objects
      //we decided to look in the cache since the cache stores all the operations and doesn't persist
      //return operationPersistenceManager.getActiveOperationForStates(opStateCanonicalNames);
      
      ArrayList<Operation> operations = new ArrayList<>();
      //Hint: THIS IS NOT THREAD SAFE
      /*for (Operation operation:cache.values()) {
         if (opStateCanonicalNames.contains(operation.getState().getClass().getCanonicalName())) {
            operations.add(operation);
         }
      }
      */
      
      //code inspired by processOperations
      //convert to set, so duplicates are removed
      
      long startTime = System.currentTimeMillis();
      
      Set<Long> noDupOperationIds = new HashSet<Long>(idToOperationIdMap.values());
      for (Long operationId : noDupOperationIds) {
         Operation op = getCachedOperation(operationId);
         if (op != null && opStateCanonicalNames.contains(op.getState().getClass().getCanonicalName())) {
            operations.add(op);
         }
      }

      LOGGER.info("getOperationsByStates execution time (millis): " + (System.currentTimeMillis() - startTime));
      
      return operations;
   }   
   
}
