package it.softsolutions.bestx.appstatus;

public class ApplicationStatus {

	public static enum Type {
		MONITOR,
		EXECUTION
	}
	
	private ApplicationStatus.Type type = ApplicationStatus.Type.EXECUTION; // Default

	public synchronized ApplicationStatus.Type getType() {
		return type;
	}

	public synchronized void setType(ApplicationStatus.Type type) {
		this.type = type;
	}
	
}
