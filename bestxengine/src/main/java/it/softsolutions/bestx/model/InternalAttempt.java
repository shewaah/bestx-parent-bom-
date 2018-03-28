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

import java.util.List;



/**  
*
* Purpose: this class is mainly for retain history of internalization attempts
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class InternalAttempt {
	@SuppressWarnings("unused")
    private Long id;

    /**
     * The active flag is used to nullify the internal attempt without deleting it. It has to be retained for audit pourposes.
     */
    private boolean active = false;
    private List<MarketExecutionReport> marketExecutionReports;
	public List<MarketExecutionReport> getMarketExecutionReports() {
		return marketExecutionReports;
	}

	public void setMarketExecutionReports(
			List<MarketExecutionReport> marketExecutionReports) {
		this.marketExecutionReports = marketExecutionReports;
	}

	private ClassifiedProposal executionProposal;
    private Proposal counterOffer;
    private MarketOrder marketOrder;
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    public ClassifiedProposal getExecutionProposal() {
		return executionProposal;
	}

	public void setExecutionProposal(ClassifiedProposal executionProposal) {
		this.executionProposal = executionProposal;
	}

	public Proposal getCounterOffer() {
		return counterOffer;
	}

	public void setCounterOffer(Proposal counterOffer) {
		this.counterOffer = counterOffer;
	}

	public MarketOrder getMarketOrder() {
		return marketOrder;
	}

	public void setMarketOrder(MarketOrder marketOrder) {
		this.marketOrder = marketOrder;
	}
}
