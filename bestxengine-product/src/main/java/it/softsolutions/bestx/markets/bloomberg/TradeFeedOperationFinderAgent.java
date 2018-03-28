package it.softsolutions.bestx.markets.bloomberg;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationCommand;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Rfq.OrderSide;

import java.math.BigDecimal;


public class TradeFeedOperationFinderAgent implements OperationCommand {
    private Instrument instrument;
    private BigDecimal qty;
    private OrderSide side;
    private String currency;
    private Operation foundOperation;
    public TradeFeedOperationFinderAgent(Instrument instrument, BigDecimal qty, OrderSide side, String currency) {
        this.instrument = instrument;
        this.qty = qty;
        this.side = side;
        this.currency = currency;
    }
    public void processOperation(Operation operation) throws BestXException {
        if (foundOperation == null) {
            Order order = operation.getOrder();
            if (order.getInstrument().equals(instrument) &&
                    order.getQty().compareTo(qty) == 0 &&
                    order.getSide().equals(side) &&
                    order.getCurrency().equals(currency))
                foundOperation = operation;
        }
    }
    public Operation getOperation() {
        return foundOperation;
    }
}
