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

import java.io.Serializable;

/**
 * 
 * Purpose: CustomerParamTable hibernate row mapper
 * 
 * Project Name : bestxengine 
 * First created by: ruggero.rizzo 
 * Creation date: 13/nov/2012
 * 
 **/
public class CustomerParam implements Serializable{

    private static final long serialVersionUID = 4733233077444376930L;
    
    private String clientCode;
    private Integer marketId;
    private Integer portfolioId;
    private Double firstQtyThreshold;
    private Double firstSpread;
    private Double firstTolerance;
    private Double secondQtyThreshold;
    private Double secondSpread;
    private Double secondTolerance;
    private Double thirdQtyThreshold;
    private Double thirdSpread;
    private Double thirdTolerance;
    private Double maturityDaysThreshold;
    private Double firstSpreadSecondMaturity;
    private Double firstToleranceSecondMaturity;
    private Double secondSpreadSecondMaturity;
    private Double secondToleranceSecondMaturity;
    private Double thirdSpreadSecondMaturity;
    private Double thirdToleranceSecondMaturity;
    private Double minimumFee;
    private Double minimumFeeMaxSize;
    private Boolean isinType;

    /**
     * @return the clientCode
     */
    public String getClientCode() {
        return clientCode;
    }

    /**
     * @param clientCode
     *            the clientCode to set
     */
    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    /**
     * @return the marketId
     */
    public Integer getMarketId() {
        return marketId;
    }

    /**
     * @param marketId
     *            the marketId to set
     */
    public void setMarketId(Integer marketId) {
        this.marketId = marketId;
    }

    /**
     * @return the portfolioId
     */
    public Integer getPortfolioId() {
        return portfolioId;
    }

    /**
     * @param portfolioId
     *            the portfolioId to set
     */
    public void setPortfolioId(Integer portfolioId) {
        this.portfolioId = portfolioId;
    }

    /**
     * @return the firstQtyThreshold
     */
    public Double getFirstQtyThreshold() {
        return firstQtyThreshold;
    }

    /**
     * @param firstQtyThreshold
     *            the firstQtyThreshold to set
     */
    public void setFirstQtyThreshold(Double firstQtyThreshold) {
        this.firstQtyThreshold = firstQtyThreshold;
    }

    /**
     * @return the firstSpread
     */
    public Double getFirstSpread() {
        return firstSpread;
    }

    /**
     * @param firstSpread
     *            the firstSpread to set
     */
    public void setFirstSpread(Double firstSpread) {
        this.firstSpread = firstSpread;
    }

    /**
     * @return the firstTolerance
     */
    public Double getFirstTolerance() {
        return firstTolerance;
    }

    /**
     * @param firstTolerance
     *            the firstTolerance to set
     */
    public void setFirstTolerance(Double firstTolerance) {
        this.firstTolerance = firstTolerance;
    }

    /**
     * @return the secondQtyThreshold
     */
    public Double getSecondQtyThreshold() {
        return secondQtyThreshold;
    }

    /**
     * @param secondQtyThreshold
     *            the secondQtyThreshold to set
     */
    public void setSecondQtyThreshold(Double secondQtyThreshold) {
        this.secondQtyThreshold = secondQtyThreshold;
    }

    /**
     * @return the secondSpread
     */
    public Double getSecondSpread() {
        return secondSpread;
    }

    /**
     * @param secondSpread
     *            the secondSpread to set
     */
    public void setSecondSpread(Double secondSpread) {
        this.secondSpread = secondSpread;
    }

    /**
     * @return the secondTolerance
     */
    public Double getSecondTolerance() {
        return secondTolerance;
    }

    /**
     * @param secondTolerance
     *            the secondTolerance to set
     */
    public void setSecondTolerance(Double secondTolerance) {
        this.secondTolerance = secondTolerance;
    }

    /**
     * @return the thirdQtyThreshold
     */
    public Double getThirdQtyThreshold() {
        return thirdQtyThreshold;
    }

    /**
     * @param thirdQtyThreshold
     *            the thirdQtyThreshold to set
     */
    public void setThirdQtyThreshold(Double thirdQtyThreshold) {
        this.thirdQtyThreshold = thirdQtyThreshold;
    }

    /**
     * @return the thirdSpread
     */
    public Double getThirdSpread() {
        return thirdSpread;
    }

    /**
     * @param thirdSpread
     *            the thirdSpread to set
     */
    public void setThirdSpread(Double thirdSpread) {
        this.thirdSpread = thirdSpread;
    }

    /**
     * @return the thirdTolerance
     */
    public Double getThirdTolerance() {
        return thirdTolerance;
    }

    /**
     * @param thirdTolerance
     *            the thirdTolerance to set
     */
    public void setThirdTolerance(Double thirdTolerance) {
        this.thirdTolerance = thirdTolerance;
    }

    /**
     * @return the maturityDaysThreshold
     */
    public Double getMaturityDaysThreshold() {
        return maturityDaysThreshold;
    }

    /**
     * @param maturityDaysThreshold
     *            the maturityDaysThreshold to set
     */
    public void setMaturityDaysThreshold(Double maturityDaysThreshold) {
        this.maturityDaysThreshold = maturityDaysThreshold;
    }

    /**
     * @return the firstSpreadSecondMaturity
     */
    public Double getFirstSpreadSecondMaturity() {
        return firstSpreadSecondMaturity;
    }

    /**
     * @param firstSpreadSecondMaturity
     *            the firstSpreadSecondMaturity to set
     */
    public void setFirstSpreadSecondMaturity(Double firstSpreadSecondMaturity) {
        this.firstSpreadSecondMaturity = firstSpreadSecondMaturity;
    }

    /**
     * @return the firstToleranceSecondMaturity
     */
    public Double getFirstToleranceSecondMaturity() {
        return firstToleranceSecondMaturity;
    }

    /**
     * @param firstToleranceSecondMaturity
     *            the firstToleranceSecondMaturity to set
     */
    public void setFirstToleranceSecondMaturity(Double firstToleranceSecondMaturity) {
        this.firstToleranceSecondMaturity = firstToleranceSecondMaturity;
    }

    /**
     * @return the secondSpreadSecondMaturity
     */
    public Double getSecondSpreadSecondMaturity() {
        return secondSpreadSecondMaturity;
    }

    /**
     * @param secondSpreadSecondMaturity
     *            the secondSpreadSecondMaturity to set
     */
    public void setSecondSpreadSecondMaturity(Double secondSpreadSecondMaturity) {
        this.secondSpreadSecondMaturity = secondSpreadSecondMaturity;
    }

    /**
     * @return the secondToleranceSecondMaturity
     */
    public Double getSecondToleranceSecondMaturity() {
        return secondToleranceSecondMaturity;
    }

    /**
     * @param secondToleranceSecondMaturity
     *            the secondToleranceSecondMaturity to set
     */
    public void setSecondToleranceSecondMaturity(Double secondToleranceSecondMaturity) {
        this.secondToleranceSecondMaturity = secondToleranceSecondMaturity;
    }

    /**
     * @return the thirdSpreadSecondMaturity
     */
    public Double getThirdSpreadSecondMaturity() {
        return thirdSpreadSecondMaturity;
    }

    /**
     * @param thirdSpreadSecondMaturity
     *            the thirdSpreadSecondMaturity to set
     */
    public void setThirdSpreadSecondMaturity(Double thirdSpreadSecondMaturity) {
        this.thirdSpreadSecondMaturity = thirdSpreadSecondMaturity;
    }

    /**
     * @return the thirdToleranceSecondMaturity
     */
    public Double getThirdToleranceSecondMaturity() {
        return thirdToleranceSecondMaturity;
    }

    /**
     * @param thirdToleranceSecondMaturity
     *            the thirdToleranceSecondMaturity to set
     */
    public void setThirdToleranceSecondMaturity(Double thirdToleranceSecondMaturity) {
        this.thirdToleranceSecondMaturity = thirdToleranceSecondMaturity;
    }

    /**
     * @return the minimumFee
     */
    public Double getMinimumFee() {
        return minimumFee;
    }

    /**
     * @param minimumFee
     *            the minimumFee to set
     */
    public void setMinimumFee(Double minimumFee) {
        this.minimumFee = minimumFee;
    }

    /**
     * @return the minimumFeeMaxSize
     */
    public Double getMinimumFeeMaxSize() {
        return minimumFeeMaxSize;
    }

    /**
     * @param minimumFeeMaxSize
     *            the minimumFeeMaxSize to set
     */
    public void setMinimumFeeMaxSize(Double minimumFeeMaxSize) {
        this.minimumFeeMaxSize = minimumFeeMaxSize;
    }

    /**
     * @return the isinType
     */
    public Boolean getIsinType() {
        return isinType;
    }

    /**
     * @param isinType
     *            the isinType to set
     */
    public void setIsinType(Boolean isinType) {
        this.isinType = isinType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof CustomerParam)) {
            return false;
        }
        return ((CustomerParam) o).getClientCode().equals(clientCode) && ((CustomerParam) o).getIsinType().equals(isinType) && ((CustomerParam) o).getMarketId().equals(marketId) && ((CustomerParam) o).getPortfolioId().equals(portfolioId);
    }

    @Override
    public int hashCode() {
        return clientCode.hashCode() + isinType.hashCode() + marketId.hashCode() + portfolioId.hashCode();
    }
}
