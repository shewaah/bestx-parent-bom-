/**
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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.dao.sql.SqlCSOperationStateAuditDao;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.RateLimiter;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: davide.rossoni
 * Creation date: 18/mar/2015
 * 
 */
public class MyCSPriceService {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(MyCSPriceService.class);

	private static final MetricRegistry metricRegistry = new MetricRegistry();
    private static final Slf4jReporter slf4jReporter = Slf4jReporter.forRegistry(metricRegistry)
            .outputTo(LoggerFactory.getLogger(CommonMetricRegistry.class))
            .convertRatesTo(TimeUnit.MINUTES)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    
    public static void main(String[] args) {
    	double s = 0.5;
    	double x = 60 / s;
    	
    	LOGGER.debug("START");
        
    	slf4jReporter.start(20, TimeUnit.SECONDS);
    	
//    	double ordersPerMinute = x;
    	final Random random = new Random(System.currentTimeMillis());
    	final RateLimiter rateLimiter = RateLimiter.create(x / 60, 1, TimeUnit.SECONDS);
    	
    	
    	new Thread() {
    		
    		@Override
    		public void run() {
    			int multiplier = 1;
    			Timer saveNewBook = metricRegistry.timer(MetricRegistry.name(CSPriceService.class, "saveNewBook"));
    			Counter processed = metricRegistry.counter(MetricRegistry.name(CSPriceService.class, "processed"));
    			long t0 = System.currentTimeMillis();
    			long sleep = 0;
    			
    			double ordersPerMinutes = 120;
    			
    			do {
//    				long waitRateLimiter = (long) (1000 * rateLimiter.acquire());
    				
					long waitRateLimiter = (long) (60000 / ordersPerMinutes);
					if (waitRateLimiter > 10) {
						try { Thread.sleep(waitRateLimiter); } catch (InterruptedException e) { }
					}
					
    				final Timer.Context context = saveNewBook.time();
    				processed.inc();
    				
    				// -- saveNewBook --
    				if (processed.getCount() == 30) {
    					multiplier = 1 + random.nextInt(10);
    					
    					long t1 = (System.currentTimeMillis() - t0);
    					double rate = Math.round((60 * processed.getCount()) / (t1 / 1000.0)); 
    					System.out.println("processed = " + processed.getCount() + " in " + (t1 / 1000.0) + " secs [avg sleep = " + (sleep / processed.getCount()) + " ms] > " + rate + " orders/minute");
    					System.out.println("\n== " + multiplier + " =================================");
    					
    					t0 = System.currentTimeMillis();
    					sleep = 0;
    					processed.dec(processed.getCount());
    					
    					saveNewBook = metricRegistry.timer(MetricRegistry.name(CSPriceService.class, "saveNewBook"));
    	    			processed = metricRegistry.counter(MetricRegistry.name(CSPriceService.class, "processed"));
    					
    					LOGGER.debug("multiplier = {}", multiplier);
    				}
    				
    				long millis = SqlCSOperationStateAuditDao.getLastSaveTimeMillis().get();
//    				long millis = 1 + random.nextInt(200 * multiplier);
    				sleep += millis;
    				
    				try { Thread.sleep(millis); } catch (InterruptedException e) { }
    				context.stop();
    				
    				ordersPerMinutes = Math.min((60 * 1000) / millis, ordersPerMinutes);
//    				rateLimiter.setRate(ordersPerMinutes / 60);
    				LOGGER.debug("{}.\t-> {} ms -> {} orders/min\t[sleep {} ms]", processed.getCount(), millis, ordersPerMinutes, waitRateLimiter);
	                
                } while (true);
    		}
    		
    	}.start();
    	
    	do {
    		try { Thread.sleep(5000); } catch (InterruptedException e) { }
        } while (true);
    }
    
    
}