package it.softsolutions.bestx.appstatus;

public class ApplicationStatus {

	public static enum Type {
		INITIAL_MONITORING,
		NORMAL
	}
	
	private ApplicationStatus.Type type = ApplicationStatus.Type.NORMAL; // Default

	public ApplicationStatus.Type getType() {
		return type;
	}

	public void setType(ApplicationStatus.Type type) {
		this.type = type;
	}
	
}
