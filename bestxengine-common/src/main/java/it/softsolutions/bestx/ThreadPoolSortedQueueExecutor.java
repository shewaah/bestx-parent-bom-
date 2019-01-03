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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Purpose: this class has been created to grant correct sequencing in managing of marketEvents 
 *
 * Project Name : bestxengine-common 
 * First created by: anna.cochetti 
 * Creation date: 08/dic/2018 
 * 
 **/
public class ThreadPoolSortedQueueExecutor extends ThreadPoolExecutor implements Executor, Runnable {
    private LinkedBlockingQueue<Runnable> runnableQueue;
    private Thread this_thread;
    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolSortedQueueExecutor.class);
	
	public ThreadPoolSortedQueueExecutor() {
	}

	@Override
	public void initialize() {
		super.initialize();
		runnableQueue = new LinkedBlockingQueue<Runnable>();
        start();
	}


    /**
     * start method of the thread
     */
    public void start() {
        if (this_thread == null) {
            this_thread = new Thread(this, threadNamePrefix);
            this_thread.start();
        }
    }

    /**
     * stop method of the thread
     */
    public void stop() {
        Thread old = this_thread;
        this_thread = null;
        old.interrupt();
    }

    public void run() {
        while (this_thread == Thread.currentThread()) {
            try {
                workerPool.execute(runnableQueue.take());
                LOGGER.debug("Queue size: {}", runnableQueue.size());
            } catch (InterruptedException e) {
                LOGGER.debug("BloombergConnector - Forced queue wait interruption.");
            } catch (Exception ex) {
                LOGGER.error("BloombergConnector - Error: ", ex);
            }
        }
    }

    @Override
    public void execute(Runnable runnable) {
        LOGGER.debug("Enqueued runnable: {}", runnable.toString());
        if (!runnableQueue.offer(runnable)) {
            LOGGER.error("runnable queue is full!");
        }
    }

}

