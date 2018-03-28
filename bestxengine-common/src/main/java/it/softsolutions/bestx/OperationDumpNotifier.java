package it.softsolutions.bestx;

import it.softsolutions.bestx.connections.OperatorConsoleConnection;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.management.OperationDumpNotifierMBean;

public class OperationDumpNotifier implements OperationStateListener, OperationDumpNotifierMBean {
    private OperatorConsoleConnection operatorConsoleConnection;
    private volatile boolean enabled;
    
    public void init() throws BestXException {
        checkPreRequisites();
    }
    public void setOperatorConsoleConnection(OperatorConsoleConnection operatorConsoleConnection) {
        this.operatorConsoleConnection = operatorConsoleConnection;
    }
    public void enable() {
        enabled = true;
    }
    public void disable() {
        enabled = false;
    }
    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) {
        if (enabled && operatorConsoleConnection.isConnected())
            operatorConsoleConnection.publishOperationDump(operation, newState);
    }
    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (operatorConsoleConnection == null) {
            throw new ObjectNotInitializedException("Operation console connection not set");
        }
    }
}
