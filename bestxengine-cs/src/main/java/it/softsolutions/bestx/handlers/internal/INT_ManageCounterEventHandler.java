/**
 * 
 */
package it.softsolutions.bestx.handlers.internal;

import org.apache.commons.lang3.time.DateUtils;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.internal.deprecated.INT_ExecutedState;
import it.softsolutions.bestx.states.internal.deprecated.INT_RejectedState;


/**
 * @author Stefano
 *
 */
public class INT_ManageCounterEventHandler extends CSBaseOperationEventHandler {

    private static final long serialVersionUID = -3943473644128146447L;
    
    private final MarketBuySideConnection cmfConnection;
	
	/**
	 * @param operation
	 */
	public INT_ManageCounterEventHandler(Operation operation, MarketBuySideConnection cmfConnection) {
		super(operation);
		this.cmfConnection = cmfConnection;
	}
	
	@Override
	public void onNewState(OperationState currentState) {
		Proposal secondBest;
		if(operation.getLastAttempt().getSortedBook().getValidSideProposals(operation.getOrder().getSide()).size()<=1) {
			secondBest = null;
		} else {
			secondBest = operation.getLastAttempt().getSortedBook().getValidSideProposals(operation.getOrder().getSide()).get(1);
		}
		boolean settlDateCompatible = DateUtils.isSameDay(operation.getOrder().getFutSettDate(), operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getFutSettDate());
		
		boolean isBetterThanLimit = true;
		if (operation.getOrder().getSide() == OrderSide.BUY) {
			isBetterThanLimit = operation.getOrder().getLimit() == null ||	operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount().compareTo(operation.getOrder().getLimit().getAmount()) <= 0;
		} else {
			isBetterThanLimit = operation.getOrder().getLimit() == null ||	operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount().compareTo(operation.getOrder().getLimit().getAmount()) >= 0;
		}
		boolean isBetterThanSecondBest = true;
		if(secondBest != null) {
			if(operation.getOrder().getSide() == OrderSide.SELL){
				isBetterThanSecondBest = operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount().compareTo(secondBest.getPrice().getAmount()) >= 0;
			} else {
				isBetterThanSecondBest = operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount().compareTo(secondBest.getPrice().getAmount()) <= 0;
			}
		}
		if (settlDateCompatible && isBetterThanLimit && isBetterThanSecondBest) {
			try {
				cmfConnection.acceptProposal(operation, operation.getLastAttempt().getSortedBook().getInstrument(), operation.getLastAttempt().getExecutablePrice(0).getClassifiedProposal());
			} catch (BestXException exc) {
				operation.setStateResilient(new WarningState(currentState, exc, Messages.getString("INTMarketQuoteAcceptError.0")), ErrorState.class);
			}
		}
		else {
			operation.setStateResilient(new INT_RejectedState(), ErrorState.class); 
		}
		
	}
	
	@Override
	public void onTradingConsoleOrderPendingAccepted(TradingConsoleConnection source) {
		operation.setStateResilient(new INT_ExecutedState(), ErrorState.class);
	}
	
	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.DefaultOperationEventHandler#onTradingConsoleTradeAcknowledged(it.softsolutions.bestx.connections.TradingConsoleConnection)
	 */
	@Override
	public void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
		operation.setStateResilient(new INT_ExecutedState(), ErrorState.class);
	}

	@Override
	public void onTradingConsoleOrderPendingRejected(TradingConsoleConnection source, String reason) {
		operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
	}
	
	@Override
	public void onTradingConsoleOrderPendingExpired(TradingConsoleConnection source) {
		operation.setStateResilient(new INT_RejectedState(), ErrorState.class);
	}
}
