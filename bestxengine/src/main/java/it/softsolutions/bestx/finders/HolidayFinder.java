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
package it.softsolutions.bestx.finders;

import it.softsolutions.bestx.model.Holiday;

import java.util.Date;
import java.util.List;

/**
 * 
 * Purpose: finder class to manage serch of Holidays
 * 
 * Project Name : bestxengine First created by: stefano.pontillo Creation date: 24/mag/2012
 * 
 **/
public interface HolidayFinder {

    /**
     * Check if the given date is an holiday for the currency
     * 
     * @param currency
     *            Currency code to verify
     * @param futDate
     *            Settlement date to verify
     * @return true if the given date is an holiday for the given currency
     */
    boolean isAnHoliday(String currency, Date futDate);

    /**
     * @param currency
     * @param countryCode
     * @param futDate
     * @return
     */
    boolean isAnHoliday(String currency, String countryCode, Date futDate);

    /**
     * @param currency
     * @param countryCode
     * @return
     */
    List<Holiday> getFilteredHolidays(String currency, String countryCode);

}
