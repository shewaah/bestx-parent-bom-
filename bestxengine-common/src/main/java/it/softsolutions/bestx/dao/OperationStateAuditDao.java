/*
* Project Name : bestxengine-common
* First created by: matteo.salis
* Creation date: 10/mag/2012
*
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
*
*/

package it.softsolutions.bestx.dao;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.exceptions.SaveBookException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.TradeFill;
import it.softsolutions.jsscommon.Money;

/**
 * 
 * Interface OperationStateAuditDao
 *
 */
public interface OperationStateAuditDao {

    // [DR20120830] Per ora le mettiamo qui perchè le usa solo questo DAO, ma sarebbero da mettere a livello di prodotto e sfruttate anche per altri scopi
    public enum Action {
        AUTO_MANUAL_ORDER,
        COMMAND_RESEND_EXECUTIONREPORT,
        FORCE_EXECUTED,
        FORCE_NOT_EXECUTED,
        FORCE_RECEIVED,
        MANUAL_MANAGE,
        MERGE_ORDER,
        NOT_EXECUTED,
        ORDER_RETRY,
        SEND_DDE_DATA,
        SEND_DES_DATA,
        STOP_TLX_EXEC,
        SEND_MSG_TO_OTEX
    }
    
	/**
	 * Save Market attempt
	 * @param orderId
	 * @param attemptNo
	 * @param marketCode
	 * @param disabled
	 * @param disabledComment
	 */
	void saveMarketAttemptStatus(String orderId, int attemptNo, MarketCode marketCode, boolean disabled, String disabledComment);
	
	/**
	 * Save new State
	 * @param orderId
	 * @param previousState
	 * @param currentState
	 * @param attemptNo
	 * @param comment
	 */
	void saveNewState(String orderId, OperationState previousState, OperationState currentState, int attemptNo, String comment);
	
	/**
	 * Save new Order
	 * @param order
	 * @param currentState
	 * @param propName
	 * @param propCode
	 * @param operatorCode
	 * @param event
	 * @param availableActions
	 * @param receiveTime
	 * @param sessionId
	 * @param notAutoExecute
	 */
	void saveNewOrder(Order order, OperationState currentState, String propName, String propCode, String operatorCode, String event, OperationStateAuditDao.Action[] availableActions, Date receiveTime, String sessionId, boolean notAutoExecute);
	
	/**
	 * Update ProposalName by OrderID
	 * @param orderId
	 * @param propName
	 */
	void updateOrderPropName(String orderId, String propName);
	
	/**
	 * Update Revoke State
	 * @param orderId
	 * @param revoked
	 * @param revokeTime
	 * @param revokeNumber
	 */
	void updateRevokeState(String orderId, boolean revoked, Date revokeTime, String revokeNumber);
	
	/**
	 * Update Order
	 * @param order
	 * @param currentState
	 * @param handlingState
	 * @param filter262Passed
	 * @param event
	 * @param availableActions
	 * @param notes
	 * @param notAutoExecute
	 */
	void updateOrder(Order order, OperationState currentState, boolean handlingState, boolean filter262Passed, String event, OperationStateAuditDao.Action[] availableActions, String notes, boolean notAutoExecute);
	
	/**
	 * Update Order Fill
	 * @param order
	 * @param ticketNum
	 * @param accruedInterest
	 * @param accruedInterestDays
	 */
	void updateOrderFill(Order order, String ticketNum, Money accruedInterest, Integer accruedInterestDays);
	
	/**
	 * Finalize Order
	 * @param order
	 * @param lastAttempt
	 * @param executionReport
	 * @param fulfillingTime
	 */
	void finalizeOrder(Order order, Attempt lastAttempt, ExecutionReport executionReport, Date fulfillingTime);
	
	/**
	 * Save new attempt
	 * @param orderId
	 * @param attempt
	 * @param tsn
	 * @param attemptNo
	 * @param ticketNum
	 * @param lastSavedAttempt the number of last saved attempt in the operation
	 * @return the number of the last saved attempt
	 */
	int saveNewAttempt(String orderId, Attempt attempt, String tsn, int attemptNo, String ticketNum, int lastSavedAttempt);
	
	/**
	 * Update attempt already created
	 * @param orderId
	 * @param attempt
	 * @param tsn
	 * @param attemptNo
	 * @param ticketNum
	 * @param executionReport
	 */
	void updateAttempt(String orderId, Attempt attempt, String tsn, int attemptNo, String ticketNum, ExecutionReport executionReport);
	
	/**
	 * Save new Book
	 * @param orderId
	 * @param attemptNo
	 * @param sortedBook
	 * @throws SaveBookException
	 */
	void saveNewBook(String orderId, int attemptNo, SortedBook sortedBook) throws SaveBookException;
	
	/**
	 * Save the list of Executable prices
	 * @param orderId the FIX Order ID
	 * @param attemptNo the attempt number
	 * @param executablePrices the list of executable prices
	 * @throws Exception if any issues arise
	 */
	void saveExecutablePrices(String orderId, int attemptNo, String isin, List<ExecutablePrice> executablePrices) throws Exception;
	
	/**
	 * Check for already used Order Id
	 * @param id
	 * @return
	 */
	boolean usedOrderId(String id);
	
	/**
	 * Add Order Count
	 */
	void addOrderCount();
	
	/**
	 * Save all fills for <Order>
	 * @param order
	 * @param fills
	 */
//    void saveAllFills(Order order, List<MarketExecutionReport> fills);
    
    /**
     * Save new Fill for <Order>
     * @param order
     * @param fill
     */
    void saveNewFill(Order order, MarketExecutionReport fill);
    
    /**
     * Save cancel Fill for <Order>
     * @param order
     * @param fill
     */
//    void saveCancelFill(Order order, MarketExecutionReport fill);
    
    /**
     * Svae Price Fill for <Order>
     * @param order
     * @param fill
     */
    void savePriceFill(Order order, MarketExecutionReport fill);
	
    /**
     * Update Order attempt
     * @param orderId
     * @param attemptNo
     * @param matchingOrderId
     */
    void updateMatchingOrderAttempt(String orderId, int attemptNo, String matchingOrderId);
	
    /**
     * Add Bloomberg Trade
     * @param trade
     */
    void addBloombergTrade(TradeFill trade);
    
    /**
     * Assign Blloomber Trade To Order
     * @param trade
     * @param order
     */
	void assignBloombergTradeToOrder(TradeFill trade, Order order);
	
	/**
	 * Update Order Status Description
	 * @param order
	 * @param price
	 */
	void updateOrderStatusDescription(final Order order, final BigDecimal price);
	
	/**
	 * Update Order Execution Destination
	 * @param orderId
	 * @param executionDestination
	 */
	void updateOrderExecutionDestination(String orderId, String executionDestination);
	
	/**
	 * Update settlement date for Order
	 * @param order
	 */
	void updateOrderSettlementDate(final Order order);
	

	/**
	 * update the next execution time to show to web in limit files management
	 * 
	 * @param order
	 * @param nextExecutionTime
	 */
   void updateOrderNextExecutionTime(final Order order, final Date nextExecutionTime);
	
	
	/**
	 * 
	 * @return
	 */
   public List<String> getMagnetOperations();
   
   /**
    * Check if the new attempt has already been saved in the database
    * @param order order to be checked
    * @param newAttemptNo new attempt number
    * @return true if already saved, false otherwise
    */
    public boolean attemptAlreadySaved(final Order order, final int newAttemptNo);

    /**
     * Get the list of the orders that must be closed at the end of day
     * @return List of numOrder
     */
    List<String> getEndOfDayOrdersToClose();
        
    /**
     * Update the field BestAndLimitDelta of the TabHistoryOrdini with the new value of the delta between the limit price and the best
     * proposal price
     * @param order the order
     * @param bestAndLimitDelta the new delta value
     */
    public void updateOrderBestAndLimitDelta(final Order order, final Double bestAndLimitDelta);  
    
    /**
     * Updates the tab history's operator code field
     * @param orderId
     * @param operatorCode
     */
    public void updateTabHistoryOperatorCode(String orderId, String operatorCode);
}