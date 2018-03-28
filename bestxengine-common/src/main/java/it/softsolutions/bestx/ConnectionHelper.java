/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx;

import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: Decorator class for Connection objects. It adds robustness in management of
 * connection process. It doesn't enqueue commands: last command will be
 * immediately executed, and previous ones discarded.
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 19/feb/2013
 * 
 **/
public class ConnectionHelper implements Connection, ConnectionListener, Runnable {
    private static enum Action {
        CONNECT, DISCONNECT
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionHelper.class);
    private static final int DEFAULT_MAX_CONNECT_RETRY = 0;
    private static final int DEFAULT_RETRY_DELAY_MSEC = 5000;
    private static final int DEFAULT_CONNECT_WAIT_MSEC = 1000;
    private Connection connection;
    private ConnectionListener listener;
    private Thread connectionThread;
    private String name;
    private int connectRetry;
    private int maxConnectRetry = DEFAULT_MAX_CONNECT_RETRY;
    private long retryDelayMSec = DEFAULT_RETRY_DELAY_MSEC;
    private long connectWaitMSec = DEFAULT_CONNECT_WAIT_MSEC;
    private Action action;
    private volatile boolean connected; // volatile to guarantee that non-synchronized isConnected() returns up-to-date value
    private boolean errorCondition;
    private boolean isInRunMethod;

    public void setConnection(Connection connection) {
        this.connection = connection;
        name = "CONN[" + connection.getConnectionName() + "]";
        connection.setConnectionListener(this);
    }

    public synchronized void connect() {
        action = Action.CONNECT;
        triggerCommand();
    }

    public synchronized void disconnect() {
        action = Action.DISCONNECT;
        triggerCommand();
        notify();
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onConnection(Connection source) {
        LOGGER.info("Received connection event from '{}'", connection.getConnectionName());
        synchronized (this) {
            connectRetry = 0;
            connected = true;
            notify();
        }
        LOGGER.debug("Dispatch connection event from '{}'", connection.getConnectionName());
        if (listener != null) {
            listener.onConnection(this);
        }
    }

    @Override
    public void onDisconnection(Connection source, String reason) {
        LOGGER.info("Received disconnection event from '{}' - Reason: {}", connection.getConnectionName(), reason);
        if (isInRunMethod) {
            connectRetry = 0;
            connected = false;
            if (errorCondition) {
                errorCondition = false;
                action = Action.CONNECT;
            }
            notify();
        } else {
            synchronized (this) {
                connectRetry = 0;
                connected = false;
                if (errorCondition) {
                    errorCondition = false;
                    action = Action.CONNECT;
                }
                notify();
            }
        }
        LOGGER.debug("Dispatch disconnection event from '" + connection.getConnectionName() + "'");

        if (listener != null) {
            listener.onDisconnection(this, reason);
        }
    }

    public String getConnectionName() {
        return name;
    }

    public synchronized void run() {
        for (;;) {
            try {
                isInRunMethod = true;

                if (action == Action.CONNECT) {
                    connection.connect();
                } else if (action == Action.DISCONNECT) {
                    connection.disconnect();
                }

                LOGGER.debug("Connection thread '{}' entering wait for '{}'", connection.getConnectionName(), action.name());
                wait(connectWaitMSec);
                LOGGER.debug("Connection thread '{}' exiting wait for '{}'", connection.getConnectionName(), action.name());
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted thread while performing {} to '{}': {}", action.name(), connection.getConnectionName(), e.getMessage(), e);
                errorCondition = true;
            } catch (BestXException e) {
                LOGGER.error("A problem occurred while performing {} to '{}': {}", action.name(), connection.getConnectionName(), e.getMessage());
                errorCondition = true;
            }
            if (action == Action.CONNECT && !connected || action == Action.DISCONNECT && connected) {
                if (++connectRetry > maxConnectRetry && maxConnectRetry > 0) {
                    LOGGER.error("Aborting {} connection to '{}' after {} retries", action.name(), connection.getConnectionName(), connectRetry);
                } else {
                    LOGGER.debug("Retrying {} to '{}' in {} msec. - Retry: {}", action.name(), connection.getConnectionName(), retryDelayMSec, connectRetry);
                    try {
                        wait(retryDelayMSec);
                    } catch (InterruptedException e) {
                        LOGGER.error("Delay cycle for connection thread '{} interrupted: ", connection.getConnectionName(), e.getMessage(), e);
                    }
                    continue;
                }
            }
            LOGGER.debug("Connection thread '{}' entering wait for new command", connection.getConnectionName());
            notify();
            for (;;) {
                try {
                    isInRunMethod = false;
                    wait();
                    break;
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted connection thread '{}' while waiting for command: {}", connection.getConnectionName(), e.getMessage(), e);
                }
            }
            LOGGER.debug("Connection thread '{}' exiting wait for new command", connection.getConnectionName());
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private void triggerCommand() {
        if (connectionThread == null) {
            connectionThread = new Thread(this, name);
        }

        LOGGER.debug("Executing command " + action.name() + " on connection '" + connection.getConnectionName() + "'. Thread state: " + connectionThread.getState().name());

        switch (connectionThread.getState()) {
        case TERMINATED:
            LOGGER.error("Connection thread for '" + connection.getConnectionName() + "' has terminated unespectedly. Create a new thread.");
            connectionThread = new Thread(this, name);
            // fall over
        case NEW:
            connectionThread.start();
            break;
        case WAITING:
            notify();
            break;
        case BLOCKED:
            LOGGER.debug("Connection thread for '" + connection.getConnectionName() + "' is currently blocked on a monitor");
            break;
        default:
            LOGGER.debug("Thread state not handled: " + connectionThread.getState().name());
        }
    }

    public int getMaxConnectRetry() {
        return maxConnectRetry;
    }

    public void setMaxConnectRetry(int maxConnectRetry) {
        this.maxConnectRetry = maxConnectRetry;
    }

    public long getRetryDelayMSec() {
        return retryDelayMSec;
    }

    public void setRetryDelayMSec(long retryDelayMSec) {
        this.retryDelayMSec = retryDelayMSec;
    }

    public long getConnectWaitMSec() {
        return connectWaitMSec;
    }

    public void setConnectWaitMSec(long connectWaitMSec) {
        this.connectWaitMSec = connectWaitMSec;
    }
}
