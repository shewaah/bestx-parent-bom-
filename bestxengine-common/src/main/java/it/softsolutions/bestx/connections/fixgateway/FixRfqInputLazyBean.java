package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class FixRfqInputLazyBean extends Rfq {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixRfqInputLazyBean.class);
    private String dateFormat;
    private String dateTimeFormat;
    private SimpleDateFormat dateFormatter;
    private SimpleDateFormat dateTimeFormatter;
    private CustomerFinder customerFinder;
    private InstrumentFinder instrumentFinder;
    private Date receivedTime;
    private Customer customer;
    private XT2Msg msg;
    
    private String getStringField(String id) {
        try {
            return msg.getString(id);
        } catch (Exception e) {
            LOGGER.debug("Requested field '" + id + "' not found");
            return null;
        }
    }
    private Integer getIntField(String id) {
        try {
            return msg.getInt(id);
        } catch (Exception e) {
            LOGGER.debug("Requested field '" + id + "' not found");
            return null;
        }
    }
    private Double getDoubleField(String id) {
        try {
            return msg.getDouble(id);
        } catch (Exception e) {
            LOGGER.debug("Requested field '" + id + "' not found");
            return null;
        }
    }
    public String getSessionId() {
        return msg.getString(FixMessageFields.FIX_SessionID);
    }
    
    public FixRfqInputLazyBean(XT2Msg msg, String dateFormat, String dateTimeFormat) {
    	this.dateFormat = dateFormat;
    	this.dateTimeFormat = dateTimeFormat;
    	dateFormatter = new SimpleDateFormat(dateFormat);
    	dateTimeFormatter = new SimpleDateFormat(dateTimeFormat);
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.msg = msg;
        this.receivedTime = DateService.newLocalDate();
    }
    public void setCustomerFinder(CustomerFinder customerFinder) {
        this.customerFinder = customerFinder;
    }
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    public String getClientId() {
        return getStringField(FixMessageFields.FIX_ClientID);
    }
    public Long getId() {
        return null;
    }
    public Customer getCustomer() {
        if (customer != null) return customer; // default customer
        try {
            return customerFinder.getCustomerByFixId(getClientId());
        }
        catch (BestXException e) {
            LOGGER.error("Error while retrieving customer: {} : {}", getClientId(), e.getMessage(), e);
            return null;
        }
    }
    public String getQuoteRequestId() {
        return getStringField(FixMessageFields.FIX_QuoteRequestID);
    }
    public String getIdSource() {
        String ret = getStringField(FixMessageFields.FIX_IDSource).trim();
        if (ret == null || "".equals(ret)) {
            return "4";
        } else {
            return ret;
        }
    }
    public String getSecurityId() {
        return getStringField(FixMessageFields.FIX_SecurityID);
    }
    public String getSymbol() {
        return getStringField(FixMessageFields.FIX_Symbol);
    }
    public String getIsin() {
        if ("4".equals(getIdSource())) {
            return getSecurityId();
        } else {
            LOGGER.error("ISIN code not available");
            return null;
        }
    }
    public Instrument getInstrument() {
        Instrument instrument = instrumentFinder.getInstrumentByIsin(getIsin());
        return instrument;
    }
    public String getSettlementType() {
        return getStringField(FixMessageFields.FIX_SettlmntTyp);
    }
    public Date getTransactTime() {
        String stringFormattedDate = getStringField(FixMessageFields.FIX_TransactTime);
        if (stringFormattedDate != null) {
            try {
                return dateTimeFormatter.parse(getStringField(FixMessageFields.FIX_TransactTime));
            } catch (ParseException e) {
                LOGGER.error("Parsing error while converting field '" + FixMessageFields.FIX_TransactTime + "', value: " + stringFormattedDate + " with format: " + dateTimeFormat);
            }
        }
        return null;
    }
    public Date getFutSettDate() {
        String stringFormattedDate = getStringField(FixMessageFields.FIX_FutSettDate);
        if (stringFormattedDate != null) {
            try {
                return dateFormatter.parse(getStringField(FixMessageFields.FIX_FutSettDate));
            } catch (ParseException e) {
                LOGGER.error("Parsing error while converting field '" + FixMessageFields.FIX_FutSettDate + "', value: " + stringFormattedDate + " with format: " + dateFormat);
            }
        }
        return null;
    }
    
    public String getSecExchange() {
        return getStringField(FixMessageFields.FIX_SecurityExchange);
    }

	public Rfq.OrderSide getSide() {
		String stringEncodedSide = getStringField(FixMessageFields.FIX_Side);
		if (Rfq.OrderSide.BUY.getFixCode().equals(stringEncodedSide)) {
			return Rfq.OrderSide.BUY;
		} else if (Rfq.OrderSide.SELL.getFixCode().equals(stringEncodedSide)) {
			return Rfq.OrderSide.SELL;
		} else {
			LOGGER.error("Invalid value as order side: " + stringEncodedSide);
			return null;
		}
	}
	
    public BigDecimal getQty() {
        Double doubleEncodedQty = getDoubleField(FixMessageFields.FIX_OrderQty);
        if (doubleEncodedQty != null) {
            return new BigDecimal(String.valueOf(doubleEncodedQty));
        } else {
            LOGGER.debug("Quantity not found");
            return null;
        }
    }
    public Date getReceivedTime() {
        return receivedTime;
    }
    public void setId(Long id) {
        throw new UnsupportedOperationException();
    }
}
