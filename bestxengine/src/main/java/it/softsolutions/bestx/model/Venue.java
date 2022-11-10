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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Venue {

    private static final Logger LOGGER = LoggerFactory.getLogger(Venue.class);

    public static enum VenueType {
        MARKET, MARKET_MAKER
    }

    private String code;
    private boolean isMarket; // Hibernate field access
    private Market market;
    private MarketMaker marketMaker;

    public Venue() {
    }
    
    public Venue(String code) {
        this.code = code;
    }

    public Venue(Venue venue) {
        this.code = venue.code;
        this.isMarket = venue.isMarket;
        this.market = venue.market;
        this.marketMaker = venue.marketMaker;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setMarket(Market market) {
        if (this.market != null && this.market.getMarketCode() != market.getMarketCode()) {
            LOGGER.debug("Changing the market ({} -> {}) for the venue {}", this.market, market, code);
        }
        this.market = market;
    }

    public MarketMaker getMarketMaker() {
        return marketMaker;
    }

    public void setMarketMaker(MarketMaker marketMaker) {
        this.marketMaker = marketMaker;
    }

    public Market getMarket() {
        return market;
    }

    public void setVenueType(VenueType venueType) {
        if (VenueType.MARKET == venueType) {
            isMarket = true;
        } else {
            isMarket = false;
        }
    }

    public VenueType getVenueType() {
        if (isMarket) {
            return VenueType.MARKET;
        } else {
            return VenueType.MARKET_MAKER;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Venue)) {
            return false;
        }
        return ((Venue) o).getCode().equals(this.getCode());
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    public boolean isIsMarket() {
        return isMarket;
    }
}
