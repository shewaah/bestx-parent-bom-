/**
 * 
 */
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;

/**
 * @author Stefano
 *
 */
public class BV_RejectedEventHandler extends BaseOperationEventHandler {
	/**
	 * @param operation
	 */
	public BV_RejectedEventHandler(Operation operation) {
		super(operation);
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		//In caso di rifiuto della RFCQ da parte del MM ritorno alla creazione di un nuovo book
        operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
	}
}
