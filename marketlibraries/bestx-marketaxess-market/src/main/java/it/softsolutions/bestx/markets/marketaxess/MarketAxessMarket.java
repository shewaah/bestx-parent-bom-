
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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

package it.softsolutions.bestx.markets.marketaxess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.connections.tradestac2.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac2.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.connections.tradestac2.TradeStacTradeConnection;
import it.softsolutions.bestx.connections.tradestac2.TradeStacTradeConnectionListener;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.markets.ProposalDiscarder;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregator;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregatorListener;
import it.softsolutions.bestx.services.pricediscovery.order.OrderPriceManager;
import it.softsolutions.bestx.services.pricediscovery.worker.TimerExpiredWorker;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.marketlibraries.marketaxessfibuysidefix.fields.ExecType;
import it.softsolutions.tradestac2.api.ConnectionStatus;


/**
 *
 * Purpose: this class is the market business center for MarketAxess
 *
 * Project Name : bestx-marketaxess-market
 * First created by: anna.cochetti
 * Creation date: 11 gen 2017
 * 
 **/

public class MarketAxessMarket extends MarketCommon implements TradeStacPreTradeConnectionListener, TradeStacTradeConnectionListener, ConnectionListener, MarketBuySideConnection, MarketPriceConnection, TimerEventListener, Connection, MarketMXBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MarketAxessMarket.class);


	private ConnectionStatus preTradeConnectionMarketStatus;
	private ConnectionStatus tradeConnectionMarketStatus;

	private TradeStacPreTradeConnection preTradeConnection;
	private TradeStacTradeConnection tradeConnection;

	private Market market;
	private final Market.MarketCode marketCode = Market.MarketCode.MARKETAXESS;

	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	private MarketFinder marketFinder;
	private InstrumentFinder instrumentFinder;

	private SerialNumberService serialNumberService;
	private OperationRegistry operationRegistry;

	private Executor executor;

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	private Map<String,ConnectionListener> connectionListener;

	List<ProposalDiscarder> proposalDiscarders;

	public Map<String, ConnectionListener> getConnectionListener() {
		return connectionListener;
	}

	public void setConnectionListener(Map<String, ConnectionListener> connectionListener) {
		this.connectionListener = connectionListener;
	}

	public void setProposalDiscarders(List<ProposalDiscarder> proposalDiscarders) {
		this.proposalDiscarders = proposalDiscarders;
	}

	// <isin, proposalAggregator>
	private Map<String, ProposalAggregator> proposalAggregatorMap = new ConcurrentHashMap<String, ProposalAggregator>();

	public ConnectionStatus getTradeConnectionMarketStatus() {
		return tradeConnectionMarketStatus;
	}

	public void setTradeConnectionMarketStatus(ConnectionStatus tradeConnectionMarketStatus) {
		this.tradeConnectionMarketStatus = tradeConnectionMarketStatus;
	}

	public Market getMarket() {
		return market;
	}

	public void setMarket(Market market) {
		this.market = market;
	}

	public TradeStacTradeConnection getTradeConnection() {
		return tradeConnection;
	}

	public void setTradeConnection(TradeStacTradeConnection tradeConnection) {
		this.tradeConnection = tradeConnection;
	}

	public void setPreTradeConnectionMarketStatus(ConnectionStatus preTradeConnectionMarketStatus) {
		this.preTradeConnectionMarketStatus = preTradeConnectionMarketStatus;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public MarketMakerFinder getMarketMakerFinder() {	
		return this.marketMakerFinder;
	}

	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}

	public void setSerialNumberService(SerialNumberService serialNumberService) {
		this.serialNumberService = serialNumberService;
	}

	public void setOperationRegistry(OperationRegistry operationRegistry) {
		this.operationRegistry = operationRegistry;
	}

	public OperationRegistry getOperationRegistry() {
		return this.operationRegistry;
	}

	public TradeStacPreTradeConnection getTradeStacPreTradeConnection() {
		return preTradeConnection;
	}

	public void setTradeStacPreTradeConnection(TradeStacPreTradeConnection preTradeConnection) {
		this.preTradeConnection = preTradeConnection;
	}

	public MarketAxessMarket() {
		// nothing to do
	}

	@Override
	public void init() throws BestXException {
		checkPreRequisites();
		connectionListener = new HashMap<String, ConnectionListener>();
		market = marketFinder.getMarketByCode(marketCode, null);

		JobExecutionDispatcher.INSTANCE.addTimerEventListener(this.getClass().getSimpleName(), this);

		CommonMetricRegistry.INSTANCE.registerHealtCheck(this);

		super.init();
	}


	private void checkPreRequisites() {
		if (preTradeConnection == null) {
			throw new ObjectNotInitializedException("price connection not set");
		}
		if (tradeConnection == null) {
			throw new ObjectNotInitializedException("Buy side connection not set");
		}
		if (marketMakerFinder == null) {
			throw new ObjectNotInitializedException("Market maker finder not set");
		}
		if (marketFinder == null) {
			throw new ObjectNotInitializedException("Market finder not set");
		}
		if (venueFinder == null) {
			throw new ObjectNotInitializedException("Venue finder not set");
		}
		if (serialNumberService == null) {
			throw new ObjectNotInitializedException("Serial Number Service not set");
		}
		if (instrumentFinder == null) {
			throw new ObjectNotInitializedException("Instrument finder not set");
		}
		if (operationRegistry == null) {
			throw new ObjectNotInitializedException("Operation registry not set");
		}
		if (proposalDiscarders == null) {
			throw new ObjectNotInitializedException("Proposal Discarders not set");
		}
		if (executor == null) {
			throw new ObjectNotInitializedException("Executor not set");
		}	
	}

	@Override
	public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
		LOGGER.info("[{}] {}", connectionName, connectionStatus);
	}
	@Override
	public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
		LOGGER.info("[{}] {}", connectionName, connectionStatus);

		if (preTradeConnection != null && connectionName.equals(preTradeConnection.getConnectionName())) {
			preTradeConnectionMarketStatus = connectionStatus;
		} else if (tradeConnection != null && connectionName.equals(tradeConnection.getConnectionName())) {
			tradeConnectionMarketStatus = connectionStatus;
		} else {
			LOGGER.warn("Unexpected connectionName [{}], connectionStatus = {}", connectionName, connectionStatus);
		}
	}

	@Override
	public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal,
			ClassifiedProposal bidClassifiedProposal) {
		LOGGER.debug("{}, {}, {}", instrument, askClassifiedProposal, bidClassifiedProposal);

		String isin = instrument.getIsin();
		ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);

		if (proposalAggregator != null) {
			try {
				proposalAggregator.onProposal(askClassifiedProposal);

				//[DR20150409] La risposta MarketAxess non include nessun orderID, utilizziamo l'ISIN così come è stato utilizzato nella pricesRequested(isin);
				marketStatistics.pricesResponseReceived(isin, 1);
			} catch (BestXException e) {
				LOGGER.error("Error managing classifiedProposal {}: {}", askClassifiedProposal, e.getMessage(), e);
			}

			try {
				proposalAggregator.onProposal(bidClassifiedProposal);

				//[DR20150409] La risposta MarketAxess  non include nessun orderID, utilizziamo l'ISIN così come è stato utilizzato nella pricesRequested(isin);
				marketStatistics.pricesResponseReceived(isin, 1);
			} catch (BestXException e) {
				LOGGER.error("Error managing classifiedProposal {}: {}", bidClassifiedProposal, e.getMessage(), e);
			}
		} else {
			LOGGER.info("Error managing classifiedProposals: unable to retrieve a valid proposal aggregator for isin {}", isin);
		}
	}

	@Override
	public void onSecurityListCompleted(Set<String> securityList) {
		// do nothing
		;
	}

	@Override
	public void cleanBook() {
	}

	@Override
	public int countOrders() {
		int result= 0;
		for (ProposalAggregator propAggr : proposalAggregatorMap.values()) {
			result += propAggr.getProposalAggregatorListeners().size();
		}
		return result;
	}

	@Override
	public int getActiveTimersNum() {
		return 0;
	}

	@Override
	public String getConnectionName() {
		return getMarketCode().name();
	}

	@Override
	public void connect() throws BestXException {
		startPriceConnection();
		startBuySideConnection();
	}

	@Override
	public void disconnect() throws BestXException {
		stopPriceConnection();
		stopBuySideConnection();
	}

	@Override
	public boolean isConnected() {
		return (!isPriceConnectionProvided() || isPriceConnectionAvailable() )&& (!isBuySideConnectionProvided() || isBuySideConnectionAvailable());
	}

	@Override
	public void setConnectionListener(ConnectionListener listener) {
		connectionListener.put(((ConnectionHelper) listener).getConnectionName(), listener);	
	}

	@Override
	public void timerExpired(String jobName, String groupName) {
		if (!jobName.contains("@")) {
			LOGGER.warn("Timer {}-{} expired, but cannot be managed by the Proposal Aggregator", jobName, groupName);
			return;
		}
		LOGGER.info("Timer {}-{} expired, start to manage it", jobName, groupName);

		String isin = jobName.split("@")[0];
		String orderID = jobName.split("@")[1];

		ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);

		if (proposalAggregator != null) {
			ProposalAggregatorListener proposalAggregatorListener = proposalAggregator.getProposalAggregatorListener(orderID);

			if (proposalAggregatorListener != null) {
				executor.execute(new TimerExpiredWorker(proposalAggregatorListener));
			} else {
				LOGGER.info("ProposalAggregatorListener not found for {}@{}, skip processing", orderID, isin);
			}
		} else {
			LOGGER.info("Unable to retrieve a valid proposalAggregator for {}@{}, skip processing", orderID, isin);
		}
	}

	@Override
	public void ensurePriceAvailable() throws MarketNotAvailableException {
		if (!isPriceConnectionAvailable()) {
			throw new MarketNotAvailableException(marketCode+" not connected");
		}
	}


	@Override
	public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order) throws BestXException {
		LOGGER.debug("orderID = {}, venues = {}, maxLatency = {}", (order != null ? order.getFixOrderId() : order), venues, maxLatency);

		Instrument instrument = order.getInstrument();
		String isin = instrument.getIsin();

		LOGGER.debug("Requesting price to {} for ISIN: {}", this.getMarketCode(), isin);
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

						if (marketMarketMaker.getMarket().getMarketCode() == getMarketCode() && canTrade) {
							if (!targetMarketMarketMakers.contains(marketMarketMaker)) {
								targetMarketMarketMakers.add(marketMarketMaker);
								LOGGER.debug("Added marketMarketMaker: {}", marketMarketMaker.getMarketSpecificCode());
							}
						} else {
							for (ProposalDiscarder proposalDiscarder : proposalDiscarders) {
								boolean correctMmarketCode = proposalDiscarder.getMarketCode() == marketMarketMaker.getMarket().getMarketCode();
								boolean isPriceDiscoveryEnabled = proposalDiscarder.isEnabled();
								// if the market maker we are checking is not
								// the one belonging to the proposal discarder
								// market
								// we must not find out if he can trade the
								// instrument, it will only waste time.
								// We must add it to those we will request
								// prices to.
								if (!correctMmarketCode) {
									for (MarketMarketMaker mmm : venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode())) {
										if (!targetMarketMarketMakers.contains(mmm)) {
											targetMarketMarketMakers.add(mmm);
											LOGGER.debug("Added MarketMarketMaker: {}", mmm.getMarketSpecificCode());
										}
									}

									continue;
								}
								boolean isInstrumentTradableWithMarketMaker = proposalDiscarder.isInstrumentTradableWithMarketMaker(instrument, marketMarketMaker);
								for (MarketMarketMaker mmm : venue.getMarketMaker().getMarketMarketMakerForMarket(this.getMarketCode())) {
									if (!targetMarketMarketMakers.contains(mmm)) {
										targetMarketMarketMakers.add(mmm);
										LOGGER.debug("[{}] Added MarketMarketMaker: {}", this.getMarketCode(), mmm.getMarketSpecificCode());
									}

									if (!canTrade || !isInstrumentTradableWithMarketMaker || !isPriceDiscoveryEnabled) {
										if (!isInstrumentTradableWithMarketMaker) {
											order.addMarketMakerNotQuotingInstr(mmm, "No trading relation");
										}
									}
								}

							}
						}
					}
				}
			}

			LOGGER.info("[{}] Order {}, registering the price request to the proposal aggregator (isin = {})", this.getMarketCode(), fixOrderId, isin);
			String reason = Messages.getString("RejectProposalISINNotQuotedByMM");

			for (MarketMarketMaker targetMarketMarketMaker : targetMarketMarketMakers) {
				order.addMarketMakerNotQuotingInstr(targetMarketMarketMaker, reason);
			}

			// Retrieve all the marketSpecificCodes
			List<String> marketMakers = new ArrayList<String>();
			List<MarketMarketMakerSpec> marketSpecificCodes = new ArrayList<MarketMarketMakerSpec>(targetMarketMarketMakers.size());
			for (MarketMarketMaker marketMarketMaker : targetMarketMarketMakers) {
				marketSpecificCodes.add(new MarketMarketMakerSpec(marketMarketMaker.getMarketSpecificCode(),marketMarketMaker.getMarketSpecificCodeSource()));
				marketMakers.add(marketMarketMaker.getMarketSpecificCode());
			}
			LOGGER.info("Order {}. Market Makers that will be enquired for prices: {}", fixOrderId, marketSpecificCodes);

			// Book aggregator
			ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);
			if (proposalAggregator == null) {
				proposalAggregator = new ProposalAggregator(instrument);
				proposalAggregatorMap.put(isin, proposalAggregator);
			}

			String orderID = order.getFixOrderId();

			ProposalAggregatorListener proposalAggregatorListener = new OrderPriceManager(orderID, marketCode, marketMakers, proposalAggregator, listener);
			proposalAggregator.addProposalAggregatorListener(proposalAggregatorListener);

			String timerName = SimpleMarketProposalAggregator.buildTimerName(isin, order.getFixOrderId(), getMarketCode());

			try {
				SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
				JobDetail newJob = simpleTimerManager.createNewJob(timerName, this.getClass().getSimpleName(), false /* no durable flag required */, true /* request recovery */, true /* monitorable */);
				Trigger trigger = null;
				if (targetMarketMarketMakers.size() > 0 && maxLatency > 0) {
					// this timer is not repeatable
					trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, maxLatency);
				} else {
					// Dopo 10 secondi fa partire la onTimerExpired
					// this timer is not repeatable
					trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, 10000);
				}
				simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, true);
			} catch (SchedulerException e) {
				LOGGER.error("Error while scheduling price discovery wait timer: {}", e.getMessage(), e);
			}

			// [BXMNT-430] marketSpecificCodes * 2 (BID e ASK)
			marketStatistics.pricesRequested(isin, marketSpecificCodes.size() * 2);

//			marketSpecificCodes.remove(7);
//			marketSpecificCodes.remove(0);


			preTradeConnection.requestInstrumentPriceSnapshot(instrument, marketSpecificCodes);

		} catch (ConcurrentModificationException cme) {
			LOGGER.error("Error while starting the price requests towards {}: {}", this.getMarketCode(), cme.getMessage(), cme);
		}
	}
	@Override
	public Market getQuotingMarket(Instrument instrument) throws BestXException {
		return marketFinder.getMarketByCode(this.getMarketCode(), null);
	}

	@Override
	public Date getMarketInstrumentSettlementDate(Instrument instrument) throws BestXException {
		return instrument.getDefaultSettlementDate(); // no inventory from market, assuming standard settlement date
	}

	@Override
	public QuotingStatus getInstrumentQuotingStatus(Instrument instrument) throws BestXException {
		return QuotingStatus.NEG;  // no inventory from market, assuming all negotiable
	}

	@Override
	public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
		return true; // no inventory from market, assuming all negotiable
	}

	@Override
	public void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void sendFokOrder(Operation operation, MarketOrder marketOrder) throws BestXException {
		checkBuySideConnection();
		LOGGER.debug("listener = {}, marketOrder = {}", operation, marketOrder);

		String clOrdID = serialNumberService.getUniqueIdentifier(OperationIdType.MARKETAXESS_CLORD_ID.toString(), "MA" + "-O%010d");

		operationRegistry.bindOperation(operation, OperationIdType.MARKETAXESS_CLORD_ID, clOrdID);

		marketOrder.setMarketSessionId(clOrdID);
		marketStatistics.orderSent(operation.getOrder().getFixOrderId());
		LOGGER.info("[MktReq] Order {}, Send Order to {} - ClOrdID: {}", marketOrder.getFixOrderId(), getMarketCode(), marketOrder.getMarketSessionId());

		tradeConnection.sendOrder(marketOrder);
	}

	private void checkBuySideConnection()  throws MarketNotAvailableException {
		if(!isBuySideConnectionAvailable())
			throw new MarketNotAvailableException(this.market.getName() + " is not connected");
	}

	@Override
	public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();				
	}

	@Override
	public void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();			}

	@Override
	public void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
		throw new UnsupportedOperationException();			}

	@Override
	public void ackProposal(Operation listener, Proposal proposal) throws BestXException {
		throw new UnsupportedOperationException();			}

	@Override
	public void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason) throws BestXException {
		checkBuySideConnection();
		LOGGER.debug("listener = {}, marketOrder = {}, reason = {}", operation, marketOrder, reason);

		String clOrdID = marketOrder.getMarketSessionId();
		LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

		tradeConnection.cancelOrder(marketOrder);
	}

	@Override
	public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout)  throws BestXException {
		checkBuySideConnection();
		LOGGER.debug("listener = {}, marketOrder = {}, reason = {}, sendOrderCancelTimeout = {}", operation, marketOrder, reason, sendOrderCancelTimeout);

		String clOrdID = marketOrder.getMarketSessionId();
		LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

		tradeConnection.cancelOrder(marketOrder);
	}

	@Override
	public void matchOperations(Operation listener, Operation matching, Money ownPrice, Money matchingPrice) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();		
	}

	@Override
	public MarketCode getMarketCode() {
		return this.marketCode;
	}


	@Override
	public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker,
			Date minArrivalDate) {
		// not implemented
		return null;
	}

	@Override
	public void onConnection(Connection source) {
		if(source.getConnectionName().compareTo(preTradeConnection.getConnectionName()) == 0) {
			LOGGER.info("Price  connection of  {} Market Connected", this.getMarket().getName());
			preTradeConnectionMarketStatus = ConnectionStatus.Connected;
		} else 	if(source.getConnectionName().compareTo(tradeConnection.getConnectionName()) == 0) {
			LOGGER.info("Buy Side connection of  {} Market Connected", this.getMarket().getName());
			tradeConnectionMarketStatus = ConnectionStatus.Connected;
		}
	}

	@Override
	public void onDisconnection(Connection source, String reason) {
		if(source.getConnectionName().compareTo(preTradeConnection.getConnectionName()) == 0) {
			LOGGER.info("Price  connection of  {} Market disconnected", this.getMarket().getName());
			preTradeConnectionMarketStatus = ConnectionStatus.NotConnected;
		} else 	if(source.getConnectionName().compareTo(tradeConnection.getConnectionName()) == 0) {
			LOGGER.info("Buy Side connection of  {} Market disconnected", this.getMarket().getName());
			tradeConnectionMarketStatus = ConnectionStatus.NotConnected;
		}
	}

	@Override
	public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
		if (marketMaker == null || instrument == null) {
			throw new IllegalArgumentException("one param is null: marketMaker = " + marketMaker + ", instrument = " + instrument);
		}
		return marketMaker.canTrade(instrument);
	}

	@Override
	public boolean isBuySideConnectionProvided() {
		return tradeConnection != null;
	}

	@Override
	public boolean isPriceConnectionProvided() {
		return preTradeConnection != null;
	}

	@Override
	public boolean isAMagnetMarket() {
		return false;
	}

	@Override
	public boolean isBuySideConnectionAvailable() {
		return tradeConnection != null && tradeConnection.isConnected() && tradeConnectionMarketStatus == ConnectionStatus.Connected;
	}

	@Override
	public boolean isPriceConnectionAvailable() {
		return preTradeConnection != null && preTradeConnection.isConnected() && preTradeConnectionMarketStatus == ConnectionStatus.Connected;
	}

	@Override
	public void onOrderReject(String sessionId, String clOrdId, String reason) {
		 LOGGER.info("Order reject ({}) received from {} : {}", reason, getMarketCode(), sessionId);
		 executor.execute(new OnOrderRejectRunnable(clOrdId, reason, this));
	 }
	

	@Override
	public void onOrderCancelReject(String sessionId, String clOrdId, String ordStatus, String reason) {
	     LOGGER.info("Cancel rejected: sessionId = {}, origClOrdID = {}, ordStatus = {}, reason = {}", sessionId, clOrdId, ordStatus, reason);
	     executor.execute(new OnOrderCancelRejectRunnable(clOrdId, ordStatus, reason, this));
	}

	@Override
	public void onExecutionReport(String sessionId, String clOrdID, MarketExecutionReport executionReport) {
		LOGGER.debug("sessionId = {}, ExecutionReport {}", sessionId, executionReport);
		
		String cleanClOrdId = clOrdID; // leave here to manage when necessary the clean operation on the order id
		String text = executionReport.getText();
		String execType = executionReport.getExecType();
		char ordStatus = executionReport.getOrdStatus();
		BigDecimal lastPrice = executionReport.getLastPx(); 
		// AMC BESTX-313 20170719 added || "".equalsIgnoreCase(text)
		String cleanText = ((text == null || "".equalsIgnoreCase(text)) && ("" + ExecType.CANCELED).equals(execType)) ?  "Cancel requested by BestX!"  : text;
		try {
			final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MARKETAXESS_CLORD_ID, cleanClOrdId);

			if (operation.getOrder() == null) {
				throw new BestXException("Operation order is null");
			}

			MarketMarketMaker mmm = null;
			try {
				mmm = marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), executionReport.getExecBroker());
			} catch (BestXException e) {
				LOGGER.warn("Order {} received execution report with state {} and no Market Maker associated to market dealer code {}",
						operation.getOrder().getFixOrderId(), executionReport.getExecType(), executionReport.getExecBroker());
	        	LOGGER.info("IMPORTANT! MarketAxess returned dealer {} not configured in BestX!. Please configure it", executionReport.getExecBroker());
			}
			if(mmm != null) {
				executionReport.setMarketMaker(mmm.getMarketMaker() == null ? null : mmm.getMarketMaker());
				LOGGER.info("[MktMsg] orderID = {}, ExecutionReport received: original clOrdId={}, OrdStatus={}, ExecType={}, LastPrice={}, ExecutionBroker={}, text={}", 
						operation.getOrder().getFixOrderId(), cleanClOrdId, ordStatus, execType, lastPrice, mmm.getMarketMaker(), cleanText);
			} else 
				LOGGER.info("[MktMsg] orderID = {}, ExecutionReport received: original clOrdId={}, OrdStatus={}, ExecType={}, LastPrice={}, ExecutionBroker={}, text={}", 
						operation.getOrder().getFixOrderId(), cleanClOrdId, ordStatus, execType, lastPrice, executionReport.getExecBroker(), cleanText);
				

			String orderId = operation.getOrder().getFixOrderId();
			
			LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);
			if(!("" + ExecType.NEW).equals(execType) && !("" + ExecType.ORDER_STATUS).equals(execType)) {
   			if(executionReport.getActualQty() != null && BigDecimal.ZERO.compareTo(executionReport.getActualQty()) < 0) {
   				marketStatistics.orderResponseReceived(orderId, executionReport.getActualQty().doubleValue());
   			} else {
   			   marketStatistics.orderResponseReceived(orderId, 0.0);
   			}
			}
			executor.execute(new OnExecutionReportRunnable(operation, this, (MarketAxessExecutionReport) executionReport));

		} catch (OperationNotExistingException e) {
			LOGGER.warn("[MktMsg] Operation not found for clOrdID {} , ignoring ExecutionReport/{}/{}", cleanClOrdId, execType, ordStatus);
		} catch (BestXException e) {
			LOGGER.error("[MktMsg] Exception while handling ExecutionReport/{}/{} for clOrdID {}, ignoring it", execType, ordStatus, cleanClOrdId, e);
		}	
	}
	@Override
	public MarketBuySideConnection getBuySideConnection() {
		return this;
	}

	@Override
	public MarketPriceConnection getPriceConnection() {
		return this;
	}

	@Override
	public synchronized void startPriceConnection() throws BestXException {
		LOGGER.info("Connecting to MarketAxess Price");

		if (preTradeConnection != null && !preTradeConnection.isConnected()) {
			preTradeConnection.connect();
		}
	}

	@Override
	public synchronized void stopPriceConnection() throws BestXException {
		LOGGER.info("Required disconnection to  MarketAxess Price");

		if(preTradeConnection != null && preTradeConnection.isConnected()) {
			preTradeConnection.disconnect();
		} 
		else LOGGER.info("Already disconnected, ignoring it...");
	}

	@Override
	public synchronized void startBuySideConnection() throws BestXException {
		LOGGER.info("Required connection to MarketAxess BuySide");

		if (tradeConnection != null && !tradeConnection.isConnected()) {
			tradeConnection.connect();
		}
		else LOGGER.info("Already connected, ignoring it...");
	}

	@Override
	public synchronized void stopBuySideConnection() throws BestXException {
		LOGGER.info("Disconnecting from MarketAxess BuySide");

		if (tradeConnection != null && tradeConnection.isConnected()) {
			tradeConnection.disconnect();
		}
	}

	@Override
	public void onExecutionReport(String sessionId, String clOrdId,
			it.softsolutions.marketlibraries.quickfixjrootobjects.fields.ExecType execType,
			it.softsolutions.marketlibraries.quickfixjrootobjects.fields.OrdStatus ordStatus,
			BigDecimal accruedInterestAmount, BigDecimal accruedInterestRate, BigDecimal lastPrice, String contractNo,
			Date futSettDate, Date transactTime, String text, String executionBroker) {
		// TODO Auto-generated method stub

	}
}
