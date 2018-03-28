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

import org.quartz.SchedulerConfigException;
import org.quartz.spi.ThreadPool;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: davide.rossoni 
 * Creation date: 03/ott/2013 
 * 
 **/
public class QuartzThreadPool implements ThreadPool {
    
    private ThreadPoolExecutor threadPoolExecutor;
    
    public QuartzThreadPool() {
        super();
        threadPoolExecutor = new ThreadPoolExecutor();
    }

    @Override
    public boolean runInThread(Runnable runnable) {
        threadPoolExecutor.execute(runnable);
        return true;
    }

    @Override
    public int blockForAvailableThreads() {
        return threadPoolExecutor.getMaxPoolSize() - threadPoolExecutor.getActiveCount();
    }

    @Override
    public void initialize() throws SchedulerConfigException {
        threadPoolExecutor.initialize();        
    }

    @Override
    public void shutdown(boolean waitForJobsToComplete) {
        threadPoolExecutor.shutdown();        
    }

    @Override
    public int getPoolSize() {
        return threadPoolExecutor.getPoolSize();
    }

    @Override
    public void setInstanceId(String schedInstId) {
    }

    @Override
    public void setInstanceName(String schedName) {
        threadPoolExecutor.setThreadNamePrefix(schedName);        
    }

    /**
     * <p>
     * Set the number of worker threads in the pool - has no effect after
     * <code>initialize()</code> has been called.
     * </p>
     */
    public void setThreadCount(int count) {
        threadPoolExecutor.setCorePoolSize(count / 2);
        threadPoolExecutor.setMaxPoolSize(count);
    }

    /**
     * <p>
     * Get the number of worker threads in the pool.
     * </p>
     */
    public int getThreadCount() {
        return threadPoolExecutor.getPoolSize();
    }
}
