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
import java.util.List;
import java.util.Set;

import it.softsolutions.bestx.model.Market.MarketCode;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public class MarketMaker implements Serializable {

    private String code;
    private String name;
    private int classId;
    private int rank;
    private boolean enabled;
    private String sinfoCode;
    private Set<MarketMarketMaker> marketMarketMakers;
    private Set<MarketMakerAccount> accountCodes;

    public Set<MarketMakerAccount> getAccountCodes() {
        return accountCodes;
    }

    public void setAccountCodes(Set<MarketMakerAccount> bloombergAccountCodes) {
        this.accountCodes = bloombergAccountCodes;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        
        if (!(o instanceof MarketMaker)) {
            return false;
        }
        
        return ((MarketMaker) o).getCode().equals(code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }

    public Set<MarketMarketMaker> getMarketMarketMakers() {
        return marketMarketMakers;
    }

    public void setMarketMarketMakers(Set<MarketMarketMaker> marketMarketMakers) {
        this.marketMarketMakers = marketMarketMakers;
    }

    public List<MarketMarketMaker> getMarketMarketMakerForMarket(MarketCode marketCode) {
        ArrayList<MarketMarketMaker> marketMakers = new ArrayList<MarketMarketMaker>();
        for (MarketMarketMaker mmm : marketMarketMakers) {
            if (mmm.getMarket().getMarketCode().compareTo(marketCode) == 0) {
                marketMakers.add(mmm);
            }
        }
        return marketMakers;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSinfoCode() {
        return sinfoCode;
    }

    public void setSinfoCode(String sinfoCode) {
        this.sinfoCode = sinfoCode;
    }
}
