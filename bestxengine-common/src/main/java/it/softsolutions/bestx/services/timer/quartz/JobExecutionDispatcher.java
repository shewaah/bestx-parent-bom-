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
package it.softsolutions.bestx.services.timer.quartz;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import it.softsolutions.bestx.CommonMetricRegistry;

/**
 * 
 * Purpose: received job execution notifications and dispatch it to the task that must be executed
 * 
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation date: 22/nov/2013
 * 
 **/
public enum JobExecutionDispatcher {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(JobExecutionDispatcher.class);
    
    private Counter timerExpired = CommonMetricRegistry.INSTANCE.getMonitorRegistry().counter(MetricRegistry.name(JobExecutionDispatcher.class, "timerExpired"));
    private ConcurrentMap<String, TimerEventListener> listenersMap = new ConcurrentHashMap<String, TimerEventListener>(4);

    public void notifyJobExecution(JobExecutionContext context, JobExecutionException jobExecutionException, boolean monitorable) {
        String groupName = context.getJobDetail().getKey().getGroup();
        String jobName = context.getJobDetail().getKey().getName();
        TimerEventListener timerEventListener = listenersMap.get(groupName);
        if (timerEventListener == null) {
            LOGGER.error("Cannot find a listener for the group {}", groupName);
        } else {
            timerEventListener.timerExpired(jobName, groupName);
            
            if (monitorable) {
            	timerExpired.inc();
            }
        }
    }

    public void addTimerEventListener(String key, TimerEventListener timerEventListener) {
        if ( (key != null) && (!key.isEmpty()) && (timerEventListener != null) ) {
            listenersMap.put(key, timerEventListener);
        }
    }
    public void removeTimerEventListener(String key) {
        if ( (key != null) && (!key.isEmpty()) ) {
            listenersMap.remove(key);
        }
    }
    
}
