/*
 * Copyright 1997-2018 SoftSolutions! srl 
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

package it.softsolutions.bestx;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: stefano.pontillo 
 * Creation date: 25 set 2018 
 * 
 **/
public class CurandoTimerProfile implements Cloneable {

	private static final Logger LOGGER = LoggerFactory.getLogger(CurandoTimerProfile.class);

	private String profileName;
	private List<Double> threshold;
	private List<Integer> timeout;



	public CurandoTimerProfile(String profileName){
		this.profileName = profileName;
		this.threshold = new ArrayList<Double>();
		this.timeout = new ArrayList<Integer>();
	}

	/**
	 * Return default timer configuration
	 * 
	 * @return
	 */
	public int getTimeForDeviation() {
		int result = -1;
		if (this.timeout.size() > this.threshold.size()) {
			result = this.timeout.get(this.timeout.size() - 1);
		}
		return result;
	}


	/**
	 * Return timer configuration for the given deviation
	 * 
	 * @param deviation
	 * @return
	 */
	public int getTimeForDeviation(Double deviation) {
		if(deviation == null) {
			LOGGER.info("Applying default timeout " + getTimeForDeviation() + " for null deviation. No valid best price has been discovered");
			return getTimeForDeviation();
		}
		int result = -1;

		for (int i = 0; i < this.threshold.size(); i++) {
			double value = this.threshold.get(i); 
			if (deviation <= value) {
				result = this.timeout.get(i);
				break;
			}
		}
		if (result < 0) {
			//Return the default value if any
			if (this.timeout.size() > this.threshold.size()) {
				result = this.timeout.get(this.timeout.size() - 1);
			}
		}
		return result;
	}

	/**
	 * @return the profileName
	 */
	public String getProfileName() {
		return profileName;
	}

	/**
	 * @param profileName the profileName to set
	 */
	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	/**
	 * Set index threshold
	 * 
	 * @param index
	 * @param threshold
	 */
	public void setThreshold(int index, Double threshold) {
		if (this.threshold.size() > index) {
			this.threshold.set(index, threshold);
		} else {
			this.threshold.add(index, threshold);
		}
	}

	/**
	 * Set index timeout value
	 * 
	 * @param index
	 * @param timeout
	 */
	public void setTimeout(int index, Integer timeout) {
		if (this.timeout.size() > index) {
			this.timeout.set(index, timeout);
		} else {
			this.timeout.add(index, timeout);
		}
	}

	/**
	 * Sets default timeout
	 * @param timeout
	 */
	public void setTimeout(Integer timeout) {
		int index = this.timeout.size();
		setTimeout(index, timeout);
	}


	@Override
	protected CurandoTimerProfile clone() throws CloneNotSupportedException {
		CurandoTimerProfile newObj = new CurandoTimerProfile(this.profileName);
		newObj.threshold = new ArrayList<Double>(this.threshold);
		newObj.timeout = new ArrayList<Integer>(this.timeout);
		return newObj;
	}

}
