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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.UserModelDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.UserModelFinder;
import it.softsolutions.bestx.model.UserModel;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: alberto.plebani
* Creation date: 30/ago/2017
* 
**/
public class DBUserModelFinder implements UserModelFinder {
   
   private static final Logger LOGGER = LoggerFactory.getLogger(DBUserModelFinder.class);
   
   private UserModelDao userModelDao;
   
   private void checkPreRequisites() throws ObjectNotInitializedException {
      if (userModelDao == null) {
          throw new ObjectNotInitializedException("userModelDao not set");
      }
      
   }
   
   public UserModelDao getUserModelDao() {
      return userModelDao;
   }
   
   public void setUserModelDao(UserModelDao userModelDao) {
      this.userModelDao = userModelDao;
   }

   public void init() throws BestXException {
      checkPreRequisites();
   }

   @Override
   public UserModel getUserByUserName(String userName) throws BestXException {
      LOGGER.info("DBUserModelFinder getUserByUserName called for userName {}", userName);
      return (userName == null ? null : userModelDao.getUserByUserName(userName));
   }

   /*@Override
   public String getGroupForUser(String userName) throws BestXException {
      LOGGER.info("DBUserModelFinder getGroupForUser called for userName {}", userName);
      return (userName == null ? null : userModelDao.getGroupForUser(userName));
   }*/
   
   
   
}
