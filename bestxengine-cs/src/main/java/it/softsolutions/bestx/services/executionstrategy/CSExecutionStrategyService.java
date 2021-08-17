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

package it.softsolutions.bestx.services.executionstrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.booksorter.BookSorterImpl;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.CurandoState;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderCancelRequestState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_RejectedState;
import it.softsolutions.bestx.states.bloomberg.BBG_StartExecutionState;
import it.softsolutions.bestx.states.marketaxess.MA_RejectedState;
import it.softsolutions.bestx.states.marketaxess.MA_StartExecutionState;
import it.softsolutions.bestx.states.tradeweb.TW_RejectedState;
import it.softsolutions.bestx.states.tradeweb.TW_StartExecutionState;

/**  
 *
 * Purpose: abstract class used as a template for every execution strategy service needed  
 *
 * Project Name : bestxengine-cs 
 * First created by: ruggero.rizzo 
 * Creation date: 23/ott/2013 
 * 
 **/
public abstract class CSExecutionStrategyService implements ExecutionStrategyService, CSExecutionStrategyServiceMBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(CSExecutionStrategyService.class);
	//    protected ExecutionStrategyServiceCallback executionStrategyServiceCallback;
	protected PriceResult priceResult = null;
	protected boolean rejectOrderWhenBloombergIsBest;

	protected List<MarketCode> allMarketsToTry;
	protected Operation operation;
	protected MarketFinder marketFinder;
	protected ArrayList<Market> marketsToTry;
	protected BookClassifier bookClassifier;
	protected BookSorterImpl bookSorter;
	protected ApplicationStatus applicationStatus;
	private MarketConnectionRegistry marketConnectionRegistry;
	protected int minimumRequiredBookDepth = 3;

	
	public MarketConnectionRegistry getMarketConnectionRegistry() {
		return marketConnectionRegistry;
	}

	public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
		this.marketConnectionRegistry = marketConnectionRegistry;
	}



	public BookClassifier getBookClassifier() {
		return bookClassifier;
	}

	public void setBookClassifier(BookClassifier bookClassifier) {
		this.bookClassifier = bookClassifier;
	}

	public BookSorterImpl getBookSorter() {
		return bookSorter;
	}

	public void setBookSorter(BookSorterImpl bookSorter) {
		this.bookSorter = bookSorter;
	}

	public List<Market> getMarketsToTry() {
		return marketsToTry;
	}

	public MarketFinder getMarketFinder()
	{
		return marketFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder)
	{
		this.marketFinder = marketFinder;
	}


	public List<MarketCode> getAllMarketsToTry() {
		return allMarketsToTry;
	}

	
	
	public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}

	
	
	public int getMinimumRequiredBookDepth() {
		return minimumRequiredBookDepth;
	}

	public void setMinimumRequiredBookDepth(int minimumRequiredBookDepth) {
		this.minimumRequiredBookDepth = minimumRequiredBookDepth;
	}

	@Override
	public abstract void manageAutomaticUnexecution(Order order, Customer customer) throws BestXException;

	/**
	 * The starting of an order execution has been centralized in this method.
	 * 
	 * @param operation
	 *            : the operation that is going to be executed
	 * @param currentAttempt
	 *            : current attempt
	 * @param matchingVenue
	 *            : the venue for the matching market
	 * @param internalVenue
	 *            : the venue for the internal market
	 * @param serialNumberService
	 *            : the serial number service
	 */
	@Override
	public void startExecution(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService) {
		// [BESTX-458] If we are in a Monitor Application Status stop the execution and go back
		if (this.applicationStatus.getType() == ApplicationStatus.Type.MONITOR) {
			try {
				ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
				String msg;
				if (currentAttempt.getMarketOrder() == null) {
					msg = Messages.getString("RejectInsufficientBookDepth.0", 3 /*TODO bookDepthValidator.getMinimumRequiredBookDepth()*/);
				} else  {
					msg = Messages.getString("Monitor.RejectMessage", currentAttempt.getMarketOrder().getMarket().getMicCode());
				}
				operation.setStateResilient(new SendAutoNotExecutionReportState(msg), ErrorState.class);
			} catch (BestXException e) {
				LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
				String errorMessage = e.getMessage();
				operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
			}
			return;
		}
		
		// manage custom strategy to execute UST on Tradeweb with no MMM specified and limit price as specified in client order
		if(BondTypesService.isUST(operation.getOrder().getInstrument())) { // BESTX-382
			if (!CheckIfBuySideMarketIsConnectedAndEnabled(MarketCode.TW)) { // BESTX-574
				String reason = "TW Market is not available";
				try {
					ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
					operation.setStateResilient(new SendAutoNotExecutionReportState(reason), ErrorState.class);
				} catch (BestXException e) {
					LOGGER.error("Order {}, error while creating report for TW market not available report.", operation.getOrder().getFixOrderId(), e);
					String errorMessage = e.getMessage();
					operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
					
				}
				return;				
			} else {
				// override execution proposal every time
				MarketOrder marketOrder = new MarketOrder();
				currentAttempt.setMarketOrder(marketOrder);
				marketOrder.setValues(operation.getOrder());
				marketOrder.setTransactTime(DateService.newUTCDate());
				try {
					marketOrder.setMarket(marketFinder.getMarketByCode(MarketCode.TW, null));
				} catch (BestXException e) {
					LOGGER.info("Error when trying to send an order to Tradeweb: unable to find market with code {}", MarketCode.TW.name());
				}
				marketOrder.setVenue(null);
				marketOrder.setMarketMarketMaker(null);

				marketOrder.setLimit(operation.getOrder().getLimit());  // if order limit is null, send a market order to TW
				LOGGER.info("Order={}, Selecting for execution market market maker: null and price null", operation.getOrder().getFixOrderId());
				String twSessionId = operation.getIdentifier(OperationIdType.TW_SESSION_ID);
				if (twSessionId != null) {
					operation.removeIdentifier(OperationIdType.TW_SESSION_ID);
				}
				operation.setStateResilient(new TW_StartExecutionState(), ErrorState.class);
				// last command in method for this case
			}
		} else {
			//we must always preserve the existing comment, because it could be the one sent to us through OMS
			if(currentAttempt == null || currentAttempt.getMarketOrder() == null || currentAttempt.getMarketOrder().getMarket() == null) {
				LOGGER.warn("Order {},  invalid market order when trying to start execution. currentAttempt.MarketOrder = {}", currentAttempt == null ? "null.null" : currentAttempt.getMarketOrder());
				operation.setStateResilient(new WarningState(operation.getState(), null, 
						"invalid market order when trying to start execution. currentAttempt.MarketOrder =" + (currentAttempt == null ? "null.null" : currentAttempt.getMarketOrder())), ErrorState.class);
				return;
			}
			
			if (currentAttempt.getMarketOrder().getMarket().isHistoric()) {
				Market originalMarket = currentAttempt.getMarketOrder().getMarket().getOriginalMarket();
				currentAttempt.getMarketOrder().setMarket(originalMarket);
			}
			
			switch (currentAttempt.getMarketOrder().getMarket().getMarketCode()) {
			case BLOOMBERG:
				if (rejectOrderWhenBloombergIsBest) {
					// send not execution report
					try {
						ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
						operation.setStateResilient(new SendAutoNotExecutionReportState(Messages.getString("RejectWhenBloombergBest.0")), ErrorState.class);
					} catch (BestXException e) {
						LOGGER.error("Order {}, error while starting automatic not execution.", operation.getOrder().getFixOrderId(), e);
						String errorMessage = e.getMessage();
						operation.setStateResilient(new WarningState(operation.getState(), null, errorMessage), ErrorState.class);
					}
				} else {
					String bbg_orderID = operation.getIdentifier(OperationIdType.BLOOMBERG_CLORD_ID);
					if (bbg_orderID != null) {
						operation.removeIdentifier(OperationIdType.BLOOMBERG_CLORD_ID);
					}
					// requested on March 2019 rendez vous un Zurich currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
					currentAttempt.getMarketOrder().setMarketMarketMaker(null);
					if(CheckIfBuySideMarketIsConnectedAndEnabled(MarketCode.BLOOMBERG))
						operation.setStateResilient(new BBG_StartExecutionState(), ErrorState.class);
					else {
						operation.setStateResilient(new BBG_RejectedState("Bloomberg Market is unavailable or disabled", false), ErrorState.class);
					}
				}
				break;
			case TW:
				String twSessionId = operation.getIdentifier(OperationIdType.TW_SESSION_ID);
				if (twSessionId != null) {
					operation.removeIdentifier(OperationIdType.TW_SESSION_ID);
				}

				// requested on March 2019 rendez vous un Zurich currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
				currentAttempt.getMarketOrder().setMarketMarketMaker(null);
				if(CheckIfBuySideMarketIsConnectedAndEnabled(MarketCode.TW))
					operation.setStateResilient(new TW_StartExecutionState(), ErrorState.class);
				else {
					operation.setStateResilient(new TW_RejectedState("Tradeweb Market is unavailable or disabled"), ErrorState.class);
				}
				break;
			case MARKETAXESS:
				String maSessionId = operation.getIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
				if (maSessionId != null) {
					operation.removeIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
				}
				//SP20200310 BESTX-542
//            MarketOrder marketOrderMA = currentAttempt.getMarketOrder();
            // removed management of MA dealers for BESTX-901
//            List<MarketMarketMakerSpec> dealersMA = currentAttempt.getSortedBook().getValidProposalDealersByMarket(MarketCode.MARKETAXESS, marketOrderMA.getSide());
//            marketOrderMA.setDealers(dealersMA);
            
				// requested on March 2019 rendez vous un Zurich currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
//				currentAttempt.getMarketOrder().setMarketMarketMaker(null);
				if(CheckIfBuySideMarketIsConnectedAndEnabled(MarketCode.MARKETAXESS))
					operation.setStateResilient(new MA_StartExecutionState(), ErrorState.class);
				else {
					operation.setStateResilient(new MA_RejectedState("MarketAxess Market is unavailable or disabled"), ErrorState.class);
				}
				break;
			default:
				operation.removeLastAttempt();
				operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("MARKET_UNKNOWN",
						currentAttempt.getMarketOrder().getMarket().getMarketCode().name())), ErrorState.class);
			}
		}
	}

	public CSExecutionStrategyService() {
		super();
	}

	@Deprecated
	public CSExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus, int minimumRequiredBookDepth) {
		if (executionStrategyServiceCallback == null) {
			throw new IllegalArgumentException("executionStrategyServiceCallback is null");
		}

		//        this.executionStrategyServiceCallback = executionStrategyServiceCallback;
		this.priceResult = priceResult;
		this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;
		this.applicationStatus = applicationStatus;
		this.minimumRequiredBookDepth = minimumRequiredBookDepth;
	}

	public CSExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest, ApplicationStatus applicationStatus, int minimumRequiredBookDepth) {
		if (operation == null) {
			throw new IllegalArgumentException("operation is null");
		}

		this.operation = operation;
		this.priceResult = priceResult;
		this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;
		this.applicationStatus = applicationStatus;
		this.minimumRequiredBookDepth = minimumRequiredBookDepth;
	}

	/**
	 * Manages the rejection from a market (replace the RejectStatus handler
	 * @param operation the operation being rejected
	 * @param currentAttempt the attempt being failed (got reject)
	 * @param numberSerialService ignored
	 */
	@Override
	public void manageMarketReject(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService) throws BestXException {
		// ### First of all, manage all cases where a new execution attempt is not required
		// manage customer revoke
		if(operation.isCustomerRevokeReceived()) {
			this.acceptOrderRevoke(operation, currentAttempt, serialNumberService);
			return;
		}
		// manage EOD
		if(operation.isStopped()) {
			this.onUnexecutionResult(Result.EODCalled, "End Of Day");
			return;
		}
		if(BondTypesService.isUST(operation.getOrder().getInstrument()) 
						&& currentAttempt.getMarketOrder().getMarket().getMarketCode() == MarketCode.TW) { // have got a rejection on the single attempt on TW
			Order order= operation.getOrder();
			manageAutomaticUnexecution(order, order.getCustomer());
			return;
		}
		// ###  End
		
		//BESTX-865 retry a price discovery every attempt
      operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
		
		/*
		List<MarketMaker> doNotIncludeMM = new ArrayList<MarketMaker>();
		// move book to a new attempt
		operation.addAttempt();
		Order customerOrder = operation.getOrder();
		Attempt newAttempt = operation.getLastAttempt();
		// discard all proposals in book which have been used before
		newAttempt.setSortedBook(bookSorter.getSortedBook(
				bookClassifier.getClassifiedBook(
						currentAttempt.getSortedBook().clone(), operation.getOrder(), operation.getAttemptsInCurrentCycle(), null)));
		// remove dealers that were included in the preceding RFQ/Orders
		List<Attempt> currentAttempts = 
				operation.getAttemptsInCurrentCycle();

		currentAttempts.forEach(attempt->{ 
			currentAttempt.getExecutablePrices().forEach(execPx->{
				if(execPx.getMarketMaker() != null) {
					doNotIncludeMM.add(execPx.getMarketMaker());
				}
			});
		});

		// BESTX-736 Only if Limit File and consolidated book is empty
		boolean emptyConsolidatedBook = newAttempt.getSortedBook().getValidSideProposals(operation.getOrder().getSide()).isEmpty();
		if (operation.getOrder().isLimitFile() && !operation.isNotAutoExecute() && emptyConsolidatedBook) {
			this.startExecution(operation, newAttempt, serialNumberService);
			return;
		}

		ClassifiedProposal executionProposal = newAttempt.getSortedBook().getBestProposalBySide(customerOrder.getSide());
		if(executionProposal == null) {
			this.manageAutomaticUnexecution(customerOrder, customerOrder.getCustomer());
			return;
		} else {
			newAttempt.setExecutionProposal(executionProposal);
		}

		MarketOrder marketOrder = new MarketOrder();	
		// maintain price
		marketOrder.setValues(currentAttempt.getMarketOrder());
		// generate the list of dealers to be excluded because they have been contacted in one preceding attempt
		List<MarketMarketMakerSpec> excludeDealers = new ArrayList<MarketMarketMakerSpec>();
		if (currentAttempt.getExecutionProposal() != null) {
			marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());
			marketOrder.setMarketMarketMaker(executionProposal.getMarketMarketMaker());
			List<MarketMarketMaker> doNotIncludeMMM = new ArrayList<MarketMarketMaker>();
			doNotIncludeMM.forEach(marketMaker->{
				if(marketMaker.getMarketMarketMakerForMarket(marketOrder.getMarket().getMarketCode()) != null)
					doNotIncludeMMM.addAll(marketMaker.getMarketMarketMakerForMarket(marketOrder.getMarket().getMarketCode()));
			});
			doNotIncludeMMM.forEach(mmm -> {
				excludeDealers.add(new MarketMarketMakerSpec(mmm.getMarketSpecificCode(), mmm.getMarketSpecificCodeSource()));
			});
			marketOrder.setExcludeDealers(excludeDealers);
		}
		// create the list of dealers to be included in the order dealers list, which shall not contain any of the excluded dealers
		List<MarketMarketMakerSpec> dealers = newAttempt.getSortedBook().getValidProposalDealersByMarket(executionProposal.getMarket().getMarketCode(), marketOrder.getSide());
		dealers.removeAll(excludeDealers);
		marketOrder.setDealers(dealers);
		marketOrder.setVenue(null);
		marketOrder.setMarketSessionId(null);
		marketOrder.setMarket(executionProposal.getMarket());
		marketOrder.setTransactTime(DateService.newUTCDate());
		newAttempt.setMarketOrder(marketOrder);
		if(!operation.isNotAutoExecute()) {
			this.startExecution(operation, newAttempt, serialNumberService);
		} else {
			LOGGER.info("Order {} is not autoexecutable, go to Curando State", customerOrder.getFixOrderId());
			operation.setStateResilient(new CurandoState(), ErrorState.class);
		}
		*/
	}


	@Override
	public void acceptOrderRevoke(Operation operation, Attempt currentAttempt,
			SerialNumberService serialNumberService) {
		//BESTX-483 TDR 20190828
//		operation.setStateResilient(new OrderRevocatedState( Messages.getString("REVOKE_ACKNOWLEDGED")), ErrorState.class);	
		LOGGER.debug("Operation {}, Attempt {}, SerialNumberService {}", operation, currentAttempt, serialNumberService);
		operation.setStateResilient(new OrderCancelRequestState( Messages.getString("REVOKE_ACKNOWLEDGED")), ErrorState.class);
	}

	/** get from the market codes in the getAllMarketsToTry which is the first one not used in the current attempt cycle
	 * 
	 * @param operation needed to get the market code used in all the attempts in current attempt cycle
	 * @return
	 */
	@SuppressWarnings("unused") // may be used when the markets ranking will be used to determine next MTF to use
	private Market getNextMarketToTry(Operation operation, Market market) throws IllegalArgumentException{
		if(market == null) throw new IllegalArgumentException("null Market not allowed in getNextMarketToTry()");
		// copy all markets to be tried in local parameter
		@SuppressWarnings("unchecked")
		List<Market> currentMarkets = (List<Market>) marketsToTry.clone();
		// remove from all markets all the tried ones in current execution cycle
		List<Attempt> currentAttempts = 
				operation.getAttempts().subList(operation.getFirstAttemptInCurrentCycle(), operation.getAttempts().size() - 1);

		//		currentAttempts.forEach(new AttemptConsumer(currentMarkets));
		currentAttempts.forEach(attempt->{ 
			if(attempt.getMarketOrder()!= null) 
				currentMarkets.remove(attempt.getMarketOrder().getMarket());
		});
		// return first not tried or null
		if(currentMarkets.size() > 0 && currentMarkets.contains(market)) {
			int marketCodeIndex = currentMarkets.indexOf(market);
			return marketCodeIndex < currentMarkets.size() ? currentMarkets.get(marketCodeIndex) : null;
		}
		return null;
	}

    /** 
     * Validates the options available when the order cannot be further executed.
     */
	public void onUnexecutionResult(Result result, String message) {
	    switch (result) {
	    case MaxDeviationLimitViolated:
	    case Success:
	    	if(operation.isNotAutoExecute())
	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
	    	else
	        try {
	        	ExecutionReportHelper.prepareForAutoNotExecution(this.operation, SerialNumberServiceProvider.getSerialNumberService(), ExecutionReportState.REJECTED);
	        	this.operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
	        } catch (BestXException e) {
	            LOGGER.error("Order {}, error while starting automatic not execution.", this.operation.getOrder().getFixOrderId(), e);
	            String errorMessage = e.getMessage();
	            this.operation.setStateResilient(new WarningState(this.operation.getState(), null, errorMessage), ErrorState.class);
	        }
	        break;
// after real attempt of execution
	    case USSingleAttemptNotExecuted:
	    case CustomerAutoNotExecution:
	    case EODCalled:
	        try {
	        	ExecutionReportHelper.prepareForAutoNotExecution(this.operation, SerialNumberServiceProvider.getSerialNumberService(), ExecutionReportState.REJECTED);
	        	this.operation.setStateResilient(new SendAutoNotExecutionReportState(message), ErrorState.class);
	        } catch (BestXException e) {
	            LOGGER.error("Order {}, error while starting automatic not execution.", this.operation.getOrder().getFixOrderId(), e);
	            String errorMessage = e.getMessage();
	            this.operation.setStateResilient(new WarningState(this.operation.getState(), null, errorMessage), ErrorState.class);
	        }
	        break;
	    case Failure:
	        LOGGER.error("Order {} : ", operation.getOrder().getFixOrderId(), message);
	        this.operation.setStateResilient(new WarningState(operation.getState(), null, message), ErrorState.class);
	        break;
	    case LimitFileNoPrice:
	    	if(this.operation.isNotAutoExecute())
	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
	        else
	    		this.operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
	        break;
	    case LimitFile:
	        //Update the BestAndLimitDelta field on the TabHistoryOrdini table
	        Order order = this.operation.getOrder();
	        OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
	    	if(operation.isNotAutoExecute())
	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
	    	else
	    		this.operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
	        break;
	    default:
	        LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", this.operation.getOrder().getFixOrderId());
	        this.operation.setStateResilient(new WarningState(this.operation.getState(), null, message), ErrorState.class);
	        break;
	    }
	}
	
	/**
	 * Check if buy side market is connected and enabled.
	 *
	 * @param marketCode the market code
	 * @return true, if successful
	 */
	public boolean CheckIfBuySideMarketIsConnectedAndEnabled (MarketCode marketCode){
		MarketConnection marketConnection = marketConnectionRegistry.getMarketConnection(marketCode);
		if (marketConnection == null) {
			LOGGER.error("MarketCode {} is not contained in market connection list", marketCode.toString());
			return false;
		}
		
		if (!marketConnection.isBuySideConnectionEnabled()) {             
			LOGGER.info("MarketCode {} is not enabled", marketCode.toString());
			return false;			
		}
		
		if (!marketConnection.isBuySideConnectionAvailable()) {             
			LOGGER.info("MarketCode {} is not available", marketCode.toString());
			return false;			
		}		

		return true;		
	}

}
/**
 * As a side effect purges the market code used in the attempt from the currentMarkets
 * @author anna.cochetti
 *
 */
final class AttemptConsumer implements Consumer<Attempt> {
	private List<MarketCode> currentMarkets;

	AttemptConsumer(List<MarketCode> currentMarkets) {
		this.currentMarkets = currentMarkets;
	}

	@Override
	public void accept(Attempt att) {
		if(att.getMarketOrder()!= null)
			currentMarkets.remove(att.getMarketOrder().getMarket().getMarketCode());
	}

}