package it.softsolutions.bestx.management;

public interface ConfigurableOperationRegistryMBean {
    /**
     * Returns the number of active operations currently in the registry
     * 
     * @return An integer value
     */
    int getNumberOfActiveOperations();
    long getNumberOfExceptions();
    String putOperationInVisibleState(String orderId);
    /**
     * Return the total number of operations
     * @return total operations number
     */
    int getTotalNumberOfOperations();
    
    /**
     * Called to update the text field of an Order object. Its persistence is left to the state changing mechanism.
     * @param orderID the order whose text must be updated
     * @param newText the new text
     * @return 1 if succesfull, 0 if something went wrong
     */
    int updateOrderText(String orderID, String newText);
}
