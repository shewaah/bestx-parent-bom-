package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class FixQuoteOutputLazyBean extends FixOutputLazyBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixQuoteOutputLazyBean.class);

    private static final int PRICE_DECIMAL_SCALE = 20;
    private static final int QTY_DECIMAL_SCALE = 20;
    private int errorCode;
    private String errorMessage;
    private String securityId;
    private String symbol;
    private String idSource;
    private String securityExchange;
    private String securityDesc;
    private BigDecimal bidPx;
    private BigDecimal offerPx;
    private BigDecimal bidSize;
    private BigDecimal offerSize;
    private Date validUntilTime;
    private Date transactTime;
    private Date futSettDate;
    private String currency;
    private Integer accruedDays;
    private BigDecimal accruedInterest;
    private String lastMkt;
    private XT2Msg msg;

    /**
     * Constructor for accepted RFQ response message
     * 
     * @param fixSessionId
     *            ID of FIX session
     * @param quoteRequestId
     *            ID or RFQ
     * @param quoteId
     *            ID of quote
     * @param quote
     *            Quote object
     */
    public FixQuoteOutputLazyBean(String fixSessionId, String quoteRequestId, String quoteId, Quote quote) {
        super(fixSessionId);
        
        idSource = "4";
        
        Instrument instrument = quote.getInstrument();
        if (instrument != null) {
            securityId = instrument.getIsin();
            securityDesc = instrument.getDescription();
            symbol = instrument.getEpic();
        }
        
        Proposal bidProposal = quote.getBidProposal();
        if (bidProposal != null) {
            bidPx = bidProposal.getPrice() != null ? bidProposal.getPrice().getAmount() : null; 
            bidSize = bidProposal.getQty();
        }
        
        Proposal askProposal = quote.getAskProposal();
        if (askProposal != null) {
            offerPx = askProposal.getPrice() != null ? askProposal.getPrice().getAmount() : null; 
            currency = askProposal.getPrice() != null ? askProposal.getPrice().getStringCurrency() : null;
            offerSize = askProposal.getQty();
            lastMkt = askProposal.getMarket() != null ? askProposal.getMarket().getMicCode() : null;
        }
        
        accruedInterest = quote.getAccruedInterest() != null ? quote.getAccruedInterest().getAmount() : BigDecimal.ZERO;
        accruedDays = quote.getAccruedInterest() != null ? quote.getAccruedDays() : 0;
        validUntilTime = quote.getExpiration();
        futSettDate = quote.getFutSettDate();
        securityExchange = "L"; // va bene per TDS
        transactTime = DateService.newLocalDate();
        
        msg = super.getMsg();
        msg.setValue(FixMessageFields.FIX_ErrorCode, errorCode);
        msg.setName(FixMessageTypes.QUOTE.toString());
        
        if (quoteRequestId != null) {
            msg.setValue(FixMessageFields.FIX_QuoteRequestID, quoteRequestId);
        }
        if (quoteId != null) {
            msg.setValue(FixMessageFields.FIX_QuoteID, quoteId);
        }
        if (errorMessage != null) {
            msg.setValue(FixMessageFields.FIX_ErrorMsg, errorMessage);
        }
        if (symbol != null) {
            msg.setValue(FixMessageFields.FIX_Symbol, symbol);
        }
        if (securityId != null) {
            msg.setValue(FixMessageFields.FIX_SecurityID, securityId);
        }
        if (idSource != null) {
            msg.setValue(FixMessageFields.FIX_IDSource, idSource);
        }
        if (securityExchange != null) {
            msg.setValue(FixMessageFields.FIX_ExDestination, securityExchange);
        }
        if (securityDesc != null) {
            msg.setValue(FixMessageFields.FIX_SecurityDesc, securityDesc);
        }
        if (bidPx != null) {
            msg.setValue(FixMessageFields.FIX_BidPriceAmount, bidPx.setScale(PRICE_DECIMAL_SCALE).doubleValue());
        }
        if (bidSize != null) {
            msg.setValue(FixMessageFields.FIX_BidPriceQty, bidSize.setScale(QTY_DECIMAL_SCALE).doubleValue());
        }
        if (offerPx != null) {
            msg.setValue(FixMessageFields.FIX_AskPriceAmount, offerPx.setScale(PRICE_DECIMAL_SCALE).doubleValue());
        }
        if (offerSize != null) {
            msg.setValue(FixMessageFields.FIX_AskPriceQty, offerSize.setScale(QTY_DECIMAL_SCALE).doubleValue());
        }
        if (validUntilTime != null) {
            msg.setValue(FixMessageFields.FIX_QuoteValidityTime, DateService.format(DateService.dateTimeISO, validUntilTime));
        }
        if (transactTime != null) {
            msg.setValue(FixMessageFields.FIX_TransactTime, DateService.format(DateService.dateTimeISO, transactTime));
        }
        if (futSettDate != null) {
            try {
                msg.setValue(FixMessageFields.FIX_FutSettDate, DateService.formatAsLong(DateService.dateTimeISO, futSettDate));
            } catch (Exception e) {
                LOGGER.error("Error converting futSettDate [{}]: {}", futSettDate, e.getMessage(), e);
            }
        }
        if (currency != null) {
            msg.setValue(FixMessageFields.FIX_Currency, currency);
        }
        if (accruedInterest != null) {
            accruedInterest = CommonFacilities.setRequestedScale(accruedInterest);
            msg.setValue(FixMessageFields.FIX_QuoteAccruedInterestAmount, accruedInterest.doubleValue());
        }

        msg.setValue(FixMessageFields.FIX_QuoteAccruedDays, accruedDays);

        if (lastMkt != null) {
            msg.setValue(FixMessageFields.FIX_QuoteMarket, lastMkt);
        }
    }

    @Override
	public XT2Msg getMsg() {
        return msg;
    }
}
