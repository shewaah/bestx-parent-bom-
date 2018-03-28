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
import java.util.Date;

/** 
*
* Purpose: this class is the bean representation of the Holiday database table
*
* Project Name : bestxengine
* First created by: stefano.pontillo
* Creation date: 24/mag/2012
*
**/
public class Holiday implements Serializable {

    private static final long serialVersionUID = -5845386582067684523L;

    private String currency;
    private Date date;
    private String description;
    private String countryCode;

    /**
     * Return the currency code for the holiday
     * 
     * @return the currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Set the currency code for the holiday
     * 
     * @param currency
     *            the currency to set
     */
    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Return date of the holiday
     * 
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * Set date of the holiday
     * 
     * @param date
     *            the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * Return description of the holiday
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set description of the holiday
     * 
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Return the country code who the holiday is applied
     * 
     * @return country code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Set the country code for the holiday
     * 
     * @param countryCode
     *            of the holiday
     */
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Holiday)) {
            return false;
        }
        return ((Holiday) o).getCountryCode().equalsIgnoreCase(countryCode) && ((Holiday) o).getDate().equals(date);
    }

    @Override
    public int hashCode() {
        return countryCode.hashCode() + date.hashCode();
    }
}
