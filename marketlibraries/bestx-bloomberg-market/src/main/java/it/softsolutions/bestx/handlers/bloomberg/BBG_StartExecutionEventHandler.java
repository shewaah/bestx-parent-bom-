/**
 * 
 */
package it.softsolutions.bestx.handlers.bloomberg;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;

import org.apache.commons.lang3.time.DateUtils;

/**
 * @author Stefano
 *
 */
public class BBG_StartExecutionEventHandler extends BaseOperationEventHandler {
	
    private static final long serialVersionUID = 3476645384592497728L;

    /**
	 * @param operation
	 */
	public BBG_StartExecutionEventHandler(Operation operation) {
		super(operation);
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		operation.setStateResilient(new BBG_SendRfqState(), ErrorState.class);
	}
}
