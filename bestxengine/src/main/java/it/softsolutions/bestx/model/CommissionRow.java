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
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 13/nov/2012 
 * 
 **/
public class CommissionRow {
    private Commission commission;
    private BigDecimal minQty;
    private BigDecimal maxQty;
    private BigDecimal minimumFee;
    private BigDecimal minimumFeeMaxSize;

    public CommissionRow(BigDecimal minQty, BigDecimal maxQty, Commission commission, BigDecimal minimumFee, BigDecimal minimumFeeMaxSize) {
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.commission = commission;
        this.minimumFee = minimumFee;
        this.minimumFeeMaxSize = minimumFeeMaxSize;
    }

    /**
     * @return the commission
     */
    public Commission getCommission() {
        return commission;
    }

    /**
     * @param commission
     *            the commission to set
     */
    public void setCommission(Commission commission) {
        this.commission = commission;
    }

    /**
     * @return the minQty
     */
    public BigDecimal getMinQty() {
        return minQty;
    }

    /**
     * @param minQty
     *            the minQty to set
     */
    public void setMinQty(BigDecimal minQty) {
        this.minQty = minQty;
    }

    /**
     * @return the maxQty
     */
    public BigDecimal getMaxQty() {
        return maxQty;
    }

    /**
     * @param maxQty
     *            the maxQty to set
     */
    public void setMaxQty(BigDecimal maxQty) {
        this.maxQty = maxQty;
    }

    public BigDecimal getMinimumFee() {
        return minimumFee;
    }

    public void setMinimumFee(BigDecimal minimumFee) {
        this.minimumFee = minimumFee;
    }

    public BigDecimal getMinimumFeeMaxSize() {
        return minimumFeeMaxSize;
    }

    public void setMinimumFeeMaxSize(BigDecimal minimumFeeMaxSize) {
        this.minimumFeeMaxSize = minimumFeeMaxSize;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CommissionRow [commission=");
        builder.append(commission);
        builder.append(", minQty=");
        builder.append(minQty);
        builder.append(", maxQty=");
        builder.append(maxQty);
        builder.append(", minimumFee=");
        builder.append(minimumFee);
        builder.append(", minimumFeeMaxSize=");
        builder.append(minimumFeeMaxSize);
        builder.append("]");
        return builder.toString();
    }
}