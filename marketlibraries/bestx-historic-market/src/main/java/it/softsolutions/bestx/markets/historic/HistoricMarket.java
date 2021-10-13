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
import it.softsolutions.bestx.connections.MarketConnection;
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
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.jsscommon.Money;
import quickfix.field.PriceType;

public class HistoricMarket extends MarketCommon implements MarketPriceConnection, HistoricMarketMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricMarket.class);
	
	private String historicPricesQuery;
	private MarketCode marketCode;
	private MarketFinder marketFinder;
	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	
	private int numPricePoints = 2;
	private int numDays = 1;
	
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private MarketConnection referenceMarketConnection;
	
	@Override
	public void cleanBook() {
		// this JMX method does not have an implementation specific to historic markets
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
		return this.referenceMarketConnection.isBuySideConnectionProvided();
	}

	@Override
	public boolean isPriceConnectionProvided() {
		return true;
	}

	@Override
	public boolean isBuySideConnectionAvailable() {
		return this.referenceMarketConnection.isBuySideConnectionAvailable();
	}

	@Override
	public boolean isPriceConnectionAvailable() {
		return this.referenceMarketConnection.isPriceConnectionAvailable();
	}

	@Override
	public boolean isAMagnetMarket() {
		return false;
	}

	@Override
    public boolean isPriceConnectionEnabled() {
        return this.referenceMarketConnection.isPriceConnectionEnabled();
    }

    @Override
    public boolean isBuySideConnectionEnabled() {
        return this.referenceMarketConnection.isBuySideConnectionEnabled();
    }
	
	
	@Override
	public void ensurePriceAvailable() throws MarketNotAvailableException {
		// prices are availble by definition, so no need to throw any exception
	}

	@Override
	public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order)
			throws BestXException {
		LOGGER.debug("orderID = {}, venues = {}, maxLatency = {}", (order != null ? order.getFixOrderId() : order), venues, maxLatency);

		Instrument instrument = (order!= null) ? order.getInstrument() : null;
		String isin = instrument.getIsin();
		Market market = this.marketFinder.getMarketByCode(this.marketCode, null);

		// BESTX-910 TC time (a)
		LOGGER.debug("Requesting prices to {} for ISIN: {}",this.getMarketCode(),  isin);
		try {	
			final List<ClassifiedProposal> allProposals = Collections.synchronizedList(new ArrayList<>());
			
			Map<String, Object> namedParameters = new HashMap<>();
			namedParameters.put("paramIsin", isin);
			namedParameters.put("paramSide", (order.getSide().equals(OrderSide.BUY) ? ProposalSide.ASK.getFixCode() : ProposalSide.BID.getFixCode()));
			namedParameters.put("paramNumPricePoints", this.numPricePoints);
			namedParameters.put("paramNumDays", this.numDays);
			
			Long marketId = market.getOriginalMarket().getMarketId();
			if (Market.MarketCode.BLOOMBERG.equals(market.getOriginalMarket().getMarketCode())) {
				marketId = marketFinder.getMarketByCode(Market.MarketCode.TSOX, null).getMarketId(); 
			}
			namedParameters.put("paramMarketId", marketId);
			LOGGER.debug("[DBPERF] Start query to {} for ISIN: {}", this.getMarketCode(), isin);
			this.namedParameterJdbcTemplate.query(this.historicPricesQuery, namedParameters, (ResultSet rs) -> {
				try {
				   ClassifiedProposal prop = this.buildProposalFromResult(order, instrument, rs);
				   if (prop != null) {
				      allProposals.add(prop);
				   }
				} catch (Exception e) {
					LOGGER.error("Error while trying to create proposal", e);
				}
			});
			// BESTX-910 TC time (b)
			LOGGER.debug("[DBPERF] Stop query to {} for ISIN: {}", this.getMarketCode(), isin);
			
			BaseBook book = new BaseBook();
			book.setInstrument(instrument);
            for(Proposal bestProposal : allProposals) {
                book.addProposal(bestProposal);
            }
            
    		// BESTX-910 TC time (c)
			LOGGER.debug("Proposals put in the book: calling listener. Market {} ISIN: {}", this.getMarketCode(), isin);
            listener.onMarketBookComplete(marketCode, book);
			
		} catch (ConcurrentModificationException cme) {
			LOGGER.error("Error while starting the price requests towards BBG: {}", cme.getMessage(), cme);
		}
		
	}

	private ClassifiedProposal buildProposalFromResult(Order order, Instrument instrument, ResultSet rs) {

        ClassifiedProposal classifiedProposal = null;

        try {
        	
        	String marketMarketMakerCode = rs.getString("MarketBankCode");
        	
        	Market historicMarket = this.marketFinder.getMarketByCode(this.marketCode, null);
        	Market originalMarket = historicMarket.getOriginalMarket();
        	MarketCode originalMarketCode = originalMarket.getMarketCode();
        	Venue marketVenue = null;
        	
            MarketMarketMaker marketMarketMaker = this.marketMakerFinder.getSmartMarketMarketMakerByCode(originalMarketCode, marketMarketMakerCode);
            if (marketMarketMaker == null) {
            	LOGGER.info("Order={}. Executable price found in market {} for unknown dealer {}", order.getFixOrderId(), originalMarket.getName(), marketMarketMakerCode);
            	marketMarketMaker = this.createTransientMarketMarketMaker(marketMarketMakerCode, originalMarket);
            } else {
	            Venue venue = this.venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
	            if (venue == null) {
	                return null;
	            }
	            if (venue.getMarket() == null) {
	            	return null;
	            }
	            marketVenue = new Venue(venue);
	            marketVenue.setMarket(originalMarket);
            }
            
            if (marketVenue == null) {
            	marketVenue = new Venue();
            	marketVenue.setMarketMaker(marketMarketMaker.getMarketMaker());
            	marketVenue.setCode(marketMarketMaker.getMarketMaker().getCode());
            	marketVenue.setMarket(originalMarket);
            	marketVenue.setVenueType(VenueType.MARKET_MAKER);
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
            default:
            	priceType = Proposal.PriceType.PRICE;
            }
                        
            ProposalType proposalType = ProposalType.TRADEABLE;

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
	
	private MarketMarketMaker createTransientMarketMarketMaker(String dealerCode, Market market) {
		MarketMarketMaker mmm = new MarketMarketMaker();
		MarketMaker mm = new MarketMaker();
		mmm.setMarketSpecificCode(dealerCode);
		mmm.setMarket(market);
		mm.setName("UNKNOWN");
		mm.setCode("UNKNOWN");
		mm.setRank(99999);
		mmm.setMarketMaker(mm);
		return mmm;
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

	public MarketConnection getReferenceMarketConnection() {
		return referenceMarketConnection;
	}

	public void setReferenceMarketConnection(MarketConnection referenceMarketConnection) {
		this.referenceMarketConnection = referenceMarketConnection;
	}
}
