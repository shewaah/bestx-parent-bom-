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
package it.softsolutions.bestx.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.instrument.BondTypesService;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 12/lug/2013
 * 
 **/
public class CSBookDepthController extends BookDepthValidator {

    public CSBookDepthController(int minimumRequiredDepth) {
		super(minimumRequiredDepth);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CSBookDepthController.class);

	public void setMinimumRequiredBookDepth(int minimumRequiredBookDepth) {
		this.minimumRequiredBookDepth = minimumRequiredBookDepth;
	}
	
	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.BookValidator#isBookDepthValid(int, it.softsolutions.bestx.model.Attempt, it.softsolutions.bestx.model.Order)
	 */
    @Override
	public boolean isBookDepthValid(Attempt currentAttempt, Order order) {
        if (currentAttempt == null) {
            throw new IllegalArgumentException("currentAttempt cannot be null");
        }
        
//        if (order.isLimitFile()) {
//            LOGGER.info("Order {}, consider book as valid because order is LimitFile", order.getFixOrderId());
////            return true;
//        }
        
//		if(BondTypesService.isUST(order.getInstrument()))
//			return true; // BESTX-382

        if (this.minimumRequiredBookDepth > 0) {//this.minimumRequiredBookDepth = 0
            if (currentAttempt.getSortedBook() != null) {
                List<ClassifiedProposal> bookDepth = currentAttempt.getSortedBook().getValidSideProposals(order.getSide());
                if (bookDepth.size() < minimumRequiredBookDepth) {
                    LOGGER.info("Insufficient book depth ({}, required {}), rejecting order {}.", bookDepth.size(), minimumRequiredBookDepth, order.getFixOrderId());
                    return false;
                }
            } else {
                LOGGER.info("Order {}, consider book as valid because book data needed to check book depth is not availale: book {}", order.getFixOrderId(), currentAttempt.getSortedBook());
            }
        }
        return true;
    }
}
