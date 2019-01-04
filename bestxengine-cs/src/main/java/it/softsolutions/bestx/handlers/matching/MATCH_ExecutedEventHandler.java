/**
 * 
 */
package it.softsolutions.bestx.handlers.matching;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendExecutionReportState;

/**
 * @author Stefano
 *
 */
public class MATCH_ExecutedEventHandler extends CSBaseOperationEventHandler {
	/**
	 * @param operation
	 */
	public MATCH_ExecutedEventHandler(Operation operation) {
		super(operation);
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
	}
}
