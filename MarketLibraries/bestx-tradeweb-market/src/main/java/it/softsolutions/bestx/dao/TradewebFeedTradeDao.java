/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao;

import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.markets.tradeweb.model.TradewebFeedTrade;

/**
 *
 * Purpose: this interface is mainly for describe the methods used to Access and
 * manipulate the {@link TradewebFeedTrade}.
 * 
 * @author Davide Rossoni
 * 
 *         Project Name : bestx-tradeweb-market First created by: davide.rossoni
 *         Creation date: 20/feb/2013
 * 
 **/
public interface TradewebFeedTradeDao {

    /**
     * Saves Or Updates TradewebFeedTrade
     * 
     * @param trade
     *            the TradewebFeedTrade to be saved or updated.
     */
    void saveTrade(TradewebFeedTrade trade);

    /**
     * Sets the 'matched' flag to true for the specifeid trade, and saves it
     * 
     * @param trade
     *            : the trade to be assigned
     */
    void setTradeAssigned(TradewebFeedTrade trade);

    /**
     * Gets all TradewebFeedTrades not assigned ('matched' = false), starting
     * from minDate
     * 
     * @param minDate the minimum date and time to check for non assigned trades
     * @return a list of trades on Tradeweb
     */
    List<TradewebFeedTrade> getAllNotAssignedTrades(Date minDate);
}
