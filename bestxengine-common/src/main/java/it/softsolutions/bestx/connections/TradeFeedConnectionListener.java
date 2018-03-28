package it.softsolutions.bestx.connections;

import it.softsolutions.bestx.model.Rfq.OrderSide;

import java.math.BigDecimal;
import java.util.Date;

public interface TradeFeedConnectionListener {
    void onTradeFeedNotify(String instrumentCode,
    		BigDecimal qty,
    		String traderName,
    		String ticketNum,
            OrderSide side,
            String currency,
            BigDecimal executionPriceAmount,
            Date futSettDate,
            String accountCode,
            int numberOfDaysAccrued,
            BigDecimal accruedInterestAmount);
}
