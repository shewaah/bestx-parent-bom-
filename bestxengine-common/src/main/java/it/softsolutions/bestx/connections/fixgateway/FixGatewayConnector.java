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

package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.connections.CustomerConnection.ErrorCode;
import it.softsolutions.bestx.connections.FixGatewayConnection;
import it.softsolutions.bestx.connections.FixGatewayConnectionListener;
import it.softsolutions.bestx.connections.fixgateway.FixExecutionReportOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.connections.fixgateway.FixMessageTypes;
import it.softsolutions.bestx.connections.fixgateway.FixOrderInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderRejectOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOrderResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixQuoteOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeReportLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRevokeResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixRfqResponseOutputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixValidator;
import it.softsolutions.bestx.connections.fixgateway.statistics.FixOrderQueueMonitor;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: Creation date: 23-ott-2012
 * 
 **/
public class FixGatewayConnector extends XT2BaseConnector implements FixGatewayConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixGatewayConnector.class);

    private FixGatewayConnectionListener listener;
    private String dateFormat;
    private String dateTimeFormat;
    private FixValidator fixValidator;
    // [DR20131224] Se dovesse arrivare un blocco di più di 1024 ordini, i restanti resterebbero nella coda InfoBus tra l'OMS1 FixGateway e BestX 
    private BlockingQueue<XT2Msg> customerOrders = new LinkedBlockingQueue<XT2Msg>(1024);
    private CustomerOrdersConsumer customerOrdersConsumer;

    private BlockingQueue<XT2Msg> customerReplies = new LinkedBlockingQueue<XT2Msg>(1024);
    private CustomerRepliesConsumer customerRepliesConsumer;

    private FixOrderQueueMonitor fixOrderQueueMonitor;
    private long dumpMonitorStatisticsInterval;
    
    private Counter pendingJobs;

    /**
     * Inits the.
     * 
     * @throws BestXException
     *             the best x exception
     */
    public void init() throws BestXException {
        checkPreRequisites();
        fixOrderQueueMonitor = FixOrderQueueMonitor.getInstance(dumpMonitorStatisticsInterval);
        new Thread(fixOrderQueueMonitor).start();
        customerOrdersConsumer = new CustomerOrdersConsumer();
        new Thread(customerOrdersConsumer).start();

        customerRepliesConsumer = new CustomerRepliesConsumer();
        Thread customerRepliesThread = new Thread(customerRepliesConsumer);
        customerRepliesThread.setDaemon(true);
        customerRepliesThread.setName("FixGWConnector-CustomerRepliesConsumer");
        customerRepliesThread.start();
        
        try {
        	pendingJobs = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(FixGatewayConnector.class, "pending-jobs"));
        } catch (IllegalArgumentException e) {
        }
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
     * @param dateFormat
     *            the new date format
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Sets the date time format.
     * 
     * @param dateTimeFormat
     *            the new date time format
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
            } catch (BestXException e1) {
                LOGGER.error("Error while trying to disconnect. Presume disconnection : ", e);
                onDisconnection(serviceName, userName);
            }
        }
    }

    @Override
    public void onNotification(final XT2Msg msg) {
        LOGGER.info("FIX customer notification received [{}]", msg);
        if (!onMessage(msg)) {
            super.onNotification(msg);
        }
    }

    /**
     * 
     * 
     * @param msg
     *            XT2Message coming from market
     * @return true if idle is arrived
     */
    protected boolean isIdleMsg(XT2Msg msg) {
        String idle = msg.getString("$IBType_____");
        if ("SUB_IDLE".equalsIgnoreCase(idle)) {
            return true;
        }

        return false;
    }

    @Override
    public void onPublish(XT2Msg msg) {
        LOGGER.info("FIX customer publish received [{}]", msg);
        if (!onMessage(msg)) {
            super.onPublish(msg);
        }
    }

    /**
     * On message.
     * 
     * @param msg
     *            the msg
     * @return true, if successful
     */
    public boolean onMessage(final XT2Msg msg) {
        LOGGER.trace("{}", msg);

        try {
            FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

            switch (messageType) {
            case QUOTE_REQUEST: {
                LOGGER.debug("RFQ received");
                if (listener == null) {
                    LOGGER.error("No listener available!");
                } else {
                    listener.onFixRfqNotification(FixGatewayConnector.this, new FixRfqInputLazyBean(msg, dateFormat, dateTimeFormat));
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
                        LOGGER.error("An error occurred while reading Quote Ack Status from message [{}]. Discarding message. : ", msg, e);
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
                LOGGER.debug("Order received");
                try {
                	pendingJobs.inc();
                    customerOrders.put(msg);
                    fixOrderQueueMonitor.newInMsg();
                } catch (Exception e2) {
                    LOGGER.error("Exception while putting the new order [{}] in the queue", msg, e2);
                }
            }
                break;
            case ORDCANCEL: {
                LOGGER.debug("Order revoke received");
                try {
                	pendingJobs.inc();
                    customerOrders.put(msg);
				} catch (InterruptedException e) {
					LOGGER.error("Exception while putting the new revoke [{}] in the queue", msg, e);
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
        LOGGER.info("FIX customer reply received [{}]", msg);

        FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

        if (messageType == FixMessageTypes.TRADE_RESP) {
            LOGGER.debug("Execution report received");
            try {
               pendingJobs.inc();
               customerReplies.put(msg);
            }
            catch (InterruptedException e) {
               LOGGER.error("Exception while putting the new Execution report [{}] in the queue", msg, e);
            }
        } else {
            super.onPublish(msg);
        }
    }

    @Override
    public void sendExecutionReport(FixGatewayConnectionListener source, FixExecutionReportOutputLazyBean executionReport) throws BestXException {
        LOGGER.info("Send execution report to FIX customer interface [{}]", executionReport.getMsg());
        sendRequest(executionReport.getMsg());
    }

    @Override
    public void sendOrderReject(FixGatewayConnectionListener source, FixOrderRejectOutputLazyBean orderReject) throws BestXException {
        LOGGER.info("Send order reject to FIX customer interface [{}]", orderReject.getMsg());
        sendRequest(orderReject.getMsg());
    }

    @Override
    public void sendRfqResponse(FixGatewayConnectionListener source, FixRfqResponseOutputLazyBean rfqResponse) throws BestXException {
        LOGGER.info("Send RFQ response to FIX customer interface [{}]", rfqResponse.getMsg());
        sendRequest(rfqResponse.getMsg());
    }

    @Override
    public void sendQuote(FixGatewayConnectionListener source, FixQuoteOutputLazyBean quote) throws BestXException {
        LOGGER.info("Send Quote to FIX customer interface [{}]", quote.getMsg());
        sendRequest(quote.getMsg());
    }

    @Override
    public void sendOrderResponse(FixGatewayConnectionListener source, FixOrderResponseOutputLazyBean orderResponse) throws BestXException {
        LOGGER.info("Send Order Response to FIX customer interface [{}]", orderResponse.getMsg());
        sendRequest(orderResponse.getMsg());
    }

    @Override
    public void sendRevokeResponse(FixGatewayConnectionListener source, String revokeId, FixRevokeResponseOutputLazyBean revokeResponse) throws BestXException {
        XT2Msg msg = revokeResponse.getMsg();
        msg.setValue(FixMessageFields.FIX_ClOrdID, revokeId);
        LOGGER.info("Send revoke response to FIX customer interface [{}]", msg);
        sendRequest(msg);
    }

    @Override
    public void sendRevokeReport(FixGatewayConnectionListener source, String revokeId, FixRevokeReportLazyBean revokeReport) throws BestXException {
        XT2Msg msg = revokeReport.getMsg();
        msg.setValue(FixMessageFields.FIX_ClOrdID, revokeId);
        LOGGER.info("Send revoke report to FIX customer interface [{}]", msg);
        sendRequest(msg);
    }

    /**
     * Gets the fix validator.
     * 
     * @return the fix validator
     */
    public FixValidator getFixValidator() {
        return fixValidator;
    }

    /**
     * Sets the fix validator.
     * 
     * @param fixValidator
     *            the new fix validator
     */
    public void setFixValidator(FixValidator fixValidator) {
        this.fixValidator = fixValidator;
    }

    @Override
    public void sendGenericRevokeReport(FixGatewayConnectionListener source, String revokeId, FixOutputLazyBean revokeReport) throws BestXException {
        XT2Msg msg = revokeReport.getMsg();
        msg.setValue(FixMessageFields.FIX_ClOrdID, revokeId);
        LOGGER.info("Send revoke report to FIX customer interface [{}]", msg);
        sendRequest(msg);
    }

    /**
     * @return the dumpMonitorStatisticsInterval
     */
    public long getDumpMonitorStatisticsInterval() {
        return dumpMonitorStatisticsInterval;
    }

    /**
     * @param dumpMonitorStatisticsInterval
     *            the dumpMonitorStatisticsInterval to set
     */
    public void setDumpMonitorStatisticsInterval(long dumpMonitorStatisticsInterval) {
        this.dumpMonitorStatisticsInterval = dumpMonitorStatisticsInterval;
    }

    /**
     * This inner class consumes order and revoke messages put in the blocking queue allowing its serial processing.
     * 
     * @author ruggero.rizzo
     * 
     */
	class CustomerOrdersConsumer implements Runnable {
		private boolean stop;

		public CustomerOrdersConsumer() {
		}

		@Override
		public void run() {
			stop = false;
			while (!stop) {
				XT2Msg msg = null;
				try {
					msg = customerOrders.take();
					pendingJobs.dec();
					FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

					switch (messageType) {
					case ORDER:
						LOGGER.debug("Orders in queue = {}", customerOrders.size());
						fixOrderQueueMonitor.newOutMsg();
						if (isIdleMsg(msg)) {
							LOGGER.info("idle msg received for [{}] subscription, ignoring it", msg.getSubject());
							// nothing to do with the idle message, go on
							continue;
						}

						LOGGER.debug("Order received");
						if (listener == null) {
							LOGGER.error("No listener available!");
						} else {
							FixOrderInputLazyBean fixOrderInputLazyBean = new FixOrderInputLazyBean(msg, dateFormat, dateTimeFormat);

							// [DR20120613] Perform pre-validation on supported FIX values
							if (fixValidator != null) {
								try {
									fixValidator.performPreValidation(fixOrderInputLazyBean);
								} catch (BestXException e) {
									LOGGER.error("Pre-validation not passed [{}], order can not be processed: ", msg, e);

									// Returns an ExecutionReport.Reject to the FIX gateway
									String sessionId = fixOrderInputLazyBean.getSessionId();
									Order order = fixOrderInputLazyBean;
									ErrorCode errorCode = ErrorCode.GENERIC_ERROR;
									String errorMessage = e.getMessage();

									FixOrderResponseOutputLazyBean fixOrdeResponseOutputLazyBean = new FixOrderResponseOutputLazyBean(sessionId, order, errorCode, errorMessage);
									try {
										sendRequest(fixOrdeResponseOutputLazyBean.getMsg());
									} catch (BestXException e1) {
										LOGGER.error("Unable to send the orderResponse [{}] to the FIX client: ", fixOrdeResponseOutputLazyBean.getMsg(), e);
									}
								}
							} else {
								LOGGER.trace("Skip FIX pre-validation, fixValidator not set");
							}
							listener.onFixOrderNotification(FixGatewayConnector.this, fixOrderInputLazyBean);
						}
						break;
					case ORDCANCEL:
						if (listener == null) {
							LOGGER.error("No listener available!");
						} else {
							String revokeId = msg.getString(FixMessageFields.FIX_ClOrdID);
							String origClOrdId = msg.getString(FixMessageFields.FIX_OrigClOrdID);
							String fixSessionId = msg.getString(FixMessageFields.FIX_SessionID);
							listener.onFixRevokeNotification(FixGatewayConnector.this, origClOrdId, revokeId, fixSessionId);
						}
						break;
					default:
						LOGGER.error("Message type not managed by CustomerOrdersConsumer {1}!", messageType);
					}
				}catch (InterruptedException e2) {
					LOGGER.error("Error while getting an order from the queue", e2);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						LOGGER.error("Error while sleeping", e);
					}
					// continue to have not a null msg going on, try again
					continue;
				}
			}
		}
	}
	
   class CustomerRepliesConsumer implements Runnable {

      private boolean stop;

      public CustomerRepliesConsumer(){}

      @Override
      public void run() {
         stop = false;
         while (!stop) {
            XT2Msg msg = null;
            try {
               msg = customerReplies.take();
               pendingJobs.dec();
               LOGGER.debug("Handling a new reply msg = {}", msg);
               LOGGER.debug("Replies in queue = {}", customerReplies.size());
               FixMessageTypes messageType = FixMessageTypes.valueOf(msg.getName());

               switch (messageType) {
                  case TRADE_RESP: {
                     LOGGER.debug("Execution report received");
                     if (listener == null) {
                        LOGGER.error("No listener available!");
                     }
                     else {
                        int errorCode;
                        try {
                           errorCode = msg.getInt(FixMessageFields.FIX_ErrorCode);
                        }
                        catch (Exception e) {
                           LOGGER.error("An error occurred while trying to get error code from message: {}, assuming error: {}", msg, e);
                           errorCode = 1000;
                        }
                        // 20110907 - Ruggero
                        // for tlx, in the exec report, tag 37 (OrderID) we send the market order id, thus this field
                        // will be sent back to us on the exec rep ack. This way we cannot find the operation because
                        // it has been bound the the BestX order id, which is sent back to us in the ClORdID field.
                        // String orderId = msg.getString(FixMessageFields.FIX_OrderID);
                        String orderId = msg.getString(FixMessageFields.FIX_ClOrdID);
                        String executionReportId = msg.getString(FixMessageFields.FIX_ExecID);
                        LOGGER.debug("Execution report reply error: {}", errorCode);
                        if (errorCode == 0) {
                           listener.onFixTradeAcknowledged(FixGatewayConnector.this, orderId, executionReportId);
                        }
                        else {
                           String errorMsg = msg.getString(FixMessageFields.FIX_ErrorMsg);
                           listener.onFixTradeNotAcknowledged(FixGatewayConnector.this, orderId, executionReportId, errorCode, errorMsg);
                        }
                     }
                     break;
                  }
                  default:
                     LOGGER.error("Message type not managed by CustomerOrdersConsumer {1}!", messageType);
               }
            }
            catch (InterruptedException e2) {
               LOGGER.error("Error while getting an order from the queue", e2);
               try {
                  Thread.sleep(500);
               }
               catch (InterruptedException e) {
                  LOGGER.error("Error while sleeping", e);
               }
            }
            catch (Exception e) {
               LOGGER.error("Error: " + e.getMessage() + " on message " + msg, e);
            }
         }
      }
   }
   
}
