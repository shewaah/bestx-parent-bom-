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
package it.softsolutions.bestx.connections.tradeweb.tradexpress;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.connections.tradestac.TradeStacConnectionListener;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix50.TSExecutionReport;

/**
 * Purpose: this class is a listener to some events from TradeXpress. Here we
 * List only the events of interest which are in some way correlated to the
 * requests sent using the {@link TradeXpressConnection}.
 * 
 * @author Davide Rossoni
 * 
 *         Project Name : bestx-tradeweb-market First created by: davide.rossoni
 *         Creation date: 19/dec/2014
 * 
 **/
public interface TradeXpressConnectionListener extends TradeStacConnectionListener {

    /**
     * Notifies that an order has been rejected. This can be cause (at FIX
     * level) by a Reject or a BusinessMessageReject.
     * 
     * @param sessionId
     *            the String representation of the SessionID used to send the
     *            rejected order.
     * @param clOrdId
     *            the identifier of the order that has been rejected.
     * @param reason
     *            explains why the order has been rejected.
     */
    void onOrderReject(String sessionId, String clOrdId, String reason);

    /**
     * Notifies that an order cancellation has been denied.
     * 
     * @param sessionId
     *            the String representation of the SessionID used to cancel the
     *            order.
     * @param clOrdId
     *            the identifier of the OrderCancel message.
     * @param reason
     *            explains while the order cancel has been refused.
     */
    void onOrderCancelReject(String sessionId, String clOrdId, String reason);

    // Negotiation done
    // ExecType = Trade and OrdStatus = Filled
    // > Dealer specified in the NoPartyIDs group where 452=1 and 447=B

    // ** Negotiation failed (a. dealer price worse than limit price, b. dealer
    // timeout)
    // ExecType = Cancel and OrdStatus = Canceled
    // - If none of the dealer quotes within a given RFQ match the target level
    // Text = Target price not met
    // - If the best valid dealer quote (upon attempted execution) returns a
    // time-out:
    // Text = Trading session ended

    /**
     * Notifies that the negotiation has been concluded.
     * 
     * @param sessionId
     *            the String representation of the SessionID used to send the
     *            order.
     * @param clOrdId
     *            the identifier of the corresponding Order.
     * @param execType
     *            the purpose of the execution report (New, Trade, etc.).
     * @param ordStatus
     *            the current state of the order(New,Filled, Cancel, etc.).
     * @param accruedInterestAmount
     *            the amount of accrued interest.
     * @param accruedInterestRate
     *            the annualized accrued interest amount divided by the purchase
     *            price.
     * @param lastPrice
     *            the price of this order if it has been filled.
     * @param contractNo
     *            the unique identifier of the corresponding execution report as
     *            assigned by sell-side.
     * @param futSettDate
     *            the date of trade settlement.
     * @param transactTime
     *            the time of execution/order creation.
     * @param text
     *            the text. Can contain additional informations.
     * @param executionBroker
     * 			  the execution firm code
     */
    void onExecutionReport(String sessionId, String clOrdId, TSExecutionReport tsExecutionReport);

}
