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
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class DiscardMarketTriedInEarlierAttemptProposalClassifier implements ProposalClassifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardMarketTriedInEarlierAttemptProposalClassifier.class);

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		if (proposal.getVenue() != null && previousAttempts != null && !previousAttempts.isEmpty()) {
			LOGGER.debug("Order {}, check markets used in previous attempts : {}", order.getFixOrderId(), proposal);
			for (Attempt a : previousAttempts) {
				// Market used in an earlier attempt
				if (a.getMarketOrder() != null && a.getMarketOrder().getMarket().equals(proposal.getMarket())) {
					LOGGER.debug("Order {}, proposal {}, already tried market {}", order.getFixOrderId(), proposal.getMarket(), proposal.getMarket());
					List<MarketExecutionReport> execReports = a.getMarketExecutionReports();
					// The earlier attempt has a market execution report stating that a technical or business failure prevented
					// fill
					if (execReports != null && execReports.size() != 0) {
						LOGGER.debug("Order {}, check the execution reports", order.getFixOrderId());
						proposal.setProposalState(Proposal.ProposalState.REJECTED);
						proposal.setReason(Messages.getString("DiscardMarketTriedInEarlierAttemptProposalClassifier.0"));
					} else {
						// there are no market execution reports at all
						LOGGER.debug("Order {}, no execution reports, check the if the market allows to reuse prices.", order.getFixOrderId());
						if (proposal.getMarket() != null && a.getMarketOrder().getMarket().equals(proposal.getMarket()) && !proposal.getMarket().isReusePrices()) {
							proposal.setProposalState(Proposal.ProposalState.REJECTED);
							proposal.setReason(Messages.getString("DiscardMarketTriedInEarlierAttemptProposalClassifier.0"));
						}
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
}