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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.softsolutions.bestx.model.Proposal.ProposalState;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class ClassifiedBook implements Book {
    
    @SuppressWarnings("unused")
    private Long id;
    private Instrument instrument;
    private List<ClassifiedProposal> askProposals = new ArrayList<ClassifiedProposal>();
    private List<ClassifiedProposal> bidProposals = new ArrayList<ClassifiedProposal>();

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    @Override
	public Instrument getInstrument() {
        return instrument;
    }


    public void setAskProposals(List<ClassifiedProposal> askProposals) {
        this.askProposals = askProposals;
    }

    @Override
	public Collection<? extends ClassifiedProposal> getAskProposals() {
        return askProposals;
    }

    public void setBidProposals(List<ClassifiedProposal> bidProposals) {
        this.bidProposals = bidProposals;
    }

    @Override
    public Collection<? extends Proposal>  getProposals(Proposal.ProposalSide side) {
    	if(side == null) return null;
        return (side == Proposal.ProposalSide.ASK) ? askProposals : bidProposals ;
    }
    
    @Override
	public Collection<? extends ClassifiedProposal> getBidProposals() {
        return bidProposals;
    }

    @Override
	public void addProposal(Proposal proposal) {
        switch (proposal.getSide()) {
        case BID:
            bidProposals.add((ClassifiedProposal) proposal);
            break;
        case ASK:
            askProposals.add((ClassifiedProposal) proposal);
            break;
        default:
            break;
        }
    }

    public ClassifiedProposal getBestProposalBySide(Rfq.OrderSide side) {
        List<ClassifiedProposal> sideProposals = null;

        switch (side) {
        case BUY:
            sideProposals = askProposals;
            break;
        case SELL:
            sideProposals = bidProposals;
            break;
        default:
            break;
        }

        for (ClassifiedProposal prop : sideProposals) {
            if (prop.getProposalState() == ProposalState.VALID) {
                return prop;
            }
        }
        return null;
    }

    public List<ClassifiedProposal> getValidSideProposals(Rfq.OrderSide side) {
        List<ClassifiedProposal> result = new ArrayList<ClassifiedProposal>();
        List<ClassifiedProposal> sideProposals = null;

        switch (side) {
        case BUY:
            sideProposals = askProposals;
            break;
        case SELL:
            sideProposals = bidProposals;
            break;
        default:
            break;
        }

        for (ClassifiedProposal prop : sideProposals) {
            if (prop.getProposalState() == ProposalState.VALID) {
                result.add(prop);
            }
        }
        return result;
    }
    
    @Override
    public ClassifiedBook clone() {
        ClassifiedBook newBook = new ClassifiedBook();
        List<ClassifiedProposal> newList = new ArrayList<ClassifiedProposal>();
        newList.addAll(askProposals);
        newBook.setAskProposals(newList);
        newList = new ArrayList<ClassifiedProposal>();
        newList.addAll(bidProposals);
        newBook.setBidProposals(newList);
        newBook.setInstrument(instrument);
        return newBook;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("BOOK, ASK proposals: ");
        int count = 1;
        for (ClassifiedProposal proposal : askProposals) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getType()).append(",");
            sb.append(proposal.getProposalState()).append(",");
            sb.append(proposal.getProposalSubState()).append(",");
            sb.append(proposal.getTimestampstr()).append(",");
            sb.append(proposal.getQty()).append(",");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }

        sb.append(" BID proposals: ");
        count = 1;
        for (ClassifiedProposal proposal : bidProposals) {
            sb.append(count++).append("-[");
            sb.append(proposal.getMarketMarketMaker() == null ? proposal.getMarket().getMarketCode() : proposal.getMarketMarketMaker().getMarketSpecificCode()).append(", ");
            sb.append(proposal.getType()).append(",");
            sb.append(proposal.getProposalState()).append(",");
            sb.append(proposal.getProposalSubState()).append(",");
            sb.append(proposal.getTimestampstr()).append(",");
            sb.append(proposal.getQty()).append(",");
            sb.append(proposal.getPrice().getAmount()).append("], ");
        }
        return sb.toString();
    }
}
