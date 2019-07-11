
/*
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.handlers.BusinessValidationEventHandler;
import it.softsolutions.bestx.handlers.CSBaseOperationEventHandler;
import it.softsolutions.bestx.handlers.CSCurandoEventHandler;
import it.softsolutions.bestx.handlers.CSExecutionReportHelper;
import it.softsolutions.bestx.handlers.CSInitialEventHandler;
import it.softsolutions.bestx.handlers.CSOrderNotExecutableEventHandler;
import it.softsolutions.bestx.handlers.CSRejectedEventHandler;
import it.softsolutions.bestx.handlers.CSSendExecutionReportEventHandler;
import it.softsolutions.bestx.handlers.CSSendPOBExEventHandler;
import it.softsolutions.bestx.handlers.FormalValidationKOEventHandler;
import it.softsolutions.bestx.handlers.FormalValidationOkEventHandler;
import it.softsolutions.bestx.handlers.LimitFileHelper;
import it.softsolutions.bestx.handlers.LimitFileNoPriceEventHandler;
import it.softsolutions.bestx.handlers.ManualExecutionWaitingPriceEventHandler;
import it.softsolutions.bestx.handlers.ManualManageEventHandler;
import it.softsolutions.bestx.handlers.ManualWaitingFillEventHandler;
import it.softsolutions.bestx.handlers.MonitorEventHandler;
import it.softsolutions.bestx.handlers.MultipleQuotesHandler;
import it.softsolutions.bestx.handlers.OrderReceivedEventHandler;
import it.softsolutions.bestx.handlers.OrderRejectableEventHandler;
import it.softsolutions.bestx.handlers.OrderRevocatedEventHandler;
import it.softsolutions.bestx.handlers.ParkedOrderEventHandler;
import it.softsolutions.bestx.handlers.PriceDiscoveryEventHandler;
import it.softsolutions.bestx.handlers.SendNotExecutionReportEventHandler;
import it.softsolutions.bestx.handlers.TerminalEventHandler;
import it.softsolutions.bestx.handlers.ValidateByPunctualFilterEventHandler;
import it.softsolutions.bestx.handlers.WaitingPriceEventHandler;
import it.softsolutions.bestx.handlers.WarningEventHandler;
import it.softsolutions.bestx.handlers.bloomberg.BBG_ExecutedEventHandler;
import it.softsolutions.bestx.handlers.bloomberg.BBG_RejectedEventHandler;
import it.softsolutions.bestx.handlers.bloomberg.BBG_SendEnquiryEventHandler;
import it.softsolutions.bestx.handlers.bloomberg.BBG_StartExecutionEventHandler;
import it.softsolutions.bestx.handlers.bloomberg.UnreconciledTradeEventHandler;
import it.softsolutions.bestx.handlers.bondvision.BV_SendOrderEventHandler;
import it.softsolutions.bestx.handlers.bondvision.BV_SendRFCQEventHandler;
import it.softsolutions.bestx.handlers.bondvision.BV_StartExecutionEventHandler;
import it.softsolutions.bestx.handlers.internal.INT_ExecutedEventHandler;
import it.softsolutions.bestx.handlers.internal.INT_ManageCounterEventHandler;
import it.softsolutions.bestx.handlers.internal.INT_RejectedEventHandler;
import it.softsolutions.bestx.handlers.internal.INT_StartExecutionEventHandler;
import it.softsolutions.bestx.handlers.marketaxess.MA_CancelledEventHandler;
import it.softsolutions.bestx.handlers.marketaxess.MA_ExecutedEventHandler;
import it.softsolutions.bestx.handlers.marketaxess.MA_RejectedEventHandler;
import it.softsolutions.bestx.handlers.marketaxess.MA_SendOrderEventHandler;
import it.softsolutions.bestx.handlers.marketaxess.MA_StartExecutionEventHandler;
import it.softsolutions.bestx.handlers.matching.MATCH_ExecutedEventHandler;
import it.softsolutions.bestx.handlers.tradeweb.TW_CancelledEventHandler;
import it.softsolutions.bestx.handlers.tradeweb.TW_ExecutedEventHandler;
import it.softsolutions.bestx.handlers.tradeweb.TW_RejectedEventHandler;
import it.softsolutions.bestx.handlers.tradeweb.TW_SendOrderEventHandler;
import it.softsolutions.bestx.handlers.tradeweb.TW_StartExecutionEventHandler;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.services.BookDepthValidator;
import it.softsolutions.bestx.services.CSBookDepthController;
import it.softsolutions.bestx.services.CSConfigurationPropertyLoader;
import it.softsolutions.bestx.services.CommissionService;
import it.softsolutions.bestx.services.ExecutionDestinationService;
import it.softsolutions.bestx.services.MarketSecurityStatusService;
import it.softsolutions.bestx.services.OrderValidationService;
import it.softsolutions.bestx.services.PriceServiceProvider;
import it.softsolutions.bestx.services.TitoliIncrociabiliService;
import it.softsolutions.bestx.services.customservice.CustomService;
import it.softsolutions.bestx.services.grdlite.GRDLiteService;
import it.softsolutions.bestx.services.instrumentstatus.InstrumentStatusNotifier;
import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.autocurando.AutoCurandoStatus;

/**
 * 
 * Purpose: this class manages the state change and the loading of the right
 * handler for the given state
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date:
 * 06/lug/2012
 * 
 **/
public class CSStrategy implements Strategy, SystemStateSelector, Modality {

	private static final Logger LOGGER = LoggerFactory.getLogger(CSStrategy.class);
	private static final long LIMIT_FILE_MINIMUM_PD_INTERVAL = 15;
	private TitoliIncrociabiliService titoliIncrociabiliService = null;
	private MarketConnectionRegistry marketConnectionRegistry;
	private int marketCommunicationTimeout;
	private long waitPriceTimeoutMSec;
	private volatile boolean orderEnabled;
	private volatile String stateDescription;
	private OrderValidationService orderValidationService;
	@SuppressWarnings("unused")
	private OrderValidationService priceDiscoveryValidationService;
	private SerialNumberService serialNumberService;
	private MifidConfig mifidConfig;
	private CustomerConnection customerConnection;
	private OperatorConsoleConnection operatorConsoleConnection;
	private CustomerFinder customerFinder;
	private MarketFinder marketFinder;
	private VenueFinder venueFinder;
	private MarketMakerFinder marketMakerFinder;
	private MarketSecurityStatusService marketSecurityStatusService;
	@SuppressWarnings("unused")
	private long bbgWaitFillMSec;
	@SuppressWarnings("unused")
	private long bbgFillPollingMSec;
	private long manualWaitFillMSec;
	private long manualFillPollingMSec;
	private int curandoRetryTimeout;
	private long marketPriceTimeout;
	private long priceDiscoveryTimeout;
	private long marketAxessExecTimeout = 10 * 60 * 1000;
	private long marketExecTimeout;
	private long internalRfqReplyTimeout;
	private String internalRfqMessagePrefix;

	private int mtsCreditExecTimeout;
	private long bondVisionExecTimeout = 120 * 1000;
	private int sendExecRepTimeout;
	private String matchingMMcode;
	private List<String> internalMMcodesList;
	private CommissionService commissionService;
	private RegulatedMktIsinsLoader regulatedMktIsinsLoader;
	private AutoCurandoStatus autoCurandoStatus;
	private List<String> regulatedMarketPolicies;
	private OperationRegistry operationRegistry;
	private InstrumentStatusNotifier instrStatusNotifier;
	private ExecutionDestinationService executionDestinationService;
	private long waitingCMFTimeout = 90000; // default value is 90 seconds
	private PriceServiceProvider priceServiceProvider;
	private boolean rejectWhenBloombergIsBest;
	private int minimumRequiredBookDepth = 3;
	private BookDepthValidator bookDepthValidator = new CSBookDepthController(minimumRequiredBookDepth);

	private Map<String, MultipleQuotesHandler> multipleQuotesHandlers = new HashMap<String, MultipleQuotesHandler>();
	private long grdLiteLoadResponseTimeout;
	private GRDLiteService grdLiteService;
	private InstrumentFinder instrumentFinder;
	private long limitFileBestWithinLimitThresholdPDIntervalInSeconds;
	private long limitFileBestOutsideLimitThresholdPDIntervalInSeconds;
	private String limitFilePdTimes;
	private List<Long> limitFileNPPriceDiscoveryTimes = new ArrayList<Long>();

	private String limitFileCommentPrefix;
	private String limitFileNoPriceCommentPrefix;
	@SuppressWarnings("unused")
	private int limitFileCommentMaxLen;
	private OperationStateAuditDao operationStateAuditDao;
	private Set<String> tsoxTechnicalRejectReasons = new HashSet<String>();
	// order cancel delay timer, initially introduced for CS Tradeweb market
	private long orderCancelDelay;
	private BestXConfigurationDao bestXConfigurationDao;
	// Price discovery
	private OperatorConsoleConnection priceDiscoveryConnection;
	private Customer priceDiscoveryCustomer;
	private Integer bookDepth;
	private Integer orderBookDepth;
	private String priceDiscoveryCustomerId;
	private int priceDecimals;
	private int pobExMaxSize;
	private int targetPriceMaxLevel;
	private Modality.Type modality;

	public long getBondVisionExecTimeout() {
		return bondVisionExecTimeout;
	}

	public void setBondVisionExecTimeout(long bondVisionExecTimeout) {
		this.bondVisionExecTimeout = bondVisionExecTimeout;
	}

	public int getTargetPriceMaxLevel() {
		return targetPriceMaxLevel;
	}

	public void setTargetPriceMaxLevel(int targetPriceMaxLevel) {
		this.targetPriceMaxLevel = targetPriceMaxLevel;
	}

//20180925 - SP - BESTX-352
	private CurandoTimerRetriever curandoTimerRetriever;

	public int getPobExMaxSize() {
		return pobExMaxSize;
	}

	public void setPobExMaxSize(int pobExMaxSize) {
		this.pobExMaxSize = pobExMaxSize;
	}

	public Integer getOrderBookDepth() {
		return orderBookDepth;
	}

	public void setOrderBookDepth(Integer orderBookDepth) {
		this.orderBookDepth = orderBookDepth;
	}

	public Integer getBookDepth() {
		return bookDepth;
	}

	public void setBookDepth(Integer bookDepth) {
		this.bookDepth = bookDepth;
	}

	public String getPriceDiscoveryCustomerId() {
		return priceDiscoveryCustomerId;
	}

	public void setPriceDiscoveryCustomerId(String priceDiscoveryCustomerId) {
		this.priceDiscoveryCustomerId = priceDiscoveryCustomerId;
	}

	/**
	 * Set the order validation service.
	 * 
	 * @param orderValidationService : order validation service
	 */
	public void setOrderValidationService(OrderValidationService orderValidationService) {
		this.orderValidationService = orderValidationService;
	}

	/**
	 * Instantiates a new cS strategy.
	 */
	public CSStrategy() {
		setStateDescription("OK");
		setRfqEnabled(true);
		setOrderEnabled(true);
	}

	/**
	 * Set the market connection regtistry.
	 * 
	 * @param marketConnectionRegistry : the market connection registry
	 */
	public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
		this.marketConnectionRegistry = marketConnectionRegistry;
	}

	/**
	 * Set the titoli incorciabili service.
	 * 
	 * @param titoliIncrociabiliService : the service
	 */
	public void setTitoliIncrociabiliService(TitoliIncrociabiliService titoliIncrociabiliService) {
		this.titoliIncrociabiliService = titoliIncrociabiliService;
	}

	/**
	 * Set the serial number service.
	 * 
	 * @param serialNumberService : the service
	 */
	public void setSerialNumberService(SerialNumberService serialNumberService) {
		this.serialNumberService = serialNumberService;
	}

	/**
	 * Set the customer connection.
	 * 
	 * @param customerConnection the customerConnection to set
	 */
	public void setCustomerConnection(CustomerConnection customerConnection) {
		this.customerConnection = customerConnection;
	}

	/**
	 * Set the operator console connection.
	 * 
	 * @param operatorConsoleConnection : the connection
	 */
	public void setOperatorConsoleConnection(OperatorConsoleConnection operatorConsoleConnection) {
		this.operatorConsoleConnection = operatorConsoleConnection;
	}

	/**
	 * Set the mifid config.
	 * 
	 * @param mifidConfig : the mifid config
	 */
	public void setMifidConfig(MifidConfig mifidConfig) {
		this.mifidConfig = mifidConfig;
	}

	/**
	 * Set the market finder.
	 * 
	 * @param marketFinder the marketFinder to set
	 */
	public void setMarketFinder(MarketFinder marketFinder) {
		this.marketFinder = marketFinder;
	}

	/**
	 * Set the length of the bloomberg waiting fill timer.
	 * 
	 * @param bbgWaitFillMSec : milliseconds
	 */
	public void setBbgWaitFillMSec(long bbgWaitFillMSec) {
		this.bbgWaitFillMSec = bbgWaitFillMSec;
	}

	/**
	 * Set the length of the bloomberg fill polling timer.
	 * 
	 * @param bbgFillPollingMSec : milliseconds
	 */
	public void setBbgFillPollingMSec(long bbgFillPollingMSec) {
		this.bbgFillPollingMSec = bbgFillPollingMSec;
	}

	/**
	 * Set the length of the manual manage waiting fill timer.
	 * 
	 * @param manualWaitFillMSec : milliseconds
	 */
	public void setManualWaitFillMSec(long manualWaitFillMSec) {
		this.manualWaitFillMSec = manualWaitFillMSec;
	}

	/**
	 * Set the length of the manual manage fill polling timer.
	 * 
	 * @param manualFillPollingMSec : milliseconds
	 */
	public void setManualFillPollingMSec(long manualFillPollingMSec) {
		this.manualFillPollingMSec = manualFillPollingMSec;
	}

	/**
	 * Set the commission service.
	 * 
	 * @param commissionService the commissionService to set
	 */
	public void setCommissionService(CommissionService commissionService) {
		this.commissionService = commissionService;
	}

	/**
	 * Set the venue finder.
	 * 
	 * @param venueFinder : the finder
	 */
	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	/**
	 * Set the length of the waiting price timer.
	 * 
	 * @param waitPriceTimeoutMSec the new wait price timeout m sec
	 */
	public void setWaitPriceTimeoutMSec(long waitPriceTimeoutMSec) {
		this.waitPriceTimeoutMSec = waitPriceTimeoutMSec;
	}

	/**
	 * Set the length of the curando retry timer.
	 * 
	 * @param curandoRetryTimeout the new curando retry timeout
	 */
	public void setCurandoRetryTimeout(int curandoRetryTimeout) {
		this.curandoRetryTimeout = curandoRetryTimeout;
	}

	public void setPriceDiscoveryTimeout(long priceDiscoveryTimeout) {
		this.priceDiscoveryTimeout = priceDiscoveryTimeout;
	}

	public String getLimitFilePdTimes() {
		return limitFilePdTimes;
	}

	private static final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("HH:mm:ss");

	public void setLimitFilePdTimes(String limitFilePdTimes) {
		// the final value will be multiplied by 1000 because the timers work with
		// milliseconds intervals
		if (limitFilePdTimes.length() <= 0) {
			LOGGER.warn("Limit file price discovery times, for limit orders with no price not set");
			this.limitFilePdTimes = "";
		} else {
			this.limitFilePdTimes = limitFilePdTimes;
		}
		// get date millis
		DateTime midnight = DateTime.now().withTime(LocalTime.MIDNIGHT);
		long midnightMilliSec = midnight.getMillis();
		StringTokenizer st = new StringTokenizer(limitFilePdTimes, ",", false);
		// Add all times from the beginning of the day
		try {
			while (st.hasMoreTokens()) {
				String dateStr = st.nextToken();
				Long nextTime = timeFormatter.parseLocalTime(dateStr.trim()).getMillisOfDay() + midnightMilliSec;
				this.limitFileNPPriceDiscoveryTimes.add(nextTime);
				LOGGER.info("Add PD time for LF No Price at {}", new Date(nextTime));
			}
		} catch (Exception e) {
			LOGGER.warn("Exception while trying to set timers for LFNP orders from string {}.",
					limitFileNPPriceDiscoveryTimes);
		}

	}

	@Override
	public OperationEventListener getHandler(Operation operation, OperationState state) throws BestXException {
		checkPreRequisites();
		BaseOperationEventHandler handler = null;
		OperationState.Type type = state.getType();
		Market.MarketCode marketCode = state.getMarketCode();

		// remove multiple quotes handler, it is not needed anymore when state is
		// terminal;
		// in case of new price discovery, it will be removed and recreated, otherwise
		// it would contains quoteid/quotereqid of previous rfqs
		if (state.isTerminal() || (type == OperationState.Type.WaitingPrice)) {
			removeMultipleQuotesHandler(operation.getOrder().getFixOrderId());
		}

		switch (type) {
//         case AcceptQuote:
//            switch (marketCode) {
//               case BLOOMBERG:
//                  handler = new BBG_AcceptQuoteEventHandler(operation, marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG).getBuySideConnection(), serialNumberService,
//                        marketExecTimeout, getMultipleQuotesHandler(operation.getOrder().getFixOrderId()));
//               break;
//               default:
//                  throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
//            }
//         break;
		case BusinessValidation:
			handler = new BusinessValidationEventHandler(operation, orderValidationService);
			break;
		case Cancelled:
			switch (marketCode) {
			case TW:
				handler = new TW_CancelledEventHandler(operation, serialNumberService);
				break;
			case MARKETAXESS:
				handler = new MA_CancelledEventHandler(operation, serialNumberService);
				break;
			/*
			 * case BV: handler = new BV_CancelledEventHandler(operation,
			 * serialNumberService); break;
			 */
			default:
				throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
			}
			break;
//         case CurandoAuto:
		case OrderNotExecutable:
			handler = new CSOrderNotExecutableEventHandler(operation, serialNumberService, curandoRetryTimeout,
					marketFinder, operationStateAuditDao, curandoTimerRetriever.getProfile());
			break;
		case Curando: // orders not autoexecutable for size, LF or market, go here
			handler = new CSCurandoEventHandler(operation, serialNumberService, curandoRetryTimeout,
					curandoTimerRetriever.getProfile());
			break;

		case DifferentDatesExecuted:
		case MarketExecuted: {
			switch (marketCode) {
			case INTERNALIZZAZIONE:
				handler = new INT_ExecutedEventHandler(operation, serialNumberService, marketFinder);
				break;
			case MATCHING:
				handler = new MATCH_ExecutedEventHandler(operation);
				break;
			case BLOOMBERG:
				handler = new BBG_ExecutedEventHandler(operation);
				break;
			case TW:
				handler = new TW_ExecutedEventHandler(operation);
				break;
			case MARKETAXESS:
				handler = new MA_ExecutedEventHandler(operation);
				break;
			/*
			 * case BV: handler = new BV_ExecutedEventHandler(operation); break;
			 */
			default:
				throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
			}
			break;
		}
		case FormalValidationKO:
			handler = new FormalValidationKOEventHandler(operation);
			break;
		case FormalValidationOK:
			handler = new FormalValidationOkEventHandler(operation);
			break;
		case Initial:
			handler = new CSInitialEventHandler(operation);
			break;
		case ManageCounter:
			handler = new INT_ManageCounterEventHandler(operation,
					marketConnectionRegistry.getMarketConnection(MarketCode.INTERNALIZZAZIONE).getBuySideConnection());
			break;
		case ManualManage:
			handler = new ManualManageEventHandler(operation, serialNumberService, marketFinder,
					marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG));
			break;
		case ManualWaitingFill:
			handler = new ManualWaitingFillEventHandler(operation,
					marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG), marketMakerFinder,
					manualWaitFillMSec, manualFillPollingMSec);
			break;
//         case MatchFound:
//            handler = new MATCH_MatchFoundEventHandler(operation, marketConnectionRegistry.getMarketConnection(MarketCode.MATCHING).getBuySideConnection(), serialNumberService,
//                  titoliIncrociabiliService, matchingMMcode);
//         break;
		case OrderReceived:
			handler = new OrderReceivedEventHandler(operation, grdLiteLoadResponseTimeout, grdLiteService,
					instrumentFinder, orderValidationService);
			break;
		case OrderRejectable:
			handler = new OrderRejectableEventHandler(operation, serialNumberService);
			break;
		case OrderRevocated:
			handler = new OrderRevocatedEventHandler(operation, serialNumberService, instrumentFinder,
					operationRegistry);
			break;
		case Rejected: {
			if (marketCode == null) {
				handler = new CSRejectedEventHandler(operation, rejectWhenBloombergIsBest, serialNumberService);
				break;
			} else {
				switch (marketCode) {
				case BLOOMBERG:
					handler = new BBG_RejectedEventHandler(operation, serialNumberService);
					break;
				case TW:
					handler = new TW_RejectedEventHandler(operation, serialNumberService, bestXConfigurationDao);
					break;
				case MARKETAXESS:
					handler = new MA_RejectedEventHandler(operation, serialNumberService, bestXConfigurationDao);
					break;
				/*
				 * case BV: handler = new BV_RejectedEventHandler(operation,
				 * serialNumberService, bestXConfigurationDao); break;
				 */
				case INTERNALIZZAZIONE:
					handler = new INT_RejectedEventHandler(operation);
					break;
				default:
					throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
				}
				break;
			}
		}
//         case RejectQuote:
//            switch (marketCode) {
//               case BLOOMBERG:
//                  handler = new BBG_RejectQuoteEventHandler(operation, marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG).getBuySideConnection(),
//                        getMultipleQuotesHandler(operation.getOrder().getFixOrderId()));
//               break;
//               default:
//                  throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
//            }
//         break;
//         case RejectQuoteAndAutoNotExecutionReport:
//            switch (marketCode) {
//               case BLOOMBERG:
//                  handler = new RejectQuoteAndSendAutoNotExecutionEventHandler(operation, marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG).getBuySideConnection(),
//                        serialNumberService);
//               break;
//               default:
//                  throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
//            }
//         break;
//         case ReceiveQuote:
//            switch (marketCode) {
//               case BLOOMBERG:
//                  handler = new BBG_ReceiveQuoteEventHandler(operation, getMultipleQuotesHandler(operation.getOrder().getFixOrderId()));
//               break;
//			default:
//				break;
//            }
//         break;
		case SendAutoNotExecutionReport:
		case SendNotExecutionReport:
			handler = new SendNotExecutionReportEventHandler(operation, serialNumberService);
			break;
		case SendExecutionReport:
			handler = new CSSendExecutionReportEventHandler(operation, commissionService, sendExecRepTimeout,
					pobExMaxSize);
			break;
		case SendOrder:
			switch (marketCode) {
			case TW:
				handler = new TW_SendOrderEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.TW).getBuySideConnection(),
						serialNumberService, marketExecTimeout, orderCancelDelay, bestXConfigurationDao);
				break;
			case MARKETAXESS:
				handler = new MA_SendOrderEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.MARKETAXESS).getBuySideConnection(),
						serialNumberService, marketAxessExecTimeout, orderCancelDelay, bestXConfigurationDao,
						marketMakerFinder, marketFinder.getMarketByCode(MarketCode.MARKETAXESS, null), venueFinder);
				break;
			case BV:
				handler = new BV_SendOrderEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.BV).getBuySideConnection(),
						serialNumberService, bondVisionExecTimeout, orderCancelDelay, bestXConfigurationDao,
						marketMakerFinder, marketFinder.getMarketByCode(MarketCode.BV, null), venueFinder);
				break;
			default:
				throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
			}
			break;
		case SendRfq:
			switch (marketCode) {
			case BLOOMBERG:
				handler = new BBG_SendEnquiryEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG).getBuySideConnection(),
						serialNumberService, marketExecTimeout, tsoxTechnicalRejectReasons);
				break;
			case BV:
				handler = new BV_SendRFCQEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.BV).getBuySideConnection(),
						serialNumberService, bondVisionExecTimeout, orderCancelDelay, bestXConfigurationDao,
						marketMakerFinder, marketFinder.getMarketByCode(MarketCode.BV, null), venueFinder);
				break;
			default:
				throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
			}
			break;

		case StartExecution: {
			switch (marketCode) {
			case INTERNALIZZAZIONE:
				handler = new INT_StartExecutionEventHandler(operation, marketConnectionRegistry
						.getMarketConnection(MarketCode.INTERNALIZZAZIONE).getBuySideConnection(), waitingCMFTimeout);
				break;
//               case MATCHING:
//                  handler = new MATCH_StartExecutionEventHandler(operation, marketConnectionRegistry.getMarketConnection(MarketCode.MATCHING).getBuySideConnection(), titoliIncrociabiliService);
//               break;
			case BLOOMBERG:
				handler = new BBG_StartExecutionEventHandler(operation);
				break;
			case TW:
				handler = new TW_StartExecutionEventHandler(operation,
						marketConnectionRegistry.getMarketConnection(MarketCode.TW).getBuySideConnection(),
						orderCancelDelay);
				break;
			case MARKETAXESS:
				handler = new MA_StartExecutionEventHandler(operation);
				break;
			case BV:
				handler = new BV_StartExecutionEventHandler(operation);
				break;
			default:
				throw new BestXException(Messages.getString("StrategyUnexpectedMarketCode.0", marketCode));
			}
			break;
		}
		case ValidateByPunctualFilter:
			handler = new ValidateByPunctualFilterEventHandler(operation, orderValidationService);
			break;
		case ManualExecutionWaitingPrice:
			Boolean doNotExecuteMEW = CSConfigurationPropertyLoader
					.getBooleanProperty(CSConfigurationPropertyLoader.LIMITFILE_DONOTEXECUTE, false);
			if (AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus.getAutoCurandoStatus())) {
				doNotExecuteMEW = true;
			}

			handler = new ManualExecutionWaitingPriceEventHandler(operation, getPriceService(operation.getOrder()),
					titoliIncrociabiliService, customerFinder, serialNumberService, regulatedMktIsinsLoader,
					regulatedMarketPolicies, waitPriceTimeoutMSec, mifidConfig.getNumRetry(), marketPriceTimeout,
					marketSecurityStatusService, executionDestinationService, rejectWhenBloombergIsBest,
					doNotExecuteMEW, bookDepthValidator, operationStateAuditDao, this);
			break;
		case WaitingPrice:
			Boolean doNotExecuteWP = CSConfigurationPropertyLoader
					.getBooleanProperty(CSConfigurationPropertyLoader.LIMITFILE_DONOTEXECUTE, false);
			if (AutoCurandoStatus.SUSPENDED.equalsIgnoreCase(autoCurandoStatus.getAutoCurandoStatus())
					|| operation.isNotAutoExecute()) {
				doNotExecuteWP = true;
			}

			handler = new WaitingPriceEventHandler(operation, getPriceService(operation.getOrder()),
					titoliIncrociabiliService, customerFinder, serialNumberService, regulatedMktIsinsLoader,
					regulatedMarketPolicies, waitPriceTimeoutMSec, mifidConfig.getNumRetry(), marketPriceTimeout,
					marketSecurityStatusService, executionDestinationService, rejectWhenBloombergIsBest, doNotExecuteWP,
					bookDepthValidator, internalMMcodesList, operationStateAuditDao, targetPriceMaxLevel, this);

			if (CSExecutionReportHelper.isPOBex(operation)) {
				CSSendPOBExEventHandler customerHandler = new CSSendPOBExEventHandler(operation, orderBookDepth,
						priceDecimals, priceDiscoveryConnection, serialNumberService, pobExMaxSize);
				customerHandler.setCustomerConnection(customerConnection);
				customerHandler.setOperatorConsoleConnection(operatorConsoleConnection);
				handler.setCustomerSpecificHandler(customerHandler);
			}
			break;
		// case WaitingFill:
		// handler = new BBG_WaitingFillEventHandler(operation,
		// marketConnectionRegistry.getMarketConnection(MarketCode.BLOOMBERG),
		// bbgWaitFillMSec, bbgFillPollingMSec);
		// break;
		case Monitor:
			handler = new MonitorEventHandler(operation);
			break;
		case UnreconciledTrade:
			handler = new UnreconciledTradeEventHandler(operation, marketMakerFinder, serialNumberService);
			break;
		case Error:
		case Warning:
			handler = new WarningEventHandler(operation, serialNumberService);
			break;
		case LimitFileNoPrice:
			handler = new LimitFileNoPriceEventHandler(operation, serialNumberService, autoCurandoStatus,
					limitFileNPPriceDiscoveryTimes, marketFinder, operationStateAuditDao);
			break;
		case PriceDiscovery:
			handler = new PriceDiscoveryEventHandler(operation, getPriceService(operation.getOrder()),
					priceDiscoveryTimeout, priceDiscoveryCustomer, bookDepth, priceDecimals, priceDiscoveryConnection);
			break;
		case LimitFileParkedOrder:
			handler = new ParkedOrderEventHandler(operation, serialNumberService, marketFinder, autoCurandoStatus);
			break;
		default:
			if (state.isTerminal()) {
				handler = new TerminalEventHandler(operation);
			} else {
				LOGGER.warn("Unexpected type {} for fixOrderID = {}", type, operation.getOrder().getFixOrderId());
				return new DefaultOperationEventHandler(operation);
			}
			break;
		}
		handler.setOperatorConsoleConnection(operatorConsoleConnection);
		handler.setCustomerConnection(customerConnection);
		if (!handler.hasCustomerSpecificHandler() && !(handler instanceof CSBaseOperationEventHandler)) {
			CSBaseOperationEventHandler customerHandler = new CSBaseOperationEventHandler(operation);
			customerHandler.setCustomerConnection(customerConnection);
			customerHandler.setOperatorConsoleConnection(operatorConsoleConnection);
			handler.setCustomerSpecificHandler(customerHandler);
		}
		return handler;
	}

	private void checkPreRequisites() throws ObjectNotInitializedException, BestXException {
		if (orderValidationService == null) {
			throw new ObjectNotInitializedException("Order validation service not set");
		}
		/*
		 * if (priceService == null) { throw new
		 * ObjectNotInitializedException("Price service not set"); }
		 */
		if (priceServiceProvider == null) {
			throw new ObjectNotInitializedException("Price service provider not set");
		}
		if (serialNumberService == null) {
			throw new ObjectNotInitializedException("Serial number service not set");
		}
		if (marketFinder == null) {
			throw new ObjectNotInitializedException("Market finder not set");
		}
		if (marketConnectionRegistry == null) {
			throw new ObjectNotInitializedException("Market connection registry not set");
		}
		if (titoliIncrociabiliService == null) {
			; // do nothing, it's OK not to have this service
		}
		if (customerConnection == null) {
			throw new ObjectNotInitializedException("Customer connection not set");
		}
		if (operatorConsoleConnection == null) {
			throw new ObjectNotInitializedException("Operator console connection not set");
		}
		if (mifidConfig == null) {
			throw new ObjectNotInitializedException("Mifid Config not set");
		}
		if (venueFinder == null) {
			throw new ObjectNotInitializedException("Venue Finder not set");
		}
		if (customerFinder == null) {
			throw new ObjectNotInitializedException("Customer Finder not set");
		}
		if (regulatedMktIsinsLoader == null) {
			throw new ObjectNotInitializedException("RegulatedMktIsinsLoader not set");
		}
		if (regulatedMarketPolicies == null) {
			throw new ObjectNotInitializedException("RegulatedMarketPolicies not set");
		}
		if (marketSecurityStatusService == null) {
			throw new ObjectNotInitializedException("MarketSecurityStatusService not set");
		}
		if (operationRegistry == null) {
			throw new ObjectNotInitializedException("OperationRegistry not set");
		}
		if (instrStatusNotifier == null) {
			throw new ObjectNotInitializedException("InstrumentStatusNotifier not set");
		}
		if (priceDiscoveryCustomer == null) {
			priceDiscoveryCustomer = customerFinder.getCustomerByFixId(priceDiscoveryCustomerId);
			if (priceDiscoveryCustomer == null) {
				throw new ObjectNotInitializedException(
						"Unable to load customer for property PriceDiscovery.Customer. Check your configuration");
			}

		}
		if (curandoTimerRetriever == null) {
			throw new ObjectNotInitializedException(
					"Unable to init CurandoTimerRetriever in CSStrategy instance. Check your configuration");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.softsolutions.bestx.SystemStateSelector#getStateDescription()
	 */
	@Override
	public String getStateDescription() {
		return stateDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.softsolutions.bestx.SystemStateSelector#isOrderEnabled()
	 */
	@Override
	public boolean isOrderEnabled() {
		return orderEnabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.softsolutions.bestx.SystemStateSelector#setOrderEnabled(boolean)
	 */
	@Override
	public void setOrderEnabled(boolean orderEnabled) {
		LOGGER.info("Change System State: Order {}", orderEnabled);
		this.orderEnabled = orderEnabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.softsolutions.bestx.SystemStateSelector#setStateDescription(java.lang.
	 * String)
	 */
	@Override
	public void setStateDescription(String stateDescription) {
		this.stateDescription = stateDescription;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.softsolutions.bestx.SystemStateSelector#isRfqEnabled()
	 */
	@Override
	public boolean isRfqEnabled() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.softsolutions.bestx.SystemStateSelector#setRfqEnabled(boolean)
	 */
	@Override
	public void setRfqEnabled(boolean notUsed) { // not supported for Akros
	}

	/**
	 * Set the market maker finder.
	 * 
	 * @param marketMakerFinder : the finder
	 */
	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	/**
	 * Set the matching market maker code.
	 * 
	 * @param matchingMMcode : the market maker code
	 */
	public void setMatchingMMcode(String matchingMMcode) {
		this.matchingMMcode = matchingMMcode;
	}

	/**
	 * Set the internal market makers codes.
	 * 
	 * @param internalMMcodes : the codes of the internal market makers
	 */
	public void setInternalMMcodes(String internalMMcodes) {
		internalMMcodesList = new ArrayList<String>();
		String[] mmSplit = internalMMcodes.split(",");
		for (int count = 0; count < mmSplit.length; count++) {
			internalMMcodesList.add(mmSplit[count].trim());
			LOGGER.debug("Internal MM added: {}", mmSplit[count]);
		}
	}

	/**
	 * Set the timeout for the price discovery.
	 * 
	 * @param marketPriceTimeout : timeout in milliseconds
	 */
	public void setMarketPriceTimeout(long marketPriceTimeout) {
		this.marketPriceTimeout = marketPriceTimeout;
	}

	/**
	 * Set the timeout for the execution.
	 * 
	 * @param marketExecTimeout : timeout in milliseconds
	 */
	public void setMarketExecTimeout(long marketExecTimeout) {
		this.marketExecTimeout = marketExecTimeout;
	}

	/**
	 * Set the timeout for the sending of the execution report.
	 * 
	 * @param sendExecRepTimeout : timeout in milliseconds
	 */
	public void setSendExecRepTimeout(int sendExecRepTimeout) {
		this.sendExecRepTimeout = sendExecRepTimeout;
	}

	/**
	 * Set the customer finder.
	 * 
	 * @param customerFinder : the finder
	 */
	public void setCustomerFinder(CustomerFinder customerFinder) {
		this.customerFinder = customerFinder;
	}

	/**
	 * Get the loader for isins of the regulated markets.
	 * 
	 * @return the loader
	 */
	public RegulatedMktIsinsLoader getRegulatedMktIsinsLoader() {
		return regulatedMktIsinsLoader;
	}

	/**
	 * Set the loader for isins of the regulated markets.
	 * 
	 * @param regulatedMktIsinsLoader : the loader
	 */
	public void setRegulatedMktIsinsLoader(RegulatedMktIsinsLoader regulatedMktIsinsLoader) {
		this.regulatedMktIsinsLoader = regulatedMktIsinsLoader;
	}

	/**
	 * Get the activation status of the automatic curando.
	 * 
	 * @return the automatic curando activation status
	 */
	public AutoCurandoStatus getAutoCurandoStatus() {
		return autoCurandoStatus;
	}

	/**
	 * Set the activation status of the automatic curando.
	 * 
	 * @param autoCurandoStatus : automatic curando activation status
	 */
	public void setAutoCurandoStatus(AutoCurandoStatus autoCurandoStatus) {
		this.autoCurandoStatus = autoCurandoStatus;
	}

	/**
	 * Get the policies related to regulated markets.
	 * 
	 * @return a list of policies
	 */
	public List<String> getRegulatedMarketPolicies() {
		return regulatedMarketPolicies;
	}

	/**
	 * Set the policies related to regulated markets.
	 * 
	 * @param regulatedMarketPolicies : new list of policies
	 */
	public void setRegulatedMarketPolicies(List<String> regulatedMarketPolicies) {
		this.regulatedMarketPolicies = regulatedMarketPolicies;
	}

	/**
	 * Set the market security status service.
	 * 
	 * @param marketSecurityStatusService : the service
	 */
	public void setMarketSecurityStatusService(MarketSecurityStatusService marketSecurityStatusService) {
		this.marketSecurityStatusService = marketSecurityStatusService;
	}

	/**
	 * Set the operation registry.
	 * 
	 * @param operationRegistry : the operation registry
	 */
	public void setOperationRegistry(OperationRegistry operationRegistry) {
		this.operationRegistry = operationRegistry;
	}

	/**
	 * Get the instrument status notifier.
	 * 
	 * @return the instrument status notifier
	 */
	public InstrumentStatusNotifier getInstrStatusNotifier() {
		return instrStatusNotifier;
	}

	/**
	 * Set the instrument status notifier.
	 * 
	 * @param instrStatusNotifier : the notifier
	 */
	public void setInstrStatusNotifier(InstrumentStatusNotifier instrStatusNotifier) {
		this.instrStatusNotifier = instrStatusNotifier;
	}

	/**
	 * Get the execution destination service.
	 * 
	 * @return the service
	 */
	public ExecutionDestinationService getExecutionDestinationService() {
		return this.executionDestinationService;
	}

	/**
	 * Set the execution destination service.
	 * 
	 * @param executionDestinationService : the new service
	 */
	public void setExecutionDestinationService(ExecutionDestinationService executionDestinationService) {
		this.executionDestinationService = executionDestinationService;
	}

	/**
	 * Set the timeout for market communications.
	 * 
	 * @param marketCommunicationTimeout : timeout in milliseconds
	 */
	public void setMarketCommunicationTimeout(int marketCommunicationTimeout) {
		this.marketCommunicationTimeout = marketCommunicationTimeout;
	}

	/**
	 * Get the timeout for market communications.
	 * 
	 * @return timeout in milliseconds
	 */
	public int getMarketCommunicationTimeout() {
		return marketCommunicationTimeout;
	}

	/**
	 * Get the timeout for CMF waitings.
	 * 
	 * @return timeout in milliseconds
	 */
	public long getWaitingCMFTimeout() {
		return this.waitingCMFTimeout;
	}

	/**
	 * Set the timout for CMF waiting.
	 * 
	 * @param waitingCMFTimeout : the timeout in milliseconds
	 */
	public void setWaitingCMFTimeout(long waitingCMFTimeout) {
		this.waitingCMFTimeout = waitingCMFTimeout;
	}

	/**
	 * Set the price service provider.
	 * 
	 * @param priceServiceProvider : price service provider
	 */
	public void setPriceServiceProvider(PriceServiceProvider priceServiceProvider) {
		this.priceServiceProvider = priceServiceProvider;
	}

	/**
	 * Get the price service for a given order.
	 * 
	 * @param order : the order related to this price service
	 * @return the price service for the order
	 */
	public PriceService getPriceService(Order order) {
		PriceDiscoveryType priceDiscType = order.getPriceDiscoverySelected();
		PriceService priceService = priceServiceProvider.getPriceService(priceDiscType);
		return priceService;
	}

	/**
	 * Get the timeout for communications with the MTSPrime market.
	 * 
	 * @return timeout in milliseconds
	 */
	public int getMtsCreditExecTimeout() {
		return mtsCreditExecTimeout;
	}

	/**
	 * Set the timeout for communications with the MTSPrime market.
	 * 
	 * @param mtsCreditExecTimeout : timeout in milliseconds
	 */
	public void setMtsCreditExecTimeout(int mtsCreditExecTimeout) {
		this.mtsCreditExecTimeout = mtsCreditExecTimeout;
	}

	/**
	 * Check if we must reject orders if Bloomberg is Best.
	 * 
	 * @return the rejectWhenBloombergIsBest
	 */
	public boolean isRejectWhenBloombergIsBest() {
		return rejectWhenBloombergIsBest;
	}

	/**
	 * Set the reject orders when Bloomberg is Best flag.
	 * 
	 * @param rejectWhenBloombergIsBest the rejectWhenBloombergIsBest to set
	 */
	public void setRejectWhenBloombergIsBest(boolean rejectWhenBloombergIsBest) {
		this.rejectWhenBloombergIsBest = rejectWhenBloombergIsBest;
	}

	/**
	 * Gets the minimum required book depth.
	 *
	 * @return the minimum required book depth
	 */
	public int getMinimumRequiredBookDepth() {
		return minimumRequiredBookDepth;
	}

	/**
	 * Sets the minimum required book depth.
	 *
	 * @param minimumRequiredBookDepth the new minimum required book depth
	 */
	public void setMinimumRequiredBookDepth(int minimumRequiredBookDepth) {
		this.minimumRequiredBookDepth = minimumRequiredBookDepth;
		((CSBookDepthController) bookDepthValidator).setMinimumRequiredBookDepth(minimumRequiredBookDepth);
	}

	/**
	 * Gets the cs-spring internal market exec timeout.
	 * 
	 * @return the internal market exec timeout
	 */
	public long getInternalRfqReplyTimeout() {
		return internalRfqReplyTimeout;
	}

	/**
	 * Sets the internal market exec timeout.
	 * 
	 * @param internalRfqReplyTimeout the new internal market exec timeout
	 */
	public void setInternalRfqReplyTimeout(long internalRfqReplyTimeout) {
		this.internalRfqReplyTimeout = internalRfqReplyTimeout;
	}

	/**
	 * Gets the internal RFQ message prefix.
	 *
	 * @return the internal RFQ message prefix
	 */
	public String getInternalRfqMessagePrefix() {
		return internalRfqMessagePrefix;
	}

	/**
	 * Sets the internal RFQ message prefix.
	 *
	 * @param internalRfqMessagePrefix the internal RFQ message prefix
	 */
	public void setInternalRfqMessagePrefix(String internalRfqMessagePrefix) {
		this.internalRfqMessagePrefix = internalRfqMessagePrefix;
	}

	private MultipleQuotesHandler getMultipleQuotesHandler(String operationId) {
		if (!multipleQuotesHandlers.containsKey(operationId)) {
			LOGGER.debug("[INT-TRACE] Adding multiple quotes handler for {}", operationId);

			multipleQuotesHandlers.put(operationId, new MultipleQuotesHandler());
		}

		return multipleQuotesHandlers.get(operationId);
	}

	private void removeMultipleQuotesHandler(String operationId) {
		MultipleQuotesHandler handler = getMultipleQuotesHandler(operationId);
		if (handler != null) {
			String externalLastQuoteId = "";
			Proposal externalQuote = handler.getUpdatedExternalQuote();
			if (externalQuote != null) {
				externalLastQuoteId = externalQuote.getSenderQuoteId();
			}
			String internalLastQuoteId = "";

			LOGGER.debug(
					"[INT-TRACE] Removing multiple quotes handler for {} - external quoteID={}, internal quoteID={}",
					operationId, externalLastQuoteId, internalLastQuoteId);

			multipleQuotesHandlers.remove(operationId);
		}
	}

	/**
	 * Gets the grd lite load response timeout.
	 *
	 * @return the grdLiteLoadResponseTimeout
	 */
	public long getGrdLiteLoadResponseTimeout() {
		return grdLiteLoadResponseTimeout;
	}

	/**
	 * Sets the grd lite load response timeout.
	 *
	 * @param grdLiteLoadResponseTimeout the grdLiteLoadResponseTimeout to set
	 */
	public void setGrdLiteLoadResponseTimeout(long grdLiteLoadResponseTimeout) {
		this.grdLiteLoadResponseTimeout = grdLiteLoadResponseTimeout;
	}

	/**
	 * Gets the grd lite service.
	 *
	 * @return the grdLiteService
	 */
	public CustomService getGrdLiteService() {
		return grdLiteService;
	}

	/**
	 * Sets the grd lite service.
	 *
	 * @param grdLiteService the grdLiteService to set
	 */
	public void setGrdLiteService(GRDLiteService grdLiteService) {
		this.grdLiteService = grdLiteService;
	}

	/**
	 * Gets the instrument finder.
	 *
	 * @return the instrumentFinder
	 */
	public InstrumentFinder getInstrumentFinder() {
		return instrumentFinder;
	}

	/**
	 * Sets the instrument finder.
	 *
	 * @param instrumentFinder the instrumentFinder to set
	 */
	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}

	/**
	 * @param limitFileBestWithinLimitThresholdPDIntervalInSeconds the
	 *                                                             limitFileBestWithinLimitThresholdPDIntervalInSeconds
	 *                                                             to set
	 */
	public void setLimitFileBestWithinLimitThresholdPDIntervalInSeconds(
			long limitFileBestWithinLimitThresholdPDIntervalInSeconds) {
		// the final value will be multiplied by 1000 because the timers work with
		// milliseconds intervals
		if (limitFileBestWithinLimitThresholdPDIntervalInSeconds < LIMIT_FILE_MINIMUM_PD_INTERVAL) {
			LOGGER.warn(
					"Limit file price discovery interval, for orders with best/limit delta within threshold, too short: setting it to a minimum of {} seconds.",
					LIMIT_FILE_MINIMUM_PD_INTERVAL);
			this.limitFileBestWithinLimitThresholdPDIntervalInSeconds = LIMIT_FILE_MINIMUM_PD_INTERVAL * 1000;
		} else {
			this.limitFileBestWithinLimitThresholdPDIntervalInSeconds = limitFileBestWithinLimitThresholdPDIntervalInSeconds
					* 1000;
		}
	}

	/**
	 * @param limitFileBestOutsideLimitThresholdPDIntervalInSeconds the
	 *                                                              limitFileBestOutsideLimitThresholdPDIntervalInSeconds
	 *                                                              to set
	 */
	public void setLimitFileBestOutsideLimitThresholdPDIntervalInSeconds(
			long limitFileBestOutsideLimitThresholdPDIntervalInSeconds) {
		// the final value will be multiplied by 1000 because the timers work with
		// milliseconds intervals
		if (limitFileBestOutsideLimitThresholdPDIntervalInSeconds < LIMIT_FILE_MINIMUM_PD_INTERVAL) {
			LOGGER.warn(
					"Limit file price discovery interval, for orders with best/limit delta outside threshold, too short: setting it to a minimum of {} seconds.",
					LIMIT_FILE_MINIMUM_PD_INTERVAL);
			this.limitFileBestOutsideLimitThresholdPDIntervalInSeconds = LIMIT_FILE_MINIMUM_PD_INTERVAL * 1000;
		} else {
			this.limitFileBestOutsideLimitThresholdPDIntervalInSeconds = limitFileBestOutsideLimitThresholdPDIntervalInSeconds
					* 1000;
		}
	}

	/**
	 * 
	 * @return limitFileBestWithinLimitThresholdPDIntervalInSeconds the
	 *         limitFileBestWithinLimitThresholdPDIntervalInSeconds
	 */
	public long getLimitFileBestWithinLimitThresholdPDIntervalInSeconds() {
		return this.limitFileBestWithinLimitThresholdPDIntervalInSeconds / 1000;
	}

	/**
	 * 
	 * @return limitFileBestOutsideLimitThresholdPDIntervalInSeconds the
	 *         limitFileBestOutsideLimitThresholdPDIntervalInSeconds
	 */
	public long getLimitFileBestOutsideLimitThresholdPDIntervalInSeconds() {
		return this.limitFileBestOutsideLimitThresholdPDIntervalInSeconds / 1000;
	}

	/**
	 * @param limitFileCommentPrefix the limitFileCommentPrefix to set
	 */
	public void setLimitFileCommentPrefix(String limitFileCommentPrefix) {
		this.limitFileCommentPrefix = limitFileCommentPrefix;

		LimitFileHelper.getInstance().setLimitFileCommentPrefix(limitFileCommentPrefix);
	}

	/**
	 * Gets the limit file comment prefix.
	 *
	 * @return the limit file comment prefix
	 */
	public String getLimitFileCommentPrefix() {
		return limitFileCommentPrefix;
	}

	/**
	 * @param limitFileNoPriceCommentPrefix the limitFileNoPriceCommentPrefix to set
	 */
	public void setLimitFileNoPriceCommentPrefix(String limitFileNoPriceCommentPrefix) {
		this.limitFileNoPriceCommentPrefix = limitFileNoPriceCommentPrefix;

		LimitFileHelper.getInstance().setLimitFileNoPriceCommentPrefix(limitFileNoPriceCommentPrefix);
	}

	/**
	 * Gets the limit file no price comment prefix.
	 *
	 * @return the limit file no price comment prefix
	 */
	public String getLimitFileNoPriceCommentPrefix() {
		return limitFileNoPriceCommentPrefix;
	}

	/**
	 * @param limitFileCommentMaxLen the max length of limit file comments
	 */
	public void setLimitFileCommentMaxLen(int limitFileCommentMaxLen) {
		this.limitFileCommentMaxLen = limitFileCommentMaxLen;

		LimitFileHelper.getInstance().setCommentsMaxLen(limitFileCommentMaxLen);
	}

	public void setPriceDiscoveryValidationService(OrderValidationService priceDiscoveryValidationService) {
		this.priceDiscoveryValidationService = priceDiscoveryValidationService;
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
	 * Sets the tsox technical reject reasons.
	 *
	 * @param tsoxTechnicalRejectReasons the tsox technical reject reasons list
	 */
	public void setTsoxTechnicalRejectReasons(String tsoxTechnicalRejectReasons) {
		if ((tsoxTechnicalRejectReasons != null) && (!tsoxTechnicalRejectReasons.isEmpty())) {

			Collections.addAll(this.tsoxTechnicalRejectReasons, tsoxTechnicalRejectReasons.toLowerCase().split("\\|"));
		}
	}

	public void setOrderCancelDelay(long orderCancelDelay) {
		this.orderCancelDelay = orderCancelDelay;
	}

	public void setBestXConfigurationDao(BestXConfigurationDao bestXConfigurationDao) {
		this.bestXConfigurationDao = bestXConfigurationDao;
	}

	public int getPriceDecimals() {
		return priceDecimals;
	}

	public void setPriceDecimals(int priceDecimals) {
		this.priceDecimals = priceDecimals;
	}

	public OperatorConsoleConnection getPriceDiscoveryConnection() {
		return priceDiscoveryConnection;
	}

	public void setPriceDiscoveryConnection(OperatorConsoleConnection priceDiscoveryConnection) {
		this.priceDiscoveryConnection = priceDiscoveryConnection;
	}

	public long getMarketAxessExecTimeout() {
		return marketAxessExecTimeout;
	}

	public void setMarketAxessExecTimeout(long marketAxessExecTimeout) {
		this.marketAxessExecTimeout = marketAxessExecTimeout;
	}

	public void setCurandoTimerRetriever(CurandoTimerRetriever curandoTimerRetriever) {
		this.curandoTimerRetriever = curandoTimerRetriever;
	}

	@Override
	public synchronized void setModality(Type modality) {
		this.modality = modality;
	}

	@Override
	public synchronized Type getModality() {
		return this.modality;
	}
}