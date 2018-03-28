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
package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;

/**
 * Interface for management of connection process of external modules.
 * 
 * @author lsgro
 * 
 */
public interface Connection {

    /**
     * Accessor method for the name of the connection
     * 
     * @return A String representing the connection name
     */
    String getConnectionName();

    /**
     * Starts the connection process. Connection and disconnection events will be notified to {@link ConnectionListener} objects
     * 
     * @throws BestXException
     */
    void connect() throws BestXException;

    /**
     * Starts disconnection process. Connection and disconnection events will be notified to {@link ConnectionListener} objects
     * 
     * @throws BestXException
     */
    void disconnect() throws BestXException;

    /**
     * State of the connection.
     * 
     * @return True if the connection is up, down if the connection id down
     */
    boolean isConnected();

    /**
     * Set a listener object that will be notified about connection and disconnection events
     * 
     * @param listener
     */
    void setConnectionListener(ConnectionListener listener);
}
