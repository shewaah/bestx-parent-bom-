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
package it.softsolutions.bestx.services.financecalc;

import it.softsolutions.bestx.model.Holiday;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.settlmentdate.SettlementDateManager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose : Service class to calculate settlement dates and settlement days.
 * 
 * Project Name : bestxengine-product First created by: stefano.pontillo Creation date: 24/mag/2012
 * 
 **/
public class SettlementDateCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettlementDateCalculator.class);

    private SettlementDateManager settlementDateManager;

    /**
     * Generic empty constructor
     */
    public SettlementDateCalculator() {
    }

    /**
     * Verify if the order settlement date is standard
     * 
     * @param order
     *            The customer order
     * @param instrument
     *            The instrument requested - unused
     * @return true if the order settlement date is standard
     */
    public boolean isOrderSettlementDateStandard(Order order, Instrument instrument) {
        if (settlementDateManager == null) {
            LOGGER.warn("settlementDateManager is null");
            return false;
        }
        // the settlementDateManager replaces the control on the instrument BBGSettlementDate
        // because BBG dates are no more received.
        return DateUtils.isSameDay(order.getFutSettDate(), settlementDateManager.getSettlementDate(order));
    }

    /**
     * Set the settlement date manager
     * 
     * @param settlementDateManager
     */
    public void setSettlementDateManager(SettlementDateManager settlementDateManager) {
        this.settlementDateManager = settlementDateManager;
    }

    /**
     * Return the settlement date for the instrument
     * 
     * @param order
     *            The customer order
     * @return The Settlement Date for the instrument in the given order
     */
    public Date getInstrumentSettlementDate(Order order) {
        if (settlementDateManager == null) {
            LOGGER.warn("settlementDateManager is null");
            return null;
        }
        return settlementDateManager.getSettlementDate(order);
    }

    /**
     * Return the settlement date calculated from startDate to the date represented by startDate + settlementDays excluded holidays and no
     * business days
     * 
     * @param settlementDays
     *            Number of days to calculated the settlement date
     * @param filteredHolidays
     *            Collection of holidays (filtered by currency and country)
     * @param startDate
     *            Start date for the calculation
     * @return Date representing the settlement date, null if startDate is null
     */
    public Date getCalculatedSettlementDate(int settlementDays, List<Holiday> filteredHolidays, Date startDate) {
        if (startDate == null) {
            return null;
        }

        Calendar startCal;
        Calendar endCal;
        startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        endCal = Calendar.getInstance();
        endCal.setTime(startDate);
        int daysCount = settlementDays;
        // Return today if days is set to 0
        if (daysCount <= 0) {
            return startDate;
        }
        do {
            // today is not included in the first loop
            endCal.add(Calendar.DAY_OF_YEAR, 1);

            // Check if the date isn't weekend or holiday
            if (endCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && endCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                    && (filteredHolidays == null || !isAnHoliday(endCal.getTime(), filteredHolidays))) {
                --daysCount;
            }
        } while (daysCount > 0);
        return endCal.getTime();
    }

    /**
     * Calculate the number of market days between a given start date and end date
     * 
     * @param startDate
     *            Start date for the calculation
     * @param endDate
     *            End date for the calculation
     * @param filteredHolidays
     *            Collection of holidays (filtered by currency and country)
     * @return the number of days between the start date and the end date excluded holidays and no business days, 0 if startDate and endDate
     *         are the same, -1 if startDate or endDate are null
     */
    public int getCalculatedSettlementDays(Date startDate, Date endDate, List<Holiday> filteredHolidays) {
        if (startDate == null || endDate == null) {
            return -1;
        }

        Calendar startCal;
        Calendar endCal;
        startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal = DateUtils.truncate(startCal, Calendar.DAY_OF_MONTH);
        endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal = DateUtils.truncate(endCal, Calendar.DAY_OF_MONTH);
        int workDays = 0;

        // Return 0 if start and end are the same
        if (DateUtils.isSameDay(startCal, endCal)) {
            return 0;
        }

        // ??? Ask SP
        if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
            startCal.setTime(endDate);
            endCal.setTime(startDate);
        }

        do {
            // today is not included in the first loop
            startCal.add(Calendar.DAY_OF_YEAR, 1);

            // Check if the date isn't weekend or holiday
            if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                    && (filteredHolidays == null || !isAnHoliday(startCal.getTime(), filteredHolidays))) {
                ++workDays;
            }
        } while (startCal.getTimeInMillis() < endCal.getTimeInMillis());
        
        return workDays;
    }

    private boolean isAnHoliday(Date date, List<Holiday> holidays) {
        for (Holiday holiday : holidays) {
            if (DateUtils.isSameDay(holiday.getDate(), date)) {
                return true;
            }
        }
        return false;
    }

}
