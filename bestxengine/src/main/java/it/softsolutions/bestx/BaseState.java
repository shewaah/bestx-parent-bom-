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
package it.softsolutions.bestx;

import it.softsolutions.bestx.exceptions.OperationValidationException;
import it.softsolutions.bestx.model.Market;

import java.util.Date;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine First created by: davide.rossoni Creation date: 05/ott/2012
 * 
 **/
public abstract class BaseState implements OperationState, Cloneable {

    @SuppressWarnings("unused")
    private Long id; // persistence

    protected Operation owner;
    private String stateClassName;
    private String comment;
    private Date enteredTime;
    private OperationState.Type type;
    private Market.MarketCode marketCode;

    public BaseState(OperationState.Type type, Market.MarketCode marketCode) {
        this.type = type;
        this.marketCode = marketCode;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Market.MarketCode getMarketCode() {
        return marketCode;
    }

    @Override
    public void setOperation(Operation owner, Date enteredTime) {
        this.owner = owner;
        this.enteredTime = enteredTime;
    }

    @Override
    public void setOperation(Operation owner) {
        setOperation(owner, new Date());
    }

    @Override
    public Date getEnteredTime() {
        if (enteredTime != null) {
            return enteredTime;
        } else {
            return new Date();
        }
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public boolean isRevocable() {
        // Stefano - 20080731 - Nuova gestione revoche, tutti gli stati non finali sono revocabili
        return !isTerminal();
    }

    @Override
    public boolean mustSaveBook() {
        return false;
    }

    @Override
    public abstract OperationState clone() throws CloneNotSupportedException;

    // NOP - terminal is readOnly and depends on class - but we want it persisted
    public void setTerminal(boolean unused) {
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    /**
     * Are quote updates allowed.
     * Some markets can send quote unsolicited updates to rfqs. 
     * This flag is used to identify the states which can handle them, so that
     * they can be handled or discarded
     *      
     * @return true, if multiple quote updates are allowed
     */
    public boolean areMultipleQuotesAllowed() {
        return false;
    }
    
    protected void validateRfq() throws BestXException {
        if (owner.getRfq() == null) {
            throw new OperationValidationException("Rfq is null for operation: " + owner);
        }
    }

    protected void validateQuote() throws BestXException {
        if (owner.getQuote() == null) {
            throw new OperationValidationException("Quote is null for operation: " + owner);
        }
    }

    protected void validateOrder() throws BestXException {
        if (owner.getOrder() == null) {
            throw new OperationValidationException("Order is null for operation: " + owner);
        }
    }

    protected void validateExecutionReports() throws BestXException {
        if (owner.getExecutionReports() == null || owner.getExecutionReports().size() == 0) {
            throw new OperationValidationException("No Execution Report for operation: " + owner);
        }
    }

    protected void validateAttempts() throws BestXException {
        if (owner.getAttempts() == null || owner.getAttempts().size() == 0) {
            throw new OperationValidationException("No Attempts for operation: " + owner);
        }
    }

	/**
	 * @return the stateClassName
	 */
	public String getStateClassName() {
		return stateClassName;
	}

	/**
	 * @param stateClassName the stateClassName to set
	 */
	public void setStateClassName(String stateClassName) {
		this.stateClassName = stateClassName;
	}

}
