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
package it.softsolutions.bestx.services.ordervalidation.utils;

import it.softsolutions.bestx.dao.HolidaysDao;
import it.softsolutions.bestx.model.Holiday;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;

/**
 * 
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: stefano.pontillo Creation date: 24/mag/2012
 * 
 **/
public class FakeHolidaysDao implements HolidaysDao {

    @Override
    public boolean isAnHoliday(String currency, Date futDate) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Holiday> getFilteredHolidays(String currency, String countryCode) {
        List<Holiday> holidays = new ArrayList<Holiday>();

        try {
            // Create generic ALL holiday for Euro currency
            Holiday holiday = new Holiday();
            holiday.setCountryCode("ALL");
            holiday.setCurrency("EUR");
            holiday.setDate(DateUtils.parseDate("15/08/2012", new String[] { "dd/MM/yyyy" }));
            holiday.setDescription("Holiday 15/08/2012");
            holidays.add(holiday);

            holiday = new Holiday();
            holiday.setCountryCode("ALL");
            holiday.setCurrency("EUR");
            holiday.setDate(DateUtils.parseDate("25/12/2012", new String[] { "dd/MM/yyyy" }));
            holiday.setDescription("Holiday 25/12/2012");
            holidays.add(holiday);

            holiday = new Holiday();
            holiday.setCountryCode("ALL");
            holiday.setCurrency("EUR");
            holiday.setDate(DateUtils.parseDate("26/12/2012", new String[] { "dd/MM/yyyy" }));
            holiday.setDescription("Holiday 26/12/2012");
            holidays.add(holiday);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return holidays;
    }

    @Override
    public boolean isAnHoliday(String currency, String countryCode, Date date) {
        // TODO Auto-generated method stub
        return false;
    }
}
