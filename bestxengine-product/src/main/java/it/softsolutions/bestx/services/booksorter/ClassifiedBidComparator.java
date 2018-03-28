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

package it.softsolutions.bestx.services.booksorter;

import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue.VenueType;

/**  
*
* Purpose: comparator to sort bid side ClassifiedProposals in a Book
*
* Project Name : bestxengine-product 
* First created by: stefano 
* Creation date: 19-ott-2012 
* 
**/
public class ClassifiedBidComparator implements Comparator<ClassifiedProposal> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClassifiedBidComparator.class);

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(ClassifiedProposal o1, ClassifiedProposal o2) {
		int result = 0;

		if(ProposalState.VALID.equals(o1.getProposalState()) && ProposalState.VALID.equals(o2.getProposalState())) {
			result = o1.getProposalSubState().compareTo(o2.getProposalSubState());
		}
		
		//Order by price
		if (result == 0) { 
			result = o2.getPrice().getAmount().compareTo(o1.getPrice().getAmount());
		}

		// counter offers have best rank in book
		if(result == 0 && (o1.getType() == ProposalType.COUNTER || o1.getType() == ProposalType.COUNTER)) {
			result = o1.getType().compareTo(o2.getType());
		}
		
		// order by market rank
		if (result == 0) {
			if (o2.getMarket().getRanking() < o1.getMarket().getRanking()) {
				result = 1;
			} else if (o2.getMarket().getRanking() > o1.getMarket().getRanking()) {
				result = -1;
			}
		}

		// Order by execution venue rank (former MarketMaker rank)
		if (result==0) {
			if (o1.getVenue().getVenueType() != VenueType.MARKET && o2.getVenue().getVenueType() != VenueType.MARKET) {
				if (o2.getVenue().getMarketMaker().getRank() < o1.getVenue().getMarketMaker().getRank()) {
					result = 1;
				} else if (o2.getVenue().getMarketMaker().getRank() > o1.getVenue().getMarketMaker().getRank()) {
					result = -1;
				}
			}
		}

		// Order by proposal status
		if (result == 0) {
			result = o2.getType().compareTo(o1.getType());
		}

		// youngest proposal has a better rank
		if (result == 0) { 
			if (o2.getTimestamp().after(o1.getTimestamp())) {
				result = 1;
			} else {
				result = -1;
			}
		}
		LOGGER.debug("result={}", result);
		return result;
	}
}
