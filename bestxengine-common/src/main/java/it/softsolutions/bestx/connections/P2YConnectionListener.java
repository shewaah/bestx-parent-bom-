package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.Date;

public interface P2YConnectionListener {
    void onP2YAccruedInterestResponse(String sessionId, BigDecimal accruedInterestAmount, int accruedDays, int errorCode, String errorMessage);

	void onP2YInflationRatioResponse(String requestId,
			BigDecimal calculatedInflationRatio, int errorCode, String errorMessage);
	void onP2YNextCouponDateResponse(String requestId,
			Date nextCouponDate, int errorCode, String errorMessage);
}
