/*
 * Copyright 1997-2021 SoftSolutions! srl 
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

package it.softsolutions.bestx.bestexec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketMarketMakerSpec;

/**  
 *
 * Purpose: this class the abstraction for classes that implement the MarketOrder creation and fill depending on the installation specifities
 *
 * Project Name : bestxengine 
 * First created by: stefano.pontillo 
 * Creation date: 27 lug 2021 
 * 
 **/
public abstract class MarketOrderBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarketOrderBuilder.class);
	
	public MarketOrderBuilder() {
	}

	private Set<String> marketMakerCompositeCodesSet;
	private String marketMakerCompositeCodes;
	private MarketMakerFinder marketMakerFinder;


	/**
	 * This is used to tell if the marketMakerCode identifies a composite price market maker
	 * 
	 * @param marketMakerCode
	 * @return true if the marketMakerCode is a composite price market maker
	 */
	public boolean isCompositePriceMarketMaker(String marketMakerCode) {
		return marketMakerCompositeCodesSet.contains(marketMakerCode);
	}

	protected boolean isCompositePriceMarketMaker(MarketMarketMaker marketMarketMaker) {
		if (marketMarketMaker != null && marketMarketMaker.getMarketMaker() != null) {
			return isCompositePriceMarketMaker(marketMarketMaker.getMarketMaker().getCode());
		}
		else {
			LOGGER.info("Unable to get market maker code for composite price check. MarketMarketMaker: {}", marketMarketMaker);
			return false;
		}
	}


	/**
	 * This method take informations from operation to create the market order for the current attempt
	 * 
	 * @param operation
	 * @param listener is normally the operation. The Event Handler of the status where this method is called (normally WaitingPricesEventHandler) needs to implement the MarketOrderBuilderListener interface
	 * @return null if is not possible to create the market order
	 * @throws Exception
	 */
	public abstract void buildMarketOrder(Operation operation, MarketOrderBuilderListener listener) throws Exception;

	/**
	 * Removes from the input list (of MarketMarketMakerSpec) the codes related to a composite price
	 * @param dealerList
	 */
	protected void removeCompositePricesFromList(List<MarketMarketMakerSpec> dealerList, Market.MarketCode marketCode) {
		Predicate<MarketMarketMakerSpec> isMmmsAComposite = mmms -> {
			try {
				return isCompositePriceMarketMaker(marketMakerFinder.getMarketMarketMakerByCode(marketCode, mmms.getMarketMakerMarketSpecificCode()));
			} catch (BestXException e) {
				e.printStackTrace();
				return false;
			}
		};
		dealerList.removeIf(isMmmsAComposite);			   
	}
	
	public MarketMakerFinder getMarketMakerFinder() {
		return marketMakerFinder;
	}

	public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
		this.marketMakerFinder = marketMakerFinder;
	}


	public Set<String> getMarketMakerCompositeCodesSet() {
		return marketMakerCompositeCodesSet;
	}

	public void setMarketMakerCompositeCodesSet(String marketMakersCompositeCodes) {
		this.marketMakerCompositeCodesSet = new HashSet<>();
		if (StringUtils.isNotBlank(this.marketMakerCompositeCodes)) {
			this.marketMakerCompositeCodesSet.addAll(Arrays.asList(this.marketMakerCompositeCodes.split(",")));
		}
	}

	public void setMarketMakerCompositeCodesSet(Set<String> marketMakerCompositeCodesSet) {
		this.marketMakerCompositeCodesSet = marketMakerCompositeCodesSet;
	}

	public String getMarketMakerCompositeCodes() {
		return marketMakerCompositeCodes;
	}

	public void setMarketMakerCompositeCodes(String marketMakersCompositeCodes) {
		this.marketMakerCompositeCodes = marketMakersCompositeCodes;
		setMarketMakerCompositeCodesSet(this.marketMakerCompositeCodes);

	}
}
