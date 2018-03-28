package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;

public class OperationAlreadyMatchedException extends BestXException {

    private static final long serialVersionUID = 4913273531110656157L;
    
    private Operation operation;

    public OperationAlreadyMatchedException(Operation operation) {
        super("Operation already matched: " + operation);
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }
}
