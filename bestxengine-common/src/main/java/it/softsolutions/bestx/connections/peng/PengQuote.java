package it.softsolutions.bestx.connections.peng;


import java.math.BigDecimal;
import java.util.Date;

public interface PengQuote {
    String getIsin();
    Integer getTraderId();
    Date getUpdateTime();
    BigDecimal getBidPriceAmount();
    BigDecimal getAskPriceAmount();
    BigDecimal getBidQuantity();
    BigDecimal getAskQuantity();
    BigDecimal getBidYield();
    BigDecimal getAskYield();
    int getQuoteType(); // 1=price quoted, 3=yeld quoted
}
