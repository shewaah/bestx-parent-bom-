package it.softsolutions.bestx.api;

import java.sql.Connection;

public interface BestxProtocolMBean {
	/**
	 * Send an order acceptance command to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public  void sendAcceptOrderCommand(String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception;
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
	public void sendManuallyExecuteCareOrderCommand (String orderNumber, String comment, String price, String qty, String market, String dealer, String refPrice, String requestingUser, boolean superTrader) throws Exception;		


	/**
	 * Send a accept order settlement date command to bestx engine. If the order settlement date validation fails this command forces bestx to accept the order settlement date
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendAcceptOrderSettlementDateCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;

	/**
	 * Send a wait order cancel command to bestx engine. When the order is in a state of validation failed the client can request by phone to stop the order waiting for their cancel request
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendWaitCancelRequestCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send an accept cancel request command to bestx engine.
	 * The cancel request has been received and registered by bestx engine
	 * @param orderNumber the bestx order number
	 * @param note the note to be added to the order notes field
	 * @throws Exception
	 */
	public void sendCancelRequestAcceptCommand (String orderNumber, String note, String requestingUser, boolean superTrader) throws Exception;


	/**
	 * Send a prices discovery care order activation command to bestx engine.
	 * When the order is in a state with no valid prices this command asks bestx engine to automatically perform price discovery in search
	 * of liquidity with the preconfigured price discovery strategy
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendRestartOrderCareCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send a wait for incoming trades (for reconciliation) command to bestx engine.
	 * When the order has been manually executed on a platform whose trades are captured by BestX! feeds, use this command to ask Bestx engine to wait for the trades.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceTradeWaitCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send a forced command of change order state to ExecutedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state Executed.
	 * This command does not ask bestx engine to send or resend the execution report to the client OMS. To do so use command sendResendExecutionReportCommand.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceExecutedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception ;

	/**
	 * Send a forced command of change order state to NotExecutedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state NotExecuted.
	 * This command does not ask bestx engine to send or resend the execution report to the client OMS. To do so use command sendResendExecutionReportCommand.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceUnexecutedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception ;
	/**
	 * Send a forced command of change order state to ReceivedState to bestx engine.
	 * When the order has is in a technical issue state, this command allows to set the state of the order to the state ReceivedState.
	 * This command in fact asks bestx engine to start the validation process on the order
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendForceReceivedCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception ;
	/**
	 * Send a forced command to resend the last execution report to bestx engine.
	 * When the order has is in a technical issue state, this command to resend the last execution report to the client OMS and to change the order status accordingly.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendResendExecutionReportCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;	
	/**
	 * Send a command to put the order to OutsidePolicyState to bestx engine.
	 * This command asks bestx engine to start a new Price Discovery and to put to OutsidePolicyOrderState the order.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendToOutsidePolicyCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;	
	/**
	 * Send a command to ask bestx engine to restart a cycle of automatic execution.
	 * @param orderNumber the bestx order number
	 * @exception Exception when unable to send the requested command
	 */
	public void sendRetryExecutionCommand (String orderNumber, String requestingUser, boolean superTrader) throws Exception;	
	/**
	 * Sets the comment in the Note field in the order. This action changes the note in the audit database.
	 * @param orderNumber the bestx order number
	 * @param note the note to be added to the order notes field
	 * @param conn the java.sql.Connection to use to set the not in the DB
	 * @exception Exception when unable to send the requested command
	 */
	//FIXME da rendere congruente con gli altri accessi a DB
	public void setNoteToOrder(String orderNumber, String note, Connection conn) throws Exception;
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
	public void sendAcceptCounterCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send a command to bestx engine to put the order to SendNotExecutionReportState
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendNotExecutedCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send a command to ask bestx engine to send a cancel request of the market order to the specified execution platform or market
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @param market the execution platform/market. Must be one of the values acceptable in the installation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendMarketCancelRequestCommand (String orderNumber, String comment, String market, String requestingUser, boolean superTrader) throws Exception;
	/**
	 * Send a disable market command to bestx engine
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendDisableMarketCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception;

	/**
	 * Send a command to reject the cancel request to bestx engine
	 * @param orderNumber the bestx order number
	 * @param comment any comment, possibly an empty string, required for this operation
	 * @exception Exception when unable to send the requested command
	 */
	public void sendCancelRequestRejectCommand (String orderNumber, String comment, String requestingUser, boolean superTrader) throws Exception;

	/**
	 * send a command to move to Limit File No Price status
	 * @param orderNumber
	 * @param comment
	 * @param requestingUser
	 * @throws Exception
	 */
	public void sendToLFNP (String orderNumber, String requestingUser, boolean superTrader) throws Exception;

	/**
	 * This method tries to assign ownership to userToAssign. Operation is performed by requestingUser.
	 * When requestingUser is a supertrader, then he can assign ownership to a different user (so that userToAssign
	 * is not equal to equestingUser). In any other case, userToAssign is the same of requestingUser
	 * @param orderNumber: order number
	 * @param userToAssign: user to assign as owner
	 * @param requestingUser: requesting user
	 * @throws Exception
	 */
	public void takeOwnership(String orderNumber, String userToAssign, String requestingUser, boolean superTrader) throws Exception;
}
