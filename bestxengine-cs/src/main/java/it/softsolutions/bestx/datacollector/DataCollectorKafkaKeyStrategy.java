package it.softsolutions.bestx.datacollector;

import it.softsolutions.bestx.Operation;

public interface DataCollectorKafkaKeyStrategy {
	String calculateKey(Operation operation, int attemptNo);
}
