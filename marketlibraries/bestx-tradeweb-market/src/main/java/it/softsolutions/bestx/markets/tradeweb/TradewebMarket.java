/*
 * Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.markets.tradeweb;

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
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnection;
import it.softsolutions.bestx.connections.tradestac.TradeStacPreTradeConnectionListener;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnection;
import it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressConnectionListener;
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
import it.softsolutions.bestx.markets.tradeweb.services.TradewebTradeMatchingService;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutablePrice;
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
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
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
import it.softsolutions.tradestac.api.ConnectionStatus;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix.field.PartyIDSource;
import it.softsolutions.tradestac.fix.field.PartyRole;
import it.softsolutions.tradestac.fix.field.PriceType;
import it.softsolutions.tradestac.fix50.TSExecutionReport;
import it.softsolutions.tradestac.fix50.TSNoPartyID;
import it.softsolutions.tradestac.fix50.component.TSCompDealersGrpComponent;
import quickfix.DoubleField;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.IntField;
import quickfix.MessageComponent;
import quickfix.StringField;
import quickfix.field.CompDealerID;
import quickfix.field.CompDealerParQuote;
import quickfix.field.CompDealerQuote;
import quickfix.field.CompDealerQuotePriceType;
import quickfix.field.CompDealerStatus;
import tw.quickfix.field.MiFIDMIC;
/**
 * 
 * @author Davide Rossoni
 * 
 * Project Name : bestx-tradeweb-market First created by: davide.rossoni
 * Creation date: 19/dec/2014
 * 
 **/
public class TradewebMarket extends MarketCommon
      implements TradeStacPreTradeConnectionListener, TradeXpressConnectionListener, ConnectionListener, MarketBuySideConnection, MarketPriceConnection, TimerEventListener, Connection, MarketMXBean {

   private static final Logger LOGGER = LoggerFactory.getLogger(TradewebMarket.class);

   public static final String TW_TRADE = "TW_TRADE_SEQNO";
   public static final String TW_MAX_EXEC_RETRIES_PROPERTY = "TradwebExecution.MaxRetriesWhenNoAnswers";
   // Price Connection
   private TradeStacPreTradeConnection tradeStacPreTradeConnection;

   // BuySide Connection
   private TradeXpressConnection tradeXpressConnection;

   private Map<String, ConnectionListener> connectionListener;

   private Market market;
   private static final Market.MarketCode marketCode = Market.MarketCode.TW;

   private MarketMakerFinder marketMakerFinder;
   private VenueFinder venueFinder;
   private MarketFinder marketFinder;
   private InstrumentFinder instrumentFinder;

   private SerialNumberService serialNumberService;
   private OperationRegistry operationRegistry;
   private TradewebTradeMatchingService tradewebTradeMatchingService;

   private List<ProposalDiscarder> proposalDiscarders;
   private List<ConnectionListener> symbioticConnectionListeners;

   private ConnectionStatus mdsConnectionMarketStatus = ConnectionStatus.NotConnected;
   private ConnectionStatus tradeXpressConnectionMarketStatus = ConnectionStatus.NotConnected;

   private Executor executor;

   // <isin, proposalAggregator>
   private Map<String, ProposalAggregator> proposalAggregatorMap = new ConcurrentHashMap<String, ProposalAggregator>();

   private Boolean miFIDRestricted;

   // <ClOrdID, DealerCode>
   // private Map<String, String> dealerCodeMap = new ConcurrentHashMap<String,
   // String>();

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
         }
         catch (BestXException e) {
            LOGGER.error("Error managing classifiedProposal {}: {}", askClassifiedProposal, e.getMessage(), e);
         }

         try {
            proposalAggregator.onProposal(bidClassifiedProposal);

            // [DR20150409] La risposta Bloomberg non include nessun orderID, utilizziamo l'ISIN così come è stato utilizzato nella pricesRequested(isin);
            marketStatistics.pricesResponseReceived(isin, 1);
         }
         catch (BestXException e) {
            LOGGER.error("Error managing classifiedProposal {}: {}", bidClassifiedProposal, e.getMessage(), e);
         }
      }
      else {
         LOGGER.info("Error managing classifiedProposals: unable to retrieve a valid proposal aggregator for isin {}", isin);
      }
   }

   private void checkPreRequisites() {
      if (tradeStacPreTradeConnection == null) {
         throw new ObjectNotInitializedException("MDS connection not set");
      }
      if (tradeXpressConnection == null) {
         throw new ObjectNotInitializedException("TradeXpress connection not set");
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
      // if (tradeXpressConnection != null && tradewebTradeMatchingService ==
      // null) {
      // LOGGER.warn("Trade Matching service not set");
      // throw new
      // ObjectNotInitializedException("Trade Matching service not set");
      // }
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

      JobExecutionDispatcher.INSTANCE.addTimerEventListener(TradewebMarket.class.getSimpleName(), this);

      CommonMetricRegistry.INSTANCE.registerHealtCheck(this);

      super.init();
   }

   public void requestInstrumentStatus() throws BestXException {
      LOGGER.debug("Sending Security List Request");
      tradeStacPreTradeConnection.requestInstrumentStatus();
   }

   @Override
   public synchronized void startPriceConnection() throws BestXException {
      LOGGER.info("Connecting to Tradeweb Price");

      if (tradeStacPreTradeConnection != null && !tradeStacPreTradeConnection.isConnected()) {
         tradeStacPreTradeConnection.connect();
      }
      else {
         LOGGER.info("Unable to start Price connection: mdsConnection is null");
      }
   }

   @Override
   public synchronized void stopPriceConnection() throws BestXException {
      LOGGER.info("Disconnecting from Tradeweb Price");

      if (symbioticConnectionListeners != null) {
         for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
            ((MarketMXBean) symbioticConnectionListener).disablePriceConnection();
         }
      }
      if (tradeStacPreTradeConnection != null && tradeStacPreTradeConnection.isConnected())
         tradeStacPreTradeConnection.disconnect();
   }

   @Override
   public synchronized void startBuySideConnection() throws BestXException {
      LOGGER.info("Connecting to Tradeweb BuySide");

      if (tradeXpressConnection != null && !tradeXpressConnection.isConnected()) {
         tradeXpressConnection.connect();

         if (tradewebTradeMatchingService != null) {
            tradewebTradeMatchingService.start();
         }
      }
      else {
         LOGGER.info("Unable to start BuySide connection: tradeXpressConnection is null");
      }
   }

   // [DR20120508] stopBuySideConnection should Disconnect also the "Price"
   // connection (TradeStac)
   @Override
   public synchronized void stopBuySideConnection() throws BestXException {
      LOGGER.info("Disconnecting from Tradeweb BuySide");

      if (tradeXpressConnection != null && tradeXpressConnection.isConnected()) {
         tradeXpressConnection.disconnect();

         if (tradewebTradeMatchingService != null) {
            tradewebTradeMatchingService.stop();
         }
      }
      else {
         LOGGER.info("Unable to stop BuySide connection: tradeXpressConnection is null");
      }
   }

   @Override
   public void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus) {
      LOGGER.info("[{}] {}", connectionName, connectionStatus);

      if (connectionName.equals(tradeStacPreTradeConnection.getConnectionName())) {
         switch (connectionStatus) {
            case Connected:
               if (mdsConnectionMarketStatus == ConnectionStatus.Connected) {
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
         mdsConnectionMarketStatus = connectionStatus;
         switch (connectionStatus) {
            case Connected: {
               for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
                  symbioticConnectionListener.onConnection(tradeStacPreTradeConnection);

               }
               /*
                * At the first connection with MDServer, send security list
                * request for all authorized group.
                */
               //                try {
               //                    requestInstrumentStatus();
               //                } catch (BestXException e) {
               //                    LOGGER.error(e.getMessage(), e);
               //                }
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
      }
      else if (tradeXpressConnection != null && connectionName.equals(tradeXpressConnection.getConnectionName())) {
         tradeXpressConnectionMarketStatus = connectionStatus;
      }
      else {
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
      return (!isPriceConnectionProvided() || isPriceConnectionAvailable()) && (!isBuySideConnectionProvided() || isBuySideConnectionAvailable());
   }

   @Override
   public void setConnectionListener(ConnectionListener listener) {
      connectionListener.put(((ConnectionHelper) listener).getConnectionName(), listener);
   }

   @Override
   public void ensurePriceAvailable() throws MarketNotAvailableException {
      if (!isPriceConnectionAvailable()) {
         throw new MarketNotAvailableException(marketCode + " not connected");
      }
   }

   @Override
   public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order) throws BestXException {
      LOGGER.debug("orderID = {}, venues = {}, maxLatency = {}", (order != null ? order.getFixOrderId() : order), venues, maxLatency);

      Instrument instrument = order.getInstrument();
      String isin = instrument.getIsin();

      LOGGER.debug("Requesting price to Tradeweb for ISIN: {}", isin);
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
                  }
                  else {
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
            JobDetail newJob = simpleTimerManager.createNewJob(timerName, this.getClass().getSimpleName(), false /* no durable flag required */, true /* request recovery */, true /* monitorable */);
            Trigger trigger = null;
            if (targetMarketMarketMakers.size() > 0 && maxLatency > 0) {
               // this timer is not repeatable
               trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, maxLatency);
            }
            else {
               // Dopo 10 secondi fa partire la onTimerExpired
               // this timer is not repeatable
               trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, 10000);
            }
            simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, true);
         }
         catch (SchedulerException e) {
            LOGGER.error("Error while scheduling price discovery wait timer: {}", e.getMessage(), e);
         }

         // [BXMNT-430] marketSpecificCodes * 2 (BID e ASK)
         marketStatistics.pricesRequested(isin, marketSpecificCodes.size() * 2);

         tradeStacPreTradeConnection.requestInstrumentPriceSnapshot(instrument, marketSpecificCodes);

      }
      catch (ConcurrentModificationException cme) {
         LOGGER.error("Error while starting the price requests towards TW: {}", cme.getMessage(), cme);
      }
   }

   @Override
   public Market getQuotingMarket(Instrument instrument) throws BestXException {
      return marketFinder.getMarketByCode(MarketCode.TW, null);
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
      throw new UnsupportedOperationException();
   }

   @Override
   public void sendFokOrder(Operation operation, MarketOrder marketOrder) throws BestXException {
      checkBuySideConnection();
      LOGGER.debug("listener = {}, marketOrder = {}", operation, marketOrder);

      String clOrdID = serialNumberService.getUniqueIdentifier(OperationIdType.TRADEWEB_CLORD_ID.toString(), "TW" + "-O%010d");

      operationRegistry.bindOperation(operation, OperationIdType.TRADEWEB_CLORD_ID, clOrdID);

      marketOrder.setMarketSessionId(clOrdID);
      marketOrder.setMiFIDRestricted(miFIDRestricted);
      //setMifidRestricted(true);

      marketStatistics.orderSent(operation.getOrder().getFixOrderId());
      LOGGER.info("[MktReq] Order {}, Send Order to {} - ClOrdID: {}", marketOrder.getFixOrderId(), getMarketCode(), marketOrder.getMarketSessionId());

      tradeXpressConnection.sendOrder(marketOrder);
   }

   private void checkBuySideConnection() throws MarketNotAvailableException {
      if (!isBuySideConnectionAvailable())
         throw new MarketNotAvailableException(this.market.getName() + " is not connected");
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
      checkBuySideConnection();
      throw new UnsupportedOperationException();
   }

   @Override
   public void rejectProposal(Operation operation, Instrument instrument, Proposal proposal) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason) throws BestXException {
      checkBuySideConnection();
      LOGGER.debug("listener = {}, marketOrder = {}, reason = {}", operation, marketOrder, reason);

      String clOrdID = marketOrder.getMarketSessionId();
      LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

      tradeXpressConnection.cancelOrder(marketOrder);
   }

   @Override
   public void revokeOrder(Operation operation, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
      checkBuySideConnection();
      LOGGER.debug("listener = {}, marketOrder = {}, reason = {}, sendOrderCancelTimeout = {}", operation, marketOrder, reason, sendOrderCancelTimeout);

      String clOrdID = marketOrder.getMarketSessionId();
      LOGGER.info("[MktReq] Order {}, Sending Cancel - ClOrdID={}", operation.getOrder().getFixOrderId(), clOrdID);

      tradeXpressConnection.cancelOrder(marketOrder);
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

      if (tradewebTradeMatchingService != null) {
         return tradewebTradeMatchingService.matchTrade(order, executionPrice, marketMaker, minArrivalDate);
      }
      else {
         LOGGER.warn("tradeMatchingService not available, matchingTrade can not be retrieved for [{}]", order);
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
      return MarketCode.TW;
   }

   @Override
   public boolean isBuySideConnectionProvided() {
      return tradeXpressConnection != null;
   }

   @Override
   public boolean isPriceConnectionProvided() {
      return tradeStacPreTradeConnection != null;
   }

   @Override
   public boolean isBuySideConnectionAvailable() {
      return tradeXpressConnection != null && tradeXpressConnection.isConnected() && tradeXpressConnectionMarketStatus == ConnectionStatus.Connected;
   }

   @Override
   public boolean isPriceConnectionAvailable() {
      return tradeStacPreTradeConnection.isConnected() && mdsConnectionMarketStatus == ConnectionStatus.Connected;
   }

   @Override
   public boolean isAMagnetMarket() {
      return false;
   }

   @Override
   public void onConnection(Connection source) {
      LOGGER.info("Tradeweb Market Connected");

      for (ConnectionListener symbioticConnectionListener : symbioticConnectionListeners) {
         symbioticConnectionListener.onConnection(source);
      }
   }

   @Override
   public void onDisconnection(Connection source, String reason) {
      LOGGER.info("Tradeweb Market Disconnected");

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

   public void setTradeMatchingService(TradewebTradeMatchingService tradeMatchingService) {
      this.tradewebTradeMatchingService = tradeMatchingService;
   }

   /**
    * Used to set MDSConnection configured via Spring
    * 
    * @param mdsConnection the pre-trade connection
    */
   public void setTradeStacPreTradeConnection(TradeStacPreTradeConnection mdsConnection) {
      this.tradeStacPreTradeConnection = mdsConnection;
   }

   /**
    * Used to set TradeXpressConnection configured via Spring
    * 
    * @param tradeXpressConnection the trade connection
    */
   public void setTradeXpressConnection(TradeXpressConnection tradeXpressConnection) {
      this.tradeXpressConnection = tradeXpressConnection;
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
   @SuppressWarnings("deprecation")
   public void onExecutionReport(String sessionId, String clOrdID, TSExecutionReport tsExecutionReport) {

	   ExecType execType = tsExecutionReport.getExecType();
	   OrdStatus ordStatus = tsExecutionReport.getOrdStatus();
	   BigDecimal accruedInterestAmount = tsExecutionReport.getAccruedInterestAmt() != null ? BigDecimal.valueOf(tsExecutionReport.getAccruedInterestAmt()) : BigDecimal.ZERO;
	   BigDecimal accruedInterestRate = BigDecimal.ZERO;

	   // SP-20191016 - BESTX-546
	   BigDecimal lastPrice = BigDecimal.ZERO;
	   if (tsExecutionReport.getPriceType() != null && tsExecutionReport.getPriceType() == PriceType.Percentage) {
		   if (tsExecutionReport.getLastParPrice() != null) {
			   lastPrice = tsExecutionReport.getLastParPrice() != null ? BigDecimal.valueOf(tsExecutionReport.getLastParPrice()) : BigDecimal.ZERO;
		   } else {
			   lastPrice = tsExecutionReport.getLastPx() != null ? BigDecimal.valueOf(tsExecutionReport.getLastPx()) : BigDecimal.ZERO;
		   }
	   } else {
		   if (tsExecutionReport.getAvgPx() != null) {
			   lastPrice = BigDecimal.valueOf(tsExecutionReport.getAvgPx());
		   } else {
			   lastPrice = tsExecutionReport.getLastParPrice() != null ? BigDecimal.valueOf(tsExecutionReport.getLastParPrice()) : BigDecimal.ZERO;
		   }
	   }
	   String contractNo = tsExecutionReport.getExecID();
	   Date futSettDate = tsExecutionReport.getSettlDate();
	   String settlType = (tsExecutionReport.getSettlType() != null ? tsExecutionReport.getSettlType().getFIXValue() : null);
	   Date transactTime = tsExecutionReport.getTransactTime();
	   String text = tsExecutionReport.getText();
	   String executionBroker = null;
	   if(tsExecutionReport.getTSParties() != null)
		   for (TSNoPartyID party : tsExecutionReport.getTSParties().getTSNoPartyIDsList()) {
			   if (party.getPartyRole() == PartyRole.ExecutingFirm && party.getPartyIDSource() != PartyIDSource.LegalEntityIdentifier) {
				   executionBroker = party.getPartyID();
				   break;
			   }
		   }
	   String micCode = null;
	   micCode = tsExecutionReport.getCustomFieldString(MiFIDMIC.FIELD);

	   //BESTX-348: SP-20180905 added numDaysInterest field
	   Integer numDaysInterest = tsExecutionReport.getNumDaysInterest();

	   //BESTX-385: SP-20190116 manage factor (228) field
	   BigDecimal factor = tsExecutionReport.getFactor() != null ? BigDecimal.valueOf(tsExecutionReport.getFactor()) : BigDecimal.ZERO;


	   LOGGER.debug(
			   "sessionId = {}, clOrdID = {}, execType = {}, ordStatus = {}, accruedInterestAmount = {}, accruedInterestRate = {}, lastPrice = {}, contractNo = {}, futSettDate = {}, transactTime = {}, text = {}",
			   sessionId, clOrdID, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate, transactTime, text);

	   String cleanClOrdId = clOrdID.replace("OCR#", ""); // this to manage Tradeweb cancel reply
	   String cleanText = (text == null && ExecType.Canceled.equals(execType)) ? "Cancel requested by BestX:FI-A" : text;

	   try {
		   final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TRADEWEB_CLORD_ID, cleanClOrdId);

		   if (operation.getOrder() == null) {
	        	operation.onApplicationError(operation.getState(), new NullPointerException(), "Operation " + operation.getId() + " has no order!");
	        	return;
		   }
		   
		   
		   MarketMarketMaker mmm = marketMakerFinder.getMarketMarketMakerByCode(this.getMarketCode(), executionBroker);
		   if (mmm != null)
			   executionBroker = mmm.getMarketMaker().getCode();

		   LOGGER.info("[MktMsg] orderID = {}, ExecutionReport received: original clOrdId={}, OrdStatus={}, ExecType={}, LastPrice={}, ExecutionBroker={}, text={}", operation.getOrder().getFixOrderId(),
				   cleanClOrdId, ordStatus, execType, lastPrice, executionBroker, cleanText);

		   String orderId = operation.getOrder().getFixOrderId();
		   LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);
		   if (execType != ExecType.New && execType != ExecType.OrderStatus) {
			   if (lastPrice.doubleValue() > 0.0) {
				   marketStatistics.orderResponseReceived(orderId, operation.getOrder().getQty().doubleValue());
			   }
			   else {
				   marketStatistics.orderResponseReceived(orderId, 0.0);
			   }
		   }

		   //Add execution prices from tsExecutionReport
		   Attempt attempt = operation.getLastAttempt();
		   String textTruncated = null;
		   if (OrdStatus.Canceled.equals(tsExecutionReport.getOrdStatus())) {
		      
            if(!addPobexInformation(attempt, tsExecutionReport, operation)) {
               LOGGER.info("[MktMsg] No custom component found in execution report {}. Use test field (tag 58) for  pobex!", tsExecutionReport.getClOrdID());

   			   String notes = tsExecutionReport.getText();
   
   			   if (notes != null && notes.indexOf("[") >= 0 && notes.indexOf("]") >= 0) {
   				   String prices[] = notes.substring(notes.indexOf("[") + 1, notes.indexOf("]")).split(";");
   				   int rank = 0;
   				   for (int i = 0; i < prices.length; i++) {
   					   String data[] = prices[i].split(":");
   					   
   					   if (data.length > 1) {
      					   ExecutablePrice price = new ExecutablePrice();
      					   price.setMarket(this.market);
      					   price.setAuditQuoteState("Rejected");
      					   price.setOriginatorID(data[0].trim());
      					   price.setPrice(new Money(operation.getOrder().getCurrency(), new BigDecimal(data[1].trim())));
      					   price.setPriceType(Proposal.PriceType.PRICE);
      					   price.setQty(operation.getOrder().getQty());
      					   price.setTimestamp(tsExecutionReport.getTransactTime());
      					   price.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
      					   MarketMarketMaker tempMM = marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), data[0]);
      					   if(tempMM == null) {
      						   LOGGER.info("IMPORTANT! Tradeweb returned dealer {} not configured in BestX:FI-A. Please configure it", data[0]);
      						   price.setOriginatorID(data[0]);
      					   } else {
      						   price.setMarketMarketMaker(tempMM);
      					   }   
      					   attempt.addExecutablePrice(price, rank++);
      					   if(mmm == null) {
      						   LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", 
      								   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getOriginatorID(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
      					   } else {
      						   LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", 
      								   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getMarketMarketMaker().getMarketMaker().getName(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
      					   }
   					   } else {
   					      LOGGER.info("Ignoring value {}", data != null ? data[0] : "");
   					   }
   				   }
   				   textTruncated = notes.substring(0, notes.indexOf("["));
   			   }
            }
		   } else {
			   if(OrdStatus.Filled.equals(tsExecutionReport.getOrdStatus()) || OrdStatus.PartiallyFilled.equals(tsExecutionReport.getOrdStatus()) ) {

				   //BESTX-363 SP20181010 : Added executable price of execution broker
				   ExecutablePrice priceExec = new ExecutablePrice();
				   priceExec.setMarket(this.market);
				   priceExec.setAuditQuoteState("Done");
               priceExec.setOriginatorID(executionBroker);
				   if(mmm == null) {
                  LOGGER.info("IMPORTANT! Tradeweb returned dealer {} not configured in BestX:FI-A. Please configure it! ClOrdId={}, OrdId={}, CompDlrId={}", executionBroker, cleanClOrdId, orderId);

				   } else {
					   priceExec.setMarketMarketMaker(mmm);
				   }
				   priceExec.setPrice(new Money(operation.getOrder().getCurrency(), Double.toString(lastPrice.doubleValue())));
				   priceExec.setPriceType(Proposal.PriceType.PRICE);
				   priceExec.setQty(operation.getOrder().getQty());
				   priceExec.setTimestamp(tsExecutionReport.getTransactTime());
				   priceExec.setType(ProposalType.COUNTER);
				   priceExec.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
				   priceExec.setQuoteReqId(attempt.getMarketOrder().getFixOrderId());
				   attempt.addExecutablePrice(priceExec, 0);

				   if(mmm == null) {
					   LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", 
							   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), priceExec.getOriginatorID(), priceExec.getPrice().getAmount().toString(), priceExec.getAuditQuoteState());
				   } else {
					   LOGGER.info("Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", 
							   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), priceExec.getMarketMarketMaker().getMarketMaker().getName(), priceExec.getPrice().getAmount().toString(), priceExec.getAuditQuoteState());
				   }
				   //END BESTX-366
			   }            

            TSCompDealersGrpComponent compDealersGrpComponent = getCompDealersGrpComponent(tsExecutionReport);
            if(!addPobexInformation(attempt, tsExecutionReport, operation)) {
				   LOGGER.info("[MktMsg] No custom component found in execution report {}", tsExecutionReport.getClOrdID());
			   }
		   }
		   
		   for (ExecutablePrice ep : operation.getLastAttempt().getExecutablePrices()) {
	            if (ep.getMarketMarketMaker() != null && ep.getMarketMarketMaker().getMarketMaker() != null) {
	                ep.setVenue(this.venueFinder.getMarketMakerVenue(ep.getMarketMarketMaker().getMarketMaker()));
	            }
		   }
		   
		   LOGGER.debug("Passing to executor message with status {} for order {} for management", ordStatus, cleanClOrdId);
		   executor.execute(new OnExecutionReportRunnable(operation, this, market, cleanClOrdId, execType, ordStatus, accruedInterestAmount, accruedInterestRate, lastPrice, contractNo, futSettDate,
				   transactTime, (textTruncated != null ? textTruncated : cleanText), mmm, executionBroker, micCode, numDaysInterest, factor, settlType));
	   }
	   catch (OperationNotExistingException e) {
		   LOGGER.warn("[MktMsg] Operation not found for clOrdID {} , ignoring ExecutionReport/{}/{}", cleanClOrdId, execType, ordStatus);
	   }
	   catch (BestXException e) {
		   LOGGER.error("[MktMsg] Exception while handling ExecutionReport/{}/{} for clOrdID {}, ignoring it", execType, ordStatus, cleanClOrdId, e);
	   }
   }

   /**
    * @param attempt
    * @param tsExecutionReport
    * @param operation
    * @throws BestXException
    */
   private boolean addPobexInformation(Attempt attempt, TSExecutionReport tsExecutionReport, final Operation operation) throws BestXException {
      boolean added = false;
      TSCompDealersGrpComponent pobexGrp = getCompDealersGrpComponent(tsExecutionReport);
      String clOrdId = tsExecutionReport.getClOrdID();
      String ordId =tsExecutionReport.getOrderID();
	   if (pobexGrp != null) {
		   try {
			   quickfix.field.NoCompDealers compDealerGrp = ((TSCompDealersGrpComponent) pobexGrp).get(new quickfix.field.NoCompDealers());
			   List<Group> groups = ((TSCompDealersGrpComponent) pobexGrp).getGroups(compDealerGrp.getField());
			   added = groups.size() > 0;
			   
			   for (int i = 0; i < groups.size(); i++) {

				   ExecutablePrice price = new ExecutablePrice();
				   price.setMarket(this.market);
				   MarketMarketMaker tempMM = null;
				   boolean isValidPrice = true;
				   String quotingDealer = "";
				   if (groups.get(i).isSetField(CompDealerID.FIELD)) {
					   quotingDealer = groups.get(i).getField(new StringField(CompDealerID.FIELD)).getValue();

					   tempMM = marketMakerFinder.getMarketMarketMakerByCode(market.getMarketCode(), quotingDealer);
                  price.setOriginatorID(quotingDealer);
					   if(tempMM == null) {
						   LOGGER.info("IMPORTANT! Tradeweb returned dealer {} not configured in BestX:FI-A. Please configure it! ClOrdId={}, OrdId={}, CompDlrId={}", quotingDealer, clOrdId, ordId, quotingDealer);
					   } else {
						   price.setMarketMarketMaker(tempMM);
					   }
				   }
				   
				   //SP-20191016 - BESTX-546
				   if (groups.get(i).isSetField(CompDealerParQuote.FIELD)) {
					   Double compDealerQuote = groups.get(i).getField(new DoubleField(CompDealerParQuote.FIELD)).getValue();
					   price.setPrice(new Money(operation.getOrder().getCurrency(), Double.toString(compDealerQuote)));
					   price.setPriceType(Proposal.PriceType.PRICE);
				   } else {
					   LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={} : CompDealerParQuote not set for percentage price, use CompDealerQuote!", clOrdId, ordId, quotingDealer);
					   if (groups.get(i).isSetField(CompDealerQuote.FIELD)) {
						   Double compDealerQuote = groups.get(i).getField(new DoubleField(CompDealerQuote.FIELD)).getValue();
						   price.setPrice(new Money(operation.getOrder().getCurrency(), Double.toString(compDealerQuote)));
						   if (groups.get(i).isSetField(CompDealerQuotePriceType.FIELD)) {
							   price.setPriceType(Proposal.PriceType.createPriceType(groups.get(i).getField(new IntField(CompDealerQuotePriceType.FIELD)).getValue()));
						   }
					   } else {
						   LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: CompDealerParQuote and CompDealerQuote not set! Default value price is 0.0!", clOrdId, ordId, quotingDealer);
						   price.setPrice(new Money(operation.getOrder().getCurrency(), "0.0"));
						   price.setPriceType(Proposal.PriceType.PRICE);
						   isValidPrice = false;
					   }
				   }

				   price.setQty(operation.getOrder().getQty());
				   price.setTimestamp(tsExecutionReport.getTransactTime());
				   price.setType(ProposalType.COUNTER);
				   price.setSide(operation.getOrder().getSide() == OrderSide.BUY ? ProposalSide.ASK : ProposalSide.BID);
				   price.setQuoteReqId(attempt.getMarketOrder().getFixOrderId());
				   attempt.addExecutablePrice(price, i + 1);
				   if(tempMM == null) {
					   LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", clOrdId, ordId, quotingDealer, 
							   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getOriginatorID(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
				   } else {
					   LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: Added Executable price for order {}, attempt {}, marketmaker {}, price {}, status {}", clOrdId, ordId, quotingDealer, 
							   operation.getOrder().getFixOrderId(), operation.getAttemptNo(), price.getMarketMarketMaker().getMarketMaker().getName(), price.getPrice().getAmount().toString(), price.getAuditQuoteState());
				   }
               int status = -1;
               if (groups.get(i).isSetField(CompDealerStatus.FIELD)) {
                  status = groups.get(i).getField(new IntField(CompDealerStatus.FIELD)).getValue();
               }
               //0= Error, 1= Pass, 2= Timed out, 3= Rejected, 4= Expired, 5= Ended, 6= Pass on Last Look
               switch (status) {
               case 0:
                  price.setAuditQuoteState("Error");
                  break;
               case 1:
                  price.setAuditQuoteState("Passed");
                  break;
               case 2:
                  price.setAuditQuoteState("Timed Out");
                  break;
               case 3:
                  price.setAuditQuoteState("Rejected");
                  break;
               case 4:
                  price.setAuditQuoteState("EXP-Price");
                  break;
               case 5:
                  price.setAuditQuoteState("Cancelled");
                  break;
               case 6:
                  price.setAuditQuoteState("Passed");
                  break;
               default:
                  if(status == -1) {
                     // No status in the execution report
                     if(isValidPrice) {
                        if(OrdStatus.Canceled.equals(tsExecutionReport.getOrdStatus())){
                           price.setAuditQuoteState("Passed");
                           LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: No CompDealerStatus and valid price! Default value status is Passed in canceled executionReport!", clOrdId, ordId, quotingDealer);
                        }
                        else {
                           price.setAuditQuoteState("Covered");
                           LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: No CompDealerStatus and valid price! Default value status is Covered in fill executionReport!", clOrdId, ordId, quotingDealer);
                        }
                     }
                     else {
                        price.setAuditQuoteState("Passed");
                        LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: No CompDealerStatus and invalid price! Default value status is Passed!", clOrdId, ordId, quotingDealer);
                     }
                  }
                  else {
                     LOGGER.info("ClOrdId={}, OrdId={}, CompDlrId={}: CompDealerStatus out of renge ({})! Default value status is Passed!", clOrdId, ordId, quotingDealer, status);
                     price.setAuditQuoteState("Passed");
                  }
                  break;
               }
			   }
		   }
		   catch (FieldNotFound e) {
			   LOGGER.warn("[MktMsg] Field not found in component dealers", e);
		   }
	   }
	   
	   return added;
   }

   private TSCompDealersGrpComponent getCompDealersGrpComponent(TSExecutionReport tsExecutionReport) {
      TSCompDealersGrpComponent elem = null;
      List<MessageComponent> customComp = tsExecutionReport.getCustomComponents();
      if (customComp != null) {
         for (MessageComponent comp : customComp) {
            if (comp instanceof TSCompDealersGrpComponent) {
               elem = (TSCompDealersGrpComponent)comp;
               break;
            }
         }
      }
      return elem;
   }

   @Override
   public void onOrderReject(final String sessionId, String clOrdID, final String reason) {
      LOGGER.info("Order reject ({}) received from {} : {}", reason, getMarketCode(), sessionId);
      executor.execute(new OnOrderRejectRunnable(clOrdID, reason, this));
   }

   protected String getOperationTWOrderSessionId(Operation operation) throws BestXException {
      String sessionId = operation.getIdentifier(OperationIdType.TRADEWEB_CLORD_ID);
      if (sessionId == null) {
         sessionId = serialNumberService.getUniqueIdentifier(OperationIdType.TRADEWEB_CLORD_ID.toString(), "TradeXpress" + "-O%010d");
         operationRegistry.bindOperation(operation, OperationIdType.TRADEWEB_CLORD_ID, sessionId);
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
      LOGGER.warn("Order {} : Timer PriceStateTimeout expired for ISIN: {}, start to manage it", jobName, groupName);

      String isin = jobName.split("@")[0];
      String orderID = jobName.split("@")[1];

      ProposalAggregator proposalAggregator = proposalAggregatorMap.get(isin);

      if (proposalAggregator != null) {
         ProposalAggregatorListener proposalAggregatorListener = proposalAggregator.getProposalAggregatorListener(orderID);

         if (proposalAggregatorListener != null) {
            executor.execute(new TimerExpiredWorker(proposalAggregatorListener));
         }
         else {
            LOGGER.info("ProposalAggregatorListener not found for {}@{}, skip processing", orderID, isin);
         }
      }
      else {
         LOGGER.info("Unable to retrieve a valid proposalAggregator for {}@{}, skip processing", orderID, isin);
      }
   }

   @Override
   public void onOrderCancelReject(String sessionId, String origClOrdID, String reason) {
      LOGGER.info("Cancel rejected: sessionId = {}, origClOrdID = {}, reason = {}", sessionId, origClOrdID, reason);
      executor.execute(new OnOrderCancelRejectRunnable(origClOrdID, reason, this));
   }

   @Override
   public void ackProposal(Operation operation, Proposal proposal) throws BestXException {
      throw new UnsupportedOperationException();
   }

   @Override
   public void onSecurityListCompleted(Set<String> securityList) {
      LOGGER.info("Call to onSecurityListCompleted. Doing nothing");
   }

   public Boolean getMiFIDRestricted() {
      return miFIDRestricted;
   }

   public void setMiFIDRestricted(Boolean miFIDRestricted) {
      this.miFIDRestricted = miFIDRestricted;
   }
   
   public Market getMarket() {
      return market;
   }
}
