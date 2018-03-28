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
        ProposalType type = operation.getLastAttempt().getExecutionProposal().getType();
        //[RR20131024] BXMNT-356: we can start di execution also from proposals of type COUNTER
        if (type == ProposalType.INDICATIVE || type == ProposalType.COUNTER
                        //|| (type == ProposalType.COUNTER /* // FIXME ticket 4281 && market maker e' il mio (PriceForgeService.getAkrosDeskMarketMaker()) */)
                        || !DateUtils.isSameDay(operation.getOrder().getFutSettDate(), operation.getLastAttempt().getExecutionProposal().getFutSettDate())) {
            operation.setStateResilient(new BBG_SendRfqState(), ErrorState.class);
        } else {
            // TODO PM should be BBG_ReceiveQuoteState ?
            //operation.setStateResilient(new BBG_ReceiveQuoteState(), ErrorState.class);
            operation.setStateResilient(new WarningState(currentState, null, "Support for proposals non INDICATIVE not yet implemented"), ErrorState.class);
        }
        
	}
}
