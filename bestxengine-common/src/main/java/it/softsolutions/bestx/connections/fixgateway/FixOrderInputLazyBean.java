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
package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.TradingCapacity;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.jsscommon.Money;
import it.softsolutions.xt2.protocol.XT2Msg;
import quickfix.fix50sp2.component.Parties;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-common First created by: Creation date: 23-ott-2012
 * 
 **/
public class FixOrderInputLazyBean extends Order {
	private static final Logger LOGGER = LoggerFactory.getLogger(FixOrderInputLazyBean.class);
	private CustomerFinder customerFinder;
	private InstrumentFinder instrumentFinder;
	private final Date receivedTime;
	private Customer customer;
	private final XT2Msg msg;
	private InstrumentDao sqlInstrumentDao;
	private Instrument relatedInstrument = null;
	
	private DateTimeFormatter dateFormatter;
	private DateTimeFormatter dateTimeFormatter;


	private String getStringField(String id) {
		try {
			return msg.getString(id);
		} catch (Exception e) {
			LOGGER.debug("Requested field '{}' not found", id);
			return null;
		}
	}

	private Integer getIntField(String id) {
		try {
			return msg.getInt(id);
		} catch (Exception e) {
			LOGGER.debug("Requested field '{}' not found", id);
			return null;
		}
	}

	private Double getDoubleField(String id) {
		try {
			return msg.getDouble(id);
		} catch (Exception e) {
			LOGGER.debug("Requested field '{}' not found", id);
			return null;
		}
	}

	/**
	 * Gets the session id.
	 * 
	 * @return the session id
	 */
	public String getSessionId() {
		return msg.getString(FixMessageFields.FIX_SessionID);
	}

	/**
	 * Instantiates a new fix order input lazy bean.
	 * 
	 * @param msg
	 *            the msg
	 * @param dateFormat
	 *            the date format
	 * @param dateTimeFormat
	 *            the date time format
	 */
	public FixOrderInputLazyBean(XT2Msg msg, String dateFormat, String dateTimeFormat) {
		if (msg == null || dateFormat == null || dateTimeFormat == null) {
			throw new IllegalArgumentException("params can't be null");
		}

		this.msg = msg;
		this.receivedTime = DateService.newLocalDate();
		
		dateFormatter = DateTimeFormat.forPattern(dateFormat).withZone(DateTimeZone.UTC);
		dateTimeFormatter = DateTimeFormat.forPattern(dateTimeFormat).withZone(DateTimeZone.UTC);
	}

	@Override
	public Order.OrderType getType() {
		String stringEncodedType = getStringField(FixMessageFields.FIX_OrdType);
		if (Order.OrderType.LIMIT.getFixCode().equals(stringEncodedType)) {
			return Order.OrderType.LIMIT;
		} else if (Order.OrderType.MARKET.getFixCode().equals(stringEncodedType)) {
			return Order.OrderType.MARKET;
		} else if (Order.OrderType.ATQUOTE.getFixCode().equals(stringEncodedType)) {
			return Order.OrderType.ATQUOTE;
		} else {
			LOGGER.error("Invalid value as order type: {}", stringEncodedType);
			return Order.OrderType.NOT_RECOGNIZED;
		}
	}

	@Override
	public Money getLimit() {
		Double doubleEncodedPrice = getDoubleField(FixMessageFields.FIX_Price);
		if (Order.OrderType.LIMIT.equals(this.getType()) && doubleEncodedPrice != null) {
			return new Money(getCurrency(), String.valueOf(doubleEncodedPrice));
		} else if (Order.OrderType.MARKET.equals(this.getType())) {
			LOGGER.debug("Requested limit price on MARKET order type");
			return null;
		} else if (Order.OrderType.ATQUOTE.equals(this.getType())) {
			LOGGER.debug("Requested limit price on ATQUOTE order type");
			return null;
		} else {
			LOGGER.debug("No price found");
			return null;
		}
	}

	/**
	 * Gets the quote id.
	 * 
	 * @return the quote id
	 */
	public String getQuoteId() {
		return getStringField(FixMessageFields.FIX_QuoteID);
	}

	@Override
	public String getCurrency() {
		try {
			return getStringField(FixMessageFields.FIX_Currency);
		} catch (IllegalArgumentException iae) {
			return null;
		}
	}

	@Override
	public String getExecutionDestination() {
		return getStringField(FixMessageFields.FIX_ExDestination);
	}

	/**
	 * Gets the customer type.
	 * 
	 * @return the customer type
	 */
	public String getCustomerType() {
		return getStringField(FixMessageFields.FIX_CustomerType);
	}

	@Override
	public TimeInForce getTimeInForce() {
		Character timeInForceCode = null;

		// [DR20120613] BugFixed on NullPointerException: timeInForceCode was a int and getIntField can return a null value
		// [DR20131104] BugFixed on String fields: timeInForce is a XT2(STRING) so use getStringField
		String timeInForceStr = getStringField(FixMessageFields.FIX_TimeInForce);

		if (timeInForceStr != null) {
			try {
				timeInForceCode = timeInForceStr.charAt(0);
			} catch (IndexOutOfBoundsException e) {
				LOGGER.error("Invalid format for TimeInForce field [{}]: {}", timeInForceStr, e.getMessage());
			}
		}

		return (timeInForceCode != null) ? TimeInForce.getByFixCode(timeInForceCode) : null;
	}

	/**
	 * Gets the text.
	 * 
	 * @return the text
	 */
	@Override
	public String getText() {
		return getStringField(FixMessageFields.FIX_Text);
	}

	@Override
	public String getCustomerOrderId() {
		return getStringField(FixMessageFields.FIX_CustomerOrderId);
	}

	/**
	 * Gets the client order id.
	 * 
	 * @return the client order id
	 */
	public String getClientOrderId() {
		return getStringField(FixMessageFields.FIX_ClOrdID);
	}

	@Override
	public String getFixOrderId() {
		return getStringField(FixMessageFields.FIX_ClOrdID);
	}

	/**
	 * Sets the customer finder.
	 * 
	 * @param customerFinder
	 *            the new customer finder
	 */
	public void setCustomerFinder(CustomerFinder customerFinder) {
		this.customerFinder = customerFinder;
	}

	/**
	 * Sets the instrument finder.
	 * 
	 * @param instrumentFinder
	 *            the new instrument finder
	 */
	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}

	@Override
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	/**
	 * Gets the client id.
	 * 
	 * @return the client id
	 */
	public String getClientId() {
		return getStringField(FixMessageFields.FIX_Account);
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public Long getId() {
		return null;
	}

	@Override
	public Customer getCustomer() {
		if (customer != null)
			return customer; // default customer

		// [DR20120613] BugFixed: prevent NullPointerException on customerFinder
		if (customerFinder == null) {
			LOGGER.warn("customerFinder is null, unable to retrieve the customer");
			return null;
		}

		try {
			return customerFinder.getCustomerByFixId(getClientId());
		} catch (BestXException e) {
			LOGGER.error("Error retrieving customer {}: {}", getClientId(), e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Gets the quote request id.
	 * 
	 * @return the quote request id
	 */
	public String getQuoteRequestId() {
		return getStringField(FixMessageFields.FIX_QuoteRequestID);
	}

	/**
	 * Gets the id source.
	 * 
	 * @return the id source
	 */
	public String getIdSource() {
		// [DR20120613] BugFixed on NullPointerException, trim removed from the getStringField
		String ret = getStringField(FixMessageFields.FIX_IDSource);

		if (ret == null || ret.trim().isEmpty()) {
			// [DR20120613] Adopted the standard FIX constant instead of an anonymous "4"
			LOGGER.trace("Missing IDSource specified, set ISIN_NUMBER as default");
			return quickfix.field.IDSource.ISIN_NUMBER;
		} else {
			return ret.trim();
		}
	}

	/**
	 * Gets the security id.
	 * 
	 * @return the security id
	 */
	public String getSecurityId() {
		String securityId = getStringField(FixMessageFields.FIX_SecurityID);
		//[RR20150203] CRSBXTEM-157 trim leading and trailing spaces
		return securityId == null? securityId : securityId.trim();
	}

	/**
	 * Gets the symbol.
	 * 
	 * @return the symbol
	 */
	public String getSymbol() {
		String symbol = getStringField(FixMessageFields.FIX_Symbol); 
		//[RR20150203] CRSBXTEM-157 trim leading and trailing spaces		
		return symbol == null? symbol : symbol.trim();
	}

	/**
	 * Gets the isin.
	 * 
	 * @return the isin
	 */
	public String getIsin() {
		if (quickfix.field.IDSource.ISIN_NUMBER.equals(getIdSource())) {
			return getSecurityId();
		} else {
			LOGGER.error("ISIN code not available");
			return null;
		}
	}

	@Override
	public Instrument getInstrument() {

		// [DR20130711] BugFixed: prevent NullPointerException on instrumentFinder
		if (instrumentFinder == null) {
			LOGGER.warn("instrumentFinder is null, unable to retrieve the instrument");
			return null;
		}
		// [RR20130915] BXMNT-300: to avoid unuseful call to database operations we store the found instrument
		// and return it in future calls
		if (relatedInstrument == null) {
			String isin = getIsin();
			Instrument instrument = instrumentFinder.getInstrumentByIsin(isin);

			if (instrument != null) {
				relatedInstrument = instrument;
				return instrument; // instrument.isInInventory == true
			} else {
				// [RR20130912] BXMNT-300:
				// We could arrive here because the country is not known and hibernate cannot load the
				// Instrument object because it is related to the Country object.
				// Thus we retry to load the instrument with a SQL Dao, if we found something we keep
				// the informations about the country to have the order rejected by the appropriate
				// formal filter
				instrument = sqlInstrumentDao.getInstrumentByIsin(isin);
				if (instrument == null) {
					LOGGER.warn("Instrument with isin {} not found in the database", isin);
				} else {
					relatedInstrument = instrument;
				}
				return instrument;
			}
		} else {
			return relatedInstrument;
		}
	}

	@Override
	public Integer getSettlementType() {
		return getIntField(FixMessageFields.FIX_SettlmntTyp);
	}

	@Override
	public Date getTransactTime() {
		String stringFormattedDate = getStringField(FixMessageFields.FIX_TransactTime);
		if (stringFormattedDate != null) {
			try {
				DateTime dateTime = dateTimeFormatter.parseDateTime(stringFormattedDate);
				return dateTime.toDate();
			} catch (Exception e) {
				LOGGER.error("Parsing error while converting field '{}', value: {}", FixMessageFields.FIX_TransactTime, stringFormattedDate);
			}
		}
		return DateService.newLocalDate();
	}

	@Override
	public Date getFutSettDate() {
		String stringFormattedDate = getStringField(FixMessageFields.FIX_FutSettDate);
		if (stringFormattedDate != null) {
			try {
				DateTime dateTime = dateFormatter.parseDateTime(stringFormattedDate);
				return dateTime.toDate();
			} catch (Exception e) {
				LOGGER.error("Parsing error while converting field '{}', value: {}", FixMessageFields.FIX_FutSettDate, stringFormattedDate);
			}
		}
		return null;
	}

	@Override
	public String getSecExchange() {
		return getStringField(FixMessageFields.FIX_SecurityExchange);
	}
	
	@Override
	public OrderSide getSide() {
		String stringEncodedSide = getStringField(FixMessageFields.FIX_Side);
		if (OrderSide.isBuy(stringEncodedSide)) {
			return OrderSide.BUY;
		} else if (OrderSide.isSell(stringEncodedSide)) {
			return OrderSide.SELL;
		} else {
			LOGGER.error("Invalid value [{}] as order side, set side = null", stringEncodedSide);
			return null;
		}
	}

	@Override
	public BigDecimal getQty() {
		Double doubleEncodedQty = getDoubleField(FixMessageFields.FIX_OrderQty);
		if (doubleEncodedQty != null) {
			return new BigDecimal(String.valueOf(doubleEncodedQty));
		} else {
			LOGGER.debug("Quantity not found");
			return null;
		}
	}

	@Override
	public Integer getPriceType() {
		Integer priceType = getIntField(FixMessageFields.FIX_PriceType);
		return priceType != null ? priceType : 0;
	}

	
   @Override
    public String getTicketOwner() {
        return getStringField(FixMessageFields.FIX_TicketOwner);
    }
	   
	/**
	 * Gets the received time.
	 * 
	 * @return the received time
	 */
	public Date getReceivedTime() {
		return receivedTime;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the new id
	 */
	public void setId(Long id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getOrderSource() {
		return getStringField(FixMessageFields.FIX_CustomerOrderSource);
	}

	/**
	 * Gets the msg.
	 * 
	 * @return the msg
	 */
	public XT2Msg getMsg() {
		return msg;
	}

	/**
	 * @return the sqlInstrumentDao
	 */
	public InstrumentDao getSqlInstrumentDao() {
		return sqlInstrumentDao;
	}

	/**
	 * @param sqlInstrumentDao
	 *            the sqlInstrumentDao to set
	 */
	public void setSqlInstrumentDao(InstrumentDao sqlInstrumentDao) {
		this.sqlInstrumentDao = sqlInstrumentDao;
	}

	@Override
	public TradingCapacity getTradingCapacity() {
		String tcv = getStringField(FixMessageFields.FIX_TradingCapacity);
		if(tcv != null && tcv.length() > 0)
		for(TradingCapacity tc: TradingCapacity.values())
			if(tc.getValue().equalsIgnoreCase(tcv))
				return tc;
		return TradingCapacity.NONE;
	}

	@Override
	public Parties getDecisionMaker() {
		return super.getDecisionMaker();  //FIXME update for most clients
	}

	@Override
	public Parties getExecutionDecisor() {
		return super.getExecutionDecisor(); //FIXME update for most clients
	}

	@Override
	public OrderSide getShortSellIndicator() {
		String stringEncodedSide = getStringField(FixMessageFields.FIX_Side);
		switch(stringEncodedSide) {
		case "" + quickfix.field.Side.SELL_SHORT://OrderSide.SELL_SHORT.getFixCode():
			return OrderSide.SELL_SHORT;
		case "" + quickfix.field.Side.SELL_SHORT_EXEMPT://OrderSide.SELL_SHORT_EXEMPT.getFixCode():
			return OrderSide.SELL_SHORT_EXEMPT;
		case "" + quickfix.field.Side.SELL_UNDISCLOSED:
			return OrderSide.SELL_UNDISCLOSED;
		default: 
			return OrderSide.SELL_UNDISCLOSED;
		}
	}

	@Override
	public Boolean isMiFIDRestricted() {
		return ("Y".equals(getStringField(FixMessageFields.FIX_MiFIDTVRestricted)));
	}

}
