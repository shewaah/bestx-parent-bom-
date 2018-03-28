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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.HolidaysDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Holiday;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateHolidayDao implements HolidaysDao {
    private SessionFactory sessionFactory;
    private String allCountryCode;

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
    public boolean isAnHoliday(String currency, Date date) {
        Holiday holiday = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            holiday = (Holiday) session.createQuery("from Holiday where currency = :currency and date = :date")
                    .setString("currency", currency)
                    .setDate("date", date)
                    .uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return (holiday != null);
    }
    
    @Override
    public boolean isAnHoliday(String currency, String countryCode, Date date) {
        Holiday holiday = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            holiday = (Holiday) session.createQuery("from Holiday where currency = :currency and (countryCode = :countryCode OR countryCode = 'ALL') and date = :date")
                    .setString("currency", currency)
                    .setString("countryCode", countryCode)
                    .setDate("date", date)
                    .uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return (holiday != null);
    }
    

    @SuppressWarnings("unchecked")
    @Override
    public List<Holiday> getFilteredHolidays(String currency, String countryCode) {
        List<Holiday> holidays = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            holidays = session.createQuery("from Holiday where currency = :currency and (countryCode = :countryCode OR countryCode = 'ALL')")
                    .setString("currency", currency)
                    .setString("countryCode", countryCode)
                    .list();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return holidays;
    }

    /**
     * Return the code for ALL country filter
     * 
     * @return ALL country code
     */
    public String getAllCountryCode() {
        return allCountryCode;
    }

    /**
     * Set the code for ALL country
     * 
     * @param allCountryCode
     *            ALL country code
     */
    public void setAllCountryCode(String allCountryCode) {
        this.allCountryCode = allCountryCode;
    }

}
