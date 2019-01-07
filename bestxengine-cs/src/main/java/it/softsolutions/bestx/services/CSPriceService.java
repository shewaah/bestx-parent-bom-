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
package it.softsolutions.bestx.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: ruggero.rizzo
 * Creation date: 02/ago/2013
 * 
 **/
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.bestexec.BookSorter;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.MarketPriceConnection;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.CustomerRevokeReceivedException;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.MaxLatencyExceededException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.jmx.JMXNotifier;
import it.softsolutions.bestx.management.PriceServiceMBean;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyService;
import it.softsolutions.bestx.services.executionstrategy.ExecutionStrategyServiceFactory;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.bestx.services.logutils.ApplicationStatisticsHelper;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceResult.PriceResultState;
import it.softsolutions.bestx.services.price.PriceResultBean;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.price.PriceServiceListener;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;
import it.softsolutions.bestx.states.CurandoRetryState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.ManualExecutionWaitingPriceState;
import it.softsolutions.bestx.states.ManualManageState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.manageability.sl.ServiceLibrary;
import it.softsolutions.manageability.sl.monitoring.NumericValueMonitor;

public class CSPriceService extends JMXNotifier implements PriceService, PriceServiceMBean, TimerEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSPriceService.class);

	public static final String RESET_STATISTICS_LABEL = "RESET_STATISTICS_LABEL";

	private List<String> marketsNotEnabled;
	private Map<String, String> magnetMarkets;
	private Map<MarketConnection, Boolean> magnetMarketsEnabled;
	private PriceForgeService priceForgeService;
	private PriceForgeInstrumentsManager priceForgeInstrManager;
	private PriceForgeCustomerManager priceForgeCustManager;
	private List<MarketCode> marketCodes;
	private MarketConnectionRegistry marketConnectionRegistry;
	private VenueFinder venueFinder;
	private Executor executor;
	private BookClassifier bookClassifier;
	private BookSorter bookSorter;
	private final Set<MarketPriceConnection> marketPriceConnections = new HashSet<MarketPriceConnection>();
	private final Set<MarketPriceListener> marketPriceListeners = Collections.synchronizedSet(new HashSet<MarketPriceListener>());
	private AtomicLong lastRequestTime = new AtomicLong(0);
	private int configuredOrdersPerMinute;
	private boolean priceServiceEnable;
	private long timePriceDiscoveryNotifyThreshold;
	private long intervalAvgTimeInSecs;
	private int priceDiscoveryTotalNumber = 0;
	private final NumericValueMonitor queueStandTimeMonitor = new NumericValueMonitor("QueueStandTime", "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]");
	private final NumericValueMonitor priceDiscoveryTotalNumberMonitor = new NumericValueMonitor("PriceDiscoveryTotalNumber", "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]");
	private final NumericValueMonitor timePriceDiscoveryMonitor = new NumericValueMonitor("PriceDiscoveryTime", "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]");
	private long avgTimePriceDiscovery = 0;
	private int priceDiscoveryIntervalNumber = 0;
	private long lastAvgTimePriceDiscovery;
	private String checkDisabledInternalMarketMaker;
	private MarketMakerFinder marketMakerFinder;
	private BookProposalBuilder bookProposalBuilder;
	private MarketFinder marketFinder;
	private String priceServiceName;
	private SerialNumberService serialNumberService;
	private final NumericValueMonitor queueSizeMonitor = new NumericValueMonitor("PriceDiscoveryQueueSize", "Price Service", true, "info", "[PRICE_SERVICE_STATISTICS]");
	private String priceServiceJobName;

	private BlockingQueue<PlainRequest> plainRequests = new LinkedBlockingQueue<CSPriceService.PlainRequest>();
	private Counter pendingJobs;
	private Timer responses;

	private boolean rejectWhenBloombergIsBest;
	private OperationStateAuditDao operationStateAuditDao;
	// ================================================================================
	// new section
	// ================================================================================
	// TODO Monitoring-BX
	private long timePriceDiscoveryNotifyTimeout;

	private boolean variableOrdersPerMinute = false;

	@Override
	public long getTimePriceDiscoveryNotifyTimeout() {
		return this.timePriceDiscoveryNotifyTimeout;
	}

	@Override
	public void setTimePriceDiscoveryNotifyTimeout(long timePriceDiscoveryNotifyTimeout) {
		this.timePriceDiscoveryNotifyTimeout = timePriceDiscoveryNotifyTimeout;
	}
	// ================================================================================
	// end new section
	// ================================================================================

	public void init() throws MarketNotAvailableException {
		checkPreRequisites();
		try {
			ServiceLibrary.attachMonitor(this);
		} catch (Exception e) {
			LOGGER.info("Exception during the monitor attach, ignoring it: {}", e.getMessage());
		}

		priceServiceEnable = true;
		for (MarketCode marketCode : marketCodes) {
			if (marketConnectionRegistry.getMarketConnection(marketCode).getPriceConnection() == null) {
				throw new MarketNotAvailableException("No Market Price Connection Available for: " + marketCode);
			} else {
				marketPriceConnections.add(marketConnectionRegistry.getMarketConnection(marketCode).getPriceConnection());
			}
		}
		resetDiscoveryTimes();

		JobExecutionDispatcher.INSTANCE.addTimerEventListener(CSPriceService.class.getSimpleName(), this);

		try {
			priceServiceJobName = CSPriceService.RESET_STATISTICS_LABEL + "#" + getPriceServiceName();
			SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
			JobDetail newJob = simpleTimerManager.createNewJob(priceServiceJobName, this.getClass().getSimpleName(), false, false, false);
			Trigger trigger = simpleTimerManager.createNewTrigger(priceServiceJobName, this.getClass().getSimpleName(), true, intervalAvgTimeInSecs * 1000);
			simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
		} catch (SchedulerException e) {
			LOGGER.error("Error while scheduling price service statistics timer!", e);
		}

		// this.lastTimerHandle = timerService.setupOnceOnlyTimer(this, this.intervalAvgTimeInSecs*1000, RESET_STATISTICS_LABEL);
		magnetMarketsEnabled = new HashMap<MarketConnection, Boolean>();
		marketsNotEnabled = new CopyOnWriteArrayList<String>();

		queueSizeMonitor.setValue(0);
		ApplicationMonitor.setQueuePricesSize(getPriceServiceName(), 0);

		try {
			pendingJobs = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(CSPriceService.class, priceServiceName + "-pending-jobs"));
			responses = CommonMetricRegistry.INSTANCE.getMonitorRegistry().timer(MetricRegistry.name(CSPriceService.class, priceServiceName + "-responses"));
		} catch (IllegalArgumentException e) {
		}

		new Thread(new RequestProcessor(configuredOrdersPerMinute, this), "CSPriceService-RequestProcessor").start();

	}

	private void checkPreRequisites() throws ObjectNotInitializedException {
		if (marketCodes == null) {
			throw new ObjectNotInitializedException("Market codes not set");
		}
		if (executor == null) {
			throw new ObjectNotInitializedException("Executor not set");
		}
		if (marketConnectionRegistry == null) {
			throw new ObjectNotInitializedException("Market registry not set");
		}
		if (venueFinder == null) {
			throw new ObjectNotInitializedException("Venue Finder not set");
		}
		if (bookClassifier == null) {
			throw new ObjectNotInitializedException("Book classifier not set");
		}
		if (bookSorter == null) {
			throw new ObjectNotInitializedException("Book sorter not set");
		}
		if (marketMakerFinder == null) {
			throw new ObjectNotInitializedException("MarketMakerFinder not set");
		}
		if (bookProposalBuilder == null) {
			throw new ObjectNotInitializedException("bookProposalBuilder not set");
		}
		// if (priceForgeInstrManager == null)
		// throw new ObjectNotInitializedException("PriceForgeInstrumentsManager not set");
		if (marketFinder == null) {
			throw new ObjectNotInitializedException("MarketFinder not set");
		}
	}

	@Override
	public String getName() {
		return getPriceServiceName();
	}

	@Override
	public boolean isUp() {
		return false;
	}

	@Override
	public void setServiceListener(ServiceListener listener) {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public PriceResult getPrices(Instrument instrument, OrderSide orderSide, BigDecimal qty, Date futSettDate, Set<Venue> venues, long maxLatency, Money priceLimit) throws MaxLatencyExceededException {
		throw new UnsupportedOperationException();
	}

	public CSPriceService() {
		super();
	}

	@Override
	public void requestPrices(PriceServiceListener requestor, Order order, List<Attempt> previousAttempts, Set<Venue> venues, long maxLatency) throws MaxLatencyExceededException, BestXException {
		requestPrices(requestor, order, previousAttempts, venues, maxLatency, -1, null);
	}

	private class PlainRequest {
		private PriceServiceListener requestor; 
		private Order order; 
		private List<Attempt> previousAttempts; 
		private Set<Venue> venues; 
		private long maxLatency; 
		private int position; 
		private List<MarketConnection> markets; 
		private boolean excOnRevoke;
		private final Timer.Context context = responses.time();

		public PlainRequest(PriceServiceListener requestor, Order order, List<Attempt> previousAttempts, Set<Venue> venues, long maxLatency, int position, List<MarketConnection> markets,
				boolean excOnRevoke) {
			super();
			this.requestor = requestor;
			this.order = order;
			this.previousAttempts = previousAttempts;
			if (venues==null) venues = new HashSet<Venue>();
			this.venues = venues;
			this.maxLatency = maxLatency;
			this.position = position;
			this.markets = markets;
			this.excOnRevoke = excOnRevoke;
		}
	}

	public void requestPrices(PriceServiceListener requestor, Order order, List<Attempt> previousAttempts, Set<Venue> venues, long maxLatency, int position, List<MarketConnection> markets,
			boolean excOnRevoke) throws MaxLatencyExceededException, BestXException {
		PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Making price request");
		PlainRequest plainRequest = new PlainRequest(requestor, order, previousAttempts, venues, maxLatency, position, markets, excOnRevoke);

		try {
			ApplicationStatisticsHelper.logStringAndUpdateOrderIds(order, "Order.Queue_" + priceServiceName + ".In." + order.getInstrumentCode(), getClass().getName());

			LOGGER.debug("{} put {}", priceServiceName, plainRequest.order.getFixOrderId());
			pendingJobs.inc();
			plainRequests.put(plainRequest);
		} catch (InterruptedException e) {
			LOGGER.error("{}", e.getMessage(), e);
		}
	}

	private class RequestProcessor implements Runnable {

		private long ordersPerMinutes;
		PriceService ps;

		/**
		 * @param ordersPerMinutes
		 */
		public RequestProcessor(long ordersPerMinutes, PriceService ps) {
			super();
			this.ordersPerMinutes = ordersPerMinutes;
			this.ps = ps;
		}

	    private void setNotAutoExecuteOrder(Operation operation) {
    		operation.setNotAutoExecute(false);
	    }
	    
		@Override
		public void run() {
			try {

				do {
					long sleepMillis = 60000 / ordersPerMinutes;
					if (sleepMillis > 10) {
						Thread.sleep(sleepMillis);
					}

					PlainRequest plainRequest = plainRequests.take();
					pendingJobs.dec();
					plainRequest.context.stop();
					Operation operation = (Operation) plainRequest.requestor;
					LOGGER.debug("[{}] processing order {}, queueSize = {}", priceServiceName, plainRequest.order.getFixOrderId(), plainRequests.size());

					try {
						internalRequestPrices(plainRequest.requestor, plainRequest.order, plainRequest.previousAttempts, plainRequest.venues, plainRequest.maxLatency, plainRequest.position, plainRequest.markets, plainRequest.excOnRevoke);
					} catch (MarketNotAvailableException e) {
						/*
						 * This exception is thrown if : - there are no price connections enabled - there are some price connections not enabled and in
						 * the other markets the isin is not quoted
						 */
						LOGGER.info("Order {}, an error occurred while calling PriceService: {}", plainRequest.order.getFixOrderId(), e.getMessage(), e);
						Boolean notExecuteLimitFile = CSConfigurationPropertyLoader.getBooleanProperty(CSConfigurationPropertyLoader.LIMITFILE_DONOTEXECUTE, false);

						if (notExecuteLimitFile && !operation.isVolatile()) {
							operation.setNotAutoExecute(false);						}
						// [DR20140122] Tentativo di risolvere i problemi con gli attempt nei casi di LimitFile che vanno da CurandoRetry in LimitFileNoPrice
						if (operation.getAttemptNo() > 1 && !operation.getOrder().isLimitFile()) {
							operation.removeLastAttempt();
						}
						LOGGER.debug("Order {}, isLimitFile {}", operation.getOrder().getFixOrderId(), operation.getOrder().isLimitFile());
						// [RR20140805] CRSBXTEM-111: LF Never executing. Only add an attempt to display correctly order coming from CurandoRetry and going into LimitFileNoPrice
						if (operation.getState() instanceof CurandoRetryState && operation.getOrder().isLimitFile()) {
							LOGGER.debug("Order {}, add attempt to a limitfile order.", operation.getOrder().getFixOrderId());
							operation.addAttempt();
						}

						if (operation.getState() instanceof ManualExecutionWaitingPriceState) {
							operation.setStateResilient(new ManualManageState(false), ErrorState.class);
						} else {
							Customer customer = operation.getOrder().getCustomer();
							try {
								ExecutionStrategyService csExecutionStrategyService = ExecutionStrategyServiceFactory.getInstance().getExecutionStrategyService(operation.getOrder().getPriceDiscoveryType(), operation, null, rejectWhenBloombergIsBest);
								csExecutionStrategyService.manageAutomaticUnexecution(plainRequest.order, customer);
							} catch (BestXException be) {
								LOGGER.error("Order {}, error while managing no market available situation {}", plainRequest.order.getFixOrderId(), e.getMessage(), e);
								operation.removeLastAttempt();
								operation.setStateResilient(new WarningState(operation.getState(), e, Messages.getString("PriceService.15")), ErrorState.class);
							} catch (Exception ex) {
								LOGGER.error("{}", ex.getMessage(), ex);
							}
						}
					} catch(CustomerRevokeReceivedException e1){
						// AMC BESTX-313 20170719
						PriceResultBean priceResult = new PriceResultBean();
						priceResult.setReason("Cancel received - no Price Discovery performed");
						priceResult.setSortedBook(null);
						priceResult.setState(PriceResultState.NULL);
						operation.onPricesResult(ps, priceResult);
					}catch (Exception e) {
						LOGGER.error("{}", e.getMessage(), e);
					}
				} while (true);
			} catch (InterruptedException e) {
				LOGGER.error("{}", e.getMessage(), e);
			}
		}
	}

	@Override
	public void requestPrices(PriceServiceListener requestor, Order order, List<Attempt> previousAttempts, Set<Venue> venues, long maxLatency, int position, List<MarketConnection> markets)
			throws MaxLatencyExceededException, BestXException {
		requestPrices(requestor, order, previousAttempts, venues, maxLatency, position, markets, true);
	}

	private void internalRequestPrices(PriceServiceListener requestor, final Order order, List<Attempt> previousAttempts, final Set<Venue> venues, final long maxLatency, int position, List<MarketConnection> markets,
			boolean excOnRevoke) throws MaxLatencyExceededException, MarketNotAvailableException, BestXException {

		Operation operation = (Operation) requestor;

		// 20100212 Ruggero : I-41 Revoche Automatiche
		// Exiting from the queue, before going on asking for prices, check if we have received a revoke for this operation.
		// The requestor is an Operation object.
		// AMC 20170330 BXSUP-2028 test for regressions!
		if (operation.isCustomerRevokeReceived() && excOnRevoke) {
			CustomerRevokeReceivedException revokeEx = new CustomerRevokeReceivedException("Customer revoke received while going into the price discovery, start revoking process.");
			LOGGER.info("Received cancel request for order {}, no price discovery will be performed.", order.getFixOrderId());
			throw revokeEx;
		}
		PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Starting price request");

		// [DR20150316] [CRSBXTEM-160] Modifica della velocità di scodamento degli ordini LF
		operation.startTimer();

		priceDiscoveryTotalNumber++; // JMX
		priceDiscoveryTotalNumberMonitor.setValue(priceDiscoveryTotalNumber);
		// Clear all the order's lists because a new price discovery is starting
		order.clearMarketMakerNotQuotingInstr();
		order.clearMarketNotNegotiatingInstr();
		order.clearMarketNotQuotingInstr();
		String fixOrderId = order.getFixOrderId();
		String isin = order.getInstrumentCode();
		queueSizeMonitor.setValue(plainRequests.size());
		LOGGER.info("[PRICESRVQ] Order {}, queueSize = {}", fixOrderId, plainRequests.size());
		/*
		 * set to NONE the variable that tells us if we have already done the query for knowing if the order is a matching or not (used in
		 * the DiscarWorstPriceProposalClassifier)
		 */
		order.setIsMatchingFromQuery(Order.IsMatchingFromQueryStates.NONE);
		Date startQueue = DateService.newLocalDate();

		// inizializza l'ora dell'ultima richiesta
		lastRequestTime.set(DateService.currentTimeMillis());
		ApplicationStatisticsHelper.logStringAndUpdateOrderIds(order, "Order.Queue_" + priceServiceName + ".Out." + order.getInstrumentCode(), getClass().getName());
		queueSizeMonitor.setValue(plainRequests.size());
		LOGGER.debug("Price Service - Order queue elements: {}", plainRequests.size());
		long queueStandTime = DateService.newLocalDate().getTime() - startQueue.getTime();
		queueStandTimeMonitor.setValue(queueStandTime);
		LOGGER.info("[PRICESRVQ],EndQueue={}, PriceDiscoveryTotalNumber from start: {}", queueStandTime, priceDiscoveryTotalNumber);
		LOGGER.debug("START Request prices: {} - maxLatency: {}", isin, maxLatency);

		marketsNotEnabled.clear();
		ArrayList<MarketPriceConnection> marketPriceConnectionsEnabled = new ArrayList<MarketPriceConnection>();
		if (markets == null) {
			markets = marketConnectionRegistry.getAllMarketConnections(order);
		}
		List<String> unavailableMarketErrors = new ArrayList<String>();
		for (MarketConnection marketConnection : markets) {
			boolean marketIsInPolicy = isMarketInPolicy(venues, fixOrderId, marketConnection);
			if (!marketIsInPolicy) {
				LOGGER.info("Order {}, market {} will not be enquired for prices because it is not in policy.", fixOrderId, marketConnection.getMarketCode());
				continue;
			}
			if (marketConnection.isAMagnetMarket()) {
				magnetMarketsEnabled.put(marketConnection, true);
			}
			if (marketConnection.isPriceConnectionProvided()) {
				if (!marketConnection.isPriceConnectionEnabled()) { //marketConnection.enablePriceConnection()
					LOGGER.info("Order {}, market not enabled: {}", fixOrderId, marketConnection.getMarketCode());
					marketsNotEnabled.add(marketConnection.getMarketCode().name());
					// mark the magnet market as disabled
					if (marketConnection.isAMagnetMarket() && magnetMarketsEnabled.containsKey(marketConnection)) {
						magnetMarketsEnabled.put(marketConnection, false);
					}
					unavailableMarketErrors.add(marketConnection.getMarketCode()+" is disabled");
					continue;
				}
				if (marketConnection.isPriceConnectionAvailable()) {
					QuotingStatus instrQuotingStatus = marketConnection.getPriceConnection().getInstrumentQuotingStatus(order.getInstrument()); // throws
					// BestXException;
					/*
					 * Ruggero - 20111013 The price connection available is not enough to consider the market as enabled. We must also check
					 * if it has reached the idle status, it means that we must be sure that the xt2 gateway is synchronized and connected
					 * correctly.
					 */
					if (!marketConnection.isMarketIdle()) {
						LOGGER.info("Order {}, Market {} not idle.", fixOrderId, marketConnection.getMarketCode());
						marketsNotEnabled.add(marketConnection.getMarketCode().name());
						order.addMarketNotQuotingInstr(marketConnection.getMarketCode(), Messages.getString("RejectProposalMarketStatusNotIdle"));
						unavailableMarketErrors.add(marketConnection.getMarketCode()+" not available");
						continue;
					}
					if (instrQuotingStatus == null) { // instrument not available on market
						LOGGER.info("Order {}, Instrument {} not quoted on market: {}", fixOrderId, order.getInstrumentCode(), marketConnection.getMarketCode());
						// save the markets that do not quote the instrument
						LOGGER.debug("Order {}, adding the market {} to those not quoting the instrument.", fixOrderId, marketConnection.getMarketCode());
						order.addMarketNotQuotingInstr(marketConnection.getMarketCode(), Messages.getString("RejectProposalISINNotListedInMarket"));
						if (LOGGER.isDebugEnabled()) {
							for (MarketCode mktCode : order.getMarketNotQuotingInstr().keySet()) {
								LOGGER.debug("Order {}, market not quoting {} : {}", fixOrderId, isin, mktCode.name());
							}
						}
						continue;
					} else if ((instrQuotingStatus != null) && (instrQuotingStatus != QuotingStatus.NEG)) { // instrument available on
						// market, but not negotiable
						LOGGER.info("Order {}, Instrument {} not negotiable on market: {}", fixOrderId, order.getInstrument().getIsin(), marketConnection.getMarketCode());
						// save the markets that do not quote the instrument
						LOGGER.debug("Order {}, adding the market {} to those not negotiating the instrument.", fixOrderId, marketConnection.getMarketCode());
						order.addMarketNotNegotiatingInstr(marketConnection.getMarketCode(), Messages.getString("RejectProposalISINNotNegotiableOnMarket"));
						if (LOGGER.isDebugEnabled()) {
							for (MarketCode mktCode : order.getMarketNotNegotiatingInstr().keySet()) {
								LOGGER.debug("Order {}, market not negotiating {} : {}", fixOrderId, isin, mktCode.name());
							}
						}
						continue;
					}
					marketPriceConnectionsEnabled.add(marketConnection.getPriceConnection());
				} else {
					unavailableMarketErrors.add(marketConnection.getMarketCode()+" unavailable");
				}
			}
		}

		if (marketPriceConnectionsEnabled.size() == 0) {
			LOGGER.info("Order {}, using the {}, No market price connection is enabled", order.getFixOrderId(), order.getPriceDiscoverySelected());
			MarketNotAvailableException mktNotAvailableEx = new MarketNotAvailableException(Messages.getString("MarketsNotAvailable.1"));
			mktNotAvailableEx.setMarketsNotEnabled(marketsNotEnabled);
			throw mktNotAvailableEx;
		}

		// Siccome non siamo riusciti a caricare le venues del cliente, allora facciamo l'interrogazione su tutte le venue
		if (venues.size() == 0) {
			LOGGER.warn("Unable to retrieve the venues for order {}, load all the venues", order.getFixOrderId());
			venues.addAll(venueFinder.getAllVenues());
		}
		final MarketPriceListener marketPriceListener = new MarketPriceListener(requestor, order.getInstrument(), order, previousAttempts, venues, marketPriceConnectionsEnabled, this, executor,
				bookClassifier, bookSorter, marketMakerFinder, bookProposalBuilder, marketFinder, checkDisabledInternalMarketMaker, venueFinder, priceForgeService,
				priceForgeCustManager, priceForgeInstrManager, serialNumberService);
		marketPriceListeners.add(marketPriceListener);
		marketPriceListener.addErrorsInReport(unavailableMarketErrors);
		int marketsNotAvailable = 0;
		ArrayList<MarketPriceConnection> marketPriceConnectionsNotEnabled = new ArrayList<MarketPriceConnection>();
		for (final MarketPriceConnection marketPriceConnection : marketPriceConnectionsEnabled) {
			LOGGER.info("[REQPRICE] Order {} START request price for market {}", fixOrderId, marketPriceConnection.getMarketCode());
			try {
				marketPriceConnection.ensurePriceAvailable();
			} catch (MarketNotAvailableException mae) {
				LOGGER.info("[REQPRICE] Order {} Market {} not available.", fixOrderId, marketPriceConnection.getMarketCode(), mae);
				marketPriceConnectionsNotEnabled.add(marketPriceConnection);
				marketPriceListener.addErrorInReport(marketPriceConnection.getMarketCode()+" not available");
				marketPriceListener.getRemainingMarketPriceConnections().remove(marketPriceConnection);
				marketsNotAvailable++;
			}
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						marketPriceConnection.queryPrice(marketPriceListener, venues, maxLatency, order);
					} catch (BestXException e) {
						LOGGER.warn("[REQPRICE] Exception caught on query price for market {}.",  marketPriceConnection.getMarketCode(), e);
					}
				}
			});
			LOGGER.info("[REQPRICE] Order {} END request price for market {}", fixOrderId, marketPriceConnection.getMarketCode());
		}
		if (marketsNotAvailable > 0 && marketsNotAvailable == marketPriceConnectionsEnabled.size()) {
			LOGGER.info("All the markets with price connection enabled are not available!");
			MarketNotAvailableException mktNotAvailableEx = new MarketNotAvailableException(Messages.getString("MarketsNotAvailable.1"));
			throw mktNotAvailableEx;
		}
		marketPriceConnectionsEnabled.removeAll(marketPriceConnectionsNotEnabled);

		PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Price requested");
	}

	@Override
	public int getOrdersPerMinute() {
		return configuredOrdersPerMinute;
	}

	@Override
	public void setOrdersPerMinute(int ordersPerMinute) {
		LOGGER.info("[{}] Changed number of orders per minute from {} to {}", priceServiceName, ordersPerMinute, ordersPerMinute);
		this.configuredOrdersPerMinute = ordersPerMinute;
	}

	@Override
	public Date getLastRequestTime() {
		return new Date(lastRequestTime.get());
	}

	@Override
	public boolean isPriceServiceEnable() {
		return priceServiceEnable;
	}

	@Override
	public int getPriceRequestsNumber() {
		return plainRequests.size();
	}

	@Override
	public void disablePriceService() {
		LOGGER.info("Price Service - Stopped");
		priceServiceEnable = false;
	}

	@Override
	public void enablePriceService() {
		LOGGER.info("Price Service - Started");
		priceServiceEnable = true;
	}

	@Override
	public long getAvgTimePriceDiscovery() {
		return lastAvgTimePriceDiscovery;
	}

	@Override
	public long getIntervalAvgTimeInSecs() {
		return intervalAvgTimeInSecs;
	}

	@Override
	public int getPriceDiscoveryTotalNumber() {
		return priceDiscoveryTotalNumber;
	}

	@Override
	public long getTimePriceDiscoveryNotifyThreshold() {
		return timePriceDiscoveryNotifyThreshold;
	}

	@Override
	public void setTimePriceDiscoveryNotifyThreshold(long timePriceDiscoveryNotifyThreshold) {
		this.timePriceDiscoveryNotifyThreshold = timePriceDiscoveryNotifyThreshold;
	}

	@Override
	public void setIntervalAvgTimeInSecs(long intervalAvgTimeInSecs) {
		this.intervalAvgTimeInSecs = intervalAvgTimeInSecs;
	}

	@Override
	public void addNewTimePriceDiscovery(long newDiscoveryTime) {

		timePriceDiscoveryMonitor.setValue(newDiscoveryTime);
		avgTimePriceDiscovery = (priceDiscoveryIntervalNumber * avgTimePriceDiscovery + newDiscoveryTime) / ++priceDiscoveryIntervalNumber;

		LOGGER.info("[MONITOR] Price Discovery Time: {}", newDiscoveryTime);

		if (newDiscoveryTime > timePriceDiscoveryNotifyThreshold) {
			LOGGER.warn("[MONITOR] Price Discovery threshold exceeded: threshold = {}, time = {}", timePriceDiscoveryNotifyThreshold, newDiscoveryTime);
			notifyEvent("Price Discovery threshold exceeded", "threshold = " + timePriceDiscoveryNotifyThreshold + " time = " + newDiscoveryTime);
		}
	}

	public synchronized void resetDiscoveryTimes() {
		lastAvgTimePriceDiscovery = avgTimePriceDiscovery;
		priceDiscoveryIntervalNumber = 0;
		avgTimePriceDiscovery = 0;
	}

	public void setPriceForgeService(PriceForgeService priceForgeService) {
		this.priceForgeService = priceForgeService;
	}

	public List<String> getMarketsNotEnabled() {
		LOGGER.debug("Getting the markets that are not enabled.");
		return marketsNotEnabled;
	}

	public Map<String, String> getMagnetMarkets() {
		return magnetMarkets;
	}

	public void setMagnetMarkets(Map<String, String> magnetMarkets) {
		this.magnetMarkets = magnetMarkets;
	}

	public Map<MarketConnection, Boolean> getMagnetMarketsEnabled() {
		return magnetMarketsEnabled;
	}

	public void setMagnetMarketsEnabled(Map<MarketConnection, Boolean> magnetMarketsEnabled) {
		this.magnetMarketsEnabled = magnetMarketsEnabled;
	}

	public Map<String, String> getMarketsStartingState() {
		return magnetMarkets;
	}

	/**
	 * Check if the market connection is that of a market which is in the customer's policy. For a market like BV, BBG and TW that are not
	 * explicitly in the policy, we must verify if it is the market of one of the policy market makers.
	 * 
	 * @param venues
	 * @param fixOrderId
	 * @param marketConnection
	 * @return
	 */
	public boolean isMarketInPolicy(Set<Venue> venues, String fixOrderId, MarketConnection marketConnection) {
		boolean marketIsInPolicy = false;
		MarketCode marketCode = marketConnection.getMarketCode();
		for (Venue venue : venues) {
			if (venue.isMarket()) {
				MarketCode venueMktCode = venue.getMarket().getMarketCode();
				if (marketCode.equals(venueMktCode)) {
					LOGGER.debug("Order {}, market {} found among the policy venues.", fixOrderId, marketCode);
					marketIsInPolicy = true;
					break;
				}
			} else {
				/*
				 * If the venue of this iteration is not a market then it is a market maker. Check if the market of one of its market market
				 * makers is that of the market connection.
				 */
				MarketMaker marketMaker = venue.getMarketMaker();
				Set<MarketMarketMaker> marketMarketMakers = marketMaker.getMarketMarketMakers();
				for (MarketMarketMaker mmm : marketMarketMakers) {
					MarketCode venueMktCode = mmm.getMarket().getMarketCode();
					if (marketCode.equals(venueMktCode)) {
						LOGGER.debug("Order {}, market {} found among the policy venues, is the market of one of the policy market makers ({}).", fixOrderId, marketCode, mmm);
						marketIsInPolicy = true;
						break;
					}
				}
				if (marketIsInPolicy) {
					break;
				}
			}
		}
		return marketIsInPolicy;
	}

	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	public Set<MarketPriceListener> getMarketPriceListeners() {
		return marketPriceListeners;
	}

	@Override
	public String getPriceServiceName() {
		return priceServiceName;
	}

	public void setPriceServiceName(String priceServiceName) {
		this.priceServiceName = priceServiceName;
	}

	public void setSerialNumberService(SerialNumberService serialNumberService) {
		this.serialNumberService = serialNumberService;
	}

	public SerialNumberService getSerialNumberService() {
		return serialNumberService;
	}

	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
		this.marketConnectionRegistry = marketConnectionRegistry;
	}

	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	public void setMarketCodes(List<MarketCode> marketCodes) {
		this.marketCodes = marketCodes;
	}

	public void setBookClassifier(BookClassifier bookClassifier) {
		this.bookClassifier = bookClassifier;
	}

	public void setBookSorter(BookSorter bookSorter) {
		this.bookSorter = bookSorter;
	}

	public String getCheckDisabledInternalMarketMaker() {
		return checkDisabledInternalMarketMaker;
	}

	public void setCheckDisabledInternalMarketMakers(String checkDisabledMarketMaker) {
		this.checkDisabledInternalMarketMaker = checkDisabledMarketMaker;
	}

	public MarketMakerFinder getMarketMakerFinder() {
		return marketMakerFinder;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public BookProposalBuilder getBookProposalBuilder() {
		return bookProposalBuilder;
	}

	public void setBookProposalBuilder(BookProposalBuilder bookProposalBuilder) {
		this.bookProposalBuilder = bookProposalBuilder;
	}

	public void setPriceForgeInstrManager(PriceForgeInstrumentsManager priceForgeInstrManager) {
		this.priceForgeInstrManager = priceForgeInstrManager;
	}

	public void setPriceForgeCustManager(PriceForgeCustomerManager priceForgeCustManager) {
		this.priceForgeCustManager = priceForgeCustManager;
	}

	@Override
	public void timerExpired(String jobName, String groupName) {
		if (jobName.equals(priceServiceJobName)) {
			LOGGER.info("[STATISTICS] Interval Average Price Discovery Time: " + avgTimePriceDiscovery + " in interval: " + intervalAvgTimeInSecs + " calculated on " + priceDiscoveryIntervalNumber
					+ " samples");
			resetDiscoveryTimes();
		}
	}

	/**
	 * @param rejectWhenBloombergIsBest the rejectWhenBloombergIsBest to set
	 */
	public void setRejectWhenBloombergIsBest(boolean rejectWhenBloombergIsBest) {
		this.rejectWhenBloombergIsBest = rejectWhenBloombergIsBest;
	}

	@Override
	public String getImplementationVersion() {
		String res = "N/A";

		Class<?> clazz = CSPriceService.class;
		String className = clazz.getSimpleName() + ".class";
		String classPath = clazz.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
			// Class not from JAR
			LOGGER.debug("Jar not found for class [" + className + "]");
			return res;
		}
		String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
		Manifest manifest;
		try {
			manifest = new Manifest(new URL(manifestPath).openStream());
		} catch (IOException e) {
			LOGGER.error("{}", e.getMessage(), e);
			return res;
		}
		Attributes attr = manifest.getMainAttributes();

		res = attr.getValue("Implementation-Version");
		LOGGER.debug("Implementation-Version = {}", res);

		return res;

	}

	/**
	 * @return the operationStateAuditDao
	 */
	public OperationStateAuditDao getOperationStateAuditDao() {
		return operationStateAuditDao;
	}

	/**
	 * @param operationStateAuditDao the operationStateAuditDao to set
	 */
	public void setOperationStateAuditDao(OperationStateAuditDao operationStateAuditDao) {
		this.operationStateAuditDao = operationStateAuditDao;
	}

	/**
	 * @param variableOrdersPerMinute the variableOrdersPerMinute to set
	 */
	public void setVariableOrdersPerMinute(boolean variableOrdersPerMinute) {
		this.variableOrdersPerMinute = variableOrdersPerMinute;
	}
}