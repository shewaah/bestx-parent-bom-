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
package it.softsolutions.bestx.fixclient;

import it.softsolutions.bestx.fix.BXBusinessMessageReject;
import it.softsolutions.bestx.fix.BXExecutionReport;
import it.softsolutions.bestx.fix.BXOrderCancelReject;
import it.softsolutions.bestx.fix.BXReject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DataDictionary;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.ApplVerID;
import quickfix.field.MsgType;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client First created by: davide.rossoni Creation date: Jun 11, 2012
 * 
 **/
public class FIXInitiator implements Application {
    private static final Logger logger = LoggerFactory.getLogger(FIXInitiator.class);

    private FIXClientCallback fixClientCallback;
    private FIXInitiatorListener fixInitiatorListener;
    private DataDictionary dataDictionary;

    public FIXInitiator(FIXInitiatorListener fixInitiatorListener, DataDictionary dataDictionary, FIXClientCallback fixClientCallback) {
        this.fixInitiatorListener = fixInitiatorListener;
        this.fixClientCallback = fixClientCallback;
        this.dataDictionary = dataDictionary;
    }

    /**
     * This method is called when quickfix creates a new session. A session comes into and remains in existence for the life of the application. Sessions exist whether or not a counter party is
     * connected to it. As soon as a session is created, the application can begin sending messages to it. If no one is logged on, the messages will be sent at the time a connection is established
     * with the counterparty.
     */
    @Override
    public void onCreate(SessionID sessionID) {
        logger.info("sessionID = {}", sessionID);
    }

    /**
     * This callback notifies when a valid logon has been established with a counter party. This is called when a connection has been established and the FIX logon process has completed with both
     * parties exchanging valid logon messages.
     */
    @Override
    public void onLogon(SessionID sessionID) {
        logger.info("sessionID = {}", sessionID);

        fixInitiatorListener.onLogon(sessionID);
        fixClientCallback.onLogon(sessionID);
    }

    /**
     * This callback notifies when a FIX session is no longer online. This could happen during a normal logout exchange or because of a forced termination or a loss of network connection.
     */
    @Override
    public void onLogout(SessionID sessionID) {
        logger.info("sessionID = {}", sessionID);

        fixInitiatorListener.onLogout(sessionID);
        fixClientCallback.onLogout(sessionID);
    }

    /**
     * This callback provides Synapse with a peek at the administrative messages that are being sent from your FIX engine to the counter party. This is normally not useful for an application however
     * it is provided for any logging one may wish to do.
     */
    @Override
    public void toAdmin(Message message, SessionID sessionID) {
        // if (logger.isDebugEnabled()) {
        // StringBuilder sb = new StringBuilder();
        // try {
        // sb.append("Sending admin level FIX message to ").append(message.getHeader().getField(new TargetCompID()).getValue());
        // sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
        // sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
        // sb.append("\nSender ID: ").append(message.getHeader().getField(new SenderCompID()).getValue());
        // } catch (FieldNotFound e) {
        // sb.append("Sending admin level FIX message...");
        // logger.warn("One or more required fields are not found in the response message", e);
        // }
        // logger.debug(sb.toString());
        // if (logger.isTraceEnabled()) {
        // logger.trace("Message: " + message.toString());
        // }
        // }

        try {
            fromAdmin(message, sessionID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This callback notifies when an administrative message is sent from a counterparty to the FIX engine.
     */
    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.info("FromAdmin {} {}", sessionID, message);
        it.softsolutions.bestx.fix.field.ApplVerID applVerID = it.softsolutions.bestx.fix.field.ApplVerID.Unknown;
        if (message.getHeader().isSetField(ApplVerID.FIELD)) {
            String fixApplVerID = message.getHeader().getString(ApplVerID.FIELD);
            applVerID = it.softsolutions.bestx.fix.field.ApplVerID.getInstanceForFIXValue(fixApplVerID);
        } else {
            String beginString = sessionID.getBeginString();
            if (beginString.equals("FIX.4.1")) {
                applVerID = it.softsolutions.bestx.fix.field.ApplVerID.FIX41;
            } else if (beginString.equals("FIX.4.2")) {
                applVerID = it.softsolutions.bestx.fix.field.ApplVerID.FIX42;
            } else {
                logger.warn("Unexpected beginString [{}]", beginString);
                applVerID = it.softsolutions.bestx.fix.field.ApplVerID.Unknown;
            }
        }

        String fixMsgType = message.getHeader().getString(MsgType.FIELD);
        it.softsolutions.bestx.fix.field.MsgType msgType = it.softsolutions.bestx.fix.field.MsgType.getInstanceForFIXValue(fixMsgType);
        if (logger.isDebugEnabled() && msgType != it.softsolutions.bestx.fix.field.MsgType.Heartbeat) {
            logger.debug("[" + msgType + "][" + sessionID + "]");
        }

        try {
            switch (applVerID) {
            case FIX41: {
                switch (msgType) {
                case Reject:
                    // logger.warn(rejectAsString((quickfix.fix41.Reject) message));
                    BXReject bxReject = BXReject.fromFIX41Message((quickfix.fix41.Reject) message);
                    fixClientCallback.onReject(bxReject);
                    break;
                case Heartbeat:
                case TestRequest:
                case Logon:
                case ResendRequest:
                case SequenceReset:
                    // skip
                    break;
                default:
                    logger.error("MsgType [" + fixMsgType + "][" + msgType + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                    break;
                }
            }
                break;
            case FIX42: {
                switch (msgType) {
                case Reject:
                    logger.warn(rejectAsString((quickfix.fix42.Reject) message));
                    BXReject bxReject = BXReject.fromFIX42Message((quickfix.fix42.Reject) message);
                    fixClientCallback.onReject(bxReject);
                    break;
                case Heartbeat:
                case TestRequest:
                case Logon:
                case ResendRequest:
                case SequenceReset:
                    // skip
                    break;
                default:
                    logger.error("MsgType [" + fixMsgType + "][" + msgType + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                    break;
                }
            }
                break;
            default:
                logger.error("ApplVerID [" + applVerID + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                break;
            }

        } catch (FIXClientException e) {
            logger.error("[{}] {}: {}", sessionID, message, e.getMessage(), e);
        }

        // StringBuilder sb = new StringBuilder();
        // sb.append("Received admin level FIX message from ").append(message.getHeader().getField(new SenderCompID()).getValue());
        // sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
        // sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
        // sb.append("\nReceiver ID: ").append(message.getHeader().getField(new TargetCompID()).getValue());
        // logger.debug(sb.toString());
        // if (logger.isTraceEnabled()) {
        // logger.trace("Message: " + message.toString());
        // }
    }

    private String rejectAsString(quickfix.fix42.Reject reject) {
        try {
            StringBuilder sb = new StringBuilder();

            if (reject.isSetRefMsgType()) {
                sb.append(", RefMsgType [").append(reject.getRefMsgType().getValue()).append("]: ")
                                .append(it.softsolutions.bestx.fix.field.MsgType.getInstanceForFIXValue(reject.getRefMsgType().getValue()));
            }
            if (reject.isSetRefTagID()) {
                sb.append(", RefTagID: ").append(reject.getRefTagID().getValue());
            }
            if (reject.isSetText()) {
                sb.append(", Text: ").append(reject.getText().getValue());
            }

            return sb.toString().startsWith(", ") ? sb.substring(2) : sb.toString();
        } catch (FieldNotFound e) {
            return reject.toString();
        }
    }

    /**
     * This is a callback for application messages that are being sent to a counterparty.
     */
    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        // if (logger.isDebugEnabled()) {
        // StringBuilder sb = new StringBuilder();
        // try {
        // sb.append("Sending application level FIX message to ").append(message.getHeader().getField(new TargetCompID()).getValue());
        // sb.append("\nMessage Type: ").append(message.getHeader().getField(new MsgType()).getValue());
        // sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
        // sb.append("\nSender ID: ").append(message.getHeader().getField(new SenderCompID()).getValue());
        // } catch (FieldNotFound e) {
        // sb.append("Sending application level FIX message...");
        // logger.warn("One or more required fields are not found in the response message", e);
        // }
        // logger.debug(sb.toString());
        // if (logger.isTraceEnabled()) {
        // logger.trace("Message: " + message.toString());
        // }
        // }
    }

    /**
     * spawned from the thread pool for each incoming message.
     */
    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        // if (logger.isDebugEnabled()) {
        // StringBuilder sb = new StringBuilder();
        // sb.append("Received FIX message from ").append(message.getHeader().getField(new SenderCompID()).getValue());
        // sb.append("\nMessage Sequence Number: ").append(message.getHeader().getField(new MsgSeqNum()).getValue());
        // sb.append("\nReceiver ID: ").append(message.getHeader().getField(new TargetCompID()).getValue());
        // logger.debug(sb.toString());
        // if (logger.isTraceEnabled()) {
        // logger.trace("Message: " + message.toString());
        // }
        // }

        try {
            it.softsolutions.bestx.fix.field.ApplVerID applVerID = it.softsolutions.bestx.fix.field.ApplVerID.Unknown;
            if (message.getHeader().isSetField(ApplVerID.FIELD)) {
                String fixApplVerID = message.getHeader().getString(ApplVerID.FIELD);
                applVerID = it.softsolutions.bestx.fix.field.ApplVerID.getInstanceForFIXValue(fixApplVerID);
            } else {
                String beginString = sessionID.getBeginString();
                if (beginString.equals("FIX.4.1")) {
                    applVerID = it.softsolutions.bestx.fix.field.ApplVerID.FIX41;
                } else if (beginString.equals("FIX.4.2")) {
                    applVerID = it.softsolutions.bestx.fix.field.ApplVerID.FIX42;
                } else {
                    logger.warn("Unexpected beginString [{}]", beginString);
                    applVerID = it.softsolutions.bestx.fix.field.ApplVerID.Unknown;
                }
            }

            String fixMsgType = message.getHeader().getString(MsgType.FIELD);
            it.softsolutions.bestx.fix.field.MsgType msgType = it.softsolutions.bestx.fix.field.MsgType.getInstanceForFIXValue(fixMsgType);
            // logger.debug("[{}][{}]", msgType, sessionID);

            switch (applVerID) {
            case FIX41: {
                switch (msgType) {
                case ExecutionReport:
                    BXExecutionReport bxExecutionReport = BXExecutionReport.fromFIX41Message((quickfix.fix41.ExecutionReport) message);
                    bxExecutionReport.setOriginalFixMessage(message.toString());
                    fixClientCallback.onExecutionReport(bxExecutionReport);
                    break;
                case OrderCancelReject:
                    BXOrderCancelReject bxOrderCancelReject = BXOrderCancelReject.fromFIX41Message((quickfix.fix41.OrderCancelReject) message);
                    bxOrderCancelReject.setOriginalFixMessage(message.toString());
                    fixClientCallback.onOrderCancelReject(bxOrderCancelReject);
                    break;
                default:
                    logger.error("MsgType [" + fixMsgType + "][" + msgType + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                    break;
                }
            }
                break;
            case FIX42: {
                switch (msgType) {
                case ExecutionReport:
                	quickfix.fix42.ExecutionReport exRep = new quickfix.fix42.ExecutionReport();
                	exRep.fromString(message.toString(), dataDictionary, false);
                    BXExecutionReport bxExecutionReport = BXExecutionReport.fromFIX42Message(exRep);
                    bxExecutionReport.setOriginalFixMessage(message.toString());
                    fixClientCallback.onExecutionReport(bxExecutionReport);
                    break;
                case OrderCancelReject:
                    BXOrderCancelReject bxOrderCancelReject = BXOrderCancelReject.fromFIX42Message((quickfix.fix42.OrderCancelReject) message);
                    bxOrderCancelReject.setOriginalFixMessage(message.toString());
                    fixClientCallback.onOrderCancelReject(bxOrderCancelReject);
                    break;
                case BusinessMessageReject:
                    BXBusinessMessageReject bxBusinessMessageReject = BXBusinessMessageReject.fromFIX42Message((quickfix.fix42.BusinessMessageReject) message);
                    bxBusinessMessageReject.setOriginalFixMessage(message.toString());
                    fixClientCallback.onBusinessMessageReject(bxBusinessMessageReject);
                    break;
                default:
                    logger.error("MsgType [" + fixMsgType + "][" + msgType + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                    break;
                }
            }
                break;
            default:
                logger.error("ApplVerID [" + applVerID + "] not managed: message [" + message + "], sessionID [" + sessionID + "]");
                break;
            }

        } catch (FIXClientException e) {
            logger.error("[{}] {}: {}", sessionID, message, e.getMessage(), e);
        } catch (Exception ex) {
        	logger.error("[{}] {}: {}", sessionID, message, ex.getMessage(), ex);
		}
    }

}
