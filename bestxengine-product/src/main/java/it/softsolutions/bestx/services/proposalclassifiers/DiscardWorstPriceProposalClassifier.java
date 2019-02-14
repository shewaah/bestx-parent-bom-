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

import it.softsolutions.bestx.BestXException;
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
import it.softsolutions.bestx.services.TitoliIncrociabiliService;

/**
 * @author otc-go
 * 
 */
public class DiscardWorstPriceProposalClassifier implements ProposalClassifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardWorstPriceProposalClassifier.class);
	private TitoliIncrociabiliService titoliIncrociabiliService;
	private boolean matchByQueryEnabled;

	public static String REJECT_REASON = Messages.getString("BestBook.22");

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		boolean isAMatchOrder = false;
		try {
			/*
			 * if NOT enabled from config we don't go for the old way : we check if we have already done the call to
			 * isAMatch, if so we won't do it again making the query only once.
			 */
	        // FIXME AMC 20160824 l'uso del servizio titoliIncrociabiliService dovrebbe essere legato a un flag o all'essere tale servizio non nullo
	        // CS non usa e non ha mai usato il match tra ordini
	 			if (!isMatchByQueryEnabled()) {
				if (order.getIsMatchingFromQuery() == Order.IsMatchingFromQueryStates.NONE) {
					isAMatchOrder = titoliIncrociabiliService.isAMatch(order);
					order.setIsMatchingFromQuery(isAMatchOrder ? Order.IsMatchingFromQueryStates.TRUE : Order.IsMatchingFromQueryStates.FALSE);
				} else {
					isAMatchOrder = order.getIsMatchingFromQuery() == Order.IsMatchingFromQueryStates.TRUE ? true : false;
				}
			} else {
				/*
				 * MatchByQuery is enabled, so we work as we've always done : do the call to isAMatch every time hence
				 * always doing the query
				 */
				isAMatchOrder = titoliIncrociabiliService.isAMatch(order);
			}
		} catch (BestXException ex) {
			LOGGER.warn("Unable to find matching informations - order is set not matchable", ex);
		}

		if (order.isMatchingOrder() || !isAMatchOrder) {
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

	public TitoliIncrociabiliService getTitoliIncrociabiliService() {
		return titoliIncrociabiliService;
	}

	public void setTitoliIncrociabiliService(TitoliIncrociabiliService titoliIncrociabiliService) {
		this.titoliIncrociabiliService = titoliIncrociabiliService;
	}

	public boolean isMatchByQueryEnabled() {
		return matchByQueryEnabled;
	}

	public void setMatchByQueryEnabled(boolean matchByQueryEnabled) {
		this.matchByQueryEnabled = matchByQueryEnabled;
	}
}
