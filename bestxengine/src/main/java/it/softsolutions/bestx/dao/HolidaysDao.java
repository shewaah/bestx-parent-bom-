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

import it.softsolutions.bestx.model.Holiday;

import java.util.Date;
import java.util.List;

/**
 * 
 * Purpose : holiday days management DAO.
 * 
 * Project Name : bestxengine First created by: stefano.pontillo Creation date: 24/mag/2012
 * 
 **/
public interface HolidaysDao {
    /**
     * Check if the given date is a holiday for the given currency.
     * 
     * @param currency
     *            : currency to check
     * @param date
     *            : date to check
     * @return true if it is a holiday, false otherwise.
     */
    boolean isAnHoliday(String currency, Date date);

    /**
     * @param currency
     * @param countryCode
     * @param date
     * @return
     */
    boolean isAnHoliday(String currency, String countryCode, Date date);

    /**
     * @param currency
     * @param countryCode
     * @return
     */
    List<Holiday> getFilteredHolidays(String currency, String countryCode);

}
