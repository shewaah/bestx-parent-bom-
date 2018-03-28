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

import it.softsolutions.jsscommon.Money;

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
public class Quote implements Cloneable {

    public static enum QuoteType {
        STILL, INDICATIVE, CLOSED
    }

    @SuppressWarnings("unused")
    private Long id;
    private Money accruedInterest;
    private Integer accruedDays;
    private Proposal askProposal;
    private Proposal bidProposal;
    private Date expiration;
    private Date futSettDate;
    private Instrument instrument;
    private Quote.QuoteType quoteType;
    private BigDecimal executionQtyMultiplier = BigDecimal.ONE;

    public Quote clone() throws CloneNotSupportedException {
        Quote bean = null;
        try {
            bean = (Quote) super.clone();
        } catch (CloneNotSupportedException e) {
            // it should never happen
        }
        
        bean.id = null;
        bean.setAskProposal(getAskProposal().clone());
        bean.setBidProposal(getBidProposal().clone());
        bean.setInstrument(getInstrument().clone());
        bean.setExecutionQtyMultiplier(getExecutionQtyMultiplier());
        return bean;
    }

    public void setValues(Quote quote) {
        accruedInterest = quote.getAccruedInterest();
        accruedDays = quote.getAccruedDays();
        executionQtyMultiplier = quote.getExecutionQtyMultiplier();
        askProposal = quote.getAskProposal();
        bidProposal = quote.getBidProposal();
        expiration = quote.getExpiration();
        futSettDate = quote.getFutSettDate();
        instrument = quote.getInstrument();
    }

    public void setAccruedInterest(Money accruedAmt) {
        this.accruedInterest = accruedAmt;
    }

    public Money getAccruedInterest() {
        return accruedInterest;
    }

    public void setAccruedDays(Integer accruedDays) {
        this.accruedDays = accruedDays;
    }

    public Integer getAccruedDays() {
        return accruedDays;
    }

    public void setAskProposal(Proposal askProposal) {
        this.askProposal = askProposal;
    }

    public Proposal getAskProposal() {
        return askProposal;
    }

    public void setBidProposal(Proposal bidProposal) {
        this.bidProposal = bidProposal;
    }

    public Proposal getBidProposal() {
        return bidProposal;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setFutSettDate(Date futSettDate) {
        this.futSettDate = futSettDate;
    }

    public Date getFutSettDate() {
        return futSettDate;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Quote.QuoteType getQuoteType() {
        return quoteType;
    }

    public void setQuoteType(Quote.QuoteType quoteType) {
        this.quoteType = quoteType;
    }

    public void setExecutionQtyMultiplier(BigDecimal executionQtyMultiplier) {
        this.executionQtyMultiplier = executionQtyMultiplier;

    }

    public BigDecimal getExecutionQtyMultiplier() {
        return this.executionQtyMultiplier;

    }
}
