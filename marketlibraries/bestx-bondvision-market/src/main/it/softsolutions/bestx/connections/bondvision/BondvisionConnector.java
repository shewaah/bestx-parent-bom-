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

package it.softsolutions.bestx.connections.bondvision;

import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ACCRUED_VALUE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ACTION;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BOOK_QUOTE_ID;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BOOK_UPDATE_TIME;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BOOK_UPDATE_TIME_NSEC;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_CD;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_CONTRACTNO;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_CONTRACT_TIME;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_COUNTERPART_MEMBER;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ERROR_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ERROR_MESSAGE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_OFFER_PRICE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_OFFER_YIELD;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ORDERNUM;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_PRICE_ITEM_COUNT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_QUANTITY;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_QUOTE_ID;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_ID;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_PRICE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_SELL_SIDE_MEMBER_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_YIELD;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ROW_COLUMNS_COUNT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_SETT_DATE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_SIDE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_UPDATE_TIME;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_UPDATE_TIME_NSEC;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.RFQ_REPLY;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_FILL;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_RFCQ_BOOK;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_RFCQ_STATE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_ACTION_ACCEPT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_ACTION_CLOSE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_EXECUTION_REPORT_STATUS_ACTIVE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_ACCEPTED;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_CLOSED_BY_CLIENT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_CLOSED_BY_GOV;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_CLOSED_BY_SYSTEM;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_DELETED_BY_GOV;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_EXPIRED;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_PENDING;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_REJECTED_BY_ALL;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_RFQ_STATUS_REJECTED_BY_SYSTEM;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_SIDE_ASK;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_SIDE_BID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_PRICE_RESPONSE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.mts.MTSConnectionListener;
import it.softsolutions.bestx.connections.mts.MTSConnector;
import it.softsolutions.bestx.connections.mts.MTSMessageFields;
import it.softsolutions.bestx.connections.regulated.FasOrderOutputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.markets.bondvision.BondVisionMarket;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.FlyingRFQService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 13-nov-2012
 * 
 **/

public class BondvisionConnector extends MTSConnector implements BondVisionConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(BondvisionConnector.class);
    private FlyingRFQService flyingRFQService;
    
    private BondVisionConnectionListener getListener() {
    	return (BondVisionConnectionListener) listener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#onNotification(it.softsolutions.xt2.protocol.XT2Msg)
     */
    @Override
    public void onNotification(XT2Msg msg) {
        if (msg.getSubject().indexOf(SUBJECT_PRICE_RESPONSE) >= 0) {
            String bvSessionId = msg.getString(LABEL_REG_SESSION_ID);
            LOGGER.debug("Price info");
            String baseBVSessionId = bvSessionId.substring(2);

            String bidPrefix = "Bid.";
            String askPrefix = "Ask.";

            int bidPriceNumber = 0;
            int askPriceNumber = 0;
            try {
                bidPriceNumber = msg.getInt(bidPrefix + LABEL_PRICE_ITEM_COUNT);
                askPriceNumber = msg.getInt(askPrefix + LABEL_PRICE_ITEM_COUNT);
            } catch (Exception e) {
                LOGGER.error("Error while retrieving data from price notification [" + msg.toString() + "]" + " : " + e.toString(), e);
                return;
            }
            ArrayList<BondVisionProposalInputLazyBean> proposals = new ArrayList<BondVisionProposalInputLazyBean>();

            for (int rowIdx = 1; rowIdx <= bidPriceNumber; rowIdx++) {
                String fieldName = bidPrefix + LABEL_ROW_COLUMNS_COUNT + "." + rowIdx;
                try {
                    int columns = msg.getInt(fieldName);
                    for (int colIdx = 1; colIdx <= columns; colIdx++) {
                        proposals.add(new BondVisionProposalInputLazyBean(msg, rowIdx, colIdx, ProposalSide.BID));
                    }
                } catch (Exception e) {
                    LOGGER.error("Missing field " + fieldName + " in BondVision price response " + msg.toString() + ", cannot process prices.");
                    return;
                }
            }

            for (int rowIdx = 1; rowIdx <= askPriceNumber; rowIdx++) {
                String fieldName = askPrefix + LABEL_ROW_COLUMNS_COUNT + "." + rowIdx;
                try {
                    int columns = msg.getInt(fieldName);
                    for (int colIdx = 1; colIdx <= columns; colIdx++) {
                        proposals.add(new BondVisionProposalInputLazyBean(msg, rowIdx, colIdx, ProposalSide.ASK));
                    }
                } catch (Exception e) {
                    LOGGER.error("Missing field " + fieldName + " in BondVision price response " + msg.toString() + ", cannot process prices.");
                    return;
                }
            }

            getListener().onInstrumentPrices(baseBVSessionId, proposals);
            return;
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onNotification(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#onPublish(it.softsolutions.xt2.protocol.XT2Msg)
     */
    @Override
    public void onPublish(XT2Msg msg) {
        if (discardIdle(msg)) {
            return;
        }
        String messageType = getMessageTypeFromSubject(msg.getSubject());

        if (messageType.equalsIgnoreCase(SUBJECT_RFCQ_BOOK)) {
            try {
                String sessionId = msg.getString(LABEL_CD);
                String marketMaker = msg.getString(LABEL_RFCQ_SELL_SIDE_MEMBER_CODE);
                String quoteId = Long.toString(msg.getLong(LABEL_BOOK_QUOTE_ID));
                String rfqSeqNo = Long.toString(msg.getLong(LABEL_RFCQ_ID));
                BigDecimal price = new BigDecimal(msg.getDouble(LABEL_RFCQ_PRICE)).setScale(priceScale, BigDecimal.ROUND_HALF_UP);
                BigDecimal yield = new BigDecimal(msg.getDouble(LABEL_RFCQ_YIELD)).setScale(yieldScale, BigDecimal.ROUND_HALF_UP);
                BigDecimal qty = new BigDecimal(msg.getDouble(LABEL_QUANTITY), tickMc);

                ProposalSide side = null;
                // invert side, as side is the one seen as buy side
                int intSide = msg.getInt(LABEL_SIDE);
                if (intSide == VALUE_SIDE_ASK) {
                    side = ProposalSide.BID;
                } else if (intSide == VALUE_SIDE_BID) {
                    side = ProposalSide.ASK;
                } else {
                    throw new BestXException("Invalid side received: " + intSide);
                }

                Date futSettDate = DateService.parse(DateService.dateISO, Long.toString(msg.getLong(LABEL_SETT_DATE)));
                long time = msg.getLong(LABEL_UPDATE_TIME);
                String updateTimeStr = Long.toString(time);
                if (updateTimeStr.length() < 9) {
                    updateTimeStr = "0" + updateTimeStr;
                }
                String today = DateService.format(DateService.dateISO, DateService.newLocalDate());
                Date updateTime = DateService.parse(DateService.dateTimeISO, today + updateTimeStr);
                String updateTimeNSec = msg.getString(LABEL_UPDATE_TIME_NSEC);

                ((MTSConnectionListener) listener).onQuote(sessionId, rfqSeqNo, quoteId, marketMaker, price, yield, qty, side, futSettDate, updateTime, updateTimeNSec);

            } catch (Exception e) {
                LOGGER.error("Could not parse message of type" + SUBJECT_RFCQ_BOOK + "due to exception: " + e.getMessage() + ", msg was: " + msg);
            }
        } else if (messageType.equalsIgnoreCase(SUBJECT_RFCQ_STATE)) // Lasciare dopo altri RFCQ!!
        {
            String isin = msg.getString("InstrumentCode");
            try {
                String sessionId = msg.getString(LABEL_CD);
                int status = msg.getInt(LABEL_STATUS);
                if (status == VALUE_RFQ_STATUS_ACCEPTED) {
                    String trader = msg.getString("EndUser");
                    LOGGER.info("Bondvision RFQ with session id: " + sessionId + " has been accepted by BV. Trader is: " + trader);

                } else if (status == VALUE_RFQ_STATUS_PENDING) {
                    // Do nothing
                    String trader = msg.getString("EndUser");
                    LOGGER.info("Bondvision RFQ with session id: " + sessionId + " is currently pending on BV. Trader is: " + trader);
                    flyingRFQService.put(isin, sessionId);
                } else if (status == VALUE_RFQ_STATUS_REJECTED_BY_ALL) {
                    String reason = new String("Bondvision RFQ with session id: " + sessionId + " Expired");
                    listener.onOrderReject(sessionId, reason);
                } else if (status == VALUE_RFQ_STATUS_CLOSED_BY_CLIENT) {
                    String reason = new String("Bondvision RFQ with session id: " + sessionId + " Rejected");
                    listener.onOrderReject(sessionId, reason);
                } else if (status == VALUE_RFQ_STATUS_EXPIRED || status == VALUE_RFQ_STATUS_REJECTED_BY_SYSTEM) {
                    String reason = new String("Bondvision RFQ with session id: " + sessionId + " Expired");
                    listener.onOrderReject(sessionId, reason);
                } else if (status == VALUE_RFQ_STATUS_CLOSED_BY_SYSTEM) {
                    String reason = new String("Bondvision RFQ with session id: " + sessionId + " closed by system");
                    listener.onOrderReject(sessionId, reason);

                } else if (status == VALUE_RFQ_STATUS_CLOSED_BY_GOV || status == VALUE_RFQ_STATUS_DELETED_BY_GOV) {
                    String reason = new String("Bondvision RFQ with session id: " + sessionId + " Deleted by Governance");
                    listener.onOrderReject(sessionId, reason);

                } else {
                    String reason = msg.getString(LABEL_ERROR_MESSAGE);
                    if (reason == null) {
                        reason = new String("Bondvision RFQ with session id: " + sessionId + " closed by BestX!");
                    }

                    listener.onOrderReject(sessionId, reason);
                }
            } catch (Exception e) {
                LOGGER.info("Could not parse message of type" + SUBJECT_RFCQ_STATE + "due to exception: " + e.getMessage() + ", msg was: " + msg);
            } finally {
                // Remove Isin from flying rfq
                flyingRFQService.remove(isin);
            }
        } else if (messageType.equalsIgnoreCase(SUBJECT_FILL)) {
            String rfqId = null;
            try {
                rfqId = Long.toString(msg.getLong("RfcqReqId")); // Il server associa l'Id della RFQ al fill
            } catch (Exception e) {
                LOGGER.info("BV Execution Report with null RFQ ID [{}]", msg);
                return;
            }
            long status = 0;
            try {
                status = msg.getInt(LABEL_STATUS);
            } catch (Exception e) {
                LOGGER.error("BV Execution Report with null status [{}]", msg, e);
                return;
            }
            if (status == VALUE_EXECUTION_REPORT_STATUS_ACTIVE) {
                try {
                    BigDecimal lastPrice = new BigDecimal(msg.getDouble(LABEL_PRICE));
                    String contractNo = Long.toString(msg.getLong(LABEL_CONTRACTNO));
                    String marketOrderId = Long.toString(msg.getLong(LABEL_ORDERNUM));
                    String counterpart = msg.getString(LABEL_COUNTERPART_MEMBER);
                    BigDecimal accruedValue = new BigDecimal(msg.getDouble(LABEL_ACCRUED_VALUE));
                    accruedValue = accruedValue.setScale(2, RoundingMode.HALF_DOWN);

                    Date marketTime = DateService.newLocalDate();
                    if (msg.getLong(LABEL_CONTRACT_TIME) > 0) {
                        // MSA fix: time like 12456578008 is OK but time like 83678007 must be filled with 0
                        DecimalFormat timeNumFormatter = new DecimalFormat("000000000");
                        String strTime = timeNumFormatter.format(msg.getLong(LABEL_CONTRACT_TIME));
                        String today = DateService.format(DateService.dateISO, DateService.newLocalDate());
                        marketTime = DateService.parse(DateService.dateTimeISO, today + strTime);
                    }

                    if (this.listener == null) {
                        LOGGER.error("No listener available!");
                    } else {
                        ((MTSConnectionListener) listener).onExecutionReport(rfqId, ExecutionReportState.FILLED, marketOrderId, counterpart, lastPrice, contractNo, accruedValue, marketTime);
                    }
                } catch (Exception e) {
                    LOGGER.error("BV Execution Report with invalid or null info [{}]", msg, e);
                }
            } else {
                LOGGER.info("BV Execution Report with status not Active [{}], ignoring it", status);
            }
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onPublish(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#onReply(it.softsolutions.xt2.protocol.XT2Msg)
     */
    @Override
    public void onReply(XT2Msg msg) {
        int err = 0;
        String message = "";

        if (msg.getName().indexOf("XT2RfqReplyResp") >= 0) {
            try {
                err = msg.getInt(LABEL_ERROR_CODE);
                if (err != 0) {
                    message = msg.getString(LABEL_ERROR_MESSAGE);
                    LOGGER.info("RFQ accept request had an error given by the market: " + err + " - " + message);
                    String SessionId = msg.getString(LABEL_CD);
                    listener.onOrderReject(SessionId, message);
                } else {
                    LOGGER.info("Received positive reply to RFQ Accept/Refuse request.");
                }
            } catch (Exception e) {
                LOGGER.info("Unable to read reply to send RFQ to BV due to exception: " + e.getMessage());
            }
        } else {
            super.onReply(msg);

        }
    }


    protected XT2Msg createRfqReplyMsg(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr) throws Exception {
        XT2Msg msg = new XT2Msg(RFQ_REPLY);

        long rfqSeqNo, quoteId;
        quoteId = Long.parseLong(bvQuoteId.substring(0, bvQuoteId.indexOf('|')));
        rfqSeqNo = Long.parseLong(bvQuoteId.substring(bvQuoteId.indexOf('|') + 1));
        msg.setValue(LABEL_RFCQ_ID, rfqSeqNo);
        msg.setValue(LABEL_QUOTE_ID, quoteId);
        msg.setValue("Market", BondVisionMarket.REAL_MARKET_NAME);
        msg.setValue(LABEL_OFFER_PRICE, price.doubleValue());
        if (yield != null) {
            msg.setValue(LABEL_OFFER_YIELD, yield.doubleValue());
        }
        long time = DateService.formatAsLong(DateService.timeISO, proposalTime);
        msg.setValue(LABEL_BOOK_UPDATE_TIME, time);
        msg.setValue(LABEL_BOOK_UPDATE_TIME_NSEC, (nsecTimeStr == null ? "" : nsecTimeStr));
        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#acceptQuote(java.lang.String, java.lang.String, java.math.BigDecimal,
     * java.math.BigDecimal, java.util.Date, java.lang.String, it.softsolutions.bestx.model.Instrument)
     */
    @Override
	public void acceptQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException {
        XT2Msg msg = null;
        try {
            msg = this.createRfqReplyMsg(bvSessionId, bvQuoteId, price, yield, proposalTime, nsecTimeStr);
            msg.setValue(LABEL_ACTION, VALUE_ACTION_ACCEPT);
        } catch (Exception e) {
            LOGGER.error("Unable to send accept to quote on Bondvision market [{}]", msg, e);
        }
        LOGGER.info("Sending a Quote Accept message to Bondvision server. {}", msg);
        try {
            this.sendRequest(msg);
        } catch (BestXException e) {
            LOGGER.error("Unable to send request to Bondvision server [{}]", msg, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#rejectQuote(java.lang.String, java.lang.String, java.math.BigDecimal,
     * java.math.BigDecimal, java.util.Date, java.lang.String, it.softsolutions.bestx.model.Instrument)
     */
    @Override
	public void rejectQuote(String bvSessionId, String bvQuoteId, BigDecimal price, BigDecimal yield, Date proposalTime, String nsecTimeStr, Instrument instrument) throws BestXException {
        XT2Msg msg = null;
        try {
            msg = this.createRfqReplyMsg(bvSessionId, bvQuoteId, price, yield, proposalTime, nsecTimeStr);
            msg.setValue(LABEL_ACTION, VALUE_ACTION_CLOSE);
        } catch (Exception e) {
            LOGGER.error("Unable to send reject to quote on Bondvision market [{}]", msg, e);
        }
        LOGGER.info("Sending a Quote Reject message to Bondvision server. {}", msg);
        try {
            this.sendRequest(msg);
        } catch (BestXException e) {
            LOGGER.error("Unable to send request to Bondvision server [{}]", msg, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#sendRfq(java.lang.String,
     * it.softsolutions.bestx.connections.mts.bondvision.BondVisionRFQOutputLazyBean)
     */
    @Override
	public void sendRfq(String bvSessionId, BondVisionRFCQOutputLazyBean rfq) throws BestXException {
        XT2Msg msg = rfq.getMsg();
        msg.setValue("CD", bvSessionId);

        LOGGER.info("Sending a Rfq request to Bondvision server. {}", msg);

        sendRequest(msg);
    }

    /**
     * Gets the flying rfq service.
     * 
     * @return the flying rfq service
     */
    public FlyingRFQService getFlyingRFQService() {
        return flyingRFQService;
    }

    /**
     * Sets the flying rfq service.
     * 
     * @param flyingRFQService
     *            the new flying rfq service
     */
    public void setFlyingRFQService(FlyingRFQService flyingRFQService) {
        this.flyingRFQService = flyingRFQService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * it.softsolutions.bestx.connections.regulated.RegulatedConnection#setRegulatedConnectionListener(it.softsolutions.bestx.connections
     * .regulated.RegulatedConnectionListener)
     */
    @Override
    public void setRegulatedConnectionListener(RegulatedConnectionListener listener) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * it.softsolutions.bestx.connections.regulated.RegulatedConnection#sendFokOrder(it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean
     * )
     */
    @Override
    public void sendFokOrder(XT2OutputLazyBean order) throws BestXException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.regulated.RegulatedConnection#sendFasOrder(it.softsolutions.bestx.connections.regulated.
     * FasOrderOutputBean, it.softsolutions.bestx.markets.regulated.RegulatedMarket)
     */
    @Override
    public void sendFasOrder(FasOrderOutputBean order, RegulatedMarket regulatedMarket) throws BestXException {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#getSubscriptionMarketName()
     */
    @Override
    public String getSubscriptionMarketName() {
        return MTSMessageFields.VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#getRealMarketName()
     */
    @Override
    public String getRealMarketName() {
        return MTSMessageFields.VALUE_BONDVISION_REAL_MARKET_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnector#notifyNullPrices(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void notifyNullPrices(String bondVisionSessionId, String isin, String reason) {
        listener.onNullPrices(bondVisionSessionId, isin, reason);
    }

	@Override
	public void sendInventoryOrder(String bvSessionId, BondVisionInventoryOrderOutputLazyBean order) throws BestXException {
		
	}

}
