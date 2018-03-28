package it.softsolutions.bestx.connections.p2y;

import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CALENDAR_CODE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_COUPON;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CURRENCY;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_DAY_COUNT;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_DAY_COUNT_CODE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_END_OF_MONTH;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_EX_DIV_CALENDAR_CODE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_EX_DIV_DAYS;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_FIRST_COUPON_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_FREQUENCY;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_INTEREST_ACCRUAL_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISIN;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISSUE_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_MATURITY_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_MSG_ID;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_PRINCIPAL;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_REQ_NAME;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_TICKER;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_MSG_NAME;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class P2YRequestOutputLazyBean extends XT2OutputLazyBean {
    private XT2Msg message;

    public P2YRequestOutputLazyBean(String requestId, String RequestName, Instrument instrument, BigDecimal coupon, Date settlementDate) throws BestXException {
        super(P2Y_MSG_NAME);
        try {
            message = super.getMsg();
            message.setValue(P2Y_LABEL_MSG_ID, requestId);
            message.setValue(P2Y_LABEL_REQ_NAME, RequestName);
            message.setValue(P2Y_LABEL_ISIN, instrument.getIsin());
            message.setValue(P2Y_LABEL_TICKER, instrument.getBbTicker());
            message.setValue(P2Y_LABEL_CURRENCY, instrument.getCurrency());
            message.setValue(P2Y_LABEL_CALENDAR_CODE, instrument.getCalendarCode());
            message.setValue(P2Y_LABEL_DAY_COUNT, instrument.getDayCount());
            message.setValue(P2Y_LABEL_DAY_COUNT_CODE, instrument.getDayCountCode());
            message.setValue(P2Y_LABEL_COUPON, coupon.doubleValue());
            message.setValue(P2Y_LABEL_PRINCIPAL, instrument.getFaceValue().longValue());
            message.setValue(P2Y_LABEL_MATURITY_DATE, DateService.formatAsLong(DateService.dateISO, instrument.getMaturityDate()));
            message.setValue(P2Y_LABEL_ISSUE_DATE, DateService.formatAsLong(DateService.dateISO, instrument.getIssueDate()));

            message.setValue(P2Y_LABEL_FIRST_COUPON_DATE, DateService.formatAsLong(DateService.dateISO, instrument.getFirstCouponDate()));
            message.setValue(P2Y_LABEL_INTEREST_ACCRUAL_DATE, DateService.formatAsLong(DateService.dateISO, instrument.getInterestAccrualDate()));
            message.setValue(P2Y_LABEL_SETTLEMENT_DATE, DateService.formatAsLong(DateService.dateISO, settlementDate));
            if (instrument.getExDivCalendar() != null) {
                message.setValue(P2Y_LABEL_EX_DIV_CALENDAR_CODE, instrument.getExDivCalendar());
            }
            if (instrument.getExDivDays() != null) {
                message.setValue(P2Y_LABEL_EX_DIV_DAYS, instrument.getExDivDays().longValue());
            }
            String freq;
            int i = instrument.getFrequency().intValue();
            switch (i) {
            case 0:
                freq = "NoFrequency";
                break;
            case 1:
                freq = "1Y";
                break;
            case 2:
                freq = "6M";
                break;
            case 3:
                freq = "4M";
                break;
            case 4:
                freq = "3M";
                break;
            case 6:
                freq = "2M";
                break;
            case 12:
                freq = "1M";
                break;
            default:
                freq = "";
            }
            message.setValue(P2Y_LABEL_FREQUENCY, freq);
            message.setValue(P2Y_LABEL_END_OF_MONTH, instrument.isEndOfTheMonth() ? 1 : 0);
        } catch (Exception e) {
            throw new BestXException("An error occurred while creating P2Y bean" + " : " + e.getMessage(), e);
        }
    }

    @Override
    public XT2Msg getMsg() {
        return message;
    }
}
