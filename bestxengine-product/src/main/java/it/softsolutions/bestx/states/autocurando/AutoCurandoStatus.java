/*
 * Copyright 1997-2012 SoftSolutions! srl 
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

package it.softsolutions.bestx.states.autocurando;

import it.softsolutions.bestx.management.AutoCurandoStatusMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: anna.cochetti Creation date: 2010-02-08
 * 
 **/
public enum AutoCurandoStatus implements AutoCurandoStatusMBean {
	INSTANCE;

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCurandoStatus.class);

	public static final String ACTIVE = "ATTIVO";
	public static final String SUSPENDED = "SOSPESO";
	public static final String NONE = "NESSUNO";
	
	private String autoCurandoStatus;
	private int totalCurandoPriceRequestsNumber;

	private int automaticCurandoOrdersNumber;
	private int totalAutoCurandoOrdersNumber;

	@Override
	public String getAutoCurandoStatus() {
		return autoCurandoStatus;
	}

	@Override
	public synchronized void setAutoCurandoStatus(String autoCurandoStatus) {
		LOGGER.debug("Changing AutoCurando Status to {} from {}", autoCurandoStatus, getAutoCurandoStatus());
		this.autoCurandoStatus = autoCurandoStatus;
		LOGGER.debug("Status change done.");
	}

	@Override
	public int getTotalCurandoPriceRequestsNumber() {
		return totalCurandoPriceRequestsNumber;
	}

	/**
	 * Inc total curando price requests number.
	 * 
	 * @return the int
	 */
	public int incTotalCurandoPriceRequestsNumber() {
		return ++totalCurandoPriceRequestsNumber;
	}

	@Override
	public int getAutomaticCurandoOrdersNumber() {
		// TODO Monitoring-BX
		return automaticCurandoOrdersNumber;
	}

	/**
	 * Increment automaticCurandoOrdersNumber requests number.
	 * 
	 * @return the int
	 */
	public int incAutomaticCurandoOrdersNumber() {
		// TODO Monitoring-BX
		return ++automaticCurandoOrdersNumber;
	}

	@Override
	public int getTotalAutoCurandoOrdersNumber() {
		// TODO Monitoring-BX
		return totalAutoCurandoOrdersNumber;
	}

	/**
	 * Increment totalAutoCurandoOrdersNumber requests number.
	 * 
	 * @return the int
	 */
	public int incTotalAutoCurandoOrdersNumber() {
		// TODO Monitoring-BX
		return ++totalAutoCurandoOrdersNumber;
	}
}
