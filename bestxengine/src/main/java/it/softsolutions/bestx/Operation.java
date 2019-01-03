/*
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationValidationException;
import it.softsolutions.bestx.exceptions.SaveBookException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.InternalAttempt;
import it.softsolutions.bestx.model.Market;
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
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.bestx.services.price.QuoteResult;
import it.softsolutions.jsscommon.Money;

/**
 * This is a central class in the state machine of BestX Engine. It implements the {@link OperationEventListener} interface, thus being able
 * to react to all external events. State change is implemented in this class. State of Operation is represented by:
 * <ul>
 * <li>OperationState</li>
 * <li>Operation data, that include identifiers and all business data</li>
 * </ul>
 * OperationState is very important in the way Operation reacts to external events, since it determines which Operation event handler
 * actually performs event processing.
 * 
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public final class Operation implements OperationEventListener {

	private static final long serialVersionUID = -7979212191598523385L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Operation.class);

	private Long id; // Used only for persistence
	private List<OperationStateListener> stateListeners;

	private MarketExecutionListener marketExecutionListener;
	/**
	 * Current Operation state
	 */
	private OperationState currentState;
	/**
	 * State machine strategy
	 */
	private Strategy strategy;
	/**
	 * Current external event handler
	 */
	private OperationEventListener handler;
	/**
	 * String identifiers associated to this Operation
	 */
	private final Map<OperationIdType, String> identifiers = new ConcurrentHashMap<OperationIdType, String>();

	/**
	 * RFQ received from customer
	 */
	private Rfq rfq;
	/**
	 * Quote to be sent to customer
	 */
	private Quote quote;
	/**
	 * Order received from customer
	 */
	private Order order;
	/**
	 * List of attempts to close orders by this Operation
	 */
	private List<Attempt> attempts = new CopyOnWriteArrayList<Attempt>();

	/** Needed to avoid trying to save multple times tha same attempt **/
	public int lastSavedAttempt = 0;

	/**
	 * Internal attempt (to be executed in parallel to others)
	 */
	//private Attempt internalAttempt = null;

	/**
	 * Gets the last internal attempt, if active.
	 *
	 * @return the internal attempt
	 */
	public InternalAttempt getInternalAttempt() {
		InternalAttempt a = getLastAttempt().getInternalAttempt();
		if(a != null && a.isActive())
			return a;
		return null;
	}

	/**
	 * List of execution reports to be sent to customers
	 */
	private List<ExecutionReport> executionReports = new ArrayList<ExecutionReport>();

	/**
	 * List of timers pending for this Operation
	 */
	private Operation matchingOperation;

	private int attemptNo = -1;
	private int firstValidAttemptNo = 0;
	private int firstAttemptInCurrentCycle = 0;

	private boolean hasBeenInternalized = false;
	private boolean noProposalsOrderOnBook = false;

	private boolean magnetCustomerRevoke = false;
	private AtomicBoolean cancelOrderWaiting = new AtomicBoolean(false);
	private String furtherDetails = "";

	private AtomicBoolean stopped = new AtomicBoolean(false);

	// 20101201 Ruggero : I-41 Revoche Automatiche
	public boolean customerRevokeReceived = false;

	//BESTX-286
	private UserModel owner = null;

	/**
	 * Check if the operation has beeen internalized
	 * 
	 * @return true or false
	 */
	public boolean hasBeenInternalized() {
		return hasBeenInternalized;
	}

	/**
	 * Set the flag of the operation internalization
	 * 
	 * @param value
	 *            : true or false
	 */
	public void setInternalized(boolean value) {
		hasBeenInternalized = value;
	}

	/**
	 * State of revocation
	 */
	private RevocationState revocationState;

	public static enum RevocationState {
		NONE, NOT_ACKNOWLEDGED, ACKNOWLEDGED, MANUAL_REJECTED, MANUAL_ACCEPTED, AUTOMATIC_REJECTED
	}

	private AtomicBoolean isProcessingCounter = new AtomicBoolean();

	private boolean isVolatile = false;


	public void setVolatile(boolean vol) {
		isVolatile = vol;
	}

	/**
	 * Factory for Operations
	 * 
	 * @author lsgro
	 * 
	 */
	public static class DefaultOperationFactory implements OperationFactory {
		private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOperationFactory.class);
		private Strategy defaultStrategy;
		private Class<? extends OperationState> initialStateClass;
		private List<OperationStateListener> operationStateListeners;
		private MarketExecutionListener marketExecutionListener;

		@SuppressWarnings("unchecked")
		/**
		 * Set the initial state class name
		 * @param initialStateClassName : initial class name
		 */
		public void setInitialStateClassName(String initialStateClassName) {
			try {
				initialStateClass = (Class<? extends OperationState>) Class.forName(initialStateClassName);
			} catch (ClassNotFoundException e) {
				throw new ObjectNotInitializedException("Initial operation state class not found: " + e.getMessage());
			}
		}

		/**
		 * Set the default handlers selection strategy
		 * 
		 * @param strategy
		 *            : the strategy
		 */
		public void setDefaultStrategy(Strategy strategy) {
			defaultStrategy = strategy;
		}

		/**
		 * Set the list of operation state listeners
		 * 
		 * @param operationStateListeners
		 *            : listeners list
		 */
		public void setOperationStateListeners(List<OperationStateListener> operationStateListeners) {
			this.operationStateListeners = operationStateListeners;
		}

		/**
		 * Set the market execution listener
		 * 
		 * @param marketExecutionListener
		 *            : market execution listener
		 */
		public void setMarketExecutionListener(MarketExecutionListener marketExecutionListener) {
			this.marketExecutionListener = marketExecutionListener;
		}

		/**
		 * Create a new operation object
		 * 
		 * @return the operation
		 */
		@Override
		public Operation createNewOperation(boolean isVolatile) throws BestXException {
			LOGGER.debug("Create new operation with default values");
			OperationState initialState;
			try {
				initialState = initialStateClass.newInstance();
			} catch (InstantiationException e) {
				throw new BestXException("An error occurred while instantiating OperationState: " + initialStateClass.getName() + " : " + e.getMessage(), e);
			} catch (IllegalAccessException e) {
				throw new BestXException("An error occurred while instantiating OperationState: " + initialStateClass.getName() + " : " + e.getMessage(), e);
			}

			Operation operation = new Operation();
			operation.currentState = initialState;
			operation.setProcessingCounter(false);
			operation.setVolatile(isVolatile);
			operation.setOwner(null);

			initialState.setOperation(operation, new Date());

			initOperation(operation);

			operation.updateStateListeners(null, initialState);
			return operation;
		}

		/**
		 * Initialize the operation with values from another operation
		 * 
		 * @param operation
		 *            : the source operation
		 */
		@Override
		public void initOperation(Operation operation) throws BestXException {
			operation.strategy = defaultStrategy;
			operation.stateListeners = operationStateListeners;
			operation.marketExecutionListener = marketExecutionListener;
			operation.updateHandler();
		}
	}

	/**
	 * Used internally from BestX framework to set a new operation state. Action performed:
	 * <ol>
	 * <li>State validation</li>
	 * <li>Handler update (only if strategy != null)</li>
	 * <li>Listeners update</li>
	 * <li>Handler onNewState method</li>
	 * </ol>
	 * 
	 * @param newState
	 *            : new state
	 * @throws BestXException
	 *             : if errors happen
	 */
	public synchronized void setState(OperationState newState) throws BestXException {
		LOGGER.debug("Set operation state to: {}", newState.getClass().getName());
		OperationState oldState = currentState;
		newState.setOperation(this);
		currentState = newState;
		try {
			newState.validate();
		} catch (BestXException e) {
			newState.setOperation(null);
			currentState = oldState;
			throw new OperationValidationException("Operation state change to new state: '" + newState + "' failed" + " : " + e.getMessage(), e);
		}
		if (strategy != null) {
			updateHandler();
		}
		updateStateListeners(oldState, newState);
		onNewState(currentState); // this method can call setState: take care
		// of sequence of operations!
	}

	/**
	 * This method allows a secure change state: if the wanted state can't be reached (tipically for validation problems), the operation
	 * will change to an error state.
	 * 
	 * @param newState
	 *            : the state to be reached
	 * @param errorState
	 *            : the fail-over state class
	 */
	public synchronized void setStateResilient(OperationState newState, Class<? extends OperationState> errorStateClass) {
		try {
			setState(newState);
		} catch (BestXException e) {
			LOGGER.error("Couldn't change to state: {}", newState.getClass().getName(), e);
			try {

				OperationState opState = errorStateClass.newInstance();
				if (e.getCause() instanceof SaveBookException) {
					opState.setComment(e.getMessage());
				}
				setState(opState);
			} catch (BestXException e1) {
				LOGGER.error("Operation '{}' change to state '{}' failed: {}", this, errorStateClass.getName(), e.getMessage(), e1);
			} catch (InstantiationException e1) {
				LOGGER.error("Operation '{}' change to state '{}' failed because error class could not be instantiated: {}", this, errorStateClass.getName(), e.getMessage(), e1);
			} catch (IllegalAccessException e1) {
				LOGGER.error("Operation '{}' change to state '{}' failed for problems in accessing error class: {}", this, errorStateClass.getName(), e.getMessage(), e1);
			}
		}
	}

	/**
	 * Change state to an error state, if possible. Otherwise generates a log.
	 * 
	 * @param errorState
	 *            : the fail-over state
	 */
	public synchronized void setErrorState(OperationState errorState) {
		try {
			setState(errorState);
		} catch (BestXException e) {
			LOGGER.error("Operation '{}' change to state '{}' failed : {}", this, errorState.getClass().getName(), e);
		}
	}

	private void updateStateListeners(OperationState oldState, OperationState newState) throws BestXException {
		String orderId = null;
		Date now = new Date();
		if (order != null) {
			orderId = order.getFixOrderId();
			Thread.currentThread().setName(
					"updateStateListeners-" + currentState.getClass().getSimpleName() + "," + toString().replace('@', '=') + ",ISIN="
							+ order.getInstrumentCode()!=null?order.getInstrumentCode():"XXXX" + ",");
		} else {
			Thread.currentThread().setName(
					"updateStateListeners-" + currentState.getClass().getSimpleName() + "," + toString().replace('@', '=') + ",");
		}


		long delta = (now.getTime() - (getOrder() != null ? getOrder().getTransactTime().getTime() : now.getTime()));

		LOGGER.info("[STATE],Timestamp={},StartOrderDiff={},LastStateDiff={},State transition from: {} to: {} - orderId={} - operation={}", now, delta,
				(oldState != null ? now.getTime() - oldState.getEnteredTime().getTime() : "0"), (oldState != null ? oldState.getClass().getSimpleName() : null), newState.getClass().getSimpleName(),
				orderId, getId());

		//LOGGER.debug("Operation {}, notify listeners of state transition from: {} to: {}", getId(), (oldState != null ? oldState.getClass().getSimpleName() : oldState), newState.getClass().getSimpleName());
		if (stateListeners != null) {
			for (OperationStateListener listener : stateListeners) {
				try {
					listener.onOperationStateChanged(this, oldState, newState);
				} catch (SaveBookException sbe) {
					LOGGER.error(sbe.getMessage());
					LOGGER.error("[SAVEBOOKERR] ----------------------");
					LOGGER.error("[SAVEBOOKERR] ORIGINAL EXCEPTION \n");
					LOGGER.error("[SAVEBOOKERR] Message : {}\n", sbe.getOriginalExc().getMessage());
					LOGGER.error("[SAVEBOOKERR] Cause : {}\n", sbe.getOriginalExc().getCause());
					LOGGER.error("[SAVEBOOKERR] Stack : \n");
					StackTraceElement[] ste = sbe.getOriginalExc().getStackTrace();
					for (int count = 0; count < ste.length; count++) {
						LOGGER.error(ste[count].toString());
					}
					LOGGER.error("[SAVEBOOKERR] ----------------------");
					// MATCHING REINVIATI AL SISTEMA DA WARNING
					// if it's one of the matching orders I've to let him be
					// classified again as one of those
					/*
					 * if (getOrder().isMatchingOrder()) getOrder().setMatchingOrder(false);
					 */
					throw new BestXException(sbe.getMessage(), sbe);
				} catch (Exception e) { // Since it can run arbitrary code it is
					// acceptable to catch generic Exception
					LOGGER.error("Order {}: An error occurred while notifying operation change state to listeners : {}", this.getOrder().getFixOrderId(), e.getMessage(), e);
					throw new BestXException("Unable to notify operation state change to listener: " + listener.toString() + " - Exception message: " + e.getMessage(), e);
				}
			}
		}
	}

	private synchronized void updateHandler() throws BestXException {
		handler = strategy.getHandler(this, currentState);
		LOGGER.debug("Operation event handler set from strategy: {} to: {}", strategy.getClass().getName(), handler.getClass().getName());
	}

	/**
	 * Accessor method for current Operation State
	 * 
	 * @return an OperationState object
	 */
	public OperationState getState() {
		return currentState;
	}

	/**
	 * @param operationIdType
	 * @param identifier
	 */
	public void addIdentifier(OperationIdType operationIdType, String identifier) {
		identifiers.put(operationIdType, identifier);
	}

	/**
	 * @param operationIdType
	 * @return
	 */
	public String getIdentifier(OperationIdType operationIdType) {
		return identifiers.get(operationIdType);
	}

	/**
	 * @param idType
	 */
	public void removeIdentifier(OperationIdType operationIdType) {
		identifiers.remove(operationIdType);
	}

	/**
	 * @return
	 */
	public Map<OperationIdType, String> getIdentifiers() {
		return identifiers;
	}

	/**
	 * Set the operation RFQ
	 * 
	 * @param rfq
	 *            : the rfq
	 */
	public void setRfq(Rfq rfq) {
		this.rfq = rfq;
	}

	/**
	 * Get the operation RFQ
	 * 
	 * @return the rfq
	 */
	public Rfq getRfq() {
		return rfq;
	}

	/**
	 * Set the operation Quote
	 * 
	 * @param quote
	 *            : the quote
	 */
	public void setQuote(Quote quote) {
		this.quote = quote;
	}

	/**
	 * Get the operation quote
	 * 
	 * @return the quote
	 */
	public Quote getQuote() {
		return quote;
	}

	/**
	 * Increment the attempts counter and add a new Attempt to the operation Attempts
	 */
	public void addAttempt() {
		++attemptNo;
		LOGGER.trace("Order {} add attempt {}", order.getFixOrderId(), attemptNo);
		getAttempts().add(new Attempt());
	}

	/**
	 * Get the number of attempts
	 * 
	 * @return attempts number
	 */
	public int getAttemptNo() {
		return attemptNo;
	}

	/**
	 * Get the attempts list
	 * 
	 * @return a list of Attempts
	 */
	public List<Attempt> getAttempts() {
		return attempts;
	}

	/**
	 * Set the operation order
	 * 
	 * @param order
	 *            : the order
	 */
	public void setOrder(Order order) {
		this.order = order;
	}

	/**
	 * Get the operation order
	 * 
	 * @return the order
	 */
	public Order getOrder() {
		return order;
	}

	/**
	 * Set the list of attempts
	 * 
	 * @param attempts
	 *            : a list of attempts
	 */
	public void setAttempts(List<Attempt> attempts) {
		this.attempts = attempts;
	}

	/**
	 * Set the list of execution reports
	 * 
	 * @param executionReports
	 *            : list of execution reports
	 */
	public void setExecutionReports(List<ExecutionReport> executionReports) {
		this.executionReports = executionReports;
	}

	/**
	 * Get the list of execution reports
	 * 
	 * @return list of exec reports
	 */
	public List<ExecutionReport> getExecutionReports() {
		if (executionReports==null) executionReports = new ArrayList<ExecutionReport>();
		return executionReports;
	}

	/**
	 * Set the revocation state
	 * 
	 * @param revocationState
	 *            : revocation state
	 */
	public void setRevocationState(RevocationState revocationState) {
		this.revocationState = revocationState;
	}

	/**
	 * Get the revocation state
	 * 
	 * @return revocation state
	 */
	public RevocationState getRevocationState() {
		return revocationState;
	}

	/**
	 * Get the matching operation
	 * 
	 * @return matching operation
	 */
	public Operation getMatchingOperation() {
		return matchingOperation;
	}

	/**
	 * Set the matching operation
	 * 
	 * @param matchingOperation
	 *            : matching operation
	 */
	public void setMatchingOperation(Operation matchingOperation) {
		this.matchingOperation = matchingOperation;
	}

	/**
	 * Utility method
	 * 
	 * @return An Attempt object
	 */
	public Attempt getLastAttempt() {
		Attempt res = null;
		if (attempts.size() > 0) {
			res = attempts.get(attempts.size() - 1);
		}
		return res;
	}

	/**
	 * Remove the last attempt
	 * 
	 * @return the last attempt
	 */
	public Attempt removeLastAttempt() {
		--attemptNo;

		Attempt res = null;
		if (attempts.size() > 0) {
			res = attempts.remove(attempts.size() - 1);
		} else {
			attemptNo = -1;
		}
		return res;
	}

	//    @Override
	//    public String toString() {
	//
	//        // try with rfqID
	//        String uniqueId = identifiers.get(OperationIdType.RFQ_ID);
	//        // try with orderID
	//        uniqueId = (uniqueId == null) ? identifiers.get(OperationIdType.ORDER_ID) : uniqueId;
	//        // try with fixOrderID
	//        uniqueId = (uniqueId == null && order != null) ? order.getFixOrderId() : uniqueId;
	//        // no uniqueID found, use the plain toString
	//        uniqueId = (uniqueId == null) ? super.toString() : uniqueId;
	//
	//        StringBuilder sb = new StringBuilder();
	//        sb.append("Operation=");
	//        sb.append(uniqueId);
	//        return sb.toString();
	//    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Operation [id=");
		builder.append(id);
		builder.append(", currentState=");
		builder.append(currentState);
		//	    builder.append(", identifiers=");
		//	    builder.append(identifiers);
		//	    builder.append(", rfq=");
		//	    builder.append(rfq);
		//	    builder.append(", quote=");
		//	    builder.append(quote);
		builder.append(", order=");
		builder.append(order);
		//	    builder.append(", attempts=");
		//	    builder.append(attempts);
		//	    builder.append(", internalAttempts=");
		//	    builder.append(internalAttempts);
		//	    builder.append(", executionReports=");
		//	    builder.append(executionReports);
		//	    builder.append(", matchingOperation=");
		//	    builder.append(matchingOperation);
		//	    builder.append(", attemptNo=");
		//	    builder.append(attemptNo);
		//	    builder.append(", firstValidAttemptNo=");
		//	    builder.append(firstValidAttemptNo);
		//	    builder.append(", firstAttemptInCurrentCycle=");
		//	    builder.append(firstAttemptInCurrentCycle);
		//	    builder.append(", hasBeenInternalized=");
		//	    builder.append(hasBeenInternalized);
		//	    builder.append(", noProposalsOrderOnBook=");
		//	    builder.append(noProposalsOrderOnBook);
		//	    builder.append(", magnetCustomerRevoke=");
		//	    builder.append(magnetCustomerRevoke);
		//	    builder.append(", cancelOrderWaiting=");
		//	    builder.append(cancelOrderWaiting);
		//	    builder.append(", furtherDetails=");
		//	    builder.append(furtherDetails);
		//	    builder.append(", customerRevokeReceived=");
		//	    builder.append(customerRevokeReceived);
		//	    builder.append(", revocationState=");
		//	    builder.append(revocationState);
		//	    builder.append(", isProcessingCounter=");
		//	    builder.append(isProcessingCounter);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public synchronized void onNewState(OperationState currentState) {
		Thread.currentThread().setName(
				"new-" + currentState.getClass().getSimpleName() + "-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrumentCode() != null ? order.getInstrumentCode() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onNewState(currentState);
		} catch (ObjectNotInitializedException onie) {
			LOGGER.error("Application Exception while handling event: {}", onie.getMessage());
			onApplicationError(getState(), onie, onie.getMessage());
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling event: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event: {}", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onStateRestore(OperationState currentState) {
		Thread.currentThread().setName(
				"restore-" + currentState.getClass().getSimpleName() + "-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onStateRestore(currentState);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTimerExpired(String jobName, String groupName) {
		Thread.currentThread().setName(
				"onTimerExpire-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTimerExpired(jobName, groupName);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onCustomerOrder(CustomerConnection source, Order order) {
		Thread.currentThread().setName(
				"onCustomerOrder-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomerOrder(source, order);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCustomerRfq(CustomerConnection source, Rfq rfq) {
		Thread.currentThread().setName("onCustomerRfq-" + toString() + "-ISIN:" + (rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomerRfq(source, rfq);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCustomerQuoteAcknowledged(CustomerConnection source, Quote quote) {
		Thread.currentThread().setName(
				"onQuoteAck-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomerQuoteAcknowledged(source, quote);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCustomerQuoteNotAcknowledged(CustomerConnection source, Quote quote, Integer errorCode, String errorMessage) {
		Thread.currentThread().setName(
				"onQuoteNack-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomerQuoteNotAcknowledged(source, quote, errorCode, errorMessage);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleOrderAutoExecution(TradingConsoleConnection source) {
		Thread.currentThread().setName(
				"onAutoExec-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleOrderAutoExecution(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleOrderPendingAccepted(TradingConsoleConnection source) {
		Thread.currentThread().setName(
				"onPendAccept-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleOrderPendingAccepted(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleOrderPendingCounter(TradingConsoleConnection source, ClassifiedProposal counter) {
		Thread.currentThread().setName(
				"onPendCounter-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleOrderPendingCounter(source, counter);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleOrderPendingExpired(TradingConsoleConnection source) {
		Thread.currentThread().setName(
				"onPendExpired-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleOrderPendingExpired(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleOrderPendingRejected(TradingConsoleConnection source, String reason) {
		Thread.currentThread().setName("onPendReject-" + toString() + "-ISIN:" + order.getInstrument().getIsin());
		try {
			handler.onTradingConsoleOrderPendingRejected(source, reason);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source) {
		Thread.currentThread().setName(
				"onTradeAck-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleTradeAcknowledged(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleTradeNotAcknowledged(TradingConsoleConnection source) {
		Thread.currentThread().setName("onTradeNack-" + toString() + "-ISIN:" + order.getInstrument().getIsin());
		try {
			handler.onTradingConsoleTradeNotAcknowledged(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleTradeReceived(TradingConsoleConnection source) {
		Thread.currentThread().setName(
				"onTradeRec-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleTradeReceived(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradingConsoleTradeRejected(TradingConsoleConnection source, String reason) {
		Thread.currentThread().setName(
				"onTradeReject-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradingConsoleTradeRejected(source, reason);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCmfErrorReply(TradingConsoleConnection source, String errorMessage, int errorCode) {
		Thread.currentThread().setName(
				"onCmfErrorReply-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCmfErrorReply(source, errorMessage, errorCode);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport) {
		Thread.currentThread().setName(
				"onExecRepAck-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			LOGGER.debug("Received an Acknowledgement, starting to manage it.");
			handler.onCustomerExecutionReportAcknowledged(source, executionReport);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onCustomerExecutionReportNotAcknowledged(CustomerConnection source, ExecutionReport executionReport, Integer errorCode, String errorMessage) {
		Thread.currentThread().setName(
				"onExecRepNack-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomerExecutionReportNotAcknowledged(source, executionReport, errorCode, errorMessage);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorAbortState(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onAbort-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorAbortState(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorForceState(OperatorConsoleConnection source, OperationState newState, String comment) {
		Thread.currentThread().setName(
				"onForce-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorForceState(source, newState, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorReinitiateProcess(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onReinit-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorReinitiateProcess(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorRestoreState(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onRestore-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorRestoreState(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorRetryState(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onRetry-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorRetryState(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorSuspendState(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onSuspend-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorSuspendState(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorTerminateState(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onTerminate-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorTerminateState(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorMatchOrders(OperatorConsoleConnection source, Money ownPrice, Money matchingPrice, String comment) {
		Thread.currentThread().setName(
				"onMatchOrders-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorMatchOrders(source, ownPrice, matchingPrice, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onPricesResult(PriceService source, PriceResult priceResult) {
		Thread.currentThread().setName(
				"onPricesResult-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onPricesResult(source, priceResult);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onQuoteResult(PriceService source, QuoteResult quoteResult) {
		Thread.currentThread().setName(
				"onQuoteResult-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onQuoteResult(source, quoteResult);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorExecuteState(OperatorConsoleConnection source, Money execPrice, String comment) {
		Thread.currentThread().setName(
				"onOperatorExecuteState-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorExecuteState(source, execPrice, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onOperatorSendDDECommand(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorSendDDECommand-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorSendDDECommand(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketMatchFound(MarketBuySideConnection source, Operation matching) {
		Thread.currentThread().setName(
				"onMarketMatchFound-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketMatchFound(source, matching);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketRevocationAccepted(MarketBuySideConnection source) {
		Thread.currentThread().setName(
				"onMarketRevocationAccepted-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketRevocationAccepted(source);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketRevocationRejected(MarketBuySideConnection source, String reason) {
		Thread.currentThread().setName(
				"onMarketRevocationRejected-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketRevocationRejected(source, reason);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onTradeFeedDataRetrieved(MarketBuySideConnection source, String ticketNum, int numberOfDaysAccrued, BigDecimal accruedInterestAmount) {
		Thread.currentThread().setName(
				"onTradeFeedDataRetrieved-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onTradeFeedDataRetrieved(source, ticketNum, numberOfDaysAccrued, accruedInterestAmount);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketExecutionReport(MarketBuySideConnection source, Order order, MarketExecutionReport marketExecutionReport) {
		Thread.currentThread().setName(
				"onMarketExecutionReport-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onMarketExecutionReport(source, order, marketExecutionReport);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketOrderReject(MarketBuySideConnection source, Order order, String reason, String sessionId) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected order reject received in state [{}], discarding it - event market code: {}, reason: {}", currentState, eventMarketCode, reason);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now),
		// unless the state handler states that it can be accepted (false by default)
		if ( (currentMarketCode != eventMarketCode) && (!handler.isEventFromOtherMarketAcceptable(eventMarketCode)) ) {
			LOGGER.warn("Order reject from market [{}] received in state on different market [{}], discarding it - reason was: {}", eventMarketCode, currentMarketCode, reason); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketOrderReject-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderReject(source, order, reason, sessionId);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onQuoteStatusTimeout(MarketBuySideConnection source, Order order, String quoteReqId) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected QuoteStatus/Timeout in state [{}], discarding it - event market code: {}", currentState, eventMarketCode);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now),
		// unless the state handler states that it can be accepted (false by default)
		if ( (currentMarketCode != eventMarketCode) && (!handler.isEventFromOtherMarketAcceptable(eventMarketCode)) ) {
			LOGGER.warn("QuoteStatus/Timeout from market [{}] received in state on different market [{}], discarding it", eventMarketCode, currentMarketCode); 
			return;
		}

		Thread.currentThread().setName(
				"onQuoteStatusTimeout-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onQuoteStatusTimeout(source, order, quoteReqId);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public void onQuoteStatusTradeEnded(MarketBuySideConnection source, Order order, String quoteReqId, String text) {
		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected QuoteStatus/TradeEnded in state [{}], discarding it - event market code: {}, text: {}", currentState, eventMarketCode, text);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now),
		// unless the state handler states that it can be accepted (false by default)
		if ( (currentMarketCode != eventMarketCode) && (!handler.isEventFromOtherMarketAcceptable(eventMarketCode)) ) {
			LOGGER.warn("QuoteStatus/TradeEnded from market [{}] received in state on different market [{}], discarding it", eventMarketCode, currentMarketCode); 
			return;
		}

		Thread.currentThread().setName(
				"onQuoteStatusTradeEnded-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onQuoteStatusTradeEnded(source, order, quoteReqId, text);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}


	@Override
	public synchronized void onMarketOrderStatus(MarketBuySideConnection source, Order order, Order.OrderStatus orderStatus) {
		Thread.currentThread().setName(
				"onMarketOrderStatus-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderStatus(source, order, orderStatus);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onUnexpectedMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {
		Thread.currentThread().setName(
				"onUnexpectedMarketQuote-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			LOGGER.debug("notifying unexpected counter to handler: {}", handler.getClass().getSimpleName());
			handler.onUnexpectedMarketProposal(source, instrument, proposal);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketProposal(MarketBuySideConnection source, Instrument instrument, ClassifiedProposal proposal) {

		// Validate the proposal
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// a proposal received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected proposal received in state [{}], discarding it - proposal market code: {}, proposal: {}", currentState, eventMarketCode, proposal);
			return;
		}

		// a proposal from a market different from the state's market should be discarded (some error occured if I receive it now)
		if ( (currentMarketCode != eventMarketCode) && (!handler.isEventFromOtherMarketAcceptable(eventMarketCode)) ) {
			LOGGER.warn("Proposal from market [{}] received in state on different market [{}], discarding it - was: {}", eventMarketCode, currentMarketCode, proposal); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketQuote-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			LOGGER.debug("[INT-TRACE] notifying counter to handler: {}", handler.getClass().getSimpleName());
			handler.onMarketProposal(source, instrument, proposal);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}


	@Override
	public synchronized void onMarketProposalStatusChange(MarketBuySideConnection source, String quoteId, Proposal.ProposalType status)
	{
		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected proposal status change received in state [{}], discarding it - event market code: {}", currentState, eventMarketCode);
			return;
		}

		Thread.currentThread().setName(
				"onMarketProposalStatusChange-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketProposalStatusChange(source, quoteId, status);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}    

	@Override
	public synchronized void onMarketRfqReject(MarketBuySideConnection source, Rfq rfq, String reason) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected rfq reject received in state [{}], discarding it - event market code: {}, reason: {}", currentState, eventMarketCode, reason);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now)
		if (currentMarketCode != eventMarketCode) {
			LOGGER.warn("Rfq reject from market [{}] received in state on different market [{}], discarding it - reason was: {}", eventMarketCode, currentMarketCode, reason); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketRfqReject-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketRfqReject(source, rfq, reason);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onApplicationError(OperationState currentState, Exception exception, String message) {
		Thread.currentThread().setName(
				"onApplicationError-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onApplicationError(currentState, exception, message);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onMarketOrderCancelled(MarketBuySideConnection source, Order order, String reason) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected order cancelled event received in state [{}], discarding it - event market code: {}, reason: {}", currentState, eventMarketCode, reason);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now)
		if (currentMarketCode != eventMarketCode) {
			LOGGER.warn("Order cancelled event from market [{}] received in state on different market [{}], discarding it - reason was: {}", eventMarketCode, currentMarketCode, reason); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketOrderCancelled-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderCancelled(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorForceReceived(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorForceReceived-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onOperatorForceReceived(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorAcceptOrder-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorAcceptOrder(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorManualManage(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorManualManage-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorManualManage(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorForceNotExecution-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onOperatorForceNotExecution(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorManualExecution(OperatorConsoleConnection source, String comment, Money price, BigDecimal qty, String mercato, String marketMaker, Money prezzoRif) {
		Thread.currentThread().setName("onOperatorManualExecution-" + toString() + "-ISIN:" + order.getInstrument().getIsin());
		try {
			handler.onOperatorManualExecution(source, comment, price, qty, mercato, marketMaker, prezzoRif);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorForceExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorForceExecutedWithoutExecutionReport-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorForceExecutedWithoutExecutionReport(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorForceNotExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorForceNotExecutedWithoutExecutionReport-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorForceNotExecutedWithoutExecutionReport(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorResendExecutionReport(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorResendExecutionReport-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorResendExecutionReport(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorStopOrderExecution(OperatorConsoleConnection source, String comment, Money price) {
		Thread.currentThread().setName(
				"onOperatorStopOrderExecution-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorStopOrderExecution(source, comment, price);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onFixRevoke(CustomerConnection source) {
		Thread.currentThread().setName(
				"onFixRevoke-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onFixRevoke(source);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorRevokeAccepted(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorRevokeAccepted-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorRevokeAccepted(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorRevokeRejected(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorRevokeRejected-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorRevokeRejected(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorExceedFilterRecalculate(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorExceedFilterRecalculate-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onOperatorExceedFilterRecalculate(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onMarketOrderCancelFillAndBook(MarketBuySideConnection source, Order order, String reason) {
		Thread.currentThread().setName(
				"onMarketOrderCancelFillAndBook-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderCancelFillAndBook(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onMarketOrderCancelFillNoBook(MarketBuySideConnection source, Order order, String reason) {
		Thread.currentThread().setName(
				"onMarketOrderCancelFillNoBook-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderCancelFillNoBook(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason) {
		Thread.currentThread().setName(
				"onMarketOrderCancelNoFill-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderCancelNoFill(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public void onMarketOrderCancelNoFill(MarketBuySideConnection source, Order order, String reason, MarketExecutionReport marketExecutionReport) {
		Thread.currentThread().setName(
				"onMarketOrderCancelNoFill-withCancelReportSaving-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderCancelNoFill(source, order, reason, marketExecutionReport);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onMarketOrderCancelRequestReject(MarketBuySideConnection source, Order order, String reason) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected cancel request reject received in state [{}], discarding it - event market code: {}, reason: {}", currentState, eventMarketCode, reason);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now)
		if (currentMarketCode != eventMarketCode) {
			LOGGER.warn("Cancel request reject from market [{}] received in state on different market [{}], discarding it - reason was: {}", eventMarketCode, currentMarketCode, reason); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketOrderCancelRequestReject-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		try {
			handler.onMarketOrderCancelRequestReject(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorMoveToNotExecutable(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorMoveToNotExecutable-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorMoveToNotExecutable(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorExecutedOperationDelete(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorExecutedOperationDelete-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorExecutedOperationDelete(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorExecutedOperationModify(OperatorConsoleConnection source, BigDecimal priceAmount, BigDecimal qty, String sDate, String comment) {
		Thread.currentThread().setName(
				"onOperatorExecutedOperationModify-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorExecutedOperationModify(source, priceAmount, qty, sDate, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}

	@Override
	public synchronized void onOperatorSendDESCommand(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorSendDESCommand-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorSendDESCommand(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event", e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public synchronized void onMarketOrderTechnicalReject(MarketBuySideConnection source, Order order, String reason) {

		// Validate the event
		MarketCode eventMarketCode = source.getMarketCode(); 
		MarketCode currentMarketCode = currentState.getMarketCode();

		// an event received in a state without a specific market code should be ignored (probably it arrived too late, and I am already doing something else - e.g. a new price discovery)
		if (currentMarketCode == null) {
			LOGGER.warn("Unexpected order technical reject received in state [{}], discarding it - event market code: {}, reason: {}", currentState, eventMarketCode, reason);
			return;
		}

		// an event from a market different from the state's market should be discarded (some error occured if I receive it now)
		if (currentMarketCode != eventMarketCode) {
			LOGGER.warn("Order technical reject from market [{}] received in state on different market [{}], discarding it - reason was: {}", eventMarketCode, currentMarketCode, reason); 
			return;
		}

		Thread.currentThread().setName(
				"onMarketOrderTechnicalReject-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onMarketOrderTechnicalReject(source, order, reason);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	/**
	 * Set the number of the first valid attempt
	 * 
	 * @param firstValidAttemptNo
	 *            : new first valid attempt number
	 */
	public void setFirstValidAttemptNo(int firstValidAttemptNo) {
		this.firstValidAttemptNo = firstValidAttemptNo;
	}

	/**
	 * Get the list of valid attempts starting from the one set as the first
	 * 
	 * @return a list of attempts
	 */
	public List<Attempt> getValidAttempts() {
		List<Attempt> result = new ArrayList<Attempt>();
		for (int i = firstValidAttemptNo; i < attempts.size(); i++) {
			result.add(attempts.get(i));
		}
		return result;
	}

	/**
	 * Check if the operation is processing a counter proposal
	 * 
	 * @return the isProcessingCounter flag
	 */
	public boolean isProcessingCounter() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[CNT] Operation " + Thread.currentThread().getName() + " calling a isProcessingCounter check, the flag is " + isProcessingCounter + ", order "
					+ (order != null ? order.getFixOrderId() : "") + ", ISIN "
					+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		}
		return isProcessingCounter.get();
	}

	/**
	 * Set the flag processingCounter
	 * 
	 * @param isProcessingCounter
	 *            the isProcessingCounter to set
	 */
	public void setProcessingCounter(boolean isProcessingCounter) {
		this.isProcessingCounter.set(isProcessingCounter);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("[CNT] Flag set to " + isProcessingCounter + " for Operation " + Thread.currentThread().getName() + ", order " + (order != null ? order.getFixOrderId() : "") + ", ISIN "
					+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));

		}
	}

	/**
	 * Check if the operation has passed the maximum number of automatic attempts
	 * 
	 * @param maxAttemptNo
	 *            : max number of attempts allowed
	 * @return true or false
	 */
	public boolean hasPassedMaxAttempt(int maxAttemptNo) {
		return getAttemptNo() > (firstAttemptInCurrentCycle + maxAttemptNo);
	}

	/**
	 * Check if the operation has reached the maximum number of automatic attempts
	 * 
	 * @param maxAttemptNo
	 *            : max number of attempts allowed
	 * @return true or false
	 */
	public boolean hasReachedMaxAttempt(int maxAttemptNo) {
		return getAttemptNo() >= (firstAttemptInCurrentCycle + maxAttemptNo);
	}

	/**
	 * Set the number of the attempt considered as the first one
	 * 
	 * @param firstAttemptInCurrentCycle
	 *            : first attempt number
	 */
	public void setFirstAttemptInCurrentCycle(int firstAttemptInCurrentCycle) {
		this.firstAttemptInCurrentCycle = firstAttemptInCurrentCycle;
	}

	/**
	 * Get the number of the first attempt
	 * 
	 * @return first attempt number
	 */
	public int getFirstAttemptInCurrentCycle() {
		return firstAttemptInCurrentCycle;
	}

	/**
	 * Set the number of attempts performed
	 * 
	 * @param attemptNo
	 *            : attempts
	 */
	public void setAttemptNo(int attemptNo) {
		this.attemptNo = attemptNo;
	}

	/**
	 * Set the market execution listener
	 * 
	 * @param marketExecutionListener
	 *            : market execution listener
	 */
	public void setMarketExecutionListener(MarketExecutionListener marketExecutionListener) {
		this.marketExecutionListener = marketExecutionListener;
	}

	/**
	 * Get the market execution listener
	 * 
	 * @return the market execution listener
	 */
	public MarketExecutionListener getMarketExecutionListener() {
		return marketExecutionListener;
	}

	/**
	 * Check if the fill received from the market is a new fill
	 * 
	 * @param currentAttempt
	 *            : the attempt we are checking
	 * @param marketExecutionReport
	 *            : the fill received from the market
	 * @return true or false
	 */
	public boolean isNewFill(Attempt currentAttempt, MarketExecutionReport marketExecutionReport) {
		LOGGER.debug("Checking if the received market exec report is a new fill.");
		List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
		if (marketExecutionReports == null) {
			marketExecutionReports = new ArrayList<MarketExecutionReport>();
			currentAttempt.setMarketExecutionReports(marketExecutionReports);
		}

		return internalIsNewFill(marketExecutionReports, marketExecutionReport);
	}

	public boolean isNewFill(InternalAttempt currentAttempt, MarketExecutionReport marketExecutionReport) {
		LOGGER.debug("Checking if the received market exec report is a new fill.");
		List<MarketExecutionReport> marketExecutionReports = currentAttempt.getMarketExecutionReports();
		if (marketExecutionReports == null) {
			marketExecutionReports = new ArrayList<MarketExecutionReport>();
			currentAttempt.setMarketExecutionReports(marketExecutionReports);
		}

		return internalIsNewFill(marketExecutionReports, marketExecutionReport);
	}

	private boolean internalIsNewFill(List<MarketExecutionReport> marketExecutionReports, MarketExecutionReport marketExecutionReport) {
		boolean newFill = true;
		LOGGER.debug("Looping through the existing market exec reports. As default it is a new fill. " + "There are {} market exec reports in the current attempt.", marketExecutionReports.size());
		for (MarketExecutionReport existingExecReport : marketExecutionReports) {
			if (existingExecReport == null || existingExecReport.getTicket() == null) {
				newFill = false;
				LOGGER.debug("Existing exec report is null or its ticket is null. Not a new fill.");
				continue;
			}

			if (marketExecutionReport == null || marketExecutionReport.getTicket() == null) {
				newFill = false;
				LOGGER.debug("Received exec report is null or its ticket is null. Not a new fill.");
				continue;
			}

			if (existingExecReport.getTicket().equalsIgnoreCase(marketExecutionReport.getTicket())) {
				newFill = false;
				LOGGER.debug("There is a market exec report with the same ticket :. Not a new fill", existingExecReport.getTicket());
			}
		}
		LOGGER.debug("It is a new fill? {}", newFill);
		return newFill;
	}



	/**
	 * Set the available attempts as bypassable to allow the reuse of previously enquired venues
	 * 
	 * @param byPass
	 *            : true or false to bypass or not
	 */
	public void markAttemptsToByPass(boolean byPass) {
		List<Attempt> attempts = getAttempts();
		LOGGER.info("Marking available attempts as bypassable when trading venues already used in previous attempts will be checked.");
		if (attempts != null) {
			for (Attempt att : attempts) {
				att.setByPassableForVenueAlreadyTried(true);
			}
		}
	}

	/**
	 * Check if this operation went on the magnet (no price proposals, the order has been put in the market book)
	 * 
	 * @return true or false
	 */
	public boolean isNoProposalsOrderOnBook() {
		return noProposalsOrderOnBook;
	}

	/**
	 * The operation is a magnet one or not
	 * 
	 * @param noProposalsOrderOnBook
	 *            : true or false
	 */
	public void setNoProposalsOrderOnBook(boolean noProposalsOrderOnBook) {
		this.noProposalsOrderOnBook = noProposalsOrderOnBook;
	}

	/**
	 * Check if we received a revoke from the customer while on the magnet
	 * 
	 * @return true or false
	 */
	public boolean isMagnetCustomerRevoke() {
		return magnetCustomerRevoke;
	}

	/**
	 * Set the reception or not of a customer revoke while on the magnet
	 * 
	 * @param magnetCustomerRevoke
	 *            : true or false
	 */
	public void setMagnetCustomerRevoke(boolean magnetCustomerRevoke) {
		this.magnetCustomerRevoke = magnetCustomerRevoke;
	}

	@Override
	public void onMarketPriceReceived(String orderId, BigDecimal marketPrice, Order order, MarketExecutionReport fill) {
		/*
		 * 11-08-2009 Ruggero Here we receive the price with which the magnet market put our market order on the isin book
		 */
		handler.onMarketPriceReceived(orderId, marketPrice, order, fill);
	}

	@Override
	public synchronized void onMarketResponseReceived() {
		/*
		 * 23-09-2009 Ruggero Here we receive response from the market to our FAS order
		 */
		handler.onMarketResponseReceived();
	}

	@Override
	public synchronized void onOperatorTakeOwnership(OperatorConsoleConnection source, UserModel userToAssign) {
		Thread.currentThread().setName(
				"onOperatorTakeOwnership-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorTakeOwnership(source, userToAssign);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}


	/**
	 * This method together with the related setter signals if the operation has received a cancel order message from the market and, being
	 * a magnet order, put the cancel management in wait to allow the processing of eventual fills. Those methods contain synchronized
	 * blocks over the Boolean variable because we want to make every access on it atomic.
	 * 
	 * @return true or false
	 */
	public boolean isCancelOrderWaiting() {
		return cancelOrderWaiting.get();
	}

	/**
	 * This method together with the isCancelOrderWaiting signals if the operation has received a cancel order message from the market and,
	 * being a magnet order, put the cancel management in wait to allow the processing of eventual fills. Those methods contain synchronized
	 * blocks over the Boolean variable because we want to make every access on it atomic.
	 * 
	 * @param cancelOrderWaiting
	 *            : true or false
	 */
	public void setCancelOrderWaiting(boolean cancelOrderWaiting) {
		LOGGER.debug("Putting cancel management wait to {}. Order {}", cancelOrderWaiting, order.getFixOrderId());
		this.cancelOrderWaiting.set(cancelOrderWaiting);
	}

	/**
	 * Get further details about the operation
	 * 
	 * @return a string
	 */
	public String getFurtherDetails() {
		return furtherDetails;
	}

	/**
	 * Set operation details
	 * 
	 * @param furtherDetails
	 *            : further details
	 */
	public void setFurtherDetails(String furtherDetails) {
		this.furtherDetails = furtherDetails;
	}

	/**
	 * Manage the start of a volatility call
	 */
	public void onVolatilityCall() {

	}

	/**
	 * Manage the end of a volatility call
	 */
	public void onEndVolatilityCall() {

	}

	@Override
	public String putInVisibleState() {
		Thread.currentThread().setName(
				"putInVisibleState-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			return handler.putInVisibleState();
		} catch (Exception e) {
			LOGGER.error("Application Exception while managing putInVisibleState {} : ", e.getMessage(), e);
			return "Unable to change status due to an exception: " + e.getMessage();
		}
	}

	/**
	 * Check if we received a customer revoke
	 * 
	 * @return true or false
	 */
	public boolean isCustomerRevokeReceived() {
		return customerRevokeReceived;
	}

	/**
	 * Set the reception of a customer revoke
	 * 
	 * @param customerRevokeReceived
	 *            : true or false
	 */
	public void setCustomerRevokeReceived(boolean customerRevokeReceived) {
		this.customerRevokeReceived = customerRevokeReceived;
	}

	@Override
	public void onRfqLifetimeUpdate(Date timerExpirationDate) {
		Thread.currentThread().setName(
				"onRfqLifetimeUpdate-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onRfqLifetimeUpdate(timerExpirationDate);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public void onOperatorUnreconciledTradeMatched(OperatorConsoleConnection source, BigDecimal executionPrice, String executionMarketMaker, String ticketNumber) {
		Thread.currentThread().setName(
				"onOperatorUnreconciledTradeMatched-" + toString() + "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onOperatorUnreconciledTradeMatched(source, executionPrice, executionMarketMaker, ticketNumber);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, e.getMessage() != null ? e.getMessage() : null);
		}
	}

	@Override
	public void onCustomServiceResponse(boolean error, String securityId) {
		Thread.currentThread().setName(
				"onCustomServiceResponse-"+ toString()+ "-ISIN:"+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onCustomServiceResponse(error, securityId);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	public Long getId() {
		return id;
	}

	@Override
	public boolean isEventFromOtherMarketAcceptable(MarketCode marketCode) {
		// ignored
		return false;
	}

	@Override
	public void onUnexecutionResult(Result result, String message) {
		handler.onUnexecutionResult(result, message);
	}

	@Override
	public void onUnexecutionDefault(String executionMarket) {
		handler.onUnexecutionDefault(executionMarket);
	}

	@Override
	public void onOperatorMoveToLimitFileNoPrice(OperatorConsoleConnection source, String comment) {
		Thread.currentThread().setName(
				"onOperatorMoveToLimitFileNoPrice-"
						+ toString()
						+ "-ISIN:"
						+ (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument()
								.getIsin() : "XXXX"));
		try {
			addAttempt();
			handler.onOperatorMoveToLimitFileNoPrice(source, comment);
		} catch (DataAccessException sqle) {
			LOGGER.error("SQL Exception while handling operator request: {}", sqle.getMessage(), sqle);
			String warningMsg = Messages.getString("DatabaseError");
			onApplicationError(getState(), sqle, warningMsg);
		} catch (Exception e) {
			LOGGER.error("Application Exception while handling event {} : ", e.getMessage(), e);
			onApplicationError(getState(), e, null);
		}
	}

	@Override
	public void onOperatorPriceDiscovery(OperatorConsoleConnection src) {
		Thread.currentThread().setName("onOperatorPriceDiscovery");
		handler.onOperatorPriceDiscovery(src);
	}

	@Override
	public void onRevoke() {
		Thread.currentThread().setName("onOperatorExecutedOperationDelete-" + toString() + "-ISIN:" + (order != null && order.getInstrument() != null ? order.getInstrument().getIsin() : rfq != null && rfq.getInstrument() != null ? rfq.getInstrument().getIsin() : "XXXX"));
		try {
			handler.onRevoke();
		} catch (Exception e) {
			LOGGER.error("Unrecoverable Application Exception. Impossible to process event {} : ", e.getMessage(), e);
		}
	}


	/**
	 * get number of consecutive execution retries from last attempt
	 */
	@Deprecated
	public int getConsecutiveExecutionRetries() {
		return getLastAttempt().getConsecutiveExecutionRetries();
	}

	/**
	 * set number of consecutive retries in last attempt
	 */
	// AMC I suspect that this operation is no longer needed
	@Deprecated
	public void setConsecutiveExecutionRetries(int consecutiveExecutionRetries) {
		getLastAttempt().setConsecutiveExecutionRetries(consecutiveExecutionRetries);
	}

	/**
	 * add a new execution try to the number of execution tries in last attempt
	 */
	@Deprecated
	public void addConsecutiveTry(){
		getLastAttempt().addConsecutiveTry();
	}

	@Override
	public void startTimer() {
		handler.startTimer();		
	}

	public void setStopped(boolean stopped) {
		this.stopped.set(stopped);
	}

	public boolean isStopped() {
		return stopped.get();
	}

	public void setId(long id) {
		this.id=id;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public UserModel getOwner() {
		return owner;
	}

	public void setOwner(UserModel owner) {
		this.owner = owner;
	}
	
	private Boolean notAutoExecute;

	/**
	 * @return the notExecuteOrder
	 */
	public Boolean isNotAutoExecute() {
		return notAutoExecute;
	}

	/**
	 * @param notExecuteOrder the notExecuteOrder to set
	 */
	public void setNotAutoExecute(Boolean notExecuteOrder) {
		this.notAutoExecute = notExecuteOrder;
	}

}