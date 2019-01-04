/*
 * Copyright 1997-2012 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.model.CustomerFilterRow;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
public class CSCustomerFilterRow implements CustomerFilterRow{
    private boolean ratingFilter;
    private boolean ocseFilter;
    private boolean maxSizeFilter;
    private boolean law262Filter;
    private boolean manualFilter;
    private boolean retailMaxSizeFilter;
    private String fixId;
    private boolean addCommissionToCustomerPriceFlagSetter;
    private boolean notRegisteredInRegulatedMarketsFilter;

    /**
     * Sets the fix id.
     *
     * @param fixId the new fix id
     */
    public void setFixId(String fixId) {
        this.fixId = fixId;
    }

    /**
     * Gets the fix id.
     *
     * @return the fix id
     */
    public String getFixId() {
        return fixId;
    }

    /**
     * Checks if is rating filter.
     *
     * @return the ratingFilter
     */
    public boolean isRatingFilter() {
        return ratingFilter;
    }

    /**
     * Sets the rating filter.
     *
     * @param ratingFilter the ratingFilter to set
     */
    public void setRatingFilter(boolean ratingFilter) {
        this.ratingFilter = ratingFilter;
    }

    /**
     * Checks if is ocse filter.
     *
     * @return the ocseFilter
     */
    public boolean isOcseFilter() {
        return ocseFilter;
    }

    /**
     * Sets the ocse filter.
     *
     * @param ocseFilter the ocseFilter to set
     */
    public void setOcseFilter(boolean ocseFilter) {
        this.ocseFilter = ocseFilter;
    }

    /**
     * Checks if is max size filter.
     *
     * @return the maxSizeFilter
     */
    public boolean isMaxSizeFilter() {
        return maxSizeFilter;
    }

    /**
     * Sets the max size filter.
     *
     * @param maxSizeFilter the maxSizeFilter to set
     */
    public void setMaxSizeFilter(boolean maxSizeFilter) {
        this.maxSizeFilter = maxSizeFilter;
    }

    /**
     * Checks if is law262 filter.
     *
     * @return the law262Filter
     */
    public boolean isLaw262Filter() {
        return law262Filter;
    }

    /**
     * Sets the law262 filter.
     *
     * @param law262Filter the law262Filter to set
     */
    public void setLaw262Filter(boolean law262Filter) {
        this.law262Filter = law262Filter;
    }

    /**
     * Checks if is manual filter.
     *
     * @return the manualFilter
     */
    public boolean isManualFilter() {
        return manualFilter;
    }

    /**
     * Sets the manual filter.
     *
     * @param manualFilter the manualFilter to set
     */
    public void setManualFilter(boolean manualFilter) {
        this.manualFilter = manualFilter;
    }

    /**
     * Checks if is retail max size filter.
     *
     * @return the retailMaxSizeFilter
     */
    public boolean isRetailMaxSizeFilter() {
        return this.retailMaxSizeFilter;
    }

    /**
     * Sets the retail max size filter.
     *
     * @param retailMaxSizeFilter the retailMaxSizeFilter to set
     */
    public void setRetailMaxSizeFilter(boolean retailMaxSizeFilter) {
        this.retailMaxSizeFilter = retailMaxSizeFilter;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.model.CustomerFilterRow#isaddCommissionToCustomerPriceFlagSetter()
     */
    public boolean isaddCommissionToCustomerPriceFlagSetter()
    {
        return addCommissionToCustomerPriceFlagSetter;
    }

    /**
     * Sets the adds the commission to customer price flag setter.
     *
     * @param addCommissionToCustomerPriceFlagSetter the new adds the commission to customer price flag setter
     */
    public void setaddCommissionToCustomerPriceFlagSetter(boolean addCommissionToCustomerPriceFlagSetter)
    {
        this.addCommissionToCustomerPriceFlagSetter = addCommissionToCustomerPriceFlagSetter;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.model.CustomerFilterRow#isNotRegisteredInRegulatedMarketsFilter()
     */
    public boolean isNotRegisteredInRegulatedMarketsFilter()
    {
        return notRegisteredInRegulatedMarketsFilter;
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.model.CustomerFilterRow#setNotRegisteredInRegulatedMarketsFilter(boolean)
     */
    public void setNotRegisteredInRegulatedMarketsFilter(boolean notRegisteredInRegulatedMarketsFilter)
    {
        this.notRegisteredInRegulatedMarketsFilter = notRegisteredInRegulatedMarketsFilter;

    }

}
