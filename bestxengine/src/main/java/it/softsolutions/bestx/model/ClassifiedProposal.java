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

import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.jsscommon.Money;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class ClassifiedProposal extends Proposal implements Cloneable {
    private Proposal.ProposalState proposalState;
    private Proposal.ProposalSubState proposalSubState = ProposalSubState.NONE;
    private String reason;
    private CommissionType commissionType;
    private Money commission;
    private Integer commissionTick;
    private Money worstPriceUsed;
    private PriceDiscoveryType priceDiscoveryType;
    private MarketMarketMaker nativeMarketMarketMaker;
    private Double bestPriceDeviationFromLimit;
	private String benchmark;
	private String auditQuoteState;

    public void setValues(ClassifiedProposal proposal) {
        super.setValues(proposal);
        setProposalState(proposal.getProposalState());
        setReason(proposal.getReason());
        setCommissionType(proposal.getCommissionType());
        setCommission(proposal.getCommission());
        setCommissionTick(proposal.getCommissionTick());
        setPriceTelQuel(proposal.getPriceTelQuel());
        setWorstPriceUsed(proposal.getWorstPriceUsed());
        setProposalSubState(proposal.getProposalSubState());
        setAuditQuoteState(proposal.getAuditQuoteState());
    }

    public void setCommission(Money commission) {
        this.commission = commission;
    }

    public Money getCommission() {
        return commission;
    }

    public void setProposalState(Proposal.ProposalState proposalState) {
        this.proposalState = proposalState;
    }

    public Proposal.ProposalState getProposalState() {
        return proposalState;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setCommissionType(CommissionType commissionType) {
        this.commissionType = commissionType;
    }

    public CommissionType getCommissionType() {
        return commissionType;
    }

    public void setCommissionTick(Integer commissionTick) {
        this.commissionTick = commissionTick;
    }

    public Integer getCommissionTick() {
        return commissionTick;
    }

    /**
     * @return the worstPriceUsed
     */
    public Money getWorstPriceUsed() {
        return worstPriceUsed;
    }

    /**
     * @param worstPriceUsed
     *            the worstPriceUsed to set
     */
    public void setWorstPriceUsed(Money worstPriceUsed) {
        this.worstPriceUsed = worstPriceUsed;
    }

    @Override
    public ClassifiedProposal clone() {
        ClassifiedProposal cloned = new ClassifiedProposal();
        cloned.setValues(this);
        return cloned;
    }
    
    public String toStringShort() {
        StringBuilder builder = new StringBuilder();
        builder.append(" | ").append(getMarket()).append('/').append(getMarketMarketMaker());
        builder.append(" | ").append(getPrice() != null ? getPrice().getAmount() : "").append(" x ").append(getQty());
        builder.append(" | ").append(getSide());
        builder.append(" | ").append(getType());
        builder.append(" | ").append(getTimestampstr());
        builder.append(" | ").append(getFutSettDate());
        builder.append(" | ").append(proposalState != null ? proposalState : "null state");
        builder.append(" | ").append(proposalSubState != null ? proposalSubState : "null subState");
        if (reason != null && !reason.isEmpty()) {
            builder.append(" / ").append(reason);
        }
        return builder.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClassifiedProposal extends ").append(super.toString());
        builder.append(" plus [proposalState=");
        builder.append(proposalState);
        builder.append(", proposalSubState=");
        builder.append(proposalSubState);
        builder.append(", reason=");
        builder.append(reason);
        builder.append(", commissionType=");
        builder.append(commissionType);
        builder.append(", commission=");
        builder.append(commission);
        builder.append(", commissionTick=");
        builder.append(commissionTick);
        builder.append(", worstPriceUsed=");
        builder.append(worstPriceUsed);
        builder.append(", priceDiscoveryType=");
        builder.append(priceDiscoveryType);
        builder.append(", nativeMarketMarketMaker=");
        builder.append(nativeMarketMarketMaker);
        builder.append("]");
        return builder.toString();
    } 
    
    public PriceDiscoveryType getPriceDiscoveryType() {
        return priceDiscoveryType;
    }

    public void setPriceDiscoveryType(PriceDiscoveryType priceDiscoveryType) {
        this.priceDiscoveryType = priceDiscoveryType;
    }

    public MarketMarketMaker getNativeMarketMarketMaker() {
        return nativeMarketMarketMaker;
    }

    public void setNativeMarketMarketMaker(MarketMarketMaker nativeMarketMarketMaker) {
        this.nativeMarketMarketMaker = nativeMarketMarketMaker;
    }

    /**
     * @return the proposalSubState
     */
    public Proposal.ProposalSubState getProposalSubState() {
        return proposalSubState;
    }

    /**
     * @param proposalSubState the proposalSubState to set
     */
    public void setProposalSubState(Proposal.ProposalSubState proposalSubState) {
        this.proposalSubState = proposalSubState;
    }

    /**
     * @return the bestPriceDeviationFromLimit
     */
    public Double getBestPriceDeviationFromLimit() {
        return bestPriceDeviationFromLimit;
    }

    /**
     * @param bestPriceDeviationFromLimit the bestPriceDeviationFromLimit to set
     */
    public void setBestPriceDeviationFromLimit(Double bestPriceDeviationFromLimit) {
        this.bestPriceDeviationFromLimit = bestPriceDeviationFromLimit;
    }

	public String getBenchmark() {
		return benchmark;
	}

	public void setBenchmark(String benchmark) {
		this.benchmark = benchmark;
	}

	public String getAuditQuoteState() {
		return auditQuoteState;
	}

	public void setAuditQuoteState(String auditQuoteState) {
		this.auditQuoteState = auditQuoteState;
	}

	
	
}
