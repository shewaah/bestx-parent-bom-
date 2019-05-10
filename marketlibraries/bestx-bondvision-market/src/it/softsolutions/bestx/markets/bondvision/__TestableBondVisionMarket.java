package it.softsolutions.bestx.markets.bondvision;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.MarketMarketMaker;

import java.util.ArrayList;
import java.util.Map;

public class __TestableBondVisionMarket extends it.softsolutions.bestx.markets.mts.bondvision.BondVisionMarket {
	public void levelBook(ArrayList<ClassifiedProposal> inBvProposals, Map<MarketMarketMaker, ClassifiedProposal> outBidProposals, Map<MarketMarketMaker, ClassifiedProposal> outAskProposals) throws BestXException
	{
		super.levelBook(inBvProposals, outBidProposals, outAskProposals);
	}
}
