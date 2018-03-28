
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

package it.softsolutions.bestx.connections.marketaxess;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.tradestac2.AbstractTradeStac2Connection;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.model.Market;


public class Tradestac2MarketAxessConnection extends AbstractTradeStac2Connection {

//	private static final Logger LOGGER = LoggerFactory.getLogger(Tradestac2MarketAxessConnection.class);

	public Tradestac2MarketAxessConnection(String connectionName) {
		super(connectionName);
	}

	protected MarketAxessHelper marketAxessHelper;
	protected InstrumentFinder instrumentFinder;

	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}

	protected MarketMakerFinder marketMakerFinder;
	protected VenueFinder venueFinder;
	protected MarketFinder marketFinder;
	protected Market market;

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}

	public void setVenueFinder(VenueFinder venueFinder) {
		this.venueFinder = venueFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder) throws BestXException {
		this.marketFinder = marketFinder;
		this.market = marketFinder.getMarketByCode(Market.MarketCode.MARKETAXESS, null);
	}

	public void setMarket(Market market) {
		this.market = market;
	}
}