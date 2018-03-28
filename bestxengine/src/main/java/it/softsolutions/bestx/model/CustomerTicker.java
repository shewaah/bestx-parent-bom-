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

import java.io.Serializable;

/**
 * 
 * Purpose: CustomerTickerTable hibernate row mapper
 * 
 * Project Name : bestxengine First created by: ruggero.rizzo Creation date: 13/nov/2012
 * 
 **/
public class CustomerTicker implements Serializable {

    private static final long serialVersionUID = 5981115755581476749L;

    private String clientCode;
    private String ticker;
    private Double spread;
    private Double tolerance;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Double getSpread() {
        return spread;
    }

    public void setSpread(Double spread) {
        this.spread = spread;
    }

    public Double getTolerance() {
        return tolerance;
    }

    public void setTolerance(Double tolerance) {
        this.tolerance = tolerance;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CustomerTicker [clientCode=");
        builder.append(clientCode);
        builder.append(", ticker=");
        builder.append(ticker);
        builder.append(", spread=");
        builder.append(spread);
        builder.append(", tolerance=");
        builder.append(tolerance);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CustomerTicker)) {
            return false;
        }
        if(((CustomerTicker) o).clientCode == null && clientCode == null)
        	 return (((CustomerTicker) o).getTicker() == null && ticker == null);
        return ((CustomerTicker) o).clientCode.equalsIgnoreCase(clientCode) && ((CustomerTicker) o).getTicker().equalsIgnoreCase(ticker);
    }

    @Override
    public int hashCode() {
        return clientCode.hashCode() + ticker.hashCode();
    }
}
