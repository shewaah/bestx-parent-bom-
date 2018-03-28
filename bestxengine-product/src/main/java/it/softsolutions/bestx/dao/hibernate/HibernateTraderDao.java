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
import it.softsolutions.bestx.dao.TraderDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Trader;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateTraderDao implements TraderDao {
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
    public Trader getTraderByTraderID(String traderId) {
        Trader trader = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            trader = (Trader) session.createQuery("from Trader where traderId = :traderId").setString("traderID", traderId).uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return trader;
    }
}
