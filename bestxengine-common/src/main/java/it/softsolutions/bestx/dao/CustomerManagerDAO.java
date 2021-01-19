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

import it.softsolutions.bestx.model.Order;

public interface CustomerManagerDAO {
   
   /**
    * Set the jdbcTemplate used to execute queries 
    * 
    * @param jdbcTemplate the JDBCTemplate to set
    */
   public void setJdbcTemplate(JdbcTemplate jdbcTemplate);
   
   /**
    * Check the CustomerNotAllowedTicker to verify if the RTFI ticker
    * of the instrument is allowed for the customer
    * 
    * @param order Order class used to retrieve the customer object
    * @return return true if ticker is in the list of those not allowed for the customer
    */
   public boolean isTheTickerNotAllowedForTheCustomer(Order order);
   
}
