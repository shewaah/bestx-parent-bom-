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
import it.softsolutions.bestx.dao.VenueDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Venue;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateVenueDao implements VenueDao {
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
    @SuppressWarnings("unchecked")
    public Collection<Venue> getAllVenues() throws DataAccessException {
        List<Venue> venues = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            venues = session.createQuery("from Venue").list();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return venues;
    }

    @Override
    public Venue getVenueByMarketId(Long marketID) {
        Venue venue = null;
        Session session = null;
        try {
            // Prevent nullPointerException
            marketID = marketID != null ? marketID : Long.MAX_VALUE;
            
            session = sessionFactory.openSession();
            venue = (Venue) session.createQuery("from Venue where isMarket = true and marketID = :marketID").setLong("marketID", marketID).uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return venue;
    }

    @Override
    public Venue getVenueByBankCode(String bankCode) {
        Venue venue = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            venue = (Venue) session.createQuery("from Venue where isMarket = false and bankCode = :bankCode").setString("bankCode", bankCode).uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return venue;
    }

    
}
