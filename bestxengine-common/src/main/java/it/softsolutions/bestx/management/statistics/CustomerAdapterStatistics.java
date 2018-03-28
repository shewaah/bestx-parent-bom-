package it.softsolutions.bestx.management.statistics;

import it.softsolutions.bestx.services.timer.quartz.JobExecutionDispatcher;
import it.softsolutions.bestx.services.timer.quartz.SimpleTimerManager;
import it.softsolutions.bestx.services.timer.quartz.TimerEventListener;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

public class CustomerAdapterStatistics implements TimerEventListener {

	public static String RECEIVED_ORDERS_STATISTICS_LABEL = "RECEIVED_ORDERS_STATISTICS_TIMER";
	protected int orderReceived;
	protected String channelName;
	protected long intervalTimeInSecs;

	public CustomerAdapterStatistics() {
		super();
	}

	public int getOrderNumberInLastInterval() {
		return orderReceived;
	}

	public void setIntervalTimeInSecs(long intervalTimeInSecs) {
		this.intervalTimeInSecs = intervalTimeInSecs;
	}
	
	public long getIntervalTimeInSecs() {
		return intervalTimeInSecs;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	/**
	 * Create a not persistent timer repeating at fixed intervals of time
	 * @param timerName the timer name
	 * @throws SchedulerException
	 */
    public void createTimer(String jobName, String groupName) throws SchedulerException {
		JobExecutionDispatcher.INSTANCE.addTimerEventListener(groupName, this);
    	
        SimpleTimerManager simpleTimerManager = SimpleTimerManager.getInstance();
        JobDetail newJob = simpleTimerManager.createNewJob(jobName, groupName, false, false, false);
        Trigger trigger = simpleTimerManager.createNewTrigger(jobName, groupName, true, intervalTimeInSecs * 1000);
        simpleTimerManager.scheduleJobWithTrigger(newJob, trigger, false);
    }

	@Override
    public void timerExpired(String jobName, String groupName) {
		orderReceived = 0;
    }

}