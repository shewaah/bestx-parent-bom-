package it.softsolutions.bestx.connections.p2y;

import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ACCRUED_AMOUNT;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ACCRUED_DAY_COUNT;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CALCULATED_INFLATION_RATIO;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_CALCULATED_NEXT_COUPON;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ERROR_CODE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_ERROR_MESSAGE;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_LABEL_MSG_ID;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.xt2.XT2InputLazyBean;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2YResultInputLazyBean extends XT2InputLazyBean {
	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

	private static final Logger LOGGER = LoggerFactory.getLogger(P2YResultInputLazyBean.class);

	public P2YResultInputLazyBean(XT2Msg msg) {
		this.msg = msg;
	}

	public String getRequestId() {
		return msg.getString(P2Y_LABEL_MSG_ID);
	}

	public int getErrorCode() {
		try {
			return msg.getInt(P2Y_LABEL_ERROR_CODE);
		} catch (Exception e) {
            LOGGER.warn("Error while extracting error code from message [{}] : {}", msg, e.getMessage());
			return 0;
		}
	}

	public String getErrorMessage() {
		return msg.getString(P2Y_LABEL_ERROR_MESSAGE);
	}

	public BigDecimal getAccruedAmount() throws BestXException {
		try {
			BigDecimal accruedAmount = new BigDecimal(msg
					.getDouble(P2Y_LABEL_ACCRUED_AMOUNT), new MathContext(20));
			accruedAmount = accruedAmount.setScale(20, BigDecimal.ROUND_FLOOR);
			return accruedAmount;
		} catch (Exception e) {
			throw new BestXException(
					"An error occurred while extracting accrued amount from message: "
							+ msg.toString() + " : " + e.getMessage(), e);
		}
	}

	public int getAccruedDayCount() throws BestXException {
		try {
			return msg.getInt(P2Y_LABEL_ACCRUED_DAY_COUNT);
		} catch (Exception e) {
			throw new BestXException(
					"An error occurred while extracting accrued day count from message: "
							+ msg.toString() + " : " + e.toString(), e);
		}
	}

	public Object getCalculatedInflationRatio() throws BestXException {
		try {
			BigDecimal calculatedInflationRatio = new BigDecimal(msg
					.getDouble(P2Y_LABEL_CALCULATED_INFLATION_RATIO),
					new MathContext(20));
			calculatedInflationRatio = calculatedInflationRatio.setScale(6,
					BigDecimal.ROUND_HALF_UP);
			return calculatedInflationRatio;
		} catch (Exception e) {
			throw new BestXException(
					"An error occurred while extracting inflation ratio from message: "
							+ msg.toString() + " : " + e.getMessage(), e);
		}
	}

	public Date getNextCouponDate() throws BestXException {
		try {
			Date nextCouponDate = dateFormatter.parse(String.valueOf(msg
					.getInt(P2Y_LABEL_CALCULATED_NEXT_COUPON)));
			return nextCouponDate;
		} catch (Exception e) {
			throw new BestXException(
					"An error occurred while extracting next coupon from message: "
							+ msg.toString() + " : " + e.getMessage(), e);
		}
	}
}
