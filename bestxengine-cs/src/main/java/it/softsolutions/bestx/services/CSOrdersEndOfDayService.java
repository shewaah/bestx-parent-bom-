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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.dao.sql.SqlCSOperationStateAuditDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

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
	
    public static final String ORDERS_END_OF_DAY_ID = "ORDERS_END_OF_DAY";
    public static final String LIMIT_FILES_NO_PRICE_TIMER_ID = "LIMIT_FILES_NO_PRICE_TIMER_ID";
    public static final String LIMIT_FILE_NON_US_AND_GLOBAL_END_OF_DAY_ID = "LIMIT_FILE_NON_US_AND_GLOBALEND_OF_DAY_ID";
    public static final String LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID = "LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CSOrdersEndOfDayService.class);

    private Integer ordersEndOfDayHour;
    private Integer ordersEndOfDayMinute;
    private Integer limitFileNonUSEndOfDayHour;
    private Integer limitFileNonUSEndOfDayMinute;
    private Integer limitFileUSEndOfDayHour;
    private Integer limitFileUSEndOfDayMinute;
    private Integer ordersEndOfDayRestartDelay; // seconds to wait in case of restart, before trigegring endofday (to wait for all the
                                                // services to be up and running)
    private SqlCSOperationStateAuditDao operationStateAuditDao;
    private OperationRegistry operationRegistry;
    private Executor executor;
    private static long expireTime;
    private boolean debugMode = false;

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

        JobExecutionDispatcher.INSTANCE.addTimerEventListener(CSOrdersEndOfDayService.class.getSimpleName(), this);
        LOGGER.info("Initialization done.");
    }

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

    @Override
    public void timerExpired(final String jobName, final String groupName) {
    	LOGGER.info("End of day for: {}-{}", jobName, groupName);
    	List<String> ordersList = null;
    	// TDR BESTX-465: to avoid deadlock on DB
    	Boolean done = false;
    	if(jobName != null) {
    	   int count = 0;
    		while(!done && count < 5) {
    		   count++;
    			try {
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
    				} else {
    					LOGGER.warn("Unexpected timer: {}", jobName);
    				}
    				done = true;
    				if((ordersList != null) && (ordersList.size() != 0)){
    					List<Operation> operationsList = getOperations(ordersList, jobName, groupName);
    					if((operationsList != null) && (operationsList.size() > 0)){
    						onTimerExpired(operationsList, jobName, groupName);
    					}
    				}
    			} catch (org.springframework.dao.DeadlockLoserDataAccessException ex) {
    				LOGGER.warn("Exception was raised when trying to get orders for {} End Of Day: retrying ", jobName, ex);
               try {
                  Thread.sleep(500);
               }
               catch (InterruptedException e) {
                  LOGGER.error("Error while sleeping", e);
               }
    			} catch (Exception e) {
    				LOGGER.error("Exception was raised when trying to get orders for {} End Of Day ", jobName, e);
    				done = true;
    			}
    		}
         if (count >= 5) {
            LOGGER.warn("The max number of attempts for timerExpired has been reached: {}", count);
         }
    	} else {
			LOGGER.warn("A job with a null name has been triggered!");
		}
    }

    /** @param ordersList
	 * 	@param jobName
	 * 	@param groupName  
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


    private void onTimerExpired(List<Operation> operationsList, final String jobName, final String groupName) {
    	for (final Operation operation : operationsList) {
    		operation.setStopped(true);
    		executor.execute(new Runnable() {

    			@Override
				public void run() {
    				LOGGER.debug("EndOfDay for operation {} - job {}, start handling - ThreadName {}", operation.getOrder().getFixOrderId(), jobName, Thread.currentThread().getId());
    				long begin = DateService.currentTimeMillis();

    				operation.onTimerExpired(jobName, groupName);
    				long end = DateService.currentTimeMillis();
    				LOGGER.debug("EndOfDay for operation {} - job {}, end managing it. Passing another order. Time {} - ThreadName {}", operation.getOrder().getFixOrderId(), jobName, end-begin,
    						Thread.currentThread().getId());
    			}
    		});
    		if (LOGGER.isDebugEnabled() && executor instanceof ThreadPoolExecutor) {
    			ThreadPoolExecutor ex = (ThreadPoolExecutor) executor;
    			LOGGER.debug("End of day for operation {} - job {}, end managing it. Passing another order. Executor active {} - queue {}", operation.getId(), jobName, ex.getActiveCount(),
    					ex.getPoolSize());
    		}
    		else {
    			LOGGER.debug("End of day for operation {} - job {}, end managing it. Passing another order.", operation.getId(), jobName);
    		}
    	}
    }

    
    /**
     * @return the limitFileNonUSEndOfDayHour
     */
    public Integer getLimitFileNonUSEndOfDayHour() {
        return limitFileNonUSEndOfDayHour;
    }

    /**
     * @param limitFileNonUSEndOfDayHour the limitFileNonUSEndOfDayHour to set
     */
    public void setLimitFileNonUSEndOfDayHour(Integer limitFileNonUSEndOfDayHour) {
        this.limitFileNonUSEndOfDayHour = limitFileNonUSEndOfDayHour;
    }

    /**
     * @return the limitFileNonUSEndOfDayMinute
     */
    public Integer getLimitFileNonUSEndOfDayMinute() {
        return limitFileNonUSEndOfDayMinute;
    }

    /**
     * @param limitFileNonUSEndOfDayMinute the limitFileNonUSEndOfDayMinute to set
     */
    public void setLimitFileNonUSEndOfDayMinute(Integer limitFileNonUSEndOfDayMinute) {
        this.limitFileNonUSEndOfDayMinute = limitFileNonUSEndOfDayMinute;
    }

    /**
     * @return the limitFileUSEndOfDayHour
     */
    public Integer getLimitFileUSEndOfDayHour() {
        return limitFileUSEndOfDayHour;
    }

    /**
     * @param limitFileUSEndOfDayHour the limitFileUSEndOfDayHour to set
     */
    public void setLimitFileUSEndOfDayHour(Integer limitFileUSEndOfDayHour) {
        this.limitFileUSEndOfDayHour = limitFileUSEndOfDayHour;
    }

    /**
     * @return the limitFileUSEndOfDayMinute
     */
    public Integer getLimitFileUSEndOfDayMinute() {
        return limitFileUSEndOfDayMinute;
    }

    /**
     * @param limitFileUSEndOfDayMinute the limitFileUSEndOfDayMinute to set
     */
    public void setLimitFileUSEndOfDayMinute(Integer limitFileUSEndOfDayMinute) {
        this.limitFileUSEndOfDayMinute = limitFileUSEndOfDayMinute;
    }

	@Override
	public void invokeTimerExpired_Orders() {
		//if (debugMode) {
			timerExpired("ORDERS_END_OF_DAY", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	@Override
	public void invokeTimerExpired_LimitFile_USandGlobal() {
	//	if (debugMode) {
			timerExpired("LIMIT_FILE_US_AND_GLOBAL_END_OF_DAY_ID", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	@Override
	public void invokeTimerExpired_LimitFile_Non_USandGlobal() {
		//if (debugMode) {
			timerExpired("LIMIT_FILE_NON_US_AND_GLOBALEND_OF_DAY_ID", CSOrdersEndOfDayService.class.getSimpleName());
//		} else {
//			LOGGER.warn("Unexpected call, please check configuration");
//		}
	}

	public boolean getDebugMode() {
		return debugMode;
	}

	public void setDebugMode(boolean debugMode) {
		this.debugMode = debugMode;
	}
	
	
}
