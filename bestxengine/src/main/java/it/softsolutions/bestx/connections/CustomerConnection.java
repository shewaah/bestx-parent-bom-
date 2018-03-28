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
package it.softsolutions.bestx.connections;

import java.math.BigDecimal;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.Operation.RevocationState;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;

/** 

*
* Purpose: this class is mainly for manage the sending of messages to the customer connection
*
* Project Name : bestxengine
* First created by: ruggero.rizzo
* Creation date: 15/giu/2012
*
**/
public interface CustomerConnection extends Connection {
    
    public static enum ErrorCode {
        GENERIC_ERROR(-1),
        OK(0),
        UNKNOWN_SYMBOL(1),
        EXCHANGE_CLOSED(2),
        ORDER_EXCEEDS_LIMIT(3),
        TOO_LATE_TO_ENTER(4),
        UNKNOWN_ORDER(5),
        DUPLICATE_ORDER(6),
        INVALID_BID_ASK_SPREAD(7),
        INVALID_PRICE(8),
        NOT_AUTHORIZED_TO_QUOTE_SECURITY(9);
        private int code;
        private ErrorCode(int code) {
            this.code = code;
        }
        public int getCode() {
            return code;
        }
    }

    /**
     * Send an RFQ reject message
     * 
     * @param source
     *            : operation whose RQF has been rejected
     * @param rfq
     *            : the rfq
     * @param rfqId
     *            : rfq id
     * @param errorCode
     *            : reject code
     * @param reason
     *            : reject reason
     * @throws BestXException
     *             : exception
     */
    void sendRfqReject(Operation source, Rfq rfq, String rfqId, ErrorCode errorCode, String reason) throws BestXException;

    /**
     * Send an RFQ response message
     * 
     * @param source
     *            : RFQ response operation
     * @param rfq
     *            : the rfq
     * @param rfqId
     *            : rfq id
     * @param quote
     *            : quote received
     * @throws BestXException
     *             : exception
     */
    void sendRfqResponse(Operation source, Rfq rfq, String rfqId, Quote quote) throws BestXException;

    /**
     * Send an order reject message
     * 
     * @param source
     *            : rejected order operation
     * @param order
     *            : the order rejected
     * @param orderId
     *            : order id
     * @param executionReport
     *            : rejection execution report
     * @param errorCode
     *            : reject code
     * @param rejectReason
     *            : reject reason
     * @throws BestXException
     *             : exception
     */
    void sendOrderReject(Operation source, Order order, String orderId, ExecutionReport executionReport, ErrorCode errorCode, String rejectReason) throws BestXException;

    /**
     * Send an execution report message
     * 
     * @param source
     *            : execution report operation
     * @param quote
     *            : quote received if available
     * @param order
     *            : the order
     * @param orderId
     *            : order id
     * @param attempt
     *            : execution attempt
     * @param executionReport
     *            : execution report
     * @throws BestXException
     *             : exception
     */
    void sendExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport) throws BestXException;

    /**
     * Send an Ack on order reception
     * 
     * @param source
     *            : order operation
     * @param order
     *            : the order
     * @param orderId
     *            : order id
     * @throws BestXException
     *             : exception
     */
    void sendOrderResponseAck(Operation source, Order order, String orderId) throws BestXException;

    /**
     * Send a not acknoledgement on an order reception
     * 
     * @param source
     *            : order operation
     * @param order
     *            : the order
     * @param orderId
     *            : order id
     * @param errorMsg
     *            : not ack message
     * @throws BestXException
     *             : exception
     */
    void sendOrderResponseNack(Operation source, Order order, String orderId, String errorMsg) throws BestXException;

    /**
     * Send a revoke acknowledgement
     * 
     * @param source
     *            : revoke operation
     * @param orderId
     *            : order id
     * @param comment
     *            : optional comment
     * @throws BestXException
     */
    void sendRevokeAck(Operation source, String orderId, String comment) throws BestXException;

    /**
     * Send a not acknowledgement on a revoke reception
     * 
     * @param source
     *            : revoke operation
     * @param order
     *            : the order revoked
     * @param comment
     *            : not ack comment
     * @throws BestXException
     *             : exception
     */
    void sendRevokeNack(Operation source, Order order, String comment) throws BestXException;

    /**
     * Send a revoke report
     * 
     * @param source
     *            : revoke operation
     * @param order
     *            : the order
     * @param revocationState
     *            : revocation state {@link RevocationState}
     * @param comment
     *            : optional comment
     * @throws BestXException
     *             : exception
     */
    void sendRevokeReport(Operation source, Order order, RevocationState revocationState, String comment) throws BestXException;

    /**
     * Send a partial fill execution report
     * 
     * @param source
     *            : partially filled operation
     * @param quote
     *            : a quote if available
     * @param order
     *            : the order
     * @param orderId
     *            : order id
     * @param attempt
     *            : execution attempt
     * @param executionReport
     *            : partial fill execution report
     * @param cumQty
     *            : quantity executed
     * @throws BestXException
     *             : exception
     */
    void sendPartialFillExecutionReport(Operation source, Quote quote, Order order, String orderId, Attempt attempt, ExecutionReport executionReport, BigDecimal cumQty) throws BestXException;

    /**
     * Tell if the operation is related to an order coming from a source that requires partial fill messages
     * 
     * @param operation
     *            : operation to be checked
     * @return true if the customer connection requires the partial fills, false otherwise
     */
    boolean isFillsManager(Operation operation);
    
    
    /**
     * Send an order cancel reject received from the market to the customer
     * 
     * @param source
     *            : operation that cannot be revoked
     * @param comment
     *            : reject comment
     * @throws BestXException
     *             : exception
     */
    void sendOrderCancelReject(Operation source, String comment) throws BestXException;
    
    /**
     * Get channel name to tag status
     *  
     */
    String getChannelName();
}
