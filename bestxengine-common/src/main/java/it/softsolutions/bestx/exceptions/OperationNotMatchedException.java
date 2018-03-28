package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;

public class OperationNotMatchedException extends BestXException {
    
    private static final long serialVersionUID = 4447238319843343611L;
    
    private Operation operation;
    
    public OperationNotMatchedException(Operation operation) {
        super("Operation already matched: " + operation);
        this.operation = operation;
    }
    
    public Operation getOperation() {
        return operation;
    }
}
