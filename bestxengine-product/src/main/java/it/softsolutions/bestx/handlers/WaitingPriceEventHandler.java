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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.datacollector.DataCollector;
import it.softsolutions.bestx.exceptions.CustomerRevokeReceivedException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.InternalAttempt;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.BookDepthValidator;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.ExecutionDestinationService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService.Result;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceCallback;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.CurandoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.manageability.sl.monitoring.NumericValueMonitor;
/**
 * 
 * 
 * Purpose: Waiting Price State handler
 * 
 * Project Name : bestxengine-product First created by: stefano.pontillo Creation date: 18/mag/2012
 * 
 **/
public class WaitingPriceEventHandler extends BaseOperationEventHandler implements ExecutionStrategyServiceCallback {

	private static final long serialVersionUID = -2138211162185307717L;

	private static final Logger LOGGER = LoggerFactory.getLogger(WaitingPriceEventHandler.class);
	// Monitorable
	// private static NumericValueMonitor totalPriceRequestsMonitor = null;
	private static Map<String, NumericValueMonitor> totalPriceRequestsMonitors = new HashMap<String, NumericValueMonitor>();
	private static Map<String, Long> totalPriceRequests = new HashMap<String, Long>();
	private static Map<String, Long> pendingPriceRequests = new HashMap<String, Long>();

	protected final PriceService priceService;
	protected final SerialNumberService serialNumberService;
	protected final long waitingPriceDelay;
	protected final long marketPriceTimeout;
	private final int maxAttemptNo;
	//private final Venue INTERNAL_MM_VENUE;
	private List<String> internalMMcodes;

	protected final ExecutionDestinationService executionDestinationService;
	private boolean rejectOrderWhenBloombergIsBest;
	protected BookDepthValidator bookDepthValidator;
	private OperationStateAuditDao operationStateAuditDao;
	protected boolean doNotExecute;
	private int targetPriceMaxLevel;
	private ApplicationStatus applicationStatus;
	
	private DataCollector dataCollector;

	/**
	 * Constructor.
	 *
	 * @param operation the operation
	 * @param priceService the price service
	 * @param customerFinder the customer finder
	 * @param marketFinder the market finder
	 * @param venueFinder the venue finder
	 * @param serialNumberService the serial number service
	 * @param regulatedMktIsinsLoader the regulated mkt isins loader
	 * @param regulatedMarketPolicies the regulated market policies
	 * @param internalMarketMaker the internal market maker
	 * @param waitingPriceDelay the waiting price delay
	 * @param maxAttemptNo the max attempt no
	 * @param marketPriceTimeout the market price timeout
	 * @param marketSecurityStatusService the market security status service
	 * @param executionDestinationService the execution destination service
	 * @param rejectOrderWhenBloombergIsBest the reject order when bloomberg is best
	 * @param doNotExecute parameter used to decide if execute a LimitFile type order
	 * @param bookDepthValidator the book depth validator customer specific
	 * @param internalMMcodes the internal m mcodes
	 * @throws BestXException the best x exception
	 */

	public WaitingPriceEventHandler(Operation operation, PriceService priceService, CustomerFinder customerFinder,
			SerialNumberService serialNumberService, RegulatedMktIsinsLoader regulatedMktIsinsLoader, List<String> regulatedMarketPolicies, long waitingPriceDelay, int maxAttemptNo,
			long marketPriceTimeout, ExecutionDestinationService executionDestinationService, boolean rejectOrderWhenBloombergIsBest,
			boolean doNotExecute, BookDepthValidator bookDepthValidator, List<String> internalMMcodes, OperationStateAuditDao operationStateAuditDao, int targetPriceMaxLevel,
			ApplicationStatus applicationStatus, DataCollector dataCollector) throws BestXException{
		super(operation);
		this.priceService = priceService;
		String priceServiceName = priceService.getPriceServiceName();
		if (!totalPriceRequestsMonitors.containsKey(priceServiceName)) {
			totalPriceRequestsMonitors.put(priceServiceName, new NumericValueMonitor("waitingPricesPriceRequests_" + priceServiceName, "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]"));
		}
		if (!pendingPriceRequests.containsKey(priceServiceName)) {
			pendingPriceRequests.put(priceServiceName, 0L);
		}
		if (!totalPriceRequests.containsKey(priceServiceName)) {
			totalPriceRequests.put(priceServiceName, 0L);
		}
		this.maxAttemptNo = maxAttemptNo;
		this.serialNumberService = serialNumberService;
		this.waitingPriceDelay = waitingPriceDelay;
		this.marketPriceTimeout = marketPriceTimeout;
		//this.INTERNAL_VENUE = venueFinder.getMarketVenue(marketFinder.getMarketByCode(MarketCode.INTERNALIZZAZIONE, null));
		//this.MATCHING_VENUE = venueFinder.getMarketVenue(marketFinder.getMarketByCode(MarketCode.MATCHING, null));
		this.executionDestinationService = executionDestinationService;
		this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;

		this.internalMMcodes = internalMMcodes;
		if (this.internalMMcodes == null) {
			this.internalMMcodes = new ArrayList<String>();
		}
		this.bookDepthValidator = bookDepthValidator;
		this.operationStateAuditDao = operationStateAuditDao;
		this.doNotExecute = doNotExecute;
		this.targetPriceMaxLevel = targetPriceMaxLevel;
		this.applicationStatus = applicationStatus;
		this.dataCollector = dataCollector;
	}

	@Override
	public void onNewState(OperationState currentState) {
		LOGGER.debug("{} WaitingPriceState entry action", operation.getOrder().getFixOrderId());
		if (customerSpecificHandler != null)
			customerSpecificHandler.onNewState(currentState);
		Order order = operation.getOrder();
		InternalAttempt iatt = null;
		if (operation.getLastAttempt() != null)
			iatt = operation.getLastAttempt().getInternalAttempt();
		if (iatt != null) {
			iatt.setActive(false); // when price discovery starts the (possible) internalAttemp must not be active
		}
		operation.addAttempt();
		operation.setNoProposalsOrderOnBook(false);
		Customer customer = operation.getOrder().getCustomer();
		Set<Venue> venues = selectVenuesForPriceDiscovery(customer);
		if (venues == null) {
			LOGGER.error("Order {}, Customer {} with no policy assigned.", operation.getOrder().getFixOrderId(), customer.getFixId());
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(currentState, null, Messages.getString("CustomerWithoutPolicy.0", customer.getName(), customer.getFixId())), ErrorState.class);
		}

		String priceServiceName = priceService.getPriceServiceName();

		long pendingPriceRequestsVal = pendingPriceRequests.get(priceServiceName);
		pendingPriceRequestsVal++;
		pendingPriceRequests.put(priceServiceName, pendingPriceRequestsVal);
		ApplicationMonitor.setQueuePricesSize(priceServiceName, pendingPriceRequestsVal);

		try {
			priceService.requestPrices(operation, order, operation.getValidAttempts(), venues, marketPriceTimeout, -1, null);
		}
		catch (MarketNotAvailableException mnae) {
			/*
			 * This exception is thrown if : - there are no price connections enabled - there are some price connections not enabled and in
			 * the other markets the isin is not quoted
			 */
			LOGGER.info("An error occurred while calling Price Service", mnae);

			if (operation.getAttemptNo() > 1) {
				operation.removeLastAttempt();
			}

			setNotAutoExecuteOrder(operation);
			customer = operation.getOrder().getCustomer();
			long pendingPricesPriceRequests = pendingPriceRequests.get(priceService.getPriceServiceName());
			pendingPricesPriceRequests--;
			pendingPriceRequests.put(priceService.getPriceServiceName(), pendingPricesPriceRequests);
			ApplicationMonitor.setQueuePricesSize(priceService.getPriceServiceName(), pendingPricesPriceRequests);
			// Create the execution strategy with a null priceResult, we did not receive any price
			try {
				ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation,
						null, rejectOrderWhenBloombergIsBest);
				csExecutionStrategyService.manageAutomaticUnexecution(order, customer);
			}
			catch (BestXException e) {
				LOGGER.error("Order {}, error while managing no market available situation {}", order.getFixOrderId(), e.getMessage(), e);
				operation.removeLastAttempt();
				operation.setStateResilient(new WarningState(currentState, e, Messages.getString("PriceService.15")), ErrorState.class);
			}
			return;
		}
		catch (CustomerRevokeReceivedException crre) {
			long pendingPricesPriceRequests = pendingPriceRequests.get(priceService.getPriceServiceName());
			pendingPricesPriceRequests--;
			pendingPriceRequests.put(priceService.getPriceServiceName(), pendingPricesPriceRequests);
			ApplicationMonitor.setQueuePricesSize(priceService.getPriceServiceName(), pendingPricesPriceRequests);

			LOGGER.info("Order={}, We received a customer revoke while starting the price discovery, we will not do it and instead start the revoking routine.", operation.getOrder().getFixOrderId());
			// if we correctly manage a revoke we can force a return to avoid the creation of the timer
			if (checkCustomerRevoke(order)) {
				return;
			}
		}
		catch (BestXException e) {
			long pendingPricesPriceRequests = pendingPriceRequests.get(priceService.getPriceServiceName());
			pendingPricesPriceRequests--;
			pendingPriceRequests.put(priceService.getPriceServiceName(), pendingPricesPriceRequests);
			ApplicationMonitor.setQueuePricesSize(priceService.getPriceServiceName(), pendingPricesPriceRequests);

			LOGGER.error("Order {}, An error occurred while calling Price Service", operation.getOrder().getFixOrderId(), e);
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(currentState, e, Messages.getString("PriceService.14")), ErrorState.class);
		}

		long totalPriceRequestsVal = totalPriceRequests.get(priceServiceName);
		totalPriceRequestsVal++;
		totalPriceRequests.put(priceServiceName, totalPriceRequestsVal);
		NumericValueMonitor totalPriceRequestsMonitor = totalPriceRequestsMonitors.get(priceServiceName);
		totalPriceRequestsMonitor.setValue(totalPriceRequestsVal);
		LOGGER.info("[MONITOR] Order={}, Waiting Prices price requests: {}", operation.getOrder().getFixOrderId(), totalPriceRequestsVal);
	}

	@Override
	public void startTimer() {
		if (waitingPriceDelay == 0) {
			LOGGER.error("No delay set for price wait. Risk of stale state");
		}
		else {
			setupDefaultTimer(waitingPriceDelay, false);
		}
	}

	@Override
	public void onTimerExpired(String jobName, String groupName) {
		String handlerJobName = super.getDefaultTimerJobName();

		if (jobName.equals(handlerJobName)) {
			if (operation.isStopped())
				return;
			LOGGER.info("Order {}, timer {}-{} expired.", operation.getOrder().getFixOrderId(), jobName, groupName);
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("WaitingPriceEventHandler.0", operation.getOrder().getFixOrderId())), ErrorState.class);
		}
		else {
			super.onTimerExpired(jobName, groupName);
		}
	}

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		Order customerOrder = operation.getOrder();
		LOGGER.info("Order {},  price result received: {}", customerOrder.getFixOrderId(), priceResult.getState());
		//2018-07-25 BESTX-334 SP: this clause allows BestX to send the price discovery result to OTEX for limit file before sending it to automatic execution
		//for limit file which are not found executable (no prices available or out of market) BestX doesn't send any price discovery result  
		if (customerSpecificHandler != null && !customerOrder.isLimitFile())
			customerSpecificHandler.onPricesResult(source, priceResult);
		// Stefano - 20080616 - for statistic purpose
		long time = System.currentTimeMillis() - operation.getState().getEnteredTime().getTime();
		LOGGER.info("[STATISTICS],Order={},OrderArrival={},PriceDiscoverStart={},PriceDiscoverStop={},TimeDiffMillis={}", operation.getOrder().getFixOrderId(),
				DateService.format(DateService.timeFIX, operation.getOrder().getTransactTime()), DateService.format(DateService.timeFIX, operation.getState().getEnteredTime()),
				DateService.format(DateService.timeFIX, DateService.newLocalDate()), time);
		priceService.addNewTimePriceDiscovery(time);

		stopDefaultTimer();
		if (operation.isStopped())
			return;
		long pendingPricesPriceRequests = pendingPriceRequests.get(priceService.getPriceServiceName());
		pendingPricesPriceRequests--;
		pendingPriceRequests.put(priceService.getPriceServiceName(), pendingPricesPriceRequests);
		ApplicationMonitor.setQueuePricesSize(priceService.getPriceServiceName(), pendingPricesPriceRequests);

		//BESTX-377: add always the book to the order
		Attempt currentAttempt = operation.getLastAttempt();
		currentAttempt.setSortedBook(priceResult.getSortedBook());

		if (dataCollector != null) {
			dataCollector.sendBookAndPrices(operation);
		}

		LOGGER.debug("Order {}, End of the price discovery, check if we received a customer revoke for this order and, if so, start the revoking routine.", operation.getOrder().getFixOrderId());
		if (checkCustomerRevoke(operation.getOrder())) {
			LOGGER.info("Order {}, end of the price discovery, customer revoke received for this order. Start the cancel routine.", operation.getOrder().getFixOrderId());
			return;
		}
		LOGGER.debug("Order {}, No customer revoke received.", operation.getOrder().getFixOrderId());

		/* BXMNT-327 */
		if (!bookDepthValidator.isBookDepthValid(currentAttempt, customerOrder) && !customerOrder.isLimitFile()
				&& !operation.isNotAutoExecute()) { // market order action +++
			if (!BondTypesService.isUST(operation.getOrder().getInstrument()) || this.applicationStatus.getType() == ApplicationStatus.Type.MONITOR) {
				try {
					ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
					operation.setStateResilient(new SendAutoNotExecutionReportState(Messages.getString("RejectInsufficientBookDepth.0", bookDepthValidator.getMinimumRequiredBookDepth())), ErrorState.class);
				}
				catch (BestXException e) {
					LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
					String errorMessage = e.getMessage();
					operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
				}
				return;
			}
		}

		MarketCode mktCode = null;
		ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, priceResult, rejectOrderWhenBloombergIsBest);
		// AMC 20190208 BESTX-385 best on bloomberg requires to be managed with auto unexecution when best on bloomberg is configured for auto unexecution
		ClassifiedProposal executionProposal = currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide());
		boolean doRejectThisBestOnBloomberg = executionProposal != null && executionProposal.getMarket().getMarketCode() == Market.MarketCode.BLOOMBERG && rejectOrderWhenBloombergIsBest;
		if(doRejectThisBestOnBloomberg)
			LOGGER.debug("Best price on Bloomberg. Order {} must be rejected back to OMS", customerOrder.getFixOrderId());
		else
			LOGGER.debug("Best not on Bloomberg or flag rejectOrderWhenBloombergIsBest is false");
		if (priceResult.getState() == PriceResult.PriceResultState.COMPLETE) {
			// Fill Attempt
			currentAttempt.setExecutionProposal(currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()));
			if (operation.hasPassedMaxAttempt(maxAttemptNo)/*&& !operation.getOrder().isLimitFile() AMC 20181210 removed because maxAttemptNo is in current lifecycle BESTX-380 */) {
				LOGGER.info("Order={}, Max number of attempts reached.", operation.getOrder().getFixOrderId());
				currentAttempt.setByPassableForVenueAlreadyTried(true);

				try {
					ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
					operation.setStateResilient(new SendAutoNotExecutionReportState(Messages.getString("EventNoMoreRetry.0")), ErrorState.class);
					return;
				} catch (BestXException e) {
					LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
					String errorMessage = e.getMessage();
					operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
					return;
				}
			}
			// Build MarketOrder
			MarketOrder marketOrder = new MarketOrder();
			Money limitPrice = calculateTargetPrice(customerOrder, currentAttempt);
			if (currentAttempt.getExecutionProposal() != null) {
				currentAttempt.setMarketOrder(marketOrder);
				marketOrder.setValues(customerOrder);
				marketOrder.setTransactTime(DateService.newUTCDate());
				marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());

				marketOrder.setMarketMarketMaker(currentAttempt.getExecutionProposal().getMarketMarketMaker());
				marketOrder.setLimit(limitPrice);
				LOGGER.info("Order={}, Selecting for execution market market maker: {} and price {}", operation.getOrder().getFixOrderId(), marketOrder.getMarketMarketMaker(), limitPrice == null? "null":limitPrice.getAmount().toString());
				marketOrder.setVenue(currentAttempt.getExecutionProposal().getVenue());
			}

			ApplicationStatisticsHelper.logStringAndUpdateOrderIds(operation.getOrder(), "Order.Execution_" + source.getPriceServiceName() + "." + operation.getOrder().getInstrument().getIsin(), this
					.getClass().getName());
			// normal flow
			if(!operation.isNotAutoExecute()) { // limit file with no auto execution case
				LOGGER.debug("Order {} identified as limit file and auto execution inactive", customerOrder.getFixOrderId());
				if(customerOrder.isLimitFile() && doNotExecute) {  // limit file order action +++
					LOGGER.info("Order {} could be executed, but BestX is configured to not execute limit file orders.", customerOrder.getFixOrderId());
					operation.setStateResilient(new OrderNotExecutableState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
				} else { // limit file with auto execution or market order
					//2018-07-25 BESTX-334 SP: this call allows BestX to send the price discovery result to OTEX for limit file before sending it to automatic execution
					//for limit file which are not found executable (no prices available or out of market) BestX doesn't send any price discovery result  
					if (customerSpecificHandler!=null && customerOrder.isLimitFile())
						customerSpecificHandler.onPricesResult(source, priceResult);
					LOGGER.debug("Order {} identified as limit file with auto execution or market order", customerOrder.getFixOrderId());
					csExecutionStrategyService.startExecution(operation, currentAttempt, serialNumberService);
					// last row in this method for executable operation
				}
			} else { // flow for not auto executable orders, limit file and market
				if (customerSpecificHandler!=null && customerOrder.isLimitFile())
					customerSpecificHandler.onPricesResult(source, priceResult);
				LOGGER.debug("Order {} identified as not auto executable. Go to CurandoState", customerOrder.getFixOrderId());
				operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
			}
		} else if (priceResult.getState() == PriceResult.PriceResultState.INCOMPLETE) {
			LOGGER.warn("Order {} , Price result is INCOMPLETE, setting to Warning state", operation.getOrder().getFixOrderId());
			checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventPriceTimeout.0", priceResult.getReason())), ErrorState.class);
		} else if (priceResult.getState() == PriceResult.PriceResultState.NULL 
				|| priceResult.getState() == PriceResult.PriceResultState.ERROR) {
			
			boolean executable = !operation.isNotAutoExecute() && (!operation.getOrder().isLimitFile() || !doNotExecute);
			
			if (executable && (BondTypesService.isUST(operation.getOrder().getInstrument()) || customerOrder.isLimitFile())) { 
				// it is an executable UST order and there are no prices on consolidated book
				csExecutionStrategyService.startExecution(operation, currentAttempt, serialNumberService);
			} else {
				Customer customer = customerOrder.getCustomer();
				// checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
				try {
					csExecutionStrategyService.manageAutomaticUnexecution(customerOrder, customer);
				} catch (BestXException e) {
					LOGGER.error("Order {}, error while managing {} price result state {}", customerOrder.getFixOrderId(), priceResult.getState().name(), e.getMessage(), e);
					operation.removeLastAttempt();
					operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
				}
			}
		}
	}

	/**
	 * @param order client order 
	 * @param currentAttempt for which the target price needs to be calculated. Contains the sorted book, the execution proposal, the market order.
	 * @return
	 */
	public Money calculateTargetPrice(Order order, Attempt currentAttempt) {
		Money limitPrice = null;
		Money ithBest = null;
		ClassifiedProposal ithBestProp = null;
		Money best = null;
		try {
			best = currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()).getPrice();
			ithBestProp = BookHelper.getIthProposal(currentAttempt.getSortedBook().getAcceptableSideProposals(operation.getOrder().getSide()), this.targetPriceMaxLevel);
			ithBest = ithBestProp.getPrice();
		} catch(NullPointerException e) {
			LOGGER.warn("NullPointerException trying to manage widen best or get the {}-th best for order {}", this.targetPriceMaxLevel, order.getFixOrderId());
			LOGGER.warn("NullPointerException trace", e);
		}
		try {
			double spread = BookHelper.getQuoteSpread(currentAttempt.getSortedBook().getAcceptableSideProposals(operation.getOrder().getSide()), this.targetPriceMaxLevel);
			CustomerAttributes custAttr = (CustomerAttributes) order.getCustomer().getCustomerAttributes();
			BigDecimal customerMaxWideSpread = custAttr.getWideQuoteSpread();
			if(customerMaxWideSpread != null && customerMaxWideSpread.doubleValue() < spread) { // must use the spread, not the i-th best
				limitPrice = BookHelper.widen(best, customerMaxWideSpread, operation.getOrder().getSide(), order.getLimit() == null ? null : order.getLimit().getAmount());
				LOGGER.info("Order {}: widening market order limit price {}. Max wide spread is {} and spread between best {} and i-th best {} has been calculated as {}",
						order.getFixOrderId(),
						limitPrice == null?" N/A":limitPrice.getAmount().toString(),
								customerMaxWideSpread == null?" N/A":customerMaxWideSpread.toString(),
										best == null?" N/A":best.getAmount().toString(),
												ithBest == null?" N/A":ithBest.getAmount().toString(),
														spread
						);
			} else {// use i-th best
				limitPrice = ithBest;
			}
			if(limitPrice == null) { // necessary to avoid null limit price. See the book depth minimum for execution 
				if (currentAttempt.getExecutionProposal().getWorstPriceUsed() != null) {
					limitPrice = currentAttempt.getExecutionProposal().getWorstPriceUsed();
					LOGGER.debug("Use worst price of consolidated proposal as market order limit price: {}", limitPrice == null? "null":limitPrice.getAmount().toString());
				} else {
					limitPrice = currentAttempt.getExecutionProposal() == null ? null : currentAttempt.getExecutionProposal().getPrice();
					LOGGER.debug("No i-th best - Use proposal as market order limit price: {}", limitPrice == null? "null":limitPrice.getAmount().toString());
				}                	
			} else {
				if(order.getLimit() != null && isWorseThan(limitPrice, order.getLimit(), order.getSide())) {
					LOGGER.debug("Found price is {}, which is worse than client order limit price: {}. Will use client order limit price", limitPrice.getAmount().toString(), order.getLimit().getAmount().toString());					
					limitPrice = order.getLimit();
				} else
					LOGGER.debug("Use less wide between i-th best proposal and best widened by {} as market order limit price: {}", customerMaxWideSpread, limitPrice == null? "null":limitPrice.getAmount().toString());
			}
		} catch (Exception e) {
			LOGGER.warn("Problem appeared while calculating target price", e);
		}
		return limitPrice;
	}

	protected boolean isWorseThan(Money p1, Money p2, OrderSide side) {
		if(side == null) return false;
		if(side == OrderSide.BUY) return p1.compareTo(p2) > 0;
		else return p1.compareTo(p2) < 0;
	}

	protected ClassifiedProposal getInternalProposal(List<ClassifiedProposal> proposals)
	{
		LOGGER.debug("[INT-TRACE] Order {} : Internal brokers list is: {}", operation.getOrder().getFixOrderId(), internalMMcodes );

		ClassifiedProposal internalProposal = null;

		boolean foundInternalMM = false;
		if (operation.getLastAttempt() == null) {
			return null;
		}
		if (operation.getLastAttempt().getExecutionProposal() == null) {
			return null;
		}

		MarketMaker bestProposalMM = operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker();
		Market bestProposalMkt = operation.getLastAttempt().getExecutionProposal().getMarket();

		// no best proposal (should not happen), so obviously nothing to internalize
		if (bestProposalMkt == null) {
			return null;
		}
		// this algorithm currently only supports Bloomberg
		if ( bestProposalMkt.getMarketCode() != MarketCode.BLOOMBERG) {
			return null;
		}
		// best proposal is from an internal MM: execute with him directly
		if (internalMMcodes.contains(bestProposalMM.getCode())) {
			return null;
		}

		// bestInternalMMCode will be used for internalization, if needed
		MarketMaker proposalMM = null;
		for (ClassifiedProposal proposal : proposals) {
			proposalMM = null;

			if (proposal != null && proposal.getMarketMarketMaker() != null && proposal.getMarketMarketMaker().getMarketMaker() != null) {
				proposalMM = proposal.getMarketMarketMaker().getMarketMaker();
			}

			if ( (proposalMM != null) && (internalMMcodes.contains(proposalMM.getCode())) ) {

				//[RR20130930] BXMNT-354: if the internal broker proposal has a quantity of zero we must not start the internalization, thus we immediately skip a proposal if
				//it has a quantity of zero
				if (proposal.getQty() == null || proposal.getQty().equals(BigDecimal.ZERO)) {
					LOGGER.warn("Order {}, proposal {} cannot be considered for internalization because the quantity is zero", operation.getOrder().getFixOrderId(), proposal);
					continue;
				} else {
					foundInternalMM = true;
					internalProposal = proposal;
					break;
				}
			}
		}

		// must internalize if internal broker is quoting, and it is not best (on RTFI)
		if ( (foundInternalMM) && (internalMMcodes.contains(bestProposalMM.getCode())) ) {
			LOGGER.info("[INT-TRACE] Order {} : Internal broker {} is quoting on RTFI and is not best --> enable internalization", operation.getOrder().getFixOrderId(), internalProposal.getMarketMarketMaker().getMarketSpecificCode() );
			return internalProposal;
		}
		else {
			if (!foundInternalMM) {
				LOGGER.info("[INT-TRACE] Order {} : No internal broker is quoting --> do not enable internalization", operation.getOrder().getFixOrderId() );
			}
			else {
				LOGGER.info("[INT-TRACE] Order {} : Internal broker {} is quoting but is best --> do not enable internalization", operation.getOrder().getFixOrderId(), internalProposal.getMarketMarketMaker().getMarketSpecificCode() );
			}
			return null;
		}

	}

	@Deprecated
	@Override
	public void onUnexecutionResult(Result result, String message) {
		switch (result) {
		case CustomerAutoNotExecution:
		case MaxDeviationLimitViolated:
			try {
				ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);

				// [RR20120910] The MaxDeviationLimitViolated case in the old implementation
				// required the following lines :
				//
				// Attempt currentAttempt = operation.getLastAttempt();
				// currentAttempt.setSortedBook(priceResult.getSortedBook());
				//
				// this operation has already been performed in the onPricesResult method
				// in a piece of code shared by all the callers, we can avoid to reput it
				// here. Check the behaviour while testing.

				operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
			} catch (BestXException e) {
				LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
				String errorMessage = e.getMessage();
				operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
			}
			break;
		case Failure:
			LOGGER.error("Order {} : ", operation.getOrder().getFixOrderId(), message);
			operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
			break;
		case LimitFileNoPrice:
			operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
			break;
		case LimitFile:
			//Update the BestANdLimitDelta field on the TabHistoryOrdini table
			Order order = operation.getOrder();
			operationStateAuditDao.updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
			operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
			break;
		default:
			LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", operation.getOrder().getFixOrderId());
			operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
			break;
		}
	}

	// result on manageAutomaticUnexecution
	@Deprecated
	@Override
	public void onUnexecutionDefault(String executionMarket) {
		if (customerSpecificHandler!=null) customerSpecificHandler.onUnexecutionDefault(executionMarket);
		if (operation.hasReachedMaxAttempt(maxAttemptNo)) {
			operation.setStateResilient(new CurandoState(Messages.getString("EventNoMoreRetry.0")), ErrorState.class);
		} else {
			operation.setStateResilient(new CurandoState(), ErrorState.class);
		}
	}

	public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}
	
	

}