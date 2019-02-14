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
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;

/**
 * @author otc-go
 * 
 */
public class SubstituteCounterProposalClassifier implements ProposalClassifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(SubstituteCounterProposalClassifier.class);

	public boolean isIn(ProposalState val, ProposalState...values) {
		for(ProposalState a : values) {
			if(val == a) return true;
		}
		return false;
	}

	// This special ProposalClassifier substitute the current proposal with a counter
	// arrived previously on the same market and marketmaker
	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {

		ClassifiedProposal result = proposal;
		for (Attempt attempt : previousAttempts) {
			if(attempt.getExecutablePrices() == null  || attempt.getExecutablePrices().size() <= 0) continue;
			for(int i = 0; i < attempt.getExecutablePrices().size(); i++) {
				if(attempt.getExecutablePrice(i) == null || attempt.getExecutablePrice(i).getClassifiedProposal() == null
							|| isIn(attempt.getExecutablePrice(i).getClassifiedProposal().getProposalState(), ProposalState.DROPPED, ProposalState.REJECTED)) continue;
				ClassifiedProposal counterOffer = attempt.getExecutablePrice(i).getClassifiedProposal();
				if (counterOffer != null && counterOffer.getMarket() != null && counterOffer.getMarketMarketMaker() != null
						&& counterOffer.getMarket().getMarketCode() == proposal.getMarket().getMarketCode()
						&& counterOffer.getMarketMarketMaker().getMarketSpecificCode().equalsIgnoreCase(proposal.getMarketMarketMaker().getMarketSpecificCode())
						&& counterOffer.getSide() == proposal.getSide()) {

					result = (ClassifiedProposal) attempt.getExecutablePrice(i).getClassifiedProposal();
					result.setCommissionType(CommissionType.AMOUNT);
					LOGGER.debug("Counter offer is " + result);
					if (result.getPrice() == null || result.getQty().compareTo(BigDecimal.ZERO) <= 0) {
						LOGGER.info("Counter offer with null price or zero quantity, go on with the loop.");
						continue;
					}
					String currency = result.getPrice().getStringCurrency();
					result.setCommission(new Money(currency, BigDecimal.ZERO));
					result.setCustomerAdditionalExpenses(new Money(currency, BigDecimal.ZERO));
					result.setPriceTelQuel(result.getPrice());
					if (result.getProposalState() == null || result.getProposalState() == Proposal.ProposalState.NEW) {
						result.setProposalState(Proposal.ProposalState.VALID);
					}

					if (order.getLimit() != null
							&& (order.getSide().equals(OrderSide.BUY) && result.getPrice().getAmount().compareTo(order.getLimit().getAmount()) > 0 || order.getSide().equals(OrderSide.SELL)
									&& result.getPrice().getAmount().compareTo(order.getLimit().getAmount()) < 0)) {
						result.setProposalState(Proposal.ProposalState.REJECTED);
						result.setProposalSubState(ProposalSubState.PRICE_WORST_THAN_LIMIT);
						result.setReason(Messages.getString("BestBook.19"));
					}
				return result;
				}
			}
		}
		return result;
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
