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
package it.softsolutions.bestx;

import it.softsolutions.bestx.dao.MarketSecurityIsinDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 19/feb/2013 
* 
**/
public class RegulatedMktIsinsLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedMktIsinsLoader.class);

    private MarketSecurityIsinDao mktSecStatusDao;
    private Map<String, List<String>> isinsAndMkts;
    private List<String> automaticCurandoMarketList;

    public void init() {
        try {
            checkPrerequisites();
        } catch (ObjectNotInitializedException e) {
            LOGGER.error("Error: {}", e.getMessage());
        } finally {
            if (mktSecStatusDao != null) {
                isinsAndMkts = mktSecStatusDao.loadInstruments();
            }
        }
    }

    public void checkPrerequisites() throws ObjectNotInitializedException {
        if (mktSecStatusDao == null)
            throw new ObjectNotInitializedException("mktSecStatusDao not set");
    }

    public void setMktSecStatusDao(MarketSecurityIsinDao mktSecStatusDao) {
        this.mktSecStatusDao = mktSecStatusDao;
    }

    public void addInstruments(String market) {
        mktSecStatusDao.addInstruments(market, isinsAndMkts);
    }

    /**
     * Add an instrument at the map; the database insert or update will be performed by the appropriate DAO, here we've only to add it at
     * the map (or barely add the market to this isin's list if it's already in the map).
     * 
     * @param isin
     *            : the isin that should be added
     * @param market
     *            : the market where this isin is quoted
     */
    public void addInstrument(String isin, String market) {
        /*
         * If the instrument is already in the list we've only to check if the market is already in it too, if not add it. If the instrument
         * isn't in the list then add it along with the market.
         */
        LOGGER.debug("Trying to add {} - {}", isin, market);

        if (isInstrumentInList(isin)) {
            LOGGER.debug("ISIN {} already registered, try adding only the market", isin);

            List<String> markets = isinsAndMkts.get(isin);
            if (markets == null) {
                markets = new ArrayList<String>();
            }

            // Logging the list of markets already available for the isin
            if (LOGGER.isDebugEnabled()) {
                if (markets != null) {
                    for (String tmpMkt : markets) {
                        LOGGER.debug("ISIN {} is already listed on {}", isin, tmpMkt);
                    }
                }
            }

            boolean mktAdded = false;
            if (!markets.contains(market)) {
                LOGGER.debug("Adding market {}", market);
                mktAdded = true;
                markets.add(market);
            }

            LOGGER.debug(mktAdded ? "Market/s added" : "No need to add markets, already registered");

        } else {
            LOGGER.debug("New ISIN {}, adding it along with the market", isin);

            // new isin, add it along with the market
            List<String> markets = new ArrayList<String>();
            markets.add(market);
            isinsAndMkts.put(isin, markets);
        }
        LOGGER.debug("Isin adding done.");
    }

    /**
     * returns true iff the given isin is in the list of managed isins
     * 
     * @param isin
     * @return true is isin is managed, false otherwise.
     */
    public boolean isInstrumentInList(String isin) {
        return isinsAndMkts.containsKey(isin);
    }

    /**
     * Returns the marketCode List to which the specified isin is mapped
     * 
     * @param isin
     * @return a List of marketCodes
     */
    public List<String> marketCodesList(String isin) {
        return isinsAndMkts.get(isin);
    }

    public int getNumIsinsLoaded() {
        return isinsAndMkts.size();
    }

    /**
     * Check if the given isin is quoted on the given market
     * 
     * @param isin
     *            : the isin to check
     * @param marketCode
     *            : the market where the isin might be quoted
     * @return true if quoted, false otherwise
     */
    public boolean isQuotedOnMarket(String isin, String marketCode) {

        // this list could be null
        List<String> marketCodes = isinsAndMkts.get(isin);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Check if {} is listed on {}", isin, marketCode);
            if (marketCodes == null) {
                LOGGER.debug("The given isin {} is not listed on regulated markets (it has not been found in the MarketSecurityStatus table).", isin);
            } else {
                // Logging the list of markets on which the isin is listed
                for (String tmpMkt : marketCodes) {
                    LOGGER.debug("{} is listed on {}", isin, tmpMkt);
                }
            }
        }

        if (marketCodes != null && marketCodes.contains(marketCode)) {
            LOGGER.debug("The instrument {} is listed on the market {}!", isin, marketCode);
            return true;
        } else {
            LOGGER.debug("The instrument {} is NOT listed on the market {}!", isin, marketCode);
            return false;
        }
    }

    public List<SubMarketCode> getSubMarketCodes(String market, String isin) {
        return mktSecStatusDao.getSubmarketCodes(market, isin);
    }

    public boolean isInstrumentInAutomaticCurandoMarketsList(String isin) {
        List<String> marketList = isinsAndMkts.get(isin);
        if (marketList == null) {
            return false;
        }
        boolean b = false;
        for (String curandoMarket : automaticCurandoMarketList) {
            if (marketList.contains(curandoMarket)) {
                b = true;
                break;
            }
        }
        return b;
    }

    public void setAutomaticCurandoMarketList(List<String> curandoMarketList) {
        this.automaticCurandoMarketList = curandoMarketList;
    }

    public List<String> getAutomaticCurandoMarketList() {
        return this.automaticCurandoMarketList;
    }

    public List<String> getMarketList(String isin) {
        return isinsAndMkts.get(isin);
    }
}
