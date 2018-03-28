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
package it.softsolutions.bestx.dao.bean;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 
 * Purpose: The PriceForgeRuleBean represents the rule object used by price discovery to managed the result of the Price Forge
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 20/feb/2013 
 * 
 **/
public class PriceForgeRuleBeanImpl implements PriceForgeRuleBean {
    private String isin;
    private ProposalSide side;
    private OperatorType conditionOperator;
    private BigDecimal conditionValue;
    private ActionType action;
    private BigDecimal spread;
    private boolean active;

    private MathContext mc = new MathContext(2, RoundingMode.HALF_EVEN);

    /**
     * Return the isin code of the instrument
     * 
     * @return isin code of the instrument
     */
    public String getIsin() {
        return this.isin;
    }

    /**
     * Set the isin code of the instrument
     * 
     * @param isin
     *            code of the instrument
     */
    public void setIsin(String isin) {
        this.isin = isin;
    }

    /**
     * Return the side of the proposal
     * 
     * @return the side of the proposal
     */
    public ProposalSide getSide() {
        return this.side;
    }

    /**
     * Set the side of the proposal
     * 
     * @param side
     *            of the proposal
     */
    public void setSide(ProposalSide side) {
        this.side = side;
    }

    /**
     * Set the side of the proposal given a String that represent it
     * 
     * @param side
     *            of the proposal
     */
    public void setSide(String side) {
        if (ProposalSide.ASK.getString().compareToIgnoreCase(side) == 0) {
            this.side = ProposalSide.ASK;
        } else if (ProposalSide.BID.getString().compareToIgnoreCase(side) == 0) {
            this.side = ProposalSide.BID;
        } else {
            // if(side.compareTo(ProposalSide.BOTH.getString())== 0)
            this.side = ProposalSide.BOTH;
        }
    }

    /**
     * Return the OperatorType of the rule
     * 
     * @return OperatorType of the rule
     */
    public OperatorType getConditionOperator() {
        return this.conditionOperator;
    }

    /**
     * Set the OperatorType of the rule
     * 
     * @param conditionOperator
     *            The OperatorType of the rule
     */
    public void setConditionOperator(OperatorType conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    /**
     * Set the OperatorType of the rule given the string representation of the OperatorType
     * 
     * @param conditionOperator
     *            of the rule
     */
    public void setConditionOperator(String conditionOperator) {
        if (conditionOperator.compareTo(OperatorType.STRICTLY_LESS_THAN.getString()) == 0) {
            this.conditionOperator = OperatorType.STRICTLY_LESS_THAN;
        } else if (conditionOperator.compareTo(OperatorType.LESS_THAN.getString()) == 0) {
            this.conditionOperator = OperatorType.LESS_THAN;
        } else if (conditionOperator.compareTo(OperatorType.EQUAL.getString()) == 0) {
            this.conditionOperator = OperatorType.EQUAL;
        } else if (conditionOperator.compareTo(OperatorType.MORE_THAN.getString()) == 0) {
            this.conditionOperator = OperatorType.MORE_THAN;
        } else {
            this.conditionOperator = OperatorType.STRICTLY_MORE_THAN;
        }
    }

    /**
     * Return the condition value of the rule
     * 
     * @return the condition value of the rule
     */
    public BigDecimal getConditionValue() {
        return this.conditionValue;
    }

    /**
     * Set the condition value of the rule
     * 
     * @param conditionValue
     *            value of the condition of the rule
     */
    public void setConditionValue(BigDecimal conditionValue) {
        this.conditionValue = conditionValue;
    }

    /**
     * Return the action of the rule
     * 
     * @return the action that the rule perform
     */
    public ActionType getAction() {
        return this.action;
    }

    /**
     * Set the action that the rule perform
     * 
     * @param action
     *            ActionType of the action that the rule perform
     */
    public void setAction(ActionType action) {
        this.action = action;
    }

    /**
     * Set the action that the rule perform given a String that represents the action
     * 
     * @param action
     *            String name of the action that the rule perform
     */
    public void setAction(String action) {
        if (action.compareTo(ActionType.NO_PRICE.getString()) == 0) {
            setAction(ActionType.NO_PRICE);
        } else {
            setAction(ActionType.ADD_SPREAD);
        }
    }

    /**
     * Return the spread of the rule
     * 
     * @return value of the spread
     */
    public BigDecimal getSpread() {
        return this.spread;
    }

    /**
     * Set the value of the spread for this rule
     * 
     * @param spread
     *            value for the rule
     */
    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    /**
     * Load rule data from a given resultset
     * 
     * @param rs
     *            the Resultset contains the data of the rule
     * @throws SQLException
     *             generic SQLException related to the Resultset
     */
    public PriceForgeRuleBeanImpl(ResultSet rs) throws SQLException {
        setIsin(rs.getString(ISIN));
        setConditionValue(new BigDecimal(rs.getDouble(CONDITION_VALUE), mc));
        setSpread(new BigDecimal(rs.getDouble(SPREAD), mc));
        setAction(rs.getString(ACTION));
        setConditionOperator(rs.getString(CONDITION_OPERATOR));
        setSide(rs.getString(SIDE));
        setActive(rs.getBoolean(ACTIVE));

    }

    /**
     * Generic constructor
     */
    public PriceForgeRuleBeanImpl() {

    }

    /**
     * Compare the given quantity with the condition value and return the result based on the condition operator
     * 
     * if conditionOperator is STRICTLY_LESS_THAN compare if the given quantity is less then the condition value if conditionOperator is
     * LESS_THAN compare if the given quantity is less or equal then the condition value if conditionOperator is EQUAL compare if the given
     * quantity is equals then the condition value if conditionOperator is MORE_THAN compare if the given quantity is more or equal then the
     * condition value if conditionOperator is STRICTLY_MORE_THAN compare if the given quantity is more then the condition value
     * 
     * @param quantity
     *            the value of the quantity to check
     * @return the result of the comparision with the condition value
     */
    public boolean isQuantityInCondition(BigDecimal quantity) {
        switch (conditionOperator.getValue()) {
        case STRICTLY_LESS_THAN:
            return quantity.compareTo(conditionValue) < 0;
        case LESS_THAN:
            return quantity.compareTo(conditionValue) <= 0;
        case EQUAL:
            return quantity.compareTo(conditionValue) == 0;
        case MORE_THAN:
            return quantity.compareTo(conditionValue) >= 0;
        case STRICTLY_MORE_THAN:
            return quantity.compareTo(conditionValue) > 0;
        }
        return false;
    }

    /**
     * Override of the standard toString method
     */
    public String toString() {
        return "Price Forge Rule : On ISIN " + isin + " and side " + side.getString() + " if Quantity " + (conditionOperator != null ? conditionOperator.getString() : "not defined") + " "
                + (conditionValue != null ? conditionValue.toPlainString() : "not defined") + " action is " + action.getString()
                + ((action.getValue() == NO_PRICE) ? "." : (" " + (spread != null ? spread.toString() : "not defined") + "."));
    }

    /**
     * Return if the rule is active
     * 
     * @return true if the rule is active
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Set the activwe flag of the rule
     * 
     * @param active
     *            true if the rule is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}

