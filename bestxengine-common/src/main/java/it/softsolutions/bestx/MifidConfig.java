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
package it.softsolutions.bestx;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 22/ago/2012 
* 
**/
public class MifidConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(MifidConfig.class);

    private static final String sqlSelect = "SELECT * from MifidConfig";

    private int numRetry;
    private BigDecimal qtyLimit;
    private String propName;
    private String propCode;
    private String operatorCode;
    private BigDecimal qtyInternal;
    private JdbcTemplate jdbcTemplate;

    public void init() {
        LOGGER.debug("Load mifidConfig");
        jdbcTemplate.query(sqlSelect, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                upload(rset);
            }
        });
    }

    /**
     * @param jdbcTemplate
     *            the jdbcTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void upload(ResultSet rset) throws SQLException {
        numRetry = rset.getInt("NumRetry");
        qtyLimit = rset.getBigDecimal("QtyLimit");
        qtyInternal = rset.getBigDecimal("QtyInternal");
        propName = rset.getString("PropName");
        propCode = rset.getString("PropCode");
        operatorCode = rset.getString("OperatorCode");
    }

    /**
     * @return the numRetry
     */
    public int getNumRetry() {
        return numRetry;
    }

    /**
     * @return the qtyLimit
     */
    public BigDecimal getQtyLimit() {
        return qtyLimit;
    }

    /**
     * @return the propName
     */
    public String getPropName() {
        return propName;
    }

    /**
     * @return the propCode
     */
    public String getPropCode() {
        return propCode;
    }

    /**
     * @return the operatorCode
     */
    public String getOperatorCode() {
        return operatorCode;
    }

    /**
     * @return the qtyInternal
     */
    public BigDecimal getQtyInternal() {
        return qtyInternal;
    }
}
