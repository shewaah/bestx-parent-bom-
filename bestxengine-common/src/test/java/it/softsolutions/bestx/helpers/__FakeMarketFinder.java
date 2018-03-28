/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.helpers;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Market.SubMarketCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class __FakeMarketFinder implements MarketFinder
{
	// TODO gestisce solo un mercato per MarketCode (non gestisce omercati multipli con vari sottomercati)
	Map<MarketCode, Market> markets;

	public __FakeMarketFinder()
	{
		markets = new HashMap<MarketCode, Market>();

		Market market = new Market();
		market.setName("TLXFIX");
		market.setMarketCode(MarketCode.TLX);
		market.setSubMarketCode(SubMarketCode.ETX);
		markets.put(market.getMarketCode(), market);

		market = new Market();
		market.setName("BLOOMBERG");
		market.setMarketCode(MarketCode.BLOOMBERG);
		markets.put(market.getMarketCode(), market);

		market = new Market();
		market.setName("BVS");
		market.setMarketCode(MarketCode.BV);
		markets.put(market.getMarketCode(), market);

		market = new Market();
		market.setName("TW");
		market.setMarketCode(MarketCode.TW);
		markets.put(market.getMarketCode(), market);
	}

	public Market getMarketByCode(MarketCode marketCode, SubMarketCode subMarketCode) throws BestXException
	{
		return markets.get(marketCode);
	}

	@Override
	public List<Market> getMarketsByCode(MarketCode marketCode) throws BestXException
	{
		List<Market> marketList = new ArrayList<Market>();
		if (markets.containsKey(marketCode))
			marketList.add(markets.get(marketCode));
		
		return marketList;
	}
}
