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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.UserModelDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.UserModel;


public class HibernateUserModelDao implements UserModelDao {
   
   private static final Logger logger = LoggerFactory.getLogger(HibernateUserModelDao.class);
   
   
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
   public UserModel getUserByUserName(String userName) {
	   
	   UserModel userModel = null;
      Session session = null;
      try {
         session = sessionFactory.openSession();
         Query query = session.createQuery("from UserModel where userName = :userName ");
         query.setString("userName", userName);
         userModel = (UserModel) query.uniqueResult();
      } finally {
         if (session != null) {session.close();}
      }
      return userModel;
      
	}

   /*@Override
   public String getGroupForUser(String userName) {
      
      String group = null;     
      Session session = null;
      try {
         session = sessionFactory.openSession();
         Query query = session.createSQLQuery("SELECT groupName FROM users_groups WHERE userName :userName").addEntity(String.class).setParameter("userName", userName);
         //Query query = session.createQuery("SELECT groupName FROM users_groups WHERE userName :userName", String.class);
         //query.setString("userName", userName);
         group = (String) query.uniqueResult();
      } finally {
         if (session != null) {session.close();}
      }
      return group;
   }*/
	
	

}
