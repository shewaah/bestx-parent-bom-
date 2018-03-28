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

import it.softsolutions.bestx.dao.InstrumentZeroCommissionDao;

import org.springframework.jdbc.core.JdbcTemplate;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class DefaultInstrumentZeroCommissionDao implements
		InstrumentZeroCommissionDao {

	protected JdbcTemplate jdbcTemplate;
	
	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.InstrumentZeroCommissionDao#setJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate)
	 */
	@Override
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		 this.jdbcTemplate = jdbcTemplate;
	}

	/* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.InstrumentZeroCommissionDao#getZeroCommissionOrderSide(java.lang.String)
	 * This implementation return in all cases null value
	 */
	@Override
	public String getZeroCommissionOrderSide(String isin) {
		//In Default implementation no isin have zero commission property
		return null;
	}

}

