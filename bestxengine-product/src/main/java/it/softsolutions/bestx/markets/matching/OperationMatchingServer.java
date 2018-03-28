package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.Operation;

public interface OperationMatchingServer {
    
    boolean isOn();

    void setOperationMatchingServerListener(OperationMatchingServerListener listener);

    void addOperation(Operation operation);

    void removeOperation(Operation operation);

    void removeOperations(Operation operationA, Operation operationB);

    void start();

    void stop();
}
