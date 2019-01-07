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
package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.DefaultOperationEventHandler;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue.VenueType;
import it.softsolutions.bestx.model.UserModel;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.CommissionService;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.FillManagerService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderRevocatedState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 19/feb/2013 
 * 
 **/
public class BaseOperationEventHandler extends DefaultOperationEventHandler {

	protected CustomerConnection customerConnection;

	protected OperatorConsoleConnection operatorConsoleConnection;

	protected CommissionService commissionService;

	private static final long serialVersionUID = -4981967094037164999L;

	private static Logger LOGGER = LoggerFactory.getLogger(BaseOperationEventHandler.class);

	protected static int numberOfTransitionToThisState;

	protected BaseOperationEventHandler customerSpecificHandler;

	public void setCustomerSpecificHandler(
			BaseOperationEventHandler customerSpecificHandler) {
		this.customerSpecificHandler = customerSpecificHandler;
	}
	
	public boolean hasCustomerSpecificHandler() {
		return this.customerSpecificHandler!=null;
	}

	public BaseOperationEventHandler(Operation operation, CommissionService commissionService) {
		super(operation);
		this.commissionService = commissionService;
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		if(this.customerSpecificHandler != null) 
			this.customerSpecificHandler.onTimerExpired(jobName, groupName);
		if (!operation.getState().isTerminal())
			throw new UnsupportedOperationException("Operation: '" + operation + "' - Timer Expired event not handled in state: " + operation.getState().getClass().getSimpleName() + " Timer ID: "
			        + jobName + '-' + groupName);
	}

	public BaseOperationEventHandler(Operation operation) {
		super(operation);
		this.commissionService = null;
	}

	public String getDefaultTimerJobName() {
		//[RR20150324] BXMNT-422-423 class name added at the timer default name to have it unique
		return (operation != null) ? operation.getId().toString() + "#" + this.getClass().getSimpleName() : null;
	}

	protected void setupDefaultTimer(long mSecDelay, boolean repeating) {
			String jobName = getDefaultTimerJobName();
			setupTimer(jobName, mSecDelay, repeating);
	}

	/** 
	 * Set a timer at a specified time
	 * @param time the date and time for the timer expiration
	 */
	protected void setupTimedTimer(Date time) {
			String jobName = getDefaultTimerJobName();
			setupTimer(jobName, time.getTime() - DateService.currentTimeMillis(), false);
	}

	protected void setupTimer(String jobName,long mSecDelay, boolean repeating) {
		setupTimer(jobName, mSecDelay, repeating, false);
	}
	
	/**
	 * Utility method used to schedule a new timer job
	 * @param jobName job name
	 * @param mSecDelay job delay
	 * @param repeating true if the job is repeatable, false otherwise
	 * @param isVolatile true if the timer should not be persisted on db
	 */
	protected void setupTimer(String jobName,long mSecDelay, boolean repeating, boolean isVolatile) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.setupTimer(jobName, mSecDelay, repeating);
		else {
			LOGGER.debug("timer [{}], delay {}, operation {}", jobName, mSecDelay, operation);
			String groupName = OperationRegistry.class.getSimpleName();
			try {
				SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
				//JobDetail newJob = simpleTimerManager.createNewJob(jobName, groupName);
				JobDetail newJob = simpleTimerManager.createNewJob(jobName, groupName, false, true, true);
				Trigger trigger = simpleTimerManager.createNewTrigger(jobName, groupName, repeating, mSecDelay);
				simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, isVolatile);
			} catch (SchedulerException e) {
				LOGGER.error("Error scheduling timer for {}-{}: {}", jobName, groupName, e.getMessage(), e);
			}
		}
	}

	protected void stopDefaultTimer() {
		String jobName = getDefaultTimerJobName();

		try {
			stopTimer(jobName);
		} catch (SchedulerException e) {
			LOGGER.error("Order {}: unable to remove timer", operation.getOrder().getFixOrderId(), e);
		}
	}

	/**
	 * Utility method that cancels a timer, if it has not yet triggered, and removes the handle from the operation data This method accesses
	 * operation data synchronizing on the operation, even though this method should be always be called by synchronized code.
	 * @param timerService
	 *            The {@link TimerService} providing the timer
	 * @param identifier
	 *            The operation-unique identifier for the timer
	 * 
	 * @throws SchedulerException 
	 */
	protected void stopTimer(String jobName) throws SchedulerException  {
		String groupName = OperationRegistry.class.getSimpleName();
		LOGGER.debug("Order {}, Stopping job {} of group {}", operation.getOrder().getFixOrderId(), jobName, groupName);

		SimpleTimerManager.getInstance().stopJob(jobName, groupName);
	}

	/**
	 * Utility method to create a basic execution report
	 * @param instrument The order instrument
	 * @param side The order side
	 * @param serialNumberService a {@link SerialNumberService} object to be queried for execution report unique number 
	 * @return An {@link ExecutionReport} object
	 * @throws BestXException In case execution report unique number generation would fail
	 */
	protected ExecutionReport createExecutionReport(Instrument instrument, OrderSide side, SerialNumberService serialNumberService) throws BestXException {
		if(this.customerSpecificHandler != null)
			return this.customerSpecificHandler.createExecutionReport(instrument, side, serialNumberService);
		else {
			ExecutionReport executionReport = new ExecutionReport();
			executionReport.setInstrument(instrument);
			executionReport.setSide(side);
			executionReport.setTransactTime(DateService.newLocalDate());
			long executionReportId = serialNumberService.getSerialNumber("EXEC_REP");           
			executionReport.setSequenceId(Long.toString(executionReportId));
			return executionReport;
		}
	}

	/**
	 * Sets the customer connection.
	 *
	 * @param customerConnection the new customer connection
	 */
	public void setCustomerConnection(CustomerConnection customerConnection) {
		this.customerConnection = customerConnection;
	}

	/**
	 * Sets the operator console connection.
	 *
	 * @param operatorConsoleConnection the new operator console connection
	 */
	public void setOperatorConsoleConnection(OperatorConsoleConnection operatorConsoleConnection) {
		this.operatorConsoleConnection = operatorConsoleConnection;
	}

	@Override
	public void onApplicationError(OperationState currentState, Exception exception, String message) {
		operation.setStateResilient(new WarningState(currentState, exception, message), ErrorState.class);
	}

	@Override
	public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onMarketExecutionReport(source, order, marketExecutionReport);
		else {
			Attempt currentAttempt = operation.getLastAttempt();
			if (currentAttempt == null) {
				LOGGER.error("No current Attempt found");
				return;
			}
	
			/* 2009-09-24 Ruggero
			 * On restarting bestx the markets resend us the fills.
			 * We must check not only in the current attempt, that may not exists,
			 * but also in the fill already saved on the database.
			 * Only if the received fill is not in the db we can write it.
			 */
			if (marketExecutionReport.getState() != ExecutionReportState.FILLED) {
				LOGGER.info("[MktMsg] Discarded ExecutionReport ({}) for orderID {} : {}", marketExecutionReport.getState(), operation.getOrder().getFixOrderId(), marketExecutionReport);
				return;
			}
	
			if (!FillManagerService.alreadySavedFill(marketExecutionReport, order))
			{   
				if(operation.isNewFill(currentAttempt, marketExecutionReport)) {
					operation.getMarketExecutionListener().onMarketExecutionReport(operation, source, order, marketExecutionReport);
				}
			}
		}
	}

	@Override
	public void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order,
			String reason, MarketExecutionReport marketExecutionReport) {
		operation.getMarketExecutionListener().onMarketOrderCancelNoFill(operation, source, order, marketExecutionReport);
	}

	@Override
	public void onOperatorRevokeAccepted(OperatorConsoleConnection source, String comment) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onOperatorRevokeAccepted(source, comment);
		else {
			LOGGER.info("Sending revoke accepted to customer");
			operation.setStateResilient(new OrderRevocatedState(comment), ErrorState.class);
		}
	}

	@Override
	public void onOperatorRevokeRejected(OperatorConsoleConnection source, String comment) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onOperatorRevokeRejected(source, comment);
		else {
			Order order = operation.getOrder();
			try {
				customerConnection.sendRevokeReport(operation, order, RevocationState.MANUAL_REJECTED, comment);
				operation.setRevocationState(RevocationState.MANUAL_REJECTED);
				//[RR20120613] CSBESTXPOC-131 : Must reset the flag if a revoke has been rejected
				operation.setCustomerRevokeReceived(false);
			} catch (BestXException e) {
				LOGGER.error("Error while sending Revoke Reject", e);
			}
			operatorConsoleConnection.updateRevocationStateChange(operation, operation.getRevocationState(), comment);
		}
	}

	@Override
	public void onStateRestore(OperationState currentState) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onStateRestore(currentState);
		else {
			LOGGER.info("Default restore operation");
			if (!currentState.isTerminal()) {
				//				&& (DateService.newLocalDate().getTime() - operation.getOrder().getTransactTime().getTime()) < 43200000) {
				operation.setStateResilient(new WarningState(currentState, null, Messages.getString("DEFAULT_RESTORE_ORDER_STATE")), ErrorState.class);
				//		}
			}
		}
	}

	@Override
	public void onUnexpectedMarketProposal(MarketBuySideConnection source, Instrument instrument,
			ClassifiedProposal proposal) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onUnexpectedMarketProposal(source, instrument, proposal);
		else {
			if (source.getMarketCode() != MarketCode.BLOOMBERG) {
				LOGGER.info("Ignoring Unexpected proposal from {}, QuoteID = {}", source.getMarketCode(), proposal.getSenderQuoteId());
				return;
			}
	
			try {
				LOGGER.info("[MktReq] Rejecting Unexpected proposal from {}, QuoteID = {}", source.getMarketCode(), proposal.getSenderQuoteId());
				source.rejectProposal(operation, instrument, proposal);
			} catch (BestXException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onMarketProposal(source, instrument, proposal);
		else {
			// ignore it
			LOGGER.info("Received counter:{}; ignoring it.", proposal);
			// arriving here means that we received a counter in a wrong state, but
			// this counter could have locked the isProcessingCounter flag of the
			// current operation. We must free it to prevent the loss of future
			// counters.
			if (operation.isProcessingCounter()) {
				operation.setProcessingCounter(false);
			}
		}
	}

	@Override
	public void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId,
			Proposal.ProposalType proposalStatus) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onMarketProposalStatusChange(source, quoteId, proposalStatus);
		else {
			LOGGER.info("Order {}, received proposal status change to {} for quoteID {}, ignoring it.", operation.getOrder().getFixOrderId(), proposalStatus, quoteId);
		}
	}

	@Override
	public void onFixRevoke(CustomerConnection source) {
		if(this.customerSpecificHandler != null) {
			this.customerSpecificHandler.onFixRevoke(source);
		} else {
			if (customerConnection == null) {
				LOGGER.error("Revoke received but no Customer Connection available");
			} else {
				Order order = operation.getOrder();
				if (operation.getState().isRevocable()) {
					// stop default timer, if any
					stopDefaultTimer();
					String comment = Messages.getString("REVOKE_ACKNOWLEDGED");
					updateOperationToRevocated(comment);
					LOGGER.info("Order {}: revoke received, it will be managed automatically. Sending automatic revoke accepted to customer", order.getFixOrderId());
					operation.setStateResilient(new OrderRevocatedState(comment), ErrorState.class);
				} 
				else {
					try {
						LOGGER.info("Revoke rejected, order in a not revocable state.");	        	   
						customerConnection.sendRevokeNack(operation, order, Messages.getString("REVOKE_NOT_ACKNOWLEDGED"));
						operation.setRevocationState(RevocationState.NOT_ACKNOWLEDGED);
						operation.setCustomerRevokeReceived(false);
					} catch (BestXException e) {
						LOGGER.error("Error while sending Revoke Nack", e);
					}	        	
					operatorConsoleConnection.updateRevocationStateChange(operation, operation.getRevocationState(), Messages.getString("REVOKE_NOT_ACKNOWLEDGED"));
				}
			}
		}
	}
	@Override
	public String putInVisibleState() {
		if(this.customerSpecificHandler != null)
			return this.customerSpecificHandler.putInVisibleState();
		else {
			if(operation.getState().isTerminal()) {
				LOGGER.info("Asked to put in Technical Error state order {} when it was in state {}. Ignoring request", operation.getOrder() != null ? operation.getOrder().getFixOrderId() : "with no FIX OrderID", operation.getState().getClass());
				return Messages.getString("PUT_IN_VISIBLE_STATE.4");
			}
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("PUT_IN_VISIBLE_STATE.0")), ErrorState.class);
			return Messages.getString("PUT_IN_VISIBLE_STATE.3");
		}
	}

	@Override
	public void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onOperatorAcceptOrder(source, comment);
		else {
			// AMC 20100908 ticket #4243. This blank method prevents application error when double acceptance on a rejectable order is called.
			LOGGER.debug("onOperatorAcceptOrder called while in {}. Ignoring it...", operation.getState().toString());
		}	
	}
	/**
	 * @param executionReport the execution report involved
	 * @param quantity the quantity (in tick or in amount)
	 * @param comm the commission
	 */
	protected void setExecutionReportAmountCommission(ExecutionReport executionReport,
			Money quantity, Commission comm) throws BestXException {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.setExecutionReportAmountCommission(executionReport, quantity, comm);
		else {
			LOGGER.debug("Setting exec rep amout commission as per I-40 ...");
			if(comm != null) {
				if(comm.getCommissionType().equals(Commission.CommissionType.TICKER))
				{
					executionReport.setAmountCommission(commissionService.calculateCommissionAmount(quantity, comm));
				}
				else
				{
					executionReport.setAmountCommission(comm.getAmount());
				}
			}
			else
			{
				executionReport.setAmountCommission(BigDecimal.ZERO);
			}
			LOGGER.debug("Setting exec rep amout commission as per I-40 DONE. Amount commission {}", executionReport.getAmountCommission().toString());
		}
	}

	/**
	 * With this method we check if we have received a customer revoke. In this case we have to manage it
	 * because it is more important than the other situations.
	 * We must also return a boolean value, helping the caller to understand if we could have managed the
	 * revoke or not. 
	 * @param order : the order that must be revoked
	 * @return true if the revoke has been managed, false if we couldn't.
	 */
	protected boolean checkCustomerRevoke(Order order) {
		if(this.customerSpecificHandler != null)
			return this.customerSpecificHandler.checkCustomerRevoke(order);
		else {
			LOGGER.debug("Checking if we have already received a customer revoke for this operation.");
	
			//20101201 Ruggero : I-41 Revoche Automatiche
			//If there is a revoke, then we must work on it
			boolean revoked = false;
			if (operation.isCustomerRevokeReceived())
			{
				if (operation.getState().isRevocable()) 
				{
					LOGGER.debug("Revoke received and the order is in a revocable state, proceed with the revoking process.");
	
					try 
					{
						customerConnection.sendRevokeAck(operation, order.getFixOrderId(), Messages.getString("AutomaticRevokeDefaultMessage.0"));
						operation.setRevocationState(RevocationState.ACKNOWLEDGED);
					} catch (BestXException e) {
						LOGGER.error("Error while sending Revoke Ack", e);
					}
					LOGGER.debug("Starting the automatic revocation.");
					revoked = true;
					operation.setStateResilient(new OrderRevocatedState(Messages.getString("AutomaticRevokeDefaultMessage.0")), ErrorState.class);
				}
				else
				{
					LOGGER.debug("Revoke received, but the order in a state that does not allow revocations, nothing to do, resume normal processing.");
				}
			}
			return revoked;
		}
	}
	
	/**
	 * Sometimes we receive an ack when we cannot manage it and there is no need to
	 * manage it.
	 * By example : while the trader manages an order sent to the internal market that
	 * has been rejected, we could receive the ack from the CMF, it happens that it sends
	 * it later. IN this situation we must only log the reception of the Ack.
	 *
	 * @param source the source
	 */
	@Override
	public void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onTradingConsoleTradeAcknowledged(source);
		else {
			LOGGER.warn("Trade ack received, cannot manage it in this state, source : {}", source);
		}
	}

	/**
	 * This method takes care of a special situation happened, until now, only
	 * with a XBridge RFQ :
	 * - with 2 seconds left before the RFQ expiry the trader presses the "Extend" key
	 * - the RFQ expires in the meantime, starting the XBridge Reject mechanism
	 * - we received the time extension message, but we are no more in a XBridge state,
	 * so the control arrives here, we have only to ignore this extension because
	 * on the trader side the RFQ has expired and in BestX! we are already in a new
	 * price request phase.
	 * 
	 * Method called only from the XBridge market
	 *
	 * @param timerExpirationDate the timer expiration date
	 */
	@Override
	public void onRfqLifetimeUpdate(Date timerExpirationDate) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onRfqLifetimeUpdate(timerExpirationDate);
		else {
			LOGGER.warn("Order " + operation.getOrder().getFixOrderId() + ", time extension message for a RFQ received, but the RFQ has already expired, ignore it.");
		}
	}

	@Override
	public void onOperatorMoveToLimitFileNoPrice(OperatorConsoleConnection source, String comment) {
		if(this.customerSpecificHandler != null)
			this.customerSpecificHandler.onOperatorMoveToLimitFileNoPrice(source, comment);
		else {
			LOGGER.info("Moving order {} to Limit File No Price state.", comment);
			operation.setStateResilient(new LimitFileNoPriceState(Messages.getString("LimitFile.OrderSentToLFNPByOperator")), ErrorState.class);
		}
	}

   @Override
   public void onOperatorTakeOwnership(OperatorConsoleConnection source, UserModel userToAssign) {
      if (!operation.getState().isTerminal()) {
         operation.setOwner(userToAssign);
      }
   }
	
	/**
	 * @param customer
	 * @return
	 */
	protected Set<Venue> selectVenuesForPriceDiscovery( Customer customer) {
		Set<Venue> venues = null;
       if (customer.getPolicy() != null) {
           venues = new HashSet<Venue>();
           for (Venue venue : customer.getPolicy().getVenues()) {
               if (venue.getVenueType().equals(VenueType.MARKET) || venue.getMarketMaker() != null && venue.getMarketMaker().isEnabled()) {
                   venues.add(venue);
               } else {
                   LOGGER.debug("Skipped Venue: {}", venue);
               }
           }
       } else {
           return null;
       }
       LOGGER.debug("Retrieved venues: {}", venues);
       LOGGER.info("[COUNTER_PRICE_REQ] operationID={}, Order={}", operation.getId(), operation.getOrder().getFixOrderId());
		return venues;
	}
	

    protected void checkOrderAndsetNotAutoExecuteOrder(Operation operation, boolean doNotExecute) {
        Order order = operation.getOrder();
        if (order.isLimitFile() && doNotExecute) {
        	setNotAutoExecuteOrder(operation);
        }
    }
    
    protected void setNotAutoExecuteOrder(Operation operation) {
		operation.setNotAutoExecute(true);
    }


    /**
     * @param reason the text sent in the cancel message to the client
     */
    protected void updateOperationToRevocated(String reason) {
    	operation.setRevocationState(RevocationState.ACKNOWLEDGED);
    	operation.getOrder().setText(reason);
    	operation.setCustomerRevokeReceived(true);
    	if(operatorConsoleConnection != null)
    		operatorConsoleConnection.updateRevocationStateChange(operation, operation.getRevocationState(), reason);
    }

	
}
