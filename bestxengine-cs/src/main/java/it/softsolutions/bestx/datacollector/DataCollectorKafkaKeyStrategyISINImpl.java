package it.softsolutions.bestx.datacollector;

import it.softsolutions.bestx.Operation;

public class DataCollectorKafkaKeyStrategyISINImpl implements DataCollectorKafkaKeyStrategy {

	@Override
	public String calculateKey(Operation operation, int attemptNo) {
		return operation.getOrder().getInstrument().getIsin();
	}

}
