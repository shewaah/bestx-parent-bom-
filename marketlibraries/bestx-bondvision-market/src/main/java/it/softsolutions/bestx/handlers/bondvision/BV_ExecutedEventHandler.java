/**
 * 
 */
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendExecutionReportState;

/**
 * @author Stefano
 *
 */
public class BV_ExecutedEventHandler extends BaseOperationEventHandler {
	/**
	 * @param operation
	 */
	public BV_ExecutedEventHandler(Operation operation) {
		super(operation);
	}
	
	@Override
	public void onNewState(OperationState currentState) {
        operation.setStateResilient(new SendExecutionReportState(), ErrorState.class);
	}
}
