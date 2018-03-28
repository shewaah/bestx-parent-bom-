package it.softsolutions.bestx.connections;

public interface ConnectionStateListener {

    void onCustomerConnectionUp(CustomerConnection source);

    void onCustomerConnectionDown(CustomerConnection source, String reason);

    void onTradingConsoleConnectionUp(TradingConsoleConnection source);

    void onTradingConsoleConnectionDown(TradingConsoleConnection source, String reason);

    void onOperatorConsoleConnectionUp(OperatorConsoleConnection source);

    void onOperatorConsoleConnectionDown(OperatorConsoleConnection source, String reason);
}
