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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**  

 *
 * Purpose: this class is a book classifier.
 * 			It compares the incoming proposal againts the best price in the book, 
 *          and discards it (REJECTED, Messages.getString("BestBook.21")) if the %spread exceeds the configured amount
 *			The amount is retrieved from the CustomerAttributes            
 *
 * Project Name : bestxengine-akros 
 * First created by: paolo.midali 
 * Creation date: 21/mag/2012 
 * 
 * 
 * 
 **/
public class DiscardTooWideQuoteSpread implements ProposalClassifier {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardTooWideQuoteSpread.class);

	private double maxQuoteSpreadPercent = 0.0;	// 0 --> disabled

	@Override
	public ClassifiedProposal getClassifiedProposal(
			ClassifiedProposal proposal, OrderSide orderSide, BigDecimal qty,
			Date futSettDate, List<Attempt> previousAttempts, Set<Venue> venues) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ClassifiedProposal getClassifiedProposal(
			ClassifiedProposal proposal, Order order,
			List<Attempt> previousAttempts, Set<Venue> venues) {
		throw new UnsupportedOperationException();
	}

	@Override
	/* 
	 * [DR20120525] Questo metodo non ha nulla del classifiedProposal, di fatto è un bookClassifier che lavora sul classifiedBook passato come parametro.
	 * La ClassifiedProposal restituita è sempre identica alla classifiedProposal passata come parametro.
	 * TODO rivedere la logica ed eventualmente decidere di creare dei BookClassifier
	 */
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		LOGGER.debug("{}", proposal);
		
		// [DR20120529] Prevent nullPointerExceptions checking the essential mandatory params 
		if (proposal == null || proposal.getProposalState() == null || proposal.getSide() == null || proposal.getPrice() == null) {
			LOGGER.error("{}", proposal);
			throw new IllegalArgumentException("classifiedProposal's fields proposalState, price and side and can't be null");
		}
		
		if (order == null || order.getCustomer() == null || order.getCustomer().getCustomerAttributes() == null) {
			LOGGER.error("{}", order);
			throw new IllegalArgumentException("order's fields customer and customerAttributes and can't be null");
		}
		
		if (book == null) {
			LOGGER.error("{}", book);
			throw new IllegalArgumentException("classifiedBook can't be null");
		}
		// check end

		if ( (proposal.getProposalState() == ProposalState.DROPPED) || (proposal.getProposalState() == ProposalState.REJECTED) ) {
			LOGGER.debug("DiscardTooWideQuoteSpread stop (proposal already " + proposal.getProposalState() + ")");
			return proposal;
		}

		// retrieve configuration value from customer attributes
		CustomerAttributes custAttr = (CustomerAttributes) order.getCustomer().getCustomerAttributes();
		if (custAttr.getWideQuoteSpread() == null) {
			LOGGER.debug("DiscardTooWideQuoteSpread: wideQuoteSpread parameter not set, setting to 0 (default)");
			maxQuoteSpreadPercent = 0.0;
		} else {
			maxQuoteSpreadPercent = custAttr.getWideQuoteSpread().doubleValue();
		}
		
		if (maxQuoteSpreadPercent == 0.0) {
			LOGGER.debug("DiscardTooWideQuoteSpread stop (disabled)");
			return proposal;
		}

		// get proposals to compare against
		Collection<? extends ClassifiedProposal> listAgainstValidate;
		listAgainstValidate = (proposal.getSide() == ProposalSide.ASK) ? book.getAskProposals(): book.getBidProposals();

		classifyProposal(proposal, maxQuoteSpreadPercent, listAgainstValidate);

		return proposal;
	}

	private void classifyProposal(ClassifiedProposal classifiedProposal, Double spreadPercent, Collection<? extends ClassifiedProposal> bookProposals) {
		BigDecimal bestPrice = new BigDecimal(0.0);
		BigDecimal zeroPrice = new BigDecimal(0.0);
		
		if (bookProposals == null) {
			LOGGER.error("{}", bookProposals);
			throw new IllegalArgumentException("classifiedBook's proposals can't be null");
		}

		// first cycle: retrieve the bestPrice
		for (ClassifiedProposal bookProposal : bookProposals) {
			
			if (bookProposal == null || bookProposal.getProposalState() == null || bookProposal.getSide() == null || bookProposal.getPrice() == null) {
				LOGGER.error("{}", bookProposal);
				throw new IllegalArgumentException("classifiedProposal's fields proposalState, price and side and can't be null");
			}

			switch (bookProposal.getProposalState()) {
			case DROPPED:
			case REJECTED:
         case ACCEPTABLE:
				// doNothing
				break;
			default:
			{
				// perform classification
				BigDecimal price = bookProposal.getPrice().getAmount();

				switch (bookProposal.getSide()) {
				case BID:
					bestPrice = price.compareTo(bestPrice) > 0 ? price : bestPrice;
					break;
				case ASK:
					if (bestPrice.equals(zeroPrice)) {
						bestPrice = price;
					}
					else {
						bestPrice = (price.doubleValue() > 0.0 && price.compareTo(bestPrice) < 0) ? price : bestPrice;
					}
					break;
				default:
					break;
				}
			}
			break;
			}
		}

		// Second cycle: discard the proposal exceeding the maximum spread
		if (bestPrice.doubleValue() > 0.0) {
			// 2) if proposal exceeds deviation discard it
			double delta = Math.abs(bestPrice.doubleValue() - classifiedProposal.getPrice().getAmount().doubleValue());
			double deltaPerc = delta / bestPrice.doubleValue() * 100.0;
			LOGGER.trace("proposal's price = {}, retrieved bestPrice = {} > delta = {}%", classifiedProposal.getPrice().getAmount(), bestPrice, deltaPerc);
			
			if (deltaPerc > spreadPercent) {
				classifiedProposal.setProposalState(Proposal.ProposalState.ACCEPTABLE);
				classifiedProposal.setReason(Messages.getString("BestBook.21", deltaPerc, spreadPercent));
				classifiedProposal.setProposalSubState(ProposalSubState.OUTSIDE_SPREAD);
			}
		} else {
			LOGGER.debug("DiscardTooWideQuoteSpread stop (no best price available)");
		}
	} 
}
