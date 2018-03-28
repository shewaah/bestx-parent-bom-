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
package it.softsolutions.bestx;

/** isPasswordEncrypted=false
*
* Purpose: interface for the management of the state of the system, if it can manage orders and/or rfqs and why.
*
* Project Name : bestxengine-common
* First created by: ruggero.rizzo
* Creation date: 09/ott/2012
*
**/
public interface SystemStateSelector {
   /**
    * Check if RFQs are allowed
    * @return true or false
    */
    boolean isRfqEnabled();
    /**
     * Check if orders are allowed
     * @return true or false
     */
    boolean isOrderEnabled();
    /**
     * Set the enabling/disabling of the RFQs
     * @param rfqEnabled : true if enabled false otherwise
     */
    void setRfqEnabled(boolean rfqEnabled);
    /**
     * Set the enabling/disabling of the orders
     * @param rfqEnabled : true if enabled false otherwise
     */
    void setOrderEnabled(boolean orderEnabled);
    /**
     * Set the state description
     * @param stateDescription : a description of the actual state
     */
    void setStateDescription(String stateDescription);
    /**
     * Get the state description
     * @return the description
     */
    String getStateDescription();
}
