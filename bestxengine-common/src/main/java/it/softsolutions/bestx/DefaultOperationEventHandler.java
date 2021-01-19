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
package it.softsolutions.bestx;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 *
 * Purpose: this is the handler that exposes default methods implementations for the management of
 * the various states
 *
 * Project Name : bestxengine-common
 * First created by: ruggero.rizzo
 * Creation date: 08/ott/2012
 *
 **/
import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.model.UserModel;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService.Result;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.price.QuoteResult;
import it.softsolutions.jsscommon.Money;

/**
 * The Class DefaultOperationEventHandler.
 */
public class DefaultOperationEventHandler implements OperationEventListener {
	private static final long serialVersionUID = -9010046329525779408L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOperationEventHandler.class);
	protected Operation operation;

	/**
	 * Instantiates a new default operation event handler.
	 * 
	 * @param operation
	 *            the operation
	 */
	public DefaultOperationEventHandler(Operation operation) {
		this.operation = operation;
	}

	private void defaultAction() {
		defaultAction("");
	}

	// 20131212 PM disabled use of StateDescriptionService
	// if state is not terminal --> exception --> WarningState
	// is state is terminal only log(warn)
	private void defaultAction(final String reason) {
		// Stefano 06/06/2008 - Final state cannot go to warning state
        String stateName = operation.getState().getClass().getSimpleName();
        String msg = "Operation: " + operation.getOrder().getFixOrderId() +" - unexpected event received in state " + stateName;
        
        if (!operation.getState().isTerminal()) {
			//String stateDescription = StateDescriptionService.getStateDescription(stateName);
			//String msg = Messages.getString("EventNotHandledInState", stateDescription);
			if (reason != null && !reason.isEmpty()) {
				msg += " [ " + reason + " ]";
			}
			LOGGER.warn(msg);   
		}
        else {
            LOGGER.warn(msg);            
        }
	}

	@Override
	public void onNewState(OperationState currentState) {
		LOGGER.debug("Operation: {} - Default action for operation state set to: {}", operation.getOrder().getFixOrderId(), currentState);
	}

	@Override
	public void onStateRestore(OperationState currentState) {
		LOGGER.debug("Operation: {} - Default action for operation state re-set to: {}", operation.getOrder().getFixOrderId(), currentState);
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		if (!operation.getState().isTerminal())
			throw new UnsupportedOperationException("Operation: '" + operation + "' - Timer Expired event not handled in state: " + operation.getState().getClass().getSimpleName() + " Timer ID: "
			        + jobName + '-' + groupName);
	}

	@Override
	public void onCustomerOrder(CustomerConnection source, Order order) {
		defaultAction();
	}

	@Override
	public void onCustomerRfq(CustomerConnection source, Rfq rfq) {
		defaultAction();
	}

	@Override
	public void onCustomerQuoteAcknowledged(CustomerConnection source, Quote quote) {
		defaultAction();
	}

	@Override
	public void onCustomerQuoteNotAcknowledged(CustomerConnection source, Quote quote, Integer errorCode, String errorMessage) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleOrderAutoExecution(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleOrderPendingAccepted(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleOrderPendingCounter(TradingConsoleConnection source, ClassifiedProposal counter) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleOrderPendingExpired(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleOrderPendingRejected(TradingConsoleConnection source, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleTradeNotAcknowledged(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleTradeReceived(TradingConsoleConnection source) {
		defaultAction();
	}

	@Override
	public void onTradingConsoleTradeRejected(TradingConsoleConnection source, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport) {
		LOGGER.info("Received ACK for execution report : {}", executionReport);
	}

	@Override
	public void onCustomerExecutionReportNotAcknowledged(CustomerConnection source, ExecutionReport executionReport, Integer errorCode, String errorMessage) {
		LOGGER.warn("Received NACK for execution report : {} error code : {} error message : {}", executionReport, errorCode, errorMessage);
	}

	@Override
	public void onOperatorAbortState(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorForceState(OperatorConsoleConnection source, OperationState newState, String comment) {
		LOGGER.info("Operator Force State requested in an incorrect state. Connection source : {}, state requested : {}, comment {}", (source != null ? source.getConnectionName() : null),
		        (newState != null ? newState.getClass().getSimpleName() : newState), comment);
	}

	@Override
	public void onOperatorReinitiateProcess(OperatorConsoleConnection source, String comment) {
		LOGGER.info("Operator Reinitiate Process requested in an incorrect state. Connection source : {}, comment {}", (source != null ? source.getConnectionName() : null), comment);
	}

	@Override
	public void onOperatorRestoreState(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
		LOGGER.info("Operator Retry State requested in an incorrect state. Connection source : {}, comment {}", (source != null ? source.getConnectionName() : null), comment);
	}

	@Override
	public void onOperatorSuspendState(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorTerminateState(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		LOGGER.info("Price result received in an incorrect state. Connection source : {}, comment {}", (source != null ? source.getPriceServiceName() : null),
		        (priceResult != null ? priceResult.getState() : null));
	}

	@Override
	public void onQuoteResult(PriceService source, QuoteResult quoteResult) {
		defaultAction();
	}

	@Override
	public void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
		defaultAction();
	}

	@Override
	public void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {
		LOGGER.info("Market order reject received in an incorrect state. Connection source : {}, orderId: {}, reason : {}, sessionId {}", (source != null ? source.getMarketCode() : null),
		        (order != null ? order.getFixOrderId() : null), reason, sessionId);
	}

	@Override
	public void onMarketOrderStatus(MarketBuySideConnection source, Order order, Order.OrderStatus orderStatus) {
		LOGGER.info("Market order status received in an incorrect state. Connection source : {}, orderId: {}, status : {}", (source != null ? source.getMarketCode() : null),
		        (order != null ? order.getFixOrderId() : null), orderStatus);
	}

	@Override
	public void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {
		LOGGER.info("Market proposal received in an incorrect state. Connection source : {}, instrument: {}, proposal : {}", (source != null ? source.getMarketCode() : null), instrument, proposal);
	}

	@Override
	public void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId, Proposal.ProposalType proposalStatus) {
		LOGGER.info("Market proposal status change received in an incorrect state. Connection source : {}, quoteID: {}, proposal status: {}", (source != null ? source.getMarketCode() : null),
		        quoteId, proposalStatus);
	}

	@Override
	public void onMarketRfqReject(MarketBuySideConnection source, Rfq rfq, String reason) {
		LOGGER.info("Market Rfq reject received in an incorrect state. Connection source : {}, reason : {}, rfq : {}", (source != null ? source.getMarketCode() : null), reason, rfq);
	}

	@Override
	public void onOperatorExecuteState(OperatorConsoleConnection source, Money execPrice, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorSendDDECommand(OperatorConsoleConnection source, String comment) {
		LOGGER.info("Sending of DDE command requested in an incorrect state. Connection source : {}, comment {}", (source != null ? source.getConnectionName() : null), comment);
	}

	@Override
	public void onMarketMatchFound(MarketBuySideConnection source, Operation matching) {
		defaultAction();
	}

	@Override
	public void onMarketRevocationAccepted(MarketBuySideConnection source) {
		defaultAction();
	}

	@Override
	public void onMarketRevocationRejected(MarketBuySideConnection source, String reason) {
		LOGGER.info("Market revocation rejected received in an incorrect state. Connection source : {}, reason : {}", (source != null ? source.getMarketCode() : null), reason);
	}

	@Override
	public void onOperatorMatchOrders(OperatorConsoleConnection source, Money ownPrice, Money matchingPrice, String comment) {
		defaultAction();
	}

	@Override
	public void onTradeFeedDataRetrieved(MarketBuySideConnection source, String ticketNum, int numberOfDaysAccrued, BigDecimal accruedInterestAmount) {
		LOGGER.info("TradeFeed data retrieved in an incorrect state. Connection source : {}, ticketNum : {}, days accrued : {}, accrued interest amount : {}", (source != null ? source.getMarketCode()
		        : null), ticketNum, numberOfDaysAccrued, accruedInterestAmount);
	}

	@Override
	public void onApplicationError(OperationState currentState, Exception exception, String message) {
		LOGGER.warn("Application error: {}", message, exception);

		defaultAction();
	}

	@Override
	public void onMarketOrderCancelled(MarketBuySideConnection source, Order order, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onOperatorForceReceived(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorManualExecution(OperatorConsoleConnection source, String comment, Money price, BigDecimal qty, String mercato, String marketMaker, Money prezzoRif) {
		defaultAction();
	}

	@Override
	public void onOperatorForceExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorForceNotExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorResendExecutionReport(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorStopOrderExecution(OperatorConsoleConnection source, String comment, Money price) {
		defaultAction();
	}

	@Override
	public void onFixRevoke(CustomerConnection source) {
		defaultAction();
	}

	@Override
	public void onOperatorRevokeAccepted(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorRevokeRejected(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorExceedFilterRecalculate(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onMarketOrderCancelFillAndBook(MarketBuySideConnection source, Order order, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onMarketOrderCancelFillNoBook(MarketBuySideConnection source, Order order, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onMarketOrderCancelRequestReject(MarketBuySideConnection source, Order order, String reason) {
		LOGGER.info("Market order cancel request reject received in an incorrect state. Connection source : {}, orderId: {}, reason : {}", (source != null ? source.getMarketCode() : null),
		        (order != null ? order.getFixOrderId() : null), reason);
	}

	@Override
	public void onOperatorMoveToNotExecutable(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorExecutedOperationDelete(OperatorConsoleConnection source, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorExecutedOperationModify(OperatorConsoleConnection source, BigDecimal priceAmount, BigDecimal qty, String date, String comment) {
		defaultAction();
	}

	@Override
	public void onOperatorSendDESCommand(OperatorConsoleConnection source, String comment) {
		LOGGER.info("Sending of DES command requested in an incorrect state. Connection source : {}, comment {}", (source != null ? source.getConnectionName() : null), comment);
	}

	@Override
	public void onMarketOrderTechnicalReject(MarketBuySideConnection source, Order order, String reason) {
		defaultAction(reason);
	}

	@Override
	public void onCmfErrorReply(TradingConsoleConnection source, String errorMessage, int errorCode) {
		defaultAction();
	}

	@Override
	public void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason, MarketExecutionReport marketExecutionReport) {
		defaultAction(reason);
	}

	@Override
	public void onMarketPriceReceived(String orderId, BigDecimal marketPrice, Order order, MarketExecutionReport marketExecutionReport) {
		LOGGER.info("Market order status received in an incorrect state. orderId: {}, price : {}, MarketExecutionReport : {}", orderId, marketPrice, marketExecutionReport);
	}

	@Override
	public void onMarketResponseReceived() {
		defaultAction();
	}

    @Override
    public void onQuoteStatusTimeout(MarketBuySideConnection source, Order order, String sessionId) {
        defaultAction();
    }

    @Override
    public void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text) {
        defaultAction();
    }
	
	public String putInVisibleState() {
		return "Impossible to change status: operation not allowed";
	}

	@Override
	public void onRfqLifetimeUpdate(Date timerExpirationDate) {
		defaultAction();
	}

	@Override
	public void onCustomServiceResponse(boolean error, String securityId) {
		defaultAction();
	}

	@Override
	public boolean isEventFromOtherMarketAcceptable(MarketCode marketCode) {
		return false;
	}

    @Override
    public void onUnexpectedMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {
        LOGGER.info("Unexpected Market proposal received in an incorrect state. Connection source : {}, instrument: {}, proposal : {}", (source != null ? source.getMarketCode() : null), instrument, proposal);
    }

    @Deprecated
    @Override
    public void onUnexecutionResult(Result result, String message) {
        defaultAction();
    }

    @Deprecated
    @Override
    public void onUnexecutionDefault(String executionMarket) {
        defaultAction();
    }
    
    @Override
    public void onOperatorMoveToLimitFileNoPrice(OperatorConsoleConnection source, String comment) {
        defaultAction();
    }

	@Override
    public void startTimer() {
		LOGGER.info("startTimer request received for Operation {}", operation.getId());
		defaultAction();
    }

	@Override
	public void onOperatorPriceDiscovery(OperatorConsoleConnection source) {
		LOGGER.info("Price Discovery request received for Operation {}", operation.getId());
		defaultAction();
	}

   @Override
   public void onOperatorTakeOwnership(OperatorConsoleConnection source, UserModel userToAssign) {
      defaultAction();
   }
   
   @Override
   public void onRevoke() {
      defaultAction();
   }
    
}
