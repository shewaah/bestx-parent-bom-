
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

import java.util.List;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.services.price.PriceResult;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
  
/**
 *
 * Purpose: this class is mainly for getting customized ExecutionServiceFactory
 *
 * Project Name : bestxengine-cs
 * First created by: anna.cochetti
 * Creation date: 14/ago/2015
 * 
 **/

public class CSExecutionStrategyServiceFactory extends ExecutionStrategyServiceFactory {
//    private static final Logger LOGGER = LoggerFactory.getLogger(CSExecutionStrategyServiceFactory.class);

    
    public CSExecutionStrategyServiceFactory() {
    	instance = this;
    }

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.executionstrategy.ExecutionServiceFactory#getExecutionService(it.softsolutions.bestx.Operation)
	 */
    @Deprecated
	@Override
    public ExecutionStrategyService getExecutionStrategyService(PriceDiscoveryType priceDiscoveryType, ExecutionStrategyServiceCallback executionStrategyServiceCallback,
            PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
		switch (priceDiscoveryType) {
		case LIMIT_FILE_PRICEDISCOVERY: {
			CSLimitFileExecutionStrategyService execService = new CSLimitFileExecutionStrategyService(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		case NORMAL_PRICEDISCOVERY: {
			CSNormalExecutionStrategyService execService = new CSNormalExecutionStrategyService(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		    // AMC 20160801 probably not needed
		case ONLY_PRICEDISCOVERY: {
			CSNormalExecutionStrategyService execService = new CSNormalExecutionStrategyService(executionStrategyServiceCallback, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		default:
		    return null;
		}
	}
    
    public ExecutionStrategyService getExecutionStrategyService(PriceDiscoveryType priceDiscoveryType, Operation operation,
            PriceResult priceResult, boolean rejectOrderWhenBloombergIsBest) {
		switch (priceDiscoveryType) {
		case LIMIT_FILE_PRICEDISCOVERY: {
			CSLimitFileExecutionStrategyService execService = new CSLimitFileExecutionStrategyService(operation, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		case NORMAL_PRICEDISCOVERY: {
			CSNormalExecutionStrategyService execService = new CSNormalExecutionStrategyService(operation, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		    // AMC 20160801 probably not needed
		case ONLY_PRICEDISCOVERY: {
			CSNormalExecutionStrategyService execService = new CSNormalExecutionStrategyService(operation, priceResult, rejectOrderWhenBloombergIsBest);
			execService.setAllMarketsToTry(this.getAllMarketsToTry());
			return execService;
		}
		default:
		    return null;
		}
	}
    protected List<MarketCode> allMarketsToTry;

	public List<MarketCode> getAllMarketsToTry() {
		return allMarketsToTry;
	}

	public void setAllMarketsToTry(List<MarketCode> allMarketsToTry) {
		this.allMarketsToTry = allMarketsToTry;
	}


}