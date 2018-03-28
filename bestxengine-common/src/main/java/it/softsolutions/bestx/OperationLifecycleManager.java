package it.softsolutions.bestx;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationLifecycleManager implements OperationStateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationLifecycleManager.class);
    
    private OperationRegistry operationRegistry;

    public void init() throws BestXException {
        checkPreRequisites();
    }

    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
    }

    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) {
        if (newState.isTerminal()) {
        	LOGGER.info("Order {}: Remove bindings from registry for operation in terminal state - ThreadName {}", operation.getOrder().getFixOrderId(), Thread.currentThread().getId());
            try {
                operationRegistry.removeOperation(operation);
            } catch (BestXException e) {
                LOGGER.error("An error occurred while removing operation from registry: {}", e.getMessage(), e);
            }
        }
    }
}
