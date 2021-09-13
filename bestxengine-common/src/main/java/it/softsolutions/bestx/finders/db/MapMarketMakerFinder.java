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
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.dao.MarketMakerDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
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
public class MapMarketMakerFinder implements MarketMakerFinder {

    private MarketMakerDao marketMakerDao = null;
    private Ehcache cache;

    public void init() throws BestXException {
        if (marketMakerDao == null) {
            throw new ObjectNotInitializedException("Market Maker DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + MarketMaker.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setMarketMakerDao(MarketMakerDao marketMakerDao) {
        this.marketMakerDao = marketMakerDao;
    }

    @Override
    public MarketMaker getMarketMakerByCode(String code) throws BestXException {
//        MarketMaker marketMaker = marketMakerDao.getMarketMakerByCode(code);
        
        String key = "C#" + code;
        Element element = cache != null ? cache.get(key) : null;
        MarketMaker marketMaker = element != null ? (MarketMaker) element.getObjectValue() : null;
        
        if (marketMaker == null) {
            marketMaker = marketMakerDao.getMarketMakerByCode(code);
            
            if (marketMaker != null) {
                cache.put(new Element(key, marketMaker));
            }
        }
        
        return marketMaker;
    }

    @Override
    public MarketMaker getMarketMakerByAccount(String accountCode) throws BestXException {
//        MarketMaker marketMaker = marketMakerDao.getMarketMakerByAccount(accountCode);
        
        String key = "A#" + accountCode;
        Element element = cache != null ? cache.get(key) : null;
        MarketMaker marketMaker = element != null ? (MarketMaker) element.getObjectValue() : null;
        
        if (marketMaker == null) {
            marketMaker = marketMakerDao.getMarketMakerByAccount(accountCode);
            
            if (marketMaker != null) {
                cache.put(new Element(key, marketMaker));
            }
        }
        
        return marketMaker;
    }

   
	@Override
	public MarketMarketMaker getSmartMarketMarketMakerByCode(MarketCode marketCode, String marketSpecificCode) throws BestXException {   
		MarketMarketMaker marketMarketMaker = null;
		String key = null;
		if(!Market.isABBGMarket(marketCode)) {
			key = "S#" + marketCode + '_' + marketSpecificCode;
			Element element = cache != null ? cache.get(key) : null;
			marketMarketMaker = element != null ? (MarketMarketMaker) element.getObjectValue() : null;

			if (marketMarketMaker == null) {
				marketMarketMaker = marketMakerDao.getMarketMarketMakerByCode(marketCode, marketSpecificCode);
			}
		}
		else {
			key = "S#" + MarketCode.BLOOMBERG + '_' + marketSpecificCode;
			Element element = cache != null ? cache.get(key) : null;
			marketMarketMaker = element != null ? (MarketMarketMaker) element.getObjectValue() : null;
			if (marketMarketMaker == null) {
				marketMarketMaker = marketMakerDao.getMarketMarketMakerByCode(Market.MarketCode.BLOOMBERG, marketSpecificCode);

				if (marketMarketMaker != null) {
					cache.put(new Element(key, marketMarketMaker));
				}
				else {
					key = "S#" + MarketCode.TSOX + '_' + marketSpecificCode;
					element = cache != null ? cache.get(key) : null;
					marketMarketMaker = element != null ? (MarketMarketMaker) element.getObjectValue() : null;
					if (marketMarketMaker == null) {
						marketMarketMaker = marketMakerDao.getMarketMarketMakerByCode(Market.MarketCode.TSOX, marketSpecificCode);
						if (marketMarketMaker != null) {
							cache.put(new Element(key, marketMarketMaker));
						}
					}
				}	
			}
		}
		return marketMarketMaker;
	}
    	
	@Override
    public MarketMarketMaker getMarketMarketMakerByCode(MarketCode marketCode, String marketSpecificCode)  {     
        String key = "S#" + marketCode + '_' + marketSpecificCode;
        Element element = cache != null ? cache.get(key) : null;
        MarketMarketMaker marketMarketMaker = element != null ? (MarketMarketMaker) element.getObjectValue() : null;
        
        if (marketMarketMaker == null) {
            marketMarketMaker = marketMakerDao.getMarketMarketMakerByCode(marketCode, marketSpecificCode);
            
            if (marketMarketMaker != null) {
                cache.put(new Element(key, marketMarketMaker));
            }
        }
        
        return marketMarketMaker;
    }
    
    /**
     * @param tsoxSpecificCode the market maker specific code on TSOX
     * @return the market maker associated to tsoxSpecificCode on TSOX
     */
    @Override
    public MarketMarketMaker getMarketMarketMakerByTSOXCode(String tsoxSpecificCode) {
    	MarketMarketMaker marketMarketMaker = null;
    	String key = "S#" + MarketCode.TSOX + '_' + tsoxSpecificCode;
		Element element = cache != null ? cache.get(key) : null;
		marketMarketMaker = element != null ? (MarketMarketMaker) element.getObjectValue() : null;
		if (marketMarketMaker == null) {
			marketMarketMaker = marketMakerDao.getMarketMarketMakerByCode(Market.MarketCode.TSOX, tsoxSpecificCode);
		}
        if (marketMarketMaker != null) {
            cache.put(new Element(key, marketMarketMaker));
        }
        return marketMarketMaker;
    }
}


    
