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

package it.softsolutions.bestx.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-cs
 * First created by: anna.cochetti
 * Creation date: 17/ago/2015
 * 
 **/

public class BestXConfigurationPropertyLoader {
    static final String PROPERTY_VALUE_COLUMN= "PropertyValue";
    static final String PROPERTY_NAME_COLUMN= "PropertyName";
    static final String PROPERTY_STATUS_COLUMN= "Active";
    

	public BestXConfigurationPropertyLoader() {
		super();
	}

	protected static JdbcTemplate jdbcTemplate;
	protected static String configurationTable;

	/**
	 * @return the jdbcTemplate
	 */
	public JdbcTemplate getJdbcTemplate() {
	    return jdbcTemplate;
	}

	/**
	 * @param jdbcTemplate the jdbcTemplate to set
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		BestXConfigurationPropertyLoader.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * @return the configurationTable
	 */
	public String getConfigurationTable() {
	    return configurationTable;
	}

	/**
	 * @param configurationTable the configurationTable to set
	 */
	public void setConfigurationTable(String configurationTable) {
		BestXConfigurationPropertyLoader.configurationTable = configurationTable;
	}

	public static Object getProperty(String propertyName) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    Object propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        propertyValue = rowSet.getObject(propertyName);
	    }
	    return propertyValue;
	}

	public static Object getProperty(String propertyName, Object defaultValue) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    Object propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        propertyValue = rowSet.getObject(propertyName);
	    }
	    return propertyValue == null ? defaultValue : propertyValue;
	}

	public static String getStringProperty(String propertyName) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    String propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        propertyValue = (String) rowSet.getObject(PROPERTY_VALUE_COLUMN);
	    }
	    return propertyValue;
	}

	public static String getStringProperty(String propertyName, String defaultValue) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    String propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        propertyValue = (String) rowSet.getObject(PROPERTY_VALUE_COLUMN);
	    }
	    return propertyValue == null ? defaultValue : propertyValue;
	}

	public static Boolean getBooleanProperty(String propertyName) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    Boolean propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        String tmpPropertyValue = (String) rowSet.getObject(PROPERTY_VALUE_COLUMN);
	        propertyValue = new Boolean(tmpPropertyValue);
	    }
	    return propertyValue;
	}

	public static Boolean getBooleanProperty(String propertyName, Boolean defaultValue) {
	    String select = "SELECT " + PROPERTY_VALUE_COLUMN + " FROM " + configurationTable + " WHERE " + PROPERTY_NAME_COLUMN + " = '" + propertyName + "' AND " + PROPERTY_STATUS_COLUMN + " = 1";
	    Boolean propertyValue = null;
	    SqlRowSet rowSet = jdbcTemplate.queryForRowSet(select);
	    while (rowSet.next()) {
	        String tmpPropertyValue = (String) rowSet.getObject(PROPERTY_VALUE_COLUMN);
	        propertyValue = new Boolean(tmpPropertyValue);
	    }
	    return propertyValue == null ? defaultValue : propertyValue;
	}

}