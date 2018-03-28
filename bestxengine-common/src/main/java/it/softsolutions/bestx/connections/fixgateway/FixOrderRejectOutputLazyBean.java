package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class FixOrderRejectOutputLazyBean extends FixOutputLazyBean {
    
    private String currency;
    private String customerOrderId;
    private OrderSide side;
    private BigDecimal orderQty;
    private String securityId;
    private Date transactTime;
    private XT2Msg msg;
    
    public FixOrderRejectOutputLazyBean(String sessionId, Order order, String orderId, int errorCode, String errorMessage) {
        super(sessionId);
        customerOrderId = order.getCustomerOrderId();
        securityId = order.getInstrument().getIsin();
        orderQty = order.getQty();
        side = order.getSide();
        currency = order.getInstrument().getCurrency();
        transactTime = DateService.newLocalDate();

        msg = super.getMsg();
        msg.setName(FixMessageTypes.TRADE.toString());
        msg.setValue(FixMessageFields.FIX_ErrorCode, errorCode);
        
        if (orderId != null) {
            msg.setValue(FixMessageFields.FIX_OrderID, orderId);
        }
        if (errorMessage != null) {
            msg.setValue(FixMessageFields.FIX_ErrorMsg, errorMessage);
        }
        if (customerOrderId != null) {
            msg.setValue(FixMessageFields.FIX_ClOrdID, customerOrderId);
        }
        if (securityId != null) {
            msg.setValue(FixMessageFields.FIX_SecurityID, securityId);
        }
        if (side != null) {
            msg.setValue(FixMessageFields.FIX_Side, side.getFixCode());
        }
        if (orderQty != null) {
            msg.setValue(FixMessageFields.FIX_OrderQty, orderQty.doubleValue());
        }
        if (currency != null) {
            msg.setValue(FixMessageFields.FIX_Currency, currency);
        }
        if (transactTime != null) {
            msg.setValue(FixMessageFields.FIX_TransactTime, DateService.format(DateService.dateTimeISO, transactTime));
        }
    }
    
    
    @Override
    public XT2Msg getMsg() {
        return msg;
    }
}
