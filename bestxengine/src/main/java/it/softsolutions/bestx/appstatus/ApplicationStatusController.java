package it.softsolutions.bestx.appstatus;

public class ApplicationStatusController implements ApplicationStatusControllerMBean {

	private ApplicationStatus applicationStatus;

	public ApplicationStatus getApplicationStatus() {
		return applicationStatus;
	}

	public void setApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
	}
	
	@Override
	public void establishExecutionApplicationStatusType() {
		this.applicationStatus.setType(ApplicationStatus.Type.EXECUTION);
	}

	@Override
	public void establishMonitorApplicationStatusType() {
		this.applicationStatus.setType(ApplicationStatus.Type.MONITOR);
	}

	@Override
	public String retrieveCurrentApplicationStatusType() {
		return this.applicationStatus.getType().toString();
	}
	
}
