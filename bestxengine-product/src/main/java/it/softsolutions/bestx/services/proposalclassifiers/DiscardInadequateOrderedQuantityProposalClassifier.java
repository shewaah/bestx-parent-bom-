/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */

package it.softsolutions.bestx.services.proposalclassifiers;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.MarketSecurityStatusService;
import it.softsolutions.bestx.services.MarketSecurityStatusService.QuantityValues;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class DiscardInadequateOrderedQuantityProposalClassifier implements ProposalClassifier {
	private MarketSecurityStatusService marketSecurityStatusService;
	private InstrumentFinder instrumentFinder;

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardInadequateOrderedQuantityProposalClassifier.class);

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		String isin = order.getInstrument().getIsin();
		Instrument instr = null;
		MarketCode marketCode = proposal.getMarket().getMarketCode();

		try {
			instr = instrumentFinder.getInstrumentByIsin(isin);
		} catch (Exception e) {
			LOGGER.error("Error while looking for the instrument " + isin + ", cannot find it!", e);
		}
		if (proposal.getMarket().getMarketCode() == MarketCode.MOT || proposal.getMarket().getMarketCode() == MarketCode.TLX) {
			if (!marketSecurityStatusService.validQuantities(marketCode, instr, order)) {
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setReason(Messages.getString("DiscardInadequateOrderedQuantityProposalClassifier.0"));
				LOGGER.info("Discarding proposal: " + proposal);
			}
		} else if (proposal.getMarket().getMarketCode() == MarketCode.HIMTF || proposal.getMarket().getMarketCode() == MarketCode.BV || proposal.getMarket().getMarketCode() == MarketCode.MTSPRIME) {
			BigDecimal minQty = new BigDecimal(1);
			BigDecimal minIncrement = new BigDecimal(1);
			BigDecimal qtyMultiplier = new BigDecimal(1);
			BigDecimal qtyValsArray[];
			qtyValsArray = this.marketSecurityStatusService.getQuantityValues(proposal.getMarket().getMarketCode(), order.getInstrument());
			minQty = qtyValsArray[QuantityValues.MIN_QTY.getPosition()];
			minIncrement = qtyValsArray[QuantityValues.MIN_INCREMENT.getPosition()];
			qtyMultiplier = qtyValsArray[QuantityValues.QTY_MULTIPLIER.getPosition()];
			if (qtyMultiplier == null) {
				LOGGER.error("QUantity multiplier for instrument " + order.getInstrument().getIsin() + " is null");
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setReason("Quantity multiplier for instrument " + order.getInstrument().getIsin() + " is null");
				LOGGER.info("Discarding proposal: " + proposal);
				return proposal;
			}
			if (order.getQty() == null) {
				LOGGER.error("Quantity for order " + order.getFixOrderId() + " is null");
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setReason("Quantity for order " + order.getFixOrderId() + " is null");
				LOGGER.info("Discarding proposal: " + proposal);
				return proposal;
			}
			if (minQty == null) {
				LOGGER.error("Minimum quantity for instrument " + order.getInstrument().getIsin() + " is null");
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setReason("Minimum quantity for instrument " + order.getInstrument().getIsin() + " is null");
				LOGGER.info("Discarding proposal: " + proposal);
				return proposal;
			}
			if (minIncrement == null) {
				LOGGER.error("Minimum increment for instrument " + order.getInstrument().getIsin() + " is null");
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setReason("Minimum increment for instrument " + order.getInstrument().getIsin() + " is null");
				return proposal;
			}

			if (proposal.getMarket().getMarketCode() == MarketCode.BV || proposal.getMarket().getMarketCode() == MarketCode.MTSPRIME) {
				if (order.getQty().compareTo(minQty.multiply(qtyMultiplier)) < 0 || order.getQty().doubleValue() % minIncrement.multiply(qtyMultiplier).doubleValue() > 0) {
					proposal.setProposalState(Proposal.ProposalState.REJECTED);
					proposal.setReason(Messages.getString("DiscardInadequateOrderedQuantityProposalClassifier.0"));
					LOGGER.info("Discarding proposal: " + proposal);
				}
			} else {
				if (minIncrement.compareTo(BigDecimal.ZERO) == 0 || qtyMultiplier.compareTo(BigDecimal.ZERO) == 0) {
					proposal.setProposalState(Proposal.ProposalState.VALID);
					LOGGER.info("Valid proposal: " + proposal + " due to minIncrement = " + minIncrement + " and QtyMultiplier = " + qtyMultiplier);
					return proposal;

				} else {
					BigDecimal[] decimalQtyz = order.getQty().divideAndRemainder(minIncrement);
					if (order.getQty().divide(qtyMultiplier).compareTo(minQty) < 0 || decimalQtyz[1].compareTo(BigDecimal.ZERO) != 0) {
						proposal.setProposalState(Proposal.ProposalState.REJECTED);
						proposal.setReason(Messages.getString("DiscardInadequateOrderedQuantityProposalClassifier.0"));
					}
				}

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

	/**
	 * Sets the market security status service.
	 * 
	 * @param marketSecurityStatusService
	 *            the new market security status service
	 */
	public void setMarketSecurityStatusService(MarketSecurityStatusService marketSecurityStatusService) {
		this.marketSecurityStatusService = marketSecurityStatusService;
	}

	/**
	 * Sets the instrument finder.
	 * 
	 * @param instrumentFinder
	 *            the new instrument finder
	 */
	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}
}
