/*
 * Copyright 1997-2012 SoftSolutions! srl 
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

package it.softsolutions.bestx.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.ThreadPoolExecutor;
import it.softsolutions.bestx.appstatus.ApplicationStatus;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.dao.sql.SqlCSOperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

// TODO: Auto-generated Javadoc
/**
 * 
 * Purpose: This class implements the service that must manage the end of day automatic processing of the magnet orders. On initialization
 * it starts the timer (this way, if BestX! should crash, on restart the timer will be recreated every time for the same hour) then waits
 * for its expiration and fetch all the orders. For each of them it will fetch the operation and, in a new thread, call the onTimerExpired
 * method.
 * 
 * Project Name : bestxengine-cs First created by: rugger.rizzo Creation date: 19-ott-2012
 * 
 **/
public class CSOrdersEndOfDayService implements TimerEventListener, CSOrdersEndOfDayServiceMBean {
	
	/** The Constant MONITOR_TO_EXECUTION_ID. */
	public static final String MONITOR_TO_EXECUTION_ID = "MONITOR_TO_EXECUTION";
	
	/** The Constant EXECUTION_TO_MONITOR_ID. */
	public static final String EXECUTION_TO_MONITOR_ID = "EXECUTION_TO_MONITOR";
    
    /** The Constant ORDERS_END_OF_DAY_ID. */
    public static final String ORDERS_END_OF_DAY_ID = "ORDERS_END_OF_DAY";
    
    /** The Constant LIMIT_FILES_NO_PRICE_TIMER_ID. */
    public static final String LIMIT_FILES_NO_PRICE_TIMER_ID = "LIMIT_FILES_NO_PRICE_TIMER_ID";
    
    /** The Constant LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID. */
    public static final String LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID = "LIMIT_FILE_NON_US_AND_GLOBALEND_OF_DAY_ID";
    
    /** The Constant LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID. */
    public static final String LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID = "LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID";
    
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CSOrdersEndOfDayService.class);

    /** The orders end of day hour. */
    private Integer ordersEndOfDayHour;
    
    /** The orders end of day minute. */
    private Integer ordersEndOfDayMinute;
    
    /** The limit file non US end of day hour. */
    private Integer limitFileNonUSEndOfDayHour;
    
    /** The limit file non US end of day minute. */
    private Integer limitFileNonUSEndOfDayMinute;
    
    /** The limit file US end of day hour. */
    private Integer limitFileUSEndOfDayHour;
    
    /** The limit file US end of day minute. */
    private Integer limitFileUSEndOfDayMinute;
    
    /** The orders end of day restart delay. */
    private Integer ordersEndOfDayRestartDelay; // seconds to wait in case of restart, before triggering endofday (to wait for all the
                                                // services to be up and running)
    
    /** The monitor to execution hour. */
    private Integer monitorToExecutionHour; // [BESTX-458] hour in which the behavior of the application is switched from monitor to execution
    
    /** The monitor to execution minute. */
    private Integer monitorToExecutionMinute; // [BESTX-458] minute in which the behavior of the application is switched from monitor to execution
    
    /** The execution to monitor hour. */
    private Integer executionToMonitorHour;// [BESTX-458] hour in which the behavior of the application is switched from execution to monitor
    
    /** The execution to monitor minute. */
    private Integer executionToMonitorMinute;// [BESTX-458] minute in which the behavior of the application is switched from execution to monitor
    
    /** The operation state audit dao. */
    private SqlCSOperationStateAuditDao operationStateAuditDao;
    
    /** The operation registry. */
    private OperationRegistry operationRegistry;
    
    /** The executor. */
    private Executor executor;
    
    /** The expire time. */
    private static long expireTime;
    
    /** The debug mode. */
    private boolean debugMode = false;
    
    /** The application status. */
    private ApplicationStatus applicationStatus; // [BESTX-458] reference to applicationstatus bean, in order to manage/know if the application is in execution or monitoring modality

    /**
     * Inits the.
     */
    public void init() {
        LOGGER.info("Initializing end of day timer service.");
        checkPrerequisites();
        // calculating remaining time to the hour given by OrdersEndOfDayHour:OrdersEndOfDayMinute
        Calendar calendar = Calendar.getInstance(); // calendar.getTime()
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        calendar.set(year, month, day, ordersEndOfDayHour, ordersEndOfDayMinute, 0);
        LOGGER.info("End of day standard scheduled for {}", calendar.getTime().toString());
        Calendar expireCalendar = setUpEndOfDayTimer(calendar, ordersEndOfDayRestartDelay, ORDERS_END_OF_DAY_ID);
        if(expireCalendar != null && expireCalendar.compareTo(calendar) != 0)
            LOGGER.info("End of day standard rescheduled at {}", expireCalendar.getTime().toString());
    
        calendar.set(year, month, day, limitFileNonUSEndOfDayHour, limitFileNonUSEndOfDayMinute, 0);
        LOGGER.info("End of day non US Limit Files scheduled for {}", calendar.getTime().toString());
        expireCalendar = setUpEndOfDayTimer(calendar, ordersEndOfDayRestartDelay, LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID);
        if(expireCalendar != null && expireCalendar.compareTo(calendar) != 0)
            LOGGER.info("End of day non US Limit Files rescheduled at {}", expireCalendar.getTime().toString());
       
        calendar.set(year, month, day, limitFileUSEndOfDayHour, limitFileUSEndOfDayMinute, 0);
        LOGGER.info("End of day US Limit Files scheduled for  {}", calendar.getTime().toString());
        expireCalendar = setUpEndOfDayTimer(calendar, ordersEndOfDayRestartDelay, LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID);
        if(expireCalendar != null && expireCalendar.compareTo(calendar) != 0)
            LOGGER.info("End of day US Limit Files rescheduled at {}", expireCalendar.getTime().toString());
        
        // [BESTX-458] Add timers in order to schedule the switch between monitor and execution modalities
        try {
        	SimpleTimerManager.getInstance().stopJob(MONITOR_TO_EXECUTION_ID, CSOrdersEndOfDayService.class.getSimpleName());
        } catch (SchedulerException e) {
        	LOGGER.error("Error when trying to delete MONITOR to EXECUTION job: ", e);
        }
        try {
        	SimpleTimerManager.getInstance().stopJob(EXECUTION_TO_MONITOR_ID, CSOrdersEndOfDayService.class.getSimpleName());
        } catch (SchedulerException e) {
        	LOGGER.error("Error when trying to delete EXECUTION to MONITOR job: ", e);
        }
        
        if ((monitorToExecutionHour != null && monitorToExecutionMinute != null) ||
        		(executionToMonitorHour != null && executionToMonitorMinute != null)) {
	        boolean startInExecutionModality = true;
	        if (monitorToExecutionHour != null && monitorToExecutionMinute != null) {
	        	calendar.set(year, month, day, monitorToExecutionHour, monitorToExecutionMinute, 0);
	            LOGGER.info("Monitor modality switching to Execution scheduled for {}", calendar.getTime().toString());
	            
	            // setupTimer bind the timer for this service
	            try {           	
	                long expireTime_ = calendar.getTimeInMillis();
	                // Calendar expireCalendar = calendar;
	                // calculating time remained from now to the EndOfDay time
	                long now = Calendar.getInstance().getTimeInMillis();
	                long timeToExpire = expireTime_ - now;
	                if (timeToExpire > 0) {
	                	String timerName = MONITOR_TO_EXECUTION_ID;
	                    SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
	                    JobDetail newJob = simpleTimerManager.createNewJob(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, false, false);
	                    //this timer is not repeatable
	                    Trigger trigger = simpleTimerManager.createNewTrigger(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, timeToExpire);
	                    simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
	                    LOGGER.info("Monitor to execution switch rescheduled at {}", expireCalendar.getTime().toString());
	                    startInExecutionModality = false;
	                }
	                else
	                {
	                	startInExecutionModality = true;
	                }
	            } catch (SchedulerException e) {
	                LOGGER.error("Error while scheduling price discovery wait timer!", e);
	            }
	        }
	        if (executionToMonitorHour != null && executionToMonitorMinute != null) {
	        	calendar.set(year, month, day, executionToMonitorHour, executionToMonitorMinute, 0);
	            LOGGER.info("Execution modality switching to Monitor scheduled for {}", calendar.getTime().toString());
	            
	            // setupTimer bind the timer for this service
	            try {           	
	                long expireTime_ = calendar.getTimeInMillis();
	                // Calendar expireCalendar = calendar;
	                // calculating time remained from now to the EndOfDay time
	                long now = Calendar.getInstance().getTimeInMillis();
	                long timeToExpire = expireTime_ - now;
	                if (timeToExpire > 0) {
	                	String timerName = EXECUTION_TO_MONITOR_ID;
	                    SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
	                    JobDetail newJob = simpleTimerManager.createNewJob(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, false, false);
	                    //this timer is not repeatable
	                    Trigger trigger = simpleTimerManager.createNewTrigger(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, timeToExpire);
	                    simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
	                    LOGGER.info("Monitor to execution switch rescheduled at {}", expireCalendar.getTime().toString());
	                } else {
	                	startInExecutionModality = false;
	                }
	            } catch (SchedulerException e) {
	                LOGGER.error("Error while scheduling price discovery wait timer!", e);
	            }
	        }     
	        
	        if (startInExecutionModality) {
	        	applicationStatus.setType(ApplicationStatus.Type.EXECUTION);
	        } else {
	        	applicationStatus.setType(ApplicationStatus.Type.MONITOR);
	        }
        }
        
        JobExecutionDispatcher.INSTANCE.addTimerEventListener(CSOrdersEndOfDayService.class.getSimpleName(), this);
        LOGGER.info("Initialization done.");
    }

    /**
     * Sets the up end of day timer.
     *
     * @param calendar the calendar
     * @param ordersEndOfDayRestartDelay the orders end of day restart delay
     * @param timerName the timer name
     * @return the calendar
     */
    private Calendar setUpEndOfDayTimer(Calendar calendar, Integer ordersEndOfDayRestartDelay, String timerName) {
        // getting the current time
        expireTime = calendar.getTimeInMillis();
        Calendar expireCalendar = calendar;
        // calculating time remained from now to the EndOfDay time
        long now = Calendar.getInstance().getTimeInMillis();
        long timeToExpire = expireTime - now;
        if (timeToExpire <= 0) {
            timeToExpire = ordersEndOfDayRestartDelay * 1000L; // close active operations anyway, in case of restart after end of day,
                                                               // allowing system to startup (currently set to 300 secs)
            expireTime = expireTime + timeToExpire;
            expireCalendar = Calendar.getInstance();
            expireCalendar.setTimeInMillis(now + timeToExpire); // expireCalendar.getTime()
        }
        // setupTimer bind the timer for this service
        try {
            SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
            JobDetail newJob = simpleTimerManager.createNewJob(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, false, false);
            //this timer is not repeatable
            Trigger trigger = simpleTimerManager.createNewTrigger(timerName, CSOrdersEndOfDayService.class.getSimpleName(), false, timeToExpire);
            simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
        } catch (SchedulerException e) {
            LOGGER.error("Error while scheduling price discovery wait timer!", e);
        }
        return expireCalendar;
    }

    /**
     * Check prerequisites.
     *
     * @throws ObjectNotInitializedException the object not initialized exception
     */
    private void checkPrerequisites() throws ObjectNotInitializedException {
        if (ordersEndOfDayHour == null) {
            throw new ObjectNotInitializedException("OrdersEndOfDayHour not set");
        }
        if (ordersEndOfDayMinute == null) {
            throw new ObjectNotInitializedException("OrdersEndOfDayMinute not set");
        }
        if (executor == null) {
            throw new ObjectNotInitializedException("Executor not set");
        }
    }

    /**
     * Gets the orders end of day hour.
     * 
     * @return the orders end of day hour
     */
    public Integer getOrdersEndOfDayHour() {
        return ordersEndOfDayHour;
    }

    /**
     * Sets the orders end of day hour.
     * 
     * @param ordersEndOfDayHour
     *            the new orders end of day hour
     */
    public void setOrdersEndOfDayHour(Integer ordersEndOfDayHour) {
        this.ordersEndOfDayHour = ordersEndOfDayHour;
    }

    /**
     * Gets the orders end of day minute.
     * 
     * @return the orders end of day minute
     */
    public Integer getOrdersEndOfDayMinute() {
        return ordersEndOfDayMinute;
    }

    /**
     * Sets the orders end of day minute.
     * 
     * @param ordersEndOfDayMinute
     *            the new orders end of day minute
     */
    public void setOrdersEndOfDayMinute(Integer ordersEndOfDayMinute) {
        this.ordersEndOfDayMinute = ordersEndOfDayMinute;
    }

    /**
     * Gets the orders end of day restart delay.
     * 
     * @return the orders end of day restart delay
     */
    public Integer getOrdersEndOfDayRestartDelay() {
        return ordersEndOfDayRestartDelay;
    }

    /**
     * Sets the orders end of day restart delay.
     * 
     * @param delayUponRestart
     *            the new orders end of day restart delay
     */
    public void setOrdersEndOfDayRestartDelay(Integer delayUponRestart) {
        this.ordersEndOfDayRestartDelay = delayUponRestart;
    }

    /**
     * Gets the operation state audit dao.
     * 
     * @return the operation state audit dao
     */
    public OperationStateAuditDao getOperationStateAuditDao() {
        return operationStateAuditDao;
    }

    /**
     * Sets the operation state audit dao.
     * 
     * @param operationStateAuditDao
     *            the new operation state audit dao
     */
    public void setOperationStateAuditDao(SqlCSOperationStateAuditDao operationStateAuditDao) {
        this.operationStateAuditDao = operationStateAuditDao;
    }

    /**
     * Gets the operation registry.
     * 
     * @return the operation registry
     */
    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    /**
     * Sets the operation registry.
     * 
     * @param operationRegistry
     *            the new operation registry
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * Gets the executor.
     * 
     * @return the executor
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Sets the executor.
     * 
     * @param executor
     *            the new executor
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * Checks if is after end of day.
     * 
     * @param arrivalTime
     *            the arrival time
     * @return true, if is after end of day
     */
    public static boolean isAfterEndOfDay(long arrivalTime) {
        long timeToExpire = expireTime - arrivalTime;
        return (timeToExpire < 0);
    }

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.services.timer.quartz.TimerEventListener#timerExpired(java.lang.String, java.lang.String)
     */
    @Override
    public void timerExpired(final String jobName, final String groupName) {
    	LOGGER.info("End of day for: {}-{}", jobName, groupName);
    	List<String> ordersList = null;
    	try {
    	if (jobName != null) {
    		if (jobName.equals(ORDERS_END_OF_DAY_ID)) {
    			long begin = DateService.currentTimeMillis();
    			ordersList = operationStateAuditDao.getEndOfDayOrdersToClose();
    			long end =  DateService.currentTimeMillis();
    			LOGGER.info("EndOfday: gets {}; Time {} ms", ORDERS_END_OF_DAY_ID, end-begin);
    		} else if (jobName.equals(LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID)) {
    			long begin = DateService.currentTimeMillis();
    			ordersList = operationStateAuditDao.getLimitFilesEndOfDayOrdersToClose(true);
    			long end =  DateService.currentTimeMillis();
    			LOGGER.info("EndOfday: gets {}; Time {} ms", LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID, end-begin);
    		} else if (jobName.equals(LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID)) {
    			long begin = DateService.currentTimeMillis();
    			ordersList = operationStateAuditDao.getLimitFilesEndOfDayOrdersToClose(false);
    			long end =  DateService.currentTimeMillis();
    			LOGGER.info("EndOfday: gets {}; Time {} ms", LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID, end-begin);
    		} else if (jobName.equals(EXECUTION_TO_MONITOR_ID)) {
   	        	applicationStatus.setType(ApplicationStatus.Type.MONITOR);
   	        	LOGGER.info("Switching to Monitoring state from Execution state");
   	        	return;
    		} else if (jobName.equals(MONITOR_TO_EXECUTION_ID)) {
    			applicationStatus.setType(ApplicationStatus.Type.EXECUTION);
    			LOGGER.info("Switching to Execution state from Monitoringstate");
    			return;
    		} else {
    			    		
    			LOGGER.warn("Unexpected timer: {}", jobName);
    		}

    		if((ordersList != null) && (ordersList.size() != 0)){
    			List<Operation> operationsList = getOperations(ordersList, jobName, groupName);
    			if((operationsList != null) && (operationsList.size() > 0)){
    				onTimerExpired(operationsList, jobName, groupName);
    			}
    		}
    	} else {
    		LOGGER.warn("A job with a null name has been triggered!");
    	}
    	} catch (Exception e) {
    		LOGGER.warn("Exception was raised when trying to get orders for {} End Of Day ", jobName, e);
    	}
    }

    /**
     * Gets the operations.
     *
     * @param ordersList the orders list
     * @param jobName the job name
     * @param groupName the group name
     * @return the operations
     */
    private List<Operation> getOperations(List<String> ordersList, final String jobName, final String groupName) {
    	List<Operation> operationsList = new ArrayList<Operation>();
    	long begin = 0;
    	long end = 0;
    	for (String order : ordersList) {
    		try {
    			LOGGER.debug("EndOfDay getOperation {} - job {}, managing it. Get Operation", order, jobName);
    			begin = DateService.currentTimeMillis();
    			Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, order);
    			end =  DateService.currentTimeMillis();
    			LOGGER.debug("EndOfDay getOperation {} - job {}, managing it. Passing control to a new thread. time {}", order, jobName, end-begin);
    			if(operation != null){
    				operationsList.add(operation);
    			}
    		}
    		catch (OperationNotExistingException e) {
    			end =  DateService.currentTimeMillis();
    			LOGGER.error("EndOfDay getOperation: Operation bound to the order {} has not been found (Time {}): {}", order, end-begin, e.getMessage());
    			continue;
    		}
    		catch (BestXException e) {
    			LOGGER.error("EndOfDay getOperation: General error while finding operation bound to the order {} for magnet end of day: {}", order, e.getMessage());
    			continue;
    		}
    	}
    	LOGGER.info("EndOfDay getOperation: retrieved {} operations for job {}", operationsList.size(), jobName);
    	return operationsList;
    }


	/**
	 * On timer expired.
	 *
	 * @param operationsList the operations list
	 * @param jobName the job name
	 * @param groupName the group name
	 */
	private void onTimerExpired(List<Operation> operationsList, final String jobName, final String groupName) {
		
		for (final Operation operation : operationsList) {
			operation.setStopped(true);
			executor.execute(new Runnable() {

				@Override
				public void run() {
					LOGGER.debug("EndOfDay for operation {} - job {}, start handling - ThreadName {}",
							operation.getOrder().getFixOrderId(), jobName, Thread.currentThread().getId());
					long begin = DateService.currentTimeMillis();

					operation.onTimerExpired(jobName, groupName);
					long end = DateService.currentTimeMillis();
					LOGGER.debug(
							"EndOfDay for operation {} - job {}, end managing it. Passing another order. Time {} - ThreadName {}",
							operation.getOrder().getFixOrderId(), jobName, end - begin, Thread.currentThread().getId());
				}
			});
			if (LOGGER.isDebugEnabled() && executor instanceof ThreadPoolExecutor) {
				ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
				LOGGER.debug(
						"End of day for operation {} - job {}, end managing it. Passing another order. Executor active {} - queue {}",
						operation.getId(), jobName, ex.getActiveCount(), ex.getPoolSize());
			} else {
				LOGGER.debug("End of day for operation {} - job {}, end managing it. Passing another order.",
						operation.getId(), jobName);
			}
		}
	}
    
    /**
     * Gets the limit file non US end of day hour.
     *
     * @return the limitFileNonUSEndOfDayHour
     */
    public Integer getLimitFileNonUSEndOfDayHour() {
        return limitFileNonUSEndOfDayHour;
    }

    /**
     * Sets the limit file non US end of day hour.
     *
     * @param limitFileNonUSEndOfDayHour the limitFileNonUSEndOfDayHour to set
     */
    public void setLimitFileNonUSEndOfDayHour(Integer limitFileNonUSEndOfDayHour) {
        this.limitFileNonUSEndOfDayHour = limitFileNonUSEndOfDayHour;
    }

    /**
     * Gets the limit file non US end of day minute.
     *
     * @return the limitFileNonUSEndOfDayMinute
     */
    public Integer getLimitFileNonUSEndOfDayMinute() {
        return limitFileNonUSEndOfDayMinute;
    }

    /**
     * Sets the limit file non US end of day minute.
     *
     * @param limitFileNonUSEndOfDayMinute the limitFileNonUSEndOfDayMinute to set
     */
    public void setLimitFileNonUSEndOfDayMinute(Integer limitFileNonUSEndOfDayMinute) {
        this.limitFileNonUSEndOfDayMinute = limitFileNonUSEndOfDayMinute;
    }

    /**
     * Gets the limit file US end of day hour.
     *
     * @return the limitFileUSEndOfDayHour
     */
    public Integer getLimitFileUSEndOfDayHour() {
        return limitFileUSEndOfDayHour;
    }

    /**
     * Sets the limit file US end of day hour.
     *
     * @param limitFileUSEndOfDayHour the limitFileUSEndOfDayHour to set
     */
    public void setLimitFileUSEndOfDayHour(Integer limitFileUSEndOfDayHour) {
        this.limitFileUSEndOfDayHour = limitFileUSEndOfDayHour;
    }

    /**
     * Gets the limit file US end of day minute.
     *
     * @return the limitFileUSEndOfDayMinute
     */
    public Integer getLimitFileUSEndOfDayMinute() {
        return limitFileUSEndOfDayMinute;
    }

    /**
     * Sets the limit file US end of day minute.
     *
     * @param limitFileUSEndOfDayMinute the limitFileUSEndOfDayMinute to set
     */
    public void setLimitFileUSEndOfDayMinute(Integer limitFileUSEndOfDayMinute) {
        this.limitFileUSEndOfDayMinute = limitFileUSEndOfDayMinute;
    }

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.CSOrdersEndOfDayServiceMBean#invokeTimerExpired_Orders()
	 */
	@Override
	public void invokeTimerExpired_Orders() {
		//if (debugMode) {
			timerExpired("ORDERS_END_OF_DAY", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.CSOrdersEndOfDayServiceMBean#invokeTimerExpired_LimitFile_USandGlobal()
	 */
	@Override
	public void invokeTimerExpired_LimitFile_USandGlobal() {
	//	if (debugMode) {
			timerExpired("LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.services.CSOrdersEndOfDayServiceMBean#invokeTimerExpired_LimitFile_Non_USandGlobal()
	 */
	@Override
	public void invokeTimerExpired_LimitFile_Non_USandGlobal() {
		//if (debugMode) {
			timerExpired("LIMIT_FILE_NON_US_AND_GLOBALEND_OF_DAY_ID", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	/**
	 * Gets the debug mode.
	 *
	 * @return the debug mode
	 */
	public boolean getDebugMode() {
		return debugMode;
	}

	/**
	 * Sets the debug mode.
	 *
	 * @param debugMode the new debug mode
	 */
	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
	
	/**
	 * Gets the application status.
	 *
	 * @return the application status
	 */
	public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	/**
	 * Sets the application status.
	 *
	 * @param applicationStatus the new application status
	 */
	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}

	public Integer getMonitorToExecutionHour() {
		return monitorToExecutionHour;
	}

	public void setMonitorToExecutionHour(Integer monitorToExecutionHour) {
		this.monitorToExecutionHour = monitorToExecutionHour;
	}

	public Integer getMonitorToExecutionMinute() {
		return monitorToExecutionMinute;
	}

	public void setMonitorToExecutionMinute(Integer monitorToExecutionMinute) {
		this.monitorToExecutionMinute = monitorToExecutionMinute;
	}

	public Integer getExecutionToMonitorHour() {
		return executionToMonitorHour;
	}

	public void setExecutionToMonitorHour(Integer executionToMonitorHour) {
		this.executionToMonitorHour = executionToMonitorHour;
	}

	public Integer getExecutionToMonitorMinute() {
		return executionToMonitorMinute;
	}

	public void setExecutionToMonitorMinute(Integer executionToMonitorMinute) {
		this.executionToMonitorMinute = executionToMonitorMinute;
	}
	
	
	
	
}
