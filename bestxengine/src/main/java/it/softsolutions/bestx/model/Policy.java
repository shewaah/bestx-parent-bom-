/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.model;

import java.util.Set;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public class Policy {
	private String name;
	private String description;
	private Set<Venue> venues;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param id the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the venues
	 */
	public Set<Venue> getVenues() {
		return venues;
	}
	/**
	 * @param venues the venues to set
	 */
	public void setVenues(Set<Venue> venues) {
		this.venues = venues;
	}
	
	@Override
	public String toString()
	{
	   String policyData = "[ PolicyName:" + getName() + ";PolicyDescription:" + getDescription() + 
	   ";Venues:" + (venues != null ? venues : "null") + " ]";
	   return policyData;
	}
}
