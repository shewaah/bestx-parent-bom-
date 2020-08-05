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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class DiscardOldProposalClassifier extends BaseMarketMakerClassifier implements ProposalClassifier {

	private int configValue = 12; // default value is 12
	private long millisecsToTake = configValue * 60 * 60 * 1000;

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
		// discard prices older than configured number of hours
		if (!isCompositePriceMarketMaker(proposal)) {
			Date twelveHoursAgo = DateService.newLocalDate();
//			long time = twelveHoursAgo.getTime();
			twelveHoursAgo.setTime(twelveHoursAgo.getTime() - millisecsToTake /* confValue * 60 * 60 *100 */);
//			time = twelveHoursAgo.getTime();
//			long ptime = proposal.getTimestamp().getTime();
			// twelveHoursAgo.setTime(twelveHoursAgo.getTime()-43200000 /* 12 * 60 * 60 *100*/ );
			if (proposal.getTimestamp().before(twelveHoursAgo)) {
				proposal.setProposalState(Proposal.ProposalState.REJECTED);
				proposal.setProposalSubState(ProposalSubState.TOO_OLD);
				proposal.setReason(Messages.getString("DiscardOldProposalClassifier.0", (proposal.getTimestamp() != null ? 
						DateService.format("dd/MM/yyyy", proposal.getTimestamp()) : "")));
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
	 * Sets the config value.
	 * 
	 * @param configValue
	 *            the new config value
	 */
	public void setConfigValue(int configValue) {
		this.configValue = configValue;
		this.millisecsToTake = configValue * 60 * 60 * 1000;
	}

	/**
	 * Gets the config value.
	 * 
	 * @return the config value
	 */
	public int getConfigValue() {
		return this.configValue;
	}
}
