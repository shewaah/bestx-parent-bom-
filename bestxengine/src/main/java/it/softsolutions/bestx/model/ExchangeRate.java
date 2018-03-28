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

import java.math.BigDecimal;
import java.util.Date;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class ExchangeRate {
    
    private String baseCurrency;
    private String currency;
    private BigDecimal exchangeRateAmount;
    private Date settlementDate;

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getExchangeRateAmount() {
        return exchangeRateAmount;
    }

    public Date getSettlementDate() {
        return settlementDate;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setExchangeRateAmount(BigDecimal exchangeRateAmount) {
        this.exchangeRateAmount = exchangeRateAmount;
    }

    public void setSettlementDate(Date settlementDate) {
        this.settlementDate = settlementDate;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExchangeRate [baseCurrency=");
        builder.append(baseCurrency);
        builder.append(", currency=");
        builder.append(currency);
        builder.append(", exchangeRateAmount=");
        builder.append(exchangeRateAmount);
        builder.append(", settlementDate=");
        builder.append(settlementDate);
        builder.append("]");
        return builder.toString();
    }
}
