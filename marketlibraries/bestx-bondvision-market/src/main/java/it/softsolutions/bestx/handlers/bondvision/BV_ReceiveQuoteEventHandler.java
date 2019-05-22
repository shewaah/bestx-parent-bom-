/**
 * 
 */
package it.softsolutions.bestx.handlers.bondvision;

import java.math.BigDecimal;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bondvision.BV_AcceptQuoteState;
import it.softsolutions.bestx.states.bondvision.BV_RejectQuoteState;

/**
 * @author Stefano
 *
 */
public class BV_ReceiveQuoteEventHandler extends BV_ManagingRfqEventHandler {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param operation
	 */
	public BV_ReceiveQuoteEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onNewState(OperationState currentState) {
		BigDecimal targetPrice = null;
		if (operation.getLastAttempt().getMarketOrder().getLimit() != null) {
			targetPrice = operation.getLastAttempt().getMarketOrder().getLimit().getAmount(); 
		}
		ClassifiedProposal counter = operation.getLastAttempt().getExecutablePrice(Attempt.BEST).getClassifiedProposal();
		if (counter == null) {
			operation.setStateResilient(new WarningState(operation.getState(), null, "Counter is null! How is this possible?"), ErrorState.class);
			return;
		}
		if (operation.getOrder().getQty().compareTo(counter.getQty()) <= 0 &&
				operation.getOrder().getFutSettDate().compareTo(counter.getFutSettDate()) == 0 &&
				(operation.getOrder().getLimit() == null || 
						((operation.getOrder().getSide() == OrderSide.BUY && 
								counter.getPrice().getAmount().compareTo(operation.getOrder().getLimit().getAmount()) <= 0) ||
						(operation.getOrder().getSide() == OrderSide.SELL && 
								counter.getPrice().getAmount().compareTo(operation.getOrder().getLimit().getAmount()) >= 0))) &&
				(targetPrice == null ||  
						(operation.getOrder().getSide() == OrderSide.BUY && 
								counter.getPrice().getAmount().compareTo(targetPrice) <= 0) ||
						(operation.getOrder().getSide() == OrderSide.SELL && 
								counter.getPrice().getAmount().compareTo(targetPrice) >= 0))) {

			operation.setStateResilient(new BV_AcceptQuoteState(), ErrorState.class);
		} else {
			if (operation.getOrder().getQty().compareTo(counter.getQty()) > 0) { 
				counter.setProposalState(Proposal.ProposalState.REJECTED);
				counter.setReason(Messages.getString("DiscardInsufficientQuantityProposalClassifier.0"));
			} else if (operation.getOrder().getFutSettDate().compareTo(counter.getFutSettDate()) != 0) {
				counter.setProposalState(Proposal.ProposalState.REJECTED);
				counter.setReason(Messages.getString("DiscardSettlementDateProposalClassifier.0"));
			}
			operation.setStateResilient(new BV_RejectQuoteState(OperationState.Type.RejectQuote, MarketCode.BV), ErrorState.class);
		}
	}
}
