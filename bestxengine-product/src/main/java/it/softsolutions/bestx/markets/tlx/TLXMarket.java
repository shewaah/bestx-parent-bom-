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
package it.softsolutions.bestx.markets.tlx;

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
 * Purpose: this class manages the trading interaction with the TLX market
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 09/ago/2012
 * 
 **/
public class TLXMarket extends RegulatedMarket {

    private static final Logger LOGGER = LoggerFactory.getLogger(TLXMarket.class);

    private final Venue virtualVenue = new Venue("TLXFIX");

    private volatile boolean tlxMarketStatus;
    private volatile boolean tlxUserStatus;
    private volatile boolean etxMarketStatus;
    private volatile boolean etxUserStatus;
    private RegulatedMktIsinsLoader regulatedMktIsinsLoader;

    @Override
    public void onDisconnection(Connection source, String reason) {
        tlxMarketStatus = false;
        tlxUserStatus = false;
        etxMarketStatus = false;
        etxUserStatus = false;
        super.onDisconnection(source, reason);
    }

    @Override
    public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField) {
        LOGGER.debug("ETLX Market connection status change: field = " + statusField + ", value = " + status);
        
        switch (statusField) {
        case MARKET_STATUS:
            etxMarketStatus = tlxMarketStatus = status;
            break;
        case USER_STATUS:
            etxUserStatus = tlxUserStatus = status;
            break;
        default:
            break;
        }
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.TLX;
    }

    @Override
    public String getConnectionName() {
        return "TLXFIX";
    }

    @Override
    protected Venue getVirtualVenue() {
        return virtualVenue;
    }

    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
        return true;
    }

    @Override
    protected MarketBuySideConnection getMarket() {
        return TLXMarket.this;
    }

    @Override
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier) {
        /*
         * ETLX specific behavior : add new instrument to the ISIN - Markets map used for the Automatic Curando for the regulated market
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

    @Override
    protected void checkPriceConnection() throws  MarketNotAvailableException {
        // if (!((tlxMarketStatus && tlxUserStatus)|| (etxMarketStatus && etxUserStatus)))
        // throw new MarketNotAvailableException("ETLX status: market = " + tlxMarketStatus + ", user = " + tlxUserStatus +
        // " - ETX status: market = " + etxMarketStatus + ", user = " + etxUserStatus);
        if (!isConnected()) {
            throw new MarketNotAvailableException("ETLX price connection is off");
        }
    }

    private void checkBuySideConnection() throws BestXException {
        if (!isConnected()) {
            throw new MarketNotAvailableException("ETLX connection is off");
        }
        
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("ETLX buy side connection is disabled");
        }
    }

    @Override
    protected String getNewPriceSessionId() throws BestXException {
        return sessionIdServer.getUniqueIdentifier("TLX_PRICE_REQUEST_ID", "P%013d");
    }

    private String retrieveOperationOrderSessionId(Operation operation) throws BestXException {
        String sessionId = operation.getIdentifier(OperationIdType.TLX_SESSION_ID);

        if (sessionId == null) {
            throw new BestXException("Unable to retrieve a valid identifier for key " + OperationIdType.TLX_SESSION_ID + " on operation " + operation.getOrder().getFixOrderId());
        }

        return sessionId;
    }

    private String createOperationOrderSessionId(Operation operation) throws BestXException {
        // [RR20120703] ATS told us to revert to the old behaviour, i.e. send always the fix order id as the
        // ClOrdId. See jira BXM-96.
        // [RR20120810] BXM-96 : time to extend the new behaviour also to TLX
        String sessionId = getNewOrderSessionId(operation.getOrder().getFixOrderId());
        // String sessionId = operation.getOrder().getFixOrderId();
        operationRegistry.bindOperation(operation, OperationIdType.TLX_SESSION_ID, sessionId);
        return sessionId;
    }

    /**
     * [DR20120628] ConfCall con ATS (Pastori e Colombo). Il clOrdID da inviare a ATS: - deve essere di almeno 10 char - deve includere,
     * dalla 4a alla 14a cifra, le 10 cifre del numero d'ordine che noi riceviamo da AMOS poichè AMOS, durante la riconciliazione, considera
     * queste 10 cifre
     * 
     * @param clOrdID
     *            clOrdID coming from the OMS (AMOS, TAS) via FIX Gateway
     * @return a unique orderSessionID to be sent to ATS
     * @throws BestXException
     *             if an error occurs
     */
    private String getNewOrderSessionId(String clOrdID) throws BestXException {
        // [RR20120703] for the TLX market, ATS requires a unique value of maximum 20 chars
        // clOrdId 14 chars + '_' + 5 chars = 20 chars
        return sessionIdServer.getUniqueIdentifier("TLX_SESSION_ID", clOrdID + "_%05d");
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return tlxMarketStatus && tlxUserStatus || etxMarketStatus && etxUserStatus;
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

        LOGGER.debug("Send FillOrKill Order to ETLX - " + marketOrder + " - Session ID: " + sessionId);
        Customer customer = listener.getOrder().getCustomer();
        regulatedConnection.sendFokOrder(new FokOrderOutputBean(marketOrder, getConnectionName(), marketOrder.getMarket().getSubMarketCode().name(), getFOKOrderAccount(customer), sessionId));
    }

    @Override
    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();
        String sessionId = createOperationOrderSessionId(listener);
        LOGGER.debug("Send FillAndStore Order to ETLX " + marketOrder + " - Session ID: " + sessionId);
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
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        checkBuySideConnection();
        String origClOrdId = retrieveOperationOrderSessionId(listener);
        String clOrdId = createOperationOrderSessionId(listener);
        LOGGER.debug("ETLX revoking order {}", listener.getOrder().getFixOrderId());
        String tlxOrderId = listener.getIdentifier(OperationIdType.TLX_ORDER_ID);
        LOGGER.debug("Send revoke order to ETLX " + marketOrder + " Reason : " + reason + " - Session ID: " + origClOrdId + " - TLX_ORDER_ID :" + tlxOrderId);
        regulatedConnection.revokeOrder(new CancelRequestOutputBean(marketOrder, getConnectionName(), getOrderAccount(), origClOrdId, clOrdId, tlxOrderId));
    }

    @Override
    public void onExecutionReport(String tlxSessionId, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        LOGGER.debug("Execution Report event received from ETLX: " + tlxSessionId);
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, tlxSessionId);
            String orderId = operation.getOrder().getFixOrderId();
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
                         * 2009-09-30 Ruggero The method called for finding the market in which the instrument is quoted doesn't care about
                         * the actual quoting status of the instrument. If we arrive here it means that we've already sent an order to the
                         * market, thus the instrument was negotiable. IT might happen that the instrument's status switch from negotiable
                         * to some other status that isn't so. If this is the case we must, anyway, fetch the market in order to manage the
                         * fill we received.
                         */
                        marketExecutionReport.setMarket(marketSecurityStatusService.getInstrumentMarket(MarketCode.TLX, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve ETLX market", be);
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
            LOGGER.error("Error while finding Operation with ETLX Session ID: " + tlxSessionId + " : " + e.toString(), e);
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
    public void onOrderReject(final String tlxSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, tlxSessionId);
            // statistics : response received, registering it
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderReject(getMarket(), operation.getOrder(), reason, tlxSessionId);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with MOT Session ID: " + tlxSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onOrderTechnicalReject(String motSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, motSessionId);
            // statistics : response received, registering it
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
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
            LOGGER.error("Error while finding Operation with MOT Session ID: " + motSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onOrderCancelled(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Order {} cancelled, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    operation.onMarketResponseReceived();
                    operation.onMarketOrderCancelled(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onCancelRequestReject(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
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
    public void onFasCancelFillAndBook(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
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
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
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
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
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
        LOGGER.debug("Execution Report for order cancel event received from ETLX: " + regSessionId);
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Order order = operation.getOrder();
                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    try {
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.TLX, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve ETLX market", be);
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
    public void onOrdeReceived(final String regSessionId, final String orderId) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
                    marketStatistics.orderResponseReceived(operation.getOrder().getFixOrderId());
                    operation.onMarketResponseReceived();
                    // statistics : response received, registering it
                    LOGGER.debug("Response received for the order {}, registering statistics.", operation.getOrder().getFixOrderId());
                    LOGGER.debug("ETLX, onOrderReceived, session {}, registering order {}", regSessionId, orderId);
                    operationRegistry.bindOperation(operation, OperationIdType.TLX_ORDER_ID, orderId);
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
    public void onMarketPriceReceived(final String regSessionId, final BigDecimal marketPrice, final RegulatedFillInputBean regulatedFillInputBean) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.TLX_SESSION_ID, regSessionId);
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
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.TLX, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve ETLX market", be);
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
            LOGGER.error("Error finding Operation for sessionID {}: {}", regSessionId, e.getMessage(), e);
        }

    }

    @Override
    public boolean isAMagnetMarket() {
        return true;
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
	    return 0;
    }
}
