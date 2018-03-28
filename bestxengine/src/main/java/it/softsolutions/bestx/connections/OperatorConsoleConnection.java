package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationState;

public interface OperatorConsoleConnection extends Connection {
    
    void publishOperationStateChange(Operation operation, OperationState oldState);

    void publishOperationDump(Operation operation, OperationState newState);

    void updateRevocationStateChange(Operation operation, RevocationState revocationState, String comment);
    
    void publishPriceDiscoveryResult(Operation operation, String priceDiscoveryResult);
}
