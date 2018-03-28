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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.dao.ExchangeRateDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.ExchangeRateFinder;
import it.softsolutions.bestx.model.ExchangeRate;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 22/ago/2012 
* 
**/
public class MapExchangeRateFinder implements ExchangeRateFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapExchangeRateFinder.class);

    private ExchangeRateDao exchangeRateDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (exchangeRateDao == null) {
            throw new ObjectNotInitializedException("Exchange Rate DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + ExchangeRate.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setExchangeRateDao(ExchangeRateDao exchangeRateDao) {
        this.exchangeRateDao = exchangeRateDao;
    }

    @Override
    public ExchangeRate getExchangeRateByCurrency(String currency) throws BestXException {        
        String key = currency;
        Element element = cache != null ? cache.get(key) : null;
        ExchangeRate exchangeRate = element != null ? (ExchangeRate) element.getObjectValue() : null;
        
        if (exchangeRate == null) {
            exchangeRate = exchangeRateDao.getExchangeRate(currency);
            
            if (exchangeRate != null) {
                cache.put(new Element(key, exchangeRate));
            }
        }
        
        if (exchangeRate == null) {
            //[RR20120830] BXM-91 : the only way to propagate the detailed error message is to throw a new exception
            LOGGER.error("Exchange rate not found for currency {}", currency);
            throw new BestXException(Messages.getString("ExchangeRateNotFound.0", currency));
        }
        return exchangeRate;
    }
}
