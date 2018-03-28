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
import it.softsolutions.bestx.dao.VenueDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.Venue;

import java.util.HashSet;
import java.util.Set;

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
public class MapVenueFinder implements VenueFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapVenueFinder.class);

    private VenueDao venueDao;
    private Ehcache cache;

    public void init() throws BestXException {
        if (venueDao == null) {
            throw new ObjectNotInitializedException("Venue DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Venue.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setVenueDao(VenueDao venueDao) {
        this.venueDao = venueDao;
    }

    @Override
    public Set<Venue> getAllVenues() throws BestXException {
        Set<Venue> res = new HashSet<Venue>(venueDao.getAllVenues());
        return res;
    }

    @Override
    public Venue getMarketVenue(Market market) throws BestXException {
        LOGGER.debug("{}", market);
        Long marketID = market != null ? market.getMarketId() : null;
//        Venue venue = venueDao.getVenueByMarketId(marketID);
        
        String key = "MI#" + marketID;
        Element element = cache != null ? cache.get(key) : null;
        Venue venue = element != null ? (Venue) element.getObjectValue() : null;
        
        if (venue == null) {
            venue = venueDao.getVenueByMarketId(marketID);
            
            if (venue != null) {
                cache.put(new Element(key, venue));
            }
        }
        
        return venue;
    }

    @Override
    public Venue getMarketMakerVenue(MarketMaker marketMaker) throws BestXException {
        LOGGER.debug("{}", marketMaker);
        String bankCode = marketMaker != null ? marketMaker.getCode() : null;
//        Venue venue = venueDao.getVenueByBankCode(bankCode);
        
        String key = "BC#" + bankCode;
        Element element = cache != null ? cache.get(key) : null;
        Venue venue = element != null ? (Venue) element.getObjectValue() : null;
        
        if (venue == null) {
            venue = venueDao.getVenueByBankCode(bankCode);
            
            if (venue != null) {
                cache.put(new Element(key, venue));
            }
        }
        
        return venue;
    }

}
