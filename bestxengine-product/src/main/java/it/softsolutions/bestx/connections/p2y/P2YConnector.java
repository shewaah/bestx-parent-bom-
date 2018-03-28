package it.softsolutions.bestx.connections.p2y;

import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_VALUE_REQ_NAME_ACCRUED_AMOUNT;
import static it.softsolutions.bestx.connections.p2y.P2YMessageFields.P2Y_VALUE_REQ_NAME_NEXT_COUPON;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.connections.P2YConnection;
import it.softsolutions.bestx.connections.P2YConnectionListener;
import it.softsolutions.bestx.connections.xt2.XT2BaseConnector;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.RPI;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2YConnector extends XT2BaseConnector implements P2YConnection {
	public static final int RPIRatioPrecision = 5;
	private static final Logger LOGGER = LoggerFactory.getLogger(P2YConnector.class);
	private P2YConnectionListener listener;

	public void setListener(P2YConnectionListener listener) {
		this.listener = listener;
	}

	public void requestAccruedInterest(String sessionId, Instrument instrument,
			BigDecimal coupon, Date settlementDate) throws BestXException {
		P2YRequestOutputLazyBean request = new P2YRequestOutputLazyBean(
				sessionId, P2Y_VALUE_REQ_NAME_ACCRUED_AMOUNT, instrument,
				coupon, settlementDate);
		LOGGER.debug("Sent message to: P2Y - msg: " + request.getMsg());
		sendRequest(request.getMsg());
	}

	public void requestNextCouponDate(String sessionId, Instrument instrument,
			Date settlementDate) throws BestXException {
		P2YRequestOutputLazyBean request = new P2YRequestOutputLazyBean(
				sessionId, P2Y_VALUE_REQ_NAME_NEXT_COUPON, instrument,
				instrument.getCoupon(), settlementDate);
		LOGGER.debug("Sent message to: P2Y - msg: " + request.getMsg());
		sendRequest(request.getMsg());
	}

	@Override
	public void onReply(XT2Msg msg) {
		LOGGER.debug("Query reply received from module: P2Y - msg: " + msg);
		if (msg.getName().equalsIgnoreCase(
				P2YMessageFields.P2Y_ACCRUED_RESP_NAME)) {
			P2YResultInputLazyBean result = new P2YResultInputLazyBean(msg);
			try {
				listener.onP2YAccruedInterestResponse(result.getRequestId(),
						result.getAccruedAmount(), result.getAccruedDayCount(),
						result.getErrorCode(), result.getErrorMessage());
			} catch (BestXException e) {
				LOGGER.error(
						"An error occurred while extracting accrued interest information from: {} : {}", msg, e.getMessage(), e);
			}
		} else if (msg.getName().equalsIgnoreCase(
				P2YMessageFields.P2Y_NEXT_COUPON_RESP_NAME)) {
			P2YResultInputLazyBean result = new P2YResultInputLazyBean(msg);
			try {
				// Date nextCouponDate = ((Date)result.getNextCouponDate());
				listener.onP2YNextCouponDateResponse(result.getRequestId(),
						result.getNextCouponDate(), result.getErrorCode(),
						result.getErrorMessage());
			} catch (Exception e) {
				LOGGER.error(
						"An error occurred while extracting next coupon date from: {} : {}", msg, e.getMessage(), e);
			}
		} else {
			P2YResultInputLazyBean result = new P2YResultInputLazyBean(msg);
			try {
				// BigDecimal calculatedInflationRatio =
				// ((BigDecimal)result.getCalculatedInflationRatio());
				listener.onP2YInflationRatioResponse(result.getRequestId(),
						(BigDecimal) result.getCalculatedInflationRatio(),
						result.getErrorCode(), result.getErrorMessage());
			} catch (Exception e) {
				LOGGER.error(
						"An error occurred while extracting inflation linked ratio information from: {} : {}", msg, e.getMessage(), e);
			}
		}
	}

	public void requestInflationLinkedRatio(String requestId,
			Instrument instrument, String calculationType,
			RPI referenceIssueDateRPI, RPI currentReferenceRPI,
			RPI issueDate2MonthsRPI, RPI current2MonthsRPI, Date settlementDate)
			throws BestXException {
		P2YInflationRatioReqOutputLazyBean request = new P2YInflationRatioReqOutputLazyBean(
				requestId, instrument, calculationType, referenceIssueDateRPI,
				currentReferenceRPI, issueDate2MonthsRPI, current2MonthsRPI,
				settlementDate);
		LOGGER.debug("Sent message to: P2Y - msg: " + request.getMsg());
		sendRequest(request.getMsg());
	}
}
