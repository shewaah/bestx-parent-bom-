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

import java.util.Date;

import it.softsolutions.bestx.model.Market;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public interface OperationState extends Cloneable {

    // [RR20120903] rimosso CurandoPopup, pare non essere usato
    public enum Type {
        AcceptQuote, BusinessValidation, Cancelled, CurandoAuto, CurandoRetry, Curando, DifferentDatesExecuted, Error, Executed, MarketExecuted, Filling, FormalValidationOK, FormalValidationKO, Initial, InternalInCurando, InternalRejectInternalAndAcceptBestState, InternalAcceptInternalAndRejectBestState, MagnetNotExecution, ManageCounter, ManualExecutionWaitingPrice, ManualManage, ManualWaitingFill, MatchFound, NotExecuted, OrderNotExecutable, OrderNotExecuted, OrderReceived, OrderRejectable, OrderRevocated, ReceiveQuote, InternalReceiveInternalQuote, InternalAcceptBestIfStillValid, InternalReceiveExecutableQuote, Rejected, RejectQuote, Revocation, SendAutoNotExecutionReport, SendExecutionReport, SendNotExecutionReport, SendOrder, SendReports, SendRfq, InternalSendRfqToBest, StandbyBook, StandbyNoBook, Standby, StandbyWithBook, StandbyWithoutBook, StartExecution, InternalSendRfqToInternal, StartMagnetExecution, UnreconciledTrade, ValidateByPunctualFilter, WaitFillAfterCancel, WaitFillAfterRevocation, WaitingFill, WaitingPrice, Warning, InternalGetExecutableQuote, RejectQuoteAndAutoNotExecutionReport, LimitFileNoPrice, PriceDiscovery, LimitFileParkedOrder
    }

    /**
     * Check if the state is a terminal one
     * 
     * @return true or false
     */
    boolean isTerminal();

    /**
     * Get the time when the operation entered in this state
     * 
     * @return a date
     */
    Date getEnteredTime();

    /**
     * Get the state comment
     * 
     * @return a comment
     */
    String getComment();

    /**
     * Set the state comment
     * 
     * @param comment
     *            : the state comment
     */
    void setComment(String comment);

    /**
     * Set the state operation
     * 
     * @param owner
     *            : state operation
     */
    void setOperation(Operation owner);

    /**
     * Set the state operation together with the time it enters in this state
     * 
     * @param owner
     *            : the operation
     * @param enteredTime
     *            : the time
     */
    void setOperation(Operation owner, Date enteredTime);

    /**
     * State validation
     * 
     * @throws BestXException
     *             : is something is wrong
     */
    void validate() throws BestXException;

    /**
     * Check if, in this state, revokes can be automatically accepted
     * 
     * @return true or false
     */
    boolean isRevocable();

    /**
     * Clone the state
     * 
     * @return a clone
     */
    OperationState clone() throws CloneNotSupportedException;

    /**
     * Check if entering in this state we must save the book in the database
     * 
     * @return true or false
     */
    boolean mustSaveBook();

    /**
     * Get the state Type
     * 
     * @return a it.softsolutions.bestx.OperationState.Type
     */
    Type getType();

    /**
     * Get the state market code if available
     * 
     * @return a Market.MarkeCode or null
     */
    Market.MarketCode getMarketCode();
}
