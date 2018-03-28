package it.softsolutions.bestx.connections.p2y;

import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CALCULATION_METHOD;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CALENDAR_CODE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CURRENT_2_MONTHS_RPI;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CURRENT_REFERENCE_RPI;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISIN;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISSUE_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISSUE_DATE_2_MONTHS_RPI;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ISSUE_DATE_REFERENCE_RPI;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_MSG_ID;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_REQ_NAME;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_SETTLEMENT_DATE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_MSG_NAME;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_VALUE_REQ_NAME_INFLATION_RATIO;

import java.util.Date;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.xt2.XT2OutputLazyBean;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.RPI;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.xt2.protocol.XT2Msg;

public class P2YInflationRatioReqOutputLazyBean extends XT2OutputLazyBean {
    private XT2Msg message;

    public P2YInflationRatioReqOutputLazyBean(String requestId, Instrument instrument, String calculationType, RPI referenceIssueDateRPI, RPI currentReferenceRPI, RPI issueDate2MonthsRPI,
            RPI current2MonthsRPI, Date settlementDate) throws BestXException {
        super(P2Y_MSG_NAME);
        try {
            message = super.getMsg();
            message.setValue(P2Y_LABEL_MSG_ID, requestId);
            message.setValue(P2Y_LABEL_REQ_NAME, P2Y_VALUE_REQ_NAME_INFLATION_RATIO);
            message.setValue(P2Y_LABEL_ISIN, instrument.getIsin());
            message.setValue(P2Y_LABEL_CALCULATION_METHOD, calculationType);
            message.setValue(P2Y_LABEL_ISSUE_DATE_REFERENCE_RPI, referenceIssueDateRPI.getValue().doubleValue());
            message.setValue(P2Y_LABEL_CURRENT_REFERENCE_RPI, currentReferenceRPI.getValue().doubleValue());
            message.setValue(P2Y_LABEL_SETTLEMENT_DATE, DateService.formatAsLong(DateService.dateISO, settlementDate));
            message.setValue(P2Y_LABEL_ISSUE_DATE, DateService.formatAsLong(DateService.dateISO, instrument.getIssueDate()));

            if (issueDate2MonthsRPI != null) {
                message.setValue(P2Y_LABEL_ISSUE_DATE_2_MONTHS_RPI, issueDate2MonthsRPI.getValue().doubleValue());
            }
            if (current2MonthsRPI != null) {
                message.setValue(P2Y_LABEL_CURRENT_2_MONTHS_RPI, current2MonthsRPI.getValue().doubleValue());
            }
            message.setValue(P2Y_LABEL_CALENDAR_CODE, instrument.getCalendarCode());
        } catch (Exception e) {
            throw new BestXException("An error occurred while creating P2Y bean" + " : " + e.getMessage(), e);
        }
    }

    public XT2Msg getMsg() {
        return message;
    }
}
