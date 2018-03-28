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
 
package it.softsolutions.bestx.services;

import java.util.List;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.jmx.JMXNotifier;
import it.softsolutions.bestx.management.BusinessValuesMonitorMBean;
import it.softsolutions.bestx.management.statistics.MarketStatistics;
import it.softsolutions.bestx.markets.MarketCommon;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;


/**  
*
* Purpose: this class is mainly for pubblicate business statistics on JMX  
*
* Project Name : bestxengine-cs 
* First created by: stefano.pontillo 
* Creation date: 27 set 2017 
* 
**/
public class BusinessValuesMonitor extends JMXNotifier implements TimerEventListener, BusinessValuesMonitorMBean {


   private static final Logger LOGGER = LoggerFactory.getLogger(BusinessValuesMonitor.class);
   public static String LOG_KEY = "[BUSINESS_STATISTICS]";
   
   private List<MarketCommon> marketList;
   private Long statsIntervalTimeInSecs;
   
   private double totalExecutedVolume;
   private double executedPercentage;
   private double totalOrders;
   private double executedOrders;
   
   public static String BIZ_MON_INTERVAL_STATISTICS_LABEL = "BIZ_MON_INTERVAL_STATS_TIMER";
   
   public void init() {
      try {
         createTimer(BIZ_MON_INTERVAL_STATISTICS_LABEL, this.statsIntervalTimeInSecs);
      } catch (SchedulerException e) {
         LOGGER.error("{} Error while starting timers: {}", LOG_KEY, e.getMessage(), e);
      }
      JobExecutionDispatcher.INSTANCE.addTimerEventListener(BusinessValuesMonitor.class.getSimpleName(), this);
   }
   
   /* (non-Javadoc)
    * @see it.softsolutions.bestx.services.timer.quartz.TimerEventListener#timerExpired(java.lang.String, java.lang.String)
    */
   @Override
   public void timerExpired(String jobName, String groupName) {
      totalExecutedVolume = 0.0;
      executedPercentage = 0.0;
      totalOrders = 0.0;
      executedOrders = 0.0;
      
      for (MarketCommon ms : marketList) {
         MarketStatistics stats = ms.getMarketStatistics();
         
         totalExecutedVolume += stats.getExecutionVolume();
         totalOrders += stats.getUnexecutionCount() + stats.getExecutionCount();
         executedOrders += stats.getExecutionCount();
      }
      //Send JMX notification
      if (totalOrders > 0.0) {
         executedPercentage = executedOrders / totalOrders;
         notifyEvent("Update", "executedPercentage = " + executedPercentage + " totalVolume = " + totalExecutedVolume);
      }
   }

   public void createTimer(String timerName, Long timerIntervalInSeconds) throws SchedulerException {
      SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
      String groupName = BusinessValuesMonitor.class.getSimpleName();
      JobDetail newJob = simpleTimerManager.createNewJob(timerName, groupName, false, false, false);
      Trigger trigger = simpleTimerManager.createNewTrigger(timerName, groupName, true, timerIntervalInSeconds * 1000);
      simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
   }
   
   public void setMarketList(List<MarketCommon> marketList) {
      this.marketList = marketList;
   }

   /**
    * @param statsIntervalTimeInSecs the statsIntervalTimeInSecs to set
    */
   public void setStatsIntervalTimeInSecs(Long statsIntervalTimeInSecs) {
      this.statsIntervalTimeInSecs = statsIntervalTimeInSecs;
   }

   @Override
   public double getExecutionVolume() {
      return totalExecutedVolume;
   }


   @Override
   public double getExecutionRatio() {
      return executedPercentage;
   }

   @Override
   public double getTotalOrders() {
      return totalOrders;
   }

   @Override
   public double getExecutedOrders() {
      return executedOrders;
   }
}
