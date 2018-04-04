/**
 * 
 */
package it.softsolutions.bestx.handlers.bloomberg;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendExecutionReportState;

/**
 * @author Stefano
 *
 */
public class BBG_ExecutedEventHandler extends BaseOperationEventHandler {
	/**
	 * @param operation
	 */
	public BBG_ExecutedEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onNewState(OperationState currentState) {
		operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
	}
}
