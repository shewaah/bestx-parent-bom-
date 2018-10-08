/*
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
package it.softsolutions.bestx.management.statistics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.jmx.JMXNotifier;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

/**
 *
 * Purpose: interface to listen to timer events
 *
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation
 * date: 22/nov/2013
 * 
 **/
public class MarketStatistics extends JMXNotifier implements TimerEventListener {

   private static final Logger LOGGER = LoggerFactory.getLogger(MarketStatistics.class);
   public static final Long THRESHOLD_SECONDS_DEFAULT = 30000000L;
   public static final Long INTERVAL_SECONDS_DEFAULT = 30000000L;
   public static String MKT_ORDER_INTERVAL_STATISTICS_LABEL = "MKT_ORDER_INTERVAL_STATS_TIMER";
   public static String MKT_PRICES_INTERVAL_STATISTICS_LABEL = "MKT_PRICES_INTERVAL_STATS_TIMER";
   public static String MKT_EXECUTIONS_INTERVAL_STATISTICS_LABEL = "MKT_EXECUTIONS_INTERVAL_STATS_TIMER";
   public static String LOG_KEY = "[MARKET_STATISTICS]";
   private Long orderThresholdSeconds;
   private Long pricesThresholdSeconds;
   private final ConcurrentMap<String, Long> orderRequestsTimes = new ConcurrentHashMap<>();;
   private final ConcurrentMap<String, Long> pricesRequestsTimes = new ConcurrentHashMap<>();
   private Long orderIntervalTimeInSecs;
   private Long pricesIntervalTimeInSecs;
   private Long executionsIntervalTimeInSecs;

   private final Market.MarketCode marketCode;

   private Histogram orderResponseHistogram = CommonMetricRegistry.INSTANCE.getMonitorRegistry().histogram(MetricRegistry.name(MarketStatistics.class, "orderResponseTime"));
   private Histogram priceDiscoveryHistogram = CommonMetricRegistry.INSTANCE.getMonitorRegistry().histogram(MetricRegistry.name(MarketStatistics.class, "priceDiscoveryTime"));

   private Counter pricesRequested = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(MarketStatistics.class, "pricesRequested"));
   private Counter pricesReceived = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(MarketStatistics.class, "pricesReceived"));

   private StatisticsSnapshot orderResponseTime = new StatisticsSnapshot(orderResponseHistogram);
   private StatisticsSnapshot priceDiscoveryTime = new StatisticsSnapshot(priceDiscoveryHistogram);
   private AtomicInteger executionCount = new AtomicInteger();
   private AtomicInteger unexecutionCount = new AtomicInteger();
   private AtomicReference<Double> executionVolume = new AtomicReference<Double>(Double.valueOf(0.0));

   public MarketStatistics(Long orderThresholdSeconds, Long orderIntervalTimeInSecs, Long pricesThresholdSeconds, Long pricesIntervalTimeInSecs, Long executionsIntervalTimeInSecs,
         Market.MarketCode marketCode){
      this.orderThresholdSeconds = orderThresholdSeconds;
      this.orderIntervalTimeInSecs = orderIntervalTimeInSecs;
      this.pricesThresholdSeconds = pricesThresholdSeconds;
      this.pricesIntervalTimeInSecs = pricesIntervalTimeInSecs;
      this.executionsIntervalTimeInSecs = executionsIntervalTimeInSecs;

      if (this.orderThresholdSeconds == null) {
         this.orderThresholdSeconds = THRESHOLD_SECONDS_DEFAULT;
         LOGGER.error(LOG_KEY + " Order response threshold seconds for Market Statistics not received, applying default value of " + THRESHOLD_SECONDS_DEFAULT + " seconds.");
      }
      if (this.orderIntervalTimeInSecs == null) {
         this.orderIntervalTimeInSecs = INTERVAL_SECONDS_DEFAULT;
         LOGGER.error(LOG_KEY + " Order response interval time in seconds for Market Statistics not received, applying default value of " + INTERVAL_SECONDS_DEFAULT + " seconds.");
      }
      if (this.pricesThresholdSeconds == null) {
         this.pricesThresholdSeconds = THRESHOLD_SECONDS_DEFAULT;
         LOGGER.error(LOG_KEY + " Prices response threshold seconds for Market Statistics not received, applying default value of " + THRESHOLD_SECONDS_DEFAULT + " seconds.");
      }
      if (this.pricesIntervalTimeInSecs == null) {
         this.pricesIntervalTimeInSecs = INTERVAL_SECONDS_DEFAULT;
         LOGGER.error(LOG_KEY + " Prices response interval time in seconds for Market Statistics not received, applying default value of " + INTERVAL_SECONDS_DEFAULT + " seconds.");
      }

      if (this.executionsIntervalTimeInSecs == null) {
         this.executionsIntervalTimeInSecs = INTERVAL_SECONDS_DEFAULT;
         LOGGER.error(LOG_KEY + " Executions response interval time in seconds for Market Statistics not received, applying default value of " + INTERVAL_SECONDS_DEFAULT + " seconds.");
      }

      this.marketCode = marketCode;

      LOGGER.info(LOG_KEY + " Initialized statistics for market " + marketCode + " with threshold " + orderThresholdSeconds + " seconds and interval time " + orderIntervalTimeInSecs + " seconds.");

      try {
         createTimer(MKT_ORDER_INTERVAL_STATISTICS_LABEL + '_' + marketCode, this.orderIntervalTimeInSecs);
         createTimer(MKT_PRICES_INTERVAL_STATISTICS_LABEL + '_' + marketCode, this.pricesIntervalTimeInSecs);
         createTimer(MKT_EXECUTIONS_INTERVAL_STATISTICS_LABEL + '_' + marketCode, this.executionsIntervalTimeInSecs);
      }
      catch (SchedulerException e) {
         LOGGER.error("{} [{}] Error while starting timers: {}", LOG_KEY, marketCode, e.getMessage(), e);
      }

      JobExecutionDispatcher.INSTANCE.addTimerEventListener(MarketStatistics.class.getSimpleName() + '_' + marketCode, this);
   }

   public void createTimer(String timerName, Long timerIntervalInSeconds) throws SchedulerException {
      SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
      String groupName = MarketStatistics.class.getSimpleName() + '_' + marketCode;
      JobDetail newJob = simpleTimerManager.createNewJob(timerName, groupName, false, false, false);
      Trigger trigger = simpleTimerManager.createNewTrigger(timerName, groupName, true, timerIntervalInSeconds * 1000);
      simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
   }

   public void orderSent(String orderId) {
      long now = DateService.currentTimeMillis();
      LOGGER.debug("{} Registering new sent order : {}; time received in milliseconds : {}", LOG_KEY, orderId, now);
      orderRequestsTimes.put(orderId, now);
   }

   public void pricesRequested(String key, int dealerCount) {
      long now = DateService.currentTimeMillis();
      LOGGER.debug("{} Registering new prices request for key : {}; time received in milliseconds : {}", LOG_KEY, key, now);
      pricesRequestsTimes.put(key, now);
      pricesRequested.inc(dealerCount);
   }

   public void orderResponseReceived(String orderId, Double volume) {
      orderResponseReceived(orderId);
      if (volume > 0.0) {
         executionCount.incrementAndGet();
         executionVolume.set(executionVolume.get() + volume);
      }
      else {
         unexecutionCount.incrementAndGet();
      }
   }

   public void orderResponseReceived(String orderId) {
      long sentTime = 0;
      LOGGER.info("{} Managing order response for order {}", LOG_KEY, orderId);
      try {
         if (orderRequestsTimes.containsKey(orderId)) {
            sentTime = orderRequestsTimes.remove(orderId);
         }
         else {
            LOGGER.debug("{} [{}] Order {} not found in the requests sent to the market.", LOG_KEY, marketCode, orderId);
            return;
         }
      }
      catch (Exception e) {
         LOGGER.error("{} [{}] Error while handling response for order {}: {}", LOG_KEY, marketCode, orderId, e.getMessage(), e);
         return;
      }

      long responseTime = DateService.currentTimeMillis();
      long delta = responseTime - sentTime;
      double deltaInSeconds = delta / 1000.0;

      LOGGER.info("[MONITOR] [{}] Execution time in milliseconds: {}", marketCode, delta);
      orderResponseHistogram.update(delta);
      orderResponseTime.setLast(delta);

      // checking the possible threshold break
      if (deltaInSeconds > orderThresholdSeconds) {
         notifyEvent(LOG_KEY + " Market response to order threshold exceeded", "Threshold " + orderThresholdSeconds + " time = " + deltaInSeconds);
      }
   }

   public void pricesResponseReceived(String orderId, int dealerCount) {
      pricesReceived.inc(dealerCount);

      long sentTime = 0;
      try {
         if (pricesRequestsTimes.containsKey(orderId)) {
            sentTime = pricesRequestsTimes.get(orderId);
            pricesRequestsTimes.remove(orderId);
         }
         else {
            LOGGER.debug("{} [{}] Order {} not found in the requests sent to the market.", LOG_KEY, marketCode, orderId);
            return;
         }
      }
      catch (Exception e) {
         LOGGER.error("{} [{}] Error while handling response for order {}: {}", LOG_KEY, marketCode, orderId, e.getMessage(), e);
         return;
      }

      long responseTime = DateService.currentTimeMillis();
      long delta = responseTime - sentTime;
      double deltaInSeconds = delta / 1000.0;

      LOGGER.info("[MONITOR] [{}] OrderId: {} PriceDiscovery time in milliseconds: {}", marketCode, orderId, delta);
      priceDiscoveryHistogram.update(delta);
      priceDiscoveryTime.setLast(delta);

      // checking the possible threshold break
      if (deltaInSeconds > pricesThresholdSeconds) {
         notifyEvent(LOG_KEY + " Market response to prices request threshold exceeded", "Threshold " + pricesThresholdSeconds + " time = " + deltaInSeconds);
      }
   }

   @Override
   public void timerExpired(String jobName, String groupName) {

      if (jobName.equals(MKT_ORDER_INTERVAL_STATISTICS_LABEL + '_' + marketCode)) {
         LOGGER.info("{} [{}] Order Response time, {}", LOG_KEY, marketCode, orderResponseTime);
      }

      if (jobName.equals(MKT_PRICES_INTERVAL_STATISTICS_LABEL + '_' + marketCode)) {
         LOGGER.info("{} [{}] Price Discovery time, {}", LOG_KEY, marketCode, priceDiscoveryTime);
      }

      if (jobName.equals(MKT_EXECUTIONS_INTERVAL_STATISTICS_LABEL + '_' + marketCode)) {
         LOGGER.info("{} [{}] Execution Counts = {}, execution Volume = {}, execution Ratio = {}, unexecutionCount = {}", LOG_KEY, marketCode, executionCount, this.getExecutionRatio(),
               executionVolume.get(), unexecutionCount);
         notifyEvent(LOG_KEY + "Execution Statistics", "Execution Counts = " + executionCount + ", execution Volume = " + executionVolume.get() + ", execution Ratio = " + this.getExecutionRatio()
               + ", unexecutionCount = " + unexecutionCount);
      }

   }

   public StatisticsSnapshot getOrderResponseTime() {
      return orderResponseTime;
   }

   public StatisticsSnapshot getPriceDiscoveryTime() {
      return priceDiscoveryTime;
   }

   //	public AtomicInteger getExecutionCount() {
   //		return executionCount;
   //	}
   //
   //	public AtomicInteger getUnexecutionCount() {
   //		return unexecutionCount;
   //	}

   public void timerExpired() {

   }

   public int getExecutionCount() {
      return executionCount.get();
   }

   public int getUnexecutionCount() {
      return unexecutionCount.get();
   }

   public double getExecutionVolume() {
      return executionVolume.get();
   }

   public double getExecutionRatio() {
      int executed = this.getExecutionCount();
      int unexecuted = this.getUnexecutionCount();
      return (executed == 0) ? 0.0 : (double) executed / (unexecuted + executed);
   }

}
