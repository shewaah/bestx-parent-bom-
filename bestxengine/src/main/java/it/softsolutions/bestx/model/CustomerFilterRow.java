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

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface CustomerFilterRow {

    /**
     * @return the ratingFilter
     */
    boolean isRatingFilter();

    /**
     * @param ratingFilter
     *            the ratingFilter to set
     */
    void setRatingFilter(boolean ratingFilter);

    /**
     * @return the ocseFilter
     */
    boolean isOcseFilter();

    /**
     * @param ocseFilter
     *            the ocseFilter to set
     */
    void setOcseFilter(boolean ocseFilter);

    /**
     * @return the maxSizeFilter
     */
    boolean isMaxSizeFilter();

    /**
     * @param maxSizeFilter
     *            the maxSizeFilter to set
     */
    void setMaxSizeFilter(boolean maxSizeFilter);

    /**
     * @return the law262Filter
     */
    boolean isLaw262Filter();

    /**
     * @param law262Filter
     *            the law262Filter to set
     */
    void setLaw262Filter(boolean law262Filter);

    /**
     * @return the manualFilter
     */
    boolean isManualFilter();

    /**
     * @param manualFilter
     *            the manualFilter to set
     */
    void setManualFilter(boolean manualFilter);

    /**
     * @return the retailMaxSizeFilter
     */
    boolean isRetailMaxSizeFilter();

    /**
     * @param retailMaxSizeFilter
     *            the retailMaxSizeFilter to set
     */
    void setRetailMaxSizeFilter(boolean retailMaxSizeFilter);

    /**
     * return the saddCommissionToCustomerPriceFlagSetter flag
     */
    boolean isaddCommissionToCustomerPriceFlagSetter();

    /**
     * 
     * @return NotRegisteredInRegulatedMarketsFilter flag
     */
    boolean isNotRegisteredInRegulatedMarketsFilter();

    void setNotRegisteredInRegulatedMarketsFilter(boolean notRegisteredInRegulatedMarketsFilter);
}
