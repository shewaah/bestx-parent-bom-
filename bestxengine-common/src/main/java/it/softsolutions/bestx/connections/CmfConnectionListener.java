package it.softsolutions.bestx.connections;

import java.math.BigDecimal;

public interface CmfConnectionListener {
    void onRequestReply(String orderId, int errorCode, String errorMessage);
    void onRequestAck(String orderId, String ticketNum, String traderId);
    void onRequestNack(String orderId);
    void onPendingAccept(String tsn, String traderId, String pendingTicket, String btsTicket, BigDecimal price);
    void onPendingReject(String tsn, String traderId, String reason);
    void onPendingExpire(String tsn, String traderId);
    void onPendingCounter(String tsn, String traderId, String pendingTicket, String btsTicket, BigDecimal price, String side);
    void onAutoExecution(String tsn, String traderId, String pendingTicket, String btsTicket, BigDecimal price);
 }
