package it.softsolutions.bestx.services.price;

import java.util.Date;
import java.util.Set;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market.MarketCode;

public class __FakeMarketPriceConnection implements MarketPriceConnection {
	private MarketCode marketCode;
	
	@Override
	public void queryPrice(MarketPriceConnectionListener listener,
			Set<Venue> venues, long maxLatency, Order order)
			throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public Market getQuotingMarket(Instrument instrument) throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getMarketInstrumentSettlementDate(Instrument instrument)
			throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QuotingStatus getInstrumentQuotingStatus(Instrument instrument)
			throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMarketCode(MarketCode marketCode) {
		this.marketCode = marketCode;
	}
	@Override
	public MarketCode getMarketCode() {
		return marketCode;
	}

	@Override
	public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
		return true;
	}

	@Override
	public void ensurePriceAvailable() throws MarketNotAvailableException {
		// TODO Auto-generated method stub
		
	}

}
