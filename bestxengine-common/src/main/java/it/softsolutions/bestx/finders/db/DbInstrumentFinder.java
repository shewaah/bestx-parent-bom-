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
import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.dao.InstrumentParamDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.management.InstrumentFinderMBean;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.InstrumentParam;
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
public class DbInstrumentFinder implements InstrumentFinder, InstrumentFinderMBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbInstrumentFinder.class);

    private InstrumentDao instrumentDao;
    private InstrumentParamDao instrumentParamDao;
    private Ehcache cache;
    
    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (instrumentDao == null) {
            throw new ObjectNotInitializedException("Instrument DAO not set");
        }
        if (instrumentParamDao == null) {
            throw new ObjectNotInitializedException("InstrumentParam DAO not set");
        }
        
        cache = CacheManager.getInstance().getCache("bestx-" + Instrument.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
    }

    public void setInstrumentDao(InstrumentDao instrumentDao) {
        this.instrumentDao = instrumentDao;
    }
    
    public void setInstrumentParamDao(InstrumentParamDao instrumentParamDao) {
        this.instrumentParamDao = instrumentParamDao;
    }

    public void init() throws BestXException {
        checkPreRequisites();
    }

    @Override
    public Instrument getInstrumentByIsin(String isin) {

    	String key = isin;
        Element element = cache != null ? cache.get(key) : null;
        Instrument instrument = element != null ? (Instrument) element.getObjectValue() : null;
        
        if (instrument == null) {
        	
        	instrument = instrumentDao.getInstrumentByIsin(isin);
            if (instrument == null) {
                LOGGER.debug("Instrument not found for isin: {}", isin);
            }
            
            if (instrument != null) {
                cache.put(new Element(key, instrument));
            }
        }

        return instrument;
    }

    /**
     * @param isin
     * @param type
     * @param key
     * @return
     */
    @Override
    public InstrumentParam getInstrumentParam(String isin, InstrumentParam.Type type, String key) {
        LOGGER.debug("{}, {}, {}", isin, type, key);
        InstrumentParam instrumentParam = instrumentParamDao.getInstrumentParam(isin, type, key);
        return instrumentParam;
    }
    
    @Override
	public long getInstrumentCount() {
		LOGGER.debug("");
		long count = instrumentDao.getInstrumentCount();
		LOGGER.debug("{}", count);
		return count;
	}
}
