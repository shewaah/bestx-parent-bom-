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
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-10-06 06:54:25 $
 * Header       : $Id: MarketCommon.java,v 1.1 2010-10-06 06:54:25 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/markets/MarketCommon.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.markets;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.management.statistics.MarketStatistics;
import it.softsolutions.bestx.management.statistics.StatisticsSnapshot;

public abstract class MarketCommon extends MarketConnection implements MarketMXBean {
    
    protected MarketStatistics marketStatistics;
    protected Long orderRespThresholdSeconds;
    protected Long orderRespIntervalTimeInSecs;
    protected Long pricesRespThresholdSeconds;
    protected Long pricesRespIntervalTimeInSecs;
    protected Long executionsRespIntervalTimeInSecs;
    protected Double executionsVolume;
    protected Double executionsRatio;

    public MarketCommon() {
        super();
    }

    protected void init() throws BestXException {
        marketStatistics = new MarketStatistics(orderRespThresholdSeconds, orderRespIntervalTimeInSecs, pricesRespThresholdSeconds, pricesRespIntervalTimeInSecs, executionsRespIntervalTimeInSecs,
                getMarketCode());
    }

    public MarketStatistics getMarketStatistics() {
        return marketStatistics;
    }

    public void setMarketStatistics(MarketStatistics marketStatistics) {
        this.marketStatistics = marketStatistics;
    }

    public long getOrderRespThresholdSeconds() {
        return orderRespThresholdSeconds;
    }

    public void setOrderRespThresholdSeconds(long orderRespThresholdSeconds) {
        this.orderRespThresholdSeconds = orderRespThresholdSeconds;
    }

    public long getOrderRespIntervalTimeInSecs() {
        return orderRespIntervalTimeInSecs;
    }

    public void setOrderRespIntervalTimeInSecs(long orderRespintervalTimeInSecs) {
        this.orderRespIntervalTimeInSecs = orderRespintervalTimeInSecs;
    }

    public Long getPricesRespIntervalTimeInSecs() {
        return pricesRespIntervalTimeInSecs;
    }

    public void setPricesRespIntervalTimeInSecs(Long pricesRespIntervalTimeInSecs) {
        this.pricesRespIntervalTimeInSecs = pricesRespIntervalTimeInSecs;
    }

    public Long getPricesRespThresholdSeconds() {
        return pricesRespThresholdSeconds;
    }

    public void setPricesRespThresholdSeconds(Long pricesRespThresholdSeconds) {
        this.pricesRespThresholdSeconds = pricesRespThresholdSeconds;
    }

    public Long getExecutionsRespIntervalTimeInSecs() {
        return executionsRespIntervalTimeInSecs;
    }

    public void setExecutionsRespIntervalTimeInSecs(Long executionsRespIntervalTimeInSecs) {
        this.executionsRespIntervalTimeInSecs = executionsRespIntervalTimeInSecs;
    }
    
    @Override
	public StatisticsSnapshot getPriceDiscoveryTime() {
		return marketStatistics.getPriceDiscoveryTime();
	}
	
    @Override
    public StatisticsSnapshot getOrderResponseTime() {
		return marketStatistics.getOrderResponseTime();
	}
    
    @Override
    public long getExecutionCount() {
		return marketStatistics.getExecutionCount();
	}
    
    @Override
    public long getUnexecutionCount() {
		return marketStatistics.getUnexecutionCount();
	}
    @Override
    public double getExecutionVolume() {
		return marketStatistics.getExecutionVolume();
	}
    @Override
    public double getExecutionRatio() {
		return marketStatistics.getExecutionRatio();
	}
}