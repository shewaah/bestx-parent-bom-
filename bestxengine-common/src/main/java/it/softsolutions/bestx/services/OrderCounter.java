/*
 * Copyright 1997-2015 SoftSolutions! srl 
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
package it.softsolutions.bestx.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Order;

/**
*
* Purpose: utility class, counts orders
*
* Project Name : bestxgui-cs First created by: ruggero.rizzo Creation date: 20-gen-2015
* 
**/
public class OrderCounter {
	private static final Logger logger = LoggerFactory.getLogger(OrderCounter.class);
	
	/**
	 * Count the total number of received orders for a given date
	 * implementation for MSServer only
	 * @param searchDate day requested
	 * @return number of orders received that day, -1 if an error occurs
	 */
	public static int countActiveOrders(Date searchDate, boolean limitFile){
		Connection conn = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Calendar calendar = Calendar.getInstance();
		ResultSet rs = null;
		ResultSet countResSet = null;
		PreparedStatement stm = null;
		try {
			if (searchDate != null) {
				calendar.setTime(searchDate);
				calendar.add(Calendar.DATE, 1);
			}
			Date endSearchDate = calendar.getTime();
			Context initContext = new InitialContext();
			Context envContext = (Context) initContext.lookup("java:/comp/env");
			DataSource ds = (DataSource) envContext.lookup("jdbc/CS_DB");
			conn = ds.getConnection();
			conn.setReadOnly(true);

//			stm = conn.createStatement();
			final String orderCountSql;

			if (limitFile) {
				// only limit file orders
				if(searchDate == null) {
					orderCountSql = "SELECT count(*) FROM TabHistoryOrdini tho WITH (NOLOCK) "
							+ " WHERE tho.Stato NOT IN ('OrderRevocatedState', 'StateExecuted', 'OrderNotExecutedState') AND tho.TimeInForce = ? AND tho.TipoOrdine = ?";
					stm = conn.prepareStatement(orderCountSql);
					stm.setString(1, it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL.toString());
					stm.setString(2, Order.OrderType.LIMIT.getFixCode());
				}
				else {
					orderCountSql = "SELECT count(*) FROM TabHistoryOrdini tho WITH (NOLOCK) "
							+ " WHERE tho.Stato NOT IN ('OrderRevocatedState', 'StateExecuted', 'OrderNotExecutedState') AND tho.TimeInForce = ? AND tho.TipoOrdine = ?"
							+ " AND tho.DataOraRicezione >= convert(datetime, ?, 121) AND tho.DataOraRicezione < convert(datetime, ?, 121)";
					stm = conn.prepareStatement(orderCountSql);
					stm.setString(1, it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL.toString());
					stm.setString(2, Order.OrderType.LIMIT.getFixCode());
					stm.setString(3, df.format(searchDate).toString());
					stm.setString(4, df.format(endSearchDate).toString());
				}
/*				orderCountSql += " AND ro.TimeInForce ='"
						+ it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL
						+ "' AND ro.OrderType = '" + Order.OrderType.LIMIT
						+ "' ";*/
			} else {
				if(searchDate == null) {
					orderCountSql = "SELECT count(*) FROM TabHistoryOrdini tho WITH (NOLOCK) "
							+ " WHERE tho.Stato NOT IN ('OrderRevocatedState', 'StateExecuted', 'OrderNotExecutedState') AND tho.TimeInForce != ?";
					stm = conn.prepareStatement(orderCountSql);
					stm.setString(1, it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL.toString());
				}
				else {
					orderCountSql = "SELECT count(*) FROM TabHistoryOrdini tho WITH (NOLOCK) "
							+ " WHERE tho.Stato NOT IN ('OrderRevocatedState', 'StateExecuted', 'OrderNotExecutedState') AND tho.TimeInForce != ?"
							+ " AND tho.DataOraRicezione >= convert(datetime, ?, 121) AND tho.DataOraRicezione < convert(datetime, ?, 121)";
	            stm = conn.prepareStatement(orderCountSql);
	            stm.setString(1, it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL.toString());
	            stm.setString(2, df.format(searchDate).toString());
	            stm.setString(3, df.format(endSearchDate).toString());
				}
/*				orderCountSql += "AND ro.TimeInForce !='"
						+ it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL
						+ "' ";*/
			}
/*			if (searchDate != null) {
				orderCountSql += " AND ro.TransactionTime >= convert(datetime, '"
						+ df.format(searchDate)
						+ "',121)"
						+ " AND ro.TransactionTime < convert(datetime, '"
						+ df.format(endSearchDate) + "',121)";
			}*/
			countResSet = stm.executeQuery();
			int numOrders = 0;
			if (countResSet.next()) {
				numOrders = countResSet.getInt(1);
			}
			return numOrders;
		} catch (Exception e) {
			logger.error("Error while fetching limit file orders, cannot build the page elencoOrdini.jsp .", e);
			return -1;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
				}
			}
			if (countResSet != null) {
				try {
					countResSet.close();
				} catch (SQLException e) {
				}
			}
			if (stm != null) {
				try {
					stm.close();
				} catch (SQLException e) {
				}
			}
			if (conn != null){
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
		}
	}
	
	private JdbcTemplate jdbcTemplate;

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }

	/**
	 * Count the total number of received orders for a given date
	 * implementation for MSServer only
	 * @param searchDate day requested
	 * @return number of orders received that day, -1 if an error occurs
	 */
	public int countActiveOrders(Date searchDate){
		checkPreRequisites();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Calendar calendar = Calendar.getInstance();
		try {
			if (searchDate != null) {
				calendar.setTime(searchDate);
				calendar.add(Calendar.DATE, 1);
			}
			Date endSearchDate = calendar.getTime();
			
			String orderCountSql = "SELECT count(*) FROM TabHistoryOrdini tho WITH (NOLOCK) "
					+ " WHERE tho.Stato NOT IN ('OrderRevocatedState', 'StateExecuted', 'OrderNotExecutedState')";

			if (searchDate != null) {
				orderCountSql += " AND tho.DataOraRicezione >= convert(datetime, '"
						+ df.format(searchDate)
						+ "',121)"
						+ " AND tho.DataOraRicezione < convert(datetime, '"
						+ df.format(endSearchDate) + "',121)";
			}
			Integer numOrders = jdbcTemplate.queryForObject(orderCountSql, Integer.class);

			return numOrders == null? 0: numOrders;
		} catch (Exception e) {
			logger.error("Error while fetching limit file orders, cannot build the page elencoOrdini.jsp .", e);
			return -1;
		} 
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
