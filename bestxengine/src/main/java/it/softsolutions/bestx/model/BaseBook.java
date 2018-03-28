/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Proposal.ProposalSide;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class BaseBook implements Cloneable, Book {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBook.class);

    private Instrument instrument;
    
    private Collection<Proposal> askProposals = new ConcurrentSkipListSet<Proposal>();
    private Collection<Proposal> bidProposals = new ConcurrentSkipListSet<Proposal>();

    @Override
    public BaseBook clone() {
        if (askProposals == null) {
            throw new IllegalArgumentException("null ask proposals list");
        }
        if (bidProposals == null) {
            throw new IllegalArgumentException("null bid proposals list");
        }

        BaseBook res = new BaseBook();

        res.setInstrument(instrument);
        res.askProposals.addAll(askProposals);
        res.bidProposals.addAll(bidProposals);

        return res;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    @Override
    public Instrument getInstrument() {
        return instrument;
    }

    public void setAskProposals(Collection<Proposal> askProposals) {
        LOGGER.debug("Setting book ask proposals to {}", askProposals);
        this.askProposals = askProposals;
    }

    @Override
    public Collection<Proposal> getAskProposals() {
        return askProposals;
    }

    public void setBidProposals(Collection<Proposal> bidProposals) {
        LOGGER.debug("Setting book bid proposals to {}", bidProposals);
        this.bidProposals = bidProposals;
    }

    @Override
    public Collection<Proposal> getBidProposals() {
        return bidProposals;
    }

    @Override
    public void addProposal(Proposal proposal) {
        LOGGER.debug("{}", proposal);

        switch (proposal.getSide()) {
        case BID:
            bidProposals.add(proposal);
            break;
        case ASK:
            askProposals.add(proposal);
            break;
        default:
            break;
        }
    }

    public void removeProposal(Proposal proposal) {
        LOGGER.debug("{}", proposal);

        switch (proposal.getSide()) {
        case BID:
            bidProposals.remove(proposal);
            break;
        case ASK:
            askProposals.remove(proposal);
            break;
        default:
            break;
        }
    }

    public void removeProposalFromMarketMakerAndSide(MarketMarketMaker marketMarketMaker, ProposalSide proposalSide) {
        String marketMarketMakerCode = marketMarketMaker.getMarketSpecificCode();
        Collection<Proposal> proposals = null;

        switch (proposalSide) {
        case BID:
            proposals = bidProposals;
            break;
        case ASK:
            proposals = askProposals;
            break;
        default:
            break;
        }

        Proposal deleteThis = null;
        for (Proposal prop : proposals) {
            if (prop.getMarketMarketMaker().getMarketSpecificCode().equals(marketMarketMakerCode)) {
                LOGGER.debug("Market maker {} older proposal {} will be removed from the book for the instrument {}", marketMarketMakerCode, prop, instrument.getIsin());
                deleteThis = prop;
                break;
            }
        }

        if (deleteThis != null) {
            proposals.remove(deleteThis);
        }
    }

    public ClassifiedProposal getAlreadyExistingProposal(MarketMarketMaker marketMarketMaker, ProposalSide proposalSide) {
        String marketMarketMakerCode = marketMarketMaker.getMarketSpecificCode();
        Collection<Proposal> proposals = null;

        switch (proposalSide) {
        case BID:
            proposals = bidProposals;
            break;
        case ASK:
            proposals = askProposals;
            break;
        default:
            break;
        }

        ClassifiedProposal oldProposal = null;
        for (Proposal prop : proposals) {
            if (prop.getMarketMarketMaker().getMarketSpecificCode().equals(marketMarketMakerCode)) {
                LOGGER.debug("Market maker {} proposal available in the book for the instrument {}, proposal found {}", marketMarketMakerCode, instrument.getIsin(), prop);
                oldProposal = (ClassifiedProposal) prop;
                break;
            }
        }

        return oldProposal;
    }

    public boolean proposalExistsForMarketMarketMaker(MarketMarketMaker marketMarketMaker, ProposalSide propSide) {
        return getAlreadyExistingProposal(marketMarketMaker, propSide) != null;
    }

    public boolean bothSidesArrivedForMarketMarketMaker(MarketMarketMaker marketMarketMaker) {
        boolean arrived = false;
        if (proposalExistsForMarketMarketMaker(marketMarketMaker, ProposalSide.ASK) && proposalExistsForMarketMarketMaker(marketMarketMaker, ProposalSide.BID)) {
            arrived = true;
        }

        return arrived;
    }

    public void checkSides() throws CloneNotSupportedException {

    	// AMC BXSUP-1959 20151027 Since we received from Reuters a book with both sides lacking one or more MM, do the process for both sides
        int askSize = askProposals.size();
        int bidSize = bidProposals.size();

        int delta = askSize - bidSize;
    	while (delta != 0) {
	        Collection<Proposal> shorterSide = null;
	        Collection<Proposal> longerSide = null;
	
	        if (delta > 0) {
	            shorterSide = bidProposals;
	            longerSide = askProposals;
	            LOGGER.info("NB: Bid book is shorter, the delta is {}", delta);
	        } else {
	            shorterSide = askProposals;
	            longerSide = bidProposals;
	            LOGGER.info("NB: Ask book is shorter, the delta is {}", delta);
	        }

	        if (shorterSide != null && longerSide != null) {
	//            int foundCounter = 0;
	            Iterator<Proposal> iterator = shorterSide.iterator();
	            // carico in una stringa le coppie mercato:marketMaker del lato piu' corto
	            String shortSide = "";
	            while (iterator.hasNext()) {
	                Proposal proposal = iterator.next();
	                Market market = proposal.getMarket();
	                MarketMarketMaker marketMaker = proposal.getMarketMarketMaker();
	                shortSide += ((market != null && market.getName() != null) ? market.getName() : null) + ":"
	                        + ((marketMaker != null && marketMaker.getMarketSpecificCode() != null) ? marketMaker.getMarketSpecificCode() : null) + ";";
	            }
	            LOGGER.debug("Shorter Side contains {}", shortSide);

	            // itero sul lato piu' lungo cercando mercato:marketMaker nell'elenco di quello piu' corto
	            // se manca aggiungo copiando la proposal, azzerando prezzo e qty e cambiando il side
	            iterator = longerSide.iterator();
	            Proposal proposal = null;
	            List<Proposal> addTheseBidProposals = new ArrayList<Proposal>();
	            List<Proposal> addTheseAskProposals = new ArrayList<Proposal>();
	            while (iterator.hasNext()) {
	                proposal = iterator.next();
	                Market market = proposal.getMarket();
	                MarketMarketMaker marketMaker = proposal.getMarketMarketMaker();
	                String checkThis = ((market != null && market.getName() != null) ? market.getName() : null) + ":"
	                        + ((marketMaker != null && marketMaker.getMarketSpecificCode() != null) ? marketMaker.getMarketSpecificCode() : null) + ";";
	                if (!shortSide.matches(".*" + checkThis + ".*")) {
	                    LOGGER.info("Shorter Side is missing proposal from {}. Adding zero qty and price one", checkThis);
	                    Proposal fooProposal = proposal.clone();
	                    fooProposal.setQty(BigDecimal.ZERO);
	                    fooProposal.setPrice(fooProposal.getPrice().multiply(BigDecimal.ZERO));
	                    fooProposal.setSide(proposal.getSide().equals(ProposalSide.ASK) ? ProposalSide.BID : ProposalSide.ASK);
	                    if (proposal.getSide().equals(ProposalSide.ASK)) {
	                        addTheseBidProposals.add(fooProposal);
	                    } else if (proposal.getSide().equals(ProposalSide.BID)) {
	                        addTheseAskProposals.add(fooProposal);
	                    }
//	                    foundCounter++;
	                    // it is not necessarily true that if founCounter == delta we have finished.
//	                    if (foundCounter == delta) {
//	                        LOGGER.debug("No more missing proposals need to be added!");
//	                        break;
//	                    }
	                }
	            }
	            bidProposals.addAll(addTheseBidProposals);
	            askProposals.addAll(addTheseAskProposals);
	            askSize = askProposals.size();
	            bidSize = bidProposals.size();
	            delta = askSize - bidSize;
	        }
    	}
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("BOOK, ASK proposals: ");
        int count = 1;
        for (Proposal proposal : askProposals) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }

        sb.append(" BID proposals: ");
        count = 1;
        for (Proposal proposal : bidProposals) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }
        return sb.toString();
    }

    public String toStringDetailed() {
        StringBuilder sb = new StringBuilder();

        sb.append("BOOK, ASK proposals: ");
        int count = 1;
        for (Proposal proposal : askProposals) {
            sb.append(count++).append("-[").append(proposal).append("], ");
        }
        sb.append("BID proposals: ");
        count = 1;
        for (Proposal proposal : bidProposals) {
            sb.append(count++).append("-[").append(proposal).append("], ");
        }

        return sb.toString();
    }


    @Override
    public Collection<? extends Proposal>  getProposals(Proposal.ProposalSide side) {
    	if(side == null) return null;
        return (side == Proposal.ProposalSide.ASK) ? getAskProposals() :getBidProposals() ;
    }
}
