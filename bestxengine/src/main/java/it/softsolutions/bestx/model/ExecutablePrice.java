
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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

import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.Proposal.PriceType;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Proposal.ProposalSubState;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.jsscommon.Money;

/**
 * Purpose: this class is mainly maintaining Quotes received by counterparts reached with an RFCQ in an execution Attempt   
 * Project Name : bestxengine First created by: anna.cochetti Creation date: 19 feb 2017
 */
public class ExecutablePrice implements Comparable<ExecutablePrice> {
	
//	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutablePrice.class);

    @SuppressWarnings("unused")
    private Long id;

    public MarketMarketMaker getMarketMarketMaker() {
		return classifiedProposal.getMarketMarketMaker();
	}

	private String originatorID;
	private String auditQuoteState;
	private Integer rank;
	private long AttemptId;
	
	public MarketMaker getMarketMaker() {
		if(getMarketMarketMaker() != null)
			return getMarketMarketMaker().getMarketMaker();
		else return null;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public ExecutablePrice(ClassifiedProposal proposal) {
		if(proposal == null) return;
		this.setClassifiedProposal(proposal);
	}

	public ExecutablePrice() {
		classifiedProposal = new ClassifiedProposal();
	}

	@Override
	public int hashCode() {
		return classifiedProposal.hashCode();
	}

	public void setValues(ClassifiedProposal proposal) {
		proposal.setValues(proposal);
	}

	public void setCommission(Money commission) {
		classifiedProposal.setCommission(commission);
	}

	public Money getCommission() {
		return classifiedProposal.getCommission();
	}

	public void setProposalState(ProposalState proposalState) {
		classifiedProposal.setProposalState(proposalState);
	}

	public void setReason(String reason) {
		classifiedProposal.setReason(reason);
	}

	public void setCommissionType(CommissionType commissionType) {
		classifiedProposal.setCommissionType(commissionType);
	}

	public CommissionType getCommissionType() {
		return classifiedProposal.getCommissionType();
	}

	public void setCommissionTick(Integer commissionTick) {
		classifiedProposal.setCommissionTick(commissionTick);
	}

	public Integer getCommissionTick() {
		return classifiedProposal.getCommissionTick();
	}

	public Money getWorstPriceUsed() {
		return classifiedProposal.getWorstPriceUsed();
	}

	public void setWorstPriceUsed(Money worstPriceUsed) {
		classifiedProposal.setWorstPriceUsed(worstPriceUsed);
	}

	public boolean isInternal() {
		return classifiedProposal.isInternal();
	}

	public String toStringShort() {
		return classifiedProposal.toStringShort();
	}

	public void setInternal(boolean isInternal) {
		classifiedProposal.setInternal(isInternal);
	}

	public Money getAccruedInterest() {
		return classifiedProposal.getAccruedInterest();
	}

	public void setAccruedInterest(Money accruedInterest) {
		classifiedProposal.setAccruedInterest(accruedInterest);
	}

	public Integer getAccruedDays() {
		return classifiedProposal.getAccruedDays();
	}

	public void setAccruedDays(Integer accruedDays) {
		classifiedProposal.setAccruedDays(accruedDays);
	}

	public PriceDiscoveryType getPriceDiscoveryType() {
		return classifiedProposal.getPriceDiscoveryType();
	}

	public void setPriceDiscoveryType(PriceDiscoveryType priceDiscoveryType) {
		classifiedProposal.setPriceDiscoveryType(priceDiscoveryType);
	}

	public MarketMarketMaker getNativeMarketMarketMaker() {
		return classifiedProposal.getNativeMarketMarketMaker();
	}

	public void setNativeMarketMarketMaker(MarketMarketMaker nativeMarketMarketMaker) {
		classifiedProposal.setNativeMarketMarketMaker(nativeMarketMarketMaker);
	}

	public void setProposalSubState(ProposalSubState proposalSubState) {
		classifiedProposal.setProposalSubState(proposalSubState);
	}

	public Double getBestPriceDeviationFromLimit() {
		return classifiedProposal.getBestPriceDeviationFromLimit();
	}

	public void setCustomerAdditionalExpenses(Money customerAdditionalExpenses) {
		classifiedProposal.setCustomerAdditionalExpenses(customerAdditionalExpenses);
	}

	public void setBestPriceDeviationFromLimit(Double bestPriceDeviationFromLimit) {
		classifiedProposal.setBestPriceDeviationFromLimit(bestPriceDeviationFromLimit);
	}

	public void setExpiration(Date expiration) {
		classifiedProposal.setExpiration(expiration);
	}

	public String getBenchmark() {
		return classifiedProposal.getBenchmark();
	}

	public void setBenchmark(String benchmark) {
		classifiedProposal.setBenchmark(benchmark);
	}

	public void setFutSettDate(Date futSettDate) {
		classifiedProposal.setFutSettDate(futSettDate);
	}

	public void setMarket(Market market) {
		classifiedProposal.setMarket(market);
	}

	public void setPrice(Money price) {
		classifiedProposal.setPrice(price);
	}

	public void setQty(BigDecimal qty) {
		classifiedProposal.setQty(qty);
	}

	public void setSide(ProposalSide side) {
		classifiedProposal.setSide(side);
	}

	public void setType(ProposalType type) {
		classifiedProposal.setType(type);
	}

	public void setVenue(Venue venue) {
		classifiedProposal.setVenue(venue);
	}

	public Money getCustomerAdditionalExpenses() {
		return classifiedProposal.getCustomerAdditionalExpenses();
	}

	public Market getMarket() {
		return classifiedProposal.getMarket();
	}

	public Venue getVenue() {
		return classifiedProposal.getVenue();
	}

	public void setTimestamp(Date timestamp) {
		classifiedProposal.setTimestamp(timestamp);
	}

	public void setTimestampstr(String timestampstr) {
		classifiedProposal.setTimestampstr(timestampstr);
	}

	public void setTrader(Trader trader) {
		classifiedProposal.setTrader(trader);
	}

	public Trader getTrader() {
		return classifiedProposal.getTrader();
	}

	public void setOrderPrice(Money orderPrice) {
		classifiedProposal.setOrderPrice(orderPrice);
	}

	public boolean isNonStandardSettlementDateAllowed() {
		return classifiedProposal.isNonStandardSettlementDateAllowed();
	}

	public void setNonStandardSettlementDateAllowed(boolean nonStandardSettlementDateAllowed) {
		classifiedProposal.setNonStandardSettlementDateAllowed(nonStandardSettlementDateAllowed);
	}

	public void setMarketMarketMaker(MarketMarketMaker marketMarketMaker) {
		classifiedProposal.setMarketMarketMaker(marketMarketMaker);
	}

	public String getSenderQuoteId() {
		return classifiedProposal.getSenderQuoteId();
	}

	public void setSenderQuoteId(String senderQuoteId) {
		classifiedProposal.setSenderQuoteId(senderQuoteId);
	}

	public String getQuoteReqId() {
		return classifiedProposal.getQuoteReqId();
	}

	public void setQuoteReqId(String quoteReqId) {
		classifiedProposal.setQuoteReqId(quoteReqId);
	}

	public BigDecimal getExecutionQtyMultiplier() {
		return classifiedProposal.getExecutionQtyMultiplier();
	}

	public void setExecutionQtyMultiplier(BigDecimal executionQtyMultiplier) {
		classifiedProposal.setExecutionQtyMultiplier(executionQtyMultiplier);
	}

	public BigDecimal getSpread() {
		return classifiedProposal.getSpread();
	}

	public void setSpread(BigDecimal spread) {
		classifiedProposal.setSpread(spread);
	}

	public void setPriceTelQuel(Money priceTelQuel) {
		classifiedProposal.setPriceTelQuel(priceTelQuel);
	}

	public Money getPriceTelQuel() {
		return classifiedProposal.getPriceTelQuel();
	}

	public void setOriginalPrice(BigDecimal originalPrice) {
		classifiedProposal.setOriginalPrice(originalPrice);
	}

	public void setYield(BigDecimal yield) {
		classifiedProposal.setYield(yield);
	}

	public void setPriceType(PriceType priceType) {
		classifiedProposal.setPriceType(priceType);
	}

	private ClassifiedProposal classifiedProposal;
	
	public ClassifiedProposal getClassifiedProposal() {
		return classifiedProposal;
	}

	public void setClassifiedProposal(ClassifiedProposal proposal) {
		this.classifiedProposal = proposal;
	}

	public String getOriginatorID() {
		return originatorID;
	}

	public void setOriginatorID(String originatorID) {
		this.originatorID = originatorID;
	}

	public String getAuditQuoteState() {
		return auditQuoteState;
	}

	public void setAuditQuoteState(String auditQuoteState) {
		this.auditQuoteState = auditQuoteState;
	}

	public Date getTimestamp() {
		return classifiedProposal.getTimestamp();
	}

	public ProposalState getProposalState() {
		return classifiedProposal.getProposalState();
	}

	public String getReason() {
		return classifiedProposal.getReason();
	}

	public ProposalSubState getProposalSubState() {
		return classifiedProposal.getProposalSubState();
	}

	public Date getExpiration() {
		return classifiedProposal.getExpiration();
	}

	public Date getFutSettDate() {
		return classifiedProposal.getFutSettDate();
	}

	public Money getPrice() {
		return classifiedProposal.getPrice();
	}

	public BigDecimal getQty() {
		return classifiedProposal.getQty();
	}

	public ProposalSide getSide() {
		return classifiedProposal.getSide();
	}

	public ProposalType getType() {
		return classifiedProposal.getType();
	}

	public String getTimestampstr() {
		return classifiedProposal.getTimestampstr();
	}

	public Money getOrderPrice() {
		return classifiedProposal.getOrderPrice();
	}

	public BigDecimal getOriginalPrice() {
		return classifiedProposal.getOriginalPrice();
	}

	public BigDecimal getYield() {
		return classifiedProposal.getYield();
	}

	public PriceType getPriceType() {
		return classifiedProposal.getPriceType();
	}

	@Override
	public int compareTo(ExecutablePrice o) {
		return this.getRank().compareTo(((ExecutablePrice)o).getRank());
	}

	public long getAttemptId() {
		return AttemptId;
	}

	public void setAttemptId(long attemptId) {
		AttemptId = attemptId;
	}

}