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
package it.softsolutions.bestx.dao.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import it.softsolutions.bestx.dao.BestXConfigurationDao;
import it.softsolutions.bestx.dao.bean.BestXConfiguration;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation
 * date: 12/set/2013
 * 
 **/
public class SqlBestXConfigurationDao implements BestXConfigurationDao {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlBestXConfigurationDao.class);
	
	private static final String SELECT_ALL_CONFIGURATIONS = "SELECT * FROM BestXConfiguration WHERE Active = 1";
	private static final String SELECT_PROPERTY = "SELECT PropertyValue FROM BestXConfiguration WHERE PropertyName = ? AND Active = 1";
	
	private JdbcTemplate jdbcTemplate;

	/**
	 * Set the jdbcTemplate used to execute queries
	 * 
	 * @param jdbcTemplate
	 *            the JDBCTemplate to set
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

    @Override
    public List<BestXConfiguration> retrieveAllConfigurations() throws Exception {
        LOGGER.debug("Load all the BestX configurations");
        
        final List<BestXConfiguration> bestXConfigurations = new ArrayList<>();
        
        jdbcTemplate.query(SELECT_ALL_CONFIGURATIONS, new RowCallbackHandler() {
            @Override
			public void processRow(ResultSet rset) throws SQLException {
                BestXConfiguration bestXConfiguration = new BestXConfiguration();
                bestXConfiguration.setPropertyName(rset.getString("PropertyName"));
                bestXConfiguration.setPropertyValue(rset.getString("PropertyValue"));
                bestXConfiguration.setActive(true);
                
                bestXConfigurations.add(bestXConfiguration);
            }}
        );
        
        return bestXConfigurations;
    }

	@Override
	public Object loadProperty(String propertyName) {
		LOGGER.debug("Loading {} property from the BestX configuration table");
		
		Object propertyValue = jdbcTemplate.queryForObject(SELECT_PROPERTY, new Object [] {propertyName}, new RowMapper<Object>() {

			@Override
			public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getInt(1);
			}
			
		});
		return propertyValue;
	}

}
