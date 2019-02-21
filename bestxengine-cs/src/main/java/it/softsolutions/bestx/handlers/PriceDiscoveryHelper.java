package it.softsolutions.bestx.handlers;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.audit.CSOperationStateAudit;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.connections.ib4j.PriceDiscoveryMessage;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal.ProposalType;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.PriceDiscoveryPerformanceMonitor;
import it.softsolutions.bestx.services.price.PriceResult;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class PriceDiscoveryHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PriceDiscoveryHelper.class);

	public static JSONObject createPriceDiscoveryMessage(SortedBook book, Order order, int bookDepth, int priceDecimals, Integer attemptNo) {
		JSONObject jsonBook = PriceDiscoveryMessage.createBook(order);
		if(book == null)
			return jsonBook;
		JSONObject prices = new JSONObject();
		JSONArray sidePrices = new JSONArray();
		JSONArray otherSidePrices = new JSONArray();
		int rank = 1;
		List<ClassifiedProposal> sideBook = null;
		List<ClassifiedProposal> otherSideBook = null;
		if(OrderSide.BUY.equals(order.getSide())) {
			sideBook = book.getValidSideProposals(order.getSide());
			otherSideBook = book.getBidProposals();
		} else  {
			sideBook = book.getValidSideProposals(order.getSide());
			otherSideBook = book.getAskProposals();
		}

		Map<String, ClassifiedProposal> propMap = new HashMap<String, ClassifiedProposal>();
		for (ClassifiedProposal prop: otherSideBook) {
			propMap.put(prop.getMarket().getMicCode()+"_"+prop.getMarketMarketMaker().getMarketMaker().getCode(), prop);
		}
		for (int i = 0; i < sideBook.size() && i< bookDepth; i++) {
			ClassifiedProposal prop = sideBook.get(i);
			sidePrices.add(createPriceRow(prop, rank, priceDecimals, order));
			ClassifiedProposal othProp = propMap.get(prop.getMarket().getMicCode()+"_"+prop.getMarketMarketMaker().getMarketMaker().getCode());
			if (othProp!=null) {
				otherSidePrices.add(createPriceRow(othProp, rank, priceDecimals, order));
			} else {
				otherSidePrices.add(createNoPriceRow(prop, rank));
			}
			rank++;
		}

		if(OrderSide.BUY.equals(order.getSide())) {
			prices.put(PriceDiscoveryMessage.FLD_ASK_SIDE, sidePrices);
			prices.put(PriceDiscoveryMessage.FLD_BID_SIDE, otherSidePrices);
		} else {
			prices.put(PriceDiscoveryMessage.FLD_BID_SIDE, sidePrices);
			prices.put(PriceDiscoveryMessage.FLD_ASK_SIDE, otherSidePrices);
		}
		if(!(attemptNo==null))
			jsonBook.put(PriceDiscoveryMessage.FLD_ATTEMPT_NO, attemptNo);
		jsonBook.put(PriceDiscoveryMessage.FLD_PRICES, prices);
		return jsonBook;
	}
	
	private static JSONObject createPriceRow(ClassifiedProposal prop, int rank, int priceDecimals, Order order) {
		JSONObject jsonRow = new JSONObject();
		jsonRow.put(PriceDiscoveryMessage.FLD_RANK, rank);
		jsonRow.put(PriceDiscoveryMessage.FLD_MULTI_DEALER, prop.getMarket().getMicCode());
		jsonRow.put(PriceDiscoveryMessage.FLD_COUNTERPARTY, prop.getMarketMarketMaker().getMarketMaker().getCode());
		jsonRow.put(PriceDiscoveryMessage.FLD_PRICE, prop.getPrice().getAmount().setScale(priceDecimals, RoundingMode.CEILING));
		jsonRow.put(PriceDiscoveryMessage.FLD_QUANTITY, prop.getQty());
		jsonRow.put(PriceDiscoveryMessage.FLD_SETTL_DATE, prop.getFutSettDate()!=null?PriceDiscoveryMessage.getSettlDateFormatter().format(prop.getFutSettDate()):(order.getFutSettDate()!= null ?PriceDiscoveryMessage.getSettlDateFormatter().format(order.getFutSettDate()):"N/A"));
		jsonRow.put(PriceDiscoveryMessage.FLD_UNCHANGED_SINCE, prop.getTimestamp()!=null?PriceDiscoveryMessage.getFormatter().format(prop.getTimestamp()):"N/A");
		jsonRow.put(PriceDiscoveryMessage.FLD_TYPE, decode(prop.getType()));
		
		return jsonRow;
	}
	
	private static JSONObject createNoPriceRow(ClassifiedProposal prop, int rank) {
		JSONObject jsonRow = new JSONObject();
		jsonRow.put(PriceDiscoveryMessage.FLD_RANK, rank);
		jsonRow.put(PriceDiscoveryMessage.FLD_MULTI_DEALER, prop.getMarket().getMicCode());
		jsonRow.put(PriceDiscoveryMessage.FLD_COUNTERPARTY, prop.getMarketMarketMaker().getMarketMaker().getCode());
		jsonRow.put(PriceDiscoveryMessage.FLD_PRICE, 0);
		jsonRow.put(PriceDiscoveryMessage.FLD_QUANTITY, 0);
		jsonRow.put(PriceDiscoveryMessage.FLD_SETTL_DATE, "N/A");
		jsonRow.put(PriceDiscoveryMessage.FLD_UNCHANGED_SINCE, "N/A");
		jsonRow.put(PriceDiscoveryMessage.FLD_TYPE, "N/A");
		
		return jsonRow;
	}

	private static String decode(ProposalType type) {
		switch(type) {
		case CLOSED: return "N/A";
		case INDICATIVE: return "I";
		case TRADEABLE: return "T";
		case RESTRICTED_TRADEABLE: return "RT";
		case COUNTER: return "T";
		case SPREAD_ON_BEST: return "N/A";
		case SET_TO_ZERO: return "N/A";
		case AXE: return "AXE";
		case IOI: return "IOI";
		}
		return "N/A";
	}
	
	public static void publishPriceDiscovery(Operation operation, PriceResult priceResult, OperatorConsoleConnection operatorConsoleConnection, int bookDepth, int priceDecimals, boolean publishAttempt) {
		Order order = operation.getOrder();
		
		// riceve il book, crea il messaggio JSON per JMS
		LOGGER.info("Got price book. Create JSON Message");
		SortedBook book = priceResult.getSortedBook();

		// create message
		PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Book arrived");
		JSONObject jsonBook;
		if(book != null) {
			if(publishAttempt) {
				jsonBook = PriceDiscoveryHelper.createPriceDiscoveryMessage(book, order, bookDepth, priceDecimals, operation.getAttemptNo());
			} else {
				jsonBook = PriceDiscoveryHelper.createPriceDiscoveryMessage(book, order, bookDepth, priceDecimals, null);
			}
			JSONArray errorReport = new JSONArray();
			for (String error: priceResult.getErrorReport()) {
				errorReport.add(error);
			}
			//discoverMarketStatus(errorReport);
			jsonBook.put(PriceDiscoveryMessage.FLD_ERROR_REPORT, errorReport);
			PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Sending price discovery");
			operatorConsoleConnection.publishPriceDiscoveryResult(operation, jsonBook.toString());
			PriceDiscoveryPerformanceMonitor.finalize(order.getCustomerOrderId(), "Price discovery sent\n\n\n");
			LOGGER.info(jsonBook.toString());
		}	else {
			LOGGER.info("Book is null. No message will be published.");
		}

	}
	
	private static boolean areAllMarketsDisabled(Operation operation) {
		for (MarketConnection marketConnection : ((CSOperationStateAudit)operation.getMarketExecutionListener()).getMarketConnectionRegistry().getAllMarketConnections()) {
            if (marketConnection.isPriceConnectionProvided()) {
            	return !marketConnection.isPriceConnectionEnabled();
            } 
        }
		return true;
	}

	public static void publishEmptyBook(Operation operation, OperatorConsoleConnection operatorConsoleConnection) {
		Order order = operation.getOrder();
		LOGGER.debug("Price discovery failed with no market");
		JSONArray errorReport = new JSONArray();
		errorReport.add(areAllMarketsDisabled(operation)?PriceDiscoveryMessage.LABEL_NO_MARKET_ENABLED:PriceDiscoveryMessage.LABEL_NO_MARKET_AVAILABLE);
		JSONObject jsonBook = PriceDiscoveryMessage.createEmptyBook(order,errorReport);

		PriceDiscoveryPerformanceMonitor.logEvent(order.getCustomerOrderId(), "Sending price discovery");
		operatorConsoleConnection.publishPriceDiscoveryResult(operation, jsonBook.toString());
		PriceDiscoveryPerformanceMonitor.finalize(order.getCustomerOrderId(), "Price discovery sent\n\n\n");
		LOGGER.info(jsonBook.toString());
	}
}
