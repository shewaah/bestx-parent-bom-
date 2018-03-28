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

/**
 * 
 * Purpose: this interface for receiving notification about {@link Connection} events.
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 15/giu/2012
 * 
 **/
public interface ConnectionListener {

    /**
     * Callback that notifies about connection status set to "connected", or "up"
     * 
     * @param source
     *            The source {@link Connection} object
     */
    void onConnection(Connection source);

    /**
     * Callback that notifies about connection status set to "disconnected", or "down"
     * 
     * @param source
     *            The source {@link Connection} object
     * @param reason
     *            The optional reason provided for disconnection
     */
    void onDisconnection(Connection source, String reason);
}