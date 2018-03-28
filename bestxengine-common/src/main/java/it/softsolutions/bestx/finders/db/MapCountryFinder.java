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
import it.softsolutions.bestx.dao.CountryDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.CountryFinder;
import it.softsolutions.bestx.model.Country;
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
public class MapCountryFinder implements CountryFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapCountryFinder.class);

    private CountryDao countryDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (countryDao == null) {
            throw new ObjectNotInitializedException("Country DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Country.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setCountryDao(CountryDao countryDao) {
        this.countryDao = countryDao;
    }

    @Override
    public Country getCountryByCode(String code) {

        String key = code;
        Element element = cache != null ? cache.get(key) : null;
        Country country = element != null ? (Country) element.getObjectValue() : null;
        
        if (country == null) {
            country = countryDao.getCountryByCode(code);
            
            if (country != null) {
                cache.put(new Element(key, country));
            }
        }
        
        if (country == null) {
            LOGGER.error("Country not found for code = {}" + code);
        }
        return country;
    }
}
