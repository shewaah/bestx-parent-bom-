/**
 * 
 */
package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.bestx.connections.CustomerConnection;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

/**
 * @author Stefano
 *
 */
public class FixOrderResponseOutputLazyBean extends FixOutputLazyBean {
    
    protected XT2Msg msg;
    
	/**
	 * @param fixSessionId
	 */
	public FixOrderResponseOutputLazyBean(String fixSessionId, Order order, CustomerConnection.ErrorCode errorCode, String errorMsg) {
		super(fixSessionId);
        msg = super.getMsg();                
        
        msg.setName(FixMessageTypes.ORDER_RESP.toString());
        msg.setValue(FixMessageFields.FIX_ExecID, "" + DateService.currentTimeMillis());
        msg.setValue(FixMessageFields.FIX_OrderID, order.getFixOrderId());
        msg.setValue(FixMessageFields.FIX_ClOrdID, order.getFixOrderId());
        msg.setValue(FixMessageFields.FIX_ErrorCode, errorCode.getCode());
        msg.setValue(FixMessageFields.FIX_ErrorMsg, errorMsg);
        msg.setValue(FixMessageFields.FIX_OrdType, order.getType().getFixCode());
        msg.setValue(FixMessageFields.FIX_Symbol, order.getInstrument() != null? order.getInstrument().getIsin() : order.getInstrumentCode());
        
        // [DR20120613] Prevent nullPointerException
        if (order.getSide() != null) {
            msg.setValue(FixMessageFields.FIX_Side, order.getSide().getFixCode());
        }
        
        // [DR20120613] Prevent nullPointerException
        if (order.getQty() != null) {
            msg.setValue(FixMessageFields.FIX_OrderQty, order.getQty().toPlainString());
        }
        
        if (order.getLimit() != null && order.getLimit().getAmount() != null) {
        	msg.setValue(FixMessageFields.FIX_Price, order.getLimit().getAmount().toPlainString());
        } else {
        	msg.setValue(FixMessageFields.FIX_Price, "0.0");
        }
        
        // [DR20120705] 
        if (order.getPriceType() != null) {
            msg.setValue(FixMessageFields.FIX_PriceType, order.getPriceType());
        }
	}
	
    public XT2Msg getMsg() {
        return msg;
    }
}
