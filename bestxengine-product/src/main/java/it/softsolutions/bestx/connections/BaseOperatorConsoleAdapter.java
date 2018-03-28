/*
 * Copyright 1997-2016 SoftSolutions! srl 
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

package it.softsolutions.bestx.connections;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.ib4j.IB4JOperatorConsoleMessage;
import it.softsolutions.bestx.connections.ib4j.IllegalArgumentReplyMessage;
import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.UserModelFinder;
import it.softsolutions.bestx.management.OperatorConsoleAdapterMBean;
import it.softsolutions.bestx.model.UserModel;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.ib4j.IBMessage;
import it.softsolutions.ib4j.clientserver.IBcsReqRespService;

public abstract class BaseOperatorConsoleAdapter implements OperatorConsoleConnection, OperatorConsoleAdapterMBean {

   // Service Variables
   private String serviceName;

   private ConnectionListener listener;

   // Utils
   private OperationRegistry operationRegistry;
   private InstrumentFinder instrumentFinder;
   private InstrumentDao sqlInstrumentDao;

   // Executor
   private Executor executor;

   // Statistics variables
   private Date statLastStartup;
   private AtomicLong statOutNoOfStateChange;
   private AtomicLong statInNoOfRetry;
   private AtomicLong statInNoOfTerminate;
   private AtomicLong statInNoOfAbort;
   private AtomicLong statInNoOfSuspend;
   private AtomicLong statInNoOfRestore;
   private AtomicLong statInNoOfReinitiate;
   private AtomicLong statInNoOfForceState;
   private AtomicLong statExceptions;

   protected void checkPreRequisites() throws ObjectNotInitializedException {
      if (this.serviceName == null) {
         throw new ObjectNotInitializedException("Service name not set");
      }

      if (this.operationRegistry == null) {
         throw new ObjectNotInitializedException("Operation registry not set");
      }

      if (instrumentFinder == null) {
         throw new ObjectNotInitializedException("Instrument finder not set");
      }

      if (sqlInstrumentDao == null) {
         throw new ObjectNotInitializedException("SQL Instrument dao not set");
      }

      if (executor == null) {
         throw new ObjectNotInitializedException("Executor not set");
      }

   }

   public abstract void init() throws BestXException;

   public void setServiceName(String serviceName) {
      this.serviceName = serviceName;
   }

   protected String getServiceName() {
      return this.serviceName;
   }

   public void setOperationRegistry(OperationRegistry operationRegistry) {
      this.operationRegistry = operationRegistry;
   }

   protected OperationRegistry getOperationRegistry() {
      return operationRegistry;
   }

   public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
      this.instrumentFinder = instrumentFinder;
   }

   protected InstrumentFinder getInstrumentFinder() {
      return instrumentFinder;
   }

   public void setSqlInstrumentDao(InstrumentDao sqlInstrumentDao) {
      this.sqlInstrumentDao = sqlInstrumentDao;
   }

   protected InstrumentDao getSqlInstrumentDao() {
      return sqlInstrumentDao;
   }

   public void setExecutor(Executor executor) {
      this.executor = executor;
   }

   protected void executeTask(final Runnable task) {
      this.executor.execute(task);
   }

   // ================================================================================
   //  OperatorConsoleConnection
   // ================================================================================

   @Override
   public String getConnectionName() {
      return this.serviceName;
   }

   @Override
   public void setConnectionListener(ConnectionListener listener) {
      this.listener = listener;
   }

   protected void notifyConnectionListener() {
      if (this.listener != null) {
         if (isConnected()) {
            this.listener.onConnection(this);
         }
         else {
            this.listener.onDisconnection(this, "Server stop from operator");
         }
      }
   }

   @Override
   public void publishOperationStateChange(Operation operation, OperationState oldState) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void publishOperationDump(Operation operation, OperationState newState) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void publishPriceDiscoveryResult(Operation operation, String priceDiscoveryResult) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void updateRevocationStateChange(Operation operation, RevocationState revocationState, String comment) {
      throw new UnsupportedOperationException();
   }
   

   // ================================================================================
   // Monitoring
   // ================================================================================
   // TODO Monitoring-BX : is static??
   private int counter;

   @Override
   public int getCounter() {
      return this.counter;
   }

   @Override
   public void incrCounter() {
      // TODO Monitoring-BX : capire chi lo incrementa??
      this.counter++;
   }

   @Override
   public int getGUIStatusConnection() {
      // TODO Monitoring-BX : fa riferimento ad una enum??
      return 1;
   }

   // ================================================================================
   //  Statistics
   // ================================================================================

   /**
    * Reset stats.
    */
   public void resetStats() {
      this.statOutNoOfStateChange = new AtomicLong(0L);
      this.statInNoOfRetry = new AtomicLong(0L);
      this.statInNoOfTerminate = new AtomicLong(0L);
      this.statInNoOfAbort = new AtomicLong(0L);
      this.statInNoOfSuspend = new AtomicLong(0L);
      this.statInNoOfRestore = new AtomicLong(0L);
      this.statInNoOfReinitiate = new AtomicLong(0L);
      this.statInNoOfForceState = new AtomicLong(0L);
      this.statExceptions = new AtomicLong(0L);
   }

   public void initLastStartup() {
      this.statLastStartup = DateService.newLocalDate();
   }

   @Override
   public long getUptime() {
      if (this.statLastStartup == null) {
         return 0L;
      }
      return DateService.currentTimeMillis() - this.statLastStartup.getTime();
   }

   @Override
   public long getNumberOfExceptions() {
      return this.statExceptions.get();
   }

   protected long incrementNumberOfExceptions() {
      return this.statExceptions.incrementAndGet();
   }

   @Override
   public long getOutNoOfStateChange() {
      return this.statOutNoOfStateChange.get();
   }

   public long incrementOutNoOfStateChange() {
      return this.statOutNoOfStateChange.incrementAndGet();
   }

   @Override
   public long getInNoOfRetry() {
      return this.statInNoOfRetry.get();
   }

   public long incrementInNoOfRetry() {
      return this.statInNoOfRetry.incrementAndGet();
   }

   @Override
   public long getInNoOfTerminate() {
      return this.statInNoOfTerminate.get();
   }

   public long incrementInNoOfTerminate() {
      return this.statInNoOfTerminate.incrementAndGet();
   }

   @Override
   public long getInNoOfAbort() {
      return this.statInNoOfAbort.get();
   }

   public long incrementInNoOfAbort() {
      return this.statInNoOfAbort.incrementAndGet();
   }

   @Override
   public long getInNoOfSuspend() {
      return this.statInNoOfSuspend.get();
   }

   public long incrementInNoOfSuspend() {
      return this.statInNoOfSuspend.incrementAndGet();
   }

   @Override
   public long getInNoOfRestore() {
      return this.statInNoOfRestore.get();
   }

   public long incrementInNoOfRestore() {
      return this.statInNoOfRestore.incrementAndGet();
   }

   @Override
   public long getInNoOfReinitiate() {
      return this.statInNoOfReinitiate.get();
   }

   public long incrementInNoOfReinitiate() {
      return this.statInNoOfReinitiate.incrementAndGet();
   }

   @Override
   public long getInNoOfForceState() {
      return this.statInNoOfForceState.get();
   }

   public long incrementInNoOfForceState() {
      return this.statInNoOfForceState.incrementAndGet();
   }

   public boolean ownershipManagement(IBMessage msg, IBcsReqRespService reqRespService, Operation operation, UserModelFinder userModelFinder, Logger LOGGER, String sessionId, String clientId, String orderNumber, OperationStateAuditDao operationStateAuditDao) {
    //this method is difficult to move in superclass since it has dependecy on userModelFinder, reqRespService
      //operation owner checks
      String messageRequestor      = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_REQUESTOR, null);         
      UserModel operationOwner     = operation.getOwner();
      UserModel requestorUserModel = null;
      
      try {
         
         requestorUserModel = userModelFinder.getUserByUserName(messageRequestor);
      
         if (operationOwner == null || (requestorUserModel != null && requestorUserModel.isSuperTrader())) {
            
            operation.setOwner(requestorUserModel);
            operationStateAuditDao.updateTabHistoryOperatorCode(orderNumber, requestorUserModel.getUserName());
         
         } else {
            
            if (!operationOwner.getUserName().equals(messageRequestor)) {
               LOGGER.warn("Operation has owner {} but requestor is {}", operationOwner.getUserName(), messageRequestor);
               reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Operation has owner " + operationOwner.getUserName() + " but requestor is " + messageRequestor), clientId);
               return false;
            }
         
         }
         
         return true;
      
      } catch (Exception e) {
         LOGGER.warn("Error while retrieving requestor user model for userName {}", messageRequestor, e.getMessage());
         reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Error while retrieving requestor user model for userName " + messageRequestor), clientId);
         return false;
      }
   }
   
}
