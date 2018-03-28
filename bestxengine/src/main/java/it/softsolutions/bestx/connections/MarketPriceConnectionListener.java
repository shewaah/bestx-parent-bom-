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

import java.util.Date;
import java.util.List;
import java.util.Set;

import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;

/**
 * 
 * 
 * Purpose: this class is a price connection listener ifc
 * 
 * Project Name : bestxengine First created by: Creation date:
 * 
 **/
public interface MarketPriceConnectionListener {
	
    public static enum BookState {
        COMPLETE, PARTIAL, NOT_AVAILABLE
    }

    void onMarketBookComplete(MarketCode marketCode, Book book);
    
    void onMarketBookPartial(MarketCode marketCode, Book book, String reason, List<String> marketMakersOnBothSides);

    /**
     * @param source
     *            : price source
     * @param book
     *            : resulting book
     */
    void onMarketBookComplete(MarketPriceConnection source, Book book);

    /**
     * @param source
     *            : price source
     * @param reason
     *            : description
     */
    void onMarketBookNotAvailable(MarketPriceConnection source, String reason);

    /**
     * @return : order
     */
    public Order getOrder();

    /**
     * @param marketCode
     * @return : list of enabled MMMs
     */
    public List<MarketMarketMaker> getMarketMarketMakersForMarket(MarketCode marketCode);

    /**
     * @return : list of enabled MMMs
     */
    public List<MarketMarketMaker> getMarketMarketMakersForEnabledMarkets();

    /**
     * @return : creation date
     */
    public Date getCreationDate();

    /**
     * @return : number of markets
     */
    public int getNumWaitingReplyMarketPriceConnection();

    /**
     * @return : remaining market price connections
     */
    public Set<MarketPriceConnection> getRemainingMarketPriceConnections();

    /**
     * @return : active state
     */
    public boolean isActive();

    /**
     * @return : true if disconnection ok
     */
    public boolean deactivate();

}
