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

import it.softsolutions.bestx.dao.ExcessDao;
import it.softsolutions.bestx.model.Customer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**  
*
* Purpose : DAO that calculates the excess towards buy orders or sell orders for the specific
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class SqlExcessDao implements ExcessDao {
	private JdbcTemplate jdbcTemplate;
	
	public void init() {
	}

   /**
    * Set the jdbcTemplate used to execute queries 
    * 
    * @param jdbcTemplate the JDBCTemplate to set
    */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.sql.ExcessDao#getLimit(it.softsolutions.bestx.model.Customer)
	 */
	public double getLimit(Customer customer) throws Exception {
		final double limit;
		SqlRowSet rset = jdbcTemplate.queryForRowSet("select QtySoglia from customerTable where clientCode = '" + 
			customer.getFixId() + "'", null);
		rset.first();
		limit = rset.getDouble("QtySoglia");
		return limit;
	}
		
	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.sql.ExcessDao#getCurrentExcess(it.softsolutions.bestx.model.Customer)
	 */
	public double getCurrentExcess(Customer customer) throws Exception {
		final double sbilancio;
		SqlRowSet rset = jdbcTemplate.queryForRowSet(
		        "select sum(case lato when '2' " + 
				"then -1 * (quantita / exchangeRate) " +
				"when '1' then (quantita / exchangeRate) end) as sbilancio " +
				"from tabhistoryOrdini inner join currencyExchange on " + 
				"currencyExchange.valuta = tabhistoryordini.valuta " +
				"where datediff(day, dataOraRicezione, getDate()) = 0 " + 
				"and cliente = '" + customer.getFixId() + "' " +
				"and stato = 'StateExecuted'", null);
		rset.first();
    	sbilancio = rset.getDouble("sbilancio");
		return sbilancio; 
	}

}
