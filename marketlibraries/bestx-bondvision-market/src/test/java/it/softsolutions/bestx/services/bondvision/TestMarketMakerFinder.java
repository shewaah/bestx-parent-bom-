package it.softsolutions.bestx.services.bondvision;

import java.util.HashSet;
import java.util.Set;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;

public class TestMarketMakerFinder implements MarketMakerFinder {

	@Override
	public MarketMaker getMarketMakerByCode(String code) throws BestXException {
		MarketMaker mm = new MarketMaker();
		Set<MarketMarketMaker> mmmList = new HashSet<MarketMarketMaker>();
		MarketMarketMaker mmm = new MarketMarketMaker();
		mmm.setMarket(new Market());
		mmm.getMarket().setMarketCode(Market.MarketCode.BV);
		mmm.setMarketMaker(mm);
		mmm.setMarketSpecificCode(code);
		mmm.setMarketSpecificCodeSource("C");
		mmmList.add(mmm);
		mm.setCode(code);
		mm.setEnabled(true);
		mm.setMarketMarketMakers(mmmList);
		return mm;
	}

	@Override
	public MarketMaker getMarketMakerByAccount(String accountCode) throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MarketMarketMaker getMarketMarketMakerByCode(MarketCode marketCode, String code) throws BestXException {
		if(marketCode == Market.MarketCode.BV) {
			MarketMaker mm = new MarketMaker();
			Set<MarketMarketMaker> mmmList = new HashSet<MarketMarketMaker>();
			MarketMarketMaker mmm = new MarketMarketMaker();
			mmm.setMarket(new Market());
			mmm.getMarket().setMarketCode(Market.MarketCode.BV);
			mmm.setMarketMaker(mm);
			mmm.setMarketSpecificCode(code);
			mmm.setMarketSpecificCodeSource("C");
			mmmList.add(mmm);
			mm.setCode(code);
			mm.setEnabled(true);
			mm.setMarketMarketMakers(mmmList);
			return mmm;
		}
		return null;
	}

	@Override
	public MarketMarketMaker getMarketMarketMakerByTSOXCode(String tsoxSpecificCode) throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

}
