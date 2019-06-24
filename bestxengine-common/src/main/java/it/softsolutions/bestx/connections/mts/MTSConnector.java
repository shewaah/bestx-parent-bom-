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

import static it.softsolutions.bestx.connections.mts.MTSMessageFields.BANK_SWITCH_REQ;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.BANK_SWITCH_RESP;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.BVS_PRICE_DISC_REQ_RESULT_MSG;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.CONTRIBUTION_SWITCH_REQ;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.CONTRIBUTION_SWITCH_RESP;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BANK_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BOND_TYPE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BUY_SIDE_MEMBER_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_BUY_SIDE_SUBSTATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_CD;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_CONN_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ENABLED;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ERROR_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ERROR_MESSAGE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_EVENT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_INSTRUMENT_NEGOTIABLE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_ISIN;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_MARKET_AFFILIATION;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_MARKET_AFFILIATIONSTR;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_MARKET_PHASE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_MARKET_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_MEMBER_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_QUOTE_INDICATOR;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_QUOTE_INDICATORSTR;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_LOT_SIZE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_MIN_QTY;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_RFCQ_QTY_TICK;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_SELL_SIDE_MEMBER_CODE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_SELL_SIDE_SUBSTATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_SETT_DATE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.LABEL_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.RFQ_RESP;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_FILL;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_INSTRUMENT;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_MARKET_STATUS;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_MEMBER_ENABLED;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_ORDER;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_RFCQ_BOOK;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_RFCQ_STATE;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_TRADING_RELATION;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.SUBJECT_TRADING_RELATION_EXCEPTION;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_CONN_STATUS_ON;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.VALUE_PHASE_STATUS_OPEN;
import static it.softsolutions.bestx.connections.mts.MTSMessageFields.XT2_MSG_SUBJECT_SEPARATOR;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_ISIN;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBMARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBSCRIPTION;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_REG_PRICEREQ_PDU;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.regulated.CancelRequestOutputBean;
import it.softsolutions.bestx.connections.regulated.RegulatedConnection;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener.StatusField;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.connections.xt2.XT2MessageFields;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * Purpose: this class is the base for all MTS markets
 * 
 * Project Name : bestxengine-common First created by: stefano.pontillo Creation date: 06/giu/2012
 * 
 **/
public abstract class MTSConnector extends XT2BaseConnector implements MTSConnection, RegulatedConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MTSConnector.class);
    public static int BV_TECH_REJ_ERR_CODE;
    public static String BV_TECH_REJ_DISTINCTIVE_MSG;
    private String xt2UserName;
    private int marketStatus = -1;
    private int userStatus = -1;
    private int connStatus = -1;

    private int phase = -1;

    private int bankStatus = -1;
    protected String memberCode;

    protected static MathContext lotMc = new MathContext(15, RoundingMode.HALF_DOWN);
    protected static MathContext tickMc = new MathContext(8, RoundingMode.HALF_DOWN);
    protected int yieldScale = 4;
    protected int priceScale = 5;
    protected int idleReceived = 0;
    protected int idleExpected = 0;

    private int technicalRejErrCode;
    protected MTSConnectionListener listener;

    /**
     * Sets the username for the XT2
     * 
     * @param xt2UserName
     */
    public void setXt2UserName(String xt2UserName) {
        this.xt2UserName = xt2UserName;
    }

    @Override
    public void onNotification(XT2Msg msg) {
        LOGGER.info("Notification message from " + getConnectionName() + " market [" + msg.toString() + "] - subject: " + msg.getSubject() + " - name: " + msg.getName());
        int errorCode = 0;
        if (msg.toString().indexOf(MTSMessageFields.LABEL_ERROR_CODE) >= 0) {
            try {
                errorCode = msg.getInt(LABEL_ERROR_CODE);

                if (errorCode != 0 && msg.getSubject().indexOf(XT2MessageFields.SUBJECT_SERVICE_STARTED) >= 0) {
                    LOGGER.error("Notify with error code: " + errorCode + " [" + msg.toString() + "]");
                    if (this.listener == null) {
                        LOGGER.error("No listener available!");
                    } else {
                        this.listener.onConnectionError();
                    }
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("Error while retrieving error code from " + getConnectionName() + " notify [" + msg.toString() + "]" + " : " + e.toString(), e);
                return;
            }
        }
        if (msg.getName().equalsIgnoreCase(XT2MessageFields.SUBJECT_SERVICE_STARTED)) {
            if (errorCode == 0 && getRealMarketName().equalsIgnoreCase(msg.getValue("Market").toString())) {
                LOGGER.info("" + getConnectionName() + " Connection Login succeded");

                try {
                    idleExpected = 8; // number of subscriptions
                    idleReceived = 0;
                    connection.subscribe("/" + getSubscriptionMarketName() + "/MARKET_STATUS");

                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_TRADING_RELATION + "/" + this.memberCode + "/*");
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_TRADING_RELATION_EXCEPTION + "/*");
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_MEMBER_ENABLED + "/*");
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_RFCQ_BOOK + "/*");
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_RFCQ_STATE + "/*");
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_FILL + "/*");
                    
                    boolean bestxRestartProperty = System.getProperty(MarketConnection.BESTX_RESTART) != null;
                    if (!bestxRestartProperty) {
                        connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_INSTRUMENT + "/*");
                    } else {
                        LOGGER.info("Bestx restart : skipping security definitions subscription");
                    }
                    connection.subscribe("/" + getSubscriptionMarketName() + "/" + SUBJECT_ORDER + "/*");

                    // inutile sulla buy side
                    // connection.subscribe("/"+marketName+"/USER_STATUS/" + xt2UserName);
                    // connection.subscribe("/"+marketName+"/EXECUTIONREPORT/*");
                } catch (Exception e) {
                    LOGGER.error("An error occurred while trying to subscribe to " + getConnectionName() + " subjects" + " : " + e.toString(), e);
                }
            } else {
                LOGGER.info("" + getConnectionName() + " Connection Login not succeded");
                if (this.listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    this.listener.onConnectionError();
                }
            }
            return;
        } else if (msg.getName().equalsIgnoreCase(XT2MessageFields.SUBJECT_SERVICE_STOPPED)) {
            LOGGER.info("" + getConnectionName() + " Connection Logoff succeded");
            return;
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onNotification(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.xt2.XT2BaseConnector#onPublish(it.softsolutions.xt2.protocol.XT2Msg)
     */
    @Override
    public void onPublish(XT2Msg msg) {
        String messageType = getMessageTypeFromSubject(msg.getSubject());

        // if(discardIdle(msg))
        // return;
        int newMarketStatus = marketStatus;
        int newConnStatus = connStatus;
        int newBankStatus = bankStatus;
        int newPhase = phase;
        LOGGER.info("Publish message from " + getConnectionName() + " market [" + msg.toString() + "]");

        // Market Status
        if (messageType.equalsIgnoreCase(SUBJECT_MARKET_STATUS)) {
            if (msg.toString().indexOf(LABEL_MARKET_STATUS) >= 0) {
                try {
                    newMarketStatus = msg.getInt(LABEL_MARKET_STATUS);
                    newConnStatus = msg.getInt(LABEL_CONN_STATUS);
                    newPhase = msg.getInt(LABEL_MARKET_PHASE);
                    LOGGER.debug("Market status received: " + newMarketStatus);
                    newBankStatus = msg.getInt(LABEL_BANK_STATUS);
                    LOGGER.debug("Member status received: " + newBankStatus);
                } catch (Exception e) {
                    LOGGER.error("An error occurred while trying to read market status from " + getConnectionName() + " message: " + msg.toString(), e);
                }
                if (marketStatus != newMarketStatus || bankStatus != newBankStatus || phase != newPhase || connStatus != newConnStatus) {
                    marketStatus = newMarketStatus;
                    phase = newPhase;
                    bankStatus = newBankStatus;
                    connStatus = newConnStatus;
                    if (listener == null) {
                        LOGGER.error("No listener available!");
                    } else {
                        listener.onConnectionStatus(null, this.connStatus == VALUE_CONN_STATUS_ON && this.phase == VALUE_PHASE_STATUS_OPEN, StatusField.MARKET_STATUS);
                    }
                }
            }

        }
        // Sell Side Member Enabled
        else if (messageType.equalsIgnoreCase(SUBJECT_MEMBER_ENABLED)) {
            try {
                String member = msg.getString(LABEL_MEMBER_CODE);
                String bondType = msg.getString(LABEL_BOND_TYPE);
                int enabled = msg.getInt(LABEL_ENABLED);
                ((MTSConnectionListener) listener).onMemberEnabled(member, bondType, enabled == 1);
            } catch (Exception e) {
                LOGGER.error("An error occurred while trying to read Member Enabled Message on " + getConnectionName() + ": " + msg.toString() + " : " + e.toString(), e);
            }
        } else if (messageType.equalsIgnoreCase(SUBJECT_TRADING_RELATION_EXCEPTION)) // LET IT BEFORE TRADING_RELATION !!!!
        {
            try {
                String bsmember = msg.getString(LABEL_BUY_SIDE_MEMBER_CODE);
                if (this.memberCode.indexOf(bsmember) < 0)
                    return;
                String ssmember = msg.getString(LABEL_SELL_SIDE_MEMBER_CODE);
                String bondType = msg.getString(LABEL_BOND_TYPE);
                int status = msg.getInt(LABEL_STATUS);
                int event = msg.getInt(LABEL_EVENT);
                ((MTSConnectionListener) this.listener).onTradingRelationException(ssmember, bondType, TradingRelationExceptionStatus.getTradingRelationExceptionStatus(status),
                                TradingRelationExceptionEvent.getTradingRelationExceptionEvent(event));
            } catch (Exception e) {
                LOGGER.info("Could not parse message of type" + SUBJECT_TRADING_RELATION_EXCEPTION + "due to exception: " + e.getMessage() + ", msg was: " + msg);
            }
        } else if (messageType.equalsIgnoreCase(SUBJECT_TRADING_RELATION)) {
            try {
                String member = msg.getString(LABEL_SELL_SIDE_MEMBER_CODE);
                int event = msg.getInt(LABEL_EVENT);
                int status = msg.getInt(LABEL_STATUS);
                int sellSideSubStatus = msg.getInt(LABEL_SELL_SIDE_SUBSTATUS);
                int buySideSubStatus = msg.getInt(LABEL_BUY_SIDE_SUBSTATUS);

                ((MTSConnectionListener) this.listener).onTradingRelation(member, TradingRelationStatus.getTradingRelationStatus(status),
                                TradingRelationStatus.getTradingRelationStatus(sellSideSubStatus), TradingRelationStatus.getTradingRelationStatus(buySideSubStatus),
                                TradingRelationEvent.getTradingEvent(event));

            } catch (Exception e) {
                LOGGER.info("Could not parse message of type" + SUBJECT_TRADING_RELATION + "due to exception: " + e.getMessage() + ", msg was: " + msg);
            }
        }
        // Instrument
        else if (messageType.equalsIgnoreCase(SUBJECT_INSTRUMENT)) {
            int negotiable = 0;
            if (msg.getValue(LABEL_INSTRUMENT_NEGOTIABLE) != null) {
                try {
                    negotiable = msg.getInt(LABEL_INSTRUMENT_NEGOTIABLE);
                } catch (Exception e) {
                    LOGGER.error("Errore retrieving field [{}] from message {}: {}", LABEL_INSTRUMENT_NEGOTIABLE, msg, e.getMessage());
                }
            } else {
                LOGGER.error("Field [{}] not found from message {}", LABEL_INSTRUMENT_NEGOTIABLE, msg);
            }

            if (negotiable == 0) {
                return;
            }

            try {
                String isin = msg.getString(LABEL_ISIN);
                BigDecimal minSize = new BigDecimal(msg.getDouble(LABEL_RFCQ_MIN_QTY), tickMc);
                BigDecimal qtyTick = new BigDecimal(msg.getDouble(LABEL_RFCQ_QTY_TICK), tickMc);
                BigDecimal lotSize = new BigDecimal(msg.getDouble(LABEL_RFCQ_LOT_SIZE), lotMc);

                int marketAffiliation = msg.getInt(LABEL_MARKET_AFFILIATION);
                String marketAffiliationStr = msg.getString(LABEL_MARKET_AFFILIATIONSTR);

                int quoteIndicator = msg.getInt(LABEL_QUOTE_INDICATOR);
                String quoteIndicatorStr = msg.getString(LABEL_QUOTE_INDICATORSTR);

                String str = msg.getValue(LABEL_SETT_DATE).toString();
                Date settlDate = null;
                if (str != null) {
                    settlDate = DateService.parse(str, DateService.dateISO);
                } else {
                    LOGGER.error("Null settlement date for Instrument: " + isin);
                }
                String bondType = msg.getString(LABEL_BOND_TYPE);

                ((MTSConnectionListener) listener).onSecurityDefinition(isin, null, settlDate, minSize, qtyTick, lotSize, bondType, marketAffiliation, marketAffiliationStr, quoteIndicator,
                                quoteIndicatorStr);
            } catch (Exception e) {
                LOGGER.warn("Exception: " + e.getMessage() + " while parsing message from market: " + msg);
            }
        } else {
            LOGGER.debug("Forward message to superclass");
            super.onPublish(msg);
        }
    }

    /**
     * 
     * 
     * @param msg
     *            XT2Message coming from market
     * @return true if idle is arrived
     */
    protected boolean discardIdle(XT2Msg msg) {
        String idle = msg.getString("$IBType_____");
        if ("SUB_IDLE".equalsIgnoreCase(idle)) {
            if (++idleReceived >= idleExpected) {
                LOGGER.info("idle for all class arrived: " + getConnectionName() + " connection is ready");
                ((MTSConnectionListener) listener).onDownloadEnd(this);
            }
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.xt2.XT2BaseConnector#onReply(it.softsolutions.xt2.protocol.XT2Msg)
     */
    @Override
    public void onReply(XT2Msg msg) {
        int err = 0;
        String message = "";
        // manage reply to UserContributionSwitch
        if (msg.getName().indexOf(CONTRIBUTION_SWITCH_RESP) >= 0 || msg.getName().indexOf(CONTRIBUTION_SWITCH_REQ) >= 0) {
            try {
                err = msg.getInt(LABEL_ERROR_CODE);
                if (err != 0) {
                    message = msg.getString(LABEL_ERROR_MESSAGE);
                    LOGGER.info("Unable to change User Status request due to exception: " + message);
                } else {
                    LOGGER.info("Received positive reply to User Status Change request");
                }
            } catch (Exception e) {
                LOGGER.info("Unable to read reply to change User Status request due to exception: " + e.getMessage());
            }
        }
        // manage reply to BankContributionSwitch
        else if (msg.getName().indexOf(BANK_SWITCH_REQ) >= 0 || msg.getName().indexOf(BANK_SWITCH_RESP) >= 0) {
            try {
                err = msg.getInt(LABEL_ERROR_CODE);
                if (err != 0) {
                    message = msg.getString(LABEL_ERROR_MESSAGE);
                    LOGGER.info("Unable to change Member Status request due to exception: " + message);
                } else {
                    LOGGER.info("Received positive reply to Member Status Change request");
                }
            } catch (Exception e) {
                LOGGER.info("Unable to read reply to change Member Status request due to exception: " + e.getMessage());
            }

        } else if (msg.getName().indexOf(RFQ_RESP) >= 0 || msg.getName().indexOf(BANK_SWITCH_RESP) >= 0) {
            try {
                err = msg.getInt(LABEL_ERROR_CODE);
                if (err != 0) {
                    message = msg.getString(LABEL_ERROR_MESSAGE);
                    LOGGER.info("RFQ request had an error given by the market: " + message);
                    String SessionId = msg.getString(LABEL_CD);
                    // we need a mean for further processing to understand if this is a tech reject
                    // for the error "Transaction failed Maximum number of proposal exceeded".
                    // Its error code is written in the spring config (should be 38) and the
                    // event handler will check the message for the phrase in the static variable
                    // BV_TECH_REJ_DISTINCTIVE_MSG.
                    if (err == BV_TECH_REJ_ERR_CODE)
                        message = BV_TECH_REJ_DISTINCTIVE_MSG + " " + message;
                    listener.onOrderTechnicalReject(SessionId, message);
                } else {
                    LOGGER.info("Received positive reply to RFQ");
                }
            } catch (Exception e) {
                LOGGER.info("Unable to read reply to send RFQ to " + getConnectionName() + " due to exception: " + e.getMessage());
            }

        } else if (msg.getName().indexOf(BVS_PRICE_DISC_REQ_RESULT_MSG) >= 0) {
            String bondVisionSessionId = msg.getString(LABEL_REG_SESSION_ID);
            LOGGER.debug("Price info");
            // String baseBVSessionId = bondVisionSessionId.substring(2);

            err = 0;
            try {
                err = msg.getInt(LABEL_ERROR_CODE);
            } catch (Exception ex) {
                LOGGER.error("Error while retrieving data from price notification [" + msg.toString() + "]" + " : " + ex.toString(), ex);
                return;
            }
            if (err != 0) {
                LOGGER.info("Negative reply result received from module: " + serviceName + " - msg: " + msg);

                String isin = null;
                try {
                    isin = msg.getString(LABEL_ISIN);
                } catch (Exception ex) {
                    LOGGER.error("Error while retrieving isin from price notification [" + msg.toString() + "]" + " : " + ex.toString(), ex);
                    return;
                }
                String reason = msg.getString(LABEL_ERROR_MESSAGE);
                notifyNullPrices(bondVisionSessionId, isin, reason);
            } else {
                LOGGER.info("Price request without errors for the BVS session " + bondVisionSessionId);
            }
        }
        // manage reply to other BV specific messages
        // manage default
        else {
            super.onReply(msg);

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.xt2.XT2BaseConnector#onDisconnection(java.lang.String, java.lang.String)
     */
    @Override
    public void onDisconnection(String serviceName, String userName) {
        super.onDisconnection(serviceName, userName);
        this.userStatus = -1;
        this.marketStatus = -1;
        this.bankStatus = -1;
        this.phase = -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#requestInstrumentPriceSnapshot(java.lang.String, it.softsolutions.bestx.model.Instrument, java.lang.String, java.lang.String)
     */
    @Override
    public void requestInstrumentPriceSnapshot(String bvSessionId, Instrument instrument, String subMarket, String market) throws BestXException {
        XT2Msg priceReq = new XT2Msg(SUBJECT_REG_PRICEREQ_PDU);
        priceReq.setValue(LABEL_REG_ISIN, instrument.getIsin());
        priceReq.setValue(LABEL_REG_MARKET, market);
        priceReq.setValue(LABEL_REG_SUBMARKET, subMarket);
        priceReq.setValue(LABEL_REG_SUBSCRIPTION, 0); // SNAPSHOT
        priceReq.setValue(LABEL_REG_SESSION_ID, bvSessionId);

        LOGGER.info("Price request towards " + market + " market [REQUEST=" + priceReq.toString() + "]");
        sendRequest(priceReq);
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#revokeOrder(it.softsolutions.bestx.connections.regulated.CancelRequestOutputBean)
     */
    @Override
    public void revokeOrder(CancelRequestOutputBean order) throws BestXException {
        throw new UnsupportedOperationException();
    }

    /*
     * Attualmente non usata e non testata
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#sendMemberTradeOnRequest()
     */
    public void sendMemberTradeOnRequest() {
        XT2Msg msg = new XT2Msg(BANK_SWITCH_REQ);
        msg.setValue("Market", VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME + "AA");
        msg.setValue("ContribEnabled", 1);
        LOGGER.info("Sending a Bank switch message to " + getConnectionName() + " server.");
        try {
            this.sendRequest(msg);
        } catch (BestXException e) {
            LOGGER.info("Unable to send request to " + getConnectionName() + " Server, due to exception: " + e.getMessage());
        }
    }

    /*
     * Attualmente non usata e non testata
     * 
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#sendUserTradeOnRequest()
     */
    public void sendUserTradeOnRequest() {
        XT2Msg msg = new XT2Msg(CONTRIBUTION_SWITCH_REQ);
        msg.setValue("Market", VALUE_BONDVISION_SUBSCRIPTION_MARKET_NAME);
        msg.setValue("ContribEnabled", 1);
        msg.setValue("ProposalEnabled", 1);
        msg.setValue("OrderEnabled", 1);
        msg.setValue("ForceContribution", 1);
        LOGGER.info("Sending a Contribution switch message to " + getConnectionName() + " server." + msg);
        try {
            this.sendRequest(msg);
        } catch (BestXException e) {
            LOGGER.info("Unable to send request to " + getConnectionName() + " Server, due to exception: " + e.getMessage());
        }
    }

    /**
     * Return the member code of the market connection
     * 
     * @return the member code
     */
    public String getMemberCode() {
        return this.memberCode;
    }

    /**
     * Sets the member code for the market connection
     * 
     * @param memberCode
     */
    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    /**
     * Return the scale of theyield
     * 
     * @return the scale of the yield
     */
    public int getYieldScale() {
        return this.yieldScale;
    }

    /**
     * Sets the scale for the yield
     * 
     * @param yieldScale
     *            the scale for the yield
     */
    public void setYieldScale(int yieldScale) {
        this.yieldScale = yieldScale;
    }

    /**
     * Return the error code for a technical reject
     * 
     * @return int the error code for a technical reject
     */
    public synchronized int getTechnicalRejErrCode() {
        return technicalRejErrCode;
    }

    /**
     * Setting the static variable to the configuration value
     * 
     * @param technicalRejErrCode
     */
    public synchronized void setTechnicalRejErrCode(int technicalRejErrCode) {
        this.technicalRejErrCode = technicalRejErrCode;
        BV_TECH_REJ_ERR_CODE = technicalRejErrCode;
        BV_TECH_REJ_DISTINCTIVE_MSG = "Error code " + BV_TECH_REJ_ERR_CODE + ".";
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.connections.mts.MTSConnection#setMTSConnectionListener(it.softsolutions.bestx.connections.mts.MTSConnectionListener)
     */
    @Override
    public void setMTSConnectionListener(MTSConnectionListener listener) {
        this.listener = listener;
    }

    /**
     * Extract the message type from the messsagesubject
     * 
     * @param subject
     *            the subject of the XT2 message
     * @return the type of the XT2 message
     */
    protected String getMessageTypeFromSubject(String subject) {
        String[] subjectSplit = subject.split(XT2_MSG_SUBJECT_SEPARATOR);
        if (subjectSplit != null && subjectSplit.length >= 3) {
            return subjectSplit[2];
        } else {
            LOGGER.warn("Cannot find message type in the message subject " + subject + ". Returning an empty message type.");
            return "";
        }
    }

    /**
     * Return the market name of this subscription
     * 
     * @return the market name
     */
    public abstract String getSubscriptionMarketName();

    /**
     * Return the real market name of this subscription
     * 
     * @return the real market name
     */
    public abstract String getRealMarketName();

    /**
     * Notify that no prices are received from the market every MTS markets may act differently receiving an error thus no prices
     * 
     * @param bondVisionSessionId
     *            market session id
     * @param isin
     *            ISIN code of the instrument to notify
     * @param reason
     *            Description of the reason thus no prices
     */
    public abstract void notifyNullPrices(String bondVisionSessionId, String isin, String reason);
}
