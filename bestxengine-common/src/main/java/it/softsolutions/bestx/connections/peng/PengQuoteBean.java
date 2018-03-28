package it.softsolutions.bestx.connections.peng;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.connections.fixgateway.FixInputLazyBean;
import it.softsolutions.bestx.connections.fixgateway.FixMessageFields;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class PengQuoteBean extends FixInputLazyBean implements PengQuote {

    private static final Logger LOGGER = LoggerFactory.getLogger(PengQuoteBean.class);

 //   private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

    public PengQuoteBean(XT2Msg msg) {
        this.msg = msg;
    }

    @Override
	public BigDecimal getAskQuantity() {
        // Integer intEncodedQty = new Integer(0);
        Integer intEncodedQty = getIntField("AskAmount");
        if (intEncodedQty != null) {
            return new BigDecimal(String.valueOf(intEncodedQty));
        } else {
            LOGGER.debug("Ask quantity not found");
            return BigDecimal.ZERO;
        }
    }

    @Override
	public BigDecimal getAskYield() {
        Double doubleEncodedYield = getDoubleField("AskYield");
        if (doubleEncodedYield != null) {
            return new BigDecimal(String.valueOf(doubleEncodedYield));
        } else {
            LOGGER.debug("Ask yield not found");
            return null;
        }
    }

    @Override
	public BigDecimal getBidQuantity() {
        // Integer intEncodedQty = new Integer(100000);
        Integer intEncodedQty = getIntField("BidAmount");
        if (intEncodedQty != null) {
            return new BigDecimal(String.valueOf(intEncodedQty));
        } else {
            LOGGER.debug("Bid quantity not found");
            return BigDecimal.ZERO;
        }
    }

    @Override
	public BigDecimal getBidYield() {
        Double doubleEncodedYield = getDoubleField("BidYield");
        if (doubleEncodedYield != null) {
            return new BigDecimal(String.valueOf(doubleEncodedYield));
        } else {
            LOGGER.debug("Bid yield not found");
            return null;
        }
    }

    @Override
	public String getIsin() {
        return getStringField("SecurityID");
    }

    @Override
	public int getQuoteType() {
        return getIntField("QuoteType").intValue();
    }

    @Override
	public Integer getTraderId() {
        return getIntField("TraderID");
    }

    @Override
	public Date getUpdateTime() {
        String stringFormattedDate = getStringField("UpdateTime");
        if (stringFormattedDate != null) {
            return DateService.parse(DateService.dateISO, getStringField(FixMessageFields.FIX_TransactTime));
        }
        return null;
    }

    @Override
	public BigDecimal getAskPriceAmount() {
        Double doubleEncodedPrice = getDoubleField("AskPrice");
        if (doubleEncodedPrice != null) {
            BigDecimal askPriceAmount = new BigDecimal(doubleEncodedPrice, new MathContext(5));
            askPriceAmount = askPriceAmount.setScale(5, BigDecimal.ROUND_HALF_UP);
            return askPriceAmount;
        } else {
            LOGGER.debug("Ask price not found");
            return null;
        }
    }

    @Override
	public BigDecimal getBidPriceAmount() {
        Double doubleEncodedPrice = getDoubleField("BidPrice");
        if (doubleEncodedPrice != null) {
            BigDecimal bidPriceAmount = new BigDecimal(doubleEncodedPrice, new MathContext(5));
            bidPriceAmount = bidPriceAmount.setScale(5, BigDecimal.ROUND_HALF_DOWN);
            return bidPriceAmount;
        } else {
            LOGGER.debug("Bid not price found");
            return null;
        }
    }
}
