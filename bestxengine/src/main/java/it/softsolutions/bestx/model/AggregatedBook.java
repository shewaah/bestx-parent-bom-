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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 29/ago/2013 
 * 
 **/
public class AggregatedBook implements Cloneable, Book {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedBook.class);

    private Instrument instrument;
    private Map<String, Proposal> askProposals = new ConcurrentHashMap<String, Proposal>();
    private Map<String, Proposal> bidProposals = new ConcurrentHashMap<String, Proposal>();
    
    public AggregatedBook(Instrument instrument) {
        super();
        this.instrument = instrument;
    }
    
    @Override
    public AggregatedBook clone() {
        AggregatedBook res = new AggregatedBook(instrument);
        res.askProposals.putAll(askProposals);
        res.bidProposals.putAll(bidProposals);
        return res;
    }

    @Override
    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public Collection<? extends Proposal>  getProposals(Proposal.ProposalSide side) {
    	if(side == null) return null;
        return (side == Proposal.ProposalSide.ASK) ? askProposals.values() :bidProposals.values() ;
    }
    
    @Override
    public Collection<? extends Proposal> getAskProposals() {
        return askProposals.values();
    }

    @Override
    public Collection<? extends Proposal> getBidProposals() {
        return bidProposals.values();
    }

    @Override
    public void addProposal(Proposal proposal) {
        LOGGER.debug("{}", proposal);
        
        switch (proposal.getSide()) {
        case BID:
            bidProposals.put(proposal.getMarketMarketMaker().getMarketSpecificCode(), proposal);
            break;
        case ASK:
            askProposals.put(proposal.getMarketMarketMaker().getMarketSpecificCode(), proposal);
            break;
        default:
            break;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("BOOK, ASK proposals: ");
        int count = 1;
        for (Proposal proposal : askProposals.values()) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }
        
        sb.append(" BID proposals: ");
        count = 1;
        for (Proposal proposal : bidProposals.values()) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }
        return sb.toString();
    }
}
