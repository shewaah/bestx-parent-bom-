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

import java.util.Date;

import org.springframework.jdbc.core.JdbcTemplate;

import it.softsolutions.bestx.dao.SerialNumberDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 24/ago/2012
 * 
 **/
public class SqlSerialNumberDao implements SerialNumberDao {
    private static final String sqlDeleteIdentifier = "DELETE FROM SerialNumber WHERE SerialNumberId = ?";
    private static final String SQL = "exec getNextSerialNumber @SERIAL_NUMBER_ID=?, @LAST_DATE=?";

    private JdbcTemplate jdbcTemplate;

    /**
     * Set the jdbcTemplate used to execute queries.
     * 
     * @param jdbcTemplate
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }

    public long getNextNumber(String identifier) {
        return getNextNumber(identifier, null);
    }

    @Override
    public long getNextNumber(String identifier, Date date) {
        checkPreRequisites();
        Long res = jdbcTemplate.queryForObject(SQL, Long.class, new Object [] {identifier, date});
        return res == null? 0: res;
    }

    @Override
    public synchronized void resetNumber(String identifier) {
        jdbcTemplate.update(sqlDeleteIdentifier, new Object [] {identifier});
    }
}
