/**
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
package it.softsolutions.bestx.dao.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import it.softsolutions.bestx.dao.DatabaseStatusDao;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product
 * First created by: davide.rossoni
 * Creation date: 24/feb/2015
 * 
 */
public class SqlDatabaseStatusDao implements DatabaseStatusDao {
	
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDatabaseStatusDao.class);

	private JdbcTemplate jdbcTemplate;
    
	@Override
    public int selectCountAs(String tableName) {
		LOGGER.trace("tableName = {}", tableName);
		
		String sql = "select count(*) from " + tableName;
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

	    return count == null? 0:count.intValue();
    }
   
	@Override
    public Date selectOldestRecordInProposal() {
		LOGGER.trace("");
        
		String sql = "SELECT MIN(Timestamp) FROM Proposal";
		Date timestamp = (Date) jdbcTemplate.queryForObject(sql, Date.class);
        
	    return timestamp;
    }

	@Override
    public Date selectOldestRecordInPriceTable() {
		LOGGER.trace("");
        
		String sql = "SELECT MIN(ArrivalTime) FROM PriceTable";
		Date timestamp = (Date) jdbcTemplate.queryForObject(sql, Date.class);
        
	    return timestamp;
    }

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
    public String getDatabaseStatus() {
		String res = "N/A";
		try {
			Connection connection = null;
			connection = jdbcTemplate.getDataSource().getConnection();
	        res = "connected = " + !connection.isClosed() + " > " + connection.toString();
        } catch (SQLException e) {
        	LOGGER.error("{}", e.getMessage(), e);
        }
		catch (NullPointerException e1) {
         LOGGER.error("{}", e1.getMessage(), e1);
        }
	    return res;
    }
}
