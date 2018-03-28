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

import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq;

/**
 * 
 * Purpose: this is the interface for every listener to a customer connection
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 08/ott/2012
 * 
 **/
public interface CustomerConnectionListener {
    
    /**
     * Callback for an RFQ reception
     * 
     * @param source
     *            : source connection
     * @param rfq
     *            : customer RFQ
     */
    void onCustomerRfq(CustomerConnection source, Rfq rfq);

    /**
     * Callback for a customer acknowledgement of a quote by the customer
     * 
     * @param source
     *            : source connection
     * @param quote
     *            : customer quote acknowledged
     */
    void onCustomerQuoteAcknowledged(CustomerConnection source, Quote quote);

    /**
     * Callback for a customer not acknowledgement of a quote by the customer
     * 
     * @param source
     *            : source connection
     * @param quote
     *            : customer quote not acknowledged
     */
    void onCustomerQuoteNotAcknowledged(CustomerConnection source, Quote quote, Integer errorCode, String errorMessage);

    /**
     * Callback for a customer order reception
     * 
     * @param source
     *            : source connection
     * @param order
     *            : the customer order
     */
    void onCustomerOrder(CustomerConnection source, Order order);

    /**
     * Callback for a customer acknowledgement of an execution report
     * 
     * @param source
     *            : source connection
     * @param executionReport
     *            : acknowledged execution report
     */
    void onCustomerExecutionReportAcknowledged(CustomerConnection source, ExecutionReport executionReport);

    /**
     * Callback for a customer not acknowledgement of an execution report
     * 
     * @param source
     *            : source connection
     * @param executionReport
     *            : not acknowledged execution report
     */
    void onCustomerExecutionReportNotAcknowledged(CustomerConnection source, ExecutionReport executionReport, Integer errorCode, String errorMessage);

    /**
     * Callback for a revoke reception
     * 
     * @param source
     *            : source connection
     */
    void onFixRevoke(CustomerConnection source);
}
