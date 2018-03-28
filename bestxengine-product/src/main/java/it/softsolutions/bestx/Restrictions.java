
/*
 * Copyright 1997-2017 SoftSolutions! srl 
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
 
package it.softsolutions.bestx;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 24 mar 2017
 * 
 **/

public class Restrictions {
    private static final Logger LOGGER = LoggerFactory.getLogger(Restrictions.class);
    private static int maxNumOrders = 100;
    
    static AtomicInteger totalNumOrders = new AtomicInteger(0);
    
	public static void onNewOrder() throws BestXException {
		int numOrders = totalNumOrders.incrementAndGet();
		LOGGER.info("new order. Total Number of orders = {}", numOrders);
		if(numOrders > maxNumOrders) {
			throw new BestXException("Too many orders incoming: " + numOrders + " trying to decrease pace");
		}
	}
	public static void onCloseOrder() {
		int numOrders = totalNumOrders.decrementAndGet();
		LOGGER.info("new order. Total Number of orders = {}", numOrders);
	}
}
