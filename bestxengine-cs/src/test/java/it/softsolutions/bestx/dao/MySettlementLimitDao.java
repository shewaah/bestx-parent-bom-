/**
 * Copyright 1997-2014 SoftSolutions! srl 
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
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.model.SettlementLimit;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: davide.rossoni
 * Creation date: 21/gen/2014
 * 
 */
public class MySettlementLimitDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(MySettlementLimitDao.class);
	private static final Random random = new Random(System.currentTimeMillis());
	
	private static SettlementLimitDao settlementLimitDao;
    private static ClassPathXmlApplicationContext context;
	
	public void init() {
		CommonMetricRegistry.INSTANCE.init(5, 5, TimeUnit.SECONDS);
		
		context = new ClassPathXmlApplicationContext("cs-spring.xml");
		settlementLimitDao = (SettlementLimitDao) context.getBean("hibernateSettlementLimitDao");
	}
	
    public void run() {
		final AtomicInteger count = new AtomicInteger();
		
		final String currency = "EUR";
		final String countryCode = "IT";
		
		for (int i = 0; i < 10; i++) {
			new Thread() {
				
				@Override
				public void run() {
					
					long t0 = System.currentTimeMillis();
					
					do {
						try { Thread.sleep(random.nextInt(10)); } catch (InterruptedException e) { }
						
						SettlementLimit res = settlementLimitDao.getValidFilteredLimit(currency, countryCode);
						LOGGER.info("XXXX {}", res);
						
					} while (count.incrementAndGet() < 1000);
					
					System.out.println(count.get() + " cycles in " + (System.currentTimeMillis() - t0) + " ms");
				}
				
			}.start();
        }
	}

	public static void main(String[] args) throws InterruptedException {
		MySettlementLimitDao mySettlementLimitDao = new MySettlementLimitDao();
		mySettlementLimitDao.init();
		mySettlementLimitDao.run();
    }
}
