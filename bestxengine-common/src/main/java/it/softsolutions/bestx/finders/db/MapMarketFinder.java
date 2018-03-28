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
package it.softsolutions.bestx.finders.db;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.dao.MarketDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 22/ago/2012 
* 
**/
public class MapMarketFinder implements MarketFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapMarketFinder.class);

    private MarketDao marketDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (marketDao == null) {
            throw new ObjectNotInitializedException("Market DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Market.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setMarketDao(MarketDao MarketDao) {
        this.marketDao = MarketDao;
    }

    @Override
    public Market getMarketByCode(MarketCode marketCode, SubMarketCode subMarketCode) throws BestXException {
        LOGGER.debug("{}, {}", marketCode, subMarketCode);
//        List<Market> marketList = marketDao.getMarketsByCode(marketCode, subMarketCode);
        
        String key = marketCode + "_" + subMarketCode;
        Element element = cache != null ? cache.get(key) : null;
        
        @SuppressWarnings("unchecked")
        List<Market> marketList = element != null ? (List<Market>) element.getObjectValue() : null;
        
        if (marketList == null) {
            marketList = marketDao.getMarketsByCode(marketCode, subMarketCode);
            
            if (marketList != null) {
                cache.put(new Element(key, marketList));
            }
        }
        
        if (marketList == null || marketList.size() == 0) {
            LOGGER.debug("Market not found for marketCode/subMarketCode = {}/{}", marketCode, subMarketCode);
            return null;
        } else {
            return marketList.get(0);
        }
    }

    @Override
    public List<Market> getMarketsByCode(MarketCode marketCode) throws BestXException {
        LOGGER.debug("{}", marketCode);
//        List<Market> marketList = marketDao.getMarketsByCode(marketCode, null);
        
        String key = "" + marketCode;
        Element element = cache != null ? cache.get(key) : null;
        
        @SuppressWarnings("unchecked")
        List<Market> marketList = element != null ? (List<Market>) element.getObjectValue() : null;
        
        if (marketList == null) {
            marketList = marketDao.getMarketsByCode(marketCode, null);
            
            if (marketList != null) {
                cache.put(new Element(key, marketList));
            }
        }
        
        if (marketList == null || marketList.size() == 0) {
            LOGGER.debug("Markets not found for marketCode = {}", marketCode);
        }
        return marketList;
    }
}
