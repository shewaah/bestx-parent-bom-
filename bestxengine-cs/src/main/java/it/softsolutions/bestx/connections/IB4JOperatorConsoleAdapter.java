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
import java.util.List;
import java.util.ResourceBundle;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.exceptions.XT2Exception;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.states.CurandoAutoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.WarningState;
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


/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: 
 * Creation date: 19-ott-2012 
 * 
 **/
@Deprecated
public class IB4JOperatorConsoleAdapter extends BaseOperatorConsoleAdapter implements IBReqRespServiceListener,
IBPubSubServiceListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(IB4JOperatorConsoleAdapter.class);
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

	@Override
	protected void checkPreRequisites() throws ObjectNotInitializedException {
		super.checkPreRequisites();

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
			catch (IBException e) {
				incrementNumberOfExceptions();
				LOGGER.warn("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_ORDER_ID);
				this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_RQF_ID + "' from message"), clientId);
				return;
			}
			String comment = null;
			try {
				comment = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT);
			}
			catch (IBException e) {
				LOGGER.debug("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_COMMENT);
			}

			final Operation operation;
			try {
				operation = getOperationRegistry().getExistingOperationById(OperationIdType.ORDER_ID, orderId);
			}
			catch (OperationNotExistingException e) {
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
//			if (!ownershipManagement(msg, reqRespService, operation, userModelFinder, LOGGER, sessionId, clientId, orderId, operationStateAuditDao))
//				return;

			final String commentCost = comment;
			LOGGER.info("Received command from GUI [{}] for Order Id: {}", commandType, orderId);
			if (IB4JOperatorConsoleMessage.CMD_RETRY.equals(commandType)) {
				incrementInNoOfRetry();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorRetryState(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_TERMINATE.equals(commandType)) {
				incrementInNoOfTerminate();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorTerminateState(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_ABORT.equals(commandType)) {
				incrementInNoOfAbort();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorAbortState(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_SUSPEND.equals(commandType)) {
				incrementInNoOfSuspend();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorSuspendState(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_RESTORE.equals(commandType)) {
				incrementInNoOfRestore();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorRestoreState(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_REINITIATE.equals(commandType)) {
				incrementInNoOfReinitiate();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorReinitiateProcess(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_FORCE_RECEIVED.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorForceReceived(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_ACCEPT_ORDER.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorAcceptOrder(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_MANUAL_MANAGE.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorManualManage(IB4JOperatorConsoleAdapter.this, "");
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_NOT_EXECUTED.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorForceNotExecution(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_FORCE_NOT_EXECUTED.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorForceNotExecutedWithoutExecutionReport(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_FORCE_EXECUTED.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorForceExecutedWithoutExecutionReport(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_RESEND_EXECUTIONREPORT.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorResendExecutionReport(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_ORDER_RETRY.equals(commandType)) {
				incrementInNoOfRetry();
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorRetryState(IB4JOperatorConsoleAdapter.this, commentCost);
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
						operation.onOperatorExceedFilterRecalculate(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_MOVE_NOT_EXECUTABLE.equals(commandType)) {
				executeTask(new Runnable() {
					@Override
					public void run() {
						operation.onOperatorMoveToNotExecutable(IB4JOperatorConsoleAdapter.this, commentCost);
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
					public void run() {
						operation.onOperatorSendDDECommand(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
			} else if (IB4JOperatorConsoleMessage.CMD_SEND_DES.equals(commandType)) {
				executeTask(new Runnable() {
					public void run() {
						operation.onOperatorSendDESCommand(IB4JOperatorConsoleAdapter.this, commentCost);
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
				catch (IBException e) {
					incrementNumberOfExceptions();
					LOGGER.error("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_STATE_NAME);
					this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_STATE_NAME + "' from message"), clientId);
					return;
				}
				Class<? extends OperationState> newState = null;
				try {
					newState = (Class<? extends OperationState>)Class.forName(newStateClassName);
				}
				catch (ClassNotFoundException e) {
					incrementNumberOfExceptions();
					LOGGER.error("Could not find state class: '{}'",  newStateClassName);
					this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Could not find state class: '" + newStateClassName + "'"), clientId);
					return;
				}
				try {
					operation.onOperatorForceState(this, newState.newInstance(), comment);
				}
				catch (InstantiationException e) {
					incrementNumberOfExceptions();
					LOGGER.error("Could not instantiate state class: '{}'", newStateClassName);
					this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Could not instantiate state class: '" + newStateClassName + "'"), clientId);
					return;
				}
				catch (IllegalAccessException e) {
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
					public void run() {
						operation.onOperatorRevokeAccepted(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
				return;
			} else if (IB4JOperatorConsoleMessage.CMD_REJECT_REVOCATION.equals(commandType)) {
				executeTask(new Runnable() {
					public void run() {
						operation.onOperatorRevokeRejected(IB4JOperatorConsoleAdapter.this, commentCost);
					}
				});
				return;
			}  else if (IB4JOperatorConsoleMessage.CMD_SEND_TO_LFNP.equals(commandType)) {
				try {
					final String finalOrderId = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID);
					executeTask(new Runnable() {
						@Override
						public void run() {
							operation.onOperatorMoveToLimitFileNoPrice(IB4JOperatorConsoleAdapter.this, finalOrderId);
						}
					});
				}
				catch (@SuppressWarnings("unused") IBException e) {
					incrementNumberOfExceptions();
					LOGGER.warn("Missing property: '{}' from message", IB4JOperatorConsoleMessage.FLD_ORDER_ID);
					this.reqRespService.sendReply(new IllegalArgumentReplyMessage(sessionId, "Missing property: '" + IB4JOperatorConsoleMessage.FLD_RQF_ID + "' from message"), clientId);
					return;
				}
			}  else if (IB4JOperatorConsoleMessage.CMD_TAKE_OWNERSHIP.equals(commandType)) {

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
					String assignToUserName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER);

					executeTask(new Runnable() {
						@Override
						public void run() {
   						if (!operation.getState().isTerminal()) {
   							if (assignToUserName != null) {
   								operation.onOperatorTakeOwnership(IB4JOperatorConsoleAdapter.this, assignToUserName);
   								operationStateAuditDao.updateTabHistoryOperatorCode(finalOrderId, assignToUserName);
   							}
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
		}  else {
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
					String mktName = msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME);
					MarketConnection mktCon = this.marketConnectionRegistry.getMarketConnection(MarketCode.valueOf(mktName));
					if (IB4JOperatorConsoleMessage.CMD_MKT_ENABLE.equals(msg.getStringProperty(IB4JOperatorConsoleMessage.CMD_CHANGE_MKT_ENABLED))) {
						LOGGER.info("Market enable request received for market : {}", mktName);
						mktCon.enableBuySideConnection();
						mktCon.enablePriceConnection();
						mktCon.setDisableComment("");
					} else {
						LOGGER.info("Market disable request received for market : {}", mktName);
						mktCon.disablePriceConnection();
						mktCon.disableBuySideConnection();
						mktCon.setDisableComment(msg.getStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_DISABLE_COMMENT));
					}
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
