package it.softsolutions.bestx.connections.fixgateway;

import it.softsolutions.xt2.protocol.XT2Msg;

public class FixRevokeResponseOutputLazyBean extends FixOutputLazyBean {
    private XT2Msg msg;
    
    public FixRevokeResponseOutputLazyBean(String fixSessionId, String orderId, boolean accept, 
    										String comment) {
        super(fixSessionId);
        msg = super.getMsg();
        msg.setName(FixMessageFields.FIX_OrderRevokeResponse);        	
        msg.setValue(FixMessageFields.FIX_OrigClOrdID, orderId);
        msg.setValue(FixMessageFields.FIX_OrderID, orderId);
        if (accept) {
            msg.setValue(FixMessageFields.FIX_ErrorCode, 0);
            msg.setValue(FixMessageFields.FIX_ErrorMsg, "");
        } else {
        	if (comment != null && comment.trim().length() > 0) {
                msg.setValue(FixMessageFields.FIX_ErrorCode, 1);
                msg.setValue(FixMessageFields.FIX_ErrorMsg, comment.trim());
        	} else {
	            msg.setValue(FixMessageFields.FIX_ErrorCode, 0);
	            msg.setValue(FixMessageFields.FIX_ErrorMsg, "");
        	}
        	msg.setValue(FixMessageFields.FIX_OrdStatus, "A");
        }
        
    }
    
    public XT2Msg getMsg() {
        return msg;
    }
}
