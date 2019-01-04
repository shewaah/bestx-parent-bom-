package it.softsolutions.bestx.connections.pricediscovery;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.dao.InstrumentDao;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;
import it.softsolutions.jsscommon.Money;

public class CSPriceDiscoveryOrderLazyBean extends Order {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CSPriceDiscoveryOrderLazyBean.class);
	
	private InstrumentFinder instrumentFinder;
	private InstrumentDao sqlInstrumentDao;
	
	private Instrument relatedInstrument = null;

	
	
	public CSPriceDiscoveryOrderLazyBean(String instrumentCode, String side, Double quantity, String fIXorderId, String curr, Double limit_price, String traceID) {
		this.setInstrumentCode(instrumentCode);
		this.setSide(side.contains("B") ? Rfq.OrderSide.BUY : Rfq.OrderSide.SELL);
		if (quantity!=null) {
			this.setQty(new BigDecimal(quantity.toString()));
		}
		this.setPriceDiscoveryType(PriceDiscoveryType.ONLY_PRICEDISCOVERY);
		this.setCurrency(curr);
		this.setCustomerOrderId(traceID);
		this.setFixOrderId(fIXorderId);
		try {
			this.setLimit(new Money(Currency.getInstance(curr),new BigDecimal(limit_price.toString())));  // AMC 20160729 ho un dubbio sull'opportunità di usare Currency
		} catch (Exception e) {
			this.setLimit(null);
		}
		this.setTransactTime(DateService.newUTCDate());
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
			Instrument instrument = instrumentFinder.getInstrumentByIsin(getInstrumentCode());

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
				instrument = sqlInstrumentDao.getInstrumentByIsin(getInstrumentCode());
				if (instrument == null) {
					LOGGER.warn("Instrument with isin {} not found in the database", getInstrumentCode());
				} else {
					relatedInstrument = instrument;
				}
				return instrument;
			}
		} else {
			return relatedInstrument;
		}
	}

	public void setSqlInstrumentDao(InstrumentDao sqlInstrumentDao) {
		this.sqlInstrumentDao = sqlInstrumentDao;
	}
	
	public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
		this.instrumentFinder = instrumentFinder;
	}
	
	private String  nl = System.getProperty("line.separator");
	
	
	public String toString() {
		StringBuilder strb = new StringBuilder("CSPriceDiscoveryOrderLazyBean: [");
		strb.append(nl);
		strb.append("instrumentCode = ");
		strb.append(getInstrumentCode());
		strb.append(nl);
		strb.append("side = ");
		strb.append(getSide().toString());
		if (getQty()!=null){
			strb.append(nl);
			strb.append("quantity = ");
			strb.append(getQty().toString());
		}
		strb.append(nl);
		strb.append("fIXorderId = ");
		strb.append(getFixOrderId());
		strb.append(nl);
		if(getLimit() != null) {
			strb.append("curr = ");
			strb.append(getLimit().getCurrency());
			strb.append(nl);
			strb.append("limit_price = ");
			strb.append(getLimit().getAmount());
		} else
			strb.append("*no limit_price*");
		strb.append("]");
		return strb.toString();
	}
}