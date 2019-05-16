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
