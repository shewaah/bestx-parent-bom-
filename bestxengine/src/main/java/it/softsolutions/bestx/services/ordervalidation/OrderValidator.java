package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Order;

/**
 * This interface allows validation of orders with respect to corresponding RFQ
 * and specific policies
 * @author lsgro
 *
 */
public interface OrderValidator {
    /**
     * Returns a {@link OrderResult} object with validation information
     * @param operation The operation against which the order is to be validated
     * @param order The order to be validated
     * @return An {@link OrderResult} object
     * @see {@link OrderResult}
     */
    OrderResult validateOrder(Operation operation, Order order);

    /**
     * 
     * @return true if the validator needs the instrument in the DB 
     */
    boolean isDbNeeded();
    
    /**
     * 
     * @return true if this validator is an instrument DB check
     */
    boolean isInstrumentDbCheck();
}
