/**
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
package it.softsolutions.bestx.management;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.management.statistics.StatisticsSnapshot;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: davide.rossoni Creation date: 06/nov/2013
 * 
 */
public interface MarketMXBean {

	// BuySide Connection
	void startBuySideConnection() throws BestXException;
	void stopBuySideConnection() throws BestXException;
	void enableBuySideConnection();
	void disableBuySideConnection();
	boolean isBuySideConnectionEnabled();
	boolean isBuySideConnectionAvailable();

	// Price Connection
	void startPriceConnection() throws BestXException;
	void stopPriceConnection() throws BestXException;
	void enablePriceConnection();
	void disablePriceConnection();
	boolean isPriceConnectionEnabled();
	boolean isPriceConnectionAvailable();
	
	void cleanBook();
	int countOrders();
	
	int getActiveTimersNum();
	
	StatisticsSnapshot getPriceDiscoveryTime();
	StatisticsSnapshot getOrderResponseTime();
	
	long getExecutionCount();
	long getUnexecutionCount();
	double getExecutionVolume();
	double getExecutionRatio();

}
