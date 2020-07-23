/*
 * Copyright 1997-2012 SoftSolutions! srl 
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
package it.softsolutions.bestx.markets.bloomberg;

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
import it.softsolutions.bestx.connections.bloomberg.tsox.TSOXConnection;
import it.softsolutions.bestx.connections.bloomberg.tsox.TSOXConnectionListener;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.exceptions.ConnectionNotAvailableException;
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
import it.softsolutions.bestx.markets.bloomberg.services.BloombergTradeMatchingService;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.DateService;
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
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix50.TSExecutionReport;

/**
 * 
 * Managed BPipe: TradeStac Implementation: BLPConnector > BLPMarket
 * 
 * Project Name : bestx-bloomberg-market First created by: fabrizio.aponte Creation date: 22/mag/2012
 * 
 **/
@SuppressWarnings("deprecation")
public class BloombergMarket extends MarketCommon implements TradeStacPreTradeConnectionListener, TSOXConnectionListener, ConnectionListener, MarketBuySideConnection, MarketPriceConnection, TimerEventListener, Connection, MarketMXBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(BloombergMarket.class);

	public static final String BBG_TRADE = "BBG_TRADE_SEQNO";

	// Price Connection
	private TradeStacPreTradeConnection tradeStacPreTradeConnection;

	public void setTradeStacPreTradeConnection(TradeStacPreTradeConnection tradeStacPreTradeConnection) {
		this.tradeStacPreTradeConnection = tradeStacPreTradeConnection;
	}

	// BuySide Connection
	private TSOXConnection tsoxConnection;

	private Map<String, ConnectionListener> connectionListener;

	private Market market;
	private Market executionMarket;
	
	private static final Market.MarketCode marketCode = Market.MarketCode.BLOOMBERG;
	private static final Market.MarketCode executionMarketCode = Market.MarketCode.TSOX;

	private MarketMakerFinder marketMakerFinder;
	private VenueFinder venueFinder;
	private MarketFinder marketFinder;
	private InstrumentFinder instrumentFinder;

	private SerialNumberService serialNumberService;
	private OperationRegistry operationRegistry;
	private BloombergTradeMatchingService bloombergTradeMatchingService;

	private List<ProposalDiscarder> proposalDiscarders;
	private List<ConnectionListener> symbioticConnectionListeners;

	private ConnectionStatus blpConnectionMarketStatus = ConnectionStatus.NotConnected;
	private ConnectionStatus tsoxConnectionMarketStatus = ConnectionStatus.NotConnected;

	private Executor executor;

	// <isin, proposalAggregator>
	private Map<String, ProposalAggregator> proposalAggregatorMap = new ConcurrentHashMap<String, ProposalAggregator>();

	// <ClOrdID, DealerCode>
	//    private Map<String, String> dealerCodeMap = new ConcurrentHashMap<String, String>();

	@Override
	public void onClassifiedProposal(Instrument instrument, ClassifiedProposal askClassifiedProposal, ClassifiedProposal bidClassifiedProposal) {
		LOGGER.debug("{}, {}, {}", instrument, askClassifiedProposal, bidClassifiedProposal);

		String isin = instrument.getIsin();
		ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);

		if (proposalAggregator != null) {
			try {
				proposalAggregator.onProposal(askClassifiedProposal);

				// [DR20150409] La risposta Bloomberg non include nessun orderID, utilizziamo l'ISIN così come è stato utilizzato nella pricesRequested(isin); 
				marketStatistics.pricesResponseReceived(isin, 1);
			} catch (BestXException e) {
				LOGGER.error("Error managing classifiedProposal {}: {}", askClassifiedProposal, e.getMessage(), e);
			}

			try {
				proposalAggregator.onProposal(bidClassifiedProposal);

				// [DR20150409] La risposta Bloomberg non include nessun orderID, utilizziamo l'ISIN così come è stato utilizzato nella pricesRequested(isin); 
				marketStatistics.pricesResponseReceived(isin, 1);
			} catch (BestXException e) {
				LOGGER.error("Error managing classifiedProposal {}: {}", bidClassifiedProposal, e.getMessage(), e);
			}
		} else {
			LOGGER.info("Error managing classifiedProposals: unable to retrieve a valid proposal aggregator for isin {}", isin);
		}
	}

	private void checkPreRequisites() {
		if (tradeStacPreTradeConnection == null) {
			throw new ObjectNotInitializedException("BLP connection not set");
		}
		if (tsoxConnection == null) {
			// [DR20150428] Do not throw exception here, in some cases TSOX could be disabled
			LOGGER.warn("TSOX connection not set");
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
		if (tsoxConnection != null && bloombergTradeMatchingService == null) {
			LOGGER.warn("Trade Matching service not set");
			// throw new ObjectNotInitializedException("Trade Matching service not set");
		}
		if (proposalDiscarders == null) {
			throw new ObjectNotInitializedException("Proposal Discarders not set");
		}
		if (symbioticConnectionListeners == null) {
			throw new ObjectNotInitializedException("SymbioticConnectionListeners not set");
		}
		if (executor == null) {
			throw new ObjectNotInitializedException("Executor not set");
		}
	}

	@Override
	public void init() throws BestXException {
		checkPreRequisites();
		connectionListener = new HashMap<String, ConnectionListener>();
		market = marketFinder.getMarketByCode(marketCode, null);
		executionMarket = marketFinder.getMarketByCode(executionMarketCode, null);

		JobExecutionDispatcher.INSTANCE.addTimerEventListener(BloombergMarket.class.getSimpleName(), this);

		CommonMetricRegistry.INSTANCE.registerHealtCheck(this);

		super.init();
	}

	@Override
	public synchronized void startPriceConnection() throws BestXException {
		LOGGER.info("Connecting to Bloomberg Price");

		tradeStacPreTradeConnection.connect();
	}

	@Override
	public synchronized void stopPriceConnection() throws BestXException {
		LOGGER.info("Disconnecting from Bloomberg Price");

		if (symbioticConnectionListeners != null) {
			for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
				((MarketMXBean) symbioticConnectionListener).disablePriceConnection();
			}
		}

		tradeStacPreTradeConnection.disconnect();
	}

	@Override
	public synchronized void startBuySideConnection() throws BestXException {
		LOGGER.info("Connecting to Bloomberg BuySide");

		if (tsoxConnection != null) {
			tsoxConnection.connect();

			if (bloombergTradeMatchingService != null) {
				bloombergTradeMatchingService.start();
			}
		} else {
			LOGGER.info("Unable to start BuySide connection: tsoxConnection is null");
		}
	}

	// [DR20120508] stopBuySideConnection should Disconnect also the "Price" connection (TradeStac)
	@Override
	public synchronized void stopBuySideConnection() throws BestXException {
		LOGGER.info("Disconnecting from Bloomberg BuySide");

		if (tsoxConnection != null) {
			tsoxConnection.disconnect();

			if (bloombergTradeMatchingService != null) {
				bloombergTradeMatchingService.stop();
			}
		} else {
			LOGGER.info("Unable to stop BuySide connection: tsoxConnection is null");
		}
	}

	@Override
	public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
		LOGGER.info("[{}] {}", connectionName, connectionStatus);

		if (connectionName.equals(tradeStacPreTradeConnection.getConnectionName())) {
			switch (connectionStatus) {
			case Connected:
				if (blpConnectionMarketStatus == ConnectionStatus.Connected) {
					for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
						symbioticConnectionListener.onConnection(tradeStacPreTradeConnection);
					}
				}
				break;
			case Stopped:
			case NotConnected:
				for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
					symbioticConnectionListener.onDisconnection(tradeStacPreTradeConnection, "Price ConnectionStatus = " + connectionStatus);
				}

				break;
			case Started:
			case InProcess:
			default:
				// doNothing
				break;
			}
		}
	}

	@Override
	public void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
		LOGGER.info("[{}] {}", connectionName, connectionStatus);

		if (connectionName.equals(tradeStacPreTradeConnection.getConnectionName())) {
			blpConnectionMarketStatus = connectionStatus;
			switch (connectionStatus) {
			case Connected:
				for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
					symbioticConnectionListener.onConnection(tradeStacPreTradeConnection);
				}

				break;
			case NotConnected:
				for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
					symbioticConnectionListener.onDisconnection(tradeStacPreTradeConnection, "Market ConnectionStatus = " + connectionStatus);
				}

				break;
			case Stopped:
				for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
					symbioticConnectionListener.onDisconnection(tradeStacPreTradeConnection, "Market ConnectionStatus = " + connectionStatus);
				}

				break;
			case Started:
			case InProcess:
				// doNothing
				break;
			default:
				LOGGER.warn("connectionStatus {} not managed", connectionStatus);
				break;
			}
		} else if (tsoxConnection != null && connectionName.equals(tsoxConnection.getConnectionName())) {
			tsoxConnectionMarketStatus = connectionStatus;
		} else {
			LOGGER.warn("Unexpected connectionName [{}], connectionStatus = {}", connectionName, connectionStatus);
		}
	}

	@Override
	public int countOrders() {
		int result = 0;

		return result;
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

						if (marketMarketMaker.getMarket().getMarketCode() == getMarketCode() && canTrade) {
							if (!targetMarketMarketMakers.contains(marketMarketMaker)) {
								targetMarketMarketMakers.add(marketMarketMaker);
								LOGGER.debug("Added marketMarketMaker: {}", marketMarketMaker.getMarketSpecificCode());
							}
						} else {
							for (ProposalDiscarder proposalDiscarder : proposalDiscarders) {
								boolean correctMmarketCode = proposalDiscarder.getMarketCode() == marketMarketMaker.getMarket().getMarketCode();
								boolean isPriceDiscoveryEnabled = proposalDiscarder.isEnabled();
								// if the market maker we are checking is not the one belonging to the proposal discarder market
								// we must not find out if he can trade the instrument, it will only waste time.
								// We must add it to those we will request prices to.
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
								for (MarketMarketMaker mmm : venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode())) {
									if (!targetMarketMarketMakers.contains(mmm)) {
										targetMarketMarketMakers.add(mmm);
										LOGGER.debug("Added MarketMarketMaker: {}", mmm.getMarketSpecificCode());
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
			ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);
			if (proposalAggregator == null) {
				proposalAggregator = new ProposalAggregator(instrument);
				proposalAggregatorMap.put(isin, proposalAggregator);
			}

			String orderID = order.getFixOrderId();
			List<String> marketMakers = marketSpecificCodes;

			ProposalAggregatorListener proposalAggregatorListener = new OrderPriceManager(orderID, marketCode, marketMakers, proposalAggregator, listener);
			proposalAggregator.addProposalAggregatorListener(proposalAggregatorListener);

			String timerName = SimpleMarketProposalAggregator.buildTimerName(isin, order.getFixOrderId(), getMarketCode());

			try {
				SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
				JobDetail newJob = simpleTimerManager.createNewJob(timerName, this.getClass().getSimpleName(), false /* no durable flag required*/, true /* request recovery*/, true /* monitorable */);
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

			tradeStacPreTradeConnection.requestInstrumentPriceSnapshot(instrument, marketSpecificCodes);

		} catch (ConcurrentModificationException cme) {
			LOGGER.error("Error while starting the price requests towards BBG: {}", cme.getMessage(), cme);
		}
	}

	@Override
	public Market getQuotingMarket(Instrument instrument) throws BestXException {
		return marketFinder.getMarketByCode(MarketCode.BLOOMBERG, null);
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
	public void sendRfq(Operation operation, MarketOrder marketOrder) throws BestXException {

		if (tsoxConnectionMarketStatus != ConnectionStatus.Connected) {
			throw new ConnectionNotAvailableException("TSOX not connected");
		}

		// uses a new QuoteReqID for each rfq, remove if exists
		//        String clOrdID = operation.getIdentifiers().get(OperationIdType.TSOX_CLORD_ID);
		//        if (clOrdID != null) {
		//            operationRegistry.removeOperationBinding(operation, OperationIdType.TSOX_CLORD_ID);
		//        }

		//should not be needed to remove, setting again the identifier must overwrite the previous one
		String clOrdID = serialNumberService.getUniqueIdentifier(OperationIdType.TSOX_CLORD_ID.toString(), "TSOX" + "-O%010d");

		operationRegistry.bindOperation(operation, OperationIdType.TSOX_CLORD_ID, clOrdID);

		marketOrder.setMarketSessionId(clOrdID);

		marketStatistics.orderSent(operation.getOrder().getFixOrderId());
		LOGGER.info("[MktReq] Order {}, Send RFQ to {} - ClOrdID: {}", marketOrder.getFixOrderId(), getMarketCode(), marketOrder.getMarketSessionId());

		//        String dealerCode = marketOrder.getMarketMarketMaker().getMarketSpecificCode();
		//        dealerCodeMap.put(clOrdID, dealerCode);

		tsoxConnection.sendRfq(marketOrder);
	}

	@Override
	public void sendFokOrder(Operation operation, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendFasOrder(Operation operation, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void sendSubjectOrder(Operation operation, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void acceptProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
		String quoteId = proposal.getSenderQuoteId();
		String clOrdID = operation.getIdentifier(OperationIdType.TSOX_CLORD_ID);
		LOGGER.info("[MktReq] Order {}, Sending QuoteResponse/HitLift - ClOrdID={}, QuoteID={}", operation.getOrder().getFixOrderId(), clOrdID, quoteId);

		// clOrdID is the one generated in the RFQ (NewOrderSingle), generated by buy side
		// quoteID is taken from proposal (Quote)
		// quoteRespID is new (currentTimeMillis)
		tsoxConnection.acceptProposal(operation, instrument, proposal);
	}

	@Override
	public void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
		String quoteId = proposal.getSenderQuoteId();
		String clOrdID = proposal.getQuoteReqId(); // got from proposal so it is surely the last one, ok? operation.getIdentifiers().get(OperationIdType.TSOX_CLORD_ID);
		LOGGER.info("[MktReq] Order {}, Sending QuoteResponse/Pass - ClOrdID={}, QuoteID={}", operation.getOrder().getFixOrderId(), clOrdID, quoteId);

		// clOrdID is the one generated in the RFQ (NewOrderSingle), generated by buy side
		// quoteID is taken from proposal (Quote)
		// quoteRespID is new (currentTimeMillis)
		tsoxConnection.rejectProposal(operation, instrument, proposal);
	}

	@Override
	public void ackProposal(Operation op, Proposal proposal) throws BestXException {
		String quoteReqId = getOperationTSoxOrderSessionId(op);
		String quoteId = proposal.getSenderQuoteId();

		LOGGER.info("Send quote ack - QuoteReqId: {} QuoteId {}", quoteReqId, quoteId);

		tsoxConnection.ackProposal(proposal);
	}

	@Override
	public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason) throws BestXException {
		if (tsoxConnectionMarketStatus != ConnectionStatus.Connected) {
			throw new ConnectionNotAvailableException("TSOX not connected");
		}
		LOGGER.debug("listener = {}, marketOrder = {}, reason = {}", operation, marketOrder, reason);

		String clOrdID = marketOrder.getMarketSessionId();
		LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

		this.tsoxConnection.cancelOrder(marketOrder);
	}

	@Override
	public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
		if (tsoxConnectionMarketStatus != ConnectionStatus.Connected) {
			throw new ConnectionNotAvailableException("TSOX not connected");
		}
		LOGGER.debug("listener = {}, marketOrder = {}, reason = {}, sendOrderCancelTimeout = {}", operation,
				marketOrder, reason, sendOrderCancelTimeout);

		String clOrdID = marketOrder.getMarketSessionId();
		LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

		this.tsoxConnection.cancelOrder(marketOrder);
	}

	@Override
	public void matchOperations(Operation operation, Operation matching, Money ownPrice, Money matchingPrice) throws BestXException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void requestOrderStatus(Operation operation, MarketOrder marketOrder) throws BestXException {
		throw new UnsupportedOperationException();
	}

	@Override
	public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
		LOGGER.debug("Match Order: {} to stored MarketExecutionReport", (order != null ? order.getFixOrderId() : order));

		if (bloombergTradeMatchingService != null) {
			return bloombergTradeMatchingService.matchTrade(order, executionPrice, marketMaker, minArrivalDate);
		} else {
			LOGGER.warn("tradeMatchingService not avaliable, matchingTrade can not be retrieved for [{}]", order);
			return null;
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
	public MarketCode getMarketCode() {
		return MarketCode.BLOOMBERG;
	}

	@Override
	public boolean isBuySideConnectionProvided() {
		return tsoxConnection != null;
	}

	@Override
	public boolean isPriceConnectionProvided() {
		return tradeStacPreTradeConnection != null;
	}

	@Override
	public boolean isBuySideConnectionAvailable() {
		return tsoxConnection != null && tsoxConnection.isConnected() && tsoxConnectionMarketStatus == ConnectionStatus.Connected;
	}

	@Override
	public boolean isPriceConnectionAvailable() {
		return tradeStacPreTradeConnection.isConnected() && blpConnectionMarketStatus == ConnectionStatus.Connected;
	}

	@Override
	public boolean isAMagnetMarket() {
		return false;
	}

	@Override
	public void onConnection(Connection source) {
		LOGGER.info("Bloomberg Market Connected");

		for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
			symbioticConnectionListener.onConnection(source);
		}
	}

	@Override
	public void onDisconnection(Connection source, String reason) {
		LOGGER.info("Bloomberg Market Disconnected");

		for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
			symbioticConnectionListener.onDisconnection(source, reason);
		}
	}

	public void setOperationRegistry(OperationRegistry operationRegistry) {
		this.operationRegistry = operationRegistry;
	}

	public void setProposalDiscarders(List<ProposalDiscarder> proposalDiscarders) {
		this.proposalDiscarders = proposalDiscarders;
	}

	public void setConnectionListener(Map<String, ConnectionListener> connectionListener) {
		this.connectionListener = connectionListener;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
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

	public void setSymbioticConnectionListeners(List<ConnectionListener> symbioticConnectionListeners) {
		this.symbioticConnectionListeners = symbioticConnectionListeners;
	}

	public void setTradeMatchingService(BloombergTradeMatchingService tradeMatchingService) {
		this.bloombergTradeMatchingService = tradeMatchingService;
	}

	public void setBlpConnection(TradeStacPreTradeConnection blpConnection) {
		this.tradeStacPreTradeConnection = blpConnection;
	}

	public void setTsoxConnection(TSOXConnection tsoxConnection) {
		this.tsoxConnection = tsoxConnection;
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
	public void onCounter(String sessionId, String quoteReqId, String quoteRespId, String quoteId, String marketMaker, BigDecimal price, BigDecimal qty, String currency, ProposalSide side,
			ProposalType type, Date futSettDate, int acknowledgeLevel, String onBehalfOfCompID) throws OperationNotExistingException, BestXException {
		LOGGER.info("counter price ({}) received : {} {}", side, sessionId, marketMaker);
		LOGGER.info(
				"sessionId = {}, quoteReqId = {}, quoteRespId = {}, quoteId = {}, marketMaker = {}, price = {}, qty = {}, currency = {}, side = {}, type = {}, futSettDate = {}, acknowledgeLevel = {}, onBehalfOfCompID = {}",
				sessionId, quoteReqId, quoteRespId, quoteId, marketMaker, price, qty, currency, side, type, futSettDate, acknowledgeLevel, onBehalfOfCompID);

		//[RR20131002] BXMNT-313 this call can throw an exception. It will be handled by the caller in order to send a quote reject to the market
		Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TSOX_CLORD_ID, quoteReqId);

		// operation cannot be null (OperationNotExistingException returned by OperationRegistry in that case)
		if (operation.getOrder() != null) {
			LOGGER.info("[MktMsg] OrderID {}, Quote received from {}-{} , QuoteReqID={}, QuoteRespID={}, QuoteID={}, price={}, FutSettDate={}", operation.getOrder().getFixOrderId(), getMarketCode(), marketMaker, quoteReqId, quoteRespId, quoteId, price, futSettDate);

			if ((operation.getOrder().getSide() == OrderSide.BUY && side == ProposalSide.ASK) || (operation.getOrder().getSide() == OrderSide.SELL && side == ProposalSide.BID)) {
				try {
					// #####################################################################
					// 	 [DR20131126] Remove this!!! Workaround for TSOX Test environment
					//                	if (dealerCodeMap.containsKey(quoteReqId)) {
					//                		String dealerCode = dealerCodeMap.get(quoteReqId);
					//                		LOGGER.warn("WARNING!!! Order {}: dealerCode [{}] replaced with [{}] in order to permit return from TSOX testing", operation.getOrder().getFixOrderId(), marketMaker, dealerCode);
					//                		marketMaker = dealerCode;
					//                	}
					// #####################################################################

					MarketMarketMaker mmm = marketMakerFinder.getMarketMarketMakerByCode(getMarketCode(), marketMaker);
					if (mmm == null) {
						LOGGER.warn("Could not retrieve market market maker, ignoring counter from {} {}", getMarketCode(), marketMaker);
						return;
					}

					Market mkt = marketFinder.getMarketByCode(getMarketCode(), null);
					if (mkt == null) {
						LOGGER.warn("Could not retrieve market, ignoring counter from {} {}", getMarketCode(), marketMaker);
						return;
					}

					Venue venue = venueFinder.getMarketMakerVenue(mmm.getMarketMaker());
					if (venue == null) {
						LOGGER.warn("Could not retrieve venue, ignoring counter from {} {}", getMarketCode(), marketMaker);
						return;
					}

					ClassifiedProposal proposal = new ClassifiedProposal();
					proposal.setProposalState(ProposalState.NEW);
					proposal.setSide(side);
					proposal.setType(type);
					proposal.setTimestamp(DateService.newLocalDate()); // FIXIT retrieve info from message
					proposal.setPrice(new Money(currency, price));
					proposal.setQty(qty);
					proposal.setMarket(mkt);
					proposal.setMarketMarketMaker(mmm);
					proposal.setVenue(venue);
					proposal.setFutSettDate(futSettDate);
					proposal.setNonStandardSettlementDateAllowed(false);
					proposal.setSenderQuoteId(quoteId);
					proposal.setOnBehalfOfCompID(onBehalfOfCompID);
					proposal.setQuoteReqId(quoteReqId);

					executor.execute(new OnCounterRunnable(this, quoteReqId, quoteId, proposal));
				} catch (BestXException e) {
					LOGGER.error("Error in counter handling, ignoring counter from {} {}", getMarketCode(), marketMaker, e);
				}
			} else {
				LOGGER.info("Ignoring counter from {} {}, order side = {}, counter side = {}", getMarketCode(), marketMaker, operation.getOrder().getSide(), side);
			}

		} else if (operation.getOrder() == null) {
			LOGGER.warn("Operation with quoteReqId {} has null order, ignoring counter from {} {}", quoteReqId, getMarketCode(), marketMaker);
		}

	}

	@Override
	public void onExecutionReport(String sessionId, String clOrdID, TSExecutionReport tsExecutionReport) {
		ExecType execType = tsExecutionReport.getExecType();
		OrdStatus ordStatus= tsExecutionReport.getOrdStatus();
		// ClordID : set when creating and sending the original enquiry
		try {
			final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TSOX_CLORD_ID, clOrdID);
			if (operation.getOrder() == null) {
				throw new BestXException("Operation order is null");
			}
			String orderId = operation.getOrder().getFixOrderId();
			LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);

			PriceType priceType = tsExecutionReport.getPriceType();
			BigDecimal lastPrice= null;
			if(priceType != null && priceType == PriceType.Percentage && tsExecutionReport.getLastParPrice() != null) {
            lastPrice = new BigDecimal(tsExecutionReport.getLastParPrice().toString());
			} else {
            lastPrice = tsExecutionReport.getLastPx() == null ? null : new BigDecimal(tsExecutionReport.getLastPx().toString());
			}
			String text = tsExecutionReport.getText();
			LOGGER.info("[MktMsg] orderID = {}, ExecutionReport received: clOrdId={}, OrdStatus={}, ExecType={}, LastPrice={}, text={}",
					operation.getOrder().getFixOrderId(), clOrdID, ordStatus, execType, lastPrice, text);

			if(execType != ExecType.New && execType != ExecType.OrderStatus) {
				if (execType != ExecType.Canceled && execType != ExecType.DoneForDay && lastPrice.doubleValue() > 0) {
					marketStatistics.orderResponseReceived(orderId, operation.getOrder().getQty().doubleValue());
				} else {
					marketStatistics.orderResponseReceived(orderId, 0.0);
				}
			}

			executor.execute(new OnExecutionReportRunnable(operation, this, market, executionMarket, tsExecutionReport, marketMakerFinder));

		} catch (OperationNotExistingException e) {
			LOGGER.warn("[MktMsg] Operation not found for quoteReqID {} , ignoring ExecutionReport/{}/{}", clOrdID, execType, ordStatus);
		} catch (BestXException e) {
			LOGGER.error("[MktMsg] Exception while handling ExecutionReport/{}/{} for quoteReqID {}, ignoring it", execType, ordStatus, clOrdID, e);
		}
	}

	@Override
	public void onOrderReject(final String sessionId, String quoteReqId, final String reason) {
		LOGGER.info("Order reject ({}) received from {} : {}", reason, getMarketCode(), sessionId);
		executor.execute(new OnOrderRejectRunnable(quoteReqId, reason, this));
	}

	@Override
	public void onQuoteStatusTimeout(String sessionId, String quoteReqId, String quoteId, String dealer, String text) {
		LOGGER.info("Dealer ({}) QuoteStatus/Timeout received from {} : {} text: [{}] quoteID: [{}]", dealer, getMarketCode(), sessionId, text, quoteId);
		executor.execute(new OnQuoteStatusTimeoutRunnable(this, quoteReqId, quoteId, dealer, text));
	}


	@Override
	public void onQuoteStatusTradeEnded(String sessionId, String quoteReqId, String quoteId, String dealer, String text) {
		LOGGER.info("Dealer ({}) QuoteStatus/TradeEnded received from {} : {} text: [{}] quoteID: [{}]", dealer, getMarketCode(), sessionId, text, quoteId);
		executor.execute(new OnQuoteStatusTradeEndedRunnable(this, quoteReqId, quoteId, dealer, text));
	}

	@Override
	public void onQuoteStatusExpired(String sessionId, String quoteReqId, String quoteId, String dealer) {
		LOGGER.info("Dealer ({}) QuoteStatus/Expired received from {} : {} quoteID: [{}]", dealer, getMarketCode(), sessionId, quoteId);
		executor.execute(new OnQuoteSubjectRunnable(quoteReqId, quoteId, this));
	}

	protected String getOperationTSoxOrderSessionId(Operation operation) throws BestXException {
		String sessionId = operation.getIdentifier(OperationIdType.TSOX_CLORD_ID);
		if (sessionId == null) {
			sessionId = serialNumberService.getUniqueIdentifier(OperationIdType.TSOX_CLORD_ID.toString(), "TSOX" + "-O%010d");
			operationRegistry.bindOperation(operation, OperationIdType.TSOX_CLORD_ID, sessionId);
		}

		return sessionId;
	}


	/**
	 * Sets the executor.
	 * 
	 * @param executor
	 *            the new executor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void cleanBook() {

	}

	public OperationRegistry getOperationRegistry() {
		return operationRegistry;
	}

	@Override
	public int getActiveTimersNum() {
		return 0;
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
	public void onSecurityListCompleted(Set<String> securityList) {
		// Bloomberg does not implement securityList message
		LOGGER.error("call to not implemented method onSecurityListCompleted");
	}

	@Override
	public void onCancelReject(String sessionId, String quoteReqId, String reason) {
	    LOGGER.info("Cancel rejected: sessionId = {}, origClOrdID = {}, reason = {}", sessionId, quoteReqId, reason);
		executor.execute(new OnOrderCancelRejectRunnable(quoteReqId, reason, this));
	}

}
