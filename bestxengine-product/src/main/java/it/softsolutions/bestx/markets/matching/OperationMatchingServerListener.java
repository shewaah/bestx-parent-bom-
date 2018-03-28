package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.Operation;

import java.util.List;

public interface OperationMatchingServerListener {
    void onOperationsMatch(Operation operationA, Operation operationB);
    void onOperationsMatch(List<Operation> matchedOperations);
}
