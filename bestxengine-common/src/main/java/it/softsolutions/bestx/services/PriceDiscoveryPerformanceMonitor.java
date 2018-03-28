package it.softsolutions.bestx.services;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
public class PriceDiscoveryPerformanceMonitor {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(PriceDiscoveryPerformanceMonitor.class);
	
	public static class Chrono {

		private long startMS;
		private long stopMS;
		private long startNS;
		private long stopNS;
		
		private long stepStartMS;
		private long stepStartNS;
		
		/**
		 * Constructor
		 */
		public Chrono(){
			start();
		}
		
		/**
		 * start
		 */
		public void start(){
			startMS = System.currentTimeMillis();
			startNS = System.nanoTime();
			stepStartMS = System.currentTimeMillis();
			stepStartNS = System.nanoTime();
		}

		/**
		 * stop
		 */
		public String stop(){
			stopMS = System.currentTimeMillis();
			stopNS = System.nanoTime();
			return this.toString();
		}
		
		/**
	    * step
	    */
	   public String step(){
		  String value = "Chrono time: " + (System.currentTimeMillis() - stepStartMS) + "ms. [" + (System.nanoTime() - stepStartNS) + "ns.]";
		  stepStartMS = System.currentTimeMillis();
		  stepStartNS = System.nanoTime();
	      return value;
	   }

		/**
		 * get time milliseconds
		 * @return
		 */
		public long getTimeMS(){
			return stopMS - startMS;
		}
		
		/**
		 * Get time nanosecond
		 * @return
		 */
		public long getTimeNS(){
			return stopNS - startNS;
		}
		
		/**
		 * To string
		 */
		public String toString(){
			return "Chrono time: " + getTimeMS() + "ms. [" + getTimeNS() + "ns.]";
		}


	}

	
	static HashMap<String, Chrono> chronoMap = new HashMap<String, Chrono>();


	public static void logEvent(String key, String logMessage) {
		if (!LOGGER.isTraceEnabled()) return;
		Chrono chrono = chronoMap.get(key);
		if (chrono==null) {
			chrono = new Chrono();
			chronoMap.put(key, chrono);
		}
		LOGGER.trace("{}|{}|{}|{}", DateService.currentTimeMillis(), key, chrono.step(), logMessage);
	}
	
	public static void finalize(String key, String logMessage) {
		if (!LOGGER.isTraceEnabled()) return;
		Chrono chrono = chronoMap.remove(key);
		if (chrono==null) {
			chrono = new Chrono();
		}
		LOGGER.trace("{}|{}|{}|{}",DateService.currentTimeMillis(), key, chrono.stop(), logMessage);
	}

}
