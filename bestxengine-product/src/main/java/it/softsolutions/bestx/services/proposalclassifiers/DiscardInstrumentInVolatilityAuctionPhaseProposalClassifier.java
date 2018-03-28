/*
 * Project Name : BestXEngine_Akros
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2009-06-03 07:17:27 $
 * Header       : $Id: DiscardInstrumentInVolatilityAuctionPhaseProposalClassifier.java,v 1.1 2009-06-03 07:17:27 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_Akros/src/it/softsolutions/bestx/services/proposalclassifiers/DiscardInstrumentInVolatilityAuctionPhaseProposalClassifier.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_Akros
 */
package it.softsolutions.bestx.services.proposalclassifiers;

import it.softsolutions.bestx.bestexec.ProposalClassifier;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class DiscardInstrumentInVolatilityAuctionPhaseProposalClassifier implements ProposalClassifier {

	@Override
	public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {

		// if (proposal.getMarket().getMarketCode() == MarketCode.MOT ){
		// if (Market.MarketPhase.VOLATILITY_AUCTION.equals(proposal.getMarket().getCurrentMarketPhase())) {
		// proposal.setProposalState(Proposal.ProposalState.REJECTED);
		// proposal.setReason(Messages.getString("DiscardInsufficientQuantityProposalClassifier.0")); //AMC 20090518
		// decidere il messaggio
		// }
		// }
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
