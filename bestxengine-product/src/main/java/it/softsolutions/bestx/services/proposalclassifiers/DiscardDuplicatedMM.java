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
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class used to configure a price validation discarding prices coming from dealers who do have better prices or equal prices also in markets with better ranking 
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class DiscardDuplicatedMM implements ProposalClassifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscardDuplicatedMM.class);

    @Override
    public ClassifiedProposal getClassifiedProposal(ClassifiedProposal proposal, Order order, List<Attempt> previousAttempts, Set<Venue> venues, ClassifiedBook book) {
        if (proposal.getVenue().getVenueType() == VenueType.MARKET) {
            return proposal;
        }

        Collection<? extends ClassifiedProposal> listAgainstValidate;
        listAgainstValidate = (proposal.getSide() == ProposalSide.ASK) ? book.getAskProposals() : book.getBidProposals();
        if (LOGGER.isDebugEnabled()) {
        	StringBuilder sb = new StringBuilder();
            sb.append("Validating proposal: ").append(proposal);
            Venue proposalVenue = proposal.getVenue();
            MarketMaker proposalMM = null;
            if (proposalVenue != null) {
            	sb.append(" - Venue=").append(proposalVenue).append(", venueType=").append(proposalVenue.getVenueType());
                proposalMM = proposal.getVenue().getMarketMaker();
            }
            Market proposalMkt = proposal.getMarket();
            if (proposalMkt != null) {
                sb.append(" - Market=").append(proposalMkt).append(", ranking=").append(proposalMkt.getRanking().intValue());
            }
            if (proposalMM != null) {
            	sb.append(" - MarketMaker=").append(proposalMM.getCode());
            }
            LOGGER.debug(sb.toString());
        }
        for (Proposal prop : listAgainstValidate) {
            //[RR20121030] There was a check for prop not being a market in the if where we decide to reject the proposal.
            //There is no need to check market proposals against the one being classified (which is never a market proposal),
            //skip those kind of proposals
            if (prop.getVenue().getVenueType() == VenueType.MARKET) {
                continue;
            }
            
            if (LOGGER.isDebugEnabled()) {
            	LOGGER.debug("Venue " + (prop.getVenue() != null ? prop.getVenue().getCode() : "") + " type : "
                        + ((prop.getVenue() != null && prop.getVenue().getVenueType() != null) ? prop.getVenue().getVenueType().name() : ""));
            	
            	StringBuilder sb = new StringBuilder();
                sb.append("Comparing with proposal: ").append(prop);

                Venue propVenue = prop.getVenue();
                MarketMaker propMM = null;
                if (propVenue != null) {
                	sb.append(" - Venue=").append(propVenue).append(", venueType=").append(propVenue.getVenueType());
                    propMM = prop.getVenue().getMarketMaker();
                }
                Market propMkt = prop.getMarket();
                if (propMkt != null) {
                    sb.append(" - Market=").append(propMkt).append(", ranking=").append(propMkt.getRanking().intValue());
                }
                if (propMM != null) {
                	sb.append(" - MarketMaker=").append(propMM.getCode());
                }
                LOGGER.debug(sb.toString());
            }

            Market proposalMarket = proposal.getMarket();
            int proposalMarketRanking = proposalMarket.getRanking();
            Market loopProposalMarket = prop.getMarket();
            int loopProposalMarketRanking = loopProposalMarket.getRanking();
            // Same market maker, different market -> keep the one with the better ranking market or better price
            ClassifiedProposal classProp = (ClassifiedProposal) prop;
            if ((classProp.getProposalState() == ProposalState.VALID || classProp.getProposalState() == ProposalState.NEW)
                    && proposal.getVenue().getMarketMaker().getCode().equalsIgnoreCase(classProp.getVenue().getMarketMaker().getCode())
                    && !loopProposalMarket.getMarketCode().equals(proposalMarket.getMarketCode())) {
                //[RR20121030] BXMNT-193 : sometimes happens that two markets have the same ranking and both proposals for the same market maker
                // will be accepted. Add a reason to show the cause.
                if (proposalMarketRanking == loopProposalMarketRanking && isFirstBetterThanSecond(classProp.getPrice().getAmount(), proposal.getPrice().getAmount(), proposal.getSide())){
                    LOGGER.info("Markets {} and {} have the same ranking: {}=={}", proposalMarket, loopProposalMarket, proposalMarketRanking, loopProposalMarketRanking);
                    proposal.setReason(Messages.getString("ProposalMarketSameRanking", proposalMarket.getMarketCode(), loopProposalMarket.getMarketCode(), proposalMarketRanking));
                } else if (isFirstBetterThanSecond(classProp.getPrice().getAmount(), proposal.getPrice().getAmount(), proposal.getSide())){
                	//[RR20150409] BXMNT-370 reject only if the better ranking proposal has also a better or equal price
                    LOGGER.info("Rejecting proposal: {} versus other proposal: {}", proposal, classProp);
                    proposal.setProposalState(Proposal.ProposalState.REJECTED);
                    proposal.setReason(Messages.getString("BestBook.23"));
                    break;
                } else if(proposal.getPrice().getAmount().compareTo(classProp.getPrice().getAmount()) == 0 &&
                		proposalMarketRanking > loopProposalMarketRanking) {
                    LOGGER.info("Rejecting proposal: {} versus other proposal: {}", proposal, classProp);
                    proposal.setProposalState(Proposal.ProposalState.REJECTED);
                    proposal.setReason(Messages.getString("BestBook.16"));
                    break;
                }
                else {
                    LOGGER.debug("Do not reject proposal {}", proposal);                	
                }
            }
        }

        return proposal;
    }

    private boolean isFirstBetterThanSecond(BigDecimal firstAmount, BigDecimal secondAmount, ProposalSide side) {
    	if(ProposalSide.ASK == side) {
    		return firstAmount.compareTo(secondAmount) < 0;
    	} 
    	if(ProposalSide.BID == side) {
    		return firstAmount.compareTo(secondAmount) > 0;
    	} 
		return false;
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
