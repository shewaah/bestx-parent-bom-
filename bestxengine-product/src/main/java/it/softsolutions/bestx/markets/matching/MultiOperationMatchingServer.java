/**
 * 
 */
package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author otc-go
 * 
 */
public class MultiOperationMatchingServer implements OperationMatchingServer, Runnable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiOperationMatchingServer.class);
    private List<Operation> matchingList = new CopyOnWriteArrayList<Operation>();
    private boolean active;
    private Thread this_thread;
    private OperationMatcher operationMatcher;
    private OperationMatchingServerListener listener;
    public volatile int newOperations = 0;

    @Override
    public void addOperation(Operation operation) {
        matchingList.add(operation);
        newOperations++;
        notify();
    }

    @Override
    public boolean isOn() {
        return active;
    }

    @Override
    public void removeOperation(Operation operation) {
        matchingList.remove(operation);
    }

    private void removeOperations(List<Operation> operations) {
        for (Operation operation : operations) {
            removeOperation(operation);
        }
    }

    @Override
    public void removeOperations(Operation operationA, Operation operationB) {
        throw new UnsupportedOperationException();
    }

    private void searchMatch() {
        int lastOperationIndex = matchingList.size() - 1;
        List<Operation> result = new ArrayList<Operation>();
        while (newOperations > 0) {
            Operation operationNew = matchingList.get(lastOperationIndex);
            if (operationMatcher.operationsMatch(operationNew)) {
                result = operationMatcher.getOperationsMatched(operationNew);
                removeOperations(result);
            }
            listener.onOperationsMatch(result);
            newOperations--;
            lastOperationIndex--;
        }
    }

    @Override
    public synchronized void start() {
        active = true;
        if (this_thread == null) {
            this_thread = new Thread(this, "MultiMatchingServer");
            this_thread.start();
        }
        LOGGER.info("OperationMatchingServer started.");
    }

    @Override
    public synchronized void stop() {
        active = false;
        notify();
        Thread old = this_thread;
        this_thread = null;
        old.interrupt();
    }

    @Override
    public void run() {
        while (active) {
            searchMatch();
            try {
                wait();
            } catch (InterruptedException e) {
                LOGGER.error("Wait interrupted" + " : " + e.toString(), e);
            }
        }
    }

    public void setOperationMatcher(OperationMatcher operationMatcher) {
        this.operationMatcher = operationMatcher;
    }

    @Override
    public void setOperationMatchingServerListener(OperationMatchingServerListener listener) {
        this.listener = listener;
    }
}
