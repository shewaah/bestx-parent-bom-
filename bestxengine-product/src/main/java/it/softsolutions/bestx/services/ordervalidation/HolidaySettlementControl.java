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

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.HolidayFinder;
import it.softsolutions.bestx.model.Order;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: Creation date: 19-ott-2012
 * 
 **/
public class HolidaySettlementControl implements OrderValidator {
    private HolidayFinder holidayFinder;

    @Override
    public OrderResult validateOrder(Operation unused, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");

        // If it is holiday the validateOrder should return false
        String countryCode = (order.getInstrument() != null && order.getInstrument().getCountry() != null) ? order.getInstrument().getCountry().getCode() : null;

        result.setValid(!(holidayFinder.isAnHoliday(order.getCurrency(), countryCode, order.getFutSettDate())));
        if (!result.isValid()) {
            result.setReason(Messages.getString("HolidaySettlement.3", order.getFutSettDate()));
        }
        return result;
    }

    /**
     * Sets the holiday finder.
     * 
     * @param holidayFinder
     *            the new holiday finder
     */
    public void setHolidayFinder(HolidayFinder holidayFinder) {
        this.holidayFinder = holidayFinder;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isDbNeeded()
     */
    public boolean isDbNeeded() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.services.ordervalidation.OrderValidator#isInstrumentDbCheck()
     */
    public boolean isInstrumentDbCheck() {
        return false;
    }
}
