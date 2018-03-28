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

import java.math.BigDecimal;

/**  
*
* Purpose: this class models the commissions for an order execution  
*
* Project Name : bestxengine-akros 
* First created by: ruggero.rizzo
* Creation date: 31-ott-2012 
* 
**/
public class Commission {

    // [DR20121025] Adopted the standard FIX constant instead of an anonymous "2" and "3"
    public static enum CommissionType {
        // [RR20121031] BXMNT-72 new commissionType
        PER_UNIT("" + quickfix.field.CommType.AMOUNT_PER_UNIT), //1
        TICKER("" + quickfix.field.CommType.PERCENT),  //2 
        AMOUNT("" + quickfix.field.CommType.ABSOLUTE)     //3
        ;

        /**
         * Get the order side fix code
         * 
         * @return fix code for the side
         */
        public String getValue() {
            return mFIXValue;
        }

        private final String mFIXValue;

        private CommissionType(String inFIXValue) {
            mFIXValue = inFIXValue;
        }
    }

    public Commission(BigDecimal amount, CommissionType commissionType) {
        this.amount = amount;
        this.commissionType = commissionType;
    }

    private CommissionType commissionType;
    private BigDecimal amount;
    private BigDecimal minimumFeeMaxQty;
    private BigDecimal commissionExcess = BigDecimal.ZERO;
    private BigDecimal minimumFee = BigDecimal.ZERO;

    /**
     * @return the commissionType
     */
    public CommissionType getCommissionType() {
        return commissionType;
    }

    /**
     * @param commissionType
     *            the type to set
     */
    public void setType(CommissionType commissionType) {
        this.commissionType = commissionType;
    }

    /**
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * @param amount
     *            the amount to set
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setMinimumFeeMaxQty(BigDecimal minimumFeeMaxQty) {
        this.minimumFeeMaxQty = minimumFeeMaxQty;
    }

    public BigDecimal getMinimumFeeMaxQty() {
        if (minimumFeeMaxQty == null) {
            minimumFeeMaxQty = BigDecimal.ZERO;
        }
        return minimumFeeMaxQty;
    }

    public BigDecimal getCommissionExcess() {
        return commissionExcess;
    }

    public void setCommissionExcess(BigDecimal commissionExcess) {
        this.commissionExcess = commissionExcess;
    }

    public BigDecimal getMinimumFee() {
        return minimumFee;
    }

    public void setMinimumFee(BigDecimal minimumFee) {
        this.minimumFee = minimumFee;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Commission [commissionType=");
        builder.append(commissionType);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", minimumFeeMaxQty=");
        builder.append(minimumFeeMaxQty);
        builder.append(", commissionExcess=");
        builder.append(commissionExcess);
        builder.append(", minimumFee=");
        builder.append(minimumFee);
        builder.append("]");
        return builder.toString();
    }
}
