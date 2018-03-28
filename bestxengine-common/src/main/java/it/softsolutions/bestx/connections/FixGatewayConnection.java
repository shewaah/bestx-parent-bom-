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

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.fixgateway.FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderRejectOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixQuoteOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqResponseOutputLazyBean;

public interface FixGatewayConnection extends Connection {
    
    void setFixGatewayListener(FixGatewayConnectionListener listener);

    void sendRfqResponse(FixGatewayConnectionListener source, FixRfqResponseOutputLazyBean rfqResponse) throws BestXException;

    void sendQuote(FixGatewayConnectionListener source, FixQuoteOutputLazyBean quote) throws BestXException;

    void sendOrderReject(FixGatewayConnectionListener source, FixOrderRejectOutputLazyBean orderReject) throws BestXException;

    void sendExecutionReport(FixGatewayConnectionListener source, FixExecutionReportOutputLazyBean executionReport) throws BestXException;

    void sendOrderResponse(FixGatewayConnectionListener source, FixOrderResponseOutputLazyBean orderResponse) throws BestXException;

    void sendRevokeResponse(FixGatewayConnectionListener source, String revokeId, FixRevokeResponseOutputLazyBean revokeResponse) throws BestXException;

    void sendRevokeReport(FixGatewayConnectionListener source, String revokeId, FixRevokeReportLazyBean revokeReport) throws BestXException;

    /**
     * Send a revoke report starting from a generic FixOutputLazyBean
     * 
     * @param source
     * @param revokeId
     * @param revokeReport
     * @throws BestXException
     */
    void sendGenericRevokeReport(FixGatewayConnectionListener source, String revokeId, FixOutputLazyBean revokeReport) throws BestXException;

}
