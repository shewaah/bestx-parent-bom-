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

import it.softsolutions.bestx.dao.RPIDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.RPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;


/**  
*
* Purpose : RPI data access DAO.
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class SqlRPIDao implements RPIDao {
    private static final String sqlSelectAll =
        "SELECT Month, Year, Value" +
        " FROM RPITable";

    private JdbcTemplate jdbcTemplate;
    
    /* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.hibernate.RPIDao#setSessionFactory(org.hibernate.SessionFactory)
	 */
    
    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("jdbcTemplate not set");
        }
    }

    /**
     * Set the jdbcTemplate used to execute queries
     * @param jdbcTemplate
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /* (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.hibernate.RPIDao#getAllRPI()
	 */
    public Collection<RPI> getAllRPI() throws DataAccessException {
        checkPreRequisites();
        
        final LinkedList<RPI> beanList = new LinkedList<RPI>();

        jdbcTemplate.query(sqlSelectAll, (Object[]) null, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                RPI bean = new RPI();
                bean.setMonth(rset.getInt("Month"));
                bean.setYear(rset.getInt("Year"));
                bean.setValue(rset.getBigDecimal("Value"));
                beanList.add(bean);             
            }});
        
        return beanList;
    }
}
