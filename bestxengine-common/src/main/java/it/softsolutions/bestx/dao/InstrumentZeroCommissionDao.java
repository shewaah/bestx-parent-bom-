/*
* Project Name : bestxengine-common
* First created by: $Author$ - SoftSolutions! Italy
* Creation date: $Date$
* Purpose: this class verify if the order customer is set to use Price Forge and if the ticker is allowed for the order customer 
*
* Copright 1997-2012 SoftSolutions! srl
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
package it.softsolutions.bestx.dao;

import org.springframework.jdbc.core.JdbcTemplate;

public interface InstrumentZeroCommissionDao
{
	/**
	 * Set JdbcTemplate
	 * @param jdbcTemplate
	 */
   public void setJdbcTemplate(JdbcTemplate jdbcTemplate);

   /**
    * Here we extract the order side to check against the current order one.
    * @param isin
    * @return the order side in the InstrumentZeroCommissionTable or null if nothing will be found.
    */
   public String getZeroCommissionOrderSide(final String isin);
}