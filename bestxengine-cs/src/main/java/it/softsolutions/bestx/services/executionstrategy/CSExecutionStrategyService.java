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
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.handlers.BookHelper;
import it.softsolutions.bestx.handlers.ExecutionReportHelper;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.bestx.states.ErrorState;
import it.softsolutions.bestx.states.SendAutoNotExecutionReportState;
import it.softsolutions.bestx.states.SendNotExecutionReportState;
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
public abstract class CSExecutionStrategyService implements ExecutionStrategyService {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(CSExecutionStrategyService.class);
    protected ExecutionStrategyServiceCallback executionStrategyServiceCallback;
    protected PriceResult priceResult = null;
    protected boolean rejectOrderWhenBloombergIsBest;
    
    protected List<MarketCode> allMarketsToTry;
	protected Operation operation;

	public List<MarketCode> getAllMarketsToTry() {
		return allMarketsToTry;
	}

	public void setAllMarketsToTry(List<MarketCode> allMarketsToTry) {
		this.allMarketsToTry = allMarketsToTry;
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
	    //we must always preserve the existing comment, because it could be the one sent to us through OTEX
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
	            currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
	            operation.setStateResilient(new BBG_StartExecutionState(), ErrorState.class);
	        }
	        break;
	    case TW:
	        String twSessionId = operation.getIdentifier(OperationIdType.TW_SESSION_ID);
	        if (twSessionId != null) {
	            operation.removeIdentifier(OperationIdType.TW_SESSION_ID);
	        }
	
	        currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
	        operation.setStateResilient(new TW_StartExecutionState(), ErrorState.class);
	        break;
	    case MARKETAXESS:
	        String maSessionId = operation.getIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
	        if (maSessionId != null) {
	            operation.removeIdentifier(OperationIdType.MARKETAXESS_SESSION_ID);
	        }
	
	        currentAttempt.getMarketOrder().setVenue(currentAttempt.getExecutionProposal().getVenue());
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
	
	@Deprecated
    public CSExecutionStrategyService(ExecutionStrategyServiceCallback executionStrategyServiceCallback, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
        if (executionStrategyServiceCallback == null) {
            throw new IllegalArgumentException("executionStrategyServiceCallback is null");
        }

        this.executionStrategyServiceCallback = executionStrategyServiceCallback;
        this.priceResult = priceResult;
        this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;
    }
    
    public CSExecutionStrategyService(Operation operation, PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
        if (operation == null) {
            throw new IllegalArgumentException("operation is null");
        }

        this.operation = operation;
        this.priceResult = priceResult;
        this.rejectOrderWhenBloombergIsBest = rejectOrderWhenBloombergIsBest;
    }

    /**
     * Manages the rejection from a market (replace the RejectStatus handler
     * @param operation the operation being rejected
     * @param currentAttempt the attempt being failed (got reject)
     * @param numberSerialService ignored
     */
    @Override
    public void manageMarketReject(Operation operation, Attempt currentAttempt, SerialNumberService serialNumberService) throws BestXException {
    	// TODO manage customer revoke
    	if(operation.isCustomerRevokeReceived()) {
    		this.manageOrderRevoke(operation, currentAttempt, serialNumberService);
    	}
    	// move book to a new attempt
    	operation.addAttempt();
		Order customerOrder = operation.getOrder();
    	Attempt newAttempt = operation.getLastAttempt();
    	newAttempt.setSortedBook(currentAttempt.getSortedBook());
    	MarketCode marketCode = this.getNextMarketToTry(operation, currentAttempt.getExecutionProposal().getMarket().getMarketCode());
    	if(marketCode == null) {
    		this.manageAutomaticUnexecution(customerOrder, customerOrder.getCustomer());
    		return;
    	} else {
    		ClassifiedProposal executionProposal = null;
    		while (executionProposal == null && marketCode != null)
    		{
    			executionProposal = BookHelper.getBestPrice(currentAttempt.getSortedBook(), marketCode, customerOrder.getSide());
    			marketCode = this.getNextMarketToTry(operation, marketCode);
    		}
    		if(marketCode == null) {  // all marketCodes have been used
    			this.manageAutomaticUnexecution(customerOrder, customerOrder.getCustomer());
    			return;
    		}
			if(executionProposal == null)
			{
				// no proposals for all markets
    			this.manageAutomaticUnexecution(customerOrder, customerOrder.getCustomer());
    			return;				
			} else {
				newAttempt.setExecutionProposal(executionProposal);
			}
			
			MarketOrder marketOrder = new MarketOrder();	
	        // maintain price
            marketOrder.setValues(currentAttempt.getMarketOrder());
            marketOrder.setTransactTime(DateService.newUTCDate());
            if (currentAttempt.getExecutionProposal() != null) {
                marketOrder.setMarket(currentAttempt.getExecutionProposal().getMarket());
                marketOrder.setMarketMarketMaker(executionProposal.getMarketMarketMaker());
            }
            newAttempt.setMarketOrder(marketOrder);
    		// do not go to marketOrder, go directly to execution
    		this.startExecution(operation, currentAttempt, serialNumberService);
    	}
    }

    @Override
    public void manageOrderRevoke(Operation operation, Attempt currentAttempt,
			SerialNumberService serialNumberService) {
		operation.setStateResilient(new SendNotExecutionReportState("Revoke accepted"), ErrorState.class);	
	}

	/** get from the market codes in the getAllMarketsToTry which is the first one not used in the current attempt cycle
	 * 
	 * @param operation needed to get the market code used in all the attempts in current attempt cycle
	 * @return
	 */
	private MarketCode getNextMarketToTry(Operation operation, MarketCode marketCode) throws IllegalArgumentException{
		if(marketCode == null) throw new IllegalArgumentException("null MarketCode not allowed in getNextMarketToTry()");
		// copy all markets to be tried in local parameter
		List<MarketCode> currentMarkets = new ArrayList<MarketCode>();
		currentMarkets.addAll(getAllMarketsToTry());
		// remove from all markets all the tried ones in current execution cycle
		List<Attempt> currentAttempts = 
				operation.getAttempts().subList(operation.getFirstAttemptInCurrentCycle(), operation.getAttempts().size() - 1);

//		currentAttempts.forEach(new AttemptConsumer(currentMarkets));
		currentAttempts.forEach(attempt->{ 
			if(attempt.getMarketOrder()!= null) 
				currentMarkets.remove(attempt.getMarketOrder().getMarket().getMarketCode());
			});
		// return first not tried or null
		if(currentMarkets.size() > 0 && currentMarkets.contains(marketCode)) {
			int marketCodeIndex = currentMarkets.indexOf(marketCode);
			return marketCodeIndex < currentMarkets.size() ? currentMarkets.get(marketCodeIndex) : null;
		}
		return null;
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