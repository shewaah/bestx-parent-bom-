package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.RPI;

import java.math.BigDecimal;
import java.util.Date;

public interface P2YConnection extends Connection {
	void setListener(P2YConnectionListener listener);

	void requestAccruedInterest(String sessionId, Instrument instrument,
			BigDecimal coupon, Date settlementDate) throws BestXException;

	void requestInflationLinkedRatio(String requestId, Instrument instrument,
			String calculationType, RPI referenceIssueDateRPI,
			RPI currentReferenceRPI, RPI issueDate2MonthsRPI,
			RPI Current2MonthsRPI, Date settlementDate) throws BestXException;

	void requestNextCouponDate(String sessionId, Instrument instrument,
			Date settlementDate) throws BestXException;
}
