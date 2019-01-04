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

import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**  
 *
 * Purpose: this class is mainly for testing of TSOX flows, including Internalization on RTFI
 *
 * - setup required for tests:
 *   - the test launches a Fix Client which sends orders, so a OMS2FixGateway must be up&running
 *   - OMS2FixGateway sends orders to a running session of BestX
 *   - BestX uses BLPConnectorFake and RTFIBSConnectorFake to receive books, and sends QuoteRequest/NewOrderSingle to TSOX, so
 *   - TradeStac bloomberg must be running, connected to TSOXSimulator
 *   - TSOXSimulator must be up&running
 *   
 * Project Name : bestxengine-cs 
 * First created by: paolo.midali 
 * Creation date: 5-mar-2013 
 * 
 **/
public class QuartzSchedulers_Test {
    // FIX configuration for connection to Xt2FixGateway


    public class QuartzJobHelper implements TimerEventListener {

        public int timersTriggered;
        private String BASE_JOB_NAME = "TestJobName";
        private int expectedTriggers;

        // one element is put in the queue when the expected number of timers is received
        // (expectedTimersAtEnd is the number of timers that should be still alive at the end of the test)
        public BlockingQueue<String> resultQueue = new ArrayBlockingQueue<String>(1);
        
        public void setupTimers(int numTimers, int intervalSecs, int expectedTriggers, boolean uniqueJobName, String groupName, boolean useDurableTimers) {
            timersTriggered = 0;
            this.expectedTriggers = expectedTriggers;

            if ( (numTimers > 0) && (expectedTriggers <= 0) ) {
                org.junit.Assert.fail("Invalid number of expected triggers: " + expectedTriggers);
            }
            
            Date startDate = new Date();
            for (int i = 0; i < numTimers; i++) {
                String jobName = BASE_JOB_NAME;
                if (uniqueJobName ) jobName += "#" + (i+1);

                setupTimer(DateUtils.toCalendar(DateUtils.addSeconds(startDate, (i+1)*intervalSecs)), jobName, groupName, useDurableTimers);
            }
        }

        /**
         * Setup timer with no listener
         * 
         * @param calendar
         * @param jobName
         * @param groupName
         */
        private void setupTimer(Calendar calendar, String jobName, String groupName, boolean useDurableTimers) {
            // getting the current time
            long expireTime = calendar.getTimeInMillis();
            // calculating time remained from now to the EndOfDay time
            long timeToExpire = expireTime - Calendar.getInstance().getTimeInMillis();

            // setupTimer bind the timer for this service
            try {
                SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
                
                //System.out.println("Creating new job: " + jobName + " , " + groupName);
                JobDetail newJob = simpleTimerManager.createNewJob(jobName, groupName, useDurableTimers, false /* recovery */, true);

                //this timer is not repeatable
                Trigger trigger = simpleTimerManager.createNewTrigger(jobName, groupName, false, timeToExpire);
                simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, useDurableTimers);
            } catch (SchedulerException e) {
                org.junit.Assert.fail(e.getMessage());
            }
        }

        @Override
        public void timerExpired(String jobName, String groupName) {
            timersTriggered++;
            
            if (timersTriggered == expectedTriggers) {
                try {
                    resultQueue.put(jobName + "/" + groupName);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("****** Expected number of timers [" + expectedTriggers + "] reached, putting element [" + jobName + "/" + groupName + "] in queue");
            }
        }

    }

    static ClassPathXmlApplicationContext context;

    @BeforeClass 
    public static void setupContext() {
        // jdbcTemplate is used to read the states an order has crossed.
        // it refers to pooledDataSource, which is configured according to 
        // hibernate.connection.url parameter in bestxengine-cs/src/test/config/hibernate.properties
        context = new ClassPathXmlApplicationContext("cs-spring.xml");
        
        try {
            SimpleTimerManager.getInstance().start();
            
            Thread.sleep(5000);
        } catch (SchedulerException | InterruptedException e) {
            org.junit.Assert.fail(e.getMessage());
        };
    }

    @AfterClass
    public static void tearDown() throws Exception {
        context.close();    Thread.sleep(1000);
        SimpleTimerManager.getInstance().stopScheduler();
    }
    
    @Test (timeout=5000)
    public void testSingleTimerNotDurable()  throws Exception {
        testSingleTimer(false);
    }
    @Test (timeout=5000)
    public void testSingleTimerDurable()  throws Exception {
        testSingleTimer(true);
    }
    
    public void testSingleTimer(boolean useDurableTimers)  throws Exception {
        final String TEST_GROUP_NAME = "testSingleTimer";
        //System.out.println("--------- Starting " + TEST_GROUP_NAME);
        int intervalSecs = 1;
        int numTimers = 1;
        int expectedTimers = 1;

        QuartzJobHelper qjl = new QuartzJobHelper();

        JobExecutionDispatcher.INSTANCE.addTimerEventListener(TEST_GROUP_NAME, qjl);
        
        qjl.setupTimers(numTimers, intervalSecs, expectedTimers, false, TEST_GROUP_NAME, useDurableTimers);

        String result = qjl.resultQueue.take();
        System.out.println(" Received result: " + result);
        Thread.sleep(2000); // wait for any other (unexpected) timer

        JobExecutionDispatcher.INSTANCE.removeTimerEventListener(TEST_GROUP_NAME);
        org.junit.Assert.assertEquals(expectedTimers, qjl.timersTriggered);

    }

    @Test (timeout=10000)
    public void testTripleTimerDifferentJobNamesNotDurable()  throws Exception {
        testTripleTimerDifferentJobNames(false);
    }
    @Test (timeout=10000)
    public void testTripleTimerDifferentJobNamesDurable()  throws Exception {
        testTripleTimerDifferentJobNames(true);
    }
    
    // create three timers with same groupname (used to register as listener)
    // and DIFFERENT jobname for each timer: ok, they are all triggerd, this is the
    // right way to create them
    //
    // p.s.: some problem if setupTimer has "durable" parameter set to true, verify... 
    //
    public void testTripleTimerDifferentJobNames(boolean useDurableTimers)  throws Exception {
        final String TEST_GROUP_NAME = "testTripleTimerDifferentJobNames";
        //System.out.println("--------- Starting " + TEST_GROUP_NAME);
        
        int intervalSecs = 1;
        int numTimers = 3;
        int expectedTimers = 3;

        QuartzJobHelper qjl = new QuartzJobHelper();

        JobExecutionDispatcher.INSTANCE.addTimerEventListener(TEST_GROUP_NAME, qjl);
        
        qjl.setupTimers(numTimers, intervalSecs, expectedTimers, true, TEST_GROUP_NAME, useDurableTimers);


        String result = qjl.resultQueue.take();
        System.out.println(" Received result: " + result);
        Thread.sleep(2000); // wait for any other (unexpected) timer

        JobExecutionDispatcher.INSTANCE.removeTimerEventListener(TEST_GROUP_NAME);
        org.junit.Assert.assertEquals(expectedTimers, qjl.timersTriggered);
    }    
    

    @Test (timeout=10000) // 20 secs max
    public void testTripleTimerSameJobNamesNotDurable()  throws Exception {
        testTripleTimerSameJobNames(false);
    }
    @Test (timeout=10000) // 20 secs max
    public void testTripleTimerSameJobNamesDurable()  throws Exception {
        testTripleTimerSameJobNames(true);
    }
    
    // in this case, I create three timers, with the same group name (used to register as listener)
    // AND THE SAME JOB NAME --> every new timer overwrites the new one, so only one is actually triggered
    //
    // --->> it is not correct to create multiple timers with the same jobnames and the same groupnames !!!!!!! <<---
    //
    public void testTripleTimerSameJobNames(boolean useDurableTimers)  throws Exception {
        final String TEST_GROUP_NAME = "testTripleTimerSameJobNames";
        //System.out.println("--------- Starting " + TEST_GROUP_NAME);
        
        int intervalSecs = 1;
        int numTimers = 3;
        int expectedTimers = 1;

        QuartzJobHelper qjl = new QuartzJobHelper();

        JobExecutionDispatcher.INSTANCE.addTimerEventListener(TEST_GROUP_NAME, qjl);
        
        qjl.setupTimers(numTimers, intervalSecs, expectedTimers, false, TEST_GROUP_NAME, useDurableTimers);

        String result = qjl.resultQueue.take();
        System.out.println(" Received result: " + result);
        Thread.sleep(2000); // wait for any other (unexpected) timer

        JobExecutionDispatcher.INSTANCE.removeTimerEventListener(TEST_GROUP_NAME);
        org.junit.Assert.assertEquals(expectedTimers, qjl.timersTriggered);
    }    
}
