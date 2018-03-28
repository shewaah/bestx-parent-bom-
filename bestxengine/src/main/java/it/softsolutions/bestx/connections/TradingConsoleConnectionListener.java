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

import it.softsolutions.bestx.model.ClassifiedProposal;

/**
 * 
 * Purpose: internal market trading console listener
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 09/ott/2012
 * 
 **/
public interface TradingConsoleConnectionListener {
    /**
     * Callback for a trade reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleTradeReceived(TradingConsoleConnection source); // CMF RequestResp ErrorCode == 0

    /**
     * Callback for a trade reject reception from the console
     * 
     * @param source
     *            : console connection
     * @param reason
     *            : reject reason
     */
    void onTradingConsoleTradeRejected(TradingConsoleConnection source, String reason); // CMF RequestResp ErrorCode != 0

    /**
     * Callback for a trade acknowledgement reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleTradeAcknowledged(TradingConsoleConnection source); // CMF Ack

    /**
     * Callback for a trade not acknowledgement reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleTradeNotAcknowledged(TradingConsoleConnection source); // CMF Nack

    /**
     * Callback for a pending accept message reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleOrderPendingAccepted(TradingConsoleConnection source); // CMF PendAccept

    /**
     * Callback for a pending reject message reception from the console
     * 
     * @param source
     *            : console connection
     * @param reason
     *            : reject reason
     */
    void onTradingConsoleOrderPendingRejected(TradingConsoleConnection source, String reason); // CMF PendReject

    /**
     * Callback for a pending expiration message reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleOrderPendingExpired(TradingConsoleConnection source); // CMF PendExpire

    /**
     * Callback for a pending counter reception from the console
     * 
     * @param source
     *            : console connection
     * @param counter
     *            : the counter proposal
     */
    void onTradingConsoleOrderPendingCounter(TradingConsoleConnection source, ClassifiedProposal counter); // CMF PendCounter | InquiryPendCounter

    /**
     * Callback for an order auto execution message reception from the console
     * 
     * @param source
     *            : console connection
     */
    void onTradingConsoleOrderAutoExecution(TradingConsoleConnection source); // CMF AutoExecution

    /**
     * Callback for an error reception from the CMF
     * 
     * @param source
     *            : console connection
     * @param errorMessage
     *            : errore message
     * @param errorCode
     *            : error code
     */
    void onCmfErrorReply(TradingConsoleConnection source, String errorMessage, int errorCode); // CMF Reply with an error, typical the -3
                                                                                               // error code
}
