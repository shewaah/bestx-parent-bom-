/*
 * Copyright 1997- 2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.services.passwordnotifier;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Purpose: this class is mainly for ...
 *
 * Project Name : passwordadvisor First created by: william.younang Creation
 * date: 12/feb/2015
 * 
 **/
public class PasswordNotifierSenderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordNotifierSenderScheduler.class);
    private final static String PROPERTIES_FILE = "BESTX.properties";
    private int timeIntervalInHours;
    private String startTime;
    private static PasswordNotifierService passwordNotifierService;

    public PasswordNotifierSenderScheduler() throws SchedulerException, ParseException {
        LOGGER.debug("Scheduling password notification");
        init();
        schedulePasswordNotification();
    }

    private void init() {

        Configuration configuration = null;
        try {
            configuration = new PropertiesConfiguration(PROPERTIES_FILE);
            this.startTime = configuration.getString("PasswordNotifierService.startTime");
            this.timeIntervalInHours = configuration.getInt("PasswordNotifierService.timeIntervalInHours");

        } catch (Exception e) {
            LOGGER.error("{} {}", e.getMessage(), e);
        }
        LOGGER.debug("Password notification will be sent at {} with a frequency of {} hour(s)", startTime, timeIntervalInHours);
    }

    public void schedulePasswordNotification() throws SchedulerException, ParseException {
        // Creating scheduler factory and scheduler
        SchedulerFactory factory = new StdSchedulerFactory();
        Scheduler scheduler = factory.getScheduler();

        // Creating Job and link to our Job class
        JobDetailImpl jobDetail = new JobDetailImpl();
        jobDetail.setName("passwordNotificationJob");
        jobDetail.setJobClass(PasswordNotifierSenderJob.class);

        //JobDetail jobDetail = JobBuilder.newJob(PasswordNotifierSenderJob.class).withIdentity("passwordNotificationJob").build();
        
        // Creating schedule time with trigger
        Date startTimez = DateUtils.parseDate(startTime, new String[] { "HH:mm" });

        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("passwordNotificationTrigger", "group").startAt(startTimez).withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(timeIntervalInHours).repeatForever()).build();        
        
        JobKey jobKey = jobDetail.getKey();
        
        if (scheduler.checkExists(jobKey)){
            LOGGER.debug("Job already exists {}, deleting and rescheduling it", jobKey);
            // uses deleteJob, then recreates it, instead of rescheduleJob, because reschedule
            // often fails (returns null, and does not setup the timer again!)
            boolean deleted = scheduler.deleteJob(jobKey);
            LOGGER.debug("Job {} deleted? {}", deleted);
        }
        
        // Start scheduler
        scheduler.start();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    /**
     * @param passwordNotifierService
     *            the passwordNotifierService to set
     */
    public void setPasswordNotifierService(PasswordNotifierService passwordNotifierService) {
        PasswordNotifierSenderScheduler.passwordNotifierService = passwordNotifierService;
    }

    /**
     * @param timeIntervalInHours
     *            the timeIntervalInHours to set
     */
    public void setTimeIntervalInHours(int timeIntervalInHours) {
        this.timeIntervalInHours = timeIntervalInHours;
    }

    /**
     * @param startTime
     *            the startTime to set
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public static class PasswordNotifierSenderJob implements Job {

        public PasswordNotifierSenderJob() {
            LOGGER.debug("");
        }

        @Override
        public void execute(JobExecutionContext arg0) throws JobExecutionException {
            LOGGER.debug("");

            try {
                passwordNotifierService.performPasswordExpiryNotification();
            } catch (Exception e) {
                LOGGER.error("Error processing notification: {}", e.getMessage(), e);
            }

        }
    }

}
