
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

import it.softsolutions.bestx.MarketConnectionRegistry;
import it.softsolutions.bestx.MifidConfig;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.bestexec.BookClassifier;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.services.booksorter.BookSorterImpl;
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

public class CSExecutionStrategyServiceFactory extends ExecutionStrategyServiceFactory implements CSExecutionStrategyServiceFactoryMBean {

	private MarketFinder marketFinder;
	private BookClassifier bookClassifier;
	private BookSorterImpl bookSorter;
	private ApplicationStatus applicationStatus;
	private MifidConfig mifidConfig;
	private MarketConnectionRegistry marketConnectionRegistry;

	public BookClassifier getBookClassifier() {
		return bookClassifier;
	}

	public void setBookClassifier(BookClassifier bookClassifier) {
		this.bookClassifier = bookClassifier;
	}

	public BookSorterImpl getBookSorter() {
		return bookSorter;
	}

	public void setBookSorter(BookSorterImpl bookSorter) {
		this.bookSorter = bookSorter;
	}
	
	public void setMarketConnectionRegistry(MarketConnectionRegistry marketConnectionRegistry) {
		this.marketConnectionRegistry = marketConnectionRegistry;
	}
	
	public MarketConnectionRegistry getMarketConnectionRegistry() {
		return marketConnectionRegistry;
	}

	public MarketFinder getMarketFinder()
	{
		return marketFinder;
	}

	public void setMarketFinder(MarketFinder marketFinder)
	{
		this.marketFinder = marketFinder;
	}
    
    public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}
	
	public CSExecutionStrategyServiceFactory() {
    	setInstance(this);
    }
    
    private static void setInstance(CSExecutionStrategyServiceFactory obj) {
    	instance = obj;
    }

    @Override
    public ExecutionStrategyService getExecutionStrategyService(PriceDiscoveryType priceDiscoveryType, Operation operation,
            PriceResult priceResult) {
      switch (priceDiscoveryType) {
      case ONLY_PRICEDISCOVERY:
      case LIMIT_FILE_PRICEDISCOVERY: {
         CSLimitFileExecutionStrategyService execService = new CSLimitFileExecutionStrategyService(operation, priceResult, this.applicationStatus, this.mifidConfig);
         execService.setMarketFinder(marketFinder);
         execService.setBookClassifier(bookClassifier);
         execService.setBookSorter(bookSorter);
         execService.setMarketConnectionRegistry(marketConnectionRegistry);
         return execService;
      }
      case NORMAL_PRICEDISCOVERY: {
         CSNormalExecutionStrategyService execService = new CSNormalExecutionStrategyService(operation, priceResult, this.applicationStatus, this.mifidConfig);
         execService.setMarketFinder(marketFinder);
         execService.setBookClassifier(bookClassifier);
         execService.setBookSorter(bookSorter);
         execService.setMarketConnectionRegistry(marketConnectionRegistry);
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

	public MifidConfig getMifidConfig() {
		return mifidConfig;
	}

	public void setMifidConfig(MifidConfig mifidConfig) {
		this.mifidConfig = mifidConfig;
	}
	
}
