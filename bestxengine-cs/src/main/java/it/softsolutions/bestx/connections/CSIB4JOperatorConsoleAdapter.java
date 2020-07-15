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


package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BaseState;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.SystemStateSelector;
import it.softsolutions.bestx.connections.ib4j.ChannelStatusReplyMessage;
import it.softsolutions.bestx.connections.ib4j.IB4JOperatorConsoleMessage;
import it.softsolutions.bestx.connections.ib4j.IllegalArgumentReplyMessage;
import it.softsolutions.bestx.connections.ib4j.InternalErrorReplyMessage;
import it.softsolutions.bestx.connections.ib4j.OperationDumpMessage;
import it.softsolutions.bestx.connections.ib4j.OperationStateChangePublishMessage;
import it.softsolutions.bestx.connections.ib4j.PriceDiscoveryMessage;
import it.softsolutions.bestx.connections.ib4j.SystemStatusReplyMessage;
import it.softsolutions.bestx.connections.pricediscovery.CSPriceDiscoveryOrderLazyBean;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationAlreadyExistingException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.exceptions.XT2Exception;
import it.softsolutions.bestx.finders.UserModelFinder;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.UserModel;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.customservice.CustomServiceException;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.states.CurandoAutoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_SendRfqState;
import it.softsolutions.bestx.states.marketaxess.MA_SendOrderState;
import it.softsolutions.bestx.states.tradeweb.TW_SendOrderState;
import it.softsolutions.ib4j.IBException;
import it.softsolutions.ib4j.IBMessage;
import it.softsolutions.ib4j.IBPubSubService;
import it.softsolutions.ib4j.IBPubSubServiceListener;
import it.softsolutions.ib4j.IBReqRespService;
import it.softsolutions.ib4j.IBReqRespServiceListener;
import it.softsolutions.ib4j.clientserver.IBcsConnectionFactory;
import it.softsolutions.ib4j.clientserver.IBcsPubSubService;
import it.softsolutions.ib4j.clientserver.IBcsReqRespService;
import it.softsolutions.jsscommon.Money;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by: 
 * Creation date: 19-ott-2012 
 * 
 **/
public class CSIB4JOperatorConsoleAdapter extends BaseOperatorConsoleAdapter implements IBReqRespServiceListener,
IBPubSubServiceListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(CSIB4JOperatorConsoleAdapter.class);
   private static final ResourceBundle messages = ResourceBundle.getBundle("messages");

   private IBcsReqRespService reqRespService;
   private IBcsPubSubService pubSubService;

   private SystemStateSelector systemStateSelector;
   
   private MarketConnectionRegistry marketConnectionRegistry;
   private ConnectionRegistry connectionRegistry;
   
   private OperationStateAuditDao operationStateAuditDao;
   
   private String servicePartition;
   private String username;
   private String password;
   private volatile boolean connected;

   private List<String> visibleStatesList;
   
   private UserModelFinder userModelFinder;
   
   public UserModelFinder getUserModelFinder() {
      return userModelFinder;
   }
   
   public void setUserModelFinder(UserModelFinder userModelFinder) {
      this.userModelFinder = userModelFinder;
   }

   private CustomService customService;

   public CustomService getCustomService() {
		return customService;
	}

	public void setCustomService(CustomService customService) {
		this.customService = customService;
	}

   @Override
   protected void checkPreRequisites() throws ObjectNotInitializedException {
      super.checkPreRequisites();

      if (this.customService == null)
      {
         throw new ObjectNotInitializedException("Instrument upload service not set");
      }
      if (this.servicePartition == null)
      {
         throw new ObjectNotInitializedException("Info-Bus service partitions not set");
      }
      if (this.username == null)
      {
         throw new ObjectNotInitializedException("Info-Bus username not set");
      }
      if (this.password == null)
      {
         throw new ObjectNotInitializedException("Info-Bus password not set");
      }
      /*
      if (this.priceService == null)
         throw new ObjectNotInitializedException("Price service not set");
       */
      if (this.systemStateSelector == null)
      {
         throw new ObjectNotInitializedException("System state selector not set");
      }
      if (this.operationStateAuditDao == null)
      {
         throw new ObjectNotInitializedException("Operation state audit DAO not set");
      }
      if (this.visibleStatesList == null)
      {
         throw new ObjectNotInitializedException("visibleStatesList not set");
      }
   }

   /**
    * Sets the service partition.
    *
    * @param servicePartition the new service partition
    */
   public void setServicePartition(String servicePartition) {
      this.servicePartition = servicePartition;
   }

   /**
    * Sets the username.
    *
    * @param username the new username
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * Sets the password.
    *
    * @param password the new password
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /**
    * Sets the system state selector.
    *
    * @param systemStateSelector the new system state selector
    */
   public void setSystemStateSelector(SystemStateSelector systemStateSelector) {
      this.systemStateSelector = systemStateSelector;
   }

   /**
    * Inits the.
    *
    * @throws BestXException the best x exception
    */
   @Override
public void init() throws BestXException {
      this.checkPreRequisites();
      if (!IBcsConnectionFactory.login(this.servicePartition, this.username, this.password)) {
         throw new XT2Exception("Could not login to IB for partition: '" + this.servicePartition + "', user: '" + this.username + "', password: '" + this.password + "'");
      }
      try {
         this.reqRespService = (IBcsReqRespService)IBcsConnectionFactory.createReqRespService(getServiceName(), this.servicePartition);
      }
      catch (IBException e) {
         throw new XT2Exception("An error occurred while creating IB RR service", e);
      }
      try {
         this.pubSubService = (IBcsPubSubService)IBcsConnectionFactory.createPubSubService(getServiceName(), this.servicePartition);
      }
      catch (IBException e) {
         throw new XT2Exception("An error occurred while creating IB PS service", e);
      }
      this.reqRespService.setReqRespListener(this);
      this.pubSubService.setPubSubListener(this);
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.Connection#connect()
    */
   @Override
   public void connect() {
      LOGGER.info("Opening connection to GUI");
      LOGGER.debug("Start IB RR service");
      try {
         this.reqRespService.startService();
      }
      catch (IBException e) {
         LOGGER.error("Could not connect RR {}/{}", getServiceName(), servicePartition, e);
         return;
      }
      LOGGER.debug("Start IB PS service");
      try {
         this.pubSubService.startService();
      }
      catch (IBException e) {
         LOGGER.error("Could not connect PS {}/{}", getServiceName(), servicePartition, e);
         return;
      }
      this.connected = true;
      initLastStartup();
      this.resetStats();
      this.notifyConnectionListener();

      try {
    	  SimpleTimerManager.getInstance().start();
      } catch (SchedulerException e) {
    	  LOGGER.error("Error while trying to start the timer manager.", e);
      }
      //request first price discovery
      String intrumentCode = getSqlInstrumentDao().getLastIssuedInstrument().getIsin();
      try {
    	  requestPriceDiscovery(intrumentCode, "buy", null, "FOAD_"+UUID.randomUUID().toString(), "FOAD_"+UUID.randomUUID().toString());
      } catch (OperationAlreadyExistingException e) {
    	  LOGGER.error("Error while trying to perform first price discovery of the day.", e);
      } catch (BestXException e) {
    	  LOGGER.error("Error while trying to perform first price discovery of the day.", e);
      }
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.Connection#disconnect()
    */
   @Override
   public void disconnect() {
      LOGGER.info("Closing connection to GUI");
      LOGGER.debug("Stop IB RR service");
      this.reqRespService.stopService();
      LOGGER.debug("Stop IB PS service");
      this.pubSubService.stopService();
      this.connected = false;
      this.notifyConnectionListener();
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.Connection#isConnected()
    */
   @Override
   public boolean isConnected() {
      return this.connected;
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

   // Since this method is running in a private thread spawned just for serving this request
   // no need for starting new thread as in CmfTradingConsoleAdapter, FixCustomerAdapter
   /* (non-Javadoc)
    * @see it.softsolutions.ib4j.IBReqRespServiceListener#onRequest(it.softsolutions.ib4j.IBMessage, java.lang.String, it.softsolutions.ib4j.IBReqRespService)
    */
   @Override
   @SuppressWarnings("unchecked")
   public void onRequest(IBMessage msg, String clientId, IBReqRespService service) {
      LOGGER.info("IB Request received [{}]", msg.toString().replace(System.getProperty("line.separator"), ","));

      String sessionId = null;
      String messageType = null;
      try {
         sessionId = msg.getStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID);
         messageType = msg.getStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE);
      }
      catch (IBException e) {
         incrementNumberOfExceptions();
         if (sessionId == null)
         {
            LOGGER.error("An error occurred while getting session ID from message [{}]", msg, e);
         }
         else
         {
            LOGGER.error("An error occurred while getting message type from message [{}]", msg, e);
         }
         return;
      }
      LOGGER.info("Received Request - Type: '{}', Session ID: '{}'", messageType, sessionId);
      if (IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND.equals(messageType)) {
         String commandType = null;
         String orderId = null;
         try {
            commandType = msg.getStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND);
         }
         catch (IBException e) {
            incrementNumberOfExceptions();
            LOGGER.error("An error occurred while getting command type from message [{}]", msg, e);
            this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.TYPE_COMMAND + "' from message"), clientId);
            return;
         }
         try {
            orderId = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID);
         }
         catch (@SuppressWarnings("unused") IBException e) {
            incrementNumberOfExceptions();
            LOGGER.warn("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_ORDER_ID);
            this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_RQF_ID + "' from message"), clientId);
            return;
         }
         String comment = null;
         try {
            comment = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT);
         }
         catch (@SuppressWarnings("unused") IBException e) {
            LOGGER.debug("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_COMMENT);
         }

         final Operation operation;
         try {
            operation = getOperationRegistry().getExistingOperationById(OperationIdType.ORDER_ID, orderId);
         }
         catch (@SuppressWarnings("unused") OperationNotExistingException e) {
            LOGGER.error("No active operation was found corresponding to Order Id: {}", orderId);
            this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "No active operation was found corresponding to Order Id: " + orderId), clientId);
            return;
         }
         catch (BestXException e) {
            LOGGER.error("No active operation was found corresponding to Order Id: {}", orderId);
            this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
            return;
         }
         
         //operation owner checks
         if (!ownershipManagement(msg, reqRespService, operation, userModelFinder, LOGGER, sessionId, clientId, orderId, operationStateAuditDao))
            return;
         
         
         final String commentCost = comment;
         LOGGER.info("Received command from GUI [{}] for Order Id: {}", commandType, orderId);
         if (IB4JOperatorConsoleMessage.CMD_RETRY.equals(commandType)) {
            incrementInNoOfRetry();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorRetryState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_TERMINATE.equals(commandType)) {
            incrementInNoOfTerminate();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorTerminateState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_ABORT.equals(commandType)) {
            incrementInNoOfAbort();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorAbortState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_SUSPEND.equals(commandType)) {
            incrementInNoOfSuspend();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorSuspendState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_RESTORE.equals(commandType)) {
            incrementInNoOfRestore();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorRestoreState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_REINITIATE.equals(commandType)) {
            incrementInNoOfReinitiate();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorReinitiateProcess(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_FORCE_RECEIVED.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorForceReceived(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_ACCEPT_ORDER.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorAcceptOrder(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_MANUAL_MANAGE.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorManualManage(CSIB4JOperatorConsoleAdapter.this, "");
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_NOT_EXECUTED.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorForceNotExecution(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_FORCE_NOT_EXECUTED.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorForceNotExecutedWithoutExecutionReport(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_FORCE_EXECUTED.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorForceExecutedWithoutExecutionReport(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_RESEND_EXECUTIONREPORT.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorResendExecutionReport(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_ORDER_RETRY.equals(commandType)) {
            incrementInNoOfRetry();
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorRetryState(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_STOP_TLX_EXECUTION.equals(commandType)) {
            try {
               String currency = operation.getOrder().getCurrency();
               String amount = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE).replaceAll(",", ".");
               Money execPrice = null;
               if (!"".equals(amount))
               {
                  execPrice = new Money(currency, amount);
               }
               operation.onOperatorStopOrderExecution(this, comment, execPrice);
            } catch (IBException e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            } catch (Exception e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            }
         } else if (IB4JOperatorConsoleMessage.CMD_RICALCOLA_SBILANCIO.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorExceedFilterRecalculate(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_MOVE_NOT_EXECUTABLE.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  operation.onOperatorMoveToNotExecutable(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_AUTO_MANUAL_ORDER.equals(commandType)) {
            try {
               Money execPrice = new Money(operation.getOrder().getCurrency(), msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE).replaceAll(",", "."));
               operation.onOperatorExecuteState(this, execPrice, comment);
            } catch (IBException e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            } catch (Exception e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            }
         } else if (IB4JOperatorConsoleMessage.CMD_MERGE.equals(commandType)) {
            try {
               Money orderPrice = new Money(operation.getOrder().getCurrency(), msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE).replaceAll(",", "."));
               Money matchPrice = new Money(operation.getOrder().getCurrency(), msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MATCH_PRICE).replaceAll(",", "."));
               operation.onOperatorMatchOrders(this, orderPrice, matchPrice, comment);
            } catch (IBException e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            } catch (Exception e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            }
         } else if (IB4JOperatorConsoleMessage.CMD_SEND_DDE.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
			public void run() {
                  operation.onOperatorSendDDECommand(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_SEND_DES.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
			public void run() {
                  operation.onOperatorSendDESCommand(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
         } else if (IB4JOperatorConsoleMessage.CMD_MANUAL_EXECUTED.equals(commandType)) {
            try {
               BigDecimal qty = new BigDecimal(msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_QUANTITY).replaceAll(",", "."));
               Money refPrice = new Money(operation.getOrder().getCurrency(), new BigDecimal(msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_REF_PRICE).replaceAll(",", ".")));
               Money price = null;
               String strPrice = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE);
               if (strPrice != null && strPrice.trim().length() > 0) {
                  price = new Money(operation.getOrder().getCurrency(), new BigDecimal(strPrice.replaceAll(",", ".")));
               }

               operation.onOperatorManualExecution(this,
                     comment,
                     price,
                     qty,
                     msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME),
                     msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MARKETMAKER_CODE),
                     refPrice);
            } catch (IBException e) {
               LOGGER.error("Illegal arguments in manual ", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            } catch (Exception e) {
               LOGGER.error("Cast error", e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            }
         } else if (IB4JOperatorConsoleMessage.CMD_FORCE_STATE.equals(commandType)) {
            incrementInNoOfForceState();
            String newStateClassName;
            try {
               newStateClassName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_STATE_NAME);
            }
            catch (@SuppressWarnings("unused") IBException e) {
               incrementNumberOfExceptions();
               LOGGER.error("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_STATE_NAME);
               this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_STATE_NAME + "' from message"), clientId);
               return;
            }
            Class<? extends OperationState> newState = null;
            try {
               newState = (Class<? extends OperationState>)Class.forName(newStateClassName);
            }
            catch (@SuppressWarnings("unused") ClassNotFoundException e) {
               incrementNumberOfExceptions();
               LOGGER.error("Could not find state class: '{}'",  newStateClassName);
               this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Could not find state class: '" + newStateClassName + "'"), clientId);
               return;
            }
            try {
               operation.onOperatorForceState(this, newState.newInstance(), comment);
            }
            catch (@SuppressWarnings("unused") InstantiationException e) {
               incrementNumberOfExceptions();
               LOGGER.error("Could not instantiate state class: '{}'", newStateClassName);
               this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Could not instantiate state class: '" + newStateClassName + "'"), clientId);
               return;
            }
            catch (@SuppressWarnings("unused") IllegalAccessException e) {
               incrementNumberOfExceptions();
               LOGGER.error("Could not access state class: '{}'", newStateClassName);
               this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Could not access state class: '" + newStateClassName + "'"), clientId);
               return;
            }
         } else if (IB4JOperatorConsoleMessage.CMD_SYSTEM_ACTIVITY_ON.equals(commandType)) {
            this.systemStateSelector.setOrderEnabled(true);
            this.systemStateSelector.setRfqEnabled(true);
            this.systemStateSelector.setStateDescription(messages.getString("SYSTEM_STATE_OK"));
            this.reqRespService.sendReply(new SystemStatusReplyMessage(sessionId, this.systemStateSelector), clientId);
            return;

         }  else if (IB4JOperatorConsoleMessage.CMD_SYSTEM_ACTIVITY_OFF.equals(commandType)) {
            this.systemStateSelector.setOrderEnabled(false);
            this.systemStateSelector.setRfqEnabled(false);
            this.systemStateSelector.setStateDescription(messages.getString("SYSTEM_STATE_INACTIVE"));
            this.reqRespService.sendReply(new SystemStatusReplyMessage(sessionId, this.systemStateSelector), clientId);
            return;
         } else if (IB4JOperatorConsoleMessage.CMD_ACCEPT_REVOCATION.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
			public void run() {
                  operation.onOperatorRevokeAccepted(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
            return;
         } else if (IB4JOperatorConsoleMessage.CMD_REJECT_REVOCATION.equals(commandType)) {
            executeTask(new Runnable() {
               @Override
			public void run() {
                  operation.onOperatorRevokeRejected(CSIB4JOperatorConsoleAdapter.this, commentCost);
               }
            });
            return;
         } else if (IB4JOperatorConsoleMessage.CMD_UNRECONCILED_TRADE_MATCHED.equals(commandType)) {
            /*
             * AkrosIB4JOperatorConsoleMessage.FLD_EXECUTION_TICKET_NUMBER
             * AkrosIB4JOperatorConsoleMessage.FLD_EXECUTION_PRICE
             * AkrosIB4JOperatorConsoleMessage.FLD_EXECUTION_MARKET_MAKER
             */
            try
            {
               final BigDecimal executionPrice = new BigDecimal(msg.getDoubleProperty(IB4JOperatorConsoleMessage.FLD_EXECUTION_PRICE));
               final String executionMarketMaker = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_EXECUTION_MARKET_MAKER);
               final String ticketNumber = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_EXECUTION_TICKET_NUMBER);

               executeTask(new Runnable() {
                  @Override
				public void run() {
                     operation.onOperatorUnreconciledTradeMatched(CSIB4JOperatorConsoleAdapter.this, executionPrice, executionMarketMaker, ticketNumber);
                  }
               });
            } catch (IBException e) {
               LOGGER.error("Error while extracting price, market maker and ticket number from the message [{}]", msg, e);
               this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while retrieving operation corresponding to Order Id: " + orderId + "(" + e.getMessage() + ")"), clientId);
               return;
            }
         }  else if (IB4JOperatorConsoleMessage.CMD_SEND_TO_LFNP.equals(commandType)) {
             try {
                 final String finalOrderId = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID);
                 executeTask(new Runnable() {
                     @Override
                     public void run() {
                         operation.onOperatorMoveToLimitFileNoPrice(CSIB4JOperatorConsoleAdapter.this, finalOrderId);
                     }
                 });
              }
              catch (@SuppressWarnings("unused") IBException e) {
                 incrementNumberOfExceptions();
                 LOGGER.warn("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_ORDER_ID);
                 this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_RQF_ID + "' from message"), clientId);
                 return;
              }
           } else if (IB4JOperatorConsoleMessage.CMD_TAKE_OWNERSHIP.equals(commandType)) {
              
              /*try {
              
                 final String finalOrderId   = orderId;
                 final String finalSessionId = sessionId;
                 String assignToUserName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);
                 
                 UserModel userToAssign = userModelFinder.getUserByUserName(assignToUserName);
                 
                 if (!operation.getState().isTerminal()) {
                    operation.setOwner(userToAssign);
                    operationStateAuditDao.updateTabHistoryOperatorCode(finalOrderId, assignToUserName);
                 }
              
              } catch (@SuppressWarnings("unused") Exception e) {
                 incrementNumberOfExceptions();
                 LOGGER.error("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);
                 this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER + "' from message"), clientId);
                 return;
              }*/
              
              
              try {
                 
                 final String finalOrderId   = orderId;
                 final String finalSessionId = sessionId;
                 String assignToUserName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);
                 
                 executeTask(new Runnable() {
                    @Override
                    public void run() {
                       UserModel userToAssign;
                     try {
                        userToAssign = userModelFinder.getUserByUserName(assignToUserName);
                        if (!operation.getState().isTerminal()) {
                           if (userToAssign != null) {
                              operation.onOperatorTakeOwnership(CSIB4JOperatorConsoleAdapter.this, userToAssign);
                              operationStateAuditDao.updateTabHistoryOperatorCode(finalOrderId, assignToUserName);
                           }
                        }
                     }
                     catch (BestXException e) {
                        LOGGER.error("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);
                        CSIB4JOperatorConsoleAdapter.this.reqRespService.sendReply(new IllegalArgumentReplyMessage(finalSessionId, "Missing user with usernam: '" + assignToUserName + "' from message"), clientId);
                     }
                       
                    }
                 });
              } catch (@SuppressWarnings("unused") IBException e) {
                 incrementNumberOfExceptions();
                 LOGGER.error("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);
                 this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER + "' from message"), clientId);
                 return;
              }
              
              return;
           }
      } else if (IB4JOperatorConsoleMessage.REQ_TYPE_PRICE_DISCOVERY.equals(messageType)) {
           try {
             String instrumentCode = msg.getStringProperty(PriceDiscoveryMessage.FLD_INSTRUMENT);
             String side = msg.getStringProperty(PriceDiscoveryMessage.FLD_SIDE);
             Double quantity = null;
             try {
            	 quantity = msg.getDoubleProperty(PriceDiscoveryMessage.FLD_QUANTITY);
             } catch (@SuppressWarnings("unused") Exception e) {
            	 quantity = null;
             }
             String orderID = msg.getStringProperty(PriceDiscoveryMessage.FLD_ORDER_ID);
             String traceID = msg.getStringProperty(PriceDiscoveryMessage.FLD_TRACE_ID);
             
             requestPriceDiscovery(instrumentCode, side, quantity, orderID, traceID);
             
          }
          catch (@SuppressWarnings("unused") BestXException | IBException e) {
             LOGGER.error("No active operation was found corresponding to Order Id: {}", sessionId);
             this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while create operation with id: " + sessionId), clientId);
             return;
          }
          
      }/* else if (IB4JOperatorConsoleMessage.REQ_TYPE_CANCEL_OPERATIONS.equals(messageType)) {
         try {
            String marketName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME);
            //final List<Class<? extends BaseState>> stateClasses = getRevokableStatesForMarket(marketName);
            executeTask(new Runnable() {
               @Override
               public void run() {
                  List<Class<? extends BaseState>> stateClasses = getRevokableStatesForMarket(marketName);
                  List<Operation> operations = getOperationRegistry().getOperationsByStates(stateClasses);
                  for (Operation operation:operations) {
                     LOGGER.info("Trying to cancel operation {}", operation);
                     operation.onRevoke();
                  }
               }
            });
         }
         catch (Exception e) {
            //catch (@SuppressWarnings("unused") BestXException | IBException e) {
            LOGGER.error("No active operation was found corresponding to Order Id: {}", sessionId);
            this.reqRespService.sendReply(new InternalErrorReplyMessage(sessionId, "Error while create operation with id: " + sessionId), clientId);
            return;
         }
         
     }*/
    	  else {
         //
         // query
         //
         String queryType = null;
         try {
            queryType = msg.getStringProperty(IB4JOperatorConsoleMessage.TYPE_QUERY);
         }
         catch (IBException e) {
            incrementNumberOfExceptions();
            LOGGER.error("An error occurred while getting query type from message [{}]", msg, e);
            this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.TYPE_QUERY + "' from message"), clientId);
            return;
         }
         if (IB4JOperatorConsoleMessage.QUERY_GET_CHANNEL_STATUS.equals(queryType)) {
            LOGGER.debug("Reply to channel status request");
            this.reqRespService.sendReply(new ChannelStatusReplyMessage(sessionId, marketConnectionRegistry, connectionRegistry), clientId);
         } else if (IB4JOperatorConsoleMessage.QUERY_GET_SYSTEM_ACTIVITY_STATUS.equals(queryType)) {
            LOGGER.debug("Reply to system activity status request");
            this.reqRespService.sendReply(new SystemStatusReplyMessage(sessionId, this.systemStateSelector), clientId);
         } else if (IB4JOperatorConsoleMessage.CMD_CHANGE_MKT_ENABLED.equals(queryType)) {
            try {
               
               //if not specified (mktName null), we operate on all market connections available
               
               String mktName                                = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME);
               boolean isEnableCommand                       = IB4JOperatorConsoleMessage.CMD_MKT_ENABLE.equals(msg.getStringProperty(IB4JOperatorConsoleMessage.CMD_CHANGE_MKT_ENABLED));
               String disableComment                         = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_DISABLE_COMMENT);
               boolean priceChannel                          = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE_CHANNEL) != null;
               boolean orderChannel                          = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_CHANNEL) != null;
               boolean cancelOperations                      = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_CANCEL_OPERATIONS) != null;
               ArrayList<MarketConnection> marketConnections = new ArrayList<>();
               
               if (mktName != null && !mktName.equals(""))
                  marketConnections.add(this.marketConnectionRegistry.getMarketConnection(MarketCode.valueOf(mktName)));
               else
                  marketConnections.addAll(marketConnectionRegistry.getAllMarketConnections());
               
               for (MarketConnection marketConnection:marketConnections)
                  manageMarketConnectionStatus(marketConnection, isEnableCommand, priceChannel, orderChannel, disableComment, cancelOperations);
               
            } catch(IBException ibe) {
               LOGGER.error("Unable to find market", ibe);
            }
            return;
         } else {
            LOGGER.error("Wrong query type: '{}'", queryType);
            this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Wrong query type: '" + queryType + "'"), clientId);
         }
      }
   }

   /**
    * This method manage market connection status. It manages enable/disable commands with on-fly cancel operation requests
    * @param mktCon
    * @param isEnableCommand
    * @param priceConnection
    * @param orderConnection
    * @param disableComment
    * @param cancelOperations
    * @throws IBException
    */
   private void manageMarketConnectionStatus(MarketConnection mktCon, boolean isEnableCommand, boolean priceConnection, boolean orderConnection, String disableComment, boolean cancelOperations) throws IBException {
      if (mktCon == null)
         return;
      if (isEnableCommand) {
         LOGGER.info("Market enable request received for market : {}", mktCon.getMarketCode());
         if (orderConnection) mktCon.enableBuySideConnection();
         if (priceConnection) mktCon.enablePriceConnection();
         mktCon.setDisableComment("");
      } else {
         LOGGER.info("Market disable request received for market : {}", mktCon.getMarketCode());
         
         //if we need to cancel operations then we cancel operations first and after we manage market connection status else we
         //proceed directly with market connection status management
         if (priceConnection) mktCon.disablePriceConnection();
         if (cancelOperations) {
            executeTask(new Runnable() {
               @Override
               public void run() {
                  List<Class<? extends BaseState>> stateClasses = getRevokableStatesForMarket(mktCon.getMarketCode().name());
                  List<Operation> operations = stateClasses.size() > 0 ? getOperationRegistry().getOperationsByStates(stateClasses) : null;
                  try {
                     if (operations != null && operations.size() > 0) {
                        for (Operation operation:operations) {
                           LOGGER.info("Trying to cancel operation {} for market {}", operation, mktCon.getMarketCode().name());
                           operation.onRevoke();
                        }
                     }
                  } catch (Exception e) {
                     LOGGER.error("Error during cancel operation");
                  } finally {
                     if (orderConnection) mktCon.disableBuySideConnection();
                     mktCon.setDisableComment(disableComment);
                  }
               }
            });
         } else {
            if (orderConnection) mktCon.disableBuySideConnection();
            mktCon.setDisableComment(disableComment);
         }
         
      }
   }
  
   private void requestPriceDiscovery(String instrumentCode, String side, Double quantity, String orderID, String traceID)
		   throws OperationAlreadyExistingException, BestXException {
	   final Operation operation;
	   operation = getOperationRegistry().getNewOperationById(OperationIdType.PRICE_DISCOVERY_ID, traceID, true);

	   CSPriceDiscoveryOrderLazyBean order = new CSPriceDiscoveryOrderLazyBean(instrumentCode, side, quantity, orderID, null, null, traceID);
	   order.setSqlInstrumentDao(getSqlInstrumentDao());
	   order.setInstrumentFinder(getInstrumentFinder());
	   operation.setOrder(order);

	   if (order.getInstrument()==null) {
           try {
				customService.sendRequest(order.getFixOrderId(), false, instrumentCode);
			} catch (CustomServiceException e) {
				throw new BestXException(e);
			}
		   JSONArray errorReport = new JSONArray();
		   errorReport.add(PriceDiscoveryMessage.LABEL_INSTRUMENT_NOT_SUPPORTED);
		   JSONObject jsonBook = PriceDiscoveryMessage.createEmptyBook(order, errorReport);
		   publishPriceDiscoveryResult(operation, jsonBook.toString());
		   return;
	   }

	   executeTask(new Runnable() {
		   @Override
		   public void run() {
			   operation.onOperatorPriceDiscovery(CSIB4JOperatorConsoleAdapter.this);
		   }
	   });
   }
   
   /**
    * This method returns a list of State classes that represent the specific market state that must be looked for during a cancel all orders operation
    * Please, refer to Market.Marketcode enum to compare market names
    * @param marketName
    * @return
    */
   private List<Class<? extends BaseState>> getRevokableStatesForMarket(String marketName) {
      ArrayList<Class<? extends BaseState>> classes = new ArrayList<>();
      if (marketName.equals("TW")) {
         classes.add(TW_SendOrderState.class);
      } else if (marketName.equals("MA") || marketName.equals("MARKETAXESS")) {
         classes.add(MA_SendOrderState.class);
      } else if (marketName.equals("BLOOMBERG")) {
         classes.add(BBG_SendRfqState.class);
      }
      return classes;
   }
   

   /* (non-Javadoc)
    * @see it.softsolutions.ib4j.IBPubSubServiceListener#onSubscribe(java.lang.String, java.lang.String, it.softsolutions.ib4j.IBPubSubService)
    */
   @Override
public void onSubscribe(String subject, String connectionId, IBPubSubService service) {
      LOGGER.debug("Subscribe message received for subject: '{}' from connection: '{}'", subject, connectionId);
   }

   /* (non-Javadoc)
    * @see it.softsolutions.ib4j.IBPubSubServiceListener#onUnSubscribe(java.lang.String, java.lang.String, it.softsolutions.ib4j.IBPubSubService)
    */
   @Override
public void onUnSubscribe(String subject, String connectionId, IBPubSubService service) {
      LOGGER.debug("Unsubscribe message received for subject: '{}' from connection: '{}'", subject, connectionId);
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.OperatorConsoleConnection#publishOperationStateChange(it.softsolutions.bestx.Operation, it.softsolutions.bestx.OperationState)
    */
   @Override
public void publishOperationStateChange(Operation operation, OperationState newState) {
      incrementOutNoOfStateChange();
      Order order = operation.getOrder();
      if(newState.getClass() == WarningState.class
            || newState.getClass() == ErrorState.class
            || newState.getClass() == CurandoAutoState.class
            || newState.getClass() == ManualManageState.class)
      {
         String rfqId = operation.getIdentifier(OperationIdType.ORDER_ID);
         if (order != null) {
            this.pubSubService.publish(IB4JOperatorConsoleMessage.NOTIFY_OP_STATUS_CHG,
                  new OperationStateChangePublishMessage(rfqId, newState.getClass().getSimpleName()),
                  true);
         }
      }
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.OperatorConsoleConnection#publishOperationDump(it.softsolutions.bestx.Operation, it.softsolutions.bestx.OperationState)
    */
   @Override
public void publishOperationDump(Operation operation, OperationState newState) {
      try {
         this.pubSubService.publish(IB4JOperatorConsoleMessage.PUB_SUBJ_OP_DUMP,
               new OperationDumpMessage(operation, newState),
               false);
      }
      catch (IBException e) {
         LOGGER.error("Operation dump publish failed", e);
      }
   }
   
   @Override
public void publishPriceDiscoveryResult(Operation operation, String priceDiscoveryResult) {
	   try {
	         this.pubSubService.publish(IB4JOperatorConsoleMessage.PUB_SUBJ_PRICE_DISCOVERY,
	               PriceDiscoveryMessage.getPDResultMessage(operation, priceDiscoveryResult),
	               false);
	      }
	      catch (IBException e) {
	         LOGGER.error("publishPriceDiscoveryResult failed", e);
	      } 
   }

   /**
    * Gets the market connection registry.
    *
    * @return the marketConnectionRegistry
    */
   public MarketConnectionRegistry getMarketConnectionRegistry() {
      return this.marketConnectionRegistry;
   }

   /**
    * Sets the market connection registry.
    *
    * @param marketConnectionRegistry the marketConnectionRegistry to set
    */
   public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
      this.marketConnectionRegistry = marketConnectionRegistry;
   }
   
	public ConnectionRegistry getConnectionRegistry() {
		return connectionRegistry;
	}

	public void setConnectionRegistry(ConnectionRegistry connectionRegistry) {
		this.connectionRegistry = connectionRegistry;
	}

	/**
    * Sets the operation state audit dao.
    *
    * @param operationStateAuditDao the new operation state audit dao
    */
   public void setOperationStateAuditDao(
         OperationStateAuditDao operationStateAuditDao) {
      this.operationStateAuditDao = operationStateAuditDao;
   }

   /* (non-Javadoc)
    * @see it.softsolutions.bestx.connections.OperatorConsoleConnection#updateRevocationStateChange(it.softsolutions.bestx.Operation, it.softsolutions.bestx.Operation.RevocationState, java.lang.String)
    */
   @Override
public void updateRevocationStateChange(Operation operation, RevocationState revocationState, String comment) {
      boolean revoked;
      switch (revocationState) {
         case NONE:
            revoked = false;
            break;
         case NOT_ACKNOWLEDGED:
            revoked = false;
            break;
         case ACKNOWLEDGED:
            revoked = true;
            break;
         case MANUAL_REJECTED:
            revoked = false;
            break;
         case MANUAL_ACCEPTED:
            revoked = true;
            break;
         default:
            revoked = false;
      }
      String orderId = operation.getIdentifier(OperationIdType.ORDER_ID);
      this.operationStateAuditDao.updateRevokeState(orderId, revoked, DateService.newLocalDate(), comment);
   }

   /**
    * Gets the visible states list.
    *
    * @return the visible states list
    */
   public List<String> getVisibleStatesList()
   {
      return this.visibleStatesList;
   }

   /**
    * Sets the visible states list.
    *
    * @param visibleStatesList the new visible states list
    */
   public void setVisibleStatesList(List<String> visibleStatesList)
   {
      this.visibleStatesList = visibleStatesList;
   }
}
