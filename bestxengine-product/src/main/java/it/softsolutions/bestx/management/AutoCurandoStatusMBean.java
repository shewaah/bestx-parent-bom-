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
package it.softsolutions.bestx.management;

/**
 * Purpose: This interface is a MBean and exposes methods to obtain the status of the Automatic Curando feature : the operator can enable or disable
 * the automatic transition of orders into the Curando Automatico state. Through this MBean the operator can know the actual status of this
 * feature and/or change it.
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 19/ott/2012
 * 
 **/
public interface AutoCurandoStatusMBean {
    
    /**
     * @return
     */
    String getAutoCurandoStatus();

    /**
     * @param autoCurandoStatus
     */
    void setAutoCurandoStatus(String autoCurandoStatus);

    /**
     * @return
     */
    int getTotalCurandoPriceRequestsNumber();
    
    /**
     * AutomaticCurandoOrdersNumber - now, min, max, avg
     * @return
     */
    int getAutomaticCurandoOrdersNumber();
    // TODO Monitoring-BX - check return type: int or String (now, min, max, avg)??
    
    /**
     * Total number of orders that have been for some time in automatic curando
     * @return
     */
    int getTotalAutoCurandoOrdersNumber();
    // TODO Monitoring-BX

}
