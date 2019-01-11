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
package it.softsolutions.bestx.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: Creation date: 23-ott-2012
 * 
 **/
public class MarketOrder extends Order {

    private Market market;
    private Venue venue;
    private String text;
    private MarketMarketMaker marketMarketMaker;
    private boolean isInternal = false;
    private String marketSessionId;

	protected List<MarketMarketMakerSpec> dealers = new ArrayList<MarketMarketMakerSpec>();
	protected List<MarketMarketMakerSpec> excludeDealers = new ArrayList<MarketMarketMakerSpec>();
	
	public void setDealers (List<MarketMarketMakerSpec> dealers) {
		this.dealers = dealers;
	}
	
	public List<MarketMarketMakerSpec> getDealers () {
		return this.dealers;
	}
	
	public void setExcludeDealers (List<MarketMarketMakerSpec> excludeMarketMakers) {
		this.excludeDealers = excludeMarketMakers;
	}
	
	public List<MarketMarketMakerSpec> getExcludeDealers () {
		return this.excludeDealers;
	}

	public boolean isInternal() {
        return isInternal;
    }

    public void setInternal(boolean isInternal) {
        this.isInternal = isInternal;
    }

    /**
     * Gets the market market maker.
     * 
     * @return the market market maker
     */
    public MarketMarketMaker getMarketMarketMaker() {
        return marketMarketMaker;
    }

    /**
     * Sets the market market maker.
     * 
     * @param marketMarketMaker
     *            the new market market maker
     */
    public void setMarketMarketMaker(MarketMarketMaker marketMarketMaker) {
        this.marketMarketMaker = marketMarketMaker;
    }

    /**
     * Gets the market.
     * 
     * @return the market
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Sets the market.
     * 
     * @param market
     *            the new market
     */
    public void setMarket(Market market) {
        this.market = market;
    }

    @Override
    public Venue getVenue() {
        return venue;
    }

    @Override
    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    /**
     * Gets the text.
     * 
     * @return the text
     */
    @Override
	public String getText() {
        return this.text;
    }

    /**
     * Sets the text.
     * 
     * @param text
     *            the new text
     */
    @Override
	public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the marketSessionId
     */
    public String getMarketSessionId() {
        return marketSessionId;
    }

    /**
     * @param marketSessionId the marketSessionId to set
     */
    public void setMarketSessionId(String marketSessionId) {
        this.marketSessionId = marketSessionId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MarketOrder [market=");
        builder.append(this.market);
        builder.append(", venue=");
        builder.append(this.venue);
        builder.append(", text=");
        builder.append(this.text);
        builder.append(", marketMarketMaker=");
        builder.append(this.marketMarketMaker);
        builder.append(", isInternal=");
        builder.append(this.isInternal);
        builder.append(", marketSessionId=");
        builder.append(this.marketSessionId);
        builder.append("TransactTime=");
        builder.append(this.getTransactTime());
        builder.append("]");
        return builder.toString();
    }
    
    public void setValues(MarketOrder marketOrder) {
		super.setValues((Order) marketOrder);
	    this.market = marketOrder.market;
	    this.venue =marketOrder.venue;
	    this.text = marketOrder.text;
	    this.marketMarketMaker = marketOrder.marketMarketMaker;
	    this.isInternal = marketOrder.isInternal;
	    this.marketSessionId = marketOrder.marketSessionId;
    }
}
