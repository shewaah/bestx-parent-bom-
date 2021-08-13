
/*
 * Copyright 1997-2016 SoftSolutions! srl 
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

import it.softsolutions.bestx.model.Rfq.OrderSide;


/**
 *
 * Purpose: this class is aimed at modeling an incoming request from the customer for a single simple price discovery
 * BestX:FI-A is requested to:
 * create an attempt 0 with no validation except the formal ones
 * create an attempt with a price discovery, creation of a consolidated book
 * reply to the MarketDataReq with a MarketDataResp message
 * both messages are probably not sent via FIX
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 20 lug 2016
 * 
 **/
@Deprecated
public class MarketDataReq {
	
    private OrderSide side = null;
    public OrderSide getSide() {
		return side;
	}
	public void setSide(OrderSide side) {
		this.side = side;
	}
	public Date getFutSettDate() {
		return futSettDate;
	}
	public void setFutSettDate(Date futSettDate) {
		this.futSettDate = futSettDate;
	}
	public Integer getSettlementType() {
		return settlementType;
	}
	public void setSettlementType(Integer settlementType) {
		this.settlementType = settlementType;
	}
	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	public Instrument getInstrument() {
		return instrument;
	}
	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}
	public Date getTransactTime() {
		return transactTime;
	}
	public void setTransactTime(Date transactTime) {
		this.transactTime = transactTime;
	}
	public BigDecimal getQty() {
		return qty;
	}
	public void setQty(BigDecimal qty) {
		this.qty = qty;
	}
	private Date futSettDate = null;
    private Integer settlementType = null;
    private Customer customer = null;
    private Instrument instrument = null;
    private Date transactTime = null;
    private BigDecimal qty = null;

    /**
     * Initialize values from a source MarketDataReq
     * @param source
     */
    public void setValues(MarketDataReq source){
    	setSide(source.getSide());
    	setTransactTime(source.getTransactTime());
    	setCustomer(source.getCustomer());
    	setFutSettDate(source.getFutSettDate());
    	setInstrument(source.getInstrument());
    	setQty(source.getQty());
    }
}
