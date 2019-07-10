
package it.softsolutions.bestx.management;
 

// TODO: Auto-generated Javadoc
/**
 * The Interface ModalityMBean.
 */
public interface ModalityControllerMBean {

	/**
	 * Establish monitor modality.
	 */
	void establishMonitorModality();
	
	/**
	 * Establish execution modality.
	 */
	void establishExecutionModality();
	
	/**
	 * Retrieve current modality.
	 *
	 * @return the string
	 */
	String retrieveCurrentModality();
  
}
