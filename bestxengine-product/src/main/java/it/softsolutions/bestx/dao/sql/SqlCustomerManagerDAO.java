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

import it.softsolutions.bestx.dao.CustomerManagerDAO;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.PriceForgeCustomerManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;

/**  
*
* Purpose : this class verify if the order customer is set to use Price Forge and if the ticker 
*
* Project Name : bestxengine-product 
* First created by: davide.rossoni 
* Creation date: 24/ago/2012 
* 
**/
public class SqlCustomerManagerDAO implements CustomerManagerDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(PriceForgeCustomerManager.class);

    private JdbcTemplate jdbcTemplate;

    private static final String selectCustomer = "SELECT * FROM UnwantedCustomerTable WHERE ClientCode = ?";
    private static final String selectAllowedTickersForCustomer = "SELECT TickerList FROM CustomerNotAllowedTicker WHERE CustomerCode = ?";

    /**
     * Set the jdbcTemplate used to execute queries
     * 
     * @param jdbcTemplate
     *            the JDBCTemplate to set
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }

    /**
     * Check the UnwantedCustomerTable to see if the customer is available, if not this is a valid Price Forge customer, if the query
     * returns a result than this customer is an unwanted customer for the Price Forge.
     * 
     * @param order
     *            Order class used to retrieve the customer object
     * @return true if the customer IS NOT in the table, false otherwise
     */
    public boolean isAPriceForgeCustomer(Order order) {
        checkPreRequisites();
        final Customer customer = order.getCustomer();

        LOGGER.debug("Order " + order.getFixOrderId() + ". Check if the customer " + customer.getName() + ", fix id " + customer.getFixId() + ", is a Price Forge one.");

        Boolean pfCustomer = (Boolean) this.jdbcTemplate.query(selectCustomer, new PreparedStatementSetter() {
            public void setValues(PreparedStatement stmt) throws SQLException {
                // 1st param : Customer code
                stmt.setString(1, customer.getFixId());
            }
        }, new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                // if we have a result, the customer is not a Price Forge allowed one
                if (rs.next()) {
                    return false;
                } else {
                    return true;
                }
            }
        });

        return pfCustomer;
    }

    /**
     * Check the CustomerNotAllowedTicker to verify if the RTFI ticker of the instrument is allowed for the customer
     * 
     * @param order
     *            Order class used to retrieve the customer object
     * @return return true if ticker is in the list of those not allowed for the customer
     */
    public boolean isTheTickerNotAllowedForTheCustomer(Order order) {
        checkPreRequisites();
        final Customer customer = order.getCustomer();
        final String orderId = order.getFixOrderId();
        Instrument instrument = order.getInstrument();
        final String ticker = instrument.getRTFITicker();
        LOGGER.info("Order " + orderId + ". Check if the customer " + customer.getName() + ", fix id " + customer.getFixId() + ", can trade the instrument " + instrument.getIsin() + " with ticker "
                + ticker);
        if (OrderSide.SELL.equals(order.getSide())) {
            LOGGER.info("Order {}. It is a SELL order, no need to check the ticker.", orderId);
            return false;
        }

        Boolean notAllowedTicker = (Boolean) this.jdbcTemplate.query(selectAllowedTickersForCustomer, new PreparedStatementSetter() {
            public void setValues(PreparedStatement stmt) throws SQLException {
                // 1st param : Customer code
                stmt.setString(1, customer.getFixId());
            }
        }, new ResultSetExtractor() {
            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                // if we have a result, the customer is not a Price Forge allowed one
                if (rs.next()) {
                    String notAllowedTickers = rs.getString("TickerList");
                    LOGGER.debug("Not allowed tickers : {}.", notAllowedTickers);
                    if (!notAllowedTickers.isEmpty() && notAllowedTickers.contains(ticker)) {
                        LOGGER.info("Order {}. Ticker in the list of those not allowed for customer {}", orderId, customer.getFixId());
                        return true;
                    } else {
                        LOGGER.info("Order {}. Ticker NOT in the list of those not allowed for customer {}", orderId, customer.getFixId());
                        return false;
                    }
                } else {
                    LOGGER.info("Order {}. No ticker list found for customer {}, consider the ticker as allowed.", orderId, customer.getFixId());
                    return false;
                }
            }

        });

        return notAllowedTicker;
    }
}
