package it.softsolutions.bestx.services.instrumentstatus;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;

/* 2009-09 Ruggero
 * This interface defines the service that listens for every instrument quoting status
 * change.
 * It requires that the developer records a map made of isin->operations on that isin and
 * another one, isin->bond quoting status.
 */
public interface InstrumentStatusNotifier {

    /*
     * THis method is called when the magnet starts. IT adds the operation to the isins list, check if the isin is in the PVOL status and
     * acts accordingly.
     * 
     * The method is not synchronized, it is enough locking the shared maps.
     */
    void recordOperationAndIsin(String isin, Operation operation);

    /*
     * This method is called when an operation reaches a terminal state. IT could be executed, rejected, cancelled and so on. Concisely :
     * the magnet is ended for this operation. We must remove it from the isin's list. If we arrive here we can say the this list MUST
     * exists, but we check for its existence nonetheless.
     * 
     * There's the chance that this method is called with a null operation, it means that the caller wants to delete the isin from the map.
     * 
     * Remember that when a new isin is added it will be made mapping to an empty operations list, empty means with size equal to zero, but
     * NOT NULL.
     * 
     * Both the method and the access to the isinsAndOrders list are synchronized because this action must be done one by one and the access
     * at the variable must be exclusive.
     */
    void removeOperationFromIsin(String isin, Operation operation);

    boolean isOperationAlreadyRegistered(Operation operation);

    /*
     * This method is called by the market connections on receiving a quoting status message. It can be on an already registered isin or on
     * a new one. In the former case we will have a null operations list (Maps behaviour) and we will have to add the new isin in the isins
     * status map and in the isins/operations map. In the latter case we have to check if there's a passage from PVOL -> other status or
     * other status -> PVOL, if so it is an end or a start of a volatility call. We have to register the new status for this isin and, if we
     * have operations mapped to it, we must notify the event.
     */
    void quotingStatusChanged(String isin, QuotingStatus quotingStatus);

}