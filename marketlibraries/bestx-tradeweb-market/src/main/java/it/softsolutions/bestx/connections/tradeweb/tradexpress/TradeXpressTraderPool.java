
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

import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Purpose: this class is mainly for ...   Project Name : bestx-tradeweb-market First created by: anna.cochetti Creation date: 26 nov 2015
 */
public class TradeXpressTraderPool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TradeXpressTraderPool.class);

	ConcurrentMap<String, AtomicInteger> traderCodesPool;
	
	int get(String traderCode) {
		return this.traderCodesPool.get(traderCode).intValue();
	}

	public void init(String traderCodes) {
		traderCodesPool = new ConcurrentHashMap<String, AtomicInteger>();
		StringTokenizer tokenizer = new StringTokenizer(traderCodes, ",");
		String tok = null;
		while(tokenizer.hasMoreTokens()) {
			tok = tokenizer.nextToken().trim();
			traderCodesPool.put(tok, new AtomicInteger(0));
		}
	}

	public String getLessLoadedTraderCode() {
		String traderCode = null;
		int minVal = 99;
		Set<String> keys = this.traderCodesPool.keySet();
		for(String key : keys) {
			int currVal = this.traderCodesPool.get(key).intValue();
			if(currVal < minVal) {
				minVal = currVal;
				traderCode = key;
			}
		}
		return traderCode;
	}
	
	public int incrementAndGetTraderCode(String traderCode) {
		return this.traderCodesPool.get(traderCode).incrementAndGet();
	}

	public int decrementAndGetTraderCode(String traderCode) {
		int ret = this.traderCodesPool.get(traderCode).decrementAndGet();
		if(ret < 0) {
//			this.traderCodesPool.get(traderCode).set(0);
			ret = 0;
		}
		return ret;
	}
}