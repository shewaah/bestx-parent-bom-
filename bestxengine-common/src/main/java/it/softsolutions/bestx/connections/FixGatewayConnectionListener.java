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

import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqInputLazyBean;

/** 

*
* Purpose: this class is mainly for manage the messages exchange between BestX and the customer connection
*
* Project Name : bestxengine-common
* First created by: ruggero.rizzo
* Creation date: 15/giu/2012
*
**/
public interface FixGatewayConnectionListener {
   /**
    * Entry point for new customer orders management
    * @param fixCustomerConnector : order accepting connection
    * @param fixOrder : customer order
    */
    void onFixOrderNotification(FixGatewayConnection fixCustomerConnector, FixOrderInputLazyBean fixOrder);
    /**
     * Entry point for new customer RFQs management
     * @param fixCustomerConnector : RFQ accepting connection
     * @param fixRfq : customer RFQ
     */
    void onFixRfqNotification(FixGatewayConnection fixCustomerConnector, FixRfqInputLazyBean fixRfq);
    /**
     * Quote acknowledgement event management
     * @param fixCustomerConnector : source connection
     * @param quoteRequestId : quote id
     */
    void onFixQuoteAcknowledged(FixGatewayConnection fixCustomerConnector, String quoteRequestId);
    /**
     * Quote not acknowledged event management
     * @param fixCustomerConnector : source connection
     * @param quoteRequestId : quote id
     * @param errorCode : not ack error code
     * @param errorMessage : not ack error message
     */
    void onFixQuoteNotAcknowledged(FixGatewayConnection fixCustomerConnector, String quoteRequestId, Integer errorCode, String errorMessage);
    /**
     * Trade acknowledgement management
     * @param fixCustomerConnector : source connection
     * @param rfqId : quote/order id
     * @param executionReportId : trade id
     */
    void onFixTradeAcknowledged(FixGatewayConnection fixCustomerConnector, String rfqId, String executionReportId);
    /**
     * Trade not acknowledged management
     * @param fixCustomerConnector : source connection
     * @param orderId : quote/order id
     * @param executionReportId : trade id
     * @param errorCode : no ack error code
     * @param errorMessage : no ack error message
     */
    void onFixTradeNotAcknowledged(FixGatewayConnection fixCustomerConnector, String orderId, String executionReportId, Integer errorCode, String errorMessage);
    /**
     * Revoke management
     * @param fixCustomerConnector : source connection
     * @param orderId : order/quote id
     * @param revokeId : revoke id
     * @param fixSessionId : fix session id
     */
    void onFixRevokeNotification(FixGatewayConnection fixCustomerConnector, String orderId, String revokeId, String fixSessionId);
}
