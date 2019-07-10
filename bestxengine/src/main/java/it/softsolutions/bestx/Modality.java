package it.softsolutions.bestx;

public interface Modality {

	public static enum Type {
		MONITOR, EXECUTION
	}
	
	void setModality(Modality.Type modality);
	
	Modality.Type getModality();
	
}
