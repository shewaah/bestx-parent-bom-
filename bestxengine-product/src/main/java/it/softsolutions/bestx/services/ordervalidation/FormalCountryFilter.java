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
package it.softsolutions.bestx.services.ordervalidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.CountryFinder;
import it.softsolutions.bestx.model.Country;
import it.softsolutions.bestx.model.Order;

/**  
 *
 * Purpose: this class checks if the order's isin country is known to BestX:FI-A
 * If not the order must be rejected. 
 *
 * Project Name : bestxengine-product 
 * First created by: ruggero.rizzo 
 * Creation date: 09/set/2013 
 * 
 **/
public class FormalCountryFilter implements OrderValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FormalCountryFilter.class);
    private CountryFinder countryFinder;
    
    @Override
    public OrderResult validateOrder(Operation operation, Order order) {
        OrderResultBean result = new OrderResultBean();
        result.setOrder(order);
        result.setReason("");
        if (order.getInstrument() != null) {
            if (order.getInstrument().getCountry() == null) {
                LOGGER.error("Order rejected, the instrument country cannot be null");
                result.setReason(Messages.getString("FormalCountryNotAvailable.1"));
            } else {
                String code = order.getInstrument().getCountry().getCode();
                Country country = null;
                try {
                    country = countryFinder.getCountryByCode(code);
                } catch (BestXException e) {
                    LOGGER.error("Error while fetching country from code {}", code, e);
                }
                
                if (country == null) {
                    LOGGER.info("Order rejected, country {} not found", code);
                    result.setReason(Messages.getString("FormalCountryNotAvailable.0", code));
                } else {
                    result.setValid(true);
                }
            }
        } else {
//            LOGGER.debug("Instrument not available, cannot get its country, reject the order");
//            LOGGER.info("Order rejected, instrument not available, country cannot be found");
//            result.setReason(Messages.getString("FormalCountryNotAvailable.1"));
            //[RR20130917] a missing instrument will be looked for using Service
            result.setValid(true);
        }
        return result;
    }

    @Override
    public boolean isDbNeeded() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isInstrumentDbCheck() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @param countryFinder the countryFinder to set
     */
    public void setCountryFinder(CountryFinder countryFinder) {
        this.countryFinder = countryFinder;
    }

}
