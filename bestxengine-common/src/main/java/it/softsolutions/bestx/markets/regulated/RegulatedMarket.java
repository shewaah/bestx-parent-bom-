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
package it.softsolutions.bestx.markets.regulated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.connections.regulated.RegulatedConnection;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedProposalInputLazyBean;
import it.softsolutions.bestx.exceptions.InstrumentNotNegotiableOnMarketException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.markets.GenericPriceListenerInfo;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.markets.VenuePriceListenerInfo;
import it.softsolutions.bestx.markets.VenuePriceListenerInfo.VenueProposalState;
import it.softsolutions.bestx.markets.regulated.comparators.RegulatedProposalAskComparator;
import it.softsolutions.bestx.markets.regulated.comparators.RegulatedProposalBidComparator;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.MarketSecurityStatusService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class manages the trading interaction with every regulated market
 * 
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation date: 09/ago/2012
 * 
 **/
public abstract class RegulatedMarket extends MarketCommon implements MarketBuySideConnection, MarketPriceConnection, MarketMXBean, TimerEventListener, RegulatedConnectionListener, Connection,
        ConnectionListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedMarket.class);

    protected ConnectionHelper connectionHelper;
    protected RegulatedConnection regulatedConnection;
    protected ConnectionListener connectionListener;
    protected MarketFinder marketFinder;
    protected OperationRegistry operationRegistry;
    protected SerialNumberService sessionIdServer;
    protected VenueFinder venueFinder;
    protected Executor executor;
    protected MarketSecurityStatusService marketSecurityStatusService;
    protected InstrumentFinder instrumentFinder;

    protected boolean connected;
    private volatile boolean buySideConnectionOn;
    protected volatile boolean priceConnectionOn;
    private String orderAccount;

    protected Map<String, VenuePriceListenerInfo> priceListeners = new ConcurrentHashMap<String, VenuePriceListenerInfo>();

    private boolean currentlyPerformingConnection;

    protected void checkPreRequisites() throws ObjectNotInitializedException {
        if (connectionHelper == null) {
            throw new ObjectNotInitializedException("Connection helper not set");
        }
        if (regulatedConnection == null) {
            throw new ObjectNotInitializedException("Market connection not set");
        }
        if (sessionIdServer == null) {
            throw new ObjectNotInitializedException("Market Session ID server not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("Market finder not set");
        }
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
        if (venueFinder == null) {
            throw new ObjectNotInitializedException("Venue finder not set");
        }
        if (executor == null) {
            throw new ObjectNotInitializedException("Executor not set");
        }
        if (marketSecurityStatusService == null) {
            throw new ObjectNotInitializedException("Security Status Service not set");
        }
        if (instrumentFinder == null) {
            throw new ObjectNotInitializedException("InstrumentFinder not set");
        }
    }

    @Override
    public void init() throws BestXException {
        checkPreRequisites();
        connectionHelper.setConnection(regulatedConnection);
        connectionHelper.setConnectionListener(this);
        
        JobExecutionDispatcher.INSTANCE.addTimerEventListener(RegulatedMarket.class.getSimpleName(), this);
        
        super.init();
    }

    @Override
    public void connect() throws BestXException {
        LOGGER.info("Connecting to " + getConnectionName());
        if (!currentlyPerformingConnection) {
            currentlyPerformingConnection = true;
            connectionHelper.connect();
        }
    }

    @Override
    public void disconnect() throws BestXException {
        LOGGER.info("Disconnecting from " + getConnectionName());
        currentlyPerformingConnection = false;
        connectionHelper.disconnect();
    }

    @Override
    public void onConnectionError() {
        LOGGER.info("Error connecting - Disconnect from " + getConnectionName());
        connectionHelper.disconnect();
    }

    @Override
    public boolean isBuySideConnectionProvided() {
        return true;
    }

    @Override
    public boolean isPriceConnectionProvided() {
        return true;
    }

    @Override
    public void onConnection(Connection source) {
        LOGGER.info(getConnectionName() + " Market Connected");
        connected = true;
        currentlyPerformingConnection = false;
        if (connectionListener != null) {
            connectionListener.onConnection(this);
        }
    }

    @Override
    public void onDisconnection(Connection source, String reason) {
        LOGGER.info(getConnectionName() + " Market Disconnected");
        connected = false;
        if (connectionListener != null) {
            connectionListener.onDisconnection(source, reason);
        }
    }

    /**
     * @param connectionHelper
     *            the connectionHelper to set
     */
    public void setConnectionHelper(ConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    /**
     * @param regulatedConnection
     *            the RegulatedConnection to set
     */
    public void setRegulatedConnection(RegulatedConnection regulatedConnection) {
        this.regulatedConnection = regulatedConnection;
        this.regulatedConnection.setRegulatedConnectionListener(this);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void setMarketSecurityStatusService(MarketSecurityStatusService marketSecurityStatusService) {
        this.marketSecurityStatusService = marketSecurityStatusService;
    }

    /**
     * @param sessionIdServer
     *            the sessionIdServer to set
     */
    public void setSessionIdServer(SerialNumberService sessionIdServer) {
        this.sessionIdServer = sessionIdServer;
    }

    /**
     * @param instrumentFinder
     *            the instrumentFinder to set
     */
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    @Override
    public MarketBuySideConnection getBuySideConnection() {
        return this;
    }

    @Override
    public MarketPriceConnection getPriceConnection() {
        return this;
    }

    public void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void matchOperations(Operation listener, Operation matching, Money ownPrice, Money matchingPrice) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void startBuySideConnection() throws BestXException {
        LOGGER.info("Start " + getConnectionName() + " buy side connection");
        if (!connected) {
            connect();
        }
        buySideConnectionOn = true;
    }

    @Override
    public synchronized void stopBuySideConnection() throws BestXException {
        LOGGER.info("Stop " + getConnectionName() + " buy side connection");
        buySideConnectionOn = false;
        if (!priceConnectionOn) {
            disconnect();
        }
    }

    @Override
    public synchronized void startPriceConnection() throws BestXException {
        LOGGER.info("Start " + getConnectionName() + " price connection");
        if (!connected) {
            connect();
        }
        priceConnectionOn = true;
    }

    @Override
    public synchronized void stopPriceConnection() throws BestXException {
        LOGGER.info("Stop " + getConnectionName() + " price connection");
        priceConnectionOn = false;
        if (!buySideConnectionOn) {
            disconnect();
        }
    }
    
    @Override
    public void ensurePriceAvailable() throws MarketNotAvailableException {
        checkPriceConnection();	
    }

    public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order) throws BestXException {
        Instrument instrument = order.getInstrument();
        OrderSide orderSide = order.getSide();
        BigDecimal qty = order.getQty();
        Money priceLimit = order.getLimit();
        String fixOrderId = order.getFixOrderId();
        String sessionId = getNewPriceSessionId();
        LOGGER.info("Order {}, requesting price for instrument: {} - {} Session ID: {}", fixOrderId, instrument.getIsin(), getConnectionName(), sessionId);
        VenuePriceListenerInfo priceListenerInfo = new VenuePriceListenerInfo();
        priceListenerInfo.listeners.add(0, listener);
        priceListenerInfo.sortedBook = new SortedBook();
        priceListenerInfo.sortedBook.setInstrument(instrument);
        priceListenerInfo.resultSent = false;
        priceListenerInfo.missingProposalStates.put(getVirtualVenue(), new VenueProposalState());
        priceListenerInfo.setOrderQty(qty);
        priceListenerInfo.setPriceLimit(priceLimit);
        priceListenerInfo.setOrderSide(orderSide);
        priceListeners.put(sessionId, priceListenerInfo);
        if (maxLatency > 0) {
            try {
                SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
                JobDetail newJob = simpleTimerManager.createNewJob(sessionId, this.getClass().getSimpleName(), false /* no durable flag required*/, true /* request recovery*/, true /* monitorable */);
                //this timer is not repeatable
                Trigger trigger = simpleTimerManager.createNewTrigger(sessionId, this.getClass().getSimpleName(), false, maxLatency);
                simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, true);
            } catch (SchedulerException e) {
                LOGGER.error("Error while scheduling price discovery wait timer!", e);
            }

//            priceListenerInfo.timerHandle = timerService.setupOnceOnlyTimer(this, maxLatency, sessionId);
        }
        Market instrumentMarket = getQuotingMarket(instrument);
        if (instrumentMarket == null) {
            throw new InstrumentNotNegotiableOnMarketException("Isin: " + instrument.getIsin() + " on Market: " + this.getMarketCode().name());
        } else {
            String subMarketName = null;
            if (instrumentMarket.getSubMarketCode() != null) {
                subMarketName = instrumentMarket.getSubMarketCode().name();
            }

            marketStatistics.pricesRequested(sessionId, venues.size());
            LOGGER.debug("Querying quote for Isin: " + instrument.getIsin() + " on SubMarket: " + subMarketName);
            regulatedConnection.requestInstrumentPriceSnapshot(sessionId, instrument, subMarketName, getConnectionName());
        }
    }

    protected ClassifiedProposal getConsolidatedProposal(ProposalSide side, OrderSide orderSide, List<RegulatedProposalInputLazyBean> list, Money orderPriceLimit, BigDecimal quantity,
            Instrument instrument) throws BestXException {

        ClassifiedProposal consolidatedProposal = null;
        BigDecimal accPrice = BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        BigDecimal totQuantity = BigDecimal.ZERO.setScale(0);
        BigDecimal neededQuantity = quantity;
        BigDecimal usedQuantity = null;
        // This is the worst price used for building the consolidated proposal
        BigDecimal worstPrice = null;
        // This price is stored for use for ZERO quantity proposals (to avoid div by 0)
        BigDecimal firstPrice = null;
        Comparator<RegulatedProposalInputLazyBean> comparator = null;
        String subMarketCode = null;
        Date timestamp = null;
        BigDecimal priceLimit = null;
        String instrumentCurrency = instrument.getCurrency();

        if (orderPriceLimit != null) {
            priceLimit = orderPriceLimit.getAmount();
        }

        // Sort proposals before consolidating
        switch (side) {
        case ASK:
            comparator = new RegulatedProposalAskComparator();
            break;
        case BID:
            comparator = new RegulatedProposalBidComparator();
            break;
        default:
            break;
        }

        Collections.sort(list, comparator);

        // 20110311 - Ruggero variable used to save the last examined proposal qty if we will have a tot qty = 0. We must put a quantity and
        // not 0 in the proposal.
        BigDecimal lastPropQty = null;

        // For each proposal, up to the number of proposals necessary to fill the order
        LOGGER.debug("Order - Side: " + side + " -  Price: " + priceLimit + " - Qty: " + quantity);
        // AMC 20100701 No difference in calculation if there is a price limit.
        // If mean price is worse than price limit the proposal will be discarded by the appropriate Proposal Classifier.
        for (RegulatedProposalInputLazyBean p : list) {

            // 20120222 - Ruggero Multiply quantity for the multiplier received with the instrument it should be null for MOT and TLX, 1 for
            // HIMTF e 1000000 for MTSPRIME
            BigDecimal qtyMultiplier = marketSecurityStatusService.getQtyMultiplier(getMarketCode(), instrument);
            BigDecimal propQty = p.getQty();

            if (qtyMultiplier == null) {
                qtyMultiplier = BigDecimal.ONE;
            }
            propQty = propQty.multiply(qtyMultiplier);
            propQty = propQty.setScale(5);

            if (subMarketCode == null) {
                subMarketCode = p.getSubMarketCode();
            }
            if (timestamp == null) {
                timestamp = p.getTimestamp();
            }

            /*
             * 20110927 - Ruggero Ticket AKR-1195, prezzi a zero nel book da parte di TLX e MOT. Il mercato ci manda proposal con prezzo e
             * quantita' a zero che creano problemi quando i prezzi validi sono tutti oltre il limite. Questo effetto causa la scrittura del
             * prezzo della BEST di MOT e/o TLX a 0, causando uno scarto per prezzo a zero invece che per prezzo oltre il limite.
             */
            if (propQty != null && propQty.compareTo(BigDecimal.ZERO) == 0 && p.getPrice() != null && p.getPrice().compareTo(BigDecimal.ZERO) == 0) {
                LOGGER.info("Proposal from market  " + subMarketCode + " with price and quantity zero. Skipped.");
                continue;
            }

            { // TODO : questo blocco apparteneva a qualcosa di significativo?
                if (firstPrice == null) {
                    firstPrice = p.getPrice();
                }

                if (isPriceLimitWorseThanPrice(orderSide, side, priceLimit, p.getPrice())) {
                    LOGGER.debug("Cut off price: " + p.getPrice() + " because it is worse than the limit price: " + priceLimit);
                    worstPrice = priceLimit;
                    lastPropQty = propQty;
                    break;
                }
                if (neededQuantity.compareTo(propQty) < 0) {
                    usedQuantity = neededQuantity; // This proposal is enough to
                                                   // finish filling the order
                } else {
                    usedQuantity = propQty; // More proposals are needed.
                                            // Consume the whole quantity of
                                            // this proposal
                }
                totQuantity = totQuantity.add(usedQuantity);
                accPrice = accPrice.add(p.getPrice().multiply(usedQuantity));
                neededQuantity = neededQuantity.subtract(usedQuantity);
                LOGGER.debug("Proposal  - Side: " + side + " -  Price: " + p.getPrice() + " - Qty: " + propQty + " - Used quantity: " + usedQuantity + " - Remaining quantity: " + neededQuantity);

                // Order filled [Be defensive: don't trust BigInteger to reach exactly ZERO]
                if (neededQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    worstPrice = p.getPrice();
                    break;
                }
            }
        }

        if (firstPrice == null) {
            firstPrice = BigDecimal.ZERO;
        }

        consolidatedProposal = new ClassifiedProposal();
        consolidatedProposal.setProposalState(ProposalState.NEW);
        consolidatedProposal.setSide(side);
        consolidatedProposal.setType(ProposalType.TRADEABLE);
        consolidatedProposal.setTimestamp(timestamp);
        try {
            consolidatedProposal.setMarket(marketFinder.getMarketByCode(getMarketCode(), SubMarketCode.valueOf(subMarketCode)));
        } catch (BestXException be) {
            LOGGER.error("Unable to retrieve market: {}",be.toString(), be);
            throw be;
        }

        // if totQuantity is ZERO, we have to set the price to ZERO if it's null
        if (totQuantity.compareTo(BigDecimal.ZERO) == 0) {
            // Take best proposal price: even if no quantity, it is needed by
            // the price service
            consolidatedProposal.setPrice(new Money(instrumentCurrency, firstPrice));
            if (lastPropQty != null) {
                consolidatedProposal.setQty(lastPropQty);
            } else {
                consolidatedProposal.setQty(BigDecimal.ZERO);
            }
        } else {
            BigDecimal wMeanPrice = accPrice.divide(totQuantity, 5, RoundingMode.HALF_UP);
            consolidatedProposal.setPrice(new Money(instrumentCurrency, wMeanPrice));
            consolidatedProposal.setQty(totQuantity.setScale(5, RoundingMode.HALF_UP));
        }

        // AMC 20100701 Only if the price limit exists must compare it to the worst price used in the mean price calculation.
        // If price limit is more strict than worse price must use limit price as limit price for the market order.
        // Remember in fact that we must avoid the risk of a market order at a price worst that the limit price: if the mean price is better than
        // the limit price the situation may be different when we close physically and the real price could be not the mean price.
        if (worstPrice != null) {
            if (priceLimit != null) {
                // Store worst price used to be used as limit later
                if (isPriceLimitWorseThanPrice(orderSide, side, priceLimit, worstPrice)) {
                    consolidatedProposal.setWorstPriceUsed(new Money(instrumentCurrency, priceLimit)); 
                } else {
                    consolidatedProposal.setWorstPriceUsed(new Money(instrumentCurrency, worstPrice)); 
                }
            } else {
                consolidatedProposal.setWorstPriceUsed(new Money(instrumentCurrency, worstPrice));
            }
        }
        return consolidatedProposal;
    }

    protected boolean isPriceLimitWorseThanPrice(OrderSide orderSide, ProposalSide side, BigDecimal priceLimit, BigDecimal price) {
        if (priceLimit == null || price == null) {
            return false;
        }
        
        return ((orderSide.equals(OrderSide.SELL) && side.equals(ProposalSide.BID) && (priceLimit.compareTo(price) > 0)) || (orderSide.equals(OrderSide.BUY) && side.equals(ProposalSide.ASK))
                && (priceLimit.compareTo(price) < 0));
    }

    /**
     * @param operationRegistry
     *            the operationRegistry to set
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * @param orderAccount
     *            the orderAccount to set
     */
    public void setOrderAccount(String orderAccount) {
        this.orderAccount = orderAccount;
    }

    public String getOrderAccount() {
        return orderAccount;
    }

    /**
     * Retrieve the order account to be sent in a FOK order. We try to send the sinfoCode, if not available we use the order account set by
     * configuration
     * 
     * @param customer
     *            : the customer whose sinfocode we are looking for
     * @return an order account
     * @throws IllegalArgumentException
     *             if the customer is null
     */
    public String getFOKOrderAccount(Customer customer) throws IllegalArgumentException {
        String fokOrderAcc = null;
        if (customer == null) {
            LOGGER.error("Error while looking for an order account, the customer cannot be null!");
            throw new IllegalArgumentException("Customer cannot be null");
        } else {
            fokOrderAcc = customer.getSinfoCode();
            if (fokOrderAcc == null) {
                fokOrderAcc = getOrderAccount();
            }
            return fokOrderAcc;
        }
    }

    public void onSecurityStatus(String isin, String subMarket, String statusCode) {
        SubMarketCode subMarketCode = null;
        try {
            subMarketCode = SubMarketCode.valueOf(subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Received market definition on security from unknown Sub Market {}: {}", subMarket, e.toString(), e);
            return;
        } catch (NullPointerException npe) {
            LOGGER.info("Received market definition on security for null Sub Market: trying to use HIMTF");
            subMarketCode = SubMarketCode.HIMTF;
        }
        Instrument.QuotingStatus quotingStatus = null;
        try {
            quotingStatus = Instrument.QuotingStatus.valueOf(statusCode);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Received unknown quoting status {}: {}", statusCode, e.toString(), e);
            return;
        }
        try {
            marketSecurityStatusService.setMarketSecurityQuotingStatus(getMarketCode(), subMarketCode, isin, quotingStatus);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving quoting status for instrument {}: {}", isin, e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving quoting status for instrument {}: {}", isin, e.toString(), e);
        }
    }

    // No more used. Replaced by
    // public void onSecurityDefinition(String isin, String subMarket, Date
    // settlementDate, BigDecimal minQty,
    // BigDecimal qtyTick, BigDecimal multiplier)
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate) {
        SubMarketCode subMarketCode = null;
        try {
            subMarketCode = SubMarketCode.valueOf(subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Received market definition on instrument from unknown Sub Market: {} / {}", subMarket , e.getMessage());
            return;
        }
        try {
            if (settlementDate != null)
                marketSecurityStatusService.setMarketSecuritySettlementDate(getMarketCode(), subMarketCode, isin, settlementDate);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date {}: {}", isin, e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date {}: {}", isin, e.toString(), e);
        }
    }

    public QuotingStatus getInstrumentQuotingStatus(Instrument instrument) throws BestXException {
        return marketSecurityStatusService.getInstrumentQuotingStatus(getMarketCode(), instrument.getIsin());
    }

    public Date getMarketInstrumentSettlementDate(Instrument instrument) throws BestXException {
        return marketSecurityStatusService.getMarketInstrumentSettlementDate(getMarketCode(), instrument.getIsin());
    }

    public Market getQuotingMarket(Instrument instrument) throws BestXException {
        return marketSecurityStatusService.getQuotingMarket(getMarketCode(), instrument.getIsin());
    }

    @Override
    public void onInstrumentPrices(String regulatedSessionId, List<RegulatedProposalInputLazyBean> regulatedProposal) {
        LOGGER.debug("Price notification received for {}", regulatedSessionId);
        
        VenuePriceListenerInfo priceListenerInfo = priceListeners.get(regulatedSessionId);
        if (priceListenerInfo == null) {
            LOGGER.debug("Stale price received from {}. Probably request timed out.", getConnectionName());
            return;
        }
        
        LOGGER.info("[PRICESRV],Market={},PricesReceivedIn={} millisecs",
                ((getMarketCode().name() != null) ? getMarketCode().name() : ""), 
                (DateService.currentTimeMillis() - priceListenerInfo.getCreationTime().getTime()));
        
        marketStatistics.pricesResponseReceived(regulatedSessionId, regulatedProposal.size());
        synchronized (priceListenerInfo) {
            String instrumentCurrency = priceListenerInfo.sortedBook.getInstrument().getCurrency();
            if (instrumentCurrency == null) {
                LOGGER.error("No currency found for the instrument:{}", priceListenerInfo.sortedBook.getInstrument().getIsin());
                priceListeners.remove(regulatedSessionId);
                if (!priceListenerInfo.resultSent) {
                    priceListenerInfo.resultSent = true;
                    priceListenerInfo.listeners.get(0).onMarketBookNotAvailable(this, "No default currency found for instrument: " + priceListenerInfo.sortedBook.getInstrument().getIsin());
                }
                return;
            }

            if (regulatedProposal.size() > 0) {
                try {
                    ClassifiedProposal proposal = getConsolidatedProposal(regulatedProposal.get(0).getSide(), priceListenerInfo.getOrderSide(), regulatedProposal, priceListenerInfo.getPriceLimit(),
                            priceListenerInfo.getOrderQty(), priceListenerInfo.sortedBook.getInstrument());

                    SubMarketCode subMarketCode = null;
                    try {
                        subMarketCode = SubMarketCode.valueOf(regulatedProposal.get(0).getSubMarketCode());
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Could not find SubMarketCode for {}: {}", regulatedProposal.get(0).getSubMarketCode(), e.toString(), e);
                    }
                    
                    // retrieve market code from lazy bean
                    proposal.setMarket(marketFinder.getMarketByCode(getMarketCode(), subMarketCode));
                    // No market market maker on regulated markets
                    proposal.setMarketMarketMaker(null); 

                    Venue venue = null;
                    try {
                        venue = venueFinder.getMarketVenue(proposal.getMarket());
                    } catch (BestXException e) {
                        LOGGER.error("Error while finding Market Venue {}: {}", proposal.getMarket().getMarketCode().name(), e.toString(), e);
                        return;
                    }
                    proposal.setVenue(venue);
                    proposal.setFutSettDate(marketSecurityStatusService.getMarketInstrumentSettlementDate(proposal.getMarket().getMarketCode(), priceListenerInfo.sortedBook.getInstrument().getIsin()));
                    // check if we already have a proposal for the given side
                    if (!priceListenerInfo.missingProposalStates.get(getVirtualVenue()).isSideArrived(proposal.getSide())) {
                        priceListenerInfo.sortedBook.addProposal(proposal);
                        priceListenerInfo.missingProposalStates.get(getVirtualVenue()).addProposalSide(proposal.getSide());
                    }

                    if (priceListenerInfo.missingProposalStates.get(getVirtualVenue()).bothSidesArrived()) {
                        LOGGER.debug("Both sides proposals arrived from {} for session ID: {}", getConnectionName(), regulatedSessionId);
                        priceListeners.remove(regulatedSessionId);
                        try {
                            SimpleTimerManager.getInstance().stopJob(regulatedSessionId, this.getClass().getSimpleName());
                        } catch (SchedulerException e) {
                            LOGGER.error("Cannot cancel timer for session ID: {}", regulatedSessionId);
                        }
                        
//                        if (priceListenerInfo.timerHandle != null) {
//                            LOGGER.debug("Cancel timer for session ID: {}", regulatedSessionId);
//                            timerService.cancelTimer(priceListenerInfo.timerHandle);
//                            priceListenerInfo.timerHandle = null;
//                        }
                        if (!priceListenerInfo.resultSent) {
                            priceListenerInfo.resultSent = true;
                            priceListenerInfo.listeners.get(0).onMarketBookComplete(this, priceListenerInfo.sortedBook);
                        }
                    }
                } catch (BestXException ex) {
                    priceListenerInfo.resultSent = true;
                    priceListenerInfo.listeners.get(0).onMarketBookNotAvailable(this,
                            "Error while creating the book for the instrument: " + priceListenerInfo.sortedBook.getInstrument().getIsin() + " : " + ex.toString());
                    return;
                }
            } else {
                LOGGER.error("Received prices but price list size is Zero! this will create a timeout condition.");
            }
        }
    }

    public abstract void onExecutionReport(String regSessionId, ExecutionReportState executionReportState, RegulatedFillInputBean regulatedFillInputBean);

    protected abstract void checkPriceConnection() throws  MarketNotAvailableException;

    protected abstract String getNewPriceSessionId() throws BestXException;

    protected abstract Venue getVirtualVenue();

    protected abstract MarketBuySideConnection getMarket();

    public void onCancelRequestReject(String regSessionId, String reason) {
        throw new UnsupportedOperationException();
    }

    public void onFasCancelFillAndBook(String regSessionId, String reason) {
        throw new UnsupportedOperationException();
    }

    public void onFasCancelFillNoBook(String regSessionId, String reason) {
        throw new UnsupportedOperationException();
    }

    public void onFasCancelNoFill(String regSessionId, String reason) {
        throw new UnsupportedOperationException();
    }

    public void onFasCancelNoFill(String regSessionId, String string, ExecutionReportState cancelled, RegulatedFillInputBean regulatedFillInputBean) {
        throw new UnsupportedOperationException();
    }

    public void onOrdeReceived(String regSessionId, String orderId) {
        throw new UnsupportedOperationException();
    }

    public int countOrders() {
        int result = 0;
        for (GenericPriceListenerInfo priceListenerInfo : priceListeners.values()) {
            result += priceListenerInfo.listeners.size();
        }
        return result;
    }

    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal qtyTick, BigDecimal multiplier) {
        this.onSecurityDefinition(isin, subMarket, settlementDate, minQty, qtyTick, multiplier, null);
    }

    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal qtyTick, BigDecimal multiplier, String bondType) {
        SubMarketCode subMarketCode = null;
        try {
            subMarketCode = SubMarketCode.valueOf(subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Received market definition on instrument from unknown Sub Market {}: {}", subMarket, e.toString(), e);
            return;
        }
        try {
            marketSecurityStatusService.setMarketSecuritySettlementDateAndBondType(getMarketCode(), subMarketCode, isin, settlementDate, bondType, 0, null, 0, null);
            marketSecurityStatusService.setMarketSecurityQuantity(getMarketCode(), subMarketCode, isin, minQty, qtyTick, multiplier);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date {}: {}", isin, e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date {}: {}", isin, e.toString(), e);
        }
    }

    public void onMarketPriceReceived(String regSessionId, BigDecimal marketPrice, RegulatedFillInputBean regFillInputBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void timerExpired(String jobName, String groupName) {
        VenuePriceListenerInfo priceListenerInfo = priceListeners.remove(jobName);
        if (priceListenerInfo == null) {
            LOGGER.error("Timeout reached on REGULATED MARKET price retrieval but no corresponding request was found");
            return;
        }
        LOGGER.warn("REGULATED MARKET timed out while waiting for prices for session id: " + jobName);

        synchronized (priceListenerInfo) {
            if (!priceListenerInfo.resultSent) {
                priceListenerInfo.resultSent = true;
                // Stefano 20080701 - Versione corretta se nei 35=j arrivasse il
                // campo 11 (CD)
                // priceListenerInfo.listeners.get(0).onMarketBookPartial(this,
                // priceListenerInfo.sortedBook,
                // priceListenerInfo.missingVenueCodes());
                priceListenerInfo.listeners.get(0).onMarketBookComplete(this, null);
            }
        }
        marketStatistics.timerExpired();
    }
}
