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

package it.softsolutions.bestx.connections.amosgateway;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.FixGatewayConnection;
import it.softsolutions.bestx.connections.FixGatewayConnectionListener;
import it.softsolutions.bestx.connections.fixgateway.FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixMessageTypes;
import it.softsolutions.bestx.connections.fixgateway.FixOrderRejectOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixQuoteOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqResponseOutputLazyBean;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.services.logutils.ApplicationMonitor;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class  OMS2GatewayConnector extends XT2BaseConnector implements FixGatewayConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(OMS2GatewayConnector.class);
   private FixGatewayConnectionListener listener;
   private String dateFormat;
   private String dateTimeFormat;
   private String privateClientId;

   /**
    * Init method
    *
    * @throws BestXException the bestx exception
    */
   public void init() throws BestXException {
      checkPreRequisites();
   }

   @Override
   protected void checkPreRequisites() throws ObjectNotInitializedException {
      if (dateFormat == null) {
         throw new ObjectNotInitializedException("Date format not set");
      }
      if (dateTimeFormat == null) {
         throw new ObjectNotInitializedException("DateTime format not set");
      }
   }

   @Override
   public void setFixGatewayListener(FixGatewayConnectionListener listener) {
      this.listener = listener;
   }

   /**
    * Sets the date format.
    *
    * @param dateFormat the new date format
    */
   public void setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
   }

   /**
    * Sets the date time format.
    *
    * @param dateTimeFormat the new date time format
    */
   public void setDateTimeFormat(String dateTimeFormat) {
      this.dateTimeFormat = dateTimeFormat;
   }

   @Override
   public void onConnection(String serviceName, String userName) {
      super.onConnection(serviceName, userName);
      try {
         connection.subscribe("/FIX/ORDER/*");
      } catch (Exception e) {
         LOGGER.error("An error occurred while trying to subscribe to order subject. Disconnect", e);
         try {
            disconnect();
         }
         catch (BestXException e1) {
            LOGGER.error("Error while trying to disconnect. Presume disconnection");
            onDisconnection(serviceName, userName);
         }
      }
   }

   @Override
   public void onNotification(final XT2Msg msg) {
      LOGGER.info("{} customer notification received [{}]", serviceName, msg);
      if (!onMessage(msg))
      {
         super.onNotification(msg);
      }
   }

   @Override
   public void onPublish(XT2Msg msg) {
      LOGGER.info("{} customer publish received [{}]", serviceName, msg);
      if (!onMessage(msg))
      {
         super.onPublish(msg);
      }
   }

   /**
    * 
    * Check if the IDLE status has been reached
    * @param msg XT2Message coming from market
    * @return true if idle is arrived
    */
   protected boolean isIdleMsg(XT2Msg msg)
   {
      String idle = msg.getString("$IBType_____");
      if("SUB_IDLE".equalsIgnoreCase(idle))
      {
         return true;
      }

      return false;
   }

   /**
    * On message.
    *
    * @param msg the msg
    * @return true, if successful
    */
    public boolean onMessage(final XT2Msg msg) {

        try {
            FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

            switch (messageType) {
            case QUOTE_REQUEST: {
                LOGGER.debug("RFQ received");
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onFixRfqNotification(OMS2GatewayConnector.this, new FixRfqInputLazyBean(msg, dateFormat, dateTimeFormat));
                }
            }
                break;
            case quoteReqID: {
                LOGGER.debug("Quote response received");
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    int ackStatus;
                    try {
                        ackStatus = msg.getInt(FixMessageFields.FIX_QuoteAckStatus);
                    } catch (Exception e) {
                        LOGGER.error("An error occurred while reading Quote Ack Status from message [{}]. Discarding message.", msg, e);
                        return true;
                    }
                    String rfqId = msg.getString(FixMessageFields.FIX_QuoteRequestID);
                    switch (ackStatus) {
                    case 5:
                        String reason = msg.getString(FixMessageFields.FIX_QuoteRejectReason);
                        String text = msg.getString(FixMessageFields.FIX_Text);
                        listener.onFixQuoteNotAcknowledged(this, rfqId, ackStatus, reason + " (" + text + ")");
                        break;
                    default:
                        listener.onFixQuoteAcknowledged(this, rfqId);
                    }
                }
            }
                break;
            case ORDER: {
                if (isIdleMsg(msg)) {
                    LOGGER.info("idle msg received for [{}] subscription, ignoring it", msg.getSubject());
                    return true;
                }

                LOGGER.debug("Order received");

                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onFixOrderNotification(OMS2GatewayConnector.this, new OMS1FixOrderInputLazyBean(msg, dateFormat, dateTimeFormat, privateClientId));
                }
            }
                break;
            case ORDCANCEL: {
                LOGGER.debug("Order revoke received");
                String revokeId = AmosUtility.getInstance().wrapString(msg.getString(FixMessageFields.FIX_ClOrdID));
                String orderId = AmosUtility.getInstance().wrapString(msg.getString(FixMessageFields.FIX_OrigClOrdID));
                String fixSessionId = msg.getString(FixMessageFields.FIX_SessionID);
                if (orderId == null || orderId.trim().isEmpty()) {
                    LOGGER.error("Revoke received without OrderId!");
                    return true;
                }
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onFixRevokeNotification(this, orderId, revokeId, fixSessionId);
                }
            }
                break;
            default:
                LOGGER.warn("Unexpected messageType [{}]: skip processing", messageType);
                return false;
            }
            return true;

        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unexpected messageType [{}]: skip processing", msg.getName());
            return false;
        }
    }

   @Override
   public void onReply(XT2Msg msg) {
      LOGGER.info("{} customer reply received [{}]", serviceName, msg);
      
      FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

      if (messageType == FixMessageTypes.TRADE_RESP) {
         LOGGER.info("Execution report received");
         if (listener == null) {
            LOGGER.error("No listener available!");
         } else {              
            int errorCode;
            try {
               errorCode = msg.getInt(FixMessageFields.FIX_ErrorCode);
            }
            catch (Exception e) {
               LOGGER.error("An error occurred while trying to get error code from message: {} assuming error", msg);
               errorCode = 1000;
            }
            //String orderId = msg.getString(FixMessageFields.FIX_OrderID);
            String orderId = msg.getString(FixMessageFields.FIX_ClOrdID);
            orderId = AmosUtility.getInstance().wrapString(orderId);
            String executionReportId = msg.getString(FixMessageFields.FIX_ExecID);
            LOGGER.debug("Execution report reply error: {}", errorCode);
            if (errorCode == 0) {
               listener.onFixTradeAcknowledged(OMS2GatewayConnector.this, orderId, executionReportId);
            } else {
               String errorMsg = msg.getString(FixMessageFields.FIX_ErrorMsg);
               listener.onFixTradeNotAcknowledged(OMS2GatewayConnector.this, orderId, executionReportId, errorCode, errorMsg);
            }
         }
      }
      else
      {
         super.onPublish(msg);
      }
   }

   @Override
   public void sendExecutionReport(FixGatewayConnectionListener source, FixExecutionReportOutputLazyBean executionReport) throws BestXException {
      LOGGER.info("Send execution report to {} customer interface [{}]", serviceName, executionReport.getMsg());
      ApplicationMonitor.onOrderClose(connection.getConnectionServiceName());	// for statistics logging
      sendRequest(executionReport.getMsg());
   }

   @Override
   public void sendOrderReject(FixGatewayConnectionListener source, FixOrderRejectOutputLazyBean orderReject) throws BestXException {
      LOGGER.info("Send order reject to {} customer interface [{}]", serviceName, orderReject.getMsg());
      sendRequest(orderReject.getMsg());
   }

   @Override
   public void sendRfqResponse(FixGatewayConnectionListener source, FixRfqResponseOutputLazyBean rfqResponse) throws BestXException {
      LOGGER.info("Send RFQ response to {} customer interface [{}]", serviceName, rfqResponse.getMsg());
      sendRequest(rfqResponse.getMsg());
   }

   @Override
   public void sendQuote(FixGatewayConnectionListener source, FixQuoteOutputLazyBean quote) throws BestXException {
      LOGGER.info("Send quote to {} customer interface [{}]", serviceName, quote.getMsg());
      sendRequest(quote.getMsg());
   }

   @Override
   public void sendOrderResponse(FixGatewayConnectionListener source, FixOrderResponseOutputLazyBean orderResponse) throws BestXException {
      LOGGER.info("Send order response to {} customer interface [{}]", serviceName, orderResponse.getMsg());
      sendRequest(orderResponse.getMsg());
   }

   @Override
   public void sendRevokeResponse(FixGatewayConnectionListener source,
         String revokeId,
         FixRevokeResponseOutputLazyBean revokeResponse)
               throws BestXException {
      XT2Msg msg = revokeResponse.getMsg();
      msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(revokeId));
      LOGGER.info("Send revoke response to {} customer interface [{}]", serviceName, msg);
      sendRequest(msg);
   }

   @Override
   public void sendRevokeReport(FixGatewayConnectionListener source,
         String revokeId,
         FixRevokeReportLazyBean revokeReport) throws BestXException {
      XT2Msg msg = revokeReport.getMsg();
      msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(revokeId));
      LOGGER.info("Send revoke report to {} customer interface [{}]", serviceName, msg);
      sendRequest(msg);
   }

   /**
    * Sets the private client id.
    *
    * @param clientId the new private client id
    */
   public void setPrivateClientId(String clientId) {
      this.privateClientId = clientId;
   }

   /**
    * Gets the date format.
    *
    * @return the date format
    */
   public String getDateFormat() {
      return dateFormat;
   }

   @Override
   public void sendGenericRevokeReport(FixGatewayConnectionListener source, String revokeId,
         FixOutputLazyBean revokeReport) throws BestXException
         {
      XT2Msg msg = revokeReport.getMsg();
      msg.setValue(FixMessageFields.FIX_ClOrdID, AmosUtility.getInstance().unwrapString(revokeId));
      LOGGER.info("Send revoke report to {} customer interface [{}]", serviceName, msg.toString());
      sendRequest(msg);   
         }

}