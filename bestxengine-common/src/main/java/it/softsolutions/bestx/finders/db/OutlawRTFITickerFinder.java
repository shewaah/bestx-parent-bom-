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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.dao.OutlawTickerDAO;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.OutlawTickerFinder;
import it.softsolutions.bestx.model.TickerOutlaw;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 22/ago/2012 
* 
**/
public class OutlawRTFITickerFinder implements OutlawTickerFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutlawRTFITickerFinder.class);

    private List<TickerOutlaw> outlawTickerList;
    private OutlawTickerDAO outlawTickerDao;
    
    @Override
    public boolean isOutLaw(String ticker, String currencyCode) {
        checkPrerequisites();
        for (TickerOutlaw outlawtick : outlawTickerList) {
            if (outlawtick.getTicker().equalsIgnoreCase(ticker) && outlawtick.getCurrencyCode().equalsIgnoreCase(currencyCode)) {
                return true;
            }
        }
        return false;
    }

    private void checkPrerequisites() throws ObjectNotInitializedException {
        if (outlawTickerDao == null) {
            throw new ObjectNotInitializedException("outlawTickerDao not set");
        }
        if (outlawTickerList == null) {
            throw new ObjectNotInitializedException("outlawTickerList not set");
        }
    }

    public void init() {
        outlawTickerList = outlawTickerDao.loadAll();
        LOGGER.info("Loaded in memory {} OutlawTicker Rules", outlawTickerList.size());
    }

    /**
     * @return the outlawTickerDao
     */
    public OutlawTickerDAO getOutlawTickerDao() {
        return outlawTickerDao;
    }

    /**
     * @param outlawTickerDao
     *            the outlawTickerDao to set
     */
    public void setOutlawTickerDao(OutlawTickerDAO outlawTickerDao) {
        this.outlawTickerDao = outlawTickerDao;
    }

}
