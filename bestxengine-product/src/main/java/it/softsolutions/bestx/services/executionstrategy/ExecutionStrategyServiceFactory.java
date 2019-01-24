
/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
 
package it.softsolutions.bestx.services.executionstrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
  
/**
 *
 * Purpose: this class is mainly for allowing creation and return of customer specific ExecutionStrategyService instances 
 *
 * Project Name : bestxengine-product
 * First created by: anna.cochetti
 * Creation date: 14/ago/2015
 * 
 **/

public abstract class ExecutionStrategyServiceFactory implements ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionStrategyServiceFactory.class);

    protected ApplicationContext applicationContext;
    
    
    @Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    	this.applicationContext = applicationContext;
	}

    public abstract ExecutionStrategyService getExecutionStrategyService(PriceDiscoveryType priceDiscoveryType, Operation ooperation,
            PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest);
	
	protected static ExecutionStrategyServiceFactory instance;
	
	public static ExecutionStrategyServiceFactory getInstance(){
		if(instance == null)
			LOGGER.error("*********************" + System.getProperty("line.separator") + "ExecutionStrategyServiceFactory not set. Revise configuration!" + "*********************" + System.getProperty("line.separator") );
		return instance;
	}
	
	public void setInstance(ExecutionStrategyServiceFactory newInstance) {
		instance = newInstance;
	}
}
