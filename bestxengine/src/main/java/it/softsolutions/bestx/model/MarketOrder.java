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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: Creation date: 23-ott-2012
 * 
 **/
public class MarketOrder extends Order {

	protected Market market;
    protected MarketMarketMaker marketMarketMaker;
    private boolean isInternal = false;
    protected String marketSessionId;
    private Money limitMonitorPrice;

	protected List<MarketMarketMakerSpec> dealers = new ArrayList<MarketMarketMakerSpec>();
	protected List<MarketMarketMakerSpec> excludeDealers = new ArrayList<MarketMarketMakerSpec>();
	
	private MarketOrderBuilder builder;
	
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

    
    
    public Money getLimitMonitorPrice() {
		return limitMonitorPrice;
	}

	public void setLimitMonitorPrice(Money limitMonitorPrice) {
		this.limitMonitorPrice = limitMonitorPrice;
	}

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MarketOrder [market=");
        builder.append(this.market);
        builder.append(", include=");
        builder.append(beautifyListOfDealers(this.getDealers()));
        builder.append(", exclude=");
        builder.append(beautifyListOfDealers(this.getExcludeDealers()));
        builder.append(", text=");
        builder.append(this.text);
        builder.append(", marketMarketMaker=");
        builder.append(this.marketMarketMaker);
        builder.append(", marketSessionId=");
        builder.append(this.marketSessionId);
        builder.append(", limitPrice=");
        builder.append(this.limit == null ? null : this.limit.getAmount());
        builder.append(", limitMonitorPrice=");
        builder.append(this.limitMonitorPrice == null ? null : this.limitMonitorPrice.getAmount());
        builder.append("TransactTime=");
        builder.append(this.getTransactTime());
        builder.append(", isInternal=");
        builder.append(this.isInternal);
        builder.append("]");
        return builder.toString();
    }
    
    public void setValues(MarketOrder marketOrder) {
		super.setValues((Order) marketOrder);
	    this.market = marketOrder.market;
	    this.venue = marketOrder.venue;
	    this.text = marketOrder.text;
	    this.marketMarketMaker = marketOrder.marketMarketMaker;
	    this.isInternal = marketOrder.isInternal;
	    this.marketSessionId = marketOrder.marketSessionId;
	    this.dealers = marketOrder.dealers;
	    this.excludeDealers = marketOrder.excludeDealers;
    }
    
    /** 
     * Adds to a String (instead of std toString() only the dealer MarketMarketMakerCode
     * @param dealerList
     * @return a String with all the dealers' MarketMarketMakerCode
     * 
     */
    public static String beautifyListOfDealers(List<MarketMarketMakerSpec> dealerList) {
    	return dealerList.stream().map(MarketMarketMakerSpec::getMarketMakerMarketSpecificCode).collect(Collectors.joining(", ", "[", "]"));
    }
    
	public static String beautifyBigDecimal(BigDecimal inputValue, int minScale, int maxScale) {
		int scale = inputValue.stripTrailingZeros().scale();
		if (scale < minScale) {
			scale = minScale;
		} else if (scale > maxScale) {
			scale = maxScale;
		}
		return inputValue.setScale(scale, RoundingMode.HALF_UP).toPlainString();
	}

	public MarketOrderBuilder getBuilder() {
		return builder;
	}

	public void setBuilder(MarketOrderBuilder builder) {
		this.builder = builder;
	}
	
	public MarketOrderBuilder.BuilderType getBuilderType() {
		return this.builder.getType();
	}
    
}
