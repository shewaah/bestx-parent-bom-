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
package it.softsolutions.bestx.markets.mot;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.regulated.CancelRequestOutputBean;
import it.softsolutions.bestx.connections.regulated.FasOrderOutputBean;
import it.softsolutions.bestx.connections.regulated.FokOrderOutputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.Customer;
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
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class manages the trading interaction with the MOT market
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 09/ago/2012
 * 
 **/
public class MOTMarket extends RegulatedMarket {

    private static final long serialVersionUID = -367170243183539504L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MOTMarket.class);

    private RegulatedMktIsinsLoader regulatedMktIsinsLoader;

    // variable to store the send cancel request timeout, other timers inside this market will be set based on this value
    private Long sendOrderCancelTimeout = null;
    private final Venue virtualVenue = new Venue("MOTFIX");
    private volatile boolean motMarketStatus;
    private volatile boolean motUserStatus;
    private volatile boolean memMarketStatus;
    private volatile boolean memUserStatus;

    @Override
    public void onDisconnection(Connection source, String reason) {
        motMarketStatus = false;
        motUserStatus = false;
        memMarketStatus = false;
        memUserStatus = false;
        super.onDisconnection(source, reason);
    }

    @Override
    public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField) {
        LOGGER.debug("MOT Market connection status change: field = " + statusField.name() + ", subMarket: " + subMarketCode + " , status = " + status);
        if (subMarketCode != null) {
            switch (subMarketCode) {
            case MOT:
                motMarketStatus = status;
                motUserStatus = status;
                break;
            case MEM:
                memMarketStatus = status;
                memUserStatus = status;
                break;
            case ETX:
            case TLX:
            case HIMTF:
                break;
            default:
                break;
            }
            
            
        } else {
            switch (statusField) {
            case MARKET_STATUS:
                motMarketStatus = memMarketStatus = status;
                break;
            case USER_STATUS:
                motUserStatus = memUserStatus = status;
                break;
            default:
                break;
            }
        }
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.MOT;
    }

    @Override
    public String getConnectionName() {
        return "MOTFIX";
    }

    @Override
    protected Venue getVirtualVenue() {
        return virtualVenue;
    }

    @Override
    protected MarketBuySideConnection getMarket() {
        return MOTMarket.this;
    }

    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
        return true;
    }

    @Override
    protected void checkPriceConnection() throws  MarketNotAvailableException {
        if (!(motMarketStatus && motUserStatus || memMarketStatus && memUserStatus)) {
            throw new MarketNotAvailableException("MOT status: market = " + motMarketStatus + ", user = " + motUserStatus + " - MEM status: market = " + memMarketStatus + ", user = " + memUserStatus);
        }
        if (!isConnected()) {
            throw new MarketNotAvailableException("MOT price connection is off");
        }
    }

    private void checkBuySideConnection() throws BestXException {
        if (!(motMarketStatus & motUserStatus)) {
            throw new MarketNotAvailableException("MOT status: market = " + motMarketStatus + ", user = " + motUserStatus);
        }
        if (!isConnected()) {
            throw new MarketNotAvailableException("MOT connection is off");
        }
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("MOT buy side connection is disabled");
        }
    }

    @Override
    protected String getNewPriceSessionId() throws BestXException {
        return sessionIdServer.getUniqueIdentifier("MOT_PRICE_REQUEST_ID", "P%013d");
    }

    private String retrieveOperationOrderSessionId(Operation operation) throws BestXException {
        String sessionId = operation.getIdentifier(OperationIdType.MOT_SESSION_ID);

        if (sessionId == null) {
            throw new BestXException("Unable to retrieve a valid identifier for key " + OperationIdType.MOT_SESSION_ID + " on operation " + operation.getOrder().getFixOrderId());
        }

        return sessionId;
    }

    private String createOperationOrderSessionId(Operation operation) throws BestXException {
        String sessionId = getNewOrderSessionId(operation.getOrder().getFixOrderId());
        // String sessionId = operation.getOrder().getFixOrderId();
        operationRegistry.bindOperation(operation, OperationIdType.MOT_SESSION_ID, sessionId);
        return sessionId;
    }

    /**
     * [DR20120628] ConfCall con ATS (Pastori e Colombo). Il clOrdID da inviare a ATS: - deve essere di almeno 10 char - deve includere,
     * dalla 4a alla 14a cifra, le 10 cifre (dalla 4a alla 14a cifra) del numero d'ordine che noi riceviamo da AMOS poichè AMOS, durante la
     * riconciliazione, considera queste 10 cifre
     * 
     * @param clOrdID
     *            clOrdID coming from the OMS (AMOS, TAS) via FIX Gateway
     * @return a unique orderSessionID to be sent to ATS
     * @throws BestXException
     *             if an error occurs
     */
    private String getNewOrderSessionId(String clOrdID) throws BestXException {
        return sessionIdServer.getUniqueIdentifier("MOT_SESSION_ID", clOrdID + "_%07d");
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return (motMarketStatus && motUserStatus) || (memMarketStatus && memUserStatus);
    }

    @Override
    public boolean isPriceConnectionAvailable() {
        return isConnected() && isBuySideConnectionAvailable();
    }

    @Override
    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();
        String sessionId = createOperationOrderSessionId(listener);

        LOGGER.debug("Sending order, registering statistics.");
        marketStatistics.orderSent(listener.getOrder().getFixOrderId());
        Customer customer = listener.getOrder().getCustomer();

        // [RR20120703] BXM-95 : Cristina Fusetti requested that, as the account code, we always send the customer sinfo code if available,
        // otherwise use the account code configured
        // in the BESTX.properties file
        regulatedConnection.sendFokOrder(new FokOrderOutputBean(marketOrder, getConnectionName(), marketOrder.getMarket().getSubMarketCode().name(), getFOKOrderAccount(customer), sessionId));
    }

    @Override
    /*
     * 11-08-2009 Ruggero Adding FAS order capability to the MOT Market as per Magnet feature introduction
     */
    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();
        String sessionId = createOperationOrderSessionId(listener);
        LOGGER.debug("Send FillAndStore Order to MOT " + marketOrder.toString() + " - Session ID: " + sessionId);
        /*
         * 25-03-2009 Ruggero As requested by Akros we've to send, as the account, the customer's sinfocode We obtain it from the customer
         * finder, it's an information taken from the CustomerTable, therefore it's been loaded in the Customer object
         */
        Customer customer = listener.getOrder().getCustomer();

        LOGGER.debug("Sending order, registering statistics.");
        marketStatistics.orderSent(listener.getOrder().getFixOrderId());
        regulatedConnection.sendFasOrder(new FasOrderOutputBean(marketOrder, getConnectionName(), marketOrder.getMarket().getSubMarketCode().name(), customer.getSinfoCode(), sessionId), this);
    }

    @Override
    public void onExecutionReport(String motSessionId, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        LOGGER.debug("Execution Report event received from MOT: " + motSessionId);
        LOGGER.debug("Fill data : " + regulatedFillInputBean.toString());
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, motSessionId);
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, regulatedFillInputBean.getQtyFilled().doubleValue());
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
                         * 2009-09-30 Ruggero The method called to find the market in which the instrument is quoted doesn't care about the
                         * actual quoting status of the instrument. If we arrive here it means that we've already sent an order to the
                         * market, thus the instrument was negotiable. IT might happen that the instrument's status switch from negotiable
                         * to some other status that isn't so. If this is the case we must, anyway, fetch the market in order to manage the
                         * fill we received.
                         */
                        marketExecutionReport.setMarket(marketSecurityStatusService.getInstrumentMarket(MarketCode.MOT, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve MOT market" + " : " + be.toString(), be);
                    }
                    marketExecutionReport.setOrderQty(order.getQty());
                    marketExecutionReport.setActualQty(regulatedFillInputBean.getQtyFilled());
                    marketExecutionReport.setLastPx(regulatedFillInputBean.getFillPrice());
                    marketExecutionReport.setPrice(new Money(order.getCurrency(), regulatedFillInputBean.getFillPrice()));
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setState(executionReportState);
                    marketExecutionReport.setTransactTime(regulatedFillInputBean.getTimeStamp());
                    marketExecutionReport.setSequenceId(regulatedFillInputBean.getFillId()); // CD
                    marketExecutionReport.setTicket(regulatedFillInputBean.getContractNumber());
                    marketExecutionReport.setOrderTrader(regulatedFillInputBean.getOrderTrader());
                    marketExecutionReport.setSecurityIdSource(regulatedFillInputBean.getSecurityIdSource());
                    marketExecutionReport.setMarketOrderID(regulatedFillInputBean.getOrderId());
                    marketExecutionReport.setCounterPart(regulatedFillInputBean.getCounterpart());
                    operation.onMarketResponseReceived();
                    operation.onMarketExecutionReport(getMarket(), order, marketExecutionReport);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MOT Session ID: " + motSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
        try {
            return getQuotingMarket(instrument) != null;
        } catch (BestXException e) {
            LOGGER.error("An error occurred while finding quoting market" + " : " + e.toString(), e);
            return false;
        }
    }

    @Override
    public void onOrderReject(final String motSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, motSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderReject(getMarket(), operation.getOrder(), reason, motSessionId);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MOT Session ID: " + motSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onOrderTechnicalReject(String motSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, motSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Technical reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderTechnicalReject(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MOT Session ID: " + motSessionId);
        }
    }

    @Override
    public void onOrderCancelled(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);

            final String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Order {} cancelled, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            // this Runnable is a Thread because we need to make it sleeps for 15 seconds
            // if it is a magnet order
            executor.execute(new Thread() {
                @Override
                public void run() {
                    /*
                     * 2009-09-07 Ruggero When we send an order without a limit price and it is partially filled, the market send us the
                     * fill and then it cancels the order. So we receive a new, a fill and a cancel execution report. The cancel seems to
                     * take precedence over the others thus not notifying to the operator the partial execution. Here, if the order is a
                     * magnet order, we will wait 15 seconds in order to give BestX! the time he needs to manage the partial fill/s
                     */
                    operation.onMarketResponseReceived();
                    if (operation.isNoProposalsOrderOnBook() && !operation.isMagnetCustomerRevoke()) {
                        operation.setCancelOrderWaiting(true);
                        try {
                            // 20111213 - Ruggero
                            // sleep a numer of ms lesser than that of the send order cancel timeout
                            // in order to not make it expire
                            // Today the reference timer is 25000 ms.
                            if (sendOrderCancelTimeout != null) {
                                long delay = sendOrderCancelTimeout - 10000;
                                if (delay > 0) {
                                    LOGGER.debug("Order {}, cancel order exec report received, wait for possible partial executions, delay : {}", orderId, delay);
                                    sleep(delay);
                                }
                            }
                        } catch (InterruptedException e) {
                            LOGGER.error("Unexpected interruption of the 15 seconds wait in the onOrderCancelled method of the MOTMarket class.", e);
                        }
                        LOGGER.debug("Order {}, cancel order exec report, restart management.", orderId);
                        operation.setCancelOrderWaiting(false);
                    }
                    operation.onMarketOrderCancelled(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onOrdeReceived(final String regSessionId, final String orderId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
                    operation.onMarketResponseReceived();
                    // statistics : response received, registering it
                    LOGGER.debug("Response received for the order {}, registering statistics.", operation.getOrder().getFixOrderId());
                    marketStatistics.orderResponseReceived(operation.getOrder().getFixOrderId());
                    LOGGER.debug("MOT, onOrderReceived, session {}, registering order {}", regSessionId, orderId);
                    operationRegistry.bindOperation(operation, OperationIdType.MOT_ORDER_ID, orderId);
                } catch (BestXException e) {
                    LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
                }
            }
        });
    }

    @Override
    public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier) {
        /*
         * MOT specific behavior : add new instrument to the ISIN - Markets map used for the Automatic Curando for the regulated market
         * quoted isins.
         */
        try {
            regulatedMktIsinsLoader.addInstrument(isin, subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        }
        // go on with the regulated markets common operations
        super.onSecurityDefinition(isin, subMarket, settlementDate);
    }

    public RegulatedMktIsinsLoader getRegulatedMktIsinsLoader() {
        return regulatedMktIsinsLoader;
    }

    public void setRegulatedMktIsinsLoader(RegulatedMktIsinsLoader regulatedMktIsinsLoader) {
        this.regulatedMktIsinsLoader = regulatedMktIsinsLoader;
    }

    @Override
    public boolean isAMagnetMarket() {
        return true;
    }

    @Override
    public void onFasCancelFillAndBook(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelFillAndBook(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onFasCancelFillNoBook(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelFillNoBook(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onFasCancelNoFill(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelNoFill(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onFasCancelNoFill(String regSessionId, final String reason, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        LOGGER.debug("Execution Report for order cancel event received from MOT: " + regSessionId);
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Order order = operation.getOrder();
                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    try {
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.MOT, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve MOT market", be);
                    }
                    marketExecutionReport.setOrderQty(order.getQty());
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setState(executionReportState);
                    marketExecutionReport.setTransactTime(regulatedFillInputBean.getTimeStamp());
                    marketExecutionReport.setSequenceId(regulatedFillInputBean.getFillId()); // CD
                    marketExecutionReport.setTicket(regulatedFillInputBean.getContractNumber());
                    marketExecutionReport.setOrderTrader(regulatedFillInputBean.getOrderTrader());
                    marketExecutionReport.setSecurityIdSource(regulatedFillInputBean.getSecurityIdSource());
                    marketExecutionReport.setMarketOrderID(regulatedFillInputBean.getOrderId());
                    marketExecutionReport.setCounterPart(regulatedFillInputBean.getCounterpart());
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelNoFill(getMarket(), order, reason, marketExecutionReport);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with ETLX Session ID: " + regSessionId + " : " + e.toString(), e);
        }

    }

    @Override
    public void onMarketPriceReceived(final String regSessionId, final BigDecimal marketPrice, final RegulatedFillInputBean regulatedFillInputBean) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            final String fixOrderId = operation.getOrder().getFixOrderId();

            // statistics : response received, registering it
            LOGGER.debug("Price received for a magnet market order ({}), is a NEW fill and we've to register the answer for statics purposes.", fixOrderId);
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
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.MOT, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve MOT market", be);
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
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }

    }

    /**
     * 20120612 - Ruggero BXM-76 This method is used when we need to send a revoke while passing into manual manage from the magnet. Here we
     * do not need to wait for partial fills, so we do not need a check on the timer as in the other revokeOrder method.
     */
    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        checkBuySideConnection();
        String origClOrdId = retrieveOperationOrderSessionId(listener);
        String clOrdId = createOperationOrderSessionId(listener);
        LOGGER.debug("MOT, revoking order {}", listener.getOrder().getFixOrderId());
        String motOrderId = listener.getIdentifier(OperationIdType.MOT_ORDER_ID);
        LOGGER.debug("Send revoke order to MOT " + marketOrder.toString() + " Reason : " + reason + " - Session ID: " + origClOrdId + " - MOT_ORDER_ID :" + motOrderId);
        regulatedConnection.revokeOrder(new CancelRequestOutputBean(marketOrder, getConnectionName(), getOrderAccount(), origClOrdId, clOrdId, motOrderId));
    }

    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
        checkBuySideConnection();

        // 20111213 - Ruggero
        // On the first revoke, store the timeout allowed for its management. Based on this, upon reception of the order
        // cancel execution report, we will wait a number of ms lesser than this timer to let possible partial execution
        // reports to arrive. Check the onOrderCancelled method.
        if (this.sendOrderCancelTimeout == null) {
            this.sendOrderCancelTimeout = sendOrderCancelTimeout;
        }

        String origClOrdId = retrieveOperationOrderSessionId(listener);
        String clOrdId = createOperationOrderSessionId(listener);
        LOGGER.debug("MOT revoking order " + listener.getOrder().getFixOrderId());
        String motOrderId = listener.getIdentifier(OperationIdType.MOT_ORDER_ID);
        LOGGER.debug("Send revoke order to MOT " + marketOrder.toString() + " Reason : " + reason + " - Session ID: " + origClOrdId);
        regulatedConnection.revokeOrder(new CancelRequestOutputBean(marketOrder, getConnectionName(), getOrderAccount(), origClOrdId, clOrdId, motOrderId));
    }

    @Override
    public void onCancelRequestReject(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.MOT_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketOrderCancelRequestReject(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void cleanBook() {
        SimpleMarketProposalAggregator.getInstance().clearBooks();
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