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
package it.softsolutions.bestx.dao.hibernate;

import java.text.ParseException;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.MarketSecurityStatusDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.markets.MarketSecurityStatus;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.services.DateService;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateMarketSecurityStatusDao implements MarketSecurityStatusDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateMarketSecurityStatusDao.class);
    
//    private static final String DATE_PATTERN = "yyyyMMdd";
    
    private SessionFactory sessionFactory;

    /**
     * Set the Hibernate SessionFactory
     * 
     * @param sessionFactory
     *            Hibernate SessionFactory
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void init() throws BestXException {
        if (sessionFactory == null) {
            throw new ObjectNotInitializedException("Session factory not set");
        }
    }

    @Override
    public MarketSecurityStatus getMarketSecurityStatus(MarketCode marketCode, SubMarketCode subMarketCode, String isin) {
        MarketSecurityStatus status = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();

            if (subMarketCode == null) {
                status = (MarketSecurityStatus) session.createQuery("from MarketSecurityStatus where MarketCode = :marketCode and ISIN = :isin")
                        .setParameter("marketCode", marketCode != null ? marketCode.name() : null)
                        .setParameter("isin", isin)
                        .uniqueResult();
            } else {
                status = (MarketSecurityStatus) session.createQuery("from MarketSecurityStatus where MarketCode = :marketCode and SubMarketCode = :subMarketCode and ISIN = :isin")
                        .setParameter("marketCode", marketCode.name())
                        .setParameter("subMarketCode", subMarketCode.name())
                        .setParameter("isin", isin)
                        .uniqueResult();
            }

        } finally {
            if (session != null) {
                session.close();
            }
        }
        return status;
    }

    @Override
    public void saveOrUpdateMarketSecurityStatus(MarketSecurityStatus marketSecurityStatus) {
        // when we receive a security definition always update the UpdateDate to today keep only year/month/day
        String formattedDate = DateService.format(DateService.dateISO, DateService.newLocalDate());
                
        try {
            marketSecurityStatus.setUpdateDate(DateUtils.parseDate(formattedDate, new String[] { DateService.dateISO }));
        } catch (ParseException e) {
            LOGGER.error("Cannot set the UpdateDate column value for the MarketSecurityStatus table.", e);
        }

        Session session = null;
        try {
            session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.saveOrUpdate(marketSecurityStatus);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
