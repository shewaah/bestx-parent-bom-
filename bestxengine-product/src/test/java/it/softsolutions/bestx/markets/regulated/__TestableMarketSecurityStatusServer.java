package it.softsolutions.bestx.markets.regulated;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.MarketSecurityStatusService;

import java.math.BigDecimal;
import java.util.Date;

public class __TestableMarketSecurityStatusServer implements
MarketSecurityStatusService {

	@Override
	public Market getQuotingMarket(MarketCode marketCode, String instrument)
			throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Date getMarketInstrumentSettlementDate(MarketCode marketCode,
			String instrument) throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QuotingStatus getInstrumentQuotingStatus(MarketCode marketCode,
			String instrument) throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insertMarketSecurityStatusItem(MarketCode marketCode,
			SubMarketCode subMarketCode, String instrument)
					throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMarketSecurityQuotingStatus(MarketCode marketCode,
			SubMarketCode subMarketCode, String instrument,
			QuotingStatus quotingStatus) throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMarketSecuritySettlementDate(MarketCode marketCode,
			SubMarketCode subMarketCode, String instrument, Date settlementDate)
					throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMarketSecuritySettlementDateAndBondType(
			MarketCode marketCode, SubMarketCode subMarketCode,
			String instrument, Date settlementDate, String bondType,
			int marketAffiliation, String marketAffiliationStr,
			int quoteIndicator, String quoteIndicatorStr) throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMarketSecurityQuantity(MarketCode marketCode,
			SubMarketCode subMarketCode, String isin, BigDecimal minQty,
			BigDecimal qtyTick, BigDecimal multiplier) throws BestXException {
		// TODO Auto-generated method stub

	}

	@Override
	public BigDecimal[] getQuantityValues(MarketCode marketCode,
			Instrument instrument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getMinTradingQty(MarketCode marketCode,
			Instrument instrument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getMinIncrement(MarketCode marketCode,
			Instrument instrument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getQtyMultiplier(MarketCode marketCode,
			Instrument instrument) {
		//it should be null for MOT and TLX, 1 for HIMTF e 1000000 for MTSPRIME
		if (marketCode.equals(MarketCode.MTSPRIME))
			return new BigDecimal(1000000.0);
		else if (marketCode.equals(MarketCode.HIMTF))
			return new BigDecimal(1.0);
		else
			return null;
	}

	@Override
	public Market getInstrumentMarket(MarketCode marketCode, String instrument)
			throws BestXException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMarketBondType(MarketCode marketCode, String instrument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMarketAffiliation(MarketCode marketCode, String instrument) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean validQuantities(MarketCode marketCode,
			Instrument instrument, Order order) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getQuotIndicator(MarketCode marketCode, String instrument) {
		// TODO Auto-generated method stub
		return 0;
	}

}
