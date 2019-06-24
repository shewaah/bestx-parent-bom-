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
package it.softsolutions.bestx.connections.fixgateway.statistics;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class monitors some dimensions for the incoming orders queue:
 * <ul>
 * <li>msg/sec in</li>
 * <li>msg/sec out</li> 
 * <li>queue size</li> 
 * </ul> 
 *
 * Project Name : bestxengine-common 
 * First created by: ruggero.rizzo 
 * Creation date: 08/lug/2013 
 * 
 **/
public class FixOrderQueueMonitor implements Runnable{
    private static final Logger LOGGER = LoggerFactory.getLogger(FixOrderQueueMonitor.class);
    AtomicInteger inMsgPerSecond;
    AtomicInteger outMsgPerSecond;
    AtomicInteger queueSize;
    AtomicInteger ackSent;
    long dumpIntervalMilliseconds;
    private static FixOrderQueueMonitor instance = null;
    
    private FixOrderQueueMonitor(long dumpIntervalMilliseconds){
        this.dumpIntervalMilliseconds = dumpIntervalMilliseconds;
        inMsgPerSecond = new AtomicInteger(0);
        outMsgPerSecond = new AtomicInteger(0);
        queueSize = new AtomicInteger(0);
        ackSent = new AtomicInteger(0);
    }
    
    public synchronized static FixOrderQueueMonitor getInstance(long dumpIntervalMilliseconds){
        if (instance == null){
            instance = new FixOrderQueueMonitor(dumpIntervalMilliseconds);
        }
        return instance;
    }
    
    public static FixOrderQueueMonitor getInstance(){
        return getInstance(5000);
    }
    
    @Override
    public void run() {
        do {
            try {
                Thread.sleep(dumpIntervalMilliseconds);
            } catch (InterruptedException e) {
                LOGGER.error("Unexpected sleep interruption", e);
            }
            LOGGER.trace("ORDERS QUEUE STATISTICS: msg/sec in {} - out {}, queue size {}, ack sent {}. Printed every {} milliseconds.", inMsgPerSecond, outMsgPerSecond, queueSize, ackSent, dumpIntervalMilliseconds);
            inMsgPerSecond.lazySet(0);
            outMsgPerSecond.lazySet(0);
            ackSent.lazySet(0);
        } while (true);
        
    }
    
    public void newInMsg(){
        inMsgPerSecond.incrementAndGet();
        queueSize.incrementAndGet();
    }
    
    public void newOutMsg(){
        outMsgPerSecond.incrementAndGet();
        queueSize.decrementAndGet();
    }
    
    public void newAckSent(){
        ackSent.incrementAndGet();
    }
}
