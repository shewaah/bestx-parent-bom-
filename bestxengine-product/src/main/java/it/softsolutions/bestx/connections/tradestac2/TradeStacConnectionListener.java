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
package it.softsolutions.bestx.connections.tradestac2;

import it.softsolutions.tradestac2.api.ConnectionStatus;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 28/set/2012 
 * 
 **/
public interface TradeStacConnectionListener {
    
    /**
     * Notifies the changes on the client connection (from BestX to TradeStac server)
     * 
     * @param connectionName the name of the Market connection
     * @param connectionStatus the status of the client connection 
     */
    void onClientConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus);
    

    /**
     * Notifies the changes on the Market connection (from TradeStac to Bloomberg server)
     * 
     * @param connectionName the name of the Market connection
     * @param connectionStatus the status of the Market connection
     */
    void onMarketConnectionStatusChange(String connectionName, ConnectionStatus connectionStatus);

}
