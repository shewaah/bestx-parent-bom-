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
package it.softsolutions.bestx.dao.sql;

import java.util.Calendar;

import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import it.softsolutions.bestx.dao.InstrumentStatusNotifierDAO;
import it.softsolutions.bestx.services.DateService;

/**  
*
* Purpose: This DAO is the database interface of the InstrumentStatusNotifier service.
* It performs the SELECT/UPDATE needed when BestX:FI-A asks or sets the instrument
* quoting status.
* It has specific methods for the volatility call status.
* It has been the need to manage the MOT volatility call that drove us to the
* creation of the service and this DAO.
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/ 
public class SqlInstrumentStatusNotifierDAO implements InstrumentStatusNotifierDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlInstrumentStatusNotifierDAO.class);
    private static final String START_VOLATILITY_CALL = " Asta volatilita : ";
    private static final String STOP_VOLATILITY_CALL = " Fine asta : ";
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

    public void updateStateDescForStartVolatilityCall(String orderId) {
        LOGGER.info("Append to status description the volatility call start.");
        updateStateDesc(orderId, START_VOLATILITY_CALL);
    }

    public void updateStateDescForEndVolatilityCall(String orderId) {
        LOGGER.info("Append to status description the volatility call end.");
        updateStateDesc(orderId, STOP_VOLATILITY_CALL);
    }
	
	/**
	 * Implementation of the update of the TabHistoryStati table
	 * 
	 * @param orderId Order id
	 * @param addToComment comment to add to the status description
	 */
	private synchronized void updateStateDesc(String orderId, String addToComment)
	{
		String currentTime = DateService.format("HH:mm:ss.SSS", Calendar.getInstance().getTime());
		String sqlSelect = "select tho.stato,tho.DescrizioneEvento " +
						   "from TabHistoryOrdini tho " +
						   "where tho.NumOrdine = '" + orderId + "'";
		
		LOGGER.info("Query to find state and state description : " + sqlSelect);
		SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
		if (rowSet.next())
		{
			String status = rowSet.getString("Stato");
			
			SerialClob clob = (SerialClob) rowSet.getObject("DescrizioneEvento");
			String statusDesc = "";
			if(clob != null) {
   			try {
   				statusDesc = clob.getSubString(1,  (int) clob.length());
   			} catch (SerialException e) {
   			    LOGGER.error("{}", e.getMessage(), e);
   			}
			}
			statusDesc += addToComment + currentTime + ".";
			
			LOGGER.info("Status description updated : " + statusDesc);
			String sqlUpdate = "UPDATE TabHistoryOrdini " +
							   "SET DescrizioneEvento = '" + statusDesc + "' " +
							   "WHERE NumOrdine = '" + orderId + "'";
			LOGGER.info("Updating TabHistoryOrdini : " + sqlUpdate);
			jdbcTemplate.execute(sqlUpdate);
			sqlUpdate = "UPDATE TabHistoryStati " +
			   			"SET DescrizioneEvento = '" + statusDesc + "' " +
			   			"WHERE NumOrdine = '" + orderId + "' " +
			   			"AND Stato = '" + status + "'" +
			   			"AND SaveTime = ( SELECT MAX(SaveTime) " +
			   			"FROM TabHistoryStati " +
			   			"WHERE NumOrdine = '"+ orderId + "')";
			LOGGER.info("Updating TabHistoryStati : " + sqlUpdate);
			jdbcTemplate.execute(sqlUpdate);
		}
		LOGGER.info("Status description updated.");
	}
}
