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
package it.softsolutions.bestx.markets.bondvision;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
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
import it.softsolutions.bestx.connections.bondvision.BondVisionConnection;
import it.softsolutions.bestx.connections.bondvision.BondVisionConnectionListener;
import it.softsolutions.bestx.connections.bondvision.BondVisionProposalInputLazyBean;
import it.softsolutions.bestx.connections.bondvision.BondVisionRFCQOutputLazyBean;
import it.softsolutions.bestx.connections.mts.MTSConnection;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelation;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionStatus;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener.StatusField;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.exceptions.InstrumentNotNegotiableOnMarketException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.markets.GenericPriceListenerInfo;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.markets.MarketMarketMakerPriceListenerInfo;
import it.softsolutions.bestx.markets.PriceTools;
import it.softsolutions.bestx.markets.ProposalDiscarder;
import it.softsolutions.bestx.markets.VenuePriceListenerInfo;
import it.softsolutions.bestx.markets.bondvision.exceptions.BVBestXFlyingException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
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
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.FlyingRFQService;
import it.softsolutions.bestx.services.MarketSecurityStatusService;
import it.softsolutions.bestx.services.MarketSecurityStatusService.QuantityValues;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: MTS Bondvision Market class
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 05/set/2012
 * 
 **/
public class BondVisionMarket extends MarketCommon implements MarketBuySideConnection, MarketPriceConnection, TimerEventListener, BondVisionConnectionListener, Connection, ConnectionListener, MarketMXBean,
        ProposalDiscarder {

    private static final Logger LOGGER = LoggerFactory.getLogger(BondVisionMarket.class);

    /** The Constant REAL_MARKET_NAME. */
    public static final String REAL_MARKET_NAME = "BVS";

    /** The Constant NO_QUOTATIONS_ERROR_MSG_REASON. */
    public static final String NO_QUOTATIONS_ERROR_MSG_REASON = "No quotation available";

    protected ConnectionHelper connectionHelper;
    protected ConnectionListener connectionListener;
    protected MarketFinder marketFinder;
    protected OperationRegistry operationRegistry;
    protected SerialNumberService sessionIdServer;
    protected VenueFinder venueFinder;
    protected Executor executor;
    protected MarketSecurityStatusService marketSecurityStatusService;
    protected InstrumentFinder instrumentFinder;

    private String serviceName;
    private final Map<String, MarketMarketMakerPriceListenerInfo> mmPriceListeners = new ConcurrentHashMap<String, MarketMarketMakerPriceListenerInfo>();
    private boolean isMarketStatusOn = false;
    private Map<String, VenuePriceListenerInfo> priceListeners = new ConcurrentHashMap<String, VenuePriceListenerInfo>();
    private MarketMakerFinder marketMakerFinder;

    // HashMap<instrumentISIN, bondType>
    private final Map<String, String> instrumentBondTypeMap = new HashMap<String, String>();
    // Map<bondType, Set<marketMarketMakerCode>>
    private final Map<String, Set<String>> bondTypeMarketMarketMakerMap = new HashMap<String, Set<String>>();
    private final Map<String, TradingRelation> marketMarketMakerTradingRelationMap = new HashMap<String, TradingRelation>();
    private final Map<String, List<String>> marketMarketMakerTradingRelationExceptionMap = new HashMap<String, List<String>>();

    private Market bvMarket;
    private BondVisionConnection bondVisionConnection;
    private boolean currentlyPerformingConnection = false;
    private boolean marketConnected = false;
    private boolean marketIdle = false;

    private volatile boolean isBuySideConnectionOn;
    private volatile boolean isPriceConnectionOn;

    private FlyingRFQService flyingRFQService;

    public void onConnection(Connection source) {
        LOGGER.info("Market Connected");
        marketConnected = true;
        currentlyPerformingConnection = false;
        if (connectionListener != null) {
            connectionListener.onConnection(this);
        }
    }

    public void onDisconnection(Connection source, String reason) {
        LOGGER.info("Market Disconnected");
        marketConnected = false;
        marketIdle = false;

        if (connectionListener != null) {
            connectionListener.onDisconnection(source, reason);
        }
    }

    @Override
    public void init() throws BestXException {
        checkPreRequisites();
        super.init();
        connectionHelper.setConnection(bondVisionConnection);
        connectionHelper.setConnectionListener(this);
        bvMarket = marketFinder.getMarketByCode(MarketCode.BV, null);
        
        JobExecutionDispatcher.INSTANCE.addTimerEventListener(BondVisionMarket.class.getSimpleName(), this);
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (connectionHelper == null) {
            throw new ObjectNotInitializedException("Connection helper not set");
        }
        if (bondVisionConnection == null) {
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
        if (marketMakerFinder == null) {
            throw new ObjectNotInitializedException("marketMakerFinder property not initialized");
        }
    }

    @Override
    public boolean okToSendBook(String isin) {
        MarketMarketMakerPriceListenerInfo info = mmPriceListeners.get(isin);
        return info == null || (info != null && info.missingProposalStates.size() == 0);
    }

    protected boolean venueAlreadyPresentInBook(SortedBook sortedBook, ClassifiedProposal proposal) {
        boolean result = false;
        List<ClassifiedProposal> bookProposals = null;

        switch (proposal.getSide()) {
        case ASK:
            bookProposals = sortedBook.getAskProposals();
            break;
        case BID:
            bookProposals = sortedBook.getBidProposals();
            break;
        default:
            break;
        }

        for (ClassifiedProposal a : bookProposals) {
            if (a.getVenue().getCode().equalsIgnoreCase(proposal.getVenue().getCode())) {
                return true;
            }
        }
        return result;
    }

    @Override
    public void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        checkBuySideConnection();
        String sessionId = listener.getIdentifier(OperationIdType.BV_SESSION_ID);
        LOGGER.info("Accept proposal from Bondvision - Session ID: " + sessionId);
        if (sessionId == null) {
            throw new BestXException("BV Session not found");
        }
        bondVisionConnection.acceptQuote(sessionId, proposal.getSenderQuoteId(), proposal.getOriginalPrice(), proposal.getYield(), proposal.getTimestamp(), proposal.getTimestampstr(), instrument);
    }

    @Override
    public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void matchOperations(Operation listener, Operation matching, Money ownPrice, Money matchingPrice) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        checkBuySideConnection();
        
        if(proposal == null) throw new BestXException("Cannot reject a null proposal"); 
        
        String sessionId = listener.getIdentifier(OperationIdType.BV_SESSION_ID);
        LOGGER.info("Accept proposal from Bondvision - Session ID: {}", sessionId);

        if (sessionId == null) {
            throw new BestXException("BV Session not found");
        }
        /*
         * AMC 20091212 per ora lasciamo scadere la RFQ. La reject e' testata e funziona, ma il mercato chiede di non usarla. Ebbene, noi la
         * usiamo lo stesso!
         */
        bondVisionConnection.rejectQuote(sessionId, proposal.getSenderQuoteId(), proposal.getOriginalPrice(), proposal.getYield(), proposal.getTimestamp(), proposal.getTimestampstr(), instrument);
    }

    @Override
    public void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();

        String operationId = flyingRFQService.get(marketOrder.getInstrument().getIsin());
        if (operationId != null) {
            throw new BVBestXFlyingException("Flying RFQ on BondVision on ISIN " + marketOrder.getInstrument().getIsin() + ". OrderId is " + operationId);
        }

        String bvSessionId = getOperationRFQSessionId(listener);
        LOGGER.debug("Sending RFQ, registering statistics.");
        marketStatistics.orderSent(listener.getOrder().getFixOrderId());
        LOGGER.info("Send RFQ to BV - Session ID: {}", bvSessionId);
        // create message
        BigDecimal qtyMultiplier = marketSecurityStatusService.getQuantityValues(MarketCode.BV, marketOrder.getInstrument())[QuantityValues.QTY_MULTIPLIER.getPosition()];
        BondVisionRFCQOutputLazyBean rfq = new BondVisionRFCQOutputLazyBean(marketOrder, qtyMultiplier);
        // send through the connection
        ((BondVisionConnection) bondVisionConnection).sendRfq(bvSessionId, rfq);
    }

    @Override
    public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
        try {
            return getQuotingMarket(instrument) != null; // cerca i sottomercati e deve trovare "BV"
        } catch (BestXException e) {
            LOGGER.error("An error occurred while finding quoting market" + " : " + e.toString(), e);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return marketConnected;
    }
    
    @Override
    public void ensurePriceAvailable() throws MarketNotAvailableException {
    	if (!isPriceConnectionAvailable()) {
            throw new MarketNotAvailableException("BV not connected");
        }
    }

    @Override
    public void queryPrice(MarketPriceConnectionListener listener, Set<Venue> venues, long maxLatency, Order order) throws BestXException {
        Instrument instrument = order.getInstrument();
        String fixOrderId = order.getFixOrderId();

        Market instrumentMarket = getQuotingMarket(instrument);
        marketStatistics.pricesRequested(instrument.getIsin(), venues.size());
        if (instrumentMarket == null) {
            throw new InstrumentNotNegotiableOnMarketException("Isin: " + instrument.getIsin() + " on Market: " + getMarketCode().name());
        }
        boolean isEnabled = isEnabled();
        // Do absolutely nothing to subscribe prices. Wait through the interface ProposalDiscarder the proposals coming from Bloomberg
        // Market
        LOGGER.info("Order {}, requesting price to BV for ISIN: {}", fixOrderId, instrument.getIsin());
        ArrayList<MarketMarketMaker> targetMarketMarketMakers = new ArrayList<MarketMarketMaker>();
        String timerName = SimpleMarketProposalAggregator.buildTimerName(instrument.getIsin(), order.getFixOrderId(), getMarketCode());
        if (order.getPriceDiscoverySelected() != null && order.getPriceDiscoverySelected().equals(PriceDiscoveryType.NATIVE_PRICEDISCOVERY)) {
            LOGGER.info("Order price discovery is " + order.getPriceDiscoverySelected() + ", querying directly the BV market.");
            String sessionId = getNewPriceSessionId();

            for (Venue venue : venues) {
                LOGGER.debug("Order {}, processing venue {}", fixOrderId, venue);
                if (VenueType.MARKET_MAKER.compareTo(venue.getVenueType()) == 0) {
                    try {
                        // 20110331 - MSA Added Check of market MarketMaker List
                        if (venue.getMarketMaker() != null && venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()) != null) {
                            if (venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()).isEmpty()) {
                                LOGGER.warn("Order " + fixOrderId + ",  Empty list of Market MarketMakers for marketCode " + getMarketCode());
                            } else {
                                MarketMarketMaker marketMarketMaker = venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()).get(0);
                                if (marketMarketMaker.canTrade(instrument) && isInstrumentTradableWithMarketMaker(instrument, marketMarketMaker) && isEnabled && venue.getMarketMaker().isEnabled()) {
                                    // get Bloomberg marketMarketMaker associated to this one
                                    List<MarketMarketMaker> bvMarketMarketMakers = venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode());
                                    if (bvMarketMarketMakers != null && bvMarketMarketMakers.size() > 0) {
                                        for (MarketMarketMaker mmm : bvMarketMarketMakers) {
                                            order.addMarketMakerNotQuotingInstr(mmm, Messages.getString("RejectProposalISINNotQuotedByMM"));
                                            LOGGER.debug("Order: " + order.getFixOrderId() + ". adding Market Maker: " + mmm + " to MarketMakerNotQuotingInstr");
                                            if (!targetMarketMarketMakers.contains(mmm)) {
                                                order.addMarketMakerNotQuotingInstr(mmm, Messages.getString("RejectProposalISINNotQuotedByMM"));
                                                targetMarketMarketMakers.add(mmm);
                                            }
                                        }
                                    }
                                } else {
                                    if (marketMarketMaker.canTrade(instrument) && venue.getMarketMaker().isEnabled()) {
                                        order.addMarketMakerNotQuotingInstr(marketMarketMaker, Messages.getString("BV.MarketMakerCannotTradeInstrument"));
                                        LOGGER.debug("Order: " + order.getFixOrderId() + ". adding Market Maker: " + marketMarketMaker + " to MarketMakerNotQuotingInstr");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Order " + fixOrderId + ", error while working on the venues to define those we have to enquire for prices.", e);
                        continue;
                    }
                }
            }

            // set here the operation registry to manage correctly an immediate timer expiration when
            // no market makers can trade the isin
            SimpleMarketProposalAggregator propAggregator = SimpleMarketProposalAggregator.getInstance();
            propAggregator.setOperationRegistry(operationRegistry);
            try {
                SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
                JobDetail newJob = simpleTimerManager.createNewJob(timerName, this.getClass().getSimpleName(), false /* no durable flag required*/, true /* request recovery*/, true /* monitorable */);
                Trigger trigger = null;
                if (targetMarketMarketMakers.size() > 0 && maxLatency > 0) {
                    //this timer is not repeatable
                    trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, maxLatency);
                } else {
                    // Dopo 1 secondo fa partire la onTimerExptired
                    //this timer is not repeatable
                    trigger = simpleTimerManager.createNewTrigger(timerName, this.getClass().getSimpleName(), false, 1000);
                }
                simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, true);
            } catch (SchedulerException e) {
                LOGGER.error("Error while scheduling price discovery wait timer!", e);
            }


            LOGGER.debug("Querying prices for Isin: " + instrument.getIsin());
            propAggregator.setMarketFinder(marketFinder);
            propAggregator.addBookListener(instrument, listener);

            if (targetMarketMarketMakers.size() > 0) {
                LOGGER.info("Order " + order.getFixOrderId() + ", " + targetMarketMarketMakers.size() + " market makers available for BV SDP, request prices.");
                bondVisionConnection.requestInstrumentPriceSnapshot(sessionId, instrument, null, getPriceConnectionName());
            } else {
                LOGGER.info("Order " + order.getFixOrderId() + ", no market makers available for BV SDP, do not request prices.");
            }
        } else {
            LOGGER.info("Order price discovery is " + order.getPriceDiscoverySelected() + ", querying the BBG market.");
            for (Venue venue : venues) {
                LOGGER.debug("Order {}, processing venue {}", fixOrderId, venue);
                if (VenueType.MARKET_MAKER.compareTo(venue.getVenueType()) == 0) {
                    try {
                        // 20110331 - MSA Added Check of market MarketMaker List
                        if (venue.getMarketMaker() != null && venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()) != null) {
                            if (venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()).isEmpty()) {
                                LOGGER.info("Order {}, venue [{}]: empty list of Market MarketMakers for marketCode {}", fixOrderId, venue.getCode(), getMarketCode());
                            } else {
                                MarketMarketMaker marketMarketMaker = venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()).get(0);
                                boolean canTrade = marketMarketMaker.canTrade(instrument);
                                if (canTrade && isInstrumentTradableWithMarketMaker(instrument, marketMarketMaker) && isEnabled && venue.getMarketMaker().isEnabled()) {
                                    // get Bloomberg marketMarketMaker associated to this one
                                    List<MarketMarketMaker> bvMarketMarketMakers = venue.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode());
                                    if (bvMarketMarketMakers != null && bvMarketMarketMakers.size() > 0) {
                                        for (MarketMarketMaker mmm : venue.getMarketMaker().getMarketMarketMakerForMarket(Market.MarketCode.BLOOMBERG)) {
                                            order.addMarketMakerNotQuotingInstr(mmm, Messages.getString("RejectProposalISINNotQuotedByMM"));
                                            LOGGER.debug("Order: " + order.getFixOrderId() + ". adding Market Maker: " + mmm + " to MarketMakerNotQuotingInstr");
                                            if (!targetMarketMarketMakers.contains(mmm)) {
                                                // ATTENZIONE: Puo' essere che il MM sia aggiunto piu' volte se ha piu' codici BBG
                                                order.addMarketMakerNotQuotingInstr(mmm, Messages.getString("RejectProposalISINNotQuotedByMM"));
                                                targetMarketMarketMakers.add(mmm);
                                                LOGGER.debug("BondVision market, added BBG MarketMarketMaker: " + mmm.getMarketSpecificCode() + " on BV request: BV code is "
                                                                + mmm.getMarketMaker().getMarketMarketMakerForMarket(getMarketCode()));
                                            }
                                        }
                                    }
                                } else {
                                    if (canTrade && venue.getMarketMaker().isEnabled()) {
                                        order.addMarketMakerNotQuotingInstr(marketMarketMaker, Messages.getString("BV.MarketMakerCannotTradeInstrument"));
                                        LOGGER.debug("Order: " + order.getFixOrderId() + ". adding Market Maker: " + marketMarketMaker + " to MarketMakerNotQuotingInstr");
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Order " + fixOrderId + ", error while working on the venues to define those we have to enquire for prices.", e);
                        continue;
                    }
                }
            }
        }

    }

    private String getPriceConnectionName() {
        return REAL_MARKET_NAME;
    }

    protected ClassifiedProposal createMissingProposal(ProposalSide side, Instrument instrument, MarketMarketMaker marketMarketMaker, Market bvMarket, Venue venue) {
        ClassifiedProposal proposal = new ClassifiedProposal();
        proposal.setMarket(bvMarket);
        proposal.setProposalState(ProposalState.REJECTED);
        proposal.setReason(Messages.getString("BV_PRICE_TIMEOUT.0"));
        proposal.setSide(side);
        proposal.setType(ProposalType.TRADEABLE);
        proposal.setTimestamp(DateService.newLocalDate());
        proposal.setPrice(new Money(instrument.getCurrency(), BigDecimal.ZERO));
        proposal.setQty(BigDecimal.ZERO);
        proposal.setMarketMarketMaker(marketMarketMaker);
        proposal.setVenue(venue);
        proposal.setFutSettDate(instrument.getBBSettlementDate());
        proposal.setNonStandardSettlementDateAllowed(true);
        return proposal;
    }

    @Override
    public void onConnectionStatusChange(boolean marketStatus, boolean userStatus) {
        LOGGER.debug("marketStatus = {}, userStatus = {}", marketStatus, userStatus);
        isMarketStatusOn = marketStatus;
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.BV;
    }

    protected SubMarketCode getSubMarketCode() {
        return null;
    }

    /**
     * On security definition.
     *
     * @param isin the isin
     * @param subMarket the sub market
     * @param settlementDate the settlement date
     * @param minQty the min qty
     * @param qtyTick the qty tick
     * @param multiplier the multiplier
     * @param bondType the bond type
     */
    @Deprecated
    private void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal qtyTick, BigDecimal multiplier, String bondType) {
        // AMC remove infinite loop from depracated method (findBugs finding)
    	//onSecurityDefinition(isin, subMarket, settlementDate, minQty, qtyTick, multiplier, bondType);
        onSecurityStatus(isin, subMarket, "NEG");
        instrumentBondTypeMap.put(isin, bondType);
    }

    /**
     * On security status.
     *
     * @param isin the isin
     * @param subMarket the sub market
     * @param statusCode the status code
     */
    public void onSecurityStatus(String isin, String subMarket, String statusCode) {
        SubMarketCode subMarketCode = null;
        try {
            subMarketCode = SubMarketCode.valueOf(subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Received market definition on security from unknown Sub Market: " + subMarket + " : " + e.toString(), e);
            return;
        } catch (NullPointerException npe) {
            LOGGER.info("Received market definition on security for null Sub Market: trying to use HIMTF");
            subMarketCode = SubMarketCode.HIMTF;
        }
        Instrument.QuotingStatus quotingStatus = null;
        try {
            quotingStatus = Instrument.QuotingStatus.valueOf(statusCode);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Received unknown quoting status: " + statusCode + " : " + e.toString(), e);
            return;
        }
        try {
            marketSecurityStatusService.setMarketSecurityQuotingStatus(getMarketCode(), subMarketCode, isin, quotingStatus);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving quoting status for instrument: " + isin + " : " + e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving quoting status for instrument: " + isin + " : " + e.toString(), e);
        }
    }

    @Override
    public String getConnectionName() {
        return MarketCode.BV.name();
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
    public boolean isAMagnetMarket() {
        return false;
    }

    /**
     * Gets the service name.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Sets the service name.
     *
     * @param serviceName the new service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void onExecutionReport(String rfqSeqNo, final ExecutionReportState executionReportState, final String marketOrderId, final String counterpart, final BigDecimal lastPrice,
                    final String contractNo, final BigDecimal accruedValue, final Date marketTime) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BV_RFQ_ID, rfqSeqNo);
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);
            if (lastPrice.doubleValue() > 0.0) {
               marketStatistics.orderResponseReceived(orderId, operation.getOrder().getQty().doubleValue());
            } else {
               marketStatistics.orderResponseReceived(orderId, 0.0);
            }

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Order order = operation.getOrder();

                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setExecBroker(counterpart);
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    marketExecutionReport.setMarket(bvMarket);
                    marketExecutionReport.setOrderQty(order.getQty());
                    BigDecimal convPrice;
                    int quoteIndicator = marketSecurityStatusService.getQuotIndicator(MarketCode.BV, operation.getOrder().getInstrument().getIsin());
                    if (quoteIndicator == MarketSecurityStatusService.VALUE_PRICE_32) {
                        convPrice = PriceTools.convertPriceFrom32(lastPrice);
                    } else {
                        convPrice = lastPrice;
                    }
                    marketExecutionReport.setPrice(new Money(order.getCurrency(), convPrice));
                    marketExecutionReport.setLastPx(lastPrice);
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setState(executionReportState);
                    marketExecutionReport.setTransactTime(marketTime);
                    marketExecutionReport.setSequenceId(null); // ID only
                    marketExecutionReport.setMarketOrderID(marketOrderId);
                    marketExecutionReport.setTicket(contractNo);
                    marketExecutionReport.setAccruedInterestAmount(null);
                    marketExecutionReport.setAccruedInterestRate(null);
                    if (order.getInstrument().getBBSettlementDate() != null && order.getInstrument().getCurrency() != null && order.getInstrument().getRateo() != null
                                    && order.getFutSettDate().equals(order.getInstrument().getBBSettlementDate()) && order.getCurrency().equals(order.getInstrument().getCurrency())) {

                        BigDecimal interestAmount;
                        BigDecimal rateo = order.getInstrument().getRateo();
                        rateo = rateo.setScale(10);

                        BigDecimal qty = order.getQty();
                        qty = qty.setScale(5);
                        interestAmount = rateo.multiply(qty).divide(new BigDecimal(100.00));
                        interestAmount = interestAmount.setScale(2, BigDecimal.ROUND_HALF_UP);
                        marketExecutionReport.setAccruedInterestAmount(new Money(order.getInstrument().getCurrency(), interestAmount));
                        marketExecutionReport.setAccruedInterestRate(rateo);
                    }
                    operation.onMarketExecutionReport(BondVisionMarket.this, order, marketExecutionReport);

                    operationRegistry.removeOperationBinding(operation, OperationIdType.BV_SESSION_ID);
                }
            });

        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with BV RFQ ID: " + rfqSeqNo + " : " + e.toString(), e);
        }
    }

    @Override
    public void onQuote(final String sessionId, final String rfqSeqNo, final String quoteId, final String marketMaker, final BigDecimal price, final BigDecimal yield, final BigDecimal qty,
                    final ProposalSide side, final Date futSettDate, final Date updateTime, final String updateTimeStr) {
        LOGGER.info("sessionId = {}, rfqSeqNo = {}, quoteId = {}, marketMaker = {}, price = {}, yield = {}, qty = {}, side = {}, futSettDate = {}, updateTime = {}, updateTimeStr = {}", 
                        sessionId, rfqSeqNo, quoteId, marketMaker, price, yield, qty, side, futSettDate, updateTime, updateTimeStr);
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BV_SESSION_ID, sessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {

                    // check che non vi sia gia' una counter in esecuzione per questa operation
                    if (!operation.isProcessingCounter()) {
                        try {
                            operationRegistry.bindOperation(operation, OperationIdType.BV_RFQ_ID, rfqSeqNo);
                        } catch (BestXException e) {
                            LOGGER.error("Error while setting Operation to BV RFQ ID: {}", rfqSeqNo, e);
                        }

                        String orderId = operation.getOrder().getFixOrderId();
                        LOGGER.debug("Quote received for the order {}, registering statistics.", orderId);
                        // marketStatistics.orderResponseReceived(orderId);

                        LOGGER.debug("[BVCNT] Setting flag to true");
                        operation.setProcessingCounter(true);

                        ClassifiedProposal proposal = new ClassifiedProposal();
                        Instrument instrument = operation.getOrder().getInstrument();
                        MarketMarketMaker marketMarketMaker = null;
                        try {
                            marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(MarketCode.BV, marketMaker);
                        } catch (BestXException e) {
                            LOGGER.error("Error while finding Market Maker with Bondvision code: {}", marketMaker, e);
                            return;
                        }
                        Venue venue = null;
                        try {
                            venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
                        } catch (BestXException e) {
                            LOGGER.error("Error while finding Venue for Market Maker: " + marketMarketMaker.getMarketSpecificCode() + " : " + e.toString(), e);
                            return;
                        }
                        try {
                            proposal.setMarket(marketFinder.getMarketByCode(getMarketCode(), getSubMarketCode()));
                        } catch (BestXException e) {
                            LOGGER.error("Error while finding Market for Markect code : " + getMarketCode() + ", " + getSubMarketCode() + " : " + e.toString(), e);
                            return;
                        }
                        Venue bvVenue = new Venue(venue);
                        bvVenue.setMarket(bvMarket);
                        int quoteIndicator = marketSecurityStatusService.getQuotIndicator(MarketCode.BV, instrument.getIsin());
                        BigDecimal convPrice;
                        if (quoteIndicator == MarketSecurityStatusService.VALUE_PRICE_32) {
                            convPrice = PriceTools.convertPriceFrom32(price);
                        } else {
                            convPrice = price;
                        }
                        proposal.setProposalState(ProposalState.NEW);
                        proposal.setSide(side);
                        proposal.setType(Proposal.ProposalType.COUNTER);
                        proposal.setPrice(new Money(instrument.getCurrency(), convPrice));
                        proposal.setOriginalPrice(price);
                        proposal.setYield(yield);
                        BigDecimal qtyMultiplier = marketSecurityStatusService.getQuantityValues(MarketCode.BV, instrument)[QuantityValues.QTY_MULTIPLIER.getPosition()];
                        proposal.setQty(qty.multiply(qtyMultiplier));
                        proposal.setMarketMarketMaker(marketMarketMaker);
                        proposal.setVenue(bvVenue);
                        proposal.setFutSettDate(futSettDate);
                        proposal.setNonStandardSettlementDateAllowed(true);
                        // AMC La giustapposizione dei due identificativi e' necessaria, poiche' BV vuole entrambe nella
                        // accettazione/rifiuto della quote
                        proposal.setSenderQuoteId(quoteId + '|' + rfqSeqNo);
                        proposal.setTimestamp(updateTime);
                        proposal.setTimestampstr(updateTimeStr);
                        operation.onMarketProposal(BondVisionMarket.this, instrument, proposal);

                    } else {
                        LOGGER.info("[BVRFCQ] BondVision RFCQ not handled, there's already one active");
                        LOGGER.info("[BVRFCQ],CounterData,sessionID:" + (sessionId != null ? sessionId : "") + ",MarketMaker:" + (marketMaker != null ? marketMaker : "") + ",QuoteId:"
                                        + (quoteId != null ? quoteId : "") + ",price:" + (price != null ? price.toPlainString() : "") + ",quantity:" + (qty != null ? qty.toPlainString() : "") + ",side:"
                                        + (side != null ? side.name() : ""));
                    }

                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Bondvision Session ID: " + sessionId + " : " + e.toString(), e);
        }
    }

    private void checkBuySideConnection() throws BestXException {
        if (!isConnected()) {
            throw new MarketNotAvailableException("BV connection is off");
        }
        if (!isMarketIdle()) {
            throw new MarketNotAvailableException("BV connection not yet idle");
        }
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("BV buy side connection is disabled");
        }
    }

    protected MarketBuySideConnection getMarket() {
        return this;
    }

    protected String getNewPriceSessionId() throws BestXException {
        return sessionIdServer.getUniqueIdentifier("BVSDP_PRICE_REQUEST_ID", "P%08d");
    }

    protected Venue getVirtualVenue() {
        throw new UnsupportedOperationException();
    }

    /**
     * On execution report.
     *
     * @param regSessionId the reg session id
     * @param executionReportState the execution report state
     * @param regulatedFillInputBean the regulated fill input bean
     */
    public void onExecutionReport(String regSessionId, ExecutionReportState executionReportState, RegulatedFillInputBean regulatedFillInputBean) {
        throw new UnsupportedOperationException();
    }

    public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField) {
        LOGGER.info("[{}] = {}, subMarketCode = {}", statusField, status, subMarketCode);

        switch (statusField) {
        case MARKET_STATUS:
            isMarketStatusOn = status;
            break;
        case MEMBER_STATUS:
            break;
        case USER_STATUS:
            break;
        default:
            break;
        }
    }

    /**
     * On order cancelled.
     *
     * @param regSessionId the reg session id
     * @param reason the reason
     */
    public void onOrderCancelled(String regSessionId, String reason) {
        throw new UnsupportedOperationException();
    }

    public void onOrderTechnicalReject(String sessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BV_SESSION_ID, sessionId);
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Technical reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderTechnicalReject(getMarket(), operation.getOrder(), reason);
                    operationRegistry.removeOperationBinding(operation, OperationIdType.BV_SESSION_ID);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Bondvision Session ID: " + sessionId);
        }
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return isConnected() && (isMarketStatusOn) && (marketIdle);
    }

    @Override
    public boolean isPriceConnectionAvailable() {
        return marketConnected;
    }

    @Override
    public void onMemberEnabled(String member, String bondType, boolean enabled) {
        Set<String> marketMakers = bondTypeMarketMarketMakerMap.get(bondType);
        if (marketMakers == null) {
            // add the bondType entry
            marketMakers = new TreeSet<String>();
            bondTypeMarketMarketMakerMap.put(bondType, marketMakers);
        }
        if (enabled) {
            if (marketMakers.add(member)) {
                LOGGER.info("[BV_TRADING_PERMISSIONS] Added couple <" + member + ", " + bondType + "> to enabled member-bondType map");
            }
        } else {
            if (marketMakers.remove(member)) {
                LOGGER.info("[BV_TRADING_PERMISSIONS] Removed couple <" + member + ", " + bondType + "> to enabled member-bondType map");
            }
        }
    }

    @Override
    public void onTradingRelation(String member, TradingRelationStatus status, TradingRelationStatus sellSideSubStatus, TradingRelationStatus buySideSubStatus, TradingRelationEvent event) {
        TradingRelation tradingRelation = marketMarketMakerTradingRelationMap.remove(member);
        if (tradingRelation != null) {
            LOGGER.info("[BV_TRADING_PERMISSIONS]  removed tradingRelation: " + tradingRelation.toString() + " from member: " + member);
        }
        tradingRelation = new TradingRelation(status, sellSideSubStatus, buySideSubStatus, event);
        if (tradingRelation.getStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0 && tradingRelation.getSellSideSubStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0
                        && tradingRelation.getBuySideSubStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0 && tradingRelation.getEvent().compareTo(TradingRelationEvent.ACCEPT) == 0) {
            marketMarketMakerTradingRelationMap.put(member, tradingRelation);
            LOGGER.info("[BV_TRADING_PERMISSIONS]  added tradingRelation: " + tradingRelation.toString() + " to member: " + member);
        }
    }

    @Override
    public void onTradingRelationException(String member, String bondType, TradingRelationExceptionStatus status, TradingRelationExceptionEvent event) {
        List<String> bondTypeList = marketMarketMakerTradingRelationExceptionMap.get(member);
        if (bondTypeList == null) {
            bondTypeList = new ArrayList<String>();
            marketMarketMakerTradingRelationExceptionMap.put(member, bondTypeList);
        }
        if (status.compareTo(TradingRelationExceptionStatus.ACTIVE) == 0 && event.compareTo(TradingRelationExceptionEvent.INSERT) == 0) {
            if (!bondTypeList.contains(bondType)) {
                bondTypeList.add(bondType);
                LOGGER.info("[BV_TRADING_PERMISSIONS]  added tradingRelationException: " + bondType + " to member: " + member);
            }
        } else {
            if (bondTypeList.contains(bondType)) {
                bondTypeList.remove(bondType);
                LOGGER.info("[BV_TRADING_PERMISSIONS]  removed tradingRelationException: " + bondType + " to member: " + member);
            }
        }
    }

    /*
     * (non-Javadoc) Does not consider the MarketMarketMaker EnableFilter configuration. Just returns true if the relations on BV between
     * the MarketMarketMaker and my members and the enable of the MarketMarketMaker on BV allow trading on the instrument
     * 
     * @see it.softsolutions.bestx.connections.MarketConnection#isInstrumentTradableWithMarketMaker(it.softsolutions.bestx.model.Instrument,
     * it.softsolutions.bestx.model.MarketMarketMaker)
     */
    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMarketMaker) {
        String bondType = marketSecurityStatusService.getMarketBondType(getMarketCode(), instrument.getIsin());
        if (!isInstrumentQuotedOnMarket(instrument)) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + marketMarketMaker.getMarketSpecificCode() + " can't trade instrument " + instrument.getIsin()
                            + ". Instrument not quoted on BV");
            return false;
        }
        // get the trading relations. It is not null only if has arrived a TR with all Accepted status and accept event
        TradingRelation tr = marketMarketMakerTradingRelationMap.get(marketMarketMaker.getMarketSpecificCode());
        if (tr == null) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + marketMarketMaker.getMarketSpecificCode() + " can't trade instrument " + instrument.getIsin()
                            + ". No trading relation with member on BV");
            return false;
        }
        // get the Trading relation exceptions. It is not null only if there is an accepted exception with event inserted
        List<String> tre = marketMarketMakerTradingRelationExceptionMap.get(marketMarketMaker.getMarketSpecificCode());
        if (tre != null && (bondType == null || tre.contains(bondType))) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + (marketMarketMaker.getMarketSpecificCode() != null ? marketMarketMaker.getMarketSpecificCode() : "null")
                            + " can't trade instrument " + (instrument.getIsin() != null ? instrument.getIsin() : "null") + ". A trading relation exception exists with member on BV");
            return false;
        }
        // get the member enable
        if (bondType == null) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + (marketMarketMaker.getMarketSpecificCode() != null ? marketMarketMaker.getMarketSpecificCode() : "null")
                            + " can't trade instrument " + (instrument.getIsin() != null ? instrument.getIsin() : "null")
                            + ". BondType is not available: a trading relation exception exists with member on BV");
            return false;
        }
        Set<String> mmmset = bondTypeMarketMarketMakerMap.get(bondType);
        if (mmmset == null) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + marketMarketMaker.getMarketSpecificCode() + " can't trade instrument " + instrument.getIsin()
                            + ". This member does not trade instrument on BV");
            return false;
        }
        if (!mmmset.contains(marketMarketMaker.getMarketSpecificCode())) {
            LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + marketMarketMaker.getMarketSpecificCode() + " can't trade instrument " + instrument.getIsin()
                            + ". This member does not trade instrument on BV");
            return false;
        }
        LOGGER.debug("[BV_TRADING_PERMISSIONS] marketMarketMaker " + marketMarketMaker.getMarketSpecificCode() + " can trade instrument " + instrument.getIsin());
        return true;
    }

    @Override
    public void onOrderReject(final String sessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BV_SESSION_ID, sessionId);
            if (operation == null) {
                return;
            }
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderReject(getMarket(), operation.getOrder(), reason, sessionId);
                    operationRegistry.removeOperationBinding(operation, OperationIdType.BV_SESSION_ID);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with BondVision Session ID: " + sessionId + " : " + e.toString(), e);
        }
    }

    /**
     * Gets the market maker finder.
     *
     * @return the market maker finder
     */
    public MarketMakerFinder getMarketMakerFinder() {
        return marketMakerFinder;
    }

    /**
     * Sets the market maker finder.
     *
     * @param marketMakerFinder the new market maker finder
     */
    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    // UTILITY METHODS

    private String getNewRFQId() throws BestXException {
        String bvSessionId = Long.toString(sessionIdServer.getSerialNumber("BV_SESSION_ID,"));
        return bvSessionId;
    }

    private String getOperationRFQSessionId(Operation operation) throws BestXException {
        String bvSessionId = operation.getIdentifier(OperationIdType.BV_SESSION_ID);
        if (bvSessionId == null) {
            bvSessionId = getNewRFQId();
            operationRegistry.bindOperation(operation, OperationIdType.BV_SESSION_ID, bvSessionId);
        }
        return bvSessionId;
    }

    /**
     * Gets the flying rfq service.
     *
     * @return the flying rfq service
     */
    public FlyingRFQService getFlyingRFQService() {
        return flyingRFQService;
    }

    /**
     * Sets the flying rfq service.
     *
     * @param flyingRFQService the new flying rfq service
     */
    public void setFlyingRFQService(FlyingRFQService flyingRFQService) {
        this.flyingRFQService = flyingRFQService;
    }

    @Override
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal qtyTick, BigDecimal multiplier, String bondType, int marketAffiliation,
                    String marketAffiliationStr, int quoteIndicator, String quoteIndicatorStr) {
        SubMarketCode subMarketCode = null;
        try {
            marketSecurityStatusService.setMarketSecuritySettlementDateAndBondType(getMarketCode(), subMarketCode, isin, settlementDate, bondType, marketAffiliation, marketAffiliationStr,
                            quoteIndicator, quoteIndicatorStr);
            marketSecurityStatusService.setMarketSecurityQuantity(getMarketCode(), subMarketCode, isin, minQty, qtyTick, multiplier);
            marketSecurityStatusService.setMarketSecurityQuotingStatus(getMarketCode(), subMarketCode, isin, Instrument.QuotingStatus.NEG);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        }
        instrumentBondTypeMap.put(isin, bondType);
    }

    @Override
    public void onDownloadEnd(Connection source) {
        marketIdle = true;
    }

    @Override
    public boolean isEnabled() {
        return isConnected() && isPriceConnectionEnabled();
    }

    /**
     * Gets the bond vision connection.
     *
     * @return the bond vision connection
     */
    public MTSConnection getBondVisionConnection() {
        return bondVisionConnection;
    }

    /**
     * Sets the bond vision connection.
     *
     * @param bondVisionConnection the new bond vision connection
     */
    public void setBondVisionConnection(BondVisionConnection bondVisionConnection) {
        this.bondVisionConnection = bondVisionConnection;
        this.bondVisionConnection.setMTSConnectionListener(this);
    }

    /**
     * Sets the session id server.
     *
     * @param sessionIdServer the new session id server
     */
    public void setSessionIdServer(SerialNumberService sessionIdServer) {
        this.sessionIdServer = sessionIdServer;
    }

    /**
     * Sets the executor.
     *
     * @param executor the new executor
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Sets the instrument finder.
     *
     * @param instrumentFinder the new instrument finder
     */
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    /**
     * Sets the venue finder.
     *
     * @param venueFinder the new venue finder
     */
    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    /**
     * Sets the market finder.
     *
     * @param marketFinder the new market finder
     */
    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    /**
     * Sets the operation registry.
     *
     * @param operationRegistry the new operation registry
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * Sets the market security status service.
     *
     * @param marketSecurityStatusService the new market security status service
     */
    public void setMarketSecurityStatusService(MarketSecurityStatusService marketSecurityStatusService) {
        this.marketSecurityStatusService = marketSecurityStatusService;
    }

    /**
     * Sets the connection helper.
     *
     * @param connectionHelper the new connection helper
     */
    public void setConnectionHelper(ConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    @Override
    public int countOrders() {
        int result = 0;
        for (GenericPriceListenerInfo priceListenerInfo : priceListeners.values()) {
            result += priceListenerInfo.listeners.size();
        }
        return result;
    }

    @Override
    public void connect() throws BestXException {
        LOGGER.info("Connecting to " + getConnectionName());
        if (!currentlyPerformingConnection) {
            currentlyPerformingConnection = true;
            connectionHelper.connect();
        }
    }

    public void disconnect() throws BestXException {
        LOGGER.info("Disconnecting from " + getConnectionName());
        currentlyPerformingConnection = false;
        connectionHelper.disconnect();
    }

    public void onConnectionError() {
        LOGGER.info("Error connecting - Disconnect from " + getConnectionName());
        connectionHelper.disconnect();
    }

    @Override
    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
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

    public void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
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
    public synchronized void startBuySideConnection() throws BestXException {
        LOGGER.info("Start " + getConnectionName() + " buy side connection");
        if (!marketConnected) {
            connect();
        }
        isBuySideConnectionOn = true;
    }

    @Override
    public synchronized void stopBuySideConnection() throws BestXException {
        LOGGER.info("Stop " + getConnectionName() + " buy side connection");
        isBuySideConnectionOn = false;
        if (!isPriceConnectionOn) {
            disconnect();
        }
    }

    @Override
    public synchronized void startPriceConnection() throws BestXException {
        LOGGER.info("Start " + getConnectionName() + " price connection");
        if (!marketConnected) {
            connect();
        }
        isPriceConnectionOn = true;
    }

    @Override
    public synchronized void stopPriceConnection() throws BestXException {
        LOGGER.info("Stop " + getConnectionName() + " price connection");
        isPriceConnectionOn = false;
        if (!isBuySideConnectionOn) {
            disconnect();
        }
    }

    @Override
    public void onInstrumentPrices(String bondVisionSessionId, ArrayList<BondVisionProposalInputLazyBean> bvProposals) {
        String isin = null;
        if (bvProposals == null) {
            LOGGER.error("ERROR : no prices received, but price management called anyway for the session " + bondVisionSessionId);
            return;
        }

        isin = bvProposals.get(0).getIsin();
        if (isin == null) {
            LOGGER.debug("No ISIN in the first BV proposal : {}", bvProposals.get(0));
            priceListeners.remove(isin);
            return;
        }

        Instrument instrument = instrumentFinder.getInstrumentByIsin(isin);

        if (instrument == null) {
            LOGGER.info("No Instrument found with ISIN: " + isin);
            priceListeners.remove(isin);
            return;
        }

        SimpleMarketProposalAggregator propAggregator = SimpleMarketProposalAggregator.getInstance();

        ArrayList<ClassifiedProposal> inProposals = new ArrayList<ClassifiedProposal>();
        for (BondVisionProposalInputLazyBean bvProposal : bvProposals) {
            LOGGER.info("BV proposal : " + bvProposal.toString());

            ClassifiedProposal proposal = null;
            try {
                proposal = getProposal(bvProposal, instrument);
                if (proposal != null) {
                    inProposals.add(proposal);
                }
            } catch (BestXException e) {
                LOGGER.error("Error while building a book proposal from the bondvision proposal " + bvProposal, e);
            }
        }

        Map<MarketMarketMaker, ClassifiedProposal> bidProposals = new HashMap<MarketMarketMaker, ClassifiedProposal>();
        Map<MarketMarketMaker, ClassifiedProposal> askProposals = new HashMap<MarketMarketMaker, ClassifiedProposal>();

        try {
            levelBook(inProposals, bidProposals, askProposals);
        } catch (BestXException e1) {
            LOGGER.error("Error while levelling book : ", e1);
        }

        propAggregator.cleanMarketBookOnNewPrices(instrument, getMarketCode());

        int totalSize = bidProposals.size() + askProposals.size();
        int i = 0;
        try {
            for (ClassifiedProposal prop : bidProposals.values()) {
                i++;
                boolean isLastProposal = (i == totalSize);
                propAggregator.onProposal(instrument, prop, this, 0, "", isLastProposal);
                marketStatistics.pricesResponseReceived(bondVisionSessionId, 1);
            }
            for (ClassifiedProposal prop : askProposals.values()) {
                i++;
                boolean isLastProposal = (i == totalSize);
                propAggregator.onProposal(instrument, prop, this, 0, "", isLastProposal);
                marketStatistics.pricesResponseReceived(bondVisionSessionId, 1);
            }
        } catch (BestXException e) {
            LOGGER.error("Error while managing price replies from BV SDP market.", e);
        }
        
    }

    /*
     * 
     * Makes both book levels the same size, adding proposal with null price if needed
     */
    protected void levelBook(ArrayList<ClassifiedProposal> inProposals, Map<MarketMarketMaker, ClassifiedProposal> outBidProposals, Map<MarketMarketMaker, ClassifiedProposal> outAskProposals)
                    throws BestXException {
        if (inProposals == null) {
            throw new BestXException("Null input proposal list");
        }
        if (outBidProposals == null) {
            throw new BestXException("Null bid proposals list");
        }
        if (outAskProposals == null) {
            throw new BestXException("Null ask proposals list");
        }

        for (ClassifiedProposal proposal : inProposals) {
            LOGGER.info("BV proposal : " + proposal.toString());

            MarketMarketMaker mmm = proposal.getMarketMarketMaker();
            if (proposal.getSide().equals(ProposalSide.BID)) {
                outBidProposals.put(mmm, proposal);
                if (!outAskProposals.containsKey(mmm)) { // add empty proposal in the meanwhile
                    ClassifiedProposal emptyProposal = proposal.clone();
                    Money origPrice = emptyProposal.getPrice();
                    emptyProposal.setPrice(new Money(origPrice.getStringCurrency(), new BigDecimal(0.0)));
                    emptyProposal.setQty(new BigDecimal(0.0));
                    emptyProposal.setSide(ProposalSide.ASK);
                    outAskProposals.put(mmm, emptyProposal);
                }
            } else {
                outAskProposals.put(mmm, proposal);
                if (!outBidProposals.containsKey(mmm)) { // add empty proposal in the meanwhile
                    ClassifiedProposal emptyProposal = proposal.clone();
                    Money origPrice = emptyProposal.getPrice();
                    emptyProposal.setPrice(new Money(origPrice.getStringCurrency(), new BigDecimal(0.0)));
                    emptyProposal.setQty(new BigDecimal(0.0));
                    emptyProposal.setSide(ProposalSide.BID);
                    outBidProposals.put(mmm, emptyProposal);
                }
            }
        }
    }

    protected ClassifiedProposal getProposal(BondVisionProposalInputLazyBean bvProposal, Instrument instrument) throws BestXException {

        ClassifiedProposal proposal = null;

        MarketMarketMaker marketMarketMaker = null;
        try {
            marketMarketMaker = marketMakerFinder.getMarketMarketMakerByCode(getMarketCode(), bvProposal.getBondVisionMarketMaker());
        } catch (BestXException e) {
            LOGGER.error("Error while finding " + getMarketCode() + " Market Maker: " + bvProposal.getBondVisionMarketMaker() + " : " + e.toString(), e);
            return null;
        }
        if (marketMarketMaker == null) {
            LOGGER.error("No Market Maker found for " + getMarketCode() + " code: " + bvProposal.getBondVisionMarketMaker());
            return null;
        }

        Venue venue = null;
        try {
            venue = venueFinder.getMarketMakerVenue(marketMarketMaker.getMarketMaker());
        } catch (BestXException e) {
            LOGGER.error("Error while finding Market Maker Venue: " + marketMarketMaker.getMarketSpecificCode() + " : " + e.toString(), e);
            return null;
        }
        if (venue == null) {
            LOGGER.error("No Venue found for Market Maker code: " + marketMarketMaker.getMarketSpecificCode());
            return null;
        }
        BigDecimal[] instrQuantities = marketSecurityStatusService.getQuantityValues(MarketCode.BV, instrument);
        BigDecimal qtyMultiplier = BigDecimal.ONE;
        if (instrQuantities != null && instrQuantities.length > 0)
            qtyMultiplier = instrQuantities[QuantityValues.QTY_MULTIPLIER.getPosition()];
        else {
            LOGGER.warn("Cannot find conversion quantities for the proposal, as default the conversion multiplier will be 1.");
        }

        Venue bvVenue = new Venue(venue);
        bvVenue.setMarket(bvMarket);
        LOGGER.debug("Multiplier to convert BondVision quantities to BestX book quantities: {}", qtyMultiplier);
        proposal = new ClassifiedProposal();
        proposal.setMarket(bvMarket);
        proposal.setProposalState(ProposalState.NEW);
        proposal.setSide(bvProposal.getSide());
        proposal.setType(bvProposal.getType());
        proposal.setTimestamp(bvProposal.getTimestamp());
        proposal.setPrice(new Money(instrument.getCurrency(), bvProposal.getPrice()));
        proposal.setQty(bvProposal.getQty().multiply(qtyMultiplier));
        proposal.setMarketMarketMaker(marketMarketMaker);
        proposal.setVenue(bvVenue);
        proposal.setSenderQuoteId(bvProposal.getSenderQuoteId());

        return proposal;
    }

    protected boolean isPriceLimitWorseThanPrice(OrderSide orderSide, ProposalSide side, BigDecimal priceLimit, BigDecimal price) {
        if (priceLimit == null || price == null) {
            return false;
        }

        return ((orderSide == OrderSide.SELL && side == ProposalSide.BID && priceLimit.compareTo(price) > 0) || (orderSide == OrderSide.BUY && side == ProposalSide.ASK && priceLimit.compareTo(price) < 0));
    }

    @Override
    public void onNullPrices(String bondVisionSessionId, String isin, ProposalSide side) {
        LOGGER.warn("Error reply for a price request, session id " + bondVisionSessionId + ", instrument " + isin + ", nothing to do in " + getMarketCode() + " market.");
    }

    @Override
    public void onNullPrices(String bondVisionSessionId, String isin, String reason) {

        Instrument instrument = instrumentFinder.getInstrumentByIsin(isin);

        if (instrument == null) {
            LOGGER.info("No Instrument found with ISIN: " + isin);
                priceListeners.remove(isin);
            return;
        }

        SimpleMarketProposalAggregator propAggregator = SimpleMarketProposalAggregator.getInstance();

        if (reason != null && reason.equals(NO_QUOTATIONS_ERROR_MSG_REASON)) {
            LOGGER.info("Reason {}: it is not an error, there are not market makers quoting the instrument at the moment.", reason);
            propAggregator.onNoQuotationAvailable(isin, this);
        } else {
            propAggregator.onBookError(isin, this);
        }

    }

    @Override
    public boolean isMarketIdle() {
        return marketIdle;
    }

    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProposalDiscarded(Instrument instrument, ClassifiedProposal proposal, int errorCode) {
        // not needed
        return false;
    }

    @Override
    public void cleanBook() {
        SimpleMarketProposalAggregator.getInstance().clearBooks();
    }

    @Override
    public void ackProposal(Operation listener, Proposal proposal) throws BestXException {
        // Do nothing
        
    }

	@Override
    public int getActiveTimersNum() {
	    // Do nothing
	    return 0;
    }

	@Override
    public void timerExpired(String jobName, String groupName) {
		if (!jobName.contains(SimpleMarketProposalAggregator.TIMER_NAME_SEPARATOR) && !jobName.contains(getMarketCode().toString())) {
            LOGGER.info("Timeout reached on BondVision price retrieval but identifier was not BV: " + jobName);
            return;
        }
        SimpleMarketProposalAggregator.getInstance().onTimerExpired(this, jobName);

        marketStatistics.timerExpired();

        if (jobName != null && jobName.contains(SimpleMarketProposalAggregator.TIMER_NAME_SEPARATOR)) {
            marketStatistics.pricesResponseReceived(jobName.split(SimpleMarketProposalAggregator.TIMER_NAME_SEPARATOR)[1], 1);
        }	    
    }
}
