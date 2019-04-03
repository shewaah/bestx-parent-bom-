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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.UnableToInterruptJobException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: ruggero.rizzo Creation date: 26/ago/2013
 * 
 **/

public class SimpleTimerManager implements TimerManager {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleTimerManager.class);
    
    private static final String QUARTZ_CONFIG_FILE = "quartz.properties";
    private static final String QUARTZ_VOLATILE_CONFIG_FILE = "quartz-volatile.properties";
    private static final String QUARTZ_JOBSTORE_PASSWORD_PROPERTY = "org.quartz.dataSource.cs_develop.password";
    private static SimpleTimerManager instance = null;
    private Scheduler scheduler = null;
    private Scheduler schedulerVolatile = null;
    private StandardPBEStringEncryptor encryptor;
    
    // <jobKey, semaphore>
    private static ConcurrentMap<JobKey, Semaphore> semaphores = new ConcurrentHashMap<JobKey, Semaphore>();
    
    private SimpleTimerManager() throws SchedulerException {
        
        Properties quartzProperties = new Properties();
        SchedulerFactory schedulerFactory = null;
        try {
            quartzProperties.load(this.getClass().getClassLoader().getResourceAsStream(QUARTZ_CONFIG_FILE)); 
            encryptor = new StandardPBEStringEncryptor(); 
            String pbeAlgorithm ="PBEWITHSHA1ANDDESEDE";
            String pbePassword = "AyT0P%c$=lPwQ**";
            encryptor.setPassword(pbePassword);
            encryptor.setAlgorithm(pbeAlgorithm);
            
            // Try to decrypt fields with encrypted passwords
            String decryptedPassword = decryptPassword(quartzProperties.getProperty(QUARTZ_JOBSTORE_PASSWORD_PROPERTY));
            quartzProperties.setProperty(QUARTZ_JOBSTORE_PASSWORD_PROPERTY, decryptedPassword);
            schedulerFactory = new org.quartz.impl.StdSchedulerFactory(quartzProperties);
        } catch (Exception e) {
            LOGGER.warn("Error while loading properties from file {}, going on with the default quartz configuration file.", QUARTZ_CONFIG_FILE, e);
            schedulerFactory = new org.quartz.impl.StdSchedulerFactory(); 
        } 
        
        Properties quartzVolatileProperties = new Properties();
        SchedulerFactory schedulerFactoryVolatile = null;
        try {
        	quartzVolatileProperties.load(this.getClass().getClassLoader().getResourceAsStream(QUARTZ_VOLATILE_CONFIG_FILE));
        	schedulerFactoryVolatile = new org.quartz.impl.StdSchedulerFactory(quartzVolatileProperties);
        } catch (Exception e) {
        	LOGGER.warn("Error while loading properties from file {}, going on with the default quartz configuration file.", QUARTZ_VOLATILE_CONFIG_FILE, e);
        	schedulerFactoryVolatile = new org.quartz.impl.StdSchedulerFactory(); 
        }
        
        scheduler = schedulerFactory.getScheduler();
        schedulerVolatile = schedulerFactoryVolatile.getScheduler();
    }
    
    public void start() throws SchedulerException {
    	LOGGER.debug("Starting scheduler");
        scheduler.start();
        schedulerVolatile.start();
        LOGGER.debug("Scheduler started");
    }

    public static SimpleTimerManager getInstance() throws SchedulerException {
        if (instance == null) {
            instance = new SimpleTimerManager();
        }
        return instance;
    }

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void stopScheduler() throws SchedulerException {
        if (scheduler.isStarted()) {
            // shutdown waiting for executing job to complete
            scheduler.shutdown(true);
        }
        if (schedulerVolatile.isStarted()) {
            // shutdown waiting for executing job to complete
            schedulerVolatile.shutdown(true);
        }
    }

    @Override
    public JobDetail createNewJob(String jobName, String groupName) throws SchedulerException {

        if (jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null");
        }
        if (groupName == null) {
            throw new IllegalArgumentException("groupName cannot be null");
        }
        LOGGER.debug("Building job {}.{} ", groupName, jobName);
        JobDetail job = newJob(TimerJob.class)
                        .withIdentity(jobName, groupName)
                        .requestRecovery(true)
                        .storeDurably(true)
                        .build();
        return job;
    }

    @Override
    public JobDetail createNewJob(String jobName, String groupName, boolean durable, boolean requestRecovery, boolean monitorable) throws SchedulerException {

    	if (jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null");
        }
        if (groupName == null) {
            throw new IllegalArgumentException("groupName cannot be null");
        }
        
        LOGGER.debug("Building job {}.{}", groupName, jobName);
        
        JobDetail job = null;
        
        if (monitorable) {
	        job = newJob(TimerJobMonitorable.class)
	                        .withIdentity(jobName, groupName)
	                        .requestRecovery(requestRecovery)
	                        .storeDurably(durable)
	                        .build();
        } else {
        	job = newJob(TimerJob.class)
                    .withIdentity(jobName, groupName)
                    .requestRecovery(requestRecovery)
                    .storeDurably(durable)
                    .build();	
        }
        return job;
    }

    @Override
    public void cleanJobData(JobExecutionContext jobExecutionContext) {
        // should not be needed
    }

    @Override
    public Trigger createNewTrigger(String triggerName, String triggerGroup, boolean repeat, int startHour, int startMinute, int startSecond, long intervalInMillis) {
    	LOGGER.debug("triggerName = {}, triggerGroup = {}, repeat = {}, {}:{}:{}, intervalInMillis = {}", triggerName, triggerGroup, repeat, startHour, startMinute, startSecond, intervalInMillis);

        if (triggerName == null) {
            throw new IllegalArgumentException("triggerName cannot be null");
        }
        if (triggerGroup == null) {
            throw new IllegalArgumentException("triggerGroup cannot be null");
        }

        Trigger trigger = null;
        int intervalInSeconds = (int) (intervalInMillis / 1000);
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), startHour, startMinute, startSecond);
        Calendar now = Calendar.getInstance();
        // check if the fire time is now or before
        if (calendar.compareTo(now) <= 0) {
            LOGGER.debug("The timer {} will start firing at {}:{}:{}", triggerName, startHour, startMinute, startSecond);
            if (repeat) {
                LOGGER.debug("The timer {} is repeatable every {} seconds", triggerName, intervalInSeconds);
                trigger = newTrigger()
                                .withIdentity(triggerName, triggerGroup)
                                .startAt(DateBuilder.todayAt(startHour, startMinute, startSecond))
                                .withSchedule(simpleSchedule()
                                                .withIntervalInSeconds(intervalInSeconds)
                                                .repeatForever())
                                .build();
            } else {
                trigger = newTrigger()
                                .withIdentity(triggerName, triggerGroup)
                                .startAt(DateBuilder.todayAt(startHour, startMinute, startSecond))
                                .build();
            }
        } else {
            LOGGER.error("Cannot create trigger {}, fire time {}:{}:{} is past due.", triggerName, startHour, startMinute, startSecond);
        }

        return trigger;
    }

    @Override
    public Trigger createNewTrigger(String triggerName, String triggerGroup, boolean repeat, long intervalInMillis) {
        if (triggerName == null) {
            throw new IllegalArgumentException("triggerName cannot be null");
        }
        if (triggerGroup == null) {
            throw new IllegalArgumentException("triggerGroup cannot be null");
        }

        Trigger trigger = null;
        int intervalInSeconds = (int) (intervalInMillis / 1000);
        LOGGER.debug("The timer {} will start firing in {} seconds", triggerName, intervalInSeconds);
        if (repeat) {
            LOGGER.debug("The timer {} is repeatable every {} seconds", triggerName, intervalInSeconds);
            trigger = newTrigger()
                            .withIdentity(triggerName, triggerGroup)
                            .startAt(DateBuilder.futureDate(intervalInSeconds, IntervalUnit.SECOND))
                            .withSchedule(simpleSchedule
                                            ().withIntervalInSeconds(intervalInSeconds)
                                            .repeatForever())
                            .endAt(DateBuilder.dateOf(23, 59, 59))
                            .build();
        } else {
            trigger = newTrigger()
                            .withIdentity(triggerName, triggerGroup)
                            .startAt(DateBuilder.futureDate(intervalInSeconds, IntervalUnit.SECOND))
                            .build();
        }
        return trigger;
    }
    
    @Override
    public void scheduleJobWithTrigger(JobDetail job, Trigger trigger) throws SchedulerException {
    	scheduleJobWithTrigger(job, trigger, false);
    }
    
    @Override
    public void scheduleJobWithTrigger(JobDetail job, Trigger trigger, boolean isVolatile) throws SchedulerException {
        if (job == null) {
            throw new IllegalArgumentException("job cannot be null!");
        }
        if (trigger == null) {
            throw new IllegalArgumentException("trigger cannot be null!");
        }

        JobKey jobKey = job.getKey();
        
        Semaphore semaphore = null;
        if (semaphores.containsKey(jobKey)) {
            semaphore = semaphores.get(jobKey);
        } else {
            semaphore = new Semaphore(1);
            semaphores.put(jobKey, semaphore);
        }

        try {
            // lock the operationID-related resource
            semaphore.acquire();
            if (isVolatile) {
            	schedule(schedulerVolatile, job, trigger, jobKey);
            } else {
            	schedule(scheduler, job, trigger, jobKey);
            }
        } catch (InterruptedException e) {
        	LOGGER.error("{}", e.getMessage(), e);
        } catch (SchedulerException se) {
            LOGGER.error("JobKey {} exception: {}", jobKey, se.getMessage(), se);
        } finally {
            // release the jobKey-related resource
            semaphore.release();
        }
    }

	private void schedule(Scheduler scheduler, JobDetail job, Trigger trigger, JobKey jobKey) throws SchedulerException {
		if (scheduler.checkExists(jobKey)){
		    LOGGER.debug("Job already exists {}, deleting and rescheduling it", jobKey);
		    // uses deleteJob, then recreates it, instead of rescheduleJob, because reschedule
		    // often fails (returns null, and does not setup the timer again!)
		    boolean deleted = scheduler.deleteJob(jobKey);
		    LOGGER.debug("Job {} deleted? {}", deleted);
		}

		LOGGER.debug("Schedule job {}, with trigger {}", jobKey, trigger);
		Date scheduleTime = scheduler.scheduleJob(job, trigger);
		if (scheduleTime == null) {
		    LOGGER.error("Could not schedule job {}, schedule time is null", jobKey);
		}
	}

    @Override
    public void stopJob(String jobName, String groupName) throws SchedulerException {
    	LOGGER.debug("jobName = {}, groupName = {}", jobName, groupName);
    	JobKey jobKey = new JobKey(jobName, groupName);
    	
    	Semaphore semaphore = null;
        if (semaphores.containsKey(jobKey)) {
            semaphore = semaphores.get(jobKey);
        } else {
            semaphore = new Semaphore(1);
            semaphores.put(jobKey, semaphore);
        }

        try {
            // lock the operationID-related resource
            semaphore.acquire();
    	
            if (schedulerVolatile.checkExists(jobKey)) {
            	stopJob(schedulerVolatile, jobKey);
            } else if (scheduler.checkExists(jobKey)) {
		    	stopJob(scheduler, jobKey);
	    	} else {
	    		LOGGER.debug("skip stopping job {}, it does not exist", jobKey);
	    	}
        } catch (InterruptedException e) {
        	LOGGER.error("{}", e.getMessage(), e);
        } finally {
            // release the jobKey-related resource
            semaphore.release();
        }
    }

	private void stopJob(Scheduler scheduler, JobKey jobKey) throws UnableToInterruptJobException, SchedulerException {
		scheduler.interrupt(jobKey);
		LOGGER.debug("job {} stopped", jobKey);
		boolean deleted = scheduler.deleteJob(jobKey);
		LOGGER.debug("job {} deleted = {}", jobKey, deleted);
		if (!deleted) {
		    LOGGER.error("Cannot stop job {}", jobKey);
		}
	}

    @Override
    public boolean jobAlreadyExists(String jobName, String jobGroup) throws SchedulerException {
        if (jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null");
        }
        if (jobGroup == null) {
            throw new IllegalArgumentException("groupName cannot be null");
        }
        
    	JobKey jobKey = new JobKey(jobName, jobGroup);
        
        Semaphore semaphore = null;
        if (semaphores.containsKey(jobKey)) {
            semaphore = semaphores.get(jobKey);
        } else {
            semaphore = new Semaphore(1);
            semaphores.put(jobKey, semaphore);
        }

        try {
            // lock the operationID-related resource
            semaphore.acquire();
            return schedulerVolatile.checkExists(new JobKey(jobName, jobGroup)) || scheduler.checkExists(new JobKey(jobName, jobGroup));
            
        } catch (InterruptedException e) {
        	LOGGER.error("{}", e.getMessage(), e);
        } finally {
            // release the jobKey-related resource
            semaphore.release();
        }
        
        return false;
    }
    
    private String decryptPassword(String encryptedPassword) {
        String password = null;
        if (encryptedPassword.startsWith("ENC(")) {
            try {
                encryptedPassword = encryptedPassword.substring(4, encryptedPassword.length() - 1);
                password = encryptor.decrypt(encryptedPassword);  
            } catch (Exception e) {
                LOGGER.warn("Unable to decrypt password, use the plain value. Reason: {}", e.getMessage());
                password = encryptedPassword;
            }
        } else {
            LOGGER.info("Password '*****' not encrypted, use the plain value.", encryptedPassword);
            password = encryptedPassword;
        }
        LOGGER.trace("encryptedPassword = '{}', password = '*****'", encryptedPassword, password);
        return password;
    }
}
