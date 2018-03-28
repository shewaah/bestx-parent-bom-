/**
 * 
 */
package it.softsolutions.bestx.connections.regulated;

import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORDER_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_ORIGINAL_CLIENT_ORDERID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_ISIN;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_MARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SESSION_ID;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SIDE;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_REG_SUBMARKET;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_SYMBOL;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.LABEL_TRANSACT_TIME;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.SUBJECT_ORDER_CANCEL_REQUEST;
import static it.softsolutions.bestx.connections.regulated.RegulatedMessageFields.VALUE_SYMBOL;

import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * @author Stefano
 * 
 */
public class CancelRequestOutputBean extends XT2OutputLazyBean {

    private XT2Msg orderCancelMessage;

    /**
     * @param motOrderId
     * @param name
     */
    public CancelRequestOutputBean(MarketOrder marketOrder, String marketCode, String account, String origClOrdId, String clOrdId, String marketOrderId) {
        super(SUBJECT_ORDER_CANCEL_REQUEST);
        orderCancelMessage = super.getMsg();
        // 2009-09-11 Ruggero: When sending a revoke to the MOT we need to specify in which submarket is our order (ATS needs it to
        // correctly send the revoke)
        String marketName = "";
        if (marketOrder != null && marketOrder.getMarket() != null && marketOrder.getMarket().getSubMarketCode() != null) {
            marketName = marketOrder.getMarket().getSubMarketCode().name();
        }
        String isin = null;
        if (marketOrder != null && marketOrder.getInstrument() != null) {
        	isin = marketOrder.getInstrument().getIsin();
        }
        String side = null;
        if(marketOrder != null && marketOrder.getSide() != null) {
        	side = marketOrder.getSide().getFixCode();
        }
        String transactTimeStr = null;
        if(marketOrder != null && marketOrder.getTransactTime() != null) {
        	transactTimeStr = DateService.format(DateService.dateTimeISO, marketOrder.getTransactTime());
        }
        orderCancelMessage.setValue(LABEL_REG_SUBMARKET, marketName);
        orderCancelMessage.setValue(LABEL_REG_MARKET, marketCode);
        orderCancelMessage.setValue(LABEL_REG_SESSION_ID, clOrdId);
        orderCancelMessage.setValue(LABEL_ORDER_ID, marketOrderId);
        orderCancelMessage.setValue(LABEL_ORIGINAL_CLIENT_ORDERID, origClOrdId);
        orderCancelMessage.setValue(LABEL_REG_ISIN, isin);
        orderCancelMessage.setValue(LABEL_REG_SIDE, side);
        orderCancelMessage.setValue(LABEL_SYMBOL, VALUE_SYMBOL);
        orderCancelMessage.setValue(LABEL_TRANSACT_TIME, transactTimeStr);
    }

    @Override
    public XT2Msg getMsg() {
        return orderCancelMessage;
    }
}
