/*
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.automatictest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.gson.Gson;

import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.fixgateway.FixGatewayConnector;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * 
 * This class allows to test the system from a JMX interface by allowing to
 * create a new order and then retrieving its status.
 * 
 * @author Robert Gonzalez & Alberto Acquafresca
 *
 */
public class AutomaticTest implements AutomaticTestMBean{

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticTest.class);

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    
	private FixGatewayConnector connector;
	
	private OperationRegistry operationRegistry;
	
	private JdbcTemplate jdbcTemplate;
	
	private Gson gson = new Gson();

    public FixGatewayConnector getConnector() {
		return connector;
	}

	public void setConnector(FixGatewayConnector connector) {
		this.connector = connector;
	}

	public OperationRegistry getOperationRegistry() {
		return operationRegistry;
	}

	public void setOperationRegistry(OperationRegistry operationRegistry) {
		this.operationRegistry = operationRegistry;
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
    public String getSimpleOrderOperationById(String id) {
		try {
		LOGGER.info("Getting information of order: " + id);
    	String ret = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, id).toString();
    	LOGGER.info("Information from order " + id + ": " + ret);
    	return ret;
		} catch (Exception e) {
			LOGGER.error("Error getting information from order " + id + ": " + e.getMessage());
			e.printStackTrace();
			return "ERROR: " + e.getMessage();
		}
    }

	// This is just a convenience method in order to avoid to set up the date
	// in the call when we want to use the current date.
	@Override
	public String createNewOrder(String isin, int quantity, String currency) {
		LocalDate today = LocalDate.now();
		
		LocalDate todayPlusTwo = LocalDate.from(today);
		
		// This code adds two days skipping Saturday and Sunday.
	    int addedDays = 0;
	    while (addedDays < 2) {
	    	todayPlusTwo = todayPlusTwo.plusDays(1);
	    	// Add Italy holidays?
	        if (!(todayPlusTwo.getDayOfWeek() == DayOfWeek.SATURDAY ||
	        		todayPlusTwo.getDayOfWeek() == DayOfWeek.SUNDAY)) {
	            ++addedDays;
	        }
	    }
	    
		return this.createNewOrder(isin, formatter.format(today), formatter.format(todayPlusTwo), quantity, currency);
	}
	
	// This code creates a new order by emulating the reception of a XT2 message from the FIX GW.
	// Then it inserts the order in the "customOrders" queue of the FixGatewayConnector bean
	// so that it can be processed by the system.
	@Override
	public String createNewOrder(String isin, String date, String settlementDate, int quantity, String currency) {
		LOGGER.info("Creating new order for ISIN: " + isin);
		XT2Msg msg = new XT2Msg();

		// UNIX Timestamp is used to create order ID
		long timestamp = System.currentTimeMillis();
		
		msg.setSourceMarketName("Oms1FixGateway");
		msg.setSubject("/FIX/ORDER/" + timestamp + "_" + isin);
		if (currency == null || currency.length() != 3) throw new IllegalArgumentException("Invalid currency");
		msg.setValue("Currency", currency);
		msg.setValue("HandlInst", "1");
		msg.setValue("UserSessionName", "QSDBX");
		msg.setValue("TimeInForce", "0");
		msg.setValue("Side", "1");
		if (quantity <= 0) throw new IllegalArgumentException("Invalid quantity. Must be > 0");
		msg.setValue("OrderQty", (double) quantity);
		msg.setValue("ClOrdID", Long.toString(timestamp));
		msg.setValue("OrdType", "1");
		msg.setValue("SettlmntTyp", "6");
		msg.setValue("Symbol", isin);
		msg.setValue("IDSource", "4");
		msg.setValue("Account", "1994");
		msg.setValue("$IBMessTy___", 7);
		msg.setValue("SecurityID", isin);
		msg.setValue("FutSettDate", settlementDate);
		msg.setValue("TransactTime", date + "-08:00:00.000");
		msg.setValue("SessionId", "FIX.4.2:SOFT1->OMS1");
		msg.setName("ORDER");
		connector.onNotification(msg);
		
		LOGGER.info("New order created and sent: " + timestamp);
		return Long.toString(timestamp);
	}

	@Override
	public String getOrderStatus(String id) {
		String ret = this.jdbcTemplate.queryForObject("SELECT cso.DescrizioneStato " + 
				"FROM TabHistoryOrdini tho " + 
				"JOIN CodiciStatiOrdine cso ON (tho.Stato = cso.CodiceStato) " + 
				"WHERE NumOrdine = ?", String.class, Long.parseLong(id.trim()));
		return ret;
	}

	@Override
	public String getOrderHistory(String id) {

		String sql = "SELECT ths.Attempt, ths.SaveTime, ths.Stato, ths.DescrizioneEvento" + 
				" FROM TabHistoryStati ths" + 
				" WHERE ths.NumOrdine = ?" + 
				" ORDER BY ths.SaveTime ASC";
		
		List<Map<String,Object>> res = this.jdbcTemplate.queryForList(sql, Long.parseLong(id.trim()));
		
		return this.gson.toJson(res);
	}

	@Override
	public String getPriceBook(String id) {
		String sql = "SELECT" + 
				" ask.Attempt, mt.MarketName, ask.BankCode," + 
				" ask.Price \"AskPrice\", ask.Qty \"AskQty\"," + 
				" bid.Price \"BidPrice\", bid.Qty \"BidQty\"," + 
				" ask.FlagScartato \"AskFlagScartato\", bid.FlagScartato \"BidFlagScartato\"" + 
				" FROM PriceTable ask" + 
				" JOIN PriceTable bid ON (" + 
				" ask.NumOrdine = bid.NumOrdine AND" + 
				" ask.Attempt = bid.Attempt AND" + 
				" ask.MarketId = bid.MarketId AND" + 
				" ask.BankCode = bid.BankCode AND" + 
				" ask.Side = 1 AND bid.Side = 0" + 
				" )" + 
				" JOIN MarketTable mt on ask.MarketId = mt.MarketId" + 
				" WHERE ask.NumOrdine = ?" + 
				" ORDER BY ask.Attempt, ask.MarketId, ask.BankCode";
		List<Map<String,Object>> res = this.jdbcTemplate.queryForList(sql, Long.parseLong(id.trim()));
		
		return this.gson.toJson(res);
	
	}

	@Override
	public String getMarketStatus(String id) {
		String sql = "SELECT tsm.Attempt, tsm.MarketCode, tsm.Disabled, tsm.DownCause" + 
				" FROM TentativiStatoMercato AS tsm" + 
				" WHERE tsm.NumOrdine = ?";
		List<Map<String,Object>> res = this.jdbcTemplate.queryForList(sql, Long.parseLong(id.trim()));
		
		return this.gson.toJson(res);
	}
	
}