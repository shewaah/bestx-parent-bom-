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
package it.softsolutions.bestx.model;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Trader {
    
    private String traderId;
    private String traderName;

    public String getTraderName() {
        return traderName;
    }

    public void setTraderName(String traderName) {
        this.traderName = traderName;
    }

    public void setTraderId(String id) {
        this.traderId = id;
    }

    public String getTraderId() {
        return traderId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Trader)) {
            return false;
        } else {
            return ((Trader) o).getTraderId().equals(this.getTraderId());
        }
    }

    @Override
    public int hashCode() {
        return traderId.hashCode();
    }

    @Override
    public String toString() {
        return traderId;
    }
}
