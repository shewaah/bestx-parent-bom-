/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.services.logutils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;

import it.softsolutions.bestx.services.DateService;

public class ApplicationMonitor extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMonitor.class);

    public static final long DEFAULT_SAMPLE_TIME = 60000;

    private boolean shutdownRequired_ = false;
    private long sampleTime_ = DEFAULT_SAMPLE_TIME;
    private JavaSysMon monitor_;
    private CpuTimes previousCPUTimes_;

    private static Map<String, Long> waitingPriceQueuesSize = new ConcurrentHashMap<String, Long>();
    private static Map<String, Queue<Long>> inputOrdersAndTimesMap = new ConcurrentHashMap<String, Queue<Long>>();
    private static Map<String, Queue<Long>> outputOrdersAndTimesMap = new ConcurrentHashMap<String, Queue<Long>>();

    public ApplicationMonitor(long sampleTime) {
        LOGGER.info("Creating ApplicationMonitor with sample time " + sampleTime + " ms.");
        setSampleTime(sampleTime);
        this.setDaemon(true);
        this.setName("APPMon-" + "-T" + this.getId());
        monitor_ = new JavaSysMon();
        LOGGER.info("ApplicationMonitor ready to start.");
    }

    public ApplicationMonitor() {
        this(DEFAULT_SAMPLE_TIME);
    }

    public long getSampleTime() {
        return sampleTime_;
    }

    public void setSampleTime(long sampleTime) {
        sampleTime_ = sampleTime;
    }

    public void run() {
        LOGGER.info("ApplicationMonitor starting...");
        previousCPUTimes_ = null;
        shutdownRequired_ = false;
        dumpSystemInfo();
        while (!shutdownRequired_) {
            try {
                dumpCurrentInfo();
                sleep(sampleTime_);
            } catch (Exception ex) {
                LOGGER.error("ApplicationMonitor error: " + ex.getMessage());
            }
        }
    }

    protected void dumpCurrentInfo() {
        ApplicationStatisticsHelper.logRealtimeSystemInfo(buildRealtimeStats());
    }

    protected void dumpSystemInfo() {
        ApplicationStatisticsHelper.logPlatformInfo(buildPlatformInfo());
    }

    public void shutdown() {
        shutdownRequired_ = true;
    }

    protected String buildRealtimeStats() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("queue:").append(buildQueueStats());
        sb.append(", input_throughput:").append(buildInputThroughputStats(60));
        sb.append(", output_throughput:").append(buildOutputThroughputStats(60));
        sb.append(", memory:").append(buildMemoryStats());
        sb.append(", cpu:").append(buildCPUStats());
        sb.append(", timestamp:").append(System.nanoTime());
        sb.append(", time:").append(DateService.currentTimeMillis());
        sb.append('}');
        return sb.toString();
    }

    protected String buildPlatformInfo() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("osName:'").append(monitor_.osName()).append("',");
        sb.append("', cpuFrequencyInHz:").append(new BigDecimal(monitor_.cpuFrequencyInHz()));
        sb.append(", currentPid:").append(monitor_.currentPid());
        sb.append(", numCpus:").append(monitor_.numCpus());
        sb.append(", uptimeInSeconds:").append(monitor_.uptimeInSeconds());
        sb.append(", osArch:'").append(System.getProperty("os.arch"));
        sb.append("', osVersion:'").append(System.getProperty("os.version"));
        sb.append("', javaVersion:'").append(System.getProperty("java.version"));
        sb.append("', javaVendor:'").append(System.getProperty("java.vendor"));
        sb.append("', memory:").append(buildMemoryStats());
        sb.append(", cpu:").append(buildCPUStats());
        sb.append(", timestamp:").append(System.nanoTime());
        sb.append('}');
        return sb.toString();
    }

    protected String buildMemoryStats() {
        MemoryStats memStats = monitor_.physical();

        StringBuilder sb = new StringBuilder("{");
        sb.append("freeBytes:").append(memStats.getFreeBytes());
        sb.append(", totalBytes:").append(memStats.getTotalBytes());
        sb.append('}');
        return sb.toString();
    }

    protected String buildQueueStats() {
        StringBuilder sb = new StringBuilder("{");

        for (String queueName : waitingPriceQueuesSize.keySet()) {
            sb.append(queueName).append(':');
            sb.append(waitingPriceQueuesSize.get(queueName));
            sb.append(',');
        }
        if (sb.substring(sb.length() - 1).equals(",")) {
            sb.deleteCharAt(sb.length() - 1); 
        }
        sb.append('}');
        return sb.toString();
    }

    protected String buildInputThroughputStats(int timeWindowSeconds) {
        StringBuilder sb = new StringBuilder("{");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -timeWindowSeconds);
        long maxValidTime = calendar.getTime().getTime();

        for (String inputConnectionName : inputOrdersAndTimesMap.keySet()) {
            Queue<Long> orderTimes = inputOrdersAndTimesMap.get(inputConnectionName);
            Long orderTime = null;
            orderTime = orderTimes.peek();
            while ((orderTime != null) && (orderTime < maxValidTime)) {
                orderTimes.poll();
                orderTime = orderTimes.peek();
            }

            sb.append(inputConnectionName).append(':');
            sb.append(orderTimes.size());
            sb.append(',');
        }
        if (sb.substring(sb.length() - 1).equals(",")) {
            sb.deleteCharAt(sb.length() - 1); 
        }
        sb.append('}');
        return sb.toString();
    }

    protected String buildOutputThroughputStats(int timeWindowSeconds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -timeWindowSeconds);
        long maxValidTime = calendar.getTime().getTime();

        for (String outputConnectionName : outputOrdersAndTimesMap.keySet()) {
            Queue<Long> orderTimes = outputOrdersAndTimesMap.get(outputConnectionName);
            Long orderTime = null;
            orderTime = orderTimes.peek();
            while ((orderTime != null) && (orderTime < maxValidTime)) {
                orderTimes.poll();
                orderTime = orderTimes.peek();
            }

            sb.append(outputConnectionName).append(':');
            sb.append(orderTimes.size());
            sb.append(',');
        }
        if (sb.substring(sb.length() - 1).equals(",")) {
            sb.deleteCharAt(sb.length() - 1); 
        }
        sb.append('}');
        return sb.toString();
    }

    protected String buildCPUStats() {
        StringBuilder sb = new StringBuilder("{");

        CpuTimes cpuTimes = monitor_.cpuTimes();

        if (previousCPUTimes_ != null) {
            sb.append("usage:");
            sb.append(cpuTimes.getCpuUsage(previousCPUTimes_));
            sb.append(',');
        }
        sb.append("idle:").append(cpuTimes.getIdleMillis());
        sb.append(", system:").append(cpuTimes.getSystemMillis());
        sb.append(", total:").append(cpuTimes.getTotalMillis());
        sb.append(", user:").append(cpuTimes.getUserMillis());

        previousCPUTimes_ = cpuTimes;
        sb.append('}');
        return sb.toString();
    }

    public static void setQueuePricesSize(String queueName, long queueSize) {
        LOGGER.debug("queueName = {}, queueSize = {}", queueName, queueSize);
        waitingPriceQueuesSize.put(queueName, queueSize);
    }

    public static void onNewOrder(String inputConnectionName) {
        if (!inputOrdersAndTimesMap.containsKey(inputConnectionName)) {
            inputOrdersAndTimesMap.put(inputConnectionName, new LinkedList<Long>());
        }
        inputOrdersAndTimesMap.get(inputConnectionName).add(DateService.currentTimeMillis());
    }

    public static void onOrderClose(String outputConnectionName) {
        if (!outputOrdersAndTimesMap.containsKey(outputConnectionName)) {
            outputOrdersAndTimesMap.put(outputConnectionName, new LinkedList<Long>());
        }

        outputOrdersAndTimesMap.get(outputConnectionName).add(DateService.currentTimeMillis());
    }

}
