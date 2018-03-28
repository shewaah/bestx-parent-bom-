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
package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.CountryFinder;
import it.softsolutions.bestx.finders.ExchangeRateFinder;
import it.softsolutions.bestx.model.Country;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: Jun 13, 2012
 * 
 **/
public class FixValidator {

    private static final String[] DATE_FORMATS = new String[] { "yyyyMMdd" };
    private static final String ACCEPTED_MINIMUM_DATE = "17530101";
    private static final String ACCEPTED_MAXIMUM_DATE = "99991231";
    private static final int SECURITY_ID_MAX_LENGTH = 12;
    private static final int TICKET_OWNER_MAX_LENGTH = 128;
    
    private CountryFinder countryFinder;
    private ExchangeRateFinder exchangeRateFinder;
    
    
    public void performPreValidation(FixOrderInputLazyBean fixOrderInputLazyBean) throws BestXException {
        
        // Check SecurityID
        if (fixOrderInputLazyBean.getSecurityId() != null && fixOrderInputLazyBean.getSecurityId().length() > SECURITY_ID_MAX_LENGTH) {
            throw new BestXException("Unsupported securityID, invalid length");
        }
        
        // Check Side
        if (fixOrderInputLazyBean.getSide() == null) {
            throw new BestXException("Unsupported side");
        }

        // Check FutSettDate
        if (fixOrderInputLazyBean.getFutSettDate() != null) {
            // [RR20120613] Prevent SQLException on Hibernate: Only dates between January 1, 1753 and December 31, 9999 are accepted.
            String futSettDateStr = fixOrderInputLazyBean.getMsg().getString(FixMessageFields.FIX_FutSettDate);
            if (futSettDateStr != null) {
                try {
                    Date futSettDate = DateUtils.parseDateStrictly(futSettDateStr, DATE_FORMATS);

                    java.sql.Date sd = new java.sql.Date(futSettDate.getTime());
                    if (sd.before(DateUtils.parseDate(ACCEPTED_MINIMUM_DATE, DATE_FORMATS)) || sd.after(DateUtils.parseDate(ACCEPTED_MAXIMUM_DATE, DATE_FORMATS))) {
                        throw new BestXException("Unsupported futSettDate, only dates between January 1, 1753 and December 31, 9999 are accepted");
                    }

                } catch (ParseException e) {
                    throw new BestXException("Unsupported futSettDate format");
                }
            }
        }
        
        // Check TicketOwner
        String ticketOwner = fixOrderInputLazyBean.getTicketOwner(); 
        if (ticketOwner != null) {
            if (ticketOwner.length() > TICKET_OWNER_MAX_LENGTH) {
                throw new BestXException("TicketOwner field too long (" + ticketOwner.length() + "), max length allowed is " + TICKET_OWNER_MAX_LENGTH);
            }
        }
        
        //[RR20141219] BXSUP-1891 check if currency and country are valid, if not send an order reject to the customer
        String currency = fixOrderInputLazyBean.getCurrency();
        if (currency == null) {
        	throw new BestXException("Currency cannot be null");
        }
        try {
	        exchangeRateFinder.getExchangeRateByCurrency(currency);
        } catch (BestXException be) {
        	throw be;
        }
        
//        Country country = fixOrderInputLazyBean.getInstrument().getCountry();
//        if (country == null) {
//        	throw new BestXException("Country cannot be null");
//        } else {
//        	try {
//        		countryFinder.getCountryByCode(country.getCode());
//        	} catch (BestXException be) {
//        		throw be;
//        	}
//        }

    }


	public CountryFinder getCountryFinder() {
		return countryFinder;
	}


	public void setCountryFinder(CountryFinder countryFinder) {
		this.countryFinder = countryFinder;
	}


	public ExchangeRateFinder getExchangeRateFinder() {
		return exchangeRateFinder;
	}


	public void setExchangeRateFinder(ExchangeRateFinder exchangeRateFinder) {
		this.exchangeRateFinder = exchangeRateFinder;
	}

}
