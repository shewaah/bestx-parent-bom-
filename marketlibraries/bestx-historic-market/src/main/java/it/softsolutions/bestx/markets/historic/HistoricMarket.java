package it.softsolutions.bestx.markets.historic;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.jsscommon.Money;
import quickfix.field.PriceType;

public class HistoricMarket extends MarketCommon implements MarketPriceConnection, HistoricMarketMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricMarket.class);
	
	private String historicPricesQuery;
	private MarketCode marketCode;
	private MarketFinder marketFinder;
	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	
	private int numPricePoints;
	private int numDays;
	
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Override
	public void cleanBook() {
	}

	@Override
	public int countOrders() {
		return 0;
	}

	@Override
	public int getActiveTimersNum() {
		return 0;
	}

	@Override
	public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
		if (marketMaker == null || instrument == null) {
			throw new IllegalArgumentException("one param is null: marketMaker = " + marketMaker + ", instrument = " + instrument);
		}

		return marketMaker.canTrade(instrument);
	}

	@Override
	public MarketCode getMarketCode() {
		return this.marketCode;
	}
	
	public void setMarketCode(MarketCode marketCode) {
		this.marketCode = marketCode;
	}

	@Override
	public boolean isBuySideConnectionProvided() {
		return true; // FIXME
	}

	@Override
	public boolean isPriceConnectionProvided() {
		return true;
	}

	@Override
	public boolean isBuySideConnectionAvailable() {
		return true; // FIXME
	}

	@Override
	public boolean isPriceConnectionAvailable() {
		return true;
	}

	@Override
	public boolean isAMagnetMarket() {
		return false;
	}

    public boolean isPriceConnectionEnabled() {
        return true;
    }

    public boolean isBuySideConnectionEnabled() {
        return true; // FIXME Link to original market
    }
	
	
	@Override
	public void ensurePriceAvailable() throws MarketNotAvailableException {
	}

	@Override
	public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order)
			throws BestXException {
		LOGGER.debug("orderID = {}, venues = {}, maxLatency = {}", (order != null ? order.getFixOrderId() : order), venues, maxLatency);

		Instrument instrument = order.getInstrument();
		String isin = instrument.getIsin();
		Market market = this.marketFinder.getMarketByCode(this.marketCode, null);

		LOGGER.debug("Requesting price to Bloomberg for ISIN: {}", isin);
		try {
			Map<String, Object> namedParameters = new HashMap<>();
			namedParameters.put("paramIsin", isin);
			namedParameters.put("paramSide", (order.getSide().equals(OrderSide.BUY) ? ProposalSide.ASK.getFixCode() : ProposalSide.BID.getFixCode()));
			namedParameters.put("paramNumPricePoints", this.numPricePoints);
			namedParameters.put("paramNumDays", this.numDays);
			
			Long marketId = market.getOriginalMarket().getMarketId();
			if (marketId == 1) {
				marketId = 2L; // FIXME Problem for TSOX...
			}
			namedParameters.put("paramMarketId", marketId);
			
			final List<ClassifiedProposal> allProposals = Collections.synchronizedList(new ArrayList<>());
			
			this.namedParameterJdbcTemplate.query(this.historicPricesQuery, namedParameters, (ResultSet rs) -> {
				try {
				   ClassifiedProposal prop = this.buildProposalFromResult(instrument, rs);
				   if (prop != null) {
				      allProposals.add(prop);
				   }
				} catch (Exception e) {
					LOGGER.error("Error while trying to create proposal", e);
				}
			});
			
			BaseBook book = new BaseBook();
			book.setInstrument(instrument);
            for(Proposal bestProposal : allProposals) {
                book.addProposal(bestProposal);
            }
            
            listener.onMarketBookComplete(marketCode, book);
			
		} catch (ConcurrentModificationException cme) {
			LOGGER.error("Error while starting the price requests towards BBG: {}", cme.getMessage(), cme);
		}
		
	}

	private ClassifiedProposal buildProposalFromResult(Instrument instrument, ResultSet rs) {

        ClassifiedProposal classifiedProposal = null;

        try {
        	
        	String marketMarketMakerCode = rs.getString("MarketBankCode");
        	
        	Market historicMarket = this.marketFinder.getMarketByCode(this.marketCode, null);
        	Market originalMarket = historicMarket.getOriginalMarket();
        	MarketCode originalMarketCode = originalMarket.getMarketCode();
        	
            MarketMarketMaker marketMarketMaker = this.marketMakerFinder.getMarketMarketMakerByCode(originalMarketCode, marketMarketMakerCode);
            if (marketMarketMaker == null) {
            	return null;
            }

            Venue venue = this.venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
            if (venue == null) {
                return null;
            }
            if (venue.getMarket() == null) {
            	return null;
            }

            String proposalSideStr = rs.getString("Side");
            
            ProposalSide proposalSide = null;
            if (ProposalSide.BID.getFixCode().equals(proposalSideStr)) {
            	proposalSide = ProposalSide.BID;
            } else if (ProposalSide.ASK.getFixCode().equals(proposalSideStr)) {
            	proposalSide = ProposalSide.ASK;
            } else {
            	return null;
            }
            
            Date timestamp = rs.getTimestamp("ArrivalTime");
            BigDecimal qty = Optional.ofNullable(rs.getBigDecimal("Qty")).orElse(BigDecimal.ZERO);
            BigDecimal amount = Optional.ofNullable(rs.getBigDecimal("Price")).orElse(BigDecimal.ZERO);
            
            Proposal.PriceType priceType = null;
            switch (rs.getInt("PriceType")) {
            case PriceType.PERCENTAGE:
            	priceType = Proposal.PriceType.PRICE;
            	break;
            case PriceType.YIELD:
            	priceType = Proposal.PriceType.YIELD;
            	break;
            case PriceType.SPREAD:
            	priceType = Proposal.PriceType.SPREAD;
            	break;
            case PriceType.PER_UNIT:
            	priceType = Proposal.PriceType.UNIT;
            	break;
            }
            
            
            ProposalType proposalType = ProposalType.TRADEABLE;


            Venue marketVenue = new Venue(venue);
            marketVenue.setMarket(originalMarket);

            classifiedProposal = new ClassifiedProposal();
            classifiedProposal.setMarket(historicMarket);
            classifiedProposal.setMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setVenue(marketVenue);
            classifiedProposal.setProposalState(ProposalState.NEW);
            classifiedProposal.setType(proposalType);
            classifiedProposal.setSide(proposalSide);
            classifiedProposal.setQty(qty);
            classifiedProposal.setFutSettDate(instrument.getBBSettlementDate());
            classifiedProposal.setNonStandardSettlementDateAllowed(false);
            classifiedProposal.setNativeMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setTimestamp(timestamp);
            Money price = new Money(instrument.getCurrency(), amount);
            classifiedProposal.setPrice(price);
            classifiedProposal.setPriceType(priceType);
            classifiedProposal.setAuditQuoteState(rs.getString("AuditQuoteState"));
            
            return classifiedProposal;
        } catch (Exception e) {
        	LOGGER.warn("Error trying to create Proposal from row in DB", e);
        	return null;
        }

	}
	
	@Override
	public Market getQuotingMarket(Instrument instrument) throws BestXException {
		return this.marketFinder.getMarketByCode(this.marketCode, null);
	}

	@Override
	public Date getMarketInstrumentSettlementDate(Instrument instrument) throws BestXException {
		return instrument.getDefaultSettlementDate();
	}

	@Override
	public QuotingStatus getInstrumentQuotingStatus(Instrument instrument) throws BestXException {
		return QuotingStatus.NEG;
	}

	@Override
	public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
		return true;
	}

	@Override
	public MarketBuySideConnection getBuySideConnection() {
		return null;
	}

	@Override
	public MarketPriceConnection getPriceConnection() {
		return this;
	}	
	
	public MarketFinder getMarketFinder() {
		return marketFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	public void setNamedParameterJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public String getHistoricPricesQuery() {
		return historicPricesQuery;
	}

	public void setHistoricPricesQuery(String historicPricesQuery) {
		this.historicPricesQuery = historicPricesQuery;
	}

	public int getNumPricePoints() {
		return numPricePoints;
	}

	public void setNumPricePoints(int numPricePoints) {
		this.numPricePoints = numPricePoints;
	}

	public int getNumDays() {
		return numDays;
	}

	public void setNumDays(int numDays) {
		this.numDays = numDays;
	}

	public MarketMakerFinder getMarketMakerFinder() {
		return marketMakerFinder;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public VenueFinder getVenueFinder() {
		return venueFinder;
	}

	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	
	
}
