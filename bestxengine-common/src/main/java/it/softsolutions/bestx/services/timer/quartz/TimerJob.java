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

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: timer obtained through the quartz job implementation. The timerServiceListener is a parameter that will be automatically get as long as the setter name remains equal to the variable one.
 * The value will be set while creating the job, in the jobDataMap we will set a property called "timerServiceListener" to the timerServiceListener value.
 * 
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation date: 26/ago/2013
 * 
 **/

public class TimerJob implements InterruptableJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerJob.class);
    
    private JobKey jobKey; 

    public TimerJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    	jobKey = jobExecutionContext.getJobDetail().getKey();    	
    	LOGGER.debug("Job {} executing", jobKey);
        JobExecutionDispatcher.INSTANCE.notifyJobExecution(jobExecutionContext, null, false);
        LOGGER.debug("Job {} executed", jobKey);
    }

	@Override
    public void interrupt() throws UnableToInterruptJobException {
        LOGGER.debug("Job {} interrupted", jobKey);	    
    }

}
