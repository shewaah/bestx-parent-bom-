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
package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq;

/**
 * 
 * Purpose: interface for a listener to a buy side connection
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 05/set/2012
 * 
 **/
public interface MarketBuySideConnectionListener {
    /**
     * Manage a RFQ reject
     * 
     * @param source
     *            : rejector
     * @param rfq
     *            : rfq
     * @param reason
     *            : reject reason
     */
    void onMarketRfqReject(MarketBuySideConnection source, Rfq rfq, String reason);

    /**
     * Manage a market proposal
     * 
     * @param source
     *            : proposal source
     * @param instrument
     *            : proposal instrument
     * @param proposal
     *            : proposal
     */
    void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal);

    /**
     * Manage an unexpecetd market proposal (reject it if needed)
     * 
     * @param source
     *            : proposal source
     * @param instrument
     *            : proposal instrument
     * @param proposal
     *            : proposal
     */
    void onUnexpectedMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal);
    
    /**
     * Manage a market proposal status change
     * 
     * @param source
     *            : proposal source
     * @param instrument
     *            : proposal instrument
     * @param proposal status
     *            : proposal new status
     */
    void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId, Proposal.ProposalType proposalStatus);
    
    /**
     * Manage a market reject for the order
     * 
     * @param source
     *            : rejector
     * @param order
     *            : order rejected
     * @param reason
     *            : reject reason
     */
    void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId);

    /**
     * Manage the reception of an execution report from the market
     * 
     * @param source
     *            : report source
     * @param order
     *            : order executed
     * @param marketExecutionReport
     *            : market execution report
     */
    void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport);

    /**
     * Manage an accept to a revocation
     * 
     * @param source
     *            : accept source
     */
    void onMarketRevocationAccepted(MarketBuySideConnection source);

    /**
     * MAnage a reject to a revocation
     * 
     * @param source
     *            : reject source
     * @param reason
     *            : reject reason
     */
    void onMarketRevocationRejected(MarketBuySideConnection source, String reason);

    /**
     * Manage a match found
     * 
     * @param source
     *            : matching source
     * @param matching
     *            : match
     */
    void onMarketMatchFound(MarketBuySideConnection source, Operation matching);

    /**
     * Manage an order status message
     * 
     * @param source
     *            : message source
     * @param order
     *            : order
     * @param orderStatus
     *            : status
     */
    void onMarketOrderStatus(MarketBuySideConnection source, Order order, Order.OrderStatus orderStatus);

    /**
     * Manage the retrieval of a tradefeed ticket
     * 
     * @param source
     *            : market
     * @param ticketNum
     *            : ticket number found
     * @param numberOfDaysAccrued
     *            : days accrued
     * @param accruedInterestAmount
     *            : accrued interest
     */
    void onTradeFeedDataRetrieved(MarketBuySideConnection source, String ticketNum, int numberOfDaysAccrued, BigDecimal accruedInterestAmount);

    /**
     * Manage a market order cancel
     * 
     * @param source
     *            : cancel source
     * @param order
     *            : order
     * @param reason
     *            : cancel reason
     */
    void onMarketOrderCancelled(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage a market reject to a cancel request
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : reject reason
     */
    void onMarketOrderCancelRequestReject(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage an order cancel with fill and the order on the book
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : cancel reason
     */
    void onMarketOrderCancelFillAndBook(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage an order cancel with fill and the order not on the book
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : cancel reason
     */
    void onMarketOrderCancelFillNoBook(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage an order cancel without fill
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : cancel reason
     */
    void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage an order cancel without fill
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : cancel reason
     * @param marketExecutionReport
     *            : market execution report
     */
    void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason, MarketExecutionReport marketExecutionReport);

    /**
     * Manage a market technical reject
     * 
     * @param source
     *            : market
     * @param order
     *            : order
     * @param reason
     *            : reject reason
     */
    void onMarketOrderTechnicalReject(MarketBuySideConnection source, Order order, String reason);

    /**
     * Manage price reception
     * 
     * @param orderId
     *            : order id
     * @param marketPrice
     *            : market price
     * @param order
     *            : order
     * @param marketExecutionReport
     *            : execution report
     */
    void onMarketPriceReceived(String orderId, BigDecimal marketPrice, Order order, MarketExecutionReport marketExecutionReport);

    /**
     * Manage a market response
     */
    void onMarketResponseReceived();

    /**
     * Manage a quote status timeout (expiration)
     */
    void onQuoteStatusTimeout(MarketBuySideConnection source, Order order, String sessionId);   
    
    /**
     * Manage a quote status tradeend
     */
    void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text);
    
    /**
     * Manage the reception of a liftime update for a RFQ
     * 
     * @param timerExpirationDate
     *            : new expiration date for the RFQ
     */
    void onRfqLifetimeUpdate(Date timerExpirationDate);
    
}