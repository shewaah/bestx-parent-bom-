/**
 * 
 */
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;

/**
 * @author Stefano
 *
 */
public class BV_RejectQuoteEventHandler extends BV_ManagingRfqEventHandler {

	private final MarketBuySideConnection bvMarket;

	/**
	 * @param operation
	 */
	public BV_RejectQuoteEventHandler(Operation operation, MarketBuySideConnection bvMarket) {
		super(operation);
		
		this.bvMarket = bvMarket;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		try {
			ClassifiedProposal counter = operation.getLastAttempt().getExecutablePrice(Attempt.BEST) == null ? null : operation.getLastAttempt().getExecutablePrice(Attempt.BEST).getClassifiedProposal();
			bvMarket.rejectProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), counter);
			operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
		} catch (BestXException exc) {
			operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("BVMarketSendQuoteRejectError.0") + exc.getMessage()), ErrorState.class);
		}
	}
}
