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
import org.springframework.dao.DataAccessException;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.InstrumentedEhcache;
import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Instrument;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation
 * date: 24/ago/2012
 * 
 **/
public class HibernateInstrumentDao implements InstrumentDao {
	private SessionFactory sessionFactory;
	
    private Ehcache cache;

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
		
		cache = CacheManager.getInstance().getCache("bestx-" + Instrument.class.getName());
        cache = InstrumentedEhcache.instrument(CommonMetricRegistry.INSTANCE.getHealtRegistry(), cache);
	}

	@Override
	public Instrument getInstrumentByIsin(String isin) throws DataAccessException {
	    Instrument instrument = null;
	    Session session = null;
	    try {
	     session = sessionFactory.openSession();
	     instrument = (Instrument) session.createQuery("from Instrument where isin = :isin").setString("isin", isin).uniqueResult();
	    } finally {
	     if (session != null) {
	      session.close();
	     }
	    }
	    return instrument;
	   }


	@Override
	public Instrument getLastIssuedInstrument() {
		Instrument instrument = null;
	    Session session = null;
	    try {
	     session = sessionFactory.openSession();
	     instrument = (Instrument) session.createQuery("from Instrument order by issuedate desc").uniqueResult();
	    } finally {
	     if (session != null) {
	      session.close();
	     }
	    }
	    return instrument;
	   }
	

	@Override
	public long getInstrumentCount() {
		long i = 0;
		Session session = null;

		try {
			session = sessionFactory.openSession();
			i = (Long) session.createQuery("select count (*) from Instrument").uniqueResult();

		} finally {
			if (session != null) {
				session.close();
			}
		}

		return i;
	}

}
