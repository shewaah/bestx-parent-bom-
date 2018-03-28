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

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.dao.TitoliIncrociabiliDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

/**  
 *
 * Purpose: DAO to load data about Matching between multiples ISIN  
 *
 * Project Name : bestxengine-product 
 * First created by: davide.rossoni 
 * Creation date: 24/ago/2012 
 * 
 **/
public class SqlTitoliIncrociabiliMultipliDao implements TitoliIncrociabiliDao {
    
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlTitoliIncrociabiliMultipliDao.class);

    public boolean isAMatch(Order order) throws SQLException {
        checkPreRequisites();
        boolean result = false;
        String sql_select = "SELECT * FROM TitoliIncrociabiliMultipli WHERE " +
        		"ISIN = ? AND " + 
        		"CodiceCliente = ? AND " + 
        		"Lato = ? AND " +  
        		"(NumOrdine = '' or NumOrdine is null)";

        Object[] args = new Object[] { 
        		order.getInstrument().getIsin(), 
        		order.getCustomer().getFixId(), 
        		order.getSide().getFixCode() 
        };

        if (order.getQty()!=null) {
        	sql_select = "SELECT * FROM TitoliIncrociabiliMultipli WHERE " +
        			"ISIN = ? AND " + 
        			"CodiceCliente = ? AND " + 
        			"Lato = ? AND " + 
        			"Quantita = ? AND " + 
        			"(NumOrdine = '' or NumOrdine is null)";

        	args = new Object[] { 
        			order.getInstrument().getIsin(), 
        			order.getCustomer().getFixId(), 
        			order.getSide().getFixCode(), 
        			order.getQty().toPlainString().replace(",", ".") 
        	};
        }

        // MATCHING REINVIATI AL SISTEMA DA WARNING
        // " and ( (NumOrdine = '' or NumOrdine is null) or NumOrdine='" + order.getFixOrderId() + "')";
        LOGGER.debug("SQL = [{}], params = {}", sql_select, Arrays.asList(args));

        final List<String> list = new LinkedList<String>();
        jdbcTemplate.query(sql_select, args, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                list.add(rset.getString("IdIncrocio"));
            }
        });
        result = !(list.isEmpty());
        return result;
    }

	/*
	 * (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.TitoliIncrociabiliDao#setMatchingOperation(it.softsolutions.bestx.Operation)
	 */
	public void setMatchingOperation(Operation operation) throws SQLException {
		checkPreRequisites();
		
		//AGGIUNTA ID --
		int singleMatchingRowId = getAvailableSingleMatchRowId(operation);
		
        if (singleMatchingRowId == -1) {
            LOGGER.error("ERROR while setting matching operation: no matching rows found, cannot obtain the single row id for order {}", operation.getOrder());
        } else {
            LOGGER.debug("Single row id found : {}", singleMatchingRowId);
        }
        
	    //-- AGGIUNTA ID
		String sql_select = "update TitoliIncrociabiliMultipli " + 
		"set NumOrdine = '" + operation.getIdentifier(OperationIdType.ORDER_ID) +
		"' where ISIN = '" + operation.getOrder().getInstrument().getIsin() + 
		"' and CodiceCliente = '" + operation.getOrder().getCustomer().getFixId() + 
		"' and Lato = '" + operation.getOrder().getSide().getFixCode() +
		"' and Quantita = " + operation.getOrder().getQty().toPlainString().replace(",", ".") +
		" and (NumOrdine = '' or NumOrdine is null)" +
	    //AGGIUNTA ID --
		" and IdSingoloMatch = " + singleMatchingRowId;
	    //-- AGGIUNTA ID
		LOGGER.debug(sql_select);

		jdbcTemplate.execute(sql_select);
	}
	
	/*
	 * (non-Javadoc)
	 * @see it.softsolutions.bestx.dao.TitoliIncrociabiliDao#resetMatchingOperation(it.softsolutions.bestx.Operation)
	 */
	public void resetMatchingOperation(Operation operation) throws SQLException {
		checkPreRequisites();
		
	    //AGGIUNTA ID --
		int singleMatchingRowId = getSingleMatchRowId(operation);
      if (singleMatchingRowId == -1) {
         LOGGER.error("ERROR while resetting operation: no matching row found, cannot obtain the single row id for order {}",
                      operation.getOrder());
      }
      else {
         LOGGER.debug("Single row id found : {}", singleMatchingRowId);
      }
      //-- AGGIUNTA ID
      
		String sql_select = "update TitoliIncrociabiliMultipli " + 
		"set NumOrdine = '" +
		"' where ISIN = '" + operation.getOrder().getInstrument().getIsin() + 
		"' and CodiceCliente = '" + operation.getOrder().getCustomer().getFixId() + 
		"' and Lato = '" + operation.getOrder().getSide().getFixCode() +
		"' and Quantita = " + operation.getOrder().getQty().toPlainString().replace(",", ".") +
	   //AGGIUNTA ID --
		" and IdSingoloMatch = " + singleMatchingRowId;
	   //-- AGGIUNTA ID
		
		LOGGER.debug(sql_select);

		jdbcTemplate.execute(sql_select);
	}
	
	
	private JdbcTemplate jdbcTemplate;

	/**
	 * Set JDBC Template
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

	public boolean isAMatch(Instrument instrument, Customer customer) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean allOrdersArrives(String matchId) throws SQLException {
		checkPreRequisites();
		boolean result = false;
        String sql_select = "select * from TitoliIncrociabiliMultipli where IdIncrocio = ? and (NumOrdine = '' or NumOrdine is null)";
		LOGGER.debug("SQL = [{}], params = {}", sql_select, matchId);

		final List<String> list = new LinkedList<String>();
		jdbcTemplate.query(sql_select, new Object[] { matchId }, new RowCallbackHandler() {
			public void processRow(ResultSet rset) throws SQLException {
				list.add(rset.getString("IdIncrocio"));
			}}
		);
		result = list.isEmpty();
		return result;
	}

	public void deleteMatching(String matchId) throws SQLException {
		checkPreRequisites();
		String sql_select = "delete from TitoliIncrociabiliMultipli where IdIncrocio = '" + matchId + "'";
		LOGGER.debug(sql_select);

		jdbcTemplate.execute(sql_select);
	}

	public String getMatchId(Operation operation) throws SQLException {
		checkPreRequisites();
		String sql_select = "select IdIncrocio from TitoliIncrociabiliMultipli where NumOrdine = ?";

		String numOrdine = operation.getIdentifier(OperationIdType.ORDER_ID);
		LOGGER.debug("SQL = [{}], params = {}", sql_select, numOrdine);

		final List<String> list = new LinkedList<String>();
		jdbcTemplate.query(sql_select, new Object[]{ numOrdine }, new RowCallbackHandler() {
			public void processRow(ResultSet rset) throws SQLException {
				list.add(rset.getString("IdIncrocio"));
			}}
		);
		return (list.isEmpty() ? null : list.get(0));
	}

	public List<String> getMatchOrdersList(String matchId) throws SQLException {
		checkPreRequisites();
		String sql_select = "select NumOrdine from TitoliIncrociabiliMultipli where IdIncrocio = ?";
        LOGGER.debug("SQL = [{}], params = {}", sql_select, matchId);

		final List<String> list = new LinkedList<String>();
		jdbcTemplate.query(sql_select, new Object[]{ matchId }, new RowCallbackHandler() {
			public void processRow(ResultSet rset) throws SQLException {
				list.add(rset.getString("NumOrdine"));
			}}
		);
		return list;
	}

    private int getAvailableSingleMatchRowId(Operation operation) throws SQLException {
        checkPreRequisites();

        Order order = operation.getOrder();
	      
        // Extract the row ids for every row of that Matching that hasn't been
        // paired to an order (with the new feature of matchings with same quantities and
        // same customer on the same ISIN and side, we could obtain more than 1 row)
        String sql_select = "SELECT IdSingoloMatch FROM TitoliIncrociabiliMultipli WHERE " +
                "ISIN = ? AND " + 
                "CodiceCliente = ? AND " + 
                "Lato = ? AND " + 
                "Quantita = ? AND " + 
                "(NumOrdine = '' or NumOrdine is null)";

        Object[] args = new Object[] { 
                order.getInstrument().getIsin(), 
                order.getCustomer().getFixId(), 
                order.getSide().getFixCode(), 
                order.getQty().toPlainString().replace(",", ".") 
        };

        LOGGER.debug("SQL = [{}], params = {}", sql_select, Arrays.asList(args));

        final List<Integer> list = new LinkedList<Integer>();
        jdbcTemplate.query(sql_select, args, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                list.add(rset.getInt("IdSingoloMatch"));
            }
        });

        /*
         * Return the 1st id available, i.e. the id of the 1st row of that matching with same qty, isin and customer that hasn't been
         * assigned to an order, or the right row id in case of a lonely isin/qty/customer row for that matching
         */
        if (list.size() > 0) {
            return list.get(0).intValue();
        } else {
            return -1;
        }
    }
	  
    private int getSingleMatchRowId(Operation operation) throws SQLException {
        checkPreRequisites();

        Order order = operation.getOrder();

        //extract the single row id for that particular order
        String sql_select = "SELECT IdSingoloMatch FROM TitoliIncrociabiliMultipli WHERE " +
                "ISIN = ? AND " + 
                "CodiceCliente = ? AND " + 
                "Lato = ? AND " + 
                "Quantita = ? AND " + 
                "NumOrdine = ?";

        Object[] args = new Object[] { 
                order.getInstrument().getIsin(), 
                order.getCustomer().getFixId(), 
                order.getSide().getFixCode(), 
                order.getQty().toPlainString().replace(",", "."),
                order.getFixOrderId()
        };

        LOGGER.debug("SQL = [{}], params = {}", sql_select, Arrays.asList(args));

        final List<Integer> list = new LinkedList<Integer>();
        jdbcTemplate.query(sql_select, args, new RowCallbackHandler() {
            public void processRow(ResultSet rset) throws SQLException {
                list.add(rset.getInt("IdSingoloMatch"));
            }
        });

        if (list.size() == 1) {
            return list.get(0).intValue();
        } else {
            return -1;
        }
    }
}
