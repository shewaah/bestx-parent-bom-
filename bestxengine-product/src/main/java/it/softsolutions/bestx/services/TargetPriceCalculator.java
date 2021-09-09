package it.softsolutions.bestx.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.jsscommon.Money;


/**
 * @author anna.cochetti
 * This class calculates the target price depending on: the book depth,
 * a spread to widen the best price in case the book depth is not enough (customerMaxWideSpread)
 * (WIP documentation)
 */
public class TargetPriceCalculator {

	private static final Logger LOGGER = LoggerFactory.getLogger(TargetPriceCalculator.class);

	private int targetPriceMaxLevel;
	private String acceptableSubstates;
	private int decimalDigitdRounding=5;

	public int getDecimalDigitdRounding() {
		return decimalDigitdRounding;
	}

	public void setDecimalDigitdRounding(int decimalDigitdRounding) {
		this.decimalDigitdRounding = decimalDigitdRounding;
	}

	/**
	 * @param order          client order
	 * @param currentAttempt for which the target price needs to be calculated.
	 *                       Contains the sorted book, the execution proposal, the
	 *                       market order.
	 * @return
	 */
	public Money calculateTargetPrice(Operation operation) {
		Money limitPrice = null;
		Money ithBest = null;
		ClassifiedProposal ithBestProp = null;
		Money best = null;
		Attempt currentAttempt = operation.getLastAttempt();
		List<ProposalSubState> wantedSubStates = loadConfiguredSubstates();

		try {
			best = currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()).getPrice();
			ithBestProp = BookHelper.getIthProposal(currentAttempt.getSortedBook().getAcceptableProposalBySubState(
					wantedSubStates, operation.getOrder().getSide()), this.targetPriceMaxLevel);
			ithBest = ithBestProp.getPrice();
		} catch (NullPointerException e) {
			LOGGER.info("NullPointerException trying to manage widen best or get the {}-th best for order {}",
					this.targetPriceMaxLevel, operation.getOrder().getFixOrderId());
			LOGGER.debug("NullPointerException trace", e);
			LOGGER.debug("currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()) = {}",
					currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()), e);
		}
		try {
			double spread = BookHelper.getQuoteSpread(currentAttempt.getSortedBook().getAcceptableProposalBySubState(
					wantedSubStates, operation.getOrder().getSide()), this.targetPriceMaxLevel);
			CustomerAttributes custAttr = (CustomerAttributes) operation.getOrder().getCustomer()
					.getCustomerAttributes();
			BigDecimal customerMaxWideSpread = custAttr.getWideQuoteSpread();
			if (customerMaxWideSpread != null && customerMaxWideSpread.doubleValue() < spread) { // must use the spread,
																									// not the i-th best
				limitPrice = BookHelper.widen(best, customerMaxWideSpread, operation.getOrder().getSide(),
						operation.getOrder().getLimit() == null ? null : operation.getOrder().getLimit().getAmount());
				LOGGER.info(
						"Order {}: widening market order limit price {}. Max wide spread is {} and spread between best {} and i-th best {} has been calculated as {}",
						operation.getOrder().getFixOrderId(),
						limitPrice == null ? " N/A" : limitPrice.getAmount().toString(),
						customerMaxWideSpread == null ? " N/A" : customerMaxWideSpread.toString(),
						best == null ? " N/A" : best.getAmount().toString(),
						ithBest == null ? " N/A" : ithBest.getAmount().toString(), spread);
			} else {// use i-th best
				limitPrice = ithBest;
			}
			if (limitPrice == null) { // necessary to avoid null limit price. See the book depth minimum for execution
				if (currentAttempt.getExecutionProposal() != null
						&& currentAttempt.getExecutionProposal().getWorstPriceUsed() != null) {
					limitPrice = currentAttempt.getExecutionProposal().getWorstPriceUsed();
					LOGGER.debug("Use worst price of consolidated proposal as market order limit price: {}",
							limitPrice == null ? "null" : limitPrice.getAmount().toString());
				} else {
					limitPrice = currentAttempt.getExecutionProposal() == null ? null
							: currentAttempt.getExecutionProposal().getPrice();
					LOGGER.debug("No i-th best - Use proposal as market order limit price: {}",
							limitPrice == null ? "null" : limitPrice.getAmount().toString());
				}
			} else {
				if (operation.getOrder().getLimit() != null
						&& isWorseThan(limitPrice, operation.getOrder().getLimit(), operation.getOrder().getSide())) {
					LOGGER.debug(
							"Found price is {}, which is worse than client order limit price: {}. Will use client order limit price",
							limitPrice.getAmount().toString(), operation.getOrder().getLimit().getAmount().toString());
					limitPrice = operation.getOrder().getLimit();
				} else
					LOGGER.debug(
							"Use less wide between i-th best proposal and best widened by {} as market order limit price: {}",
							customerMaxWideSpread, limitPrice == null ? "null" : limitPrice.getAmount().toString());
			}
		} catch (Exception e) {
			LOGGER.warn("Problem appeared while calculating target price", e);
		}
		if(limitPrice != null) {
			if(limitPrice.getStringCurrency() != null) {
				limitPrice = new Money(limitPrice.getStringCurrency(), MarketOrder.beautifyBigDecimal(limitPrice.getAmount(), 1, decimalDigitdRounding));
			} else {
				if (limitPrice.getCurrency() != null) {
					limitPrice = new Money(limitPrice.getCurrency(), MarketOrder.beautifyBigDecimal(limitPrice.getAmount(), 1, decimalDigitdRounding));
				}
			}
		}
		return limitPrice;
	}

	private boolean isWorseThan(Money p1, Money p2, OrderSide side) {
		if (side == null) {
			return false;
		} else if (side == OrderSide.BUY) {
			return p1.compareTo(p2) > 0;
		} else {
			return p1.compareTo(p2) < 0;
		}
	}

	private List<ProposalSubState> loadConfiguredSubstates() {
		String[] substateList = this.acceptableSubstates.split(",");
		List<ProposalSubState> wantedSubStates = new ArrayList<>();

		for (String substate : substateList) {
			wantedSubStates.add(ProposalSubState.valueOf(substate));
		}
		return wantedSubStates;
	}

	public int getTargetPriceMaxLevel() {
		return targetPriceMaxLevel;
	}

	public void setTargetPriceMaxLevel(int targetPriceMaxLevel) {
		this.targetPriceMaxLevel = targetPriceMaxLevel;
	}

	public String getAcceptableSubstates() {
		return acceptableSubstates;
	}

	public void setAcceptableSubstates(String acceptableSubstates) {
		this.acceptableSubstates = acceptableSubstates;
	}

}
