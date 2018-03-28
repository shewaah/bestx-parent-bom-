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
import it.softsolutions.bestx.dao.TradeFillDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.TradeFill;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateTradeFillDao implements TradeFillDao {
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
    public synchronized List<TradeFill> getAllNotAssignedTrades(Date minDate) {
        Session session = null;
        List<TradeFill> list = null;
        try {
            session = sessionFactory.openSession();
            list = (List<TradeFill>) session.createQuery("from TradeFill where transactTime >= :transactTime").setDate("transactTime", minDate).list();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return list;
    }

    @Override
    public synchronized void setTradeAssigned(TradeFill trade) {
        trade.setAssigned(true);
        saveTrade(trade);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void saveTrade(TradeFill trade) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();

            List<TradeFill> alreadyExistingList = (List<TradeFill>) session.createQuery("from TradeFill where instrument.isin = :isin and ticket = :ticket")
                    .setString("isin", trade.getInstrument().getIsin())
                    .setString("ticket", trade.getTicket())
                    .list();

            for (TradeFill alreadyExisting : alreadyExistingList) {
                session.delete(alreadyExisting);
            }

            session.save(trade);
            tx.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
}
