package it.softsolutions.bestx.connections.cmf;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.CmfConnection;
import it.softsolutions.bestx.connections.CmfConnectionListener;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

public class CmfConnector extends XT2BaseConnector implements CmfConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmfConnector.class);
    private CmfConnectionListener listener;
    
    public void setListener(CmfConnectionListener listener) {
        this.listener = listener;
    }
    
    /*
     * (non-Javadoc)
     * @see it.softsolutions.bestx.connections.CmfConnection#sendRequest(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.math.BigDecimal, java.math.BigDecimal, java.lang.String, java.lang.String, it.softsolutions.bestx.model.OrderSide, java.lang.String, java.util.Date, java.lang.String)
     * @param tsn the unique TSN of this request
     * @param traderName the name of the Bloomberg book to be reached by this request. Could be null
     * @param account the account string of the request original source: normally a customer id
     * @param isin the ISIN code of the requested instrument
     * @param qty the requested quantity
     * @param price the requested price. null if this is a RFQ
     * @param status for TDS should be set to "1" and for Akros should be set to "4"
     * @param reisChoice the type for this request: Bloomberg terminal configuration dependent("2") or always pending ("4")
     * @param orderSide buy or sell
     * @param orderId the order id
     * @param expiration in millisec the expiration timer. If null expiration time "000000" is sent to CMF
     * @param sourceCode id identifying the software sending the request
     */
    public void sendRequest(String tsn, String traderName, String account, String isin, BigDecimal qty,
            BigDecimal price, String status, String reisChoice, OrderSide orderSide, String orderId,
            String expiration, Date futSettDate, Integer sourceCode, Integer ticketNum) throws BestXException {
        XT2Msg msg = new XT2Msg("PriceInquiryReq");
        msg.setValue(CmfRequestFields.TSN, tsn);
        if (traderName != null){
        	msg.setValue(CmfRequestFields.TRADER, traderName);
        }
        msg.setValue(CmfRequestFields.ACCOUNT, account);
        msg.setValue(CmfRequestFields.SECID, isin);
        msg.setValue(CmfRequestFields.SECIDFLAG, "8"); // for ISIN
        msg.setValue(CmfRequestFields.QUANTITY, qty.doubleValue());
        if (price != null) {
            msg.setValue(CmfRequestFields.PYD, price.doubleValue());
            msg.setValue(CmfRequestFields.PYDFLAG, "1");
        } else {
            msg.setValue(CmfRequestFields.ORDER_TYPE,"30");
            msg.setValue(CmfRequestFields.PYDFLAG, "4");
        }
        msg.setValue(CmfRequestFields.STATUS, status);
        msg.setValue(CmfRequestFields.REIS_CHOICE, reisChoice);
        msg.setValue(CmfRequestFields.REIS_MARKUP, "N");//dice se esiste spread aggiuntivo sul prezzo o no
        // In this protocol, as opposed to FIX, 1 means SELL, 2 means BUY. Here we insert the opposite side respect to the customer
        msg.setValue(CmfRequestFields.SIDE, orderSide == OrderSide.BUY ? "1" : "2");
        msg.setValue(CmfRequestFields.ORDER_ID, orderId);
        msg.setValue(CmfRequestFields.EXPTIME, expiration);
        if (futSettDate != null) {
            try {
                msg.setValue(CmfRequestFields.CMF_OT_SETTLEDATE, DateService.formatAsLong(DateService.dateISO, futSettDate));
            }
            catch (Exception e) {
                LOGGER.error("An error occurred while sending Settlement Date to CMF. Abort message"+" : "+ e.toString());
                return;
            }
        }
        msg.setValue(CmfRequestFields.CMF_OT_SRCCODE, sourceCode.intValue());
        if (ticketNum != null) {
            msg.setValue(CmfPendOrderFields.CMF_OT_BTS_TICKET, ticketNum.intValue());
        }
        LOGGER.debug("Send message to CMF [" + msg.toString() + "]");
        this.sendRequest(msg);
    }
    
    public void onNotification(final XT2Msg msg) {
        LOGGER.debug("CMF notification received [" + msg.toString() + "]");
        if (CmfConnectionEventFields.CMF_OT_CONNECTION_NOTIFY_PDU_NAME.equals(msg.getName())) {
            int status = CmfConnectionEventFields.CMF_OT_STATUS_VAL_UP;
            try {
                status = msg.getInt(CmfConnectionEventFields.CMF_OT_STATUS);
            }
            catch (Exception e) {
                LOGGER.error("An error occurred while trying to read status from CMF message: " + msg.toString(), e);
            }
            if (status == CmfConnectionEventFields.CMF_OT_STATUS_VAL_DOWN) {
                LOGGER.error("Received connection down notification from CMF. Disconnecting...");
                try {
                    disconnect();
                }
                catch (BestXException e) {
                    LOGGER.error("An error occurred while trying to disconnect from CMF"+" : "+ e.toString(), e);
                }
            }
        } else if (CmfOrderAckNackFields.CMF_OT_ACK_NOTIFY_PDU_NAME.equals(msg.getName())) {
            String orderId = msg.getString(CmfOrderAckNackFields.CMF_OT_ORDERID);
            String ticketNum = msg.getString(CmfOrderAckNackFields.CMF_OT_TICKETNUM);
            String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);
            LOGGER.debug("Ack message from CMF - order ID: " + orderId);
            listener.onRequestAck(orderId, ticketNum, traderId);
        } else if (CmfOrderAckNackFields.CMF_OT_NACK_NOTIFY_PDU_NAME.equals(msg.getName())) {
            String orderId = msg.getString(CmfOrderAckNackFields.CMF_OT_ORDERID);
            LOGGER.debug("Nack message from CMF - order ID: " + orderId);
            listener.onRequestNack(orderId);
        } else if (CmfPendOrderFields.CMF_OT_ACCEPT_PDUNAME.equals(msg.getName())) {
            String tsn = msg.getString(CmfOrderAckNackFields.CMF_OT_TSN);
            String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);
            String pendingTicket = msg.getString(CmfPendOrderFields.CMF_OT_PENDING_TKT);
            String btsTicket = msg.getString(CmfPendOrderFields.CMF_OT_BTS_TICKET);
            LOGGER.debug("Accept pending message from CMF - TSN: " + tsn);
            BigDecimal price = null;
            listener.onPendingAccept(tsn, traderId, pendingTicket, btsTicket, price);
        } else if (CmfPendOrderFields.CMF_OT_REJECT_PDUNAME.equals(msg.getName())) {
            String tsn = msg.getString(CmfOrderAckNackFields.CMF_OT_TSN);
            String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);
            String reason = msg.getString(CmfOrderReplyFields.CMF_OT_ERRMSG);
            LOGGER.debug("Reject pending message from CMF - TSN: " + tsn);
            listener.onPendingReject(tsn, traderId, reason);
        } else if (CmfPendOrderFields.CMF_OT_COUNTER_PDUNAME.equals(msg.getName()) || CmfPendOrderFields.CMF_OT_INQUIRY_COUNTER_PDUNAME.equals(msg.getName())) {
            String tsn = msg.getString(CmfOrderAckNackFields.CMF_OT_TSN);
            String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);               
            String pendingTicket = msg.getString(CmfPendOrderFields.CMF_OT_PENDING_TKT);
            String btsTicket = msg.getString(CmfPendOrderFields.CMF_OT_BTS_TICKET);
            String side = msg.getString(CmfRequestFields.SIDE);
            LOGGER.debug("Counter to pending message from CMF - TSN: " + tsn);
            BigDecimal price = null;
            try {
                price = new BigDecimal(msg.getDouble(CmfPendOrderFields.CMF_OT_PYD));
                price = price.setScale(5, BigDecimal.ROUND_HALF_UP);
            } catch (Exception e) {
                LOGGER.error("No price in counter from CMF"+" : "+ e.toString(), e);
            }
            listener.onPendingCounter(tsn, traderId, pendingTicket, btsTicket, price, side);
        } else if (CmfPendOrderFields.CMF_OT_AUTOEXECUTION_PDUNAME.equals(msg.getName())) {
            String tsn = msg.getString(CmfOrderAckNackFields.CMF_OT_TSN);
            String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);
            String pendingTicket = msg.getString(CmfPendOrderFields.CMF_OT_PENDING_TKT);
            String btsTicket = msg.getString(CmfPendOrderFields.CMF_OT_BTS_TICKET);
            LOGGER.debug("Autoexecution message from CMF - TSN: " + tsn);
            BigDecimal price = null;
            try {
                price = new BigDecimal(msg.getDouble(CmfPendOrderFields.CMF_OT_PYD));
                price = price.setScale(5, BigDecimal.ROUND_HALF_UP);
            } catch (Exception e) {
                LOGGER.error("Error getting field PYD from CMF message"+" : "+ e.toString(), e);
            }            
            listener.onAutoExecution(tsn, traderId, pendingTicket, btsTicket, price);
        } else if (CmfPendOrderFields.CMF_OT_EXPIRE_PDUNAME.equals(msg.getName())) {
        	String tsn = msg.getString(CmfOrderAckNackFields.CMF_OT_TSN);
           	String traderId = msg.getString(CmfPendOrderFields.CMF_OT_TRADER);
            listener.onPendingExpire(tsn, traderId);
        } else {
        	super.onNotification(msg);
        }
    }

    public void onReply(XT2Msg msg) {
        LOGGER.debug("CMF reply received: [" + msg.toString() + "]");
        if (CmfOrderReplyFields.CMF_OT_REQUEST_RESP_PDU_NAME.equals(msg.getName())) {
            String orderId = msg.getString(CmfOrderReplyFields.CMF_OT_ORDERID);
            int errorCode = 0;
            try {
                errorCode = msg.getInt(CmfOrderReplyFields.CMF_OT_ERRCODE);
            }
            catch (Exception e) {
                LOGGER.error("An error occurred while trying to read error code from CMF message: " + msg.toString(), e);
            }
            String errorMessage = msg.getString(CmfOrderReplyFields.CMF_OT_ERRMSG);
            listener.onRequestReply(orderId, errorCode, errorMessage);
        } else {
        	super.onReply(msg);
        }
    }
}
