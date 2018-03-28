package it.softsolutions.bestx.services.ordervalidation;

import it.softsolutions.bestx.model.Order;

/**
 * Result of order validation
 * @author lsgro
 *
 */
public interface OrderResult {
    /**
     * Returns the original order
     * @return An {@link Order} object
     */
    Order getOrder();
    /**
     * Validation method
     * @return True if the order is valid, false otherwise
     */
    boolean isValid();
    /**
     * Explanation for the failed validation
     * @return In case of validation failed (isValid() == false) it can be not null
     */
    String getReason();

}
