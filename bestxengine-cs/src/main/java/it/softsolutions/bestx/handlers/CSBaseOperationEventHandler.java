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

package it.softsolutions.bestx.handlers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.markets.bloomberg.BloombergMarket;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.CSOrdersEndOfDayService;
import it.softsolutions.bestx.services.CommissionService;
import it.softsolutions.bestx.services.FillManagerService;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.OrderRevocatedState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class CSBaseOperationEventHandler extends BaseOperationEventHandler {

	private static final long serialVersionUID = -7806892014701676287L;

	protected static final BigDecimal ZERO = new BigDecimal("0.0");
	private static Logger LOGGER = LoggerFactory.getLogger(CSBaseOperationEventHandler.class);
	/**
	 * Instantiates a new cS base operation event handler.
	 *
	 * @param operation the operation
	 * @param commissionService the commission service
	 */
	public CSBaseOperationEventHandler(Operation operation, CommissionService commissionService) {
		super(operation);
	}

	/**
	 * Instantiates a new cS base operation event handler.
	 *
	 * @param operation the operation
	 */
	public CSBaseOperationEventHandler(Operation operation) {
		super(operation);
	}

	@Override
	public void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId,
			Proposal.ProposalType proposalStatus) {
		super.onMarketProposalStatusChange(source, quoteId, proposalStatus);
		if(source.getClass() == BloombergMarket.class)
		{
			if (operation.isProcessingCounter()) {
				operation.setProcessingCounter(false);
			}
		}
	}
	
	@Override
	public void onFixRevoke(CustomerConnection source) {
		if (source == null) {
			LOGGER.error("Revoke received but no Customer Connection available");
		} else {
			Order order = operation.getOrder();
			if (operation.getState().isRevocable()) {
				// stop default timer, if any
				stopDefaultTimer();

				String comment = Messages.getString("REVOKE_ACKNOWLEDGED");
				LOGGER.info("Order {}: revoke received, it will be managed automatically. Sending automatic revoke accepted to customer", order.getFixOrderId());

				// add limit file prefix, if necessary
				switch (operation.getState().getType()) {
				case LimitFileNoPrice:
					//send back to OTEX their original comment or the empty prefix
					comment = order.getText();
					comment = LimitFileHelper.getInstance().getCommentWithPrefix(comment, true);
					break;
				case OrderNotExecutable:
				case CurandoAuto:
				case CurandoRetry:
					//send back to OTEX their original comment or the empty prefix
					comment = order.getText();
					comment = LimitFileHelper.getInstance().getCommentWithPrefix(comment, false);
					break;
				default:
					break;
				}
				updateOperationToRevocated(comment);
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

	// AMC 20151014 management of execution report received when in a state not expecting adding it to the attempt history and going to warning state
	@Override
	public void onMarketExecutionReport(MarketBuySideConnection source,
			Order order, MarketExecutionReport marketExecutionReport) {

		Attempt currentAttempt = operation.getLastAttempt();
		if (currentAttempt == null) {
			LOGGER.error("No current Attempt found");
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("BaseAttemptNotFoundError.0")), ErrorState.class);
			return;
		}
		List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
//		if (marketExecutionReports == null) {
//			marketExecutionReports = new ArrayList<MarketExecutionReport>();
//			currentAttempt.setMarketExecutionReports(marketExecutionReports);
//		}
//		// if this is the first time the execution report is received
//		boolean isNewFill = true;
//		for(Attempt attempt: operation.getAttempts()) {
//			if(!operation.isNewFill(currentAttempt, marketExecutionReport)) {
//				isNewFill = false;
//				break;
//			}
//		}
//		if(isNewFill) {
		if(FillManagerService.alreadySavedFill(marketExecutionReport, operation.getOrder())){
			marketExecutionReports.add(marketExecutionReport);
			// Create Customer Execution Report from Market Execution Report
			ExecutionReport executionReport;
			try {
				executionReport = marketExecutionReport.clone();
			}
			catch (CloneNotSupportedException e1) {
				LOGGER.error("Error while trying to create Execution Report from Market Execution Report");
				operation.setStateResilient(new WarningState(operation.getState(), e1, Messages.getString("BaseCreateExecutionReportError.0")), ErrorState.class);
				return;
			}
			long executionReportId = SerialNumberServiceProvider.getSerialNumberService().getSerialNumber("EXEC_REP");
			executionReport.setLastPx(executionReport.getPrice().getAmount());
			marketExecutionReport.setLastPx(executionReport.getPrice().getAmount());
			executionReport.setSequenceId(Long.toString(executionReportId));
			// if marketExecutionReport has a broker inside use it, else use the one in lastAttempt execution proposal
	        if(marketExecutionReport.getMarketMaker() != null) {
	        	executionReport.setExecBroker(marketExecutionReport.getMarketMaker().getCode());
	        	executionReport.setCounterPart(marketExecutionReport.getMarketMaker().getCode());
	        } else if(currentAttempt.getExecutionProposal() != null && currentAttempt.getExecutionProposal().getMarketMarketMaker() != null) {
				marketExecutionReport.setExecBroker(operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());              
				executionReport.setExecBroker(marketExecutionReport.getExecBroker());
				executionReport.setCounterPart(marketExecutionReport.getExecBroker());
	        } else {
	        	LOGGER.error("Error while trying to generate Execution Report ID: unable to determine the execution broker");
				operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("BaseMarketSendOrderError.0")), ErrorState.class);
				return;
	        }
			executionReport.setMarketOrderID(marketExecutionReport.getMarketOrderID());

			LOGGER.info("{} market - new exec report added : {}", marketExecutionReport.getMarket().getMarketCode() , executionReport.toString());
			operation.getExecutionReports().add(executionReport);
				operation.setStateResilient(new WarningState(operation.getState(), null, "Received an execution report from market " + marketExecutionReport.getMarket().getMarketCode() +", price = " + marketExecutionReport.getLastPx() + ", MM: " + marketExecutionReport.getExecBroker() + "  while not expecting"), ErrorState.class);
		}
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		Order order = operation.getOrder();
		if (jobName.equals(CSOrdersEndOfDayService.ORDERS_END_OF_DAY_ID) && !order.isLimitFile()) {
			addNotExecutionReportToOperation(ExecutionReportState.EXPIRED);
			operation.setStateResilient(new SendNotExecutionReportState(operation.getState().getComment()), ErrorState.class);		
		}
		else if(order.isLimitFile() && (jobName.equals(CSOrdersEndOfDayService.LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID) 
				|| jobName.equals(CSOrdersEndOfDayService.LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID))) {
			String comment = order.getText();
			//call the SendNotExecutionReportState with the comment of the actual state: it contains the expiration message together with
			//the eventual prefix for Limit File orders (subclasses onTimerExpired method will be called first)
			comment = LimitFileHelper.getInstance().getCommentWithPrefix(comment, false);
			order.setText(comment);
			operation.getState().setComment(comment);
			addNotExecutionReportToOperation(ExecutionReportState.REJECTED);
			operation.setStateResilient(new SendNotExecutionReportState(operation.getState().getComment()), ErrorState.class);
		} else {
			LOGGER.info("Unexpected timerExpired, skip processing [{}.{}]", groupName, jobName);
		}
	}

	/**
	 * 
	 */
	protected void addNotExecutionReportToOperation(ExecutionReportState state) {
		SerialNumberService serialNumberService = SerialNumberServiceProvider.getSerialNumberService();
		ExecutionReport newExecution;
		newExecution = ExecutionReportHelper.createExecutionReport(operation.getOrder().getInstrument(), operation.getOrder().getSide(),
				serialNumberService);

		// [RR20140910] CRSBXTEM-120: orders NOT limit file generate an EXPIRY, while limit file or limit file no price orders generate a REJECT
		newExecution.setState(state);
		List<ExecutionReport> executionReports = operation.getExecutionReports();
		if (executionReports == null) {
			executionReports = new ArrayList<ExecutionReport>(); 
		}
		executionReports.add(newExecution);
		operation.setExecutionReports(executionReports);
	}
}
