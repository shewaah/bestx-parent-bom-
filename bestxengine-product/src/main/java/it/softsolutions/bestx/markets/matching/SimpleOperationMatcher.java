/**
 * 
 */
package it.softsolutions.bestx.markets.matching;

import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Rfq.OrderSide;

/**
 * @author Stefano
 *
 */
public class SimpleOperationMatcher implements OperationMatcher {

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.markets.matching.OperationMatcher#operationsMatch(it.softsolutions.bestx.Operation, it.softsolutions.bestx.Operation)
	 */
	public boolean operationsMatch(Operation operationA, Operation operationB) {
		if (operationA.getOrder().getInstrument().getIsin().equalsIgnoreCase(operationB.getOrder().getInstrument().getIsin()) &&
				operationA.getOrder().getCustomer().getFixId().equalsIgnoreCase(operationB.getOrder().getCustomer().getFixId()) &&
				operationA.getOrder().getFutSettDate().equals(operationB.getOrder().getFutSettDate()) &&
				!operationA.getOrder().getSide().getFixCode().equalsIgnoreCase(operationB.getOrder().getSide().getFixCode()) &&
				operationA.getOrder().getQty().compareTo(operationB.getOrder().getQty()) == 0 &&
				((operationA.getOrder().getLimit() == null || operationB.getOrder().getLimit() == null) ||
						(operationA.getOrder().getSide() == OrderSide.BUY &&
						operationA.getOrder().getLimit().getAmount().compareTo(operationB.getOrder().getLimit().getAmount()) >= 0) ||
						(operationA.getOrder().getSide() == OrderSide.SELL &&
								operationA.getOrder().getLimit().getAmount().compareTo(operationB.getOrder().getLimit().getAmount()) <= 0))) {

			return true;
		}
		return false;
	}

	public boolean operationsMatch(Operation operation) {
		throw new UnsupportedOperationException();
	}

	public List<Operation> getOperationsMatched(Operation operation) {
		return null;
	}
}