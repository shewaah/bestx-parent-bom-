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

import java.util.ArrayList;
import java.util.List;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Attempt {
    public static enum AttemptState { 
        COUNTER_RECEIVED, ACCEPTED_COUNTER, PASSED_COUNTER, EXECUTED, REJECTED, EXPIRED
    }
    
    public static int maxAttemptNo = 10;
    @SuppressWarnings("unused")
    private Long id;
    private ClassifiedProposal executionProposal;

    /** multiple quotes reported  in a RFCQ or other competitive trading protocol .
     * quotes(0) is always the counterOffer;
	*/
    private List<ExecutablePrice> executablePrices;

    private SortedBook sortedBook;
    private MarketOrder marketOrder;
    private List<MarketExecutionReport> marketExecutionReports;
    private boolean byPassableForVenueAlreadyTried = false;
    private InternalAttempt internalAttempt;
    
    private AttemptState attemptState;
    
    public InternalAttempt getInternalAttempt() {
		return internalAttempt;
	}
    
	public void setInternalAttempt(InternalAttempt internalAttempt) {
		this.internalAttempt = internalAttempt;
	}
	
	public void setExecutionProposal(ClassifiedProposal executionProposal) {
        this.executionProposal = executionProposal;
    }
    public ClassifiedProposal getExecutionProposal() {
        return executionProposal;
    }
    public ExecutablePrice getExecutablePrice(int rank) {
    	checkExecutablePrices();
    	// rank starts at 1, list index starts at 0
    	for(ExecutablePrice ep : this.executablePrices) {
    		if(ep.getRank() != null && ep.getRank().intValue() == rank)
    			return ep;
    	}
    	return null;
    }
    public void setExecutablePrices(List<ExecutablePrice> quotes) {
        this.executablePrices = quotes;
    }
    public List<ExecutablePrice> getExecutablePrices() {
        return this.executablePrices;
    }
    public synchronized void addExecutablePrice(ExecutablePrice proposal, int rank) {
    	checkExecutablePrices();
       	proposal.setRank(rank);
       	proposal.setAttemptId(this.id); // needed because AttemptId is not null on DB
    	this.executablePrices.add(proposal);
    	this.executablePrices.sort((a, b) -> a.compareTo(b));
    }

    public SortedBook getSortedBook() {
        return sortedBook;
    }
    public void setSortedBook(SortedBook sortedBook) {
        this.sortedBook = sortedBook;
    }
    public MarketOrder getMarketOrder() {
        return marketOrder;
    }
    public void setMarketOrder(MarketOrder marketOrder) {
        this.marketOrder = marketOrder;
    }
    public List<MarketExecutionReport> getMarketExecutionReports() {
        return marketExecutionReports;
    }
    public void setMarketExecutionReports(List<MarketExecutionReport> marketExecutionReports) {
        this.marketExecutionReports = marketExecutionReports;
    }

	public boolean isByPassableForVenueAlreadyTried() {
		return byPassableForVenueAlreadyTried;
	}

	public void setByPassableForVenueAlreadyTried(boolean byPassableForVenueAlreadyTried) {
		this.byPassableForVenueAlreadyTried = byPassableForVenueAlreadyTried;
	}

    public void setValues(Attempt attempt) {
        setExecutionProposal(attempt.getExecutionProposal());
        setExecutablePrices(attempt.getExecutablePrices());
        setSortedBook(attempt.getSortedBook());
        setMarketOrder(attempt.getMarketOrder());
        setByPassableForVenueAlreadyTried(attempt.isByPassableForVenueAlreadyTried());
        setConsecutiveExecutionRetries(attempt.getConsecutiveExecutionRetries());
    }
 
    private int consecutiveExecutionRetries = 0;

	public int getConsecutiveExecutionRetries() {
		return consecutiveExecutionRetries;
	}

	public void setConsecutiveExecutionRetries(int consecutiveExecutionRetries) {
		this.consecutiveExecutionRetries = consecutiveExecutionRetries;
	}
	
	public void addConsecutiveTry(){
		consecutiveExecutionRetries++;
	}
	
	public AttemptState getAttemptState() {
		return attemptState;
	}

	public void setAttemptState(AttemptState attemptState) {
		this.attemptState = attemptState;
	}

	private void checkExecutablePrices() {
		if(this.getExecutablePrices() == null) this.setExecutablePrices(new ArrayList<ExecutablePrice>());
	}
}
