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
public class MarketExecutionReport extends ExecutionReport {
	/**
	 * 
	 * @author anna.cochetti
	 * Reject Reason tells what type of rejection has received the MarketExecutioReport when rejected
	 * TECHNICAL_FAILURE: a technical failure on a regulated market - technical means that it is unrecoverable and related to the order itself
	 * BUSINESS_REJECTED: a rejection on a basis of a Business characteristics of the order - such as invalid settlement date - on a non-regulated market
	 * TRADER_REJECTED: a rejection received after negotiation (RFQ or RFCQ) with a Dealer on a non-regulated market
	 * SYNCHRONIZATION_PROBLEM: some possibly transient protocol issue with the market (regultaed or not)
	 * AUTO_REJECTED: rejection with no human intervention. Default value for all rejections
	 */
	public enum RejectReason {TECHNICAL_FAILURE, BUSINESS_REJECTED, TRADER_REJECTED, SYNCHRONIZATION_PROBLEM, AUTO_REJECTED};

	private RejectReason reason;
	private String orderTrader;
	private String securityIdSource;
	private char ordStatus;
	private String counterpartLEI;
	
	public char getOrdStatus() {
		return ordStatus;
	}
	public void setOrdStatus(char ordStatus) {
		this.ordStatus = ordStatus;
	}
	private String marketOrderId;
	
	public String getMarketOrderId() {
		return marketOrderId;
	}
	public void setMarketOrderId(String marketOrderId) {
		this.marketOrderId = marketOrderId;
	}
	private MarketMaker marketMaker;

	public MarketMaker getMarketMaker() {
		return marketMaker;
	}
	public void setMarketMaker(MarketMaker marketMaker) {
		this.marketMaker = marketMaker;
	}
	public RejectReason getReason() {
		return reason;
	}
	public void setReason(RejectReason reason){
		this.reason = reason;
	}
	public String getOrderTrader() {
		return orderTrader;
	}
	public void setOrderTrader(String orderTrader) {
		this.orderTrader = orderTrader;
	}
	public String getSecurityIdSource() {
		return securityIdSource;
	}
	public void setSecurityIdSource(String securityIdSource) {
		this.securityIdSource = securityIdSource;
	}
   public String getCounterpartLEI() {
      return counterpartLEI;
   }
   
   public void setCounterpartLEI(String counterpartLEI) {
      this.counterpartLEI = counterpartLEI;
   }
   
   @Override
	public String toString(){
		return super.toString() +
				" - Security ID source: " + getSecurityIdSource() +
				" - OrdStatus : " + getOrdStatus() +
				" - Market Maker : " + (getMarketMaker() != null? getMarketMaker().getCode() : "null");
	}
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof MarketExecutionReport)) return false;
		MarketExecutionReport mme = (MarketExecutionReport) obj;
		return(mme.getId().equals(this.getId()) &&
				mme.getMarket().equals(this.getMarket()));
	}
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
}