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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 
 * Purpose: class mapping the rows of the database table MarketTable
 * 
 * Project Name : bestxengine
 * First created by: ruggero.rizzo 
 * Creation date: 26/ott/2012
 * 
 **/
public class Market implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    public static enum MarketCode {
        INTERNALIZZAZIONE, BLOOMBERG, TSOX/* technical enumerated for execution on TSOX*/,  //AMC TSOX dealer codes are not the same in BPipe and trading systems
        MOT, TLX, MATCHING, TDS, HIMTF, BV, TW, XBRIDGE, MTSPRIME, MARKETAXESS 
    }

    public static enum SubMarketCode {
        MEM, MOT, ETX, TLX, HIMTF, XMOT, CRD
    }

    public static String REGULATED = "R";
    public static String NOT_REGULATED = "NR";

    private Long marketId;
    private String name;
    private Integer ranking;
    private Boolean enabledAutoexecution;
    private Boolean secure;
    private MarketCode marketCode;
    private SubMarketCode subMarketCode;
    private boolean disabled;
    private boolean reusePrices;
    private String micCode;
    private String marketBehaviour;
    private final List<String> marketBehavioursList = new ArrayList<String>();

    public Long getMarketId() {
        return marketId;
    }

    public void setMarketId(Long marketId) {
        this.marketId = marketId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public Boolean getEnabledAutoexecution() {
        return enabledAutoexecution;
    }

    public void setEnabledAutoexecution(Boolean enabledAutoexecution) {
        this.enabledAutoexecution = enabledAutoexecution;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public MarketCode getMarketCode() {
        return marketCode;
    }

    public void setMarketCode(MarketCode marketCode) {
        this.marketCode = marketCode;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(marketCode);
        sb.append('/').append(subMarketCode != null ? subMarketCode : '-');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Market)) {
            return false;
        }
        return ((Market) o).getMarketCode() == getMarketCode() && ((Market) o).getSubMarketCode() == getSubMarketCode();
    }

    @Override
    public int hashCode() {
        return getMarketCode().hashCode();
    }

    public SubMarketCode getSubMarketCode() {
        return subMarketCode;
    }

    public void setSubMarketCode(SubMarketCode subMarketCode) {
        this.subMarketCode = subMarketCode;
    }

    /**
     * @return the reusePrices
     */
    public boolean isReusePrices() {
        return reusePrices;
    }

    /**
     * @param reusePrices
     *            the reusePrices to set
     */
    public void setReusePrices(boolean reusePrices) {
        this.reusePrices = reusePrices;
    }

    public String getMicCode() {
        return micCode;
    }

    public void setMicCode(String micCode) {
        this.micCode = micCode;
    }

    public String getMarketBehaviour() {
        return marketBehaviour;
    }

    /*
     * 25-03-2009 Ruggero In the database we've the MarketBehaviour column which contains one of the following strings : - R - NR - R,NR In
     * order to take advantage of the List's features, when loading data from the database, we save those strings in a List. It will be
     * easier to check if the market has a regulated, not regulated behaviour through the java.Util.List.contains() method.
     */
    public void setMarketBehaviour(String marketBehaviour) {
        String[] splits = marketBehaviour.split(",");
        for (int count = 0; count < splits.length; count++) {
            marketBehavioursList.add(splits[count]);
        }
        this.marketBehaviour = marketBehaviour;
    }

    public boolean hasRegulatedBehaviour() {
        return (marketBehavioursList.contains(REGULATED));
    }

    public boolean hasNotRegulatedBehaviour() {
        return (marketBehavioursList.contains(NOT_REGULATED));
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }  
}
