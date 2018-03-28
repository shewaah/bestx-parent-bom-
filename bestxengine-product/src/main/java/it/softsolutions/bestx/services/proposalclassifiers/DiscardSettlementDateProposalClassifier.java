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
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Venue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose:
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 05/ott/2012
 * 
 **/
public class DiscardSettlementDateProposalClassifier implements ProposalClassifier {
	private static final Logger LOGGER = LoggerFactory.getLogger(DiscardSettlementDateProposalClassifier.class);

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		if (proposal.getFutSettDate() != null && !DateUtils.isSameDay(order.getFutSettDate(), proposal.getFutSettDate()) && !proposal.isNonStandardSettlementDateAllowed()) {
			proposal.setProposalState(Proposal.ProposalState.REJECTED);
			LOGGER.error("Order settlement date: " + order.getFutSettDate().toString() + " - Proposal settlement date: " + proposal.getFutSettDate().toString());
			proposal.setReason(Messages.getString("DiscardSettlementDateProposalClassifier.0"));
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
