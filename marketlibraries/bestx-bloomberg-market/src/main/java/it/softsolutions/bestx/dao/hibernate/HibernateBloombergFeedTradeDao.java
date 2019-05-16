/*
 * Copyright 1997-2013 SoftSolutions! srl 
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

import it.softsolutions.bestx.dao.BloombergFeedTradeDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.markets.bloomberg.model.BloombergFeedTrade;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-bloomberg-market First created by: davide.rossoni Creation date: 20/feb/2013
 * 
 **/
public class HibernateBloombergFeedTradeDao implements BloombergFeedTradeDao {

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

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (sessionFactory == null) {
            throw new ObjectNotInitializedException("Session factory not set");
        }
    }

    @Override
    public void saveTrade(BloombergFeedTrade trade) {
        checkPreRequisites();

        Session session = null;
        try {
            session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.saveOrUpdate(trade);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Override
    public void setTradeAssigned(BloombergFeedTrade trade) {
        checkPreRequisites();
        trade.setMatched(true);
        saveTrade(trade);
    }

    @Override
    public List<BloombergFeedTrade> getAllNotAssignedTrades(Date minDate) {
        checkPreRequisites();
        Session session = null;
        List<BloombergFeedTrade> list = null;
        try {
            session = sessionFactory.openSession();
            list = (List<BloombergFeedTrade>) session.createQuery("from BloombergFeedTrade where Matched=1 and transactTime >= ?").setDate(0, minDate).list();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return list;
    }
}
