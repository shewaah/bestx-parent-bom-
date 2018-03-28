package it.softsolutions.bestx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationInitAgent implements OperationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationInitAgent.class);
    
    private OperationFactory operationFactory;

    public OperationInitAgent(OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    public void processOperation(Operation operation) throws BestXException {
        LOGGER.debug("Initialize operation: {}", operation);
        
        operationFactory.initOperation(operation);
    }
}
