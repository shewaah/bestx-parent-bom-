package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.Operation;

import java.util.List;

public interface OperationMatcher {
    boolean operationsMatch(Operation operationA, Operation operationB);
    boolean operationsMatch(Operation operation);
    List<Operation> getOperationsMatched(Operation operation);
}
