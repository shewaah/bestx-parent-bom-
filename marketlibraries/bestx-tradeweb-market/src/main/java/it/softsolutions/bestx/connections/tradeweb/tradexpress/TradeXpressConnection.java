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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.model.MarketOrder;

/**
 *
 * Purpose: this class is mainly for describing the main action that can be done
 * by the BestX client when dealing with TradeXpress.
 * 
 * @author Davide Rossoni
 * 
 *         Project Name : bestx-tradeweb-market First created by: davide.rossoni
 *         Creation date: 19/dec/2014
 * 
 **/
public interface TradeXpressConnection extends Connection {

    /**
     * Changes the tradeXpressConnectionListener.
     * 
     * @param tradeXpressConnectionListener
     *            the new tradeXpressConnectionListener
     */
    void setTradeXpressConnectionListener(TradeXpressConnectionListener tradeXpressConnectionListener);

    /**
     * Sends a new Market Order.
     * 
     * @param marketOrder
     *            the Market order to send.
     * @throws BestXException
     *             if an error occurs while sending the Market order.
     */
    void sendOrder(MarketOrder marketOrder) throws BestXException;

    /**
     * Cancel an existing Market Order. This can be done at the expiration of a
     * timeout, or for other reasons. We should not send an order cancel if we
     * have already received an execution report.
     * 
     * @param marketOrder the market order
     * @throws BestXException when an error prevents the cancellation
     */
    void cancelOrder(MarketOrder marketOrder) throws BestXException;

}
