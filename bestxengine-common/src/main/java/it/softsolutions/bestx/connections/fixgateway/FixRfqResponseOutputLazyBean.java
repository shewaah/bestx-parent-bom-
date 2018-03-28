package it.softsolutions.bestx.connections.fixgateway;

import java.util.Date;

import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class FixRfqResponseOutputLazyBean extends FixOutputLazyBean {
    
    private int errorCode;
    private String errorMessage;
    private String quoteRequestId;
    private Date transactTime;
    
    /**
     * Constructor for rejected RFQ response message
     * @param fixSessionId ID of FIX session
     * @param quoteRequestId ID or RFQ
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    public FixRfqResponseOutputLazyBean(String fixSessionId, String quoteRequestId, int errorCode, String errorMessage) {
        super(fixSessionId);
        this.quoteRequestId = quoteRequestId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        transactTime = DateService.newLocalDate();
    }
    
    /**
     * Constructor for accepted RFQ response message
     * @param fixSessionId ID of FIX session 
     * @param quoteRequestId ID or RFQ
     * @param quoteId ID of quote
     * @param quote Quote object
     */
    
    @Override
	public XT2Msg getMsg() {
        XT2Msg msg = super.getMsg();
        msg.setName(FixMessageTypes.QUOTE_REQUEST_RESP.toString());
        msg.setValue(FixMessageFields.FIX_ErrorCode, errorCode);
        
        if (quoteRequestId != null) {
            msg.setValue(FixMessageFields.FIX_QuoteRequestID, quoteRequestId);
        }
        if (errorMessage != null) {
            msg.setValue(FixMessageFields.FIX_ErrorMsg, errorMessage);
        }
        if (transactTime != null) {
            msg.setValue(FixMessageFields.FIX_TransactTime, DateService.format(DateService.dateTimeISO, transactTime));
        }
        
        return msg;
    }
}
