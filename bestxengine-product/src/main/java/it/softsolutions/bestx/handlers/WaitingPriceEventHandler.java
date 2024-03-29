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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.OrderHelper;
import it.softsolutions.bestx.RegulatedMktIsinsLoader;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.MarketOrderBuilder;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.datacollector.DataCollector;
import it.softsolutions.bestx.exceptions.CustomerRevokeReceivedException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.executionflow.FreezeOrderAction;
import it.softsolutions.bestx.executionflow.FreezeOrderAction.NextPanel;
import it.softsolutions.bestx.executionflow.GoToErrorStateAction;
import it.softsolutions.bestx.executionflow.RejectOrderAction;
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
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.ExecutionDestinationService;
import it.softsolutions.bestx.services.MarketOrderFilterChain;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService.Result;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceCallback;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.CurandoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WarningState;
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
	private static Map<String, Long> totalPriceRequests = new HashMap<String, Long>();
	private static Map<String, Long> pendingPriceRequests = new HashMap<String, Long>();

	protected final PriceService priceService;
	protected final SerialNumberService serialNumberService;
	protected final long waitingPriceDelay;
	protected final long marketPriceTimeout;
	private final int maxAttemptNo;
	private List<String> internalMMcodes;

	protected final ExecutionDestinationService executionDestinationService;
	private OperationStateAuditDao operationStateAuditDao;
	protected boolean doNotExecute;
	private ApplicationStatus applicationStatus;
	
	private DataCollector dataCollector;
	private MarketOrderBuilder marketOrderBuilder;
	private MarketOrderFilterChain marketOrderFilterChain;
	
	private PriceResult priceResultReceived;
	
	public static String defaultStrategyName="Fallback: ";
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
			long marketPriceTimeout, ExecutionDestinationService executionDestinationService,
			boolean doNotExecute, List<String> internalMMcodes, OperationStateAuditDao operationStateAuditDao, 
			ApplicationStatus applicationStatus, DataCollector dataCollector, MarketOrderBuilder marketOrderBuilder, MarketOrderFilterChain marketOrderFilterChain) throws BestXException{
		super(operation);
		this.priceService = priceService;
		String priceServiceName = priceService.getPriceServiceName();
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
		this.executionDestinationService = executionDestinationService;

		this.internalMMcodes = internalMMcodes;
		if (this.internalMMcodes == null) {
			this.internalMMcodes = new ArrayList<String>();
		}
		this.operationStateAuditDao = operationStateAuditDao;
		this.doNotExecute = doNotExecute;
		this.applicationStatus = applicationStatus;
		this.dataCollector = dataCollector;
		this.marketOrderBuilder = marketOrderBuilder;
		this.marketOrderFilterChain = marketOrderFilterChain;
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
						null);
				csExecutionStrategyService.manageAutomaticUnexecution(order, customer, defaultStrategyName);
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
			//LOGGER.info("Order {}, timer {}-{} expired.", operation.getOrder().getFixOrderId(), jobName, groupName);
			LOGGER.warn("Order {}: timer MarketPriceTimeout expired.", operation.getOrder().getFixOrderId());
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("WaitingPriceEventHandler.0", operation.getOrder().getFixOrderId())), ErrorState.class);
		}
		else {
			super.onTimerExpired(jobName, groupName);
		}
	}

	@Override
	public void onPricesResult(PriceService source, PriceResult priceResult) {
		this.priceResultReceived = priceResult;
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

		if (priceResult.getState() == PriceResult.PriceResultState.COMPLETE
	         || priceResult.getState() == PriceResult.PriceResultState.EMPTY
	         || priceResult.getState() == PriceResult.PriceResultState.UNAVAILABLE) {
			// Fill Attempt
			currentAttempt.setExecutionProposal(currentAttempt.getSortedBook().getBestProposalBySide(operation.getOrder().getSide()));
			try {
				this.marketOrderBuilder.buildMarketOrder(operation, operation);
			} catch (Exception e) {
				operation.setStateResilient(new WarningState(operation.getState(), e, e.getMessage()), ErrorState.class);
			}
		} else if (priceResult.getState() == PriceResult.PriceResultState.INCOMPLETE) {
			LOGGER.warn("Order {} , Price result is INCOMPLETE, setting to Warning state", operation.getOrder().getFixOrderId());
			checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
			operation.removeLastAttempt();
			operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("EventPriceTimeout.0", priceResult.getReason())), ErrorState.class);
		} 
//		else if (priceResult.getState() == PriceResult.PriceResultState.NULL) {
//			boolean executable = !operation.isNotAutoExecute() && (!operation.getOrder().isLimitFile() || !doNotExecute);
//			ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, priceResult, rejectOrderWhenBloombergIsBest);
//			
//			if (executable && (BondTypesService.isUST(operation.getOrder().getInstrument()) || customerOrder.isLimitFile())) { 
//				// it is an executable UST order and there are no prices on consolidated book
//				csExecutionStrategyService.startExecution(operation, currentAttempt, serialNumberService);
//			} else {
//				Customer customer = customerOrder.getCustomer();
//				// checkOrderAndsetNotAutoExecuteOrder(operation, doNotExecute);
//				try {
//					csExecutionStrategyService.manageAutomaticUnexecution(customerOrder, customer);
//				} catch (BestXException e) {
//					LOGGER.error("Order {}, error while managing {} price result state {}", customerOrder.getFixOrderId(), priceResult.getState().name(), e.getMessage(), e);
//					operation.removeLastAttempt();
//					operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.16")), ErrorState.class);
//				}
//			}
//		}
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
	
	@Override
	public void onMarketOrderBuilt(MarketOrderBuilder builder, MarketOrder marketOrder) {
		Attempt currentAttempt = operation.getLastAttempt();
		Order customerOrder = operation.getOrder();

		if (marketOrder != null) {
		   currentAttempt.setMarketOrder(marketOrder);
		}
		
		this.marketOrderFilterChain.filterMarketOrder(marketOrder, operation);

		ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, this.priceResultReceived);
		
		if (currentAttempt.getNextAction() instanceof GoToErrorStateAction) {
			String errorMessage = ((GoToErrorStateAction) currentAttempt.getNextAction()).getMessage();
			operation.setStateResilient(new WarningState(operation.getState(), null, builder.getName() + ": " + errorMessage), ErrorState.class);
		} else 
		if (currentAttempt.getNextAction() instanceof RejectOrderAction) {
			try {
				String rejectMessage = builder.getName() + ": " + ((RejectOrderAction) currentAttempt.getNextAction()).getRejectReason();
				ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
				operation.setStateResilient(new SendAutoNotExecutionReportState(rejectMessage), ErrorState.class);
			} catch (BestXException e) {
				LOGGER.error("Order {}, error while starting automatic not execution using {}", operation.getOrder().getFixOrderId(), builder.getName(), e);
				String errorMessage = e.getMessage();
				operation.setStateResilient(new WarningState(operation.getState(), null, builder.getName() + ": " + errorMessage), ErrorState.class);
			}
		} else if (currentAttempt.getNextAction() instanceof FreezeOrderAction) {
			try {
				// First of all, reset the first attempt in the cycle
				operation.setFirstAttemptInCurrentCycle(operation.getAttemptNo());
				FreezeOrderAction freezeOrderAction = (FreezeOrderAction) currentAttempt.getNextAction();
				String freezeMessage =  builder.getName() + ": " + freezeOrderAction.getMessage();
				if (freezeOrderAction.getNextPanel() == NextPanel.ORDERS_NO_AUTOEXECUTION) {
					this.operation.setStateResilient(new CurandoState(freezeMessage), ErrorState.class);
				} else if (freezeOrderAction.getNextPanel() == NextPanel.LIMIT_FILE) {
   				OrderHelper.setOrderBestPriceDeviationFromLimit(operation);
   				this.operationStateAuditDao.updateOrderBestAndLimitDelta(operation.getOrder(), operation.getOrder().getBestPriceDeviationFromLimit());
					this.operation.setStateResilient(new OrderNotExecutableState(freezeMessage), ErrorState.class);
				} else if (freezeOrderAction.getNextPanel() == NextPanel.LIMIT_FILE_NO_PRICE) {
					this.operation.setStateResilient(new LimitFileNoPriceState(freezeMessage), ErrorState.class);
				} else {
            		csExecutionStrategyService.manageAutomaticUnexecution(customerOrder, customerOrder.getCustomer(), builder.getName()!= null ? builder.getName() + ": ": defaultStrategyName);
            	}
            } catch (BestXException e) {
               LOGGER.error("Order {}, error while managing {} price result state {}", customerOrder.getFixOrderId(), this.priceResultReceived.getState().name(), e.getMessage(), e);
               operation.removeLastAttempt();
               operation.setStateResilient(new WarningState(operation.getState(), e, builder.getName() + ": " + Messages.getString("PriceService.16")), ErrorState.class);
            }
		} else {
			csExecutionStrategyService.startExecution(operation, currentAttempt, serialNumberService);
		}
	}	

	@Override
	public void onMarketOrderException(MarketOrderBuilder source, Exception ex) {
		try {
			LOGGER.error("Exception received while calling GetRoutingProposal", ex);
			ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
			// TODO Internationalize message
			operation.setStateResilient(new SendAutoNotExecutionReportState(source.getName() + ": Exception during the creation of market order: " + ex.getMessage()), ErrorState.class);
		}
		catch (BestXException e) {
			LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
			String errorMessage = e.getMessage();
			operation.setStateResilient(new WarningState(operation.getState(), null, source.getName() + ":" + errorMessage), ErrorState.class);
		}
		return;
	}

	@Override
	public void onMarketOrderErrors(MarketOrderBuilder source, List<String> errors) {
		try {
			LOGGER.error("Errors received while calling GetRoutingProposal: {}", errors);
			ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
			String errorsString = errors.stream().collect(Collectors.joining(", "));
			operation.setStateResilient(new SendAutoNotExecutionReportState(source.getName() + ":" + errorsString), ErrorState.class);
		}
		catch (BestXException e) {
			LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
			String errorMessage = e.getMessage();
			operation.setStateResilient(new WarningState(operation.getState(), null, source.getName() + ":" + errorMessage), ErrorState.class);
		}
		return;
	}	
	
}