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
 * Creation date: 28/nov/2012 
 * 
 **/
public class InstrumentParam {

    public static enum Type {
        PortfolioID, SystematicInternalization, DirtyPrice
    }

    private Long id = 0L;
    private String isin;
    private InstrumentParam.Type type;
    private String key;
    private String value;

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public InstrumentParam.Type getType() {
        return type;
    }

    public void setType(InstrumentParam.Type type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("InstrumentParam [id=");
        builder.append(id);
        builder.append(", isin=");
        builder.append(isin);
        builder.append(", type=");
        builder.append(type);
        builder.append(", key=");
        builder.append(key);
        builder.append(", value=");
        builder.append(value);
        builder.append("]");
        return builder.toString();
    }

}
