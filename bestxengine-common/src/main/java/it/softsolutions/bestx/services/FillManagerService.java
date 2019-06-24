/*
* Project Name : ${project_name} 
* First created by: ${user} 
* Creation date: ${date} 
* 
* Copright 1997-${year} SoftSolutions! srl 
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
* 
*/
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 *  
 * Purpose: This DAO isn't an interface implementation because we need
 * the alreadySavedFill as a public static method.
 * Here we check if a fill received from the market is a new one or
 * has already been saved in the database.
 * The need to perform this check can arise in various situations, it
 * means that potentially every handler could call this method.
 * WE put calls to the method in the MOT/ETLX magnet handlers and in the generic
 * AkrosBaseOperationEventHandler (method onMarketExecutionReport).
 *
 */
public class FillManagerService
{
	private static final Logger LOGGER = LoggerFactory.getLogger(FillManagerService.class);
	private static JdbcTemplate jdbcTemplate;

	/**
	 * Set the jdbcTemplate used to execute queries.
	 * @param jdbcTemplate
	 */
	public static void setJdbcTemplate(JdbcTemplate jdbcTemplateNew) {
		jdbcTemplate = jdbcTemplateNew;
	}
	
	/**
	 * Check in the table TabOrderFill if the fill received from the market has
	 * already been saved.
	 * @param fill : market execution report received from the market
	 * @param order : the order
	 * @return true if the fill is already in the database, false otherwise
	 */
	public static boolean alreadySavedFill(final MarketExecutionReport fill,final Order order)
	{
		boolean saved = false;
		LOGGER.debug("Checking fill {}", fill);
		String sqlSelect = "SELECT ContractNo FROM tabOrderFill " +
				"WHERE numOrdine = '" + order.getFixOrderId() + "'";
		SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
		//When we receive a fill from the market we store the
		//ContractNo field in the marketExecutionReport field ticket
		String fillContractNo = fill.getTicket();
		while (rowSet.next()) {
				String dbContractNo = rowSet.getString("ContractNo");
				LOGGER.debug("Fill contract number : {}", fillContractNo);
				LOGGER.debug("Db loaded contract n : {}", dbContractNo);
				if (dbContractNo != null && dbContractNo.equals(fillContractNo))
				{
					saved = true;
					LOGGER.debug("Fill already stored!");
				}
		}
		//saved is true only if at least one db fill's contractNo matched the received fill's one
		return saved;
	}
}
