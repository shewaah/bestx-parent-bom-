package it.softsolutions.bestx.connections;

import java.math.BigDecimal;
import java.util.Date;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.model.Rfq.OrderSide;

public interface CmfConnection extends Connection {
    void setListener(CmfConnectionListener listener);
    /**
     * 
     * @param tsn a unique id during the day
     * @param traderName can be null
     * @param account the customer id
     * @param isin the ISIN code of the requested Bond
     * @param qty	the requested quantity
     * @param price the proposed price
     * @param status the status values accepted "2", "3", "4"
     * @param reisChoice states of the request is always pending or depending on the Bloomberg terminal conf
     * @param orderSide side
     * @param orderId FIX Order id
     * @param expiration expiration time
     * @param futSettDate settlement date. If null the default is assumed
     * @param sourceCode a parameter providing the ID of the calling source code
     * @throws BestXException
     */
    void sendRequest(
            String tsn,
            String traderName,
            String account,
            String isin,
            BigDecimal qty,
            BigDecimal price,
            String status,
            String reisChoice,
            OrderSide orderSide,
            String orderId,
            String expiration,
            Date futSettDate,
            Integer sourceCode,
            Integer ticketNum // Optional
            ) throws BestXException;
}
