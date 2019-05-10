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
package it.softsolutions.bestx.connections.bloomberg.tsox;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac.TradeStacConnectionListener;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.tradestac.fix.field.ExecType;
import it.softsolutions.tradestac.fix.field.OrdStatus;
import it.softsolutions.tradestac.fix50.TSExecutionReport;

/**  
 *
 * Purpose: this intergace exposes the methods used by test and production TSOXConnectionListeners
 *
 * Project Name : bestx-bloomberg-market 
 * First created by: davide.rossoni 
 * Creation date: 12/lug/2013
 * 
 **/
public interface TSOXConnectionListener extends TradeStacConnectionListener {
    
    void onCounter(String sessionId, String quoteReqId, String quoteRespId, String quoteId, String marketMaker, BigDecimal price, BigDecimal qty, String currency, 
                    ProposalSide side, ProposalType type, Date futSettDate, int acknowledgeLevel, String onBehalfOfCompID) throws OperationNotExistingException, BestXException;

    void onOrderReject(String sessionId, String quoteReqId, String reason);
    void onCancelReject(String sessionId, String quoteReqId, String reason);

    void onQuoteStatusTimeout(String sessionId, String quoteReqID, String quoteID, String dealer, String text);
    void onQuoteStatusTradeEnded(String sessionId, String quoteReqID, String quoteID, String dealer, String text);
    void onQuoteStatusExpired(String sessionId, String quoteReqID, String quoteID, String dealer);
    
//    void onExecutionReport(String sessionId, String clOrdId, ExecType execType, OrdStatus ordStatus, BigDecimal accruedInterestAmount, BigDecimal accruedInterestRate, BigDecimal lastPrice, String contractNo, Date futSettDate, Date transactTime, String text);
    void onExecutionReport(String sessionId, String clOrdId, @SuppressWarnings("deprecation") TSExecutionReport tsExecutionReport);
}
