/*
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
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.model.SettlementLimit;

/**
 * 
 * 
 * Purpose: this interface is the base for Dao classes who work on the SettlementLimits table
 * 
 * Project Name : bestxengine First created by: stefano.pontillo Creation date: 28/mag/2012
 * 
 **/
public interface SettlementLimitDao {

    /**
     * Return enable SettlementLimit filtered by given currency and country codes
     * 
     * @param currencyCode
     *            The code of currency to filter, "ALL" for all currencies
     * @param countryCode
     *            The code of country to filter, "ALL" for all countries
     * @return enable SettlementLimit object corresponding to the given filter
     */
    SettlementLimit getValidFilteredLimit(String currencyCode, String countryCode);
}
