package it.softsolutions.bestx.connections.ib4j;

import java.text.SimpleDateFormat;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.ib4j.IBException;
import it.softsolutions.ib4j.clientserver.IBcsMessage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class PriceDiscoveryMessage extends IBcsMessage {

	private static final long serialVersionUID = -5933239334119272833L;
	
	public static final String FLD_MSG_TYPE        = "msg_type";
	public static final String FLD_ORDER_ID        = "order_id";
	public static final String FLD_TRACE_ID        = "trace_id";
	public static final String FLD_INSTRUMENT      = "instrument";
	public static final String FLD_INSTRUMENT_NAME = "InstrumentName";
	public static final String FLD_SIDE            = "side";
	public static final String FLD_QUANTITY        = "quantity";
	public static final String FLD_TIMESTAMP       = "timestamp";
	public static final String FLD_PD_RESULT       = "price_discovery_result";
	public static final String FLD_PRICES          = "prices";
	public static final String FLD_ASK_SIDE        = "ask_side";
	public static final String FLD_BID_SIDE        = "bid_side";
	public static final String FLD_RANK            = "rank";
	public static final String FLD_MULTI_DEALER    = "multi_dealer";
	public static final String FLD_COUNTERPARTY    = "counterparty";
	public static final String FLD_PRICE           = "price";
	public static final String FLD_SETTL_DATE      = "settl_date";
	public static final String FLD_UNCHANGED_SINCE = "unchanged_since";
	public static final String FLD_TYPE            = "type";
	public static final String FLD_ERROR_REPORT    = "error_report";
	public static final String FLD_ATTEMPT_NO      = "attemptNo";
	
	public static final String LABEL_MSG_TYPE_BESTX_PRICE_DISC_RESP = "BestxPriceDiscResp";
	public static final String LABEL_INSTRUMENT_NOT_SUPPORTED = "This instrument was not available for Price Discovery. An attempt was made to be loaded automatically. Please try again in one minute. If the error persists, please report the missing instrument.";
	public static final String LABEL_NO_MARKET_AVAILABLE      = "All multi-dealers are unavailable";
	public static final String LABEL_NO_MARKET_ENABLED        = "All multi-dealers are disabled";
	
	public static PriceDiscoveryMessage getPDResultMessage(Operation operation, String priceDiscoveryResult) throws IBException {
		PriceDiscoveryMessage result = new PriceDiscoveryMessage();
		result.setIBPubSubSubject(IB4JOperatorConsoleMessage.PUB_SUBJ_PRICE_DISCOVERY);
		result.setValue(FLD_ORDER_ID, operation.getOrder().getFixOrderId());
		result.setValue(FLD_PD_RESULT, priceDiscoveryResult);
		return result;
	}
	
	public static JSONObject createEmptyBook(Order order, JSONArray errorReport) {
		JSONObject jsonBook = createBook(order);
		JSONObject prices = new JSONObject();
		prices.put(PriceDiscoveryMessage.FLD_ASK_SIDE, new JSONArray());
		prices.put(PriceDiscoveryMessage.FLD_BID_SIDE, new JSONArray());
		jsonBook.put(PriceDiscoveryMessage.FLD_PRICES, prices);
		jsonBook.put(PriceDiscoveryMessage.FLD_ERROR_REPORT, errorReport);
		return jsonBook;
	}

	public static SimpleDateFormat getFormatter() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		return dateFormatter;
	}

   public static SimpleDateFormat getSettlDateFormatter() {
      SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
//      dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      return dateFormatter;
   }

	public static JSONObject createBook(Order order) {		
		JSONObject jsonBook = new JSONObject();
		jsonBook.put(PriceDiscoveryMessage.FLD_MSG_TYPE, LABEL_MSG_TYPE_BESTX_PRICE_DISC_RESP);
		jsonBook.put(PriceDiscoveryMessage.FLD_ORDER_ID, order.getFixOrderId());
		if (StringUtils.isEmpty(order.getCustomerOrderId())) {
			jsonBook.put(PriceDiscoveryMessage.FLD_TRACE_ID, UUID.randomUUID().toString());
		} else {
			jsonBook.put(PriceDiscoveryMessage.FLD_TRACE_ID, order.getCustomerOrderId());
		}
		jsonBook.put(PriceDiscoveryMessage.FLD_TIMESTAMP, getFormatter().format(DateService.newLocalDate()));
		jsonBook.put(PriceDiscoveryMessage.FLD_INSTRUMENT, order.getInstrumentCode());
		
		if (order.getInstrument() != null) {
		   jsonBook.put(PriceDiscoveryMessage.FLD_INSTRUMENT_NAME, order.getInstrument().getDescription());
		} else {
		   jsonBook.put(PriceDiscoveryMessage.FLD_INSTRUMENT_NAME, "NA");
		}
		return jsonBook;
	}
}
