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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.model.Country;
import it.softsolutions.bestx.model.Instrument;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation
 * date: 12/set/2013
 * 
 **/
public class SqlInstrumentDao implements InstrumentDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlInstrumentStatusNotifierDAO.class);
	private static final String SELECT_INSTRUMENT_BY_ISIN = "SELECT * FROM InstrumentsTable WHERE isin='";
	private static final String SELECT_LAST_ISSUED_INSTRUMENT = "SELECT top 1 * FROM InstrumentsTable order by IssueDate desc;";
	private static final String SELECT_COUNT_INSTRUMENT = "SELECT count(*) FROM InstrumentsTable ";

	private JdbcTemplate jdbcTemplate;

	@Override
	public Instrument getInstrumentByIsin(String isin) {
		Instrument instrument = null;
		LOGGER.info("Query to find state and state description : " + SELECT_INSTRUMENT_BY_ISIN + isin + "'");
		SqlRowSet rowSet = jdbcTemplate.queryForRowSet(SELECT_INSTRUMENT_BY_ISIN + isin + "'");
		if (rowSet.next()) {
			instrument = new Instrument();
			instrument.setIsin(isin);
			String countryCode = rowSet.getString("CountryCode");
			Country country = new Country();
			country.setCode(countryCode);
			instrument.setCountry(country);
			instrument.setInInventory(rowSet.getBoolean("InInventory"));
		}
		return instrument;
	}
	
	@Override
	public Instrument getLastIssuedInstrument() {
		Instrument instrument = null;
		LOGGER.info("Query to find last issued instrument : " + SELECT_LAST_ISSUED_INSTRUMENT + "");
		SqlRowSet rowSet = jdbcTemplate.queryForRowSet(SELECT_LAST_ISSUED_INSTRUMENT);
		if (rowSet.next()) {
			instrument = new Instrument();
			instrument.setIsin(rowSet.getString("ISIN"));
			String countryCode = rowSet.getString("CountryCode");
			Country country = new Country();
			country.setCode(countryCode);
			instrument.setCountry(country);
			instrument.setInInventory(rowSet.getBoolean("InInventory"));
		}
		return instrument;
	}

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
	public long getInstrumentCount() {
		LOGGER.info("Query to find state and state description : " + SELECT_COUNT_INSTRUMENT);
		
		// TODO Monitoring-BX check 
		Long count = jdbcTemplate.queryForObject(SELECT_INSTRUMENT_BY_ISIN, Long.class);

		return count == null? 0 : count;
	}

	

}
