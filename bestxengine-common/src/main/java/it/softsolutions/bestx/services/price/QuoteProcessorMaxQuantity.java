package it.softsolutions.bestx.services.price;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public class QuoteProcessorMaxQuantity implements QuoteProcessor {

    private int maxQuoteQuantity;

    public void setMaxQuoteQuantity(int maxQuoteQuantity) {
        this.maxQuoteQuantity = maxQuoteQuantity;
    }

    public Quote process(Quote bean, Instrument instrument, OrderSide orderSide, BigDecimal qty, Date futSettDate) {
        if (bean.getAskProposal().getQty().intValue() > maxQuoteQuantity) {
            Proposal askProposal = new Proposal();
            askProposal.setValues(bean.getAskProposal());
            askProposal.setQty(new BigDecimal(maxQuoteQuantity));
            bean.setAskProposal(askProposal);
        }
        if (bean.getBidProposal().getQty().intValue() > maxQuoteQuantity) {
            Proposal bidProposal = new Proposal();
            bidProposal.setValues(bean.getBidProposal());
            bidProposal.setQty(new BigDecimal(maxQuoteQuantity));
            bean.setBidProposal(bidProposal);
        }
        return bean;
    }
}
