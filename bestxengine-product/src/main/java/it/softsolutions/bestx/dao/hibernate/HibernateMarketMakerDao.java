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

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.MarketMakerDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;

/**
 * 
 * Purpose: this class manages the mapping of MarketMaker and MarketMarketMaker
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 24/ago/2012
 * 
 **/
public class HibernateMarketMakerDao implements MarketMakerDao {
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
    public MarketMaker getMarketMakerByCode(String code) {
        MarketMaker marketMaker = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            marketMaker = (MarketMaker) session.createQuery("from MarketMaker where code = :code").setString("code", code).uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return marketMaker;
    }

    @Override
    public MarketMaker getMarketMakerByAccount(String accountCode) {
        MarketMaker marketMaker = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            marketMaker = (MarketMaker) session.createQuery("select mm from MarketMaker as mm join mm.accountCodes as mma with mma.accountCode = :accountCode").setString("accountCode", accountCode)
                    .uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return marketMaker;
    }

    @Override
    public MarketMarketMaker getMarketMarketMakerByCode(MarketCode marketCode, String marketSpecificCode) {
        MarketMarketMaker marketMarketMaker = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();
            marketMarketMaker = (MarketMarketMaker) session
                    .createQuery("select mmm from MarketMarketMaker as mmm join mmm.market as m where m.marketCode = :marketCode and mmm.marketSpecificCode = :marketSpecificCode")
                    .setString("marketCode", marketCode != null ? marketCode.name() : null)
                    .setString("marketSpecificCode", marketSpecificCode)
                    .uniqueResult();
        } finally {
            if (session != null) {
                session.close();
            }
        }
        return marketMarketMaker;
    }
}