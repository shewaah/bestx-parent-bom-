/**
 * 
 */
package it.softsolutions.bestx.services.proposalclassifiers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**
 * @author otc-go
 * 
 */
public class DiscardWorstPriceProposalClassifier implements ProposalClassifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardWorstPriceProposalClassifier.class);

	public static String REJECT_REASON = Messages.getString("BestBook.22");

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		if (order.isMatchingOrder()) {
			if (order.getLimit() != null
			        && (order.getSide() == OrderSide.BUY && proposal.getSide() == ProposalSide.ASK && proposal.getPrice().getAmount().doubleValue() > order.getLimit().getAmount().doubleValue() || (order
			                .getSide() == OrderSide.SELL && proposal.getSide() == ProposalSide.BID && proposal.getPrice().getAmount().doubleValue() < order.getLimit().getAmount().doubleValue()))) {

				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setProposalSubState(Proposal.ProposalSubState.PRICE_WORST_THAN_LIMIT);
				proposal.setReason(REJECT_REASON);
				LOGGER.debug("Proposal rejected for Order {}, price {}, limit {}", order.getFixOrderId(), proposal.getPrice().getAmount().doubleValue(), order.getLimit().getAmount().doubleValue());
			}
		}
		return proposal;
	}

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, OrderSide orderSide, BigDecimal qty, Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues) {
		throw new UnsupportedOperationException();
	}
}
