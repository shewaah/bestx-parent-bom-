package it.softsolutions.bestx.connections.mtsprime;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.EXECUTION_TYPE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_EXT_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ISIN;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_PRICE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_QUANTITY;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SIDE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.ORDER_MODE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_ORDER_REQUEST;

import java.math.BigDecimal;

import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.xt2.protocol.XT2Msg;

public class MTSPrimeOrderOutputLazyBean extends XT2OutputLazyBean {
	private final XT2Msg msg;

	public MTSPrimeOrderOutputLazyBean(MarketOrder marketOrder, String sessionId, BigDecimal qtyMultiplier) {
		super(SUBJECT_ORDER_REQUEST);
		msg = super.getMsg();

		BigDecimal propQty = marketOrder.getQty();
		if (qtyMultiplier == null)
		   qtyMultiplier = BigDecimal.ONE;
		propQty = propQty.divide(qtyMultiplier);
		//propQty = propQty.setScale(5);
		
		msg.setValue(LABEL_QUANTITY, propQty.doubleValue());

		msg.setValue(LABEL_PRICE, marketOrder.getLimit().getAmount().doubleValue());
		msg.setValue(LABEL_ISIN, marketOrder.getInstrument().getIsin());
		msg.setValue(LABEL_SIDE, marketOrder.getSide().equals(OrderSide.BUY) ? 0 : 1); // 0
																						// =
																						// Buy,
																						// 1
																						// =
																						// Sell
		msg.setValue(ORDER_MODE, 0); // --> 0 = Price, 1 = Yield
		msg.setValue(LABEL_MARKET, marketOrder.getMarket().getSubMarketCode().toString());
		// "[RoundPrice]" --> 1 default
		msg.setValue(LABEL_EXT_SESSION_ID, sessionId);
		msg.setValue(EXECUTION_TYPE, 1); // --> 0 = FAK, 1 = FOK

		// msg.setValue("Order.Aggressor.MemberId", ??);
		// msg.setValue("Order.OrderMsgInfo.MsgId", ??);
		// msg.setValue("Order.Aggressor.UserData", ??);
	}

	@Override
	public XT2Msg getMsg() {
		return msg;
	}
}
