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
package it.softsolutions.bestx.connections.xt2;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.XT2Exception;
import it.softsolutions.xt2.jpapi.XT2ConnectionIFC;
import it.softsolutions.xt2.jpapi.XT2ConnectionListener;
import it.softsolutions.xt2.jpapi.XT2NotificationListener;
import it.softsolutions.xt2.jpapi.XT2ReplyListener;
import it.softsolutions.xt2.jpapi.exceptions.XT2PapiException;
import it.softsolutions.xt2.protocol.XT2Msg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly to mage connection to a market
 * 
 * Project Name : bestxengine-common First created by: stefano.pontillo Creation date: 06/giu/2012
 * 
 **/
public class XT2BaseConnector implements Connection, XT2ConnectionListener, XT2NotificationListener, XT2ReplyListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(XT2BaseConnector.class);
    
    protected XT2ConnectionIFC connection;
    protected String serviceName;
    private boolean isConnected;
    private XT2ConnectionProvider xt2ConnectionProvider;
    private ConnectionListener listener;

    /**
     * Sets the XT2 Connection adapter
     * 
     * @param xt2Adapter
     *            the connection provider
     */
    public void setXt2ConnectionProvider(XT2ConnectionProvider xt2Adapter) {
        xt2ConnectionProvider = xt2Adapter;
    }

    /**
     * Sets the name of the service
     * 
     * @param serviceName
     *            the service name
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Verify that all requisites are set by spring framework
     * 
     * @throws ObjectNotInitializedException
     */
    protected void checkPreRequisites() throws ObjectNotInitializedException {
        if (xt2ConnectionProvider == null) {
            throw new ObjectNotInitializedException("No XT2 adapter set");
        }
        if (serviceName == null) {
            throw new ObjectNotInitializedException("Service name not set");
        }
    }

    @Override
    public void connect() throws BestXException {
        checkPreRequisites();
        try {
            connection = xt2ConnectionProvider.getConnection(serviceName);
        } catch (XT2PapiException e) {
            throw new BestXException("Error while trying to connect to '" + serviceName + "' service: " + e.getMessage(), e);
        }
        connection.removeConnectionListener(this);
        connection.addConnectionListener(this);
        connection.removeNotificationListener(this);
        connection.addNotificationListener(this);
        connection.removeReplyListener(this);
        connection.addReplyListener(this);
        try {
            connection.startConnection();
        } catch (Exception e) {
            throw new BestXException("An exception occurred while trying to start XT2 '" + serviceName + "' connection: " + e.getMessage(), e);
            // TODO ### AMC 20091124 Aggiungere qui qualche cosa che consenta al listener di capire che fallisce la login
        }
    }

    @Override
    public void disconnect() throws BestXException {
        try {
        	LOGGER.info("Disconnecting {}", connection.getConnectionServiceName());
            connection.stopConnection();
        } catch (Exception e) {
            throw new BestXException("An error occurred while trying to stop XT2 '" + serviceName + "' connection" + " : " + e.toString(), e);
        }
    }

    @Override
    public void onConnection(String serviceName, String userName) {
        LOGGER.debug("Connection: [{}] {}", serviceName, userName);
        isConnected = true;
        if (listener != null) {
            listener.onConnection(this);
        }
    }

    @Override
    public void onDisconnection(String serviceName, String userName) {
    	LOGGER.info("Disconnection: [{}] {}", serviceName, userName);
        isConnected = false;
        if (listener != null) {
            listener.onDisconnection(this, "Unknown");
        }
    }

    @Override
    public void onNotification(final XT2Msg msg) {
        LOGGER.debug("[{}] {}", serviceName, msg);
    }

    @Override
    public void onReply(XT2Msg msg) {
        LOGGER.debug("[{}] {}", serviceName, msg);
    }

    @Override
    public void onPublish(XT2Msg msg) {
        LOGGER.debug("[{}] {}", serviceName, msg);
    }

    @Override
    public void onQueryReply(XT2Msg msg) {
        LOGGER.debug("[{}] {}", serviceName, msg);
    }

    @Override
    public void onSubscribeResult(String subject, int subscribeResult) {
        LOGGER.debug("[{}] {}, {}", serviceName, subject, subscribeResult);
    }

    @Override
    public void onUnsubscribeResult(String subject, int subscribeResult) {
        LOGGER.debug("[{}] {}, {}", serviceName, subject, subscribeResult);
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Send a request message to the market, the response is handled by onReply method
     * 
     * @param msg
     *            XT2Msg contains the request
     * @throws BestXException
     *             throw if connection problem encontered
     */
    public void sendRequest(XT2Msg msg) throws BestXException {
        LOGGER.debug("[{}] {}", serviceName, msg);
        try {
            connection.sendRequest(msg);
        } catch (Exception e) {
            throw new XT2Exception("An error occurred while sending a request message through XT2 client connection to '" + serviceName + "'" + " : " + e.toString(), e);
        }
    }

    /**
     * This method subscribe to receive all message from a specific market queue
     * 
     * @param queue
     *            name of the queue to subscribe
     * @throws BestXException
     *             throw if connection problem encontered
     */
    public synchronized void subscribe(String queue) throws BestXException {
        LOGGER.debug("[{}] {}", serviceName, queue);
        try {
            connection.subscribe(queue);
        } catch (Exception e) {
            throw new XT2Exception("An error occurred while subscribe a queue through XT2 client connection to '" + serviceName + "'" + " : " + e.toString(), e);
        }
    }

    @Override
    public String getConnectionName() {
        return serviceName;
    }

    @Override
    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }
}
