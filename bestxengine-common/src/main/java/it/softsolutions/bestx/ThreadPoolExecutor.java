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
package it.softsolutions.bestx;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: davide.rossoni 
 * Creation date: 03/ott/2013 
 * 
 **/
public class ThreadPoolExecutor implements Executor {

   protected int corePoolSize;
   protected int maxPoolSize;
   protected String threadNamePrefix;
   protected java.util.concurrent.ThreadPoolExecutor workerPool;

   public ThreadPoolExecutor(){}

   @Override
   public void execute(Runnable command) {
      workerPool.execute(command);
   }

   /**
    * @return
    */
   public int getMaxPoolSize() {
      return workerPool.getMaximumPoolSize();
   }

   /**
    * @return
    */
   public int getActiveCount() {
      return workerPool.getActiveCount();
   }

   /**
    * @return
    */
   public int getPoolSize() {
      return workerPool.getPoolSize();
   }

   /**
    * 
    */
   public void shutdown() {
      workerPool.shutdown();
   }

   /**
    * @return the corePoolSize
    */
   public int getCorePoolSize() {
      return corePoolSize;
   }

   /**
    * @param corePoolSize the corePoolSize to set
    */
   public void setCorePoolSize(int corePoolSize) {
      this.corePoolSize = corePoolSize;
   }

   /**
    * @return the threadNamePrefix
    */
   public String getThreadNamePrefix() {
      return threadNamePrefix;
   }

   /**
    * @param threadNamePrefix the threadNamePrefix to set
    */
   public void setThreadNamePrefix(String threadNamePrefix) {
      this.threadNamePrefix = threadNamePrefix;
   }

   /**
    * @param maxPoolSize the maxPoolSize to set
    */
   public void setMaxPoolSize(int maxPoolSize) {
      this.maxPoolSize = maxPoolSize;
   }

   public void initialize() {
      workerPool = new java.util.concurrent.ThreadPoolExecutor(corePoolSize, maxPoolSize, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new BestXThreadFactory());

      try {
         CommonMetricRegistry.INSTANCE.getMonitorRegistry().register(MetricRegistry.name(ThreadPoolExecutor.class, threadNamePrefix, "activeCount"), new Gauge<Integer>() {

            @Override
            public Integer getValue() {
               return workerPool.getActiveCount();
            }
         });

         CommonMetricRegistry.INSTANCE.getMonitorRegistry().register(MetricRegistry.name(ThreadPoolExecutor.class, threadNamePrefix, "queueSize"), new Gauge<Integer>() {

            @Override
            public Integer getValue() {
               return workerPool.getQueue().size();
            }
         });

      }
      catch (IllegalArgumentException e) {}
   }

   class BestXThreadFactory implements ThreadFactory {
      final AtomicLong count = new AtomicLong(0);
      
      public Thread newThread(Runnable r) {
         Thread t = new Thread(r);
         t.setName(threadNamePrefix + "-T" + count.getAndIncrement());

         return t;
      }
   }
}
