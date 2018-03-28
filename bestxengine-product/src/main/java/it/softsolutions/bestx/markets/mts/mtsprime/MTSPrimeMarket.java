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
package it.softsolutions.bestx.markets.mts.mtsprime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelation;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionStatus;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus;
import it.softsolutions.bestx.connections.mts.MTSConnectionListener;
import it.softsolutions.bestx.connections.mts.MTSConnector;
import it.softsolutions.bestx.connections.mts.bondvision.BondVisionProposalInputLazyBean;
import it.softsolutions.bestx.connections.mts.mtsprime.MTSPrimeOrderOutputLazyBean;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.markets.VenuePriceListenerInfo;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: paolo.midali 
 * Creation date: 13-nov-2012 
 * 
 **/

public class MTSPrimeMarket extends RegulatedMarket implements MTSConnectionListener, RegulatedConnectionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MTSPrimeMarket.class);

    /** The Constant NO_QUOTATIONS_ERROR_MSG_REASON. */
    public static final String NO_QUOTATIONS_ERROR_MSG_REASON = "No quotation available";

    private final Venue virtualVenue = new Venue("MTSPRIME");

    // used as key in PriceListenerInfo price map - Not a real Venue
    private boolean isMarketStatusOn = false;
    private RegulatedMktIsinsLoader regulatedMktIsinsLoader;
    private final Map<String, Set<String>> bondTypeMarketMarketMakerMap = new HashMap<String, Set<String>>(); // HashMap<bondType,
    // Set<marketMarketMakerCode>>
    private final Map<String, String> instrumentBondTypeMap = new HashMap<String, String>(); // HashMap<instrumentISIN,
    // bondType>
    private final Map<String, TradingRelation> marketMarketMakerTradingRelationMap = new HashMap<String, TradingRelation>();
    private final Map<String, List<String>> marketMarketMakerTradingRelationExceptionMap = new HashMap<String, List<String>>();
    private boolean marketIdle = false;

    private MTSConnector mtsPrimeConnection;

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onDisconnection(it.softsolutions.bestx.connections.Connection, java.lang.String)
     */
    @Override
    public void onDisconnection(Connection source, String reason) {
        marketIdle = false;
        super.onDisconnection(source, reason);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener#onConnectionStatus(it.softsolutions.bestx.model.Market.SubMarketCode, boolean, it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener.StatusField)
     */
    @Override
    public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField) {
        LOGGER.debug("MTSPrime Market connection status change: field = {}, value = {}", statusField, status);

        // gestire Market Status, MemberStatus e UserStatus
        if (statusField.compareTo(StatusField.MARKET_STATUS) == 0) {
            LOGGER.info("MTSPrime Market connection status change: market = {}", status);
            isMarketStatusOn = status;
        } else if (statusField.compareTo(StatusField.USER_STATUS) == 0) {
            LOGGER.info("MTSPrime Market connection status change: user = {}", status);
        } else {
            LOGGER.info("MTSPrime Market connection status change: member = {}", status);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#getMarketCode()
     */
    @Override
    public MarketCode getMarketCode() {
        return MarketCode.MTSPRIME;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.Connection#getConnectionName()
     */
    @Override
    public String getConnectionName() {
        return "CRD";
    }

    @Override
    protected Venue getVirtualVenue() {
        return virtualVenue;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#isInstrumentTradableWithMarketMaker(it.softsolutions.bestx.model.Instrument, it.softsolutions.bestx.model.MarketMarketMaker)
     */
    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
        return true;
    }

    @Override
    protected MarketBuySideConnection getMarket() {
        return MTSPrimeMarket.this;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onSecurityDefinition(java.lang.String, java.lang.String, java.util.Date, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal)
     */
    @Override
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier) {
        // ETLX specific behavior : add new instrument to the ISIN - Markets map used for the Automatic Curando for the regulated market
        // quoted isins.
        try {
            regulatedMktIsinsLoader.addInstrument(isin, subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        }
        // go on with the regulated markets common operations
        super.onSecurityDefinition(isin, subMarket, settlementDate);
    }

    @Override
    protected void checkPriceConnection() throws  MarketNotAvailableException {
        if (!isConnected()) {
            throw new MarketNotAvailableException("MTSPrime price connection is off");
        }
    }

    private void checkBuySideConnection() throws BestXException {
        if (!isConnected()) {
            throw new MarketNotAvailableException("MTSPrime connection is off");
        }
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("MTSPrime buy side connection is disabled");
        }
    }

    @Override
    protected String getNewPriceSessionId() throws BestXException {
        return sessionIdServer.getUniqueIdentifier("MTSPRIME_PRICE_REQUEST_ID", "P%08d");
    }

    private String getOperationOrderSessionId(Operation operation) throws BestXException {
        String sessionId = operation.getIdentifier(OperationIdType.MTSPRIME_SESSION_ID);
        if (sessionId == null) {
            sessionId = operation.getOrder().getFixOrderId();
            operationRegistry.bindOperation(operation, OperationIdType.MTSPRIME_SESSION_ID, sessionId);
        }
        return sessionId;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#isBuySideConnectionAvailable()
     */
    @Override
    public boolean isBuySideConnectionAvailable() {
        return isConnected() && (isMarketStatusOn) && (marketIdle);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#isPriceConnectionAvailable()
     */
    @Override
    public boolean isPriceConnectionAvailable() {
        return connected && priceConnectionOn;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#sendFokOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.MarketOrder)
     */
    @Override
    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();
        String sessionId = getOperationOrderSessionId(listener);

        LOGGER.debug("Sending order, registering statistics.");
        marketStatistics.orderSent(listener.getOrder().getFixOrderId());

        // 20120222 - Ruggero Multiply quantity for the multiplier received with the instrument it should be null for MOT and TLX, 1 for
        // HIMTF e 1000000 for MTSPRIME
        BigDecimal qtyMultiplier = marketSecurityStatusService.getQtyMultiplier(getMarketCode(), marketOrder.getInstrument());
        LOGGER.debug("Send FillOrKill Order to MTSPrime - " + marketOrder.toString() + " - Session ID: " + sessionId);
        mtsPrimeConnection.sendFokOrder(new MTSPrimeOrderOutputLazyBean(marketOrder, sessionId, qtyMultiplier));
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#sendFasOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.MarketOrder)
     */
    @Override
    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        // TODO : da implementare quando il magnete sara' attivo
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#revokeOrder(it.softsolutions.bestx.Operation, it.softsolutions.bestx.model.MarketOrder, java.lang.String)
     */
    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        // TODO : da implementare quando il magnete sara' attivo
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onExecutionReport(java.lang.String, it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState, it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean)
     */
    @Override
    public void onExecutionReport(String mtsPrimeSessionId, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        LOGGER.debug("Execution Report event received from MTSPrime: {}", mtsPrimeSessionId);
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_ORDER_ID, mtsPrimeSessionId);
            String orderId = operation.getOrder().getFixOrderId();
            marketStatistics.orderResponseReceived(orderId, operation.getOrder().getQty().doubleValue());
            
            
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Order order = operation.getOrder();
                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    try {
                        /*
                         * 2009-09-30 Ruggero The method called for finding the market in which the instrument is quoted doesn't care about
                         * the actual quoting status of the instrument. If we arrive here it means that we've already sent an order to the
                         * market, thus the instrument was negotiable. IT might happen that the instrument's status switch from negotiable
                         * to some other status that isn't so. If this is the case we must, anyway, fetch the market in order to manage the
                         * fill we received.
                         */
                        marketExecutionReport.setMarket(marketSecurityStatusService.getInstrumentMarket(MarketCode.MTSPRIME, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve MTSPrime market", be);
                    }
                    marketExecutionReport.setOrderQty(order.getQty());

                    // 20120222 - Ruggero Multiply quantity for the multiplier received with the instrument it should be null for MOT and
                    // TLX, 1 for HIMTF e 1000000 for MTSPRIME
                    BigDecimal qtyMultiplier = marketSecurityStatusService.getQtyMultiplier(getMarketCode(), order.getInstrument());
                    BigDecimal filledQty = regulatedFillInputBean.getQtyFilled();
                    if (qtyMultiplier == null) {
                        qtyMultiplier = BigDecimal.ONE;
                    }
                    filledQty = filledQty.multiply(qtyMultiplier);
                    filledQty = filledQty.setScale(5, RoundingMode.HALF_DOWN);

                    marketExecutionReport.setActualQty(filledQty);
                    marketExecutionReport.setLastPx(regulatedFillInputBean.getPrice());
                    marketExecutionReport.setPrice(new Money(order.getCurrency(), regulatedFillInputBean.getPrice()));
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setState(executionReportState);
                    marketExecutionReport.setTransactTime(regulatedFillInputBean.getTimeStamp());
                    marketExecutionReport.setSequenceId(regulatedFillInputBean.getFillId()); // CD
                    marketExecutionReport.setTicket(regulatedFillInputBean.getContractNumber());
                    marketExecutionReport.setOrderTrader(regulatedFillInputBean.getOrderTrader());
                    marketExecutionReport.setSecurityIdSource(regulatedFillInputBean.getSecurityIdSource());
                    marketExecutionReport.setMarketOrderID(regulatedFillInputBean.getOrderId());
                    marketExecutionReport.setCounterPart(regulatedFillInputBean.getCounterpart());
                    operation.onMarketExecutionReport(getMarket(), order, marketExecutionReport);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketPriceConnection#isInstrumentQuotedOnMarket(it.softsolutions.bestx.model.Instrument)
     */
    @Override
    public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
        try {
            return getQuotingMarket(instrument) != null;
        } catch (BestXException e) {
            LOGGER.error("An error occurred while finding quoting market" + " : " + e.toString(), e);
            return false;
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener#onOrderReject(java.lang.String, java.lang.String)
     */
    @Override
    public void onOrderReject(final String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            // statistics : response received, registering it
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderReject(getMarket(), operation.getOrder(), reason, mtsPrimeSessionId);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener#onOrderTechnicalReject(java.lang.String, java.lang.String)
     */
    @Override
    public void onOrderTechnicalReject(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            // statistics : response received, registering it
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderTechnicalReject(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener#onOrderCancelled(java.lang.String, java.lang.String)
     */
    @Override
    public void onOrderCancelled(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Order {} cancelled, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelled(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onCancelRequestReject(java.lang.String, java.lang.String)
     */
    @Override
    public void onCancelRequestReject(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderCancelRequestReject(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onFasCancelFillAndBook(java.lang.String, java.lang.String)
     */
    @Override
    public void onFasCancelFillAndBook(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelFillAndBook(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onFasCancelFillNoBook(java.lang.String, java.lang.String)
     */
    @Override
    public void onFasCancelFillNoBook(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelFillNoBook(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onFasCancelNoFill(java.lang.String, java.lang.String)
     */
    @Override
    public void onFasCancelNoFill(String mtsPrimeSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    // operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelNoFill(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onFasCancelNoFill(java.lang.String, java.lang.String, it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState, it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean)
     */
    @Override
    public void onFasCancelNoFill(String regSessionId, final String reason, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        // TODO : da implementare quando il magnete sara' attivo
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onOrdeReceived(java.lang.String, java.lang.String)
     */
    @Override
    public void onOrdeReceived(final String mtsPrimeSessionId, final String orderId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
                    marketStatistics.orderResponseReceived(operation.getOrder().getFixOrderId());
                    // operation.onMarketResponseReceived();
                    // statistics : response received, registering it
                    LOGGER.debug("Response received for the order {}, registering statistics.", operation.getOrder().getFixOrderId());
                    LOGGER.debug("MTSPrime, onOrderReceived, session {}, registering order {}", mtsPrimeSessionId, orderId);
                    operationRegistry.bindOperation(operation, OperationIdType.MTSPRIME_ORDER_ID, orderId);
                } catch (BestXException e) {
                    LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketBuySideConnection#getMatchingTrade(it.softsolutions.bestx.model.Order, it.softsolutions.jsscommon.Money, it.softsolutions.bestx.model.MarketMaker, java.util.Date)
     */
    @Override
    public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onMarketPriceReceived(java.lang.String, java.math.BigDecimal, it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean)
     */
    @Override
    public void onMarketPriceReceived(final String mtsPrimeSessionId, final BigDecimal marketPrice, final RegulatedFillInputBean regulatedFillInputBean) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MTSPRIME_SESSION_ID, mtsPrimeSessionId);
            final String fixOrderId = operation.getOrder().getFixOrderId();

            // statistics : response received, registering it
            LOGGER.debug("Price received for a magnet market order ({}), is a NEW fill and we have to register the answer for statistic purposes.", fixOrderId);
            marketStatistics.pricesResponseReceived(fixOrderId, 1);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Order order = operation.getOrder();
                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    try {
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.MTSPRIME, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve MTSPrime market", be);
                    }
                    marketExecutionReport.setOrderQty(order.getQty());
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setTransactTime(regulatedFillInputBean.getTimeStamp());
                    marketExecutionReport.setSequenceId(regulatedFillInputBean.getFillId()); // CD
                    marketExecutionReport.setTicket(regulatedFillInputBean.getContractNumber());
                    marketExecutionReport.setOrderTrader(regulatedFillInputBean.getOrderTrader());
                    marketExecutionReport.setSecurityIdSource(regulatedFillInputBean.getSecurityIdSource());
                    marketExecutionReport.setMarketOrderID(regulatedFillInputBean.getOrderId());
                    marketExecutionReport.setCounterPart(regulatedFillInputBean.getCounterpart());
                    Money price = new Money(order.getCurrency(), regulatedFillInputBean.getPrice());
                    marketExecutionReport.setPrice(price);

                    operation.onMarketPriceReceived(fixOrderId, marketPrice, order, marketExecutionReport);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MTSPrime Session ID: " + mtsPrimeSessionId + " : " + e.toString(), e);
        }

    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#isAMagnetMarket()
     */
    @Override
    public boolean isAMagnetMarket() {
        return false;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.management.MarketMBean#cleanBook()
     */
    @Override
    public void cleanBook() {
        SimpleMarketProposalAggregator.getInstance().clearBooks();
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onConnectionStatusChange(boolean, boolean)
     */
    @Override
    public void onConnectionStatusChange(boolean marketStatus, boolean userStatus) {
        isMarketStatusOn = marketStatus;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onExecutionReport(java.lang.String, it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState, java.lang.String, java.lang.String, java.math.BigDecimal, java.lang.String, java.math.BigDecimal, java.util.Date)
     */
    @Override
    public void onExecutionReport(String sessionId, ExecutionReportState executionReportState, String marketOrderId, String counterpart, BigDecimal lastPrice, String contractNo,
                    BigDecimal accruedValue, Date marketTime) {
        // TODO : da implementare con l'execution report corretto

    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onQuote(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, it.softsolutions.bestx.model.Proposal.ProposalSide, java.util.Date, java.util.Date, java.lang.String)
     */
    @Override
    public void onQuote(String sessionId, String rfqSeqNo, String quoteId, String marketMaker, BigDecimal price, BigDecimal yield, BigDecimal qty, ProposalSide side, Date futSettDate, Date updateTime, String updateTimeStr) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onMemberEnabled(java.lang.String, java.lang.String, boolean)
     */
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
                LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS] Added couple <" + member + ", " + bondType + "> to enabled member-bondType map");
            }
        } else {
            if (marketMakers.remove(member)) {
                LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS] Removed couple <" + member + ", " + bondType + "> to enabled member-bondType map");
            }
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onTradingRelation(java.lang.String, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationEvent)
     */
    @Override
    public void onTradingRelation(String member, TradingRelationStatus status, TradingRelationStatus sellSideSubStatus, TradingRelationStatus buySideSubStatus, TradingRelationEvent event) {
        TradingRelation tradingRelation = marketMarketMakerTradingRelationMap.remove(member);
        if (tradingRelation != null) {
            LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS]  removed tradingRelation: " + tradingRelation.toString() + " from member: " + member);
        }
        tradingRelation = new TradingRelation(status, sellSideSubStatus, buySideSubStatus, event);
        if (tradingRelation.getStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0 && tradingRelation.getSellSideSubStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0
                        && tradingRelation.getBuySideSubStatus().compareTo(TradingRelationStatus.ACCEPTED) == 0 && tradingRelation.getEvent().compareTo(TradingRelationEvent.ACCEPT) == 0) {
            marketMarketMakerTradingRelationMap.put(member, tradingRelation);
            LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS]  added tradingRelation: " + tradingRelation.toString() + " to member: " + member);
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onTradingRelationException(java.lang.String, java.lang.String, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionStatus, it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionEvent)
     */
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
                LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS]  added tradingRelationException: " + bondType + " to member: " + member);
            }
        } else {
            if (bondTypeList.contains(bondType)) {
                bondTypeList.remove(bondType);
                LOGGER.info("[MTSPRIME_TRADING_PERMISSIONS]  removed tradingRelationException: " + bondType + " to member: " + member);
            }
        }
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.markets.regulated.RegulatedMarket#onSecurityDefinition(java.lang.String, java.lang.String, java.util.Date, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.lang.String)
     */
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal qtyTick, BigDecimal multiplier, String bondType) {
        onSecurityDefinition(isin, subMarket, settlementDate, minQty, qtyTick, multiplier);
        onSecurityStatus(isin, subMarket, "NEG");
        instrumentBondTypeMap.put(isin, bondType);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onSecurityDefinition(java.lang.String, java.lang.String, java.util.Date, java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal, java.lang.String, int, java.lang.String, int, java.lang.String)
     */
    @Override
    public void onSecurityDefinition(String isin, String string, Date settlDate, BigDecimal minSize, BigDecimal qtyTick, BigDecimal lotSize, String bondType, int marketAffiliation,
                    String marketAffiliationStr, int quoteIndicator, String quoteIndicatorStr) {
        SubMarketCode subMarketCode = SubMarketCode.CRD;
        try {
            marketSecurityStatusService.setMarketSecuritySettlementDateAndBondType(getMarketCode(), subMarketCode, isin, settlDate, bondType, marketAffiliation, marketAffiliationStr, quoteIndicator,
                            quoteIndicatorStr);
            marketSecurityStatusService.setMarketSecurityQuantity(getMarketCode(), subMarketCode, isin, minSize, qtyTick, lotSize);
            marketSecurityStatusService.setMarketSecurityQuotingStatus(getMarketCode(), subMarketCode, isin, Instrument.QuotingStatus.NEG);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        }
        instrumentBondTypeMap.put(isin, bondType);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onDownloadEnd(it.softsolutions.bestx.connections.Connection)
     */
    @Override
    public void onDownloadEnd(Connection source) {
        marketIdle = true;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.mts.MTSConnectionListener#onNullPrices(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void onNullPrices(String regulatedSessionId, String isin, String reason) {
        LOGGER.warn("Error reply for a price request, session id " + regulatedSessionId + ", instrument " + isin + ", nothing to do in " + getMarketCode() + " market.");
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.connections.MarketConnection#onNullPrices(java.lang.String, java.lang.String, it.softsolutions.bestx.model.Proposal.ProposalSide)
     */
    @Override
    public void onNullPrices(String regulatedSessionId, String isin, ProposalSide side) {
        LOGGER.debug("Price notification received for {}", regulatedSessionId);
        VenuePriceListenerInfo priceListenerInfo = priceListeners.get(regulatedSessionId);
        if (priceListenerInfo == null) {
            LOGGER.debug("Stale null price received from " + getConnectionName() + ". Probably request timed out.");
            return;
        }
        LOGGER.info("[PRICESRV],Market={},PricesReceivedIn={} millisecs", MarketCode.MTSPRIME, ((DateService.newLocalDate()).getTime() - priceListenerInfo.getCreationTime().getTime()));

        if (!priceListenerInfo.missingProposalStates.get(getVirtualVenue()).isSideArrived(side)) {
            priceListenerInfo.missingProposalStates.get(getVirtualVenue()).addProposalSide(side);
        }

        if (priceListenerInfo.missingProposalStates.get(getVirtualVenue()).bothSidesArrived()) {
            priceListeners.remove(regulatedSessionId);
            
            LOGGER.debug("Cancel timer for session ID: " + regulatedSessionId);
            try {
                SimpleTimerManager.getInstance().stopJob(regulatedSessionId, this.getClass().getSimpleName());
            } catch (SchedulerException e) {
                LOGGER.error("Cannot remove timer job {} of group {}", regulatedSessionId, this.getClass().getSimpleName(), e);
            }

            if (!priceListenerInfo.resultSent) {
                priceListenerInfo.resultSent = true;

                if (priceListenerInfo.sortedBook == null) {
                    priceListenerInfo.sortedBook = new SortedBook();
                }
                priceListenerInfo.listeners.get(0).getOrder().addMarketNotQuotingInstr(getMarketCode(), Messages.getString("MTSPRIME_PRICE_TIMEOUT.0"));
                priceListenerInfo.listeners.get(0).onMarketBookComplete(this, priceListenerInfo.sortedBook);
            }
        }
        return;
    }

    /**
     * Gets the mts prime connection.
     *
     * @return the mts prime connection
     */
    public MTSConnector getMtsPrimeConnection() {
        return mtsPrimeConnection;
    }

    /**
     * Sets the mts prime connection.
     *
     * @param mtsPrimeConnection the new mts prime connection
     */
    public void setMtsPrimeConnection(MTSConnector mtsPrimeConnection) {
        this.mtsPrimeConnection = mtsPrimeConnection;
        this.mtsPrimeConnection.setMTSConnectionListener(this);
        setRegulatedConnection(mtsPrimeConnection);
    }

    protected ClassifiedProposal getProposal(BondVisionProposalInputLazyBean bvProposal, Instrument instrument) throws BestXException {
        // TODO : da implementare a seconda dei dati che arrivano da MTSPrime
        ClassifiedProposal proposal = null;
        //        BigDecimal[] instrQuantities = marketSecurityStatusService.getQuantityValues(MarketCode.MTSPRIME, instrument);
        //        BigDecimal qtyMultiplier = BigDecimal.ONE; 
        //        if (instrQuantities != null && instrQuantities.length > 0) {
        //            qtyMultiplier = instrQuantities[QuantityValues.QTY_MULTIPLIER.getPosition()];
        //        } else {
        //            LOGGER.warn("Cannot find conversion quantities for the proposal, as default the conversion multiplier will be 1.");
        //        }

        return proposal;
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
            LOGGER.info("MTSPRIME proposal : " + proposal.toString());

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

    @Override
    public boolean isMarketIdle() {
        return marketIdle;
    }

    @Override
    public void ackProposal(Operation listener, Proposal proposal) throws BestXException {
        // TODO Auto-generated method stub
    }

	@Override
    public int getActiveTimersNum() {
	    // TODO Auto-generated method stub
	    return 0;
    }
}
