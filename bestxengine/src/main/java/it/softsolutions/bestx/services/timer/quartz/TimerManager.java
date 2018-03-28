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

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
  
/**
 *
 * Purpose: this interface lines out how a timer manager should manage the Quartz scheduler, 
 * there is only one instance of the scheduler
 *
 * Project Name : bestxengine
 * First created by: ruggero.rizzo
 * Creation date: 26/ago/2013
 * 
 **/

public interface TimerManager {

	/**
	 * Possible items that should be notified on job execution
	 * @author ruggero.rizzo
	 *
	 */
	public enum DataMapItem {
		TIMER_LISTENER
	}
	
	/**
	 * Get the scheduler
	 * @return s scheduler
	 */
	public Scheduler getScheduler();
	
	/**
	 * Start the scheduler ----> Maybe not needed
	 * @throws SchedulerException 
	 */
	//public void startScheduler() throws SchedulerException;
	
	/**
	 * Stop the scheduler
	 * @throws SchedulerException 
	 */
	public void stopScheduler() throws SchedulerException;
	
	/**
	 * Create a new durable and recoverable job with the given name and the given listener
	 * @param jobName job name
	 * @param groupName group name
	 * @return the new job
	 * @throws SchedulerException 
	 */
	public JobDetail createNewJob(String jobName, String groupName) throws SchedulerException;

	/**
     * Create a new job with the given name and the given listener, it can be durable or not and it could be recovered
     * on relaunch or not 
     * @param jobName job name
     * @param groupName group name
	 * @param durable explicitly decide if the job must be persisted or not
	 * @param requestRecovery explicitly decide if the job must be recovered on relaunch
     * @return the new job
     * @throws SchedulerException 
	 */
	public JobDetail createNewJob(String jobName, String groupName, boolean durable, boolean requestRecovery, boolean monitorable) throws SchedulerException;
	
	/**
	 * Create new trigger starting at a given hour
	 * @param triggerName trigger name
	 * @param triggerGroup trigger group
	 * @param startNow start immediately or at a given hour
	 * @param repeat repeat the timer or not
	 * @param startHour trigger starting hour (not considered if startNow is true)
	 * @param startMinute trigger starting minute (not considered if startNow is true)
	 * @param startSecond trigger starting second (not considered if startNow is true)
	 * @param intervalInSeconds trigger repeating interval
	 * @return the new trigger, or null if cannot be created
	 */
	public Trigger createNewTrigger(String triggerName, String triggerGroup, boolean repeat, int startHour, int startMinute, int startSecond, long intervalInMillis);
	
	   /**
     * Create new trigger starting from now to given milliseconds
     * @param triggerName trigger name
     * @param triggerGroup trigger group
     * @param startNow start immediately or at a given hour
     * @param repeat repeat the timer or not
     * @param intervalInSeconds trigger repeating interval
     * @return the new trigger, or null if cannot be created
     */
    public Trigger createNewTrigger(String triggerName, String triggerGroup, boolean repeat, long intervalInMillis);
	
	/**
	 * Clean the database from the job data
	 * @param jobExecutionContext the job context
	 */
	public void cleanJobData(JobExecutionContext jobExecutionContext);
	
	/**
	 * Setup the job schedulation with the given trigger
	 * @param job job to be scheduled
	 * @param trigger trigger that executes the job
	 * @throws SchedulerException 
	 */
	public void scheduleJobWithTrigger(JobDetail job, Trigger trigger) throws SchedulerException;
	

	/**
	 * Setup the job schedulation with the given trigger
	 * @param job job to be scheduled
	 * @param trigger trigger that executes the job
	 * @param isVolatile persist the job on db
	 * @throws SchedulerException 
	 */
	public void scheduleJobWithTrigger(JobDetail job, Trigger trigger, boolean isVolatile) throws SchedulerException;
	
	/**
	 * Stop the job
	 * 
	 * @throws SchedulerException 
	 */
	public void stopJob(String jobName, String groupName) throws SchedulerException;

	/**
	 * Check if the job of the given group already exists in the scheduler
	 * @param jobName the job to look for
	 * @param jobGroup the job group
	 * @return true if exists, false if not
	 * @throws SchedulerException
	 */
    public boolean jobAlreadyExists(String jobName, String jobGroup) throws SchedulerException;
    
}
