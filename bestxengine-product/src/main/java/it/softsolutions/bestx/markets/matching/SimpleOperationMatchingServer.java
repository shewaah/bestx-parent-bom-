package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.Operation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleOperationMatchingServer implements OperationMatchingServer, Runnable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleOperationMatchingServer.class);
    private List<Operation> matchingList = new CopyOnWriteArrayList<Operation>();
    private OperationMatcher operationMatcher;
    private OperationMatchingServerListener listener;
    private boolean active;
//    public volatile int newOperations = 0;
    public AtomicInteger newOperations = new AtomicInteger(0);
	private Thread this_thread;
    
	@Override
    public synchronized void start() {
        active = true;
		if (this_thread==null){
			this_thread = new Thread(this, "MatchingServer"); //$NON-NLS-1$
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
        while(active) {
            searchMatch();
            try {
            	wait();
            }
            catch (InterruptedException e) {
                LOGGER.error("Wait interrupted"+" : "+ e.toString(), e);
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

    @Override
    public void addOperation(Operation operation) {
        matchingList.add(operation);
//        newOperations++;
        newOperations.incrementAndGet();
        notify();
    }

    @Override
    public void removeOperation(Operation operation) {
        matchingList.remove(operation);
    }

    @Override
    public void removeOperations(Operation operationA, Operation operationB) {
        matchingList.remove(operationA);
        matchingList.remove(operationB);
    }
    
    private void searchMatch() {
        int lastOperationIndex = matchingList.size() - 1;
        while (newOperations.get() > 0) {
            Operation operationA = matchingList.get(lastOperationIndex);
            for (int i = 0; i < lastOperationIndex; i++) {
                Operation operationB = matchingList.get(i);
                if (operationMatcher.operationsMatch(operationA, operationB)) {
                    removeOperations(operationA, operationB);
                    listener.onOperationsMatch(operationA, operationB);
                    break;
                }
            }
//            newOperations--;
            newOperations.decrementAndGet();
            lastOperationIndex--;
        }
    }

    @Override
	public boolean isOn() {
		return active;
	}
}
