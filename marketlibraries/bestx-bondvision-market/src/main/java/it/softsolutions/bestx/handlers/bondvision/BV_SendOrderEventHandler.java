/*
 * Copyright 2019-2028 SoftSolutions! srl 
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
package it.softsolutions.bestx.handlers.bondvision;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.handlers.BaseOperationEventHandler;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.services.serial.SerialNumberService;

public class BV_SendOrderEventHandler extends BaseOperationEventHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BV_SendOrderEventHandler(Operation operation, MarketBuySideConnection buySideConnection, SerialNumberService serialNumberService,
			long waitingExecutionDelay, long orderCancelDelay, BestXConfigurationDao bestXConfigurationDao, MarketMakerFinder marketMakerFinder, Market market, VenueFinder venueFinder) {
        super(operation);
    }
}
