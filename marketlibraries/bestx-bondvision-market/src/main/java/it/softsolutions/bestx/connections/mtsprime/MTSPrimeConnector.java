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
package it.softsolutions.bestx.connections.mtsprime;

import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE_ITEM_COUNT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_DEFINED_FILL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_EVENT_TYPE_ORDER_FAILURE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_MESSAGE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_ORDER_NUM;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_ORDER_STATUS_COMPLETEFILL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_ORDER_STATUS_REFUSED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_ORDER_STATUS_ZEROFILL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.CRD_TRADE_COMMENT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_EXT_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_FILL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_EXECUTION_REPORT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_PRICE_RESPONSE;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.bondvision.BondVisionRFCQOutputLazyBean;
import it.softsolutions.bestx.connections.mts.MTSConnector;
import it.softsolutions.bestx.connections.mts.MTSMessageFields;
import it.softsolutions.bestx.connections.regulated.FasOrderOutputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener;
import it.softsolutions.bestx.connections.regulated.RegulatedProposalInputLazyBean;
import it.softsolutions.bestx.connections.xt2.XT2MessageFields;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * 
 * Purpose: This class manages the connection with the MTS Prime market
 * 
 * Project Name : bestxengine-product First created by: stefano.pontillo Creation date: 06/giu/2012
 * 
 **/
public class MTSPrimeConnector extends MTSConnector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MTSPrimeConnector.class);
    
    private RegulatedConnectionListener regulatedConnectionListener;

    @Override
    public void onNotification(XT2Msg msg) {
        if (msg.getSubject().indexOf(SUBJECT_PRICE_RESPONSE) >= 0) {
            String bvSessionId = msg.getString(LABEL_REG_SESSION_ID);
            LOGGER.debug("Price arrived is: {}", msg);

            String bidPrefix = "Bid.";
            String askPrefix = "Ask.";
            int bidPriceNumber = 0;
            int askPriceNumber = 0;
            try {
                bidPriceNumber = msg.getInt(bidPrefix + LABEL_PRICE_ITEM_COUNT);
                askPriceNumber = msg.getInt(askPrefix + LABEL_PRICE_ITEM_COUNT);
            } catch (Exception e) {
                LOGGER.error("Error while retrieving data from price notification [{}]: {}", msg, e.getMessage(), e);
                return;
            }
            ArrayList<RegulatedProposalInputLazyBean> proposals = new ArrayList<RegulatedProposalInputLazyBean>();
            for (int rowIdx = 1; rowIdx <= bidPriceNumber; rowIdx++) {
                try {
                    proposals.add(new RegulatedProposalInputLazyBean(msg, rowIdx, bidPrefix));
                } catch (Exception e) {
                    LOGGER.error("Missing field in CRD price response " + msg.toString() + ", cannot process prices.");
                    return;
                }
            }
            // send BID proposals
            regulatedConnectionListener.onInstrumentPrices(bvSessionId, proposals);
            proposals.clear();
            for (int rowIdx = 1; rowIdx <= askPriceNumber; rowIdx++) {
                try {
                    proposals.add(new RegulatedProposalInputLazyBean(msg, rowIdx, askPrefix));
                } catch (Exception e) {
                    LOGGER.error("Missing field in CRD price response {}, cannot process prices.", msg);
                    return;
                }
            }
            // send ASK proposals
            regulatedConnectionListener.onInstrumentPrices(bvSessionId, proposals);
            return;
        } else if (msg.getName().indexOf(XT2MessageFields.NAME_EVENT_NOTIFY) >= 0) {
            String bvSessionId = msg.getString(LABEL_EXT_SESSION_ID);
            String reason = msg.getString(CRD_MESSAGE);
            int eventType = -1;
            
            try {
                eventType = msg.getInt(XT2MessageFields.LABEL_EVENT_TYPE);
            } catch (Exception e) {
                LOGGER.error("Error while retrieving field {} from price notification [{}]: {}", XT2MessageFields.LABEL_EVENT_TYPE, msg, e.getMessage(), e);
                return;
            }
            
            switch (eventType) {
            case CRD_EVENT_TYPE_ORDER_FAILURE:
                LOGGER.info("Notification of invalid order with reason [" + reason + "] for execution attempt on MTSPrime with session id " + bvSessionId);
                regulatedConnectionListener.onOrderTechnicalReject(bvSessionId, reason);
                break;
            default:
                LOGGER.info("Notification of an unmanaged event from MTSPrime for execution attempt with session id " + bvSessionId);
                break;
            }
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onNotification(msg);
        }
    }

    @Override
    public void onPublish(XT2Msg msg) {
        if (discardIdle(msg)) {
            return;
        }

        if (msg.getSubject().indexOf(SUBJECT_EXECUTION_REPORT) > 0 || msg.getSubject().indexOf(LABEL_ORDER) > 0 || msg.getSubject().indexOf(LABEL_FILL) > 0) {
            
            LOGGER.info("Execution report received from the Market: {}", msg);
            
            String regSessionId = msg.getString(LABEL_EXT_SESSION_ID);
            String orderId = null;
            try {
                orderId = "" + msg.getLong(CRD_ORDER_NUM);
            } catch (Exception e) {
                LOGGER.error("FILL with OrderNum = null, msg = {}: {}", msg, e.getMessage());
                return;
            }

            if (regulatedConnectionListener == null) {
                LOGGER.error("NO Regulated Listener available");
                return;
            }
            if (regSessionId == null && orderId == null) {
                LOGGER.info("Regulated FILL with null CD: {}", msg);
                return;
            }
            int status = -1;
            try {
                status = msg.getInt("Status");
            } catch (Exception e) {
                LOGGER.error("FILL with Status = null, msg = {}: {}", msg, e.getMessage());
                return;
            }

            if (msg.getSubject().indexOf(LABEL_FILL) > 0) {
                // In caso di FILL le gestisco
                int settStatus = -1;
                try {
                    settStatus = msg.getInt("SettlStatus");
                } catch (Exception e) {
                    LOGGER.error("FILL with SettlStatus = null, msg = {}: {}", msg, e.getMessage());
                    return;
                }
                // gestisco solo le fill con SettleStatus 2
                if (settStatus == CRD_DEFINED_FILL) {
                    regulatedConnectionListener.onExecutionReport(orderId, ExecutionReportState.FILLED, new MTSPrimeFillInputBean(msg, regSessionId));
                } else {
                    LOGGER.info("FILL with STATUS {}, ignoring it. We work only on the Defined ones, SettlStatus {}", settStatus, CRD_DEFINED_FILL);
                }
            } else {
                // In caso di messaggi di stato dell'ordine li gestisco
                switch (status) {
                case CRD_ORDER_STATUS_REFUSED:
                    regulatedConnectionListener.onOrderReject(regSessionId, null);
                    break;
                case CRD_ORDER_STATUS_COMPLETEFILL:
                    regulatedConnectionListener.onOrdeReceived(regSessionId, orderId);
                    break;
                // case CRD_ORDER_STATUS_PARTIALFILL:
                // regulatedConnectionListener.onExecutionReport(regSessionId, ExecutionReportState.PART_FILL, new
                // MTSPrimeOrderInputBean(msg, regSessionId));
                // break;
                case CRD_ORDER_STATUS_ZEROFILL:
                    String reason = msg.getString(CRD_MESSAGE);
                    if (reason == null) {
                        reason = msg.getString(CRD_TRADE_COMMENT);
                    }
                    // No technical Reject management because MTSC send to us also a notification for ZeroFill status
                    // regulatedConnectionListener.onOrderTechnicalReject(regSessionId, reason);
                    StringBuilder message = new StringBuilder(250);
                    message.append("Order ").append(orderId).append(", received a zero fill execution report with reason ").append(reason);
                    LOGGER.warn("{}", message);
                    break;
                default:
                    LOGGER.error("Regulated FILL declared order status not recognized: " + msg.toString());
                }
            }
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onPublish(msg);
        }
    }

    @Override
    public void onReply(XT2Msg msg) {
        super.onReply(msg);
    }

    public void acceptQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void rejectQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendRfq(String bvSessionId, BondVisionRFCQOutputLazyBean rfq) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRegulatedConnectionListener(RegulatedConnectionListener listener) {
        regulatedConnectionListener = listener;
    }

    @Override
    public void sendFokOrder(XT2OutputLazyBean order) throws BestXException {
        XT2Msg msg = order.getMsg();
        String mkt = msg.getString(LABEL_REG_MARKET); // Can be MOT or HIMTF
        LOGGER.info("Send FOK order to {} market [{}]", mkt, msg);
        sendRequest(msg);
    }

    @Override
    public void sendFasOrder(FasOrderOutputBean order, RegulatedMarket regulatedMarket) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSubscriptionMarketName() {
        return MTSMessageFields.VALUE_MTSPRIME_SUBSCRIPTION_MARKET_NAME;
    }

    @Override
    public String getRealMarketName() {
        return MTSMessageFields.VALUE_MTSPRIME_REAL_MARKET_NAME;
    }

    @Override
    public void notifyNullPrices(String bondVisionSessionId, String isin, String reason) {
        listener.onNullPrices(bondVisionSessionId, isin, ProposalSide.ASK);
        listener.onNullPrices(bondVisionSessionId, isin, ProposalSide.BID);
    }
}
