package it.softsolutions.bestx.appstatus;

public interface ApplicationStatusControllerMBean {

	void establishExecutionApplicationStatusType();

	void establishNormalApplicationStatusType();

	String retrieveCurrentApplicationStatusType();
	
}
