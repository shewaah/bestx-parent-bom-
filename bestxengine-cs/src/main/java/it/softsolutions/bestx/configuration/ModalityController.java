package it.softsolutions.bestx.configuration;

import it.softsolutions.bestx.Modality;
import it.softsolutions.bestx.management.ModalityControllerMBean;

public class ModalityController implements ModalityControllerMBean {
	
	private Modality modality;
	
	public Modality getModality() {
		return modality;
	}

	public void setModality(Modality modality) {
		this.modality = modality;
	}

	@Override
	public void establishMonitorModality() {
		this.modality.setModality(Modality.Type.MONITOR);
		
		// A list of existing operations must be retrieved and for each a ModalityChange callback must be called
	}

	@Override
	public void establishExecutionModality() {
		this.modality.setModality(Modality.Type.EXECUTION);
		
		// A list of existing operations must be retrieved and for each a ModalityChange callback must be called
	}

	@Override
	public String retrieveCurrentModality() {
		return this.modality.getModality().toString();
	}

}
