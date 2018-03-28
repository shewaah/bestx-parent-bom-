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
package it.softsolutions.bestx.connections.mts;

import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionEvent;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationExceptionStatus;
import it.softsolutions.bestx.connections.mts.MTSConnection.TradingRelationStatus;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener.StatusField;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.bestx.model.Proposal.ProposalSide;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common
 * First created by: paolo.midali
 * Creation date: 13-nov-2012
 *
 * @see MTSConnectionEvent
 */

public interface MTSConnectionListener
{
    void onConnectionStatusChange(boolean marketStatus, boolean userStatus);
    void onExecutionReport(String sessionId, ExecutionReportState executionReportState, String marketOrderId, String counterpart,  BigDecimal lastPrice, String contractNo, BigDecimal accruedValue, Date marketTime);
    void onQuote(String sessionId, String rfqSeqNo, String quoteId, String marketMaker, BigDecimal price, BigDecimal yield, BigDecimal qty, ProposalSide side, Date futSettDate, Date updateTime, String updateTimeStr);
    void onOrderReject(String sessionId, String reason);
    void onConnectionError();
    void onMemberEnabled(String member, String bondType, boolean enabled);
    void onTradingRelation(String member, TradingRelationStatus status, TradingRelationStatus sellSideSubStatus, TradingRelationStatus buySideSubStatus,TradingRelationEvent event);
    void onTradingRelationException(String member, String bondType, TradingRelationExceptionStatus status, TradingRelationExceptionEvent event);
    void onSecurityDefinition(String isin, String string, Date settlDate, BigDecimal minSize, BigDecimal qtyTick,
                    BigDecimal lotSize, String bondType, int marketAffiliation, String marketAffiliationStr, int quoteIndicator, String quoteIndicatorStr);
    void onDownloadEnd(Connection source);
    void onConnectionStatus(SubMarketCode subMarketCode, boolean status, StatusField statusField);
    void onOrderTechnicalReject(String sessionId, String message);
    void onNullPrices(String baseBVSessionId, String isin, ProposalSide side);
    void onNullPrices(String baseBVSessionId, String isin, String reason);
}
