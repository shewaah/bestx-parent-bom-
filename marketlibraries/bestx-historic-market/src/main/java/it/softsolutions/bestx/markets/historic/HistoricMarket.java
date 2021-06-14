package it.softsolutions.bestx.markets.historic;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.model.BaseBook;
import it.softsolutions.bestx.model.Book;
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
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregator;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregatorListener;
import it.softsolutions.bestx.services.pricediscovery.order.OrderPriceManager;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.jsscommon.Money;

public class HistoricMarket extends MarketCommon implements MarketPriceConnection {

	private static final Logger LOGGER = LoggerFactory.getLogger(HistoricMarket.class);
	
	private String historicPricesQuery;
	private MarketCode marketCode;
	private MarketFinder marketFinder;
	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	
	// private Map<String, ProposalAggregator> proposalAggregatorMap = new ConcurrentHashMap<String, ProposalAggregator>();
	
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
		boolean bestExecutionRequired = order.isBestExecutionRequired();
		String fixOrderId = order.getFixOrderId();

		try {
			List<MarketMarketMaker> targetMarketMarketMakers = new ArrayList<MarketMarketMaker>();
			for (Venue venue : venues) {
				if (venue.getVenueType().compareTo(VenueType.MARKET_MAKER) == 0) {
					for (MarketMarketMaker marketMarketMaker : venue.getMarketMaker().getMarketMarketMakers()) {
						boolean canTrade = marketMarketMaker.canTrade(instrument, bestExecutionRequired);

						if (!canTrade) {
							LOGGER.info("The marketMarketMaker {} can not trade the instrument {}", marketMarketMaker, isin);
						}

						if (marketMarketMaker.getMarket().getMarketCode() == market.getOriginalMarket().getMarketCode() && canTrade) {
							if (!targetMarketMarketMakers.contains(marketMarketMaker)) {
								targetMarketMarketMakers.add(marketMarketMaker);
								LOGGER.debug("Added marketMarketMaker: {}", marketMarketMaker.getMarketSpecificCode());
							}
						} // else Removed logic about proposal discarders
					}
				}
			}

			LOGGER.info("Order {}, registering the price request to the proposal aggregator (isin = {})", fixOrderId, isin);
			String reason = Messages.getString("RejectProposalISINNotQuotedByMM");

			for (MarketMarketMaker targetMarketMarketMaker : targetMarketMarketMakers) {
				order.addMarketMakerNotQuotingInstr(targetMarketMarketMaker, reason);
			}

			// Retrieve all the marketSpecificCodes
			List<String> marketSpecificCodes = new ArrayList<String>(targetMarketMarketMakers.size());
			for (MarketMarketMaker marketMarketMaker : targetMarketMarketMakers) {
				marketSpecificCodes.add(marketMarketMaker.getMarketSpecificCode());
			}
			LOGGER.info("Order {}. Market Makers that will be enquired for prices: {}", fixOrderId, marketSpecificCodes);

			// Book aggregator
//			ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);
//			if (proposalAggregator == null) {
//				proposalAggregator = new ProposalAggregator(instrument);
//				proposalAggregatorMap.put(isin, proposalAggregator);
//			}

			String orderID = order.getFixOrderId();
			List<String> marketMakers = marketSpecificCodes;

//			ProposalAggregatorListener proposalAggregatorListener = new OrderPriceManager(orderID, marketCode, marketMakers, proposalAggregator, listener);
//			proposalAggregator.addProposalAggregatorListener(proposalAggregatorListener);

//			String timerName = SimpleMarketProposalAggregator.buildTimerName(isin, order.getFixOrderId(), getMarketCode());
//
//			try {
//				SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
//				JobDetail newJob = simpleTimerManager.createNewJob(timerName, this.getClass().getSimpleName(), false /* no durable flag required*/, true /* request recovery*/, true /* monitorable */);
//				Trigger trigger = null;
//				if (targetMarketMarketMakers.size() > 0 && maxLatency > 0) {
//					// this timer is not repeatable
//					trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, maxLatency);
//				} else {
//					// Dopo 10 secondi fa partire la onTimerExpired
//					// this timer is not repeatable
//					trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, 10000);
//				}
//				simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, true);
//			} catch (SchedulerException e) {
//				LOGGER.error("Error while scheduling price discovery wait timer: {}", e.getMessage(), e);
//			}

			// [BXMNT-430] marketSpecificCodes * 2 (BID e ASK)
			marketStatistics.pricesRequested(isin, marketSpecificCodes.size() * 2);

			// TODO Query market
			Map<String, Object> namedParameters = new HashMap<>();
			namedParameters.put("paramIsin", isin);
			namedParameters.put("paramNumPricePoints", this.numPricePoints);
			namedParameters.put("paramNumDays", this.numDays);
			
			Long marketId = market.getOriginalMarket().getMarketId();
			if (marketId == 1) {
				marketId = 2L; // FIXME Problem for TSOX...
			}
			namedParameters.put("paramMarketId", marketId);
			
			final Map<String, List<ClassifiedProposal>> askProposals = new ConcurrentHashMap<>();
			final Map<String, List<ClassifiedProposal>> bidProposals = new ConcurrentHashMap<>();
			
			this.namedParameterJdbcTemplate.query(this.historicPricesQuery, namedParameters, new RowCallbackHandler() {
				@Override // TODO Use a lambda?
				public void processRow(ResultSet rs) throws SQLException {
					try {
						ClassifiedProposal proposal = buildProposalFromResultResult(instrument, rs);
						String marketMakerSpecificCode = proposal.getMarketMarketMaker().getMarketSpecificCode();
						
						Map<String, List<ClassifiedProposal>> proposals = proposal.getSide() == ProposalSide.BID ? bidProposals : askProposals;
						if (proposals.get(marketMakerSpecificCode) == null) {
							proposals.put(marketMakerSpecificCode, new ArrayList<>());
						}
						proposals.get(marketMakerSpecificCode).add(proposal);
					} catch (Exception e) {
						LOGGER.error("Error while trying to create proposal", e);
					}
				}
			});
			
//			for (String marketMakerSpecificCode : marketSpecificCodes) {
//				try {
//				    if (askProposals.get(marketMakerSpecificCode) == null || askProposals.get(marketMakerSpecificCode).isEmpty()) {
//				    	proposalAggregator.onProposal(this.buildProposalReject(instrument, ProposalSide.ASK, marketMakerSpecificCode));
//				    } else {
//				    	for (ClassifiedProposal proposal : askProposals.get(marketMakerSpecificCode)) {
//				    		proposalAggregator.onProposal(proposal);
//				    	}
//				    }
//				    if (bidProposals.get(marketMakerSpecificCode) == null || bidProposals.get(marketMakerSpecificCode).isEmpty()) {
//				    	proposalAggregator.onProposal(this.buildProposalReject(instrument, ProposalSide.BID, marketMakerSpecificCode));
//				    } else {
//				    	for (ClassifiedProposal proposal : bidProposals.get(marketMakerSpecificCode)) {
//				    		proposalAggregator.onProposal(proposal);
//				    	}
//				    }				    
//					marketStatistics.pricesResponseReceived(isin, 1);
//				} catch (Exception e) {
//					LOGGER.error("Error managing classifiedProposal {}: {}", marketMakerSpecificCode, e.getMessage(), e);
//				}
//			}
//			
			BaseBook book = new BaseBook();
			book.setInstrument(instrument);
			List<ClassifiedProposal> allProposals = new ArrayList<>();
			for (List<ClassifiedProposal> proposalsList : askProposals.values()) {
				allProposals.addAll(proposalsList);
			}
			for (List<ClassifiedProposal> proposalsList : bidProposals.values()) {
				allProposals.addAll(proposalsList);
			}
			
            for(Proposal bestProposal : allProposals) {
                book.addProposal(bestProposal);
            }
            LOGGER.debug("Duplicated removed book: {}", book);
            
            // 3. notify the MarketPriceConnectionListener
            listener.onMarketBookComplete(marketCode, book);
			
		} catch (ConcurrentModificationException cme) {
			LOGGER.error("Error while starting the price requests towards BBG: {}", cme.getMessage(), cme);
		}
		
	}

	private ClassifiedProposal buildProposalFromResultResult(Instrument instrument, ResultSet rs) {

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
            Proposal.PriceType priceType = Proposal.PriceType.PRICE;
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
            
            return classifiedProposal;
        } catch (Exception e) {
        	LOGGER.warn("Error trying to create Proposal from row in DB", e);
        	return null;
        }

	}
	
	private ClassifiedProposal buildProposalReject(Instrument instrument, ProposalSide side, String marketMarketMakerCode) {
        LOGGER.debug("Building proposal reject: instrument = {}, marketMakerCode = {}", instrument, marketMarketMakerCode);
        
        if (marketMarketMakerCode == null || instrument == null || side == null) {
            throw new IllegalArgumentException("params can't be null\"");
        }

        if (side != ProposalSide.ASK && side != ProposalSide.BID) {
            throw new IllegalArgumentException("Unsupported Side Only Bid or Offer value is accepted");
        }
        ClassifiedProposal classifiedProposal = null;

        try {

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
            }

            classifiedProposal = new ClassifiedProposal();

            classifiedProposal.setType(ProposalType.CLOSED);
            classifiedProposal.setProposalState(ProposalState.REJECTED);
            classifiedProposal.setProposalSubState(ProposalSubState.NONE);
            classifiedProposal.setReason("No data");

            Venue marketVenue = new Venue(venue);
            marketVenue.setMarket(originalMarket);
            classifiedProposal.setMarket(historicMarket);

            classifiedProposal.setMarketMarketMaker(marketMarketMaker);
            classifiedProposal.setVenue(marketVenue);
            classifiedProposal.setQty(BigDecimal.ZERO);

            Money price = new Money(instrument.getCurrency(), BigDecimal.ZERO);
            classifiedProposal.setPrice(price);
            classifiedProposal.setSide(side);
            
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

//	public Map<String, ProposalAggregator> getProposalAggregatorMap() {
//		return proposalAggregatorMap;
//	}
//
//	public void setProposalAggregatorMap(Map<String, ProposalAggregator> proposalAggregatorMap) {
//		this.proposalAggregatorMap = proposalAggregatorMap;
//	}

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
