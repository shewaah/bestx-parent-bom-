/**
 * 
 */
package it.softsolutions.bestx.connections.regulated;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.FORMAT_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ERROR_CODE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_FILL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_MIN_INCREMENT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_MIN_QTY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_MKT_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_PRICE_ITEM_COUNT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_QTY_MULTIPLIER;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_ISIN;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SIDE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBMARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBSCRIPTION;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_USERNAME;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SECURITY_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SEGMENT_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SESSION_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TRADING_SESSION_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_USR_STATUS;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_EXECUTION_REPORT;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_ORDER_CANCEL_RESPONSE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_PRICE_RESPONSE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_REG_PRICEREQ_PDU;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_SECURITY_DEFINITION;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_MARKET_STATUS_ON;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_MKT_STATUS_ON;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_STATUS_CANCELLED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_STATUS_EXECUTED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_STATUS_NEW;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_STATUS_PARTIALFILL_EXECUTED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_ORDER_STATUS_REJECTED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_TRADING_STATUS_NOT_QUOTED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_TRADING_STATUS_QUOTED;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_USR_STATUS_ON;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.regulated.RegulatedConnectionListener.StatusField;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.connections.xt2.XT2MessageFields;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.markets.regulated.RegulatedMarket;
import it.softsolutions.bestx.markets.regulated.RegulatedMarketHelper;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.SubMarketCode;
import it.softsolutions.network.NetData;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * @author Stefano
 * 
 */
public class RegulatedConnector extends XT2BaseConnector implements RegulatedConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegulatedConnector.class);
    protected RegulatedConnectionListener listener;
    private List<String> errorList = null;
    private RegulatedMarketHelper regulatedMarketHelper;

    public RegulatedConnector() {
        super();
        regulatedMarketHelper = new RegulatedMarketHelper();
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public void setErrorList(List<String> errorList) {
        this.errorList = errorList;
    }

    @Override
    public void setRegulatedConnectionListener(RegulatedConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void requestInstrumentPriceSnapshot(String regSessionId, Instrument instrument, String subMarket, String market) throws BestXException {
        XT2Msg buyReq = new XT2Msg(SUBJECT_REG_PRICEREQ_PDU);
        buyReq.setValue(LABEL_REG_ISIN, instrument.getIsin());
        buyReq.setValue(LABEL_REG_SIDE, 0); // BUY
        buyReq.setValue(LABEL_REG_MARKET, market);
        buyReq.setValue(LABEL_REG_SUBMARKET, subMarket);
        buyReq.setValue(LABEL_REG_SUBSCRIPTION, 0); // SNAPSHOT
        buyReq.setValue(LABEL_REG_SESSION_ID, "DPB" + regSessionId);

        XT2Msg sellReq = new XT2Msg(SUBJECT_REG_PRICEREQ_PDU);
        sellReq.setValue(LABEL_REG_ISIN, instrument.getIsin());
        sellReq.setValue(LABEL_REG_SIDE, 1); // SELL
        sellReq.setValue(LABEL_REG_MARKET, market);
        sellReq.setValue(LABEL_REG_SUBMARKET, subMarket);
        sellReq.setValue(LABEL_REG_SUBSCRIPTION, 0); // SNAPSHOT
        sellReq.setValue(LABEL_REG_SESSION_ID, "DPS" + regSessionId);

        LOGGER.info("Price request towards " + market + " market [REQUEST=" + sellReq.toString() + "]");
        sendRequest(sellReq);
        LOGGER.info("Price request towards " + market + " market [REQUEST=" + buyReq.toString() + "]");
        sendRequest(buyReq);
    }

    @Override
    public void onPublish(XT2Msg msg) {
        try {
            LOGGER.info("Publish message from regulated market [" + msg.toString() + "]");
            if (msg.getName().equals(LABEL_SECURITY_STATUS)) {
                LOGGER.debug("XT2CSecurityStatus - SubMarketName: " + " - " + msg.getString(LABEL_REG_SUBMARKET) != null ? msg.getString(LABEL_REG_SUBMARKET) : " " + " - "
                        + msg.getString(LABEL_TRADING_SESSION_STATUS));
                int segmentStatus = -1;
                try {
                    if (msg.getString(LABEL_SEGMENT_STATUS) != null && msg.getString(LABEL_SEGMENT_STATUS).trim().length() > 0) {
                        segmentStatus = Integer.parseInt(msg.getString(LABEL_SEGMENT_STATUS));
                    } else {
                        segmentStatus = 1;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error extracting {} from msg [{}] : {}", LABEL_SEGMENT_STATUS, msg, e.getMessage());
                    segmentStatus = -1;
                }

                if (((msg.getString(LABEL_TRADING_SESSION_STATUS).equals(VALUE_TRADING_STATUS_QUOTED) || msg.getString(LABEL_TRADING_SESSION_STATUS).equals(VALUE_TRADING_STATUS_VOLATILITY_AUCTION)) && (segmentStatus == 1))
                        || msg.getString(LABEL_TRADING_SESSION_STATUS).equals(VALUE_TRADING_STATUS_NOT_QUOTED)) {
                    listener.onSecurityStatus(msg.getString(LABEL_REG_ISIN), msg.getString(LABEL_REG_SUBMARKET), msg.getString(LABEL_TRADING_SESSION_STATUS));
                }

            } else if (msg.getSubject().indexOf(SUBJECT_SECURITY_DEFINITION) > 0 && msg.getString(LABEL_REG_ISIN) != null) {
                try {
                    /*
                     * AMC 20090217 aggiunto perche' su HIMTF non si riesce a fare arrivare SettlmntDate ma arriva SettlDate in security
                     * definition
                     */
                    String settlementDate = msg.getString(LABEL_SETTLEMENT_DATE);
                    if (settlementDate == null) {
                        settlementDate = msg.getString("SettlDate");
                    } /* Fine AMC 20090217 */
                    listener.onSecurityDefinition(msg.getString(LABEL_REG_ISIN), msg.getString(LABEL_REG_MARKET),
                            settlementDate != null ? DateUtils.parseDate(settlementDate, new String[] { FORMAT_SETTLEMENT_DATE }) : null,
                            /*
                             * 2009-04-15 : Ruggero If the following fields have not been sent by the market then we pass null to the
                             * method. Only HIMTF should send them.
                             */
                            msg.getString(LABEL_MIN_QTY) == null ? null : BigDecimal.valueOf(msg.getDouble(LABEL_MIN_QTY)),
                            msg.getString(LABEL_MIN_INCREMENT) == null ? null : BigDecimal.valueOf(msg.getDouble(LABEL_MIN_INCREMENT)),
                            msg.getString(LABEL_QTY_MULTIPLIER) == null ? null : BigDecimal.valueOf(msg.getDouble(LABEL_QTY_MULTIPLIER)));
                } catch (Exception e) {
                    LOGGER.error("unable to parse settlement date" + " : " + e.toString(), e);
                }
            } else if (msg.getName().indexOf(RegulatedMessageFields.SUBJECT_MARKET_STATUS) >= 0 && msg.toString().indexOf(LABEL_MKT_STATUS) > 0) {
                int marketStatus;
                try {
                    marketStatus = msg.getInt(LABEL_MKT_STATUS);
                } catch (Exception e) {
                    LOGGER.error("An error occurred while retrieving Market Status" + " : " + e.toString());
                    return;
                }
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onConnectionStatus(null, marketStatus == VALUE_MKT_STATUS_ON, StatusField.MARKET_STATUS);
                }
            } else if (msg.getName().indexOf(RegulatedMessageFields.SUBJECT_USER_STATUS) >= 0 && msg.toString().indexOf(LABEL_USR_STATUS) > 0) {
                int userStatus;
                try {
                    userStatus = msg.getInt(LABEL_USR_STATUS);
                } catch (Exception e) {
                    LOGGER.error("An error occurred while retrieving User Status" + " : " + e.toString());
                    return;
                }
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onConnectionStatus(null, userStatus == VALUE_USR_STATUS_ON, StatusField.USER_STATUS);
                }
            } else if ((msg.getSubject().indexOf(SUBJECT_EXECUTION_REPORT) > 0 || msg.getSubject().indexOf(LABEL_FILL) > 0)) {

                String regSessionId = msg.getString(LABEL_REG_SESSION_ID);

                if (listener == null) {
                    LOGGER.error("NO Regulated Listener available");
                    return;
                }

                if (regSessionId == null) {
                    LOGGER.info("Regulated FILL with null CD: " + msg.toString());
                    return;
                }

                int status = -1;
                try {
                    status = msg.getInt(LABEL_ORDER_STATUS);
                    /*
                     * String sTRstatus = msg.getString(LABEL_ORDER_STATUS); status = Integer.parseInt(sTRstatus);
                     */} catch (Exception e) {
                    LOGGER.error("FILL with null STATUS: " + msg.toString() + " : " + e.toString());
                    return;
                }
                if (status == VALUE_ORDER_STATUS_NEW) {
                    String orderId = msg.getString(LABEL_ORDER_ID);
                    double price = 0.0;
                    try {
                        price = msg.getDouble(LABEL_REG_PRICE);
                    } catch (Exception ex) {
                        LOGGER.error("FILL with status " + VALUE_ORDER_STATUS_NEW + " (New) missing " + LABEL_REG_PRICE + " field.");
                        // return;
                    }
                    if (price > 0.0) {
                        LOGGER.debug("The Magnet market sent the price at which it put the order on the book : " + price);
                        // we've a price, get it and store it in the related attempt
                        regulatedMarketHelper.onMarketPriceReceived(regSessionId, BigDecimal.valueOf(price), new RegulatedFillInputBean(msg, regSessionId));
                    }

                    listener.onOrdeReceived(regSessionId, orderId);
                } else if (status == VALUE_ORDER_STATUS_REJECTED) {
                    listener.onOrderReject(regSessionId, null);
                } else if (status == VALUE_ORDER_STATUS_PARTIALFILL_EXECUTED || status == VALUE_ORDER_STATUS_EXECUTED) {
                    listener.onExecutionReport(regSessionId, ExecutionReportState.PART_FILL, new RegulatedFillInputBean(msg, regSessionId));
                } else if (status == VALUE_ORDER_STATUS_CANCELLED) {
                    String note = msg.getString("Text");
                    if (note != null && note.length() > 0 && (note.startsWith("4") || note.startsWith("6"))) {
                        regulatedMarketHelper.onFasCancelFillAndBook(regSessionId, "6");
                    } else if (note != null && note.length() > 0 && (note.startsWith("3") || note.startsWith("19"))) {
                        regulatedMarketHelper.onFasCancelFillNoBook(regSessionId, (note.startsWith("3") ? "3" : "19"));
                    } else if (note != null && note.length() > 0 && (note.startsWith("1") || note.startsWith("2") || note.startsWith("17"))) {
                        // listener.onFasCancelNoFill(regSessionId, (note.startsWith("17") ? "17" : (note.startsWith("2") ? "2" : "1")));
                        regulatedMarketHelper.onFasCancelNoFill(regSessionId, (note.startsWith("17") ? "17" : (note.startsWith("2") ? "2" : "1")), ExecutionReportState.CANCELLED,
                                new RegulatedFillInputBean(msg, regSessionId));
                    } else {
                        listener.onOrderCancelled(regSessionId, null);
                    }
                } else {
                    LOGGER.error("Regulated FILL declared order status not recognized: " + msg.toString());
                }
            } else if (msg.getSubject().indexOf(SUBJECT_ORDER_CANCEL_RESPONSE) >= 0) {
                String regSessionId = msg.getString(LABEL_REG_SESSION_ID);

                if (listener == null) {
                    LOGGER.error("NO Regulated Listener available");
                    return;
                }

                if (regSessionId == null) {
                    LOGGER.error("Regulated FILL with null CD: " + msg.toString());
                    return;
                }
                listener.onCancelRequestReject(regSessionId, "");
            } else {
                LOGGER.debug("Forward message to superclass");
                super.onPublish(msg);
            }
        } catch (Throwable t) {
            LOGGER.error("General error on Publish.", t);
        }
    }

    @Override
    public void onNotification(XT2Msg msg) {
        try {
            LOGGER.info("Notification message from regulated market [" + msg.toString() + "] - subject: " + msg.getSubject() + " - name: " + msg.getName());
            int errorCode = 0;
            if (msg.toString().indexOf(LABEL_ERROR_CODE) >= 0) {
                try {
                    errorCode = msg.getInt(LABEL_ERROR_CODE);
                } catch (Exception e) {
                    LOGGER.error("Error while retrieving error code from the notify [" + msg.toString() + "]" + " : " + e.toString(), e);
                    return;
                }
                if (errorCode != 0) {
                    LOGGER.error("Notify with error code: " + errorCode + " [" + msg.toString() + "]");
                    if (msg.getSubject().indexOf(XT2MessageFields.SUBJECT_SERVICE_STARTED) >= 0) {
                        listener.onConnectionError();
                    }
                    return;
                }
            }

            if (msg.getName().indexOf(XT2MessageFields.NAME_EVENT_NOTIFY) >= 0) {
                int eventType = msg.getInt(XT2MessageFields.LABEL_EVENT_TYPE);
                if (eventType == 4) {
                    LOGGER.info("Received a disconnection event from the market" + msg.getString(XT2MessageFields.LABEL_MSG));
                } else if (eventType == 5) {
                    LOGGER.info("Received a connection event from the market" + msg.getString(XT2MessageFields.LABEL_MSG));
                }
            }
            if (msg.getSubject().indexOf(XT2MessageFields.SUBJECT_SERVICE_STARTED) >= 0) {
                LOGGER.debug("Regulated market Connection Login succeded");
                try {
                    connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/MARKET_STATUS/" + msg.getString(LABEL_REG_MARKET));
                    connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/USER_STATUS/" + msg.getString(LABEL_REG_USERNAME));
                    connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/FILL/*");
                    connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/EXECUTIONREPORT/*");
                    boolean bestxRestartProperty = System.getProperty(MarketConnection.BESTX_RESTART) != null;
                    if (!bestxRestartProperty) {
                        connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/SECURITY_DEFINITION/*");
                    } else {
                        LOGGER.info("Bestx restart : skipping security definitions subscription");
                    }

                    connection.subscribe("/" + msg.getString(LABEL_REG_MARKET) + "/Status/*");
                } catch (Exception e) {
                    LOGGER.error("An error occurred while trying to subscribe to Regulated Market subjects" + " : " + e.toString(), e);
                }
                return;
            } else if (msg.getSubject().indexOf(XT2MessageFields.SUBJECT_SERVICE_STOPPED) >= 0) {
                LOGGER.debug("Connection Logoff succeded");
                return;
            } else if (msg.getSubject().indexOf(RegulatedMessageFields.SUBJECT_ORDER_REJECT) >= 0) {
                String regSessionId = msg.getString(LABEL_REG_SESSION_ID);
                String note = msg.getString("Text");
                /*
                 * 20081124 AMC substituted the technical reject verification because asked by TG to always treat a reject as a technical
                 * reject in case need to be re-introduced a separate management for technical and non technical reject should be placed in
                 * Spring the complete list of technical reject or maybe better the list of non technical reject (i.e. the reject due to
                 * price out of market) if (note != null && note.length() > 0 && (note.startsWith("_21_") || (note.startsWith("_50_")))) {
                 */
                if (isTechnicalErrorInMsg(msg)) { // returns true. Overriden in HIMTFConnector
                    // listener.onOrderTechnicalReject(regSessionId, null);
                    // 23-06-2009 Ruggero
                    // try to send the reject reason on
                    listener.onOrderTechnicalReject(regSessionId, note);
                    LOGGER.info("Error received from " + msg.getSourceMarketName() + " market is: " + note);
                } else {
                    listener.onOrderReject(regSessionId, note);
                }
            } else if (msg.getSubject().indexOf(RegulatedMessageFields.SUBJECT_ORDER_CANCEL_REJECT) >= 0) {
                String regSessionId = msg.getString(LABEL_REG_SESSION_ID);
                listener.onCancelRequestReject(regSessionId, null);
            } else if (msg.getSubject().indexOf(RegulatedMessageFields.SUBJECT_TRADING_SESSION_STATUS) >= 0) {
                LOGGER.debug("Trading Session Status received");
                String subMarketCodeString = msg.getString(LABEL_REG_SUBMARKET);
                if (subMarketCodeString != null) {
                    SubMarketCode subMarketCode;
                    try {
                        subMarketCode = SubMarketCode.valueOf(subMarketCodeString);
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("An error occurred while retrieving sub-market" + " : " + e.toString());
                        return;
                    }
                    int marketStatus;
                    try {
                        marketStatus = msg.getInt(LABEL_SESSION_STATUS);
                    } catch (Exception e) {
                        LOGGER.error("An error occurred while retrieving Session Status for sub-market: " + subMarketCode + " : " + e.toString());
                        return;
                    }
                    if (listener == null) {
                        LOGGER.error("No listener available!");
                    } else {
                        listener.onConnectionStatus(null, marketStatus == VALUE_MARKET_STATUS_ON, StatusField.MARKET_STATUS);
                    }
                }
                return;
            }

            String regSessionId = msg.getString(LABEL_REG_SESSION_ID);
            if (regSessionId == null) {
                LOGGER.error("Notify with null CD [" + msg.toString() + "]");
            } else {
                if (msg.getSubject().indexOf(SUBJECT_PRICE_RESPONSE) >= 0) {
                    LOGGER.debug("Price info");
                    String baseRegSessionId;
                    if (regSessionId.startsWith("DPB") || regSessionId.startsWith("DPS"))
                        baseRegSessionId = regSessionId.substring(3);
                    else {
                        LOGGER.error("Error while retrieving data from Market price notification. Invalid session ID [" + msg.toString() + "]");
                        return;
                    }
                    int priceNumber = 0;
                    try {
                        priceNumber = msg.getInt(LABEL_PRICE_ITEM_COUNT);
                    } catch (Exception e) {
                        LOGGER.error("Error while retrieving data from price notification [" + msg.toString() + "]" + " : " + e.toString(), e);
                        return;
                    }
                    ArrayList<RegulatedProposalInputLazyBean> proposals = new ArrayList<RegulatedProposalInputLazyBean>();
                    for (int index = 1; index <= priceNumber; index++)
                        proposals.add(new RegulatedProposalInputLazyBean(msg, index));
                    listener.onInstrumentPrices(baseRegSessionId, proposals);
                    return;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("General error on Notify.", t);
        }
    }

    @Override
    public void sendFokOrder(XT2OutputLazyBean order) throws BestXException {
        XT2Msg msg = order.getMsg();
        String mkt = msg.getString(LABEL_REG_MARKET); // Can be MOT or HIMTF
        LOGGER.info("Send FOK order to " + mkt + " market [" + msg.toString() + "]");
        sendRequest(msg);
    }

    @Override
    public void revokeOrder(CancelRequestOutputBean cancelRequest) throws BestXException {
        XT2Msg msg = cancelRequest.getMsg();
        LOGGER.info("Send Cancel Request to " + cancelRequest.getMsg().getString(LABEL_REG_MARKET) + " market [" + msg.toString() + "]");
        sendRequest(msg);
    }

    @Override
    public void sendFasOrder(FasOrderOutputBean order, RegulatedMarket regulatedMarket) throws BestXException {
    	if(order != null && regulatedMarket != null) {
    		XT2Msg msg = order.getMsg();
            NetData orderId = (NetData) order.getMsg().getValue(RegulatedMessageFields.LABEL_REG_SESSION_ID);
            regulatedMarketHelper.saveOrderAndMarket(orderId.toString(), regulatedMarket);
            LOGGER.info("Send FAS order to " + regulatedMarket.getMarketCode() + " market [" + msg.toString() + "]");
            sendRequest(msg);
        } else {
        	LOGGER.info("Request to send FAS order cannot be performed due to null data: market {} or order{}", regulatedMarket, order);
        	throw new BestXException("Request to send FAS order cannot be performed due to null data");
        }
    }

    protected boolean isTechnicalErrorInMsg(XT2Msg msg) {
        String note = msg.getString("Text");
        if (errorList != null) {
            for (String error : errorList) {
                if (note.indexOf(error) >= 0) {
                    return false;
                }
            }
        } else if (note.indexOf("Illegal quantity in order.") < 0) {
            // AMC 20100706 tratto la quantita' illegale come errore tecnico. Mi scartera' la trading venue in futuro.
            return false;
        }
        return true;
    }

    public RegulatedMarketHelper getRegulatedMarketHelper() {
        return regulatedMarketHelper;
    }

    public void setRegulatedMarketHelper(RegulatedMarketHelper regulatedMarketHelper) {
        this.regulatedMarketHelper = regulatedMarketHelper;
    }
}
