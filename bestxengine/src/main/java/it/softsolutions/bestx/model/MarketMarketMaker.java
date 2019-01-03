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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.services.instrument.BondTypesService;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class MarketMarketMaker implements Serializable, Comparable<MarketMarketMaker> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketMarketMaker.class);

    private static final long serialVersionUID = -7880848805721485056L;

    public static final String ENABLED_ALL = Messages.getString("MarketMakerInstrumentsFilter.EnabledAll");
    public static final String ENABLED_NONE = Messages.getString("MarketMakerInstrumentsFilter.EnabledNone");
    public static final String ENABLED_ONLY = Messages.getString("MarketMakerInstrumentsFilter.EnabledOnly");
    public static final String ENABLED_NOT_ON = Messages.getString("MarketMakerInstrumentsFilter.EnabledNotOn");

    private String marketId;
    private String enabledFilter;
    private Market market;
    private String marketSpecificCode;
    private MarketMaker marketMaker;
    private String marketSpecificCodeSource;

    public MarketMaker getMarketMaker() {
        return marketMaker;
    }

    public void setMarketMaker(MarketMaker marketMaker) {
        this.marketMaker = marketMaker;
    }

    public String getMarketSpecificCode() {
        return marketSpecificCode;
    }

    public void setMarketSpecificCode(String marketSpecificCode) {
        this.marketSpecificCode = marketSpecificCode;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    @Override
    public String toString() {
        return marketSpecificCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof MarketMarketMaker)) {
            return false;
        }

        return ((MarketMarketMaker) o).getMarketSpecificCode().equals(marketSpecificCode) && ((MarketMarketMaker) o).getMarket().equals(market);
    }

    @Override
    public int hashCode() {
        return marketSpecificCode.hashCode();
    }

    public String getEnabledFilter() {
        return enabledFilter;
    }

    public void setEnabledFilter(String enabledFilter) {
        this.enabledFilter = enabledFilter;
    }

    public boolean canTrade(Instrument instrument) {
        return canTrade(instrument, true);
    }

    public boolean canTrade(Instrument instrument, boolean bestExecutionRequired) {
        LOGGER.debug("[{}] {}, {}", marketSpecificCode, instrument, bestExecutionRequired);

        String isin = null;
        if (instrument != null) {
            isin = instrument.getIsin();
        }
        LOGGER.debug("MarketMaker {}, instrument {}. The market maker enabled filter is '{}'. Best execution required : {}", marketSpecificCode, isin, enabledFilter, bestExecutionRequired);
        if (enabledFilter == null) {
            LOGGER.debug("MarketMaker {}, instrument {}. Null enabled filter, consider if the mm can trade the instrument.", marketSpecificCode, isin);

            return true;
        }
        if (enabledFilter.contains(ENABLED_ALL)) {
            LOGGER.debug("MarketMaker {}, instrument {}. Can trade all the instruments.", marketSpecificCode, isin);

            return true;
        }
        if (enabledFilter.contains(ENABLED_NONE) && bestExecutionRequired) {
            LOGGER.info("MarketMaker {}, instrument {}. Cannot trade the instrument (enable filter = None) and best execution is required.", marketSpecificCode, isin);
            return false;
        }


        boolean contains = BondTypesService.checkBondType(instrument);

        boolean currencyEUR = "EUR".equals(instrument.getCurrency());
        LOGGER.debug("MarketMaker {}, instrument {}. Instrument currency is EUR : {}", marketSpecificCode, isin, currencyEUR);
        if (!contains || !currencyEUR) {
            boolean canTrade = enabledFilter.contains(ENABLED_NOT_ON);
            if (!canTrade) {
                LOGGER.info(
                        "Market maker {} cannot trade the instrument {}. Bond type not one of those allowed or currency not EUR, can trade this instrument only if the filter is 'Non Govies euro'.",
                        marketSpecificCode, isin);
            } else {
                LOGGER.debug("Market maker {} can trade the instrument {}. Bond type not one of those allowed or currency not EUR, can trade this instrument only if the filter is 'Non Govies euro'.",
                        marketSpecificCode, isin);
            }

            return canTrade;
        }
        if (contains && currencyEUR) {
            boolean canTrade = enabledFilter.contains(ENABLED_ONLY);
            if (!canTrade) {
                LOGGER.info(
                        "Market maker {} cannot trade the instrument {}. Bond type is one of those allowed and currency is EUR, can trade this instrument only if the filter is 'Solo Govies euro'.",
                        marketSpecificCode, isin);
            } else {
                LOGGER.debug("Market maker {} can trade the instrument {}. Bond type is one of those allowed and currency is EUR, can trade this instrument only if the filter is 'Solo Govies euro'.",
                        marketSpecificCode, isin);
            }

            return canTrade;
        }

        LOGGER.debug("MarketMaker {}, instrument {}. No conditions met, returning true, the mm can trade this instrument.", marketSpecificCode, isin);

        return true;
    }

    @Override
    public int compareTo(MarketMarketMaker o) {
        final int EQUAL = 0;

        // this optimization is usually worthwhile, and can always be added
        if (this == o) {
            return EQUAL;
        }

        StringBuilder mmMakerDef = new StringBuilder(25);
        StringBuilder otherMmMakerDef = new StringBuilder(25);

        mmMakerDef.append(this.marketId);
        mmMakerDef.append(this.marketSpecificCode);
        if (this.marketMaker != null) {
            mmMakerDef.append(this.marketMaker.getCode()).append(this.marketMaker.getName());
        }

        otherMmMakerDef.append(o.marketId);
        otherMmMakerDef.append(o.marketSpecificCode);
        if (o.marketMaker != null) {
            otherMmMakerDef.append(o.marketMaker.getCode()).append(o.marketMaker.getName());
        }

        return mmMakerDef.toString().compareTo(otherMmMakerDef.toString());
    }

	public String getMarketSpecificCodeSource() {
		return this.marketSpecificCodeSource;
	}
	public void  setMarketSpecificCodeSource(String codeSource) {
		this.marketSpecificCodeSource = codeSource;
	}

}
