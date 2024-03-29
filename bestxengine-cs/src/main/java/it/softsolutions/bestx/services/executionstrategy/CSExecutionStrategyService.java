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
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OrderHelper;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.OperationStateAuditDAOProvider;
import it.softsolutions.bestx.services.SerialNumberServiceProvider;
import it.softsolutions.bestx.services.booksorter.BookSorterImpl;
import it.softsolutions.bestx.services.instrument.BondTypesService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderCancelRequestState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.WaitingPriceState;
import it.softsolutions.bestx.states.WarningState;
import it.softsolutions.bestx.states.bloomberg.BBG_StartExecutionState;
import it.softsolutions.bestx.states.marketaxess.MA_StartExecutionState;
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

	protected PriceResult priceResult = null;

	protected List<MarketCode> allMarketsToTry;
	protected Operation operation;
	protected MarketFinder marketFinder;
	protected ArrayList<Market> marketsToTry;
	protected BookClassifier bookClassifier;
	protected BookSorterImpl bookSorter;
	protected ApplicationStatus applicationStatus;
	protected MifidConfig mifidConfig;
	private MarketConnectionRegistry marketConnectionRegistry;
	
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

	@Override
	public abstract void manageAutomaticUnexecution(Order order, Customer customer, String message) throws BestXException;

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
					String bbg_orderID = operation.getIdentifier(OperationIdType.BLOOMBERG_CLORD_ID);
					if (bbg_orderID != null) {
						operation.removeIdentifier(OperationIdType.BLOOMBERG_CLORD_ID);
					}
					// requested on March 2019 rendez vous un Zurich currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
					currentAttempt.getMarketOrder().setMarketMarketMaker(null);
						operation.setStateResilient(new BBG_StartExecutionState(), ErrorState.class);
				break;
			case TW:
				String twSessionId = operation.getIdentifier(OperationIdType.TW_SESSION_ID);
				if (twSessionId != null) {
					operation.removeIdentifier(OperationIdType.TW_SESSION_ID);
				}

				// requested on March 2019 rendez vous un Zurich currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
				currentAttempt.getMarketOrder().setMarketMarketMaker(null);
					operation.setStateResilient(new TW_StartExecutionState(), ErrorState.class);
				break;
			case MARKETAXESS:
				String maSessionId = operation.getIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
				if (maSessionId != null) {
					operation.removeIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
				}

					operation.setStateResilient(new MA_StartExecutionState(), ErrorState.class);
				break;
			default:
				operation.removeLastAttempt();
				operation.setStateResilient(new WarningState(operation.getState(), null, Messages.getString("MARKET_UNKNOWN",
						currentAttempt.getMarketOrder().getMarket().getMarketCode().name())), ErrorState.class);
			}
	}

	public CSExecutionStrategyService() {
		super();
	}

	public CSExecutionStrategyService(Operation operation, PriceResult priceResult, ApplicationStatus applicationStatus, MifidConfig mifidConfig) {
		if (operation == null) {
			throw new IllegalArgumentException("operation is null");
		}
		this.operation = operation;
		this.priceResult = priceResult;
		this.applicationStatus = applicationStatus;
		this.mifidConfig = mifidConfig;
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
			manageAutomaticUnexecution(order, order.getCustomer(), "Fallback: ");
			return;
		}
		
		// ###  End
		if (operation.hasPassedMaxAttempt(this.mifidConfig.getNumRetry() - 1)) {
			ExecutionReportHelper.prepareForAutoNotExecution(operation, serialNumberService, ExecutionReportState.REJECTED);
			operation.setStateResilient(new SendAutoNotExecutionReportState(Messages.getString("EventNoMoreRetry.0")), ErrorState.class);
			return;
		}
		//BESTX-865 retry a price discovery every attempt
		operation.setStateResilient(new WaitingPriceState(), ErrorState.class);
		
	}


	@Override
	public void acceptOrderRevoke(Operation operation, Attempt currentAttempt,
			SerialNumberService serialNumberService) {
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
//	    	if(operation.isNotAutoExecute())
//	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
//	    	else
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
//	    	if(this.operation.isNotAutoExecute())
//	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
//	        else
	    		this.operation.setStateResilient(new LimitFileNoPriceState(message), ErrorState.class);
	        break;
	    case LimitFile:
	        //Update the BestAndLimitDelta field on the TabHistoryOrdini table
	        Order order = this.operation.getOrder();
           OrderHelper.setOrderBestPriceDeviationFromLimit(operation);
	        OperationStateAuditDAOProvider.getOperationStateAuditDao().updateOrderBestAndLimitDelta(order, order.getBestPriceDeviationFromLimit());
//	    	if(operation.isNotAutoExecute())
//	    		this.operation.setStateResilient(new CurandoState(Messages.getString("LimitFile.doNotExecute")), ErrorState.class);
//	    	else
	    		this.operation.setStateResilient(new OrderNotExecutableState(message), ErrorState.class);
	        break;
	    default:
	        LOGGER.error("Order {}, unexpected behaviour while checking for automatic not execution or magnet.", this.operation.getOrder().getFixOrderId());
	        this.operation.setStateResilient(new WarningState(this.operation.getState(), null, message), ErrorState.class);
	        break;
	    }
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