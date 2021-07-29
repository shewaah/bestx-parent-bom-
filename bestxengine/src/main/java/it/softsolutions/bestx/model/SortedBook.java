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

import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;

/**  
 *
 * Purpose: this class maintains the list of all proposals
 * received during the price discovery phase, sorted by side and with their validation status
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public class SortedBook implements Book, Cloneable {

	@SuppressWarnings("unused")
	private Long id;
	private Instrument instrument;
	private List<ClassifiedProposal> askProposals = new ArrayList<ClassifiedProposal>();
	private List<ClassifiedProposal> bidProposals = new ArrayList<ClassifiedProposal>();

	public int countValidProposalsByMarket(MarketCode marketCode, Rfq.OrderSide side) throws IllegalArgumentException {
		if(marketCode == null || side == null) throw new IllegalArgumentException();
		List<ClassifiedProposal> sideProposals = (side == Rfq.OrderSide.BUY) ? this.askProposals : this.bidProposals;
		int i = 0;
		for(Proposal prop : sideProposals) {
			if(marketCode.compareTo(prop.getMarket().getMarketCode()) == 0)
				i++;
		}
		return i;
	}

	public List<MarketMarketMakerSpec> getValidProposalDealersByMarket(MarketCode marketCode, Rfq.OrderSide side) throws IllegalArgumentException {
		if(marketCode == null || side == null) throw new IllegalArgumentException();
		List<ClassifiedProposal> sideProposals = (side == Rfq.OrderSide.BUY) ? this.askProposals : this.bidProposals;
		List<MarketMarketMakerSpec> dealers = new ArrayList<MarketMarketMakerSpec>();
		for(ClassifiedProposal prop : sideProposals) {
			if(prop.getProposalState() != Proposal.ProposalState.VALID) continue;
			MarketMarketMaker mmm = prop.getMarketMarketMaker();
			if(marketCode.compareTo(prop.getMarket().getMarketCode()) == 0)
				dealers.add(new MarketMarketMakerSpec(mmm.getMarketSpecificCode(), mmm.getMarketSpecificCodeSource()));
		}
		return dealers;
	}

	public void setValues(SortedBook sortedBook) {
		setInstrument(sortedBook.getInstrument());
		setAskProposals(sortedBook.getAskProposals());
		setBidProposals(sortedBook.getBidProposals());
	}

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
	public Collection<? extends Proposal> getProposals(Proposal.ProposalSide side) {
		if(side == null) return null;
		return (side == Proposal.ProposalSide.ASK) ? askProposals :bidProposals ;
	}

	@Override
	public List<ClassifiedProposal> getAskProposals() {
		return askProposals;
	}

	public void setBidProposals(List<ClassifiedProposal> bidProposals) {
		this.bidProposals = bidProposals;
	}

	@Override
	public List<ClassifiedProposal> getBidProposals() {
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

	public void removeProposal(Proposal proposal) {
		switch (proposal.getSide()) {
		case BID:
			bidProposals.remove((ClassifiedProposal) proposal);
			break;
		case ASK:
			askProposals.remove((ClassifiedProposal) proposal);
			break;
		default:
			break;
		}
	}

	public ClassifiedProposal getBestProposalBySide(Rfq.OrderSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(side);
		for (ClassifiedProposal prop : sideProposals) {
			if (prop.getProposalState() == ProposalState.VALID) {
				return prop;
			}
		}
		return null;
	}


	public ClassifiedProposal getBestProposalBySide(ProposalSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(side);
		for (ClassifiedProposal prop : sideProposals) {
			if (prop.getProposalState() == ProposalState.VALID) {
				return prop;
			}
		}
		return null;
	}
	public List<ClassifiedProposal> getValidSideProposals(Rfq.OrderSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(side);

		List<ClassifiedProposal> result = new ArrayList<ClassifiedProposal>();
		for (ClassifiedProposal prop : sideProposals) {
			if (prop.getProposalState() == ProposalState.VALID) {
				result.add(prop);
			}
		}
		return result;
	}


	/**
	 * Look for proposals in one of the substates passed as arguments.
	 * Remembering that, as default, the substate is NONE.
	 * 
	 * @param wantedSubStates required proposal states
	 * @param side required side
	 * @return a list of proposals or null
	 */
	public List<ClassifiedProposal> getProposalBySubState(List<ProposalSubState> wantedSubStates, Rfq.OrderSide side) {
		if (wantedSubStates == null) {
			throw new IllegalArgumentException("List of wanted states cannot be null");
		}
		if (side == null) {
			throw new IllegalArgumentException("Side cannot be null");
		}

		List<ClassifiedProposal> sideProposals = getSideProposals(side);

		List<ClassifiedProposal> proposals = null;
		if (sideProposals != null) {
			proposals = new ArrayList<ClassifiedProposal>();
			for (ClassifiedProposal prop : sideProposals) {
				if (wantedSubStates.contains(prop.getProposalSubState())) {
					proposals.add(prop);
				}
			}
		}
		return proposals;
	}

	protected ProposalSide convertSide(Rfq.OrderSide side) {
		return side==Rfq.OrderSide.BUY ? ProposalSide.ASK: ProposalSide.BID;
	}
	
	protected Rfq.OrderSide convertSide(ProposalSide side) {
		return side==ProposalSide.ASK ? Rfq.OrderSide.BUY: Rfq.OrderSide.SELL;
	}
	
	/**
	 * 
	 * Returns both VALID and REJECTED proposals
	 * (Bid for OrderSide.BUY, Ask for OrderSide.SELL), 
	 * assuming that the price is > 0.0
	 * 
	 */
	public List<ClassifiedProposal> getSideProposals(Rfq.OrderSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(convertSide(side));
		List<ClassifiedProposal> result = new ArrayList<ClassifiedProposal>();
		for (ClassifiedProposal prop : sideProposals) {
			boolean stateToAccept = prop.getProposalState() == ProposalState.VALID || prop.getProposalState() == ProposalState.REJECTED || prop.getProposalState() == ProposalState.ACCEPTABLE;
			boolean validPrice = (prop.getPrice().getAmount().doubleValue() > 0.0);
			if ( stateToAccept && validPrice ){
				result.add(prop);
			}
		}
		return result;
	}
	/**
	 * 
	 * Returns only VALID proposals for the relevant side
	 * assuming that the price is > 0.0
	 * 
	 */
	public List<ClassifiedProposal> getValidSideProposals(ProposalSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(side);

		List<ClassifiedProposal> result = new ArrayList<ClassifiedProposal>();
		for (ClassifiedProposal prop : sideProposals) {
			if (prop.getProposalState() == ProposalState.VALID){
				result.add(prop);
			}
		}
		return result;
	}
	/**
	 * 
	 * Returns the relevant side proposal list of the Book (does not clone it)
	 * 
	 */
	private List<ClassifiedProposal> getSideProposals(ProposalSide side) {
		return side == ProposalSide.ASK ? askProposals : bidProposals;
	}

	@Override
	public SortedBook clone() {
		SortedBook cloned = new SortedBook();
		cloned.setInstrument(getInstrument());
		cloned.setAskProposals(new ArrayList<ClassifiedProposal>());
		for (ClassifiedProposal prop : getAskProposals()) {
			cloned.getAskProposals().add(prop.clone());
		}
		cloned.setBidProposals(new ArrayList<ClassifiedProposal>());
		for (ClassifiedProposal prop : getBidProposals()) {
			cloned.getBidProposals().add(prop.clone());
		}
		return cloned;
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

	public ClassifiedProposal getSecondBest(Rfq.OrderSide side) {
		List<ClassifiedProposal> sideProposals = getSideProposals(side);
		boolean foundFirst= false;

		for (ClassifiedProposal prop : sideProposals) {
			if (prop.getProposalState() == ProposalState.VALID && foundFirst) {
				return prop;
			} else if (prop.getProposalState() == ProposalState.VALID) {
				foundFirst = true;
			}
		}
		return null;
	}

	//    public String toStringDetailed() {
	//        String output = "BOOK, ASK proposals : ";
	//        int count = 1;
	//        for (Proposal proposal : getAskProposals()) {
	//            output += count++ + "-[" + proposal.toString() + "], ";
	//        }
	//        output += " BID proposals : ";
	//        count = 1;
	//        for (Proposal proposal : getBidProposals()) {
	//            output += count++ + "-[" + proposal.toString() + "], ";
	//        }
	//        return output;
	//    }

}
