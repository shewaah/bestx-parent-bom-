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
import it.softsolutions.bestx.dao.TraderDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.TraderFinder;
import it.softsolutions.bestx.model.Trader;
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
public class MapTraderFinder implements TraderFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapTraderFinder.class);

    private TraderDao traderDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (traderDao == null) {
            throw new ObjectNotInitializedException("Trader DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Trader.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setTraderDao(TraderDao traderDao) {
        this.traderDao = traderDao;
    }

    @Override
    public Trader getTraderById(String traderId) {
//        Trader trader = traderDao.getTraderByTraderID(traderId);
        
        String key = traderId;
        Element element = cache != null ? cache.get(key) : null;
        Trader trader = element != null ? (Trader) element.getObjectValue() : null;
        
        if (trader == null) {
            trader = traderDao.getTraderByTraderID(traderId);
            
            if (trader != null) {
                cache.put(new Element(key, trader));
            }
        }
        
        if (trader == null) {
            LOGGER.error("Trader not found for traderId = {}", traderId);
        }
        return trader;
    }
}
