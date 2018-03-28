/*
 * Copyright 1997-2015 SoftSolutions! srl 
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

package it.softsolutions.bestx.services.customservice;

import quickfix.field.SecurityType;

/**
 *
 * Purpose: this class is an interface for all custom services added for clients' special needs  
 *
 * Project Name : bestxengine
 * First created by: anna.cochetti
 * Creation date: 10/ago/2015
 * 
 **/

public interface CustomService {

	/**
	 * Send the request towards CustomService. Only one security
	 * 
	 * @param securityType
	 *            security type, one of {@link SecurityType}
	 * @param initialLoad
	 *            true if this is the Start of Day functionality, false when it is a BestX instrument request
	 * @param securityIDs
	 *            the security whose loading we are requesting
	 * @throws CustomServiceException
	 *            when something goes wrong
	 * @throws JMSException
	 *            JMS errors
	 */
	public abstract void sendRequest(String operationId, boolean initialLoad,
			String securityId) throws CustomServiceException;

	/**
	 * Reset the request after an external event, for example timeout reached
	 * 
	 * @param securityId the security whose loading we are requesting
	 */
	public abstract void resetRequest(String securityId, String operationId);

	/**
	 * Method used to check if the custom service is active. It is supposed to be set in the configuration.
	 * @return true if active, false otherwise
	 */
	public abstract boolean isActive();

	/**
	 * Method used to check if the custom service is available, meaning that the connection with the service has 
	 * been setup correctly.
	 * 
	 * @return true if available, false otherwise
	 */
	public abstract boolean isAvailable();

}