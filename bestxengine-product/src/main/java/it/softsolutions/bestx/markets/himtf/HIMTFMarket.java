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
package it.softsolutions.bestx.markets.himtf;

import java.math.BigDecimal;
import java.util.Date;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.himtf.HIMTFFokOrderOutputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedFillInputBean;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.markets.VenuePriceListenerInfo;
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
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class manages the trading interaction with the HI-MTF market
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 09/ago/2012
 * 
 **/
public class HIMTFMarket extends RegulatedMarket {

    private RegulatedMktIsinsLoader regulatedMktIsinsLoader;

    private final Venue virtualVenue = new Venue("HIMTFFIX");
    private volatile boolean himtfMarketStatus;
    private volatile boolean himtfUserStatus;
    private static final Logger LOGGER = LoggerFactory.getLogger(HIMTFMarket.class);

    @Override
    public void onConnection(Connection source) {
        himtfMarketStatus = true;
        himtfUserStatus = true;
        super.onConnection(source);
    }

    @Override
    public void onConnectionError() {
        himtfMarketStatus = false;
        himtfUserStatus = false;
        super.onConnectionError();
    }

    @Override
    public void onDisconnection(Connection source, String reason) {
        himtfMarketStatus = false;
        himtfUserStatus = false;
        super.onDisconnection(source, reason);
    }

    @Override
	public void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField) {
        LOGGER.debug("HIMTF Market connection status change: field = " + statusField.name() + ", subMarket: " + subMarketCode + " , status = " + status);
        himtfMarketStatus = status;
        himtfUserStatus = status;
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.HIMTF;
    }

    @Override
	public String getConnectionName() {
        return "HIMTFFIX";
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
        return HIMTFMarket.this;
    }

    @Override
    protected void checkPriceConnection() throws  MarketNotAvailableException {
        if (!((himtfMarketStatus && himtfUserStatus))) {
            throw new MarketNotAvailableException("HIMTF status: market = " + himtfMarketStatus + ", user = " + himtfUserStatus);
        }
        if (!isConnected()) {
            throw new MarketNotAvailableException("HIMTF price connection is off");
        }
    }

    private void checkBuySideConnection() throws BestXException {
        checkPriceConnection();
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("HIMTF buy side connection is disabled");
        }
    }

    @Override
    protected String getNewPriceSessionId() throws BestXException {
        return sessionIdServer.getUniqueIdentifier("HIMTF_PRICE_REQUEST_ID", "P%08d");
    }

    private String getOperationOrderSessionId(Operation operation) throws BestXException {
        String sessionId = operation.getIdentifier(OperationIdType.HIMTF_SESSION_ID);
        if (sessionId == null) {
            sessionId = operation.getOrder().getFixOrderId();
            operationRegistry.bindOperation(operation, OperationIdType.HIMTF_SESSION_ID, sessionId);
        }
        return sessionId;
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return (himtfMarketStatus && himtfUserStatus);
    }

    @Override
    public boolean isPriceConnectionAvailable() {
        return isConnected() && isBuySideConnectionAvailable();
    }

    @Override
    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        checkBuySideConnection();
        String sessionId = getOperationOrderSessionId(listener);

        LOGGER.debug("Sending order, registering statistics.");
        marketStatistics.orderSent(listener.getOrder().getFixOrderId());

        LOGGER.debug("Send FillOrKill Order to HIMTF - " + marketOrder.toString() + " - Session ID: " + sessionId);
        Customer customer = listener.getOrder().getCustomer();
        regulatedConnection.sendFokOrder(new HIMTFFokOrderOutputBean(marketOrder, getConnectionName(), marketOrder.getMarket().getSubMarketCode().name(), getFOKOrderAccount(customer), sessionId));
    }

    @Override
    public void onExecutionReport(String sessionId, final ExecutionReportState executionReportState, final RegulatedFillInputBean regulatedFillInputBean) {
        LOGGER.debug("Execution Report event received from: " + sessionId);
        LOGGER.debug("Fill data : " + regulatedFillInputBean.toString());

        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.HIMTF_SESSION_ID, sessionId);
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Execution report received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, regulatedFillInputBean.getQtyFilled().doubleValue());

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
				public void run() {
                    Order order = operation.getOrder();

                    // Time zone of Times in FIX is GMT. Needed to have CET Time parsing
                    MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
                    marketExecutionReport.setActualQty(order.getQty());
                    marketExecutionReport.setInstrument(order.getInstrument());
                    try {
                        marketExecutionReport.setMarket(marketSecurityStatusService.getQuotingMarket(MarketCode.HIMTF, order.getInstrument().getIsin()));
                    } catch (BestXException be) {
                        LOGGER.warn("Unable to retrieve HIMTF market" + " : " + be.toString(), be);
                    }
                    marketExecutionReport.setOrderQty(order.getQty());
                    marketExecutionReport.setActualQty(regulatedFillInputBean.getQtyFilled());
                    marketExecutionReport.setLastPx(regulatedFillInputBean.getFillPrice());
                    marketExecutionReport.setPrice(new Money(order.getCurrency(), regulatedFillInputBean.getFillPrice()));
                    marketExecutionReport.setSide(order.getSide());
                    marketExecutionReport.setState(executionReportState);
                    marketExecutionReport.setTransactTime(DateService.parse(DateService.dateTimeMixedISOFIX, regulatedFillInputBean.getTransactTime()));

                    marketExecutionReport.setSequenceId(regulatedFillInputBean.getFillId()); // CD
                    marketExecutionReport.setTicket(regulatedFillInputBean.getContractNumber());
                    marketExecutionReport.setOrderTrader(regulatedFillInputBean.getOrderTrader());
                    marketExecutionReport.setSecurityIdSource(regulatedFillInputBean.getSecurityIdSource());
                    marketExecutionReport.setMarketOrderID(regulatedFillInputBean.getOrderId());
                    marketExecutionReport.setCounterPart(regulatedFillInputBean.getCounterpart());
                    try {
                        marketExecutionReport.setFutSettDate(DateService.parse(DateService.dateISO, regulatedFillInputBean.getSettlementDate()));
                    } catch (@SuppressWarnings("unused") Exception e) {
                        marketExecutionReport.setFutSettDate(order.getFutSettDate());
                    }
                    operation.onMarketExecutionReport(getMarket(), order, marketExecutionReport);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with HIMTF Session ID: " + sessionId + " : " + e.toString(), e);
        }
    }

    @Override
	public boolean isInstrumentQuotedOnMarket(Instrument instrument) {
        try {
            return (getQuotingMarket(instrument) != null);
        } catch (BestXException e) {
            LOGGER.error("An error occurred while finding quoting market" + " : " + e.toString(), e);
            return false;
        }
    }

    @Override
	public void onOrderReject(final String motSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.HIMTF_SESSION_ID, motSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
				public void run() {
                    operation.onMarketOrderReject(getMarket(), operation.getOrder(), reason, motSessionId);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with HIMTF Session ID: " + motSessionId + " : " + e.toString(), e);
        }
    }

    @Override
	public void onOrderTechnicalReject(String sessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.HIMTF_SESSION_ID, sessionId);
            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Technical reject received for the order {}, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
				public void run() {
                    operation.onMarketOrderTechnicalReject(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (@SuppressWarnings("unused") BestXException e) {
            LOGGER.error("Error while finding Operation with HIMTF Session ID: " + sessionId);
        }
    }

    @Override
	public void onOrderCancelled(String regSessionId, final String reason) {
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.HIMTF_SESSION_ID, regSessionId);

            String orderId = operation.getOrder().getFixOrderId();
            LOGGER.debug("Order {} cancelled, registering statistics.", orderId);
            marketStatistics.orderResponseReceived(orderId, 0.0);

            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                @Override
				public void run() {
                    operation.onMarketOrderCancelled(getMarket(), operation.getOrder(), reason);
                }
            });
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
    public void onOrdeReceived(String regSessionId, String orderId) {
        // method used only to record statistics
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.HIMTF_SESSION_ID, regSessionId);
            String fixOrderId = operation.getOrder().getFixOrderId();
            LOGGER.info("Exec rep with status 0 received for order " + fixOrderId);

            // statistics : response received, registering it
            LOGGER.debug("Response received for the order {}, registering statistics.", fixOrderId);
            marketStatistics.orderResponseReceived(fixOrderId, 0.0);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Operation with Session ID: " + regSessionId + " : " + e.toString(), e);
        }
    }

    @Override
	public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    public RegulatedMktIsinsLoader getRegulatedMktIsinsLoader() {
        return regulatedMktIsinsLoader;
    }

    public void setRegulatedMktIsinsLoader(RegulatedMktIsinsLoader regulatedMktIsinsLoader) {
        this.regulatedMktIsinsLoader = regulatedMktIsinsLoader;
    }

    @Override
    public void onNullPrices(String regulatedSessionId, String reason, ProposalSide side) {
        LOGGER.debug("Price notification received for " + regulatedSessionId);

        VenuePriceListenerInfo priceListenerInfo = priceListeners.get(regulatedSessionId);
        if (priceListenerInfo == null) {
            LOGGER.debug("Stale null price received from " + getConnectionName() + ". Probably request timed out.");
            return;
        }
        LOGGER.info("[PRICESRV],Market=" + ((this.getMarketCode().name() != null) ? this.getMarketCode().name() : "") + ",PricesReceivedIn="
                + (DateService.currentTimeMillis() - priceListenerInfo.getCreationTime().getTime()) + " millisecs");
        if (!priceListenerInfo.missingProposalStates.get(getVirtualVenue()).isSideArrived(side))
            priceListenerInfo.missingProposalStates.get(getVirtualVenue()).addProposalSide(side);

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
                priceListenerInfo.listeners.get(0).onMarketBookComplete(this, priceListenerInfo.sortedBook);
            }
        }
        return;
    }

    @Override
    public void onSecurityDefinition(String isin, String subMarket, Date settlementDate, BigDecimal minQty, BigDecimal minIncrement, BigDecimal qtyMultiplier) {
        try {
            regulatedMktIsinsLoader.addInstrument(isin, subMarket);
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurred while saving market instrument settlement date: " + isin + " : " + e.toString(), e);
        }
        // go on with the regulated markets common operations
        super.onSecurityDefinition(isin, subMarket, settlementDate, minQty, minIncrement, qtyMultiplier);
    }

    @Override
    public boolean isAMagnetMarket() {
        return false;
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