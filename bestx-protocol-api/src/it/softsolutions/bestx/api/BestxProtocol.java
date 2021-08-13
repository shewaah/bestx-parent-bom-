
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
 
package it.softsolutions.bestx.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.ib4j.IB4JOperatorConsoleMessage;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.ib4j.clientserver.IBcsMessage;
import it.softsolutions.ib4j.clientserver.IBcsReqRespClient;

/**
 * This class contains the API for all the commands that the GUI can send to BestX:FI-A engine.
 * The command is allowed if and only if the order is in a status allowing the operation. To see if the command is allowed ,
 * please refer to the column <B><I>Actions</I></B> in the <B><I>TabHistoryOrdini</I></B> DB table
 * As a general rule, bestx engine accepts some of the commands 
 * 
 * This class is a singleton and must be created passing IBcsReqRespClient. This class IS NOT RESPONSIBLE for operation allowed nor for connection checking
 * 
 * @author anna.cochetti
 *
 */
public class BestxProtocol implements BestxProtocolMBean {

	private static final Logger logger = LoggerFactory.getLogger(BestxProtocol.class);
   private IBcsReqRespClient reqRespClient;
	private static String format =  DateService.dateTimeISO;
	private DateTimeFormatter formatter = DateTimeFormat.forPattern(format);

	private static BestxProtocol _instance = null;
	
	
	public static BestxProtocol createInstance(IBcsReqRespClient reqRespClient) {
	   
	   _instance = new BestxProtocol(reqRespClient);
	   	   
	   return _instance;
	}
	
	public static BestxProtocol getInstance() {
      
      if (_instance == null) {
         throw new NullPointerException("IBcsReqRespClient not passed on createInstance");
      }
      
      return _instance;
   }

	private BestxProtocol(IBcsReqRespClient reqRespClient) {
	   this.reqRespClient = reqRespClient;
	}
	
	
	/**
	 * Send an order acceptance command to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public  void sendAcceptOrderCommand(String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception{
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ACCEPT_ORDER);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE, "");
		msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send AcceptOrder on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	}
	
	/**
	 * Send a manually order execute command to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @param price the execution price
	 * @param qty the executed quantity
	 * @param market the execution platform/market. Must be one of the values acceptable in the installation
	 * @param dealer the counterpart bestx code. Must be one of the market makers accepted in the installation
	 * @param refPrice the reference price in the market (i.e. the best price in the bestx book)
	 * @exception Exception when unable to send the requested command
	 */
	public void sendManuallyExecuteCareOrderCommand (String orderNumber, String comment, String price, String qty, String market, String dealer, String refPrice, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_MANUAL_EXECUTED);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_PRICE, price);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_QUANTITY, qty);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME, market);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_MARKETMAKER_CODE, dealer);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_REF_PRICE, refPrice);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ManuallyExecuteCareOrder on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	}
	


	/**
	 * Send a accept order settlement date command to bestx engine. If the order settlement date validation fails this command forces bestx to accept the order settlement date
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendAcceptOrderSettlementDateCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ACCEPT_SETT_DATE);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send AcceptOrderSettlementDate on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
		

	}
	
	/**
	 * Send a wait order cancel command to bestx engine. When the order is in a state of validation failed the client can request by phone to stop the order waiting for their cancel request
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendWaitCancelRequestCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_REVOCATION_WAIT);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send WaitCancelRequest on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	/**
	 * Send an accept cancel request command to bestx engine.
	 * The cancel request has been received and registered by bestx engine
	 * @param orderNumber the bestx order number
	 * @param note the note to be added to the order notes field
	 * @throws Exception
	 */
    public void sendCancelRequestAcceptCommand (String orderNumber, String note, String requestingUser, boolean superTrader) throws Exception {
       IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
 		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
 		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
 		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ACCEPT_REVOCATION);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, note);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
 		logger.debug("Send CancelRequestAccept on order {}", orderNumber);
 		reqRespClient.sendRequest(msg);
    	
    }

	
	/**
	 * Send a prices discovery care order activation command to bestx engine.
	 * When the order is in a state with no valid prices this command asks bestx engine to automatically perform price discovery in search
	 * of liquidity with the preconfigured price discovery strategy
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendRestartOrderCareCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ACTIVATE_CURANDO);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send RestartOrderCare on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
}

	/**
	 * Send a wait for incoming trades (for reconciliation) command to bestx engine.
	 * When the order has been manually executed on a platform whose trades are captured by BestX:FI-A feeds, use this command to ask Bestx engine to wait for the trades.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceTradeWaitCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_FORCE_MKT_WAIT);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ForceTradeWait on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
}
	/**
	 * Send a forced command of change order state to ExecutedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state Executed.
	 * This command does not ask bestx engine to send or resend the execution report to the client OMS. To do so use command sendResendExecutionReportCommand.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceExecutedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {
		IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_FORCE_EXECUTED);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ForceExecuted on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	}

   /**
	 * Send a forced command of change order state to NotExecutedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state NotExecuted.
	 * This command does not ask bestx engine to send or resend the execution report to the client OMS. To do so use command sendResendExecutionReportCommand.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceUnexecutedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_FORCE_NOT_EXECUTED);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ForceUnexecuted on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	/**
	 * Send a forced command of change order state to ReceivedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state ReceivedState.
	 * This command in fact asks bestx engine to start the validation process on the order
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceReceivedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_FORCE_RECEIVED);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ForceReceived on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	/**
	 * Send a forced command to resend the last execution report to bestx engine.
	 * When the order has is in a technical issue state, this command to resend the last execution report to the client OMS and to change the order status accordingly.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendResendExecutionReportCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_RESEND_EXECUTIONREPORT);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ResendExecutionReport on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	
	/**
	 * Send a command to put the order to OutsidePolicyState to bestx engine.
	 * This command asks bestx engine to start a new Price Discovery and to put to OutsidePolicyOrderState the order.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendToOutsidePolicyCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_MANUAL_MANAGE);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send ToOutsidePolicy on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	
	/**
	 * Send a command to ask bestx engine to restart a cycle of automatic execution.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendRetryExecutionCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception {	
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ORDER_RETRY);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send RetryExecution on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	
}
	
	/**
	 * Sets the comment in the Note field in the order. This action changes the note in the audit database.
	 * @param orderNumber the bestx order number
	 * @param note the note to be added to the order notes field
	 * @param conn the java.sql.Connection to use to set the not in the DB
	 * @exception Exception when unable to send the requested command
	 */
	//FIXME da rendere congruente con gli altri accessi a DB
	public void setNoteToOrder (String orderNumber, String note, Connection conn) throws Exception {	
		try {
			final String sql = "UPDATE TabHistoryOrdini SET note = ? WHERE NumOrdine = ?";
         try (PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, note.replaceAll("\'", "\'\'"));
            stm.setString(2, orderNumber);
            logger.debug(sql);
            stm.executeUpdate();
         }
		} catch (Exception e) {
			logger.info("Exception in updateing the note of order {}", orderNumber, e);
		}
	}

	/**
	 * Send a accept quote from market command to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @param price the quote
	 * @param qty the quantity
	 * @param market the execution platform/market. Must be one of the values acceptable in the installation
	 * @param dealer the counterpart bestx code. Must be one of the market makers accepted in the installation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendAcceptCounterCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_ACCEPT_COUNTER);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send AcceptCounter on order {}", orderNumber);
		reqRespClient.sendRequest(msg);		
	}

	/**
	 * Send a command to bestx engine to put the order to SendNotExecutionReportState
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendNotExecutedCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_NOT_EXECUTED);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send NotExecuted on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
	}

	/**
	 * Send a command to ask bestx engine to send a cancel request of the market order to the specified execution platform or market
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @param market the execution platform/market. Must be one of the values acceptable in the installation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendMarketCancelRequestCommand (String orderNumber, String comment, String market, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_SEND_MARKET_CANCEL);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME, market);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send MarketCancelRequest on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
		
	}
	/**
	 * Send a disable market command to bestx engine
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendDisableMarketCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_DISABLE_MARKET);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send DisableMarket");
		reqRespClient.sendRequest(msg);
	}
	
	/**
	 * Send a command to reject the cancel request to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendCancelRequestRejectCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception {
	   IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
		String sessionId = formatter.print(DateService.currentTimeMillis());
		msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
		msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
		msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_REJECT_REVOCATION);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
		msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_COMMENT, comment);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
		logger.debug("Send CancelRequestReject on order {}", orderNumber);
		reqRespClient.sendRequest(msg);
		
	}
	
	/**
	 * send a command to move to Limit File No Price status
	 * @param orderNumber
	 * @param comment
	 * @param requestingUser
	 * @throws Exception
	 */
	public void sendToLFNP (String orderNumber, String requestingUser, boolean superTrader) throws Exception {
      IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
      String sessionId = formatter.print(DateService.currentTimeMillis());
      msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
      msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
      msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_SEND_TO_LFNP);
      msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
      logger.debug("Send sendToLFNP on order {}", orderNumber);
      reqRespClient.sendRequest(msg);
      
   }
	
	/**
	 * This method tries to assign ownership to userToAssign. Operation is performed by requestingUser.
	 * When requestingUser is a supertrader, then he can assign ownership to a different user (so that userToAssign
	 * is not equal to equestingUser). In any other case, userToAssign is the same of requestingUser
	 * @param orderNumber: order number
	 * @param userToAssign: user to assign as owner
	 * @param requestingUser: requesting user
	 * @throws Exception
	 */
	public void takeOwnership(String orderNumber, String userToAssign, String requestingUser, boolean superTrader) throws Exception {
      IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
      String sessionId = formatter.print(DateService.currentTimeMillis());
      msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
      msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_COMMAND);
      msg.setStringProperty(IB4JOperatorConsoleMessage.TYPE_COMMAND, IB4JOperatorConsoleMessage.CMD_TAKE_OWNERSHIP);
      msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, orderNumber);
      msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_OWNERSHIP_USER, userToAssign);
      msg.setIntProperty(IB4JOperatorConsoleMessage.FLD_IS_SUPERTRADER, (superTrader ? 1 :0));
      logger.debug("Send takeOwnership on order {} to user {} by user {}", orderNumber, userToAssign, requestingUser);
      reqRespClient.sendRequest(msg);
   }

	/*public void cancelOperationsOnMarket(String marketName, String requestingUser) throws Exception {
      IBcsMessage msg = istantiateMessageWithReqUser(requestingUser);
      String sessionId = formatter.print(DateService.currentTimeMillis());
      msg.setStringProperty(IB4JOperatorConsoleMessage.RR_SESSION_ID, sessionId);
      msg.setStringProperty(IB4JOperatorConsoleMessage.REQ_TYPE, IB4JOperatorConsoleMessage.REQ_TYPE_CANCEL_OPERATIONS);
      msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_MKT_NAME, marketName);      
      logger.debug("cancelOperationsOnMarket", requestingUser);
      reqRespClient.sendRequest(msg);
   }*/
	
	
	
	private IBcsMessage istantiateMessageWithReqUser(String requestingUser) {
	   IBcsMessage msg = new IBcsMessage();
	   msg.setStringProperty(IB4JOperatorConsoleMessage.FLD_REQUESTOR, requestingUser);
      return msg;
   }
	
	// ADD CMD_MERGE
	protected boolean isRunning() {
		return true;
	   //return connectionHelper.isConnected();
	}
	
}
