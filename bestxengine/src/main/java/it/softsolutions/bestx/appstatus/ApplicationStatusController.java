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
	public void establishNormalApplicationStatusType() {
		this.applicationStatus.setType(ApplicationStatus.Type.NORMAL);
		
		// A list of existing operations must be retrieved and for each a
		// ApplicationStatusChange callback must be called
	}

	@Override
	public String retrieveCurrentApplicationStatusType() {
		return this.applicationStatus.getType().toString();
	}
	
}
