
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
 
package it.softsolutions.bestx.connections.tradeweb.tradexpress;

import static org.junit.Assert.assertEquals;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestx-tradeweb-market
 * First created by: anna.cochetti
 * Creation date: 26 nov 2015
 * 
 **/

public class TradeXpressTraderPoolTest {

	private TradeXpressTraderPool traderPool;
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		traderPool = new TradeXpressTraderPool();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressTraderPool#init(java.lang.String)}.
	 */
	@Test
	public final void testInit() {
		traderPool.init("pippo,pluto,paperino,topolino");
		ConcurrentHashMap<String, AtomicInteger> expectedMap = new ConcurrentHashMap<String, AtomicInteger>();
		expectedMap.put("pippo", new AtomicInteger(0));
		expectedMap.put("pluto", new AtomicInteger(0));
		expectedMap.put("paperino", new AtomicInteger(0));
		expectedMap.put("topolino", new AtomicInteger(0));
		assertEquals(expectedMap.toString(), traderPool.traderCodesPool.toString());
	}

	/**
	 * Test method for {@link it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressTraderPool#getLessLoadedTraderCode()}.
	 */
	@Test
	public final void testGetLessLoadedTraderCode() {
		traderPool.init("pippo,pluto,paperino,topolino");
		traderPool.incrementAndGetTraderCode("pippo");
		traderPool.incrementAndGetTraderCode("pippo");
		traderPool.incrementAndGetTraderCode("pluto");
		traderPool.incrementAndGetTraderCode("paperino");
		assertEquals("topolino", traderPool.getLessLoadedTraderCode());
	}


	/**
	 * Test method for {@link it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressTraderPool#decrementAndGetTraderCode(java.lang.String)}.
	 */
	@Test
	public final void testDecrementAndGetTraderCode() {
		traderPool.init("pippo,pluto,paperino,topolino");
		traderPool.incrementAndGetTraderCode("pippo");
		traderPool.incrementAndGetTraderCode("pippo");
		traderPool.incrementAndGetTraderCode("pluto");
		traderPool.incrementAndGetTraderCode("paperino");
		assertEquals("topolino", traderPool.getLessLoadedTraderCode());
		traderPool.decrementAndGetTraderCode("pippo");
		traderPool.decrementAndGetTraderCode("pippo");
		traderPool.incrementAndGetTraderCode("topolino");
		assertEquals("pippo", traderPool.getLessLoadedTraderCode());
	}

	AtomicInteger pippo= new AtomicInteger(0);
	AtomicInteger pluto = new AtomicInteger(0);
	AtomicInteger paperino = new AtomicInteger(0);
	AtomicInteger topolino = new AtomicInteger(0);
	AtomicInteger disney [] = {pippo, pluto, paperino, topolino};
	int maxThreads = 500;
	Random random = new Random();
	CountDownLatch monitor = new CountDownLatch(maxThreads - 1);

	/**
	 * Test method for {@link it.softsolutions.bestx.connections.tradeweb.tradexpress.TradeXpressTraderPool#getLessLoadedTraderCode()}.
	 */
	@Test
	public final void testThreadSafeGetLessLoadedTraderCode() {
		traderPool.init("pippo,pluto,paperino,topolino");
		ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
		for (int i = 1; i <= maxThreads; i++) executor.execute(new IncrementOrDecrement(i));
		try {
			monitor.await();
		} catch (InterruptedException e) {
			;
		}
		System.out.println("traderCodePool is: " + traderPool.traderCodesPool.entrySet());
		int i = getMin(disney);
		String trader= null;
		switch(i) {
			case 0:
				trader = "pippo";
				break;
			case 1:
				trader = "pluto";
				break;
			case 2:
				trader = "paperino";
				break;
			case 3:
				trader = "topolino";
				break;
		}
		System.out.println("Selected trader is " + trader);
		if(traderPool != null)
			assertEquals(traderPool.get(trader), traderPool.get(traderPool.getLessLoadedTraderCode()));
	}
	
	private int getMin(AtomicInteger array []) {
		int min = 9999;
		int index = -1;
		for(int i = 0; i < array.length; i++) {
			if(array[i].intValue() < min ) {
				min = array[i].intValue();
				index = i;
			}
		}
		return index;
	}
	
	private class IncrementOrDecrement implements Runnable {
		int i;
		IncrementOrDecrement(int threadNo) {
			this.threadNo = threadNo;
		}
		
		int threadNo;
		@Override
		public void run() {
			synchronized(random) {
				i = random.nextInt(8);
			}
			switch(i) {
			case 0:
				traderPool.incrementAndGetTraderCode("pippo");
				disney[i].incrementAndGet();
				break;
			case 1:
				traderPool.incrementAndGetTraderCode("pluto");
				disney[i].incrementAndGet();
				break;
			case 2:
				traderPool.incrementAndGetTraderCode("paperino");
				disney[i].incrementAndGet();
				break;
			case 3:
				traderPool.incrementAndGetTraderCode("topolino");
				disney[i].incrementAndGet();
				break;
			case 4:
				traderPool.decrementAndGetTraderCode("pippo");
				disney[i-4].decrementAndGet();
				break;
			case 5:
				traderPool.decrementAndGetTraderCode("pluto");
				disney[i-4].decrementAndGet();
				break;
			case 6:
				traderPool.decrementAndGetTraderCode("paperino");
				disney[i-4].decrementAndGet();
				break;
			case 7:
				traderPool.decrementAndGetTraderCode("topolino");
				disney[i-4].decrementAndGet();
				break;
			default:
				System.out.println("threadNo: " + threadNo + " i not in range: " + i);
			}
			monitor.countDown();
		}
	}
}
