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
package it.softsolutions.bestx.finders;

import it.softsolutions.bestx.CommonMetricRegistry;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Instrument;

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
public class MyInstrumentFinder {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(MyInstrumentFinder.class);
	private static final Random random = new Random(System.currentTimeMillis());
	
	private static InstrumentFinder instrumentFinder;
    private static ClassPathXmlApplicationContext context;
	
	public void init() {
		CommonMetricRegistry.INSTANCE.init(5, 5, TimeUnit.SECONDS);
		
		context = new ClassPathXmlApplicationContext("cs-spring.xml");
		instrumentFinder = (InstrumentFinder) context.getBean("dbBasedInstrumentFinder");
	}
	
    public void run() {
		final String[] isins = new String[] {"US515110BB91", "US676167BB44", "US912828KM16", "XS0055498413", "XS0138261457", "XS0169888558", "XS0170558877", "XS0176823424", "XS0196448129", "XS0196506694", "XS0196630270", "XS0197079972", "XS0197508764", "XS0197646218", "XS0200848041", "XS0201168894", "XS0202043898", "XS0202649934", "XS0203685788", "XS0203712939", "XS0203714802", "XS0204395213", "XS0205790214", "XS0207037507", "XS0207065110", "XS0210314299", "XS0210318795", "XS0210515788", "XS0210629522", "XS0211034466", "XS0211034540", "XS0212170939", "XS0212249014", "XS0212342066", "XS0212401920", "XS0212420987", "XS0213101230", "XS0213706905", "XS0214238239", "XS0214318007", "XS0214851874", "XS0214965534", "XS0215093534", "XS0215153296", "XS0215159731", "XS0215498782"};
		final AtomicInteger count = new AtomicInteger();
		
		for (int i = 0; i < 10; i++) {
			new Thread() {
				
				@Override
				public void run() {
					
					long t0 = System.currentTimeMillis();
					
					do {
						Instrument instrument = instrumentFinder.getInstrumentByIsin(isins[random.nextInt(isins.length)]);
						LOGGER.info("XXXX {}", instrument);
						
						try { Thread.sleep(random.nextInt(10)); } catch (InterruptedException e) { }
						
					} while (count.incrementAndGet() < 10000);
					
					System.out.println(count.get() + " cycles in " + (System.currentTimeMillis() - t0) + " ms");
				}
				
			}.start();
        }
		

	}

	public static void main(String[] args) throws InterruptedException {
		MyInstrumentFinder myInstrumentFinder = new MyInstrumentFinder();
		myInstrumentFinder.init();
		myInstrumentFinder.run();
    }
}
