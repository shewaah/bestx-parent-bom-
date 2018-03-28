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

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.db.SetHolidayFinder;
import it.softsolutions.bestx.model.Holiday;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.financecalc.SettlementDateCalculator;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class FormalFutSettDateFilterUsingCalendars implements OrderValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormalFutSettDateFilterUsingCalendars.class);

    // returns the holidays collection (set by spring)
    private SetHolidayFinder holidayFinder = null;
    private SettlementDateCalculator settlementDateCalculator;

    public FormalFutSettDateFilterUsingCalendars() {
    }

    public OrderResult validateOrder(Operation unused, Order order) {
        if (order == null) {
            throw new ObjectNotInitializedException("Order not set");
        }

        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        if (order.getFutSettDate() == null) {
            if (order.getInstrument() == null) {
                throw new ObjectNotInitializedException(Messages.getString("MissingDataError.0", "Order instrument"));
            }
            if (order.getInstrument().getCurrency() == null) {
                throw new ObjectNotInitializedException(Messages.getString("MissingDataError.0", "Instrument currency"));
            }
            String instrumentCountryCode = null;
            if (order.getInstrument().getCountry() == null) {
                LOGGER.info("Order [{}] has a null country, assuming it is a Supranational", order.getFixOrderId());
            } else {
                if (order.getInstrument().getCountry().getCode() == null) {
                    throw new ObjectNotInitializedException(Messages.getString("MissingDataError.0", "Instrument country code"));
                }
                instrumentCountryCode = order.getInstrument().getCountry().getCode();
            }

            // retrieve standard settlement days from instrument, if available
            Integer instrumentStdSettlDays = order.getInstrument().getStdSettlementDays();
            String instrumentCurrency = order.getInstrument().getCurrency();

            if ((instrumentStdSettlDays == null) || (instrumentStdSettlDays < 0)) {
                LOGGER.warn("Order [{}] refers to an instrument [{}] with invalid settlement days [{}], this does not allow us to find a new settlement date, we must reject it",
                        order.getFixOrderId(), order.getInstrument(), instrumentStdSettlDays);
                result.setValid(false);
                result.setReason(Messages.getString("FormalFutSettDate.1"));
            } else {
                List<Holiday> filteredHolidays = holidayFinder.getFilteredHolidays(instrumentCurrency, instrumentCountryCode);

                Date startDate = order.getTransactTime();
                if (startDate == null) {
                    startDate = DateService.newLocalDate();
                }
                
                //[RR20121109] BXSUP-1623 : keep the arrival hour, do not truncate the date because it
                //set the hour to midnight
                //startDate = DateUtils.truncate(startDate, Calendar.DAY_OF_MONTH);

                Date newSettDate = settlementDateCalculator.getCalculatedSettlementDate(instrumentStdSettlDays, filteredHolidays, startDate);

                LOGGER.warn("Order {} without settlement date, taken from its standard settlement days [{}], currency [{}] and country code [{}]: {}", order.getFixOrderId(),
                        instrumentStdSettlDays, instrumentCurrency, instrumentCountryCode, newSettDate);
                
                order.setFutSettDate(newSettDate);
                result.setValid(true);
            }
        } else {
            result.setValid(true);
        }

        return result;
    }

    public boolean isDbNeeded() {
        return false;
    }

    public boolean isInstrumentDbCheck() {
        return false;
    }

    public SetHolidayFinder getHolidayFinder() {
        return holidayFinder;
    }

    public void setHolidayFinder(SetHolidayFinder holidayFinder) {
        this.holidayFinder = holidayFinder;
    }

    public SettlementDateCalculator getSettlementDateCalculator() {
        return settlementDateCalculator;
    }

    public void setSettlementDateCalculator(SettlementDateCalculator settlementDateCalculator) {
        this.settlementDateCalculator = settlementDateCalculator;
    }

}
