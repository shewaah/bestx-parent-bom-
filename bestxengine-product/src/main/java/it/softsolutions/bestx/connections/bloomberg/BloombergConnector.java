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
package it.softsolutions.bestx.connections.bloomberg;

import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_CLASS_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_CLASS_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_CODE_SEPARATOR;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_DES_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_DES_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ERRORCODE_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_INST_CODE_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_ISIN_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_MKT_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_MKT_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_PRICEPDU_SUBJECT;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_SNAPSHOT_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_SNAPSHOT_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_SOURCE_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_SOURCE_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_TYPE_LABEL;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.BLOOM_TYPE_VALUE;
import static it.softsolutions.bestx.connections.bloomberg.BloombergMessageFields.SUBJECT_BBG_PRICE_REQUEST;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 11/ott/2012
 * 
 **/
public class BloombergConnector extends XT2BaseConnector implements BloombergConnection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BloombergConnector.class);

    private BloombergConnectionListener listener;
    private long nyTimeZone;
    private Thread this_thread;
    private LinkedBlockingQueue<XT2Msg> messageQueue;

    public enum RequestStatus {
        PENDING, NOT_PENDING
    }

    Map<String, Map<String, RequestStatus>> pendingRequests = new ConcurrentHashMap<String, Map<String, RequestStatus>>();

    /*
     * (non-Javadoc)
     * 
     * @see
     * it.softsolutions.bestx.connections.bloomberg.BloombergConnection#setBloombergConnectionListener(it.softsolutions.bestx.connections
     * .bloomberg.BloombergConnectionListener)
     */
    public void setBloombergConnectionListener(BloombergConnectionListener listener) {
        this.listener = listener;
    }

    public void requestInstrumentPriceSnapshot(Instrument instrument, String marketMakerCode) throws BestXException {

        if (mustSendRequest(instrument.getIsin(), marketMakerCode)) {
            XT2Msg req = new XT2Msg(SUBJECT_BBG_PRICE_REQUEST);
            req.setValue(BLOOM_MKT_LABEL, BLOOM_MKT_VALUE);
            req.setValue(BLOOM_CLASS_LABEL, BLOOM_CLASS_VALUE);
            req.setValue(BLOOM_SOURCE_LABEL, BLOOM_SOURCE_VALUE);
            req.setValue(BLOOM_TYPE_LABEL, BLOOM_TYPE_VALUE);
            req.setValue(BLOOM_DES_LABEL, BLOOM_DES_VALUE);
            req.setValue(BLOOM_ISIN_LABEL, instrument.getIsin());
            req.setValue(BLOOM_SNAPSHOT_LABEL, BLOOM_SNAPSHOT_VALUE);
            req.setValue(BLOOM_INST_CODE_LABEL, instrument.getIsin() + BLOOM_CODE_SEPARATOR + marketMakerCode);

            LOGGER.info("{}", req);

            sendRequest(req);
        } else {
            LOGGER.debug("Price request for the instrument {} and market maker {} already sent, it is in pending status.", instrument.getIsin(), marketMakerCode);
        }
    }

    private boolean mustSendRequest(String isin, String marketMakerCode) {
        boolean mustSendRequest = true;
        if (pendingRequests.containsKey(isin)) {
            Map<String, RequestStatus> instrumentPendingRequests = pendingRequests.get(isin);
            RequestStatus reqStatus = instrumentPendingRequests.get(marketMakerCode);
            if (reqStatus != null && RequestStatus.PENDING.equals(reqStatus)) {
                LOGGER.debug("Request pending for isin {}, market maker {}", isin, marketMakerCode);
                mustSendRequest = false;
            }
        }
        return mustSendRequest;
    }

    /*
     * Not supportd by BBG Feed yet.
     */
    public void requestInstrumentPriceSnapshot(Instrument instrument, List<String> marketMakerCodeList) throws BestXException {
        XT2Msg req = new XT2Msg(SUBJECT_BBG_PRICE_REQUEST);
        req.setValue(BLOOM_MKT_LABEL, BLOOM_MKT_VALUE);
        req.setValue(BLOOM_CLASS_LABEL, BLOOM_CLASS_VALUE);
        req.setValue(BLOOM_SOURCE_LABEL, BLOOM_SOURCE_VALUE);
        req.setValue(BLOOM_TYPE_LABEL, BLOOM_TYPE_VALUE);
        req.setValue(BLOOM_DES_LABEL, BLOOM_DES_VALUE);
        req.setValue(BLOOM_ISIN_LABEL, instrument.getIsin());
        req.setValue(BLOOM_SNAPSHOT_LABEL, BLOOM_SNAPSHOT_VALUE);
        int i = 0;
        for (String marketMakerCode : marketMakerCodeList) {
            req.setValue(BLOOM_INST_CODE_LABEL + i++, instrument.getIsin() + BLOOM_CODE_SEPARATOR + marketMakerCode);
        }
        LOGGER.info("Request: {}", req);
        sendRequest(req);
    }

    @Override
    public void onReply(XT2Msg msg) {
        LOGGER.info("OnReply: {}", msg);
        if (!messageQueue.offer(msg)) {
            LOGGER.error("Bloomberg price message queue full");
        }
    }

    private void evaluate(XT2Msg msg) {
        int errorCode;
        try {
            errorCode = msg.getInt(BLOOM_ERRORCODE_LABEL);
            if (errorCode != 0) {
                LOGGER.info("Bloomberg message with error: {}", errorCode);
            }
        } catch (Exception e) {
            LOGGER.error("Error while retrieving error code from message : {}", e.getMessage(), e);
        }
        if (msg.getSubject().indexOf(BLOOM_PRICEPDU_SUBJECT) > 0) {
            // AMC 20091116 potrei ricevere n proposte allo stesso tempo
            // int size = msg.getLong("ProposalNum").toLong();
            // for (int i = 0; i < size; i++) {
            // listener.onInstrumentPrice(new BloombergProposalInputLazyBean(msg, ProposalSide.ASK, nyTimeZone, i),
            // new BloombergProposalInputLazyBean(msg, ProposalSide.BID, nyTimeZone,i));
            listener.onInstrumentPrice(new BloombergProposalInputLazyBean(msg, ProposalSide.ASK, nyTimeZone), new BloombergProposalInputLazyBean(msg, ProposalSide.BID, nyTimeZone));
        }
    }

    /**
     * @param nyTimeZone
     *            the nyTimeZone to set
     */
    public void setNyTimeZone(long nyTimeZone) {
        this.nyTimeZone = nyTimeZone;
    }

    public void init() throws BestXException {
        messageQueue = new LinkedBlockingQueue<XT2Msg>();
        start();
    }

    /**
     * start method of the thread
     */
    public void start() {
        if (this_thread == null) {
            this_thread = new Thread(this, "BloombergConnector"); //$NON-NLS-1$
            this_thread.start();
        }
    }

    /**
     * stop method of the thread
     */
    public void stop() {
        Thread old = this_thread;
        this_thread = null;
        old.interrupt();
    }

    public void run() {
        while (this_thread == Thread.currentThread()) {
            try {
                evaluate(messageQueue.take());
                LOGGER.debug("Queue size: {}", messageQueue.size());
            } catch (InterruptedException e) {
                LOGGER.debug("BloombergConnector - Forced queue wait interruption.");
            } catch (Exception ex) {
                LOGGER.error("BloombergConnector - Error: ", ex);
            }
        }
    }
}
