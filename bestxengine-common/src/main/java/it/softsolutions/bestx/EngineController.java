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
package it.softsolutions.bestx;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.management.EngineControllerMBean;
import it.softsolutions.bestx.services.DateService;

/**
 * 
 * Purpose: Decorator class for Connection objects. It adds robustness in management of
 * connection process. It doesn't enqueue commands: last command will be
 * immediately executed, and previous ones discarded.
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class EngineController implements EngineControllerMBean {

   private static final Logger LOGGER = LoggerFactory.getLogger(EngineController.class);

   private volatile boolean shutdown = false;
   private Date startup;

   public synchronized void idle() {
      LOGGER.info("Enter controller idle loop");
      startup = DateService.newLocalDate();
      while (true) {
         try {
            wait();
         }
         catch (InterruptedException e) {
            LOGGER.debug("Controller idle loop exiting" + " : " + e.toString(), e);
         }

         if (shutdown) {
            break;
         }
      }
      LOGGER.info("Shutdown application");
   }

   @Override
   public synchronized void shutdown() {
      shutdown = true;
      notify();
   }

   @Override
   public long getUptime() {
      return (DateService.currentTimeMillis() - startup.getTime());
   }

   @Override
   public void reloadConfiguration() {
      LOGGER.info("Reload configuration has no implementation");
   }

   public boolean getMaintainTLXNoFOKBehaviour() {
      return false;
   }

   public void setMaintainTLXNoFOKBehaviour(boolean getMaintainTLXNoFOKBehaviour) {}

   @Override
   public void reloadTimerProfiles() {
      LOGGER.info("Reload timer profiles has no implementation");
   }
}
