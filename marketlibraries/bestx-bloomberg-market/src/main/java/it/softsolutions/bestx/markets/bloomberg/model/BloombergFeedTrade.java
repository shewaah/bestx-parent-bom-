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
package it.softsolutions.bestx.markets.bloomberg.model;

import it.softsolutions.bestx.model.MarketExecutionReport;

import java.math.BigDecimal;
import java.util.Date;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestx-bloomberg-market 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class BloombergFeedTrade {
    private String execId;
    private String orderId;
    private String clOrdId;
    private String executingFirm;
    private String bloombergBrokerCode;
    private String orderOriginationTrader;
    private String uuId;
    private Date settlDate;
    private String securityId;
    private String side;
    private BigDecimal orderQty;
    private BigDecimal price;
    private String currency;
    private BigDecimal cumQty;
    private BigDecimal avgPx;
    private Date tradeDate;
    private Date transactTime;
    private boolean matched;
    private String bxOrderId;
    private BigDecimal accruedInterestAmt;
    private String settlCurrency;
    private BigDecimal settlCurrAccruedInterestAmt;
    private BigDecimal settlCurrNetMoney;
    private String notes;

    public MarketExecutionReport toMarketExecutionReport() {
        MarketExecutionReport res = new MarketExecutionReport();

        // res.setId(clOrdId);
        res.setMarketOrderID(orderId);

        return res;
    }

    /**
     * @return the execId
     */
    public String getExecId() {
        return execId;
    }

    /**
     * @param execId
     *            the execId to set
     */
    public void setExecId(String execId) {
        this.execId = execId;
    }

    /**
     * @return the orderId
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * @param orderId
     *            the orderId to set
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * @return the clOrdId
     */
    public String getClOrdId() {
        return clOrdId;
    }

    /**
     * @param clOrdId
     *            the clOrdId to set
     */
    public void setClOrdId(String clOrdId) {
        this.clOrdId = clOrdId;
    }

    /**
     * @return the executingFirm
     */
    public String getExecutingFirm() {
        return executingFirm;
    }

    /**
     * @param executingFirm
     *            the executingFirm to set
     */
    public void setExecutingFirm(String executingFirm) {
        this.executingFirm = executingFirm;
    }

    /**
     * @return the bloombergBrokerCode
     */
    public String getBloombergBrokerCode() {
        return bloombergBrokerCode;
    }

    /**
     * @param bloombergBrokerCode
     *            the bloombergBrokerCode to set
     */
    public void setBloombergBrokerCode(String bloombergBrokerCode) {
        this.bloombergBrokerCode = bloombergBrokerCode;
    }

    /**
     * @return the orderOriginationTrader
     */
    public String getOrderOriginationTrader() {
        return orderOriginationTrader;
    }

    /**
     * @param orderOriginationTrader
     *            the orderOriginationTrader to set
     */
    public void setOrderOriginationTrader(String orderOriginationTrader) {
        this.orderOriginationTrader = orderOriginationTrader;
    }

    /**
     * @return the uuId
     */
    public String getUuId() {
        return uuId;
    }

    /**
     * @param uuId
     *            the uuId to set
     */
    public void setUuId(String uuId) {
        this.uuId = uuId;
    }

    /**
     * @return the settlDate
     */
    public Date getSettlDate() {
        return settlDate;
    }

    /**
     * @param settlDate
     *            the settlDate to set
     */
    public void setSettlDate(Date settlDate) {
        this.settlDate = settlDate;
    }

    /**
     * @return the securityId
     */
    public String getSecurityId() {
        return securityId;
    }

    /**
     * @param securityId
     *            the securityId to set
     */
    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    /**
     * @return the side
     */
    public String getSide() {
        return side;
    }

    /**
     * @param side
     *            the side to set
     */
    public void setSide(String side) {
        this.side = side;
    }

    /**
     * @return the orderQty
     */
    public BigDecimal getOrderQty() {
        return orderQty;
    }

    /**
     * @param orderQty
     *            the orderQty to set
     */
    public void setOrderQty(BigDecimal orderQty) {
        this.orderQty = orderQty;
    }

    /**
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * @param price
     *            the price to set
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * @return the currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * @param currency
     *            the currency to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * @return the cumQty
     */
    public BigDecimal getCumQty() {
        return cumQty;
    }

    /**
     * @param cumQty
     *            the cumQty to set
     */
    public void setCumQty(BigDecimal cumQty) {
        this.cumQty = cumQty;
    }

    /**
     * @return the avgPx
     */
    public BigDecimal getAvgPx() {
        return avgPx;
    }

    /**
     * @param avgPx
     *            the avgPx to set
     */
    public void setAvgPx(BigDecimal avgPx) {
        this.avgPx = avgPx;
    }

    /**
     * @return the tradeDate
     */
    public Date getTradeDate() {
        return tradeDate;
    }

    /**
     * @param tradeDate
     *            the tradeDate to set
     */
    public void setTradeDate(Date tradeDate) {
        this.tradeDate = tradeDate;
    }

    /**
     * @return the transactTime
     */
    public Date getTransactTime() {
        return transactTime;
    }

    /**
     * @param transactTime
     *            the transactTime to set
     */
    public void setTransactTime(Date transactTime) {
        this.transactTime = transactTime;
    }

    /**
     * @return the matched
     */
    public boolean isMatched() {
        return matched;
    }

    /**
     * @param matched
     *            the matched to set
     */
    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    /**
     * @return the bxOrderId
     */
    public String getBxOrderId() {
        return bxOrderId;
    }

    /**
     * @param bxOrderId
     *            the bxOrderId to set
     */
    public void setBxOrderId(String bxOrderId) {
        this.bxOrderId = bxOrderId;
    }

    /**
     * @return the accruedInterestAmt
     */
    public BigDecimal getAccruedInterestAmt() {
        return accruedInterestAmt;
    }

    /**
     * @param accruedInterestAmt
     *            the accruedInterestAmt to set
     */
    public void setAccruedInterestAmt(BigDecimal accruedInterestAmt) {
        this.accruedInterestAmt = accruedInterestAmt;
    }

    /**
     * @return the settlCurrency
     */
    public String getSettlCurrency() {
        return settlCurrency;
    }

    /**
     * @param settlCurrency
     *            the settlCurrency to set
     */
    public void setSettlCurrency(String settlCurrency) {
        this.settlCurrency = settlCurrency;
    }

    /**
     * @return the settlCurrAccruedInterestAmt
     */
    public BigDecimal getSettlCurrAccruedInterestAmt() {
        return settlCurrAccruedInterestAmt;
    }

    /**
     * @param settlCurrAccruedInterestAmt
     *            the settlCurrAccruedInterestAmt to set
     */
    public void setSettlCurrAccruedInterestAmt(BigDecimal settlCurrAccruedInterestAmt) {
        this.settlCurrAccruedInterestAmt = settlCurrAccruedInterestAmt;
    }

    /**
     * @return the settlCurrNetMoney
     */
    public BigDecimal getSettlCurrNetMoney() {
        return settlCurrNetMoney;
    }

    /**
     * @param settlCurrNetMoney
     *            the settlCurrNetMoney to set
     */
    public void setSettlCurrNetMoney(BigDecimal settlCurrNetMoney) {
        this.settlCurrNetMoney = settlCurrNetMoney;
    }

    /**
     * @return the notes
     */
    public String getNotes() {
        return notes;
    }

    /**
     * @param notes
     *            the notes to set
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BloombergFeedTrade [execId=");
        builder.append(execId);
        builder.append(", orderId=");
        builder.append(orderId);
        builder.append(", clOrdId=");
        builder.append(clOrdId);
        builder.append(", executingFirm=");
        builder.append(executingFirm);
        builder.append(", bloombergBrokerCode=");
        builder.append(bloombergBrokerCode);
        builder.append(", orderOriginationTrader=");
        builder.append(orderOriginationTrader);
        builder.append(", uuId=");
        builder.append(uuId);
        builder.append(", settlDate=");
        builder.append(settlDate);
        builder.append(", securityId=");
        builder.append(securityId);
        builder.append(", side=");
        builder.append(side);
        builder.append(", orderQty=");
        builder.append(orderQty);
        builder.append(", price=");
        builder.append(price);
        builder.append(", currency=");
        builder.append(currency);
        builder.append(", cumQty=");
        builder.append(cumQty);
        builder.append(", avgPx=");
        builder.append(avgPx);
        builder.append(", tradeDate=");
        builder.append(tradeDate);
        builder.append(", transactTime=");
        builder.append(transactTime);
        builder.append(", matched=");
        builder.append(matched);
        builder.append(", bxOrderId=");
        builder.append(bxOrderId);
        builder.append(", accruedInterestAmt=");
        builder.append(accruedInterestAmt);
        builder.append(", settlCurrency=");
        builder.append(settlCurrency);
        builder.append(", settlCurrAccruedInterestAmt=");
        builder.append(settlCurrAccruedInterestAmt);
        builder.append(", settlCurrNetMoney=");
        builder.append(settlCurrNetMoney);
        builder.append(", notes=");
        builder.append(notes);
        builder.append("]");
        return builder.toString();
    }
}