/*
 * Copyright 1997-2012 SoftSolutions! srl 
 * All Rights Reserved. 
 * 
 * NOTICE: All information contained herein is, and remains the property of SoftSolutions! srl 
 * The intellectual and technical concepts contained herein are proprietary to SoftSolutions! srl and 
 * may be covered by EU, U.S. and other Foreign Patents, patents in process, and 
 * are protected by trade secret or copyright law. 
 * Dissemination of this information or reproduction of this material is strictly forbidden 
 * unless prior written permission is obtained from SoftSolutions! srl.
 * Any additional licenses, terms and conditions, if any, are defined in file 'LICENSE.txt', 
 * which may be part of this software package.
 */

package it.softsolutions.bestx.services;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.connections.regulated.RegulatedMessageFields;
import it.softsolutions.bestx.dao.InstrumentStatusNotifierDAO;
import it.softsolutions.bestx.model.Instrument.QuotingStatus;
import it.softsolutions.bestx.services.instrumentstatus.InstrumentStatusNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 2009-09 Ruggero
 * This service manages the trading status changes for MOT market
 * instruments.
 * All the actions on the isinsAndOrders list are synchronized.
 * 
 * Every MOT magnet order on start will register itself in the isinsAndOrders
 * list.
 * On every quoting status change, if it is a PVOL we'll check if there is
 * already the isin in the list. This means that there're orders on it so
 * we've to notify the volatility call.
 * If the isin is new, we just save it with an empty orders list. Future
 * orders on this isin will instantly know that it is in volatility call.
 *
 * IF the magnet never sends orders for an already registered isin, then
 * we will have a list without operations.
 */

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: Creation date: 19-ott-2012
 * 
 **/
public class InstrumentStatusNotifierImpl implements InstrumentStatusNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentStatusNotifierImpl.class);
    
    private ConcurrentMap<String, List<Operation>> isinsAndOrders;
    private ConcurrentMap<String, String> isinsStatus;
    private InstrumentStatusNotifierDAO instrStatusNotifierDAO;

    private static final String STARTING_LABEL = "[INSTR_STATUS_NOTIFIER] ";
    private static final String DEFAULT_STATUS = "CAN";

    /**
     * Inits the.
     */
    public void init() {
        isinsAndOrders = new ConcurrentHashMap<String, List<Operation>>();
        isinsStatus = new ConcurrentHashMap<String, String>();
    }

    private void addIsin(String isin) {
        LOGGER.debug("{} New isin for the instruments status notifier : {}", STARTING_LABEL, isin);
        List<Operation> emptyList = new ArrayList<Operation>();
        isinsStatus.put(isin, DEFAULT_STATUS);
        isinsAndOrders.put(isin, emptyList);
    }

    /*
     * THis method is called when the magnet starts. IT adds the operation to the isins list, check if the isin is in the PVOL status and
     * acts accordingly.
     * 
     * The method is not synchronized, it is enough locking the shared maps.
     */
    @Override
    public void recordOperationAndIsin(String isin, Operation operation) {
        LOGGER.debug("{} Order {} for the isin {} arrived.", STARTING_LABEL, operation.getOrder().getFixOrderId(), isin);
        List<Operation> tmpList = isinsAndOrders.get(isin);
        if (tmpList != null) {
            tmpList.add(operation);
        } else {
            LOGGER.debug("{} Isin not in list. Adding it.", STARTING_LABEL);
            // add the isin to the list together with an empty operations list
            // add the isin to the isins status list with the "CAN" status as a default
            addIsin(isin);
            tmpList = isinsAndOrders.get(isin);
            tmpList.add(operation);
        }
        isinsAndOrders.put(isin, tmpList);
        if (isinsStatus.get(isin).equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)) {
            // notify to only this operation the PVOL state
            instrStatusNotifierDAO.updateStateDescForStartVolatilityCall(operation.getOrder().getFixOrderId());
        }
    }

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
    @Override
    public synchronized void removeOperationFromIsin(String isin, Operation operation) {
        LOGGER.debug("{} Removing order for the isin {}.", STARTING_LABEL, isin);
        if (operation == null) {
            LOGGER.debug("{} Operation null, we assume the caller wants to remove the isin and its list if existing", STARTING_LABEL);
        } else {
            LOGGER.debug("{} Order = {}", STARTING_LABEL, operation.getOrder().getFixOrderId());
        }

        List<Operation> tmpList = isinsAndOrders.get(isin);
        if (tmpList != null) {
            if (tmpList.size() > 0 && operation != null) {
                tmpList.remove(operation);
            }
            // there are no operations (it should never happen...)
            // or the operation to remove is null
            // then we have to remove the isin from the map
            // otherwise we put the updated list in the map
            if (tmpList.size() == 0 || operation == null) {
                removeIsin(isin);
            } else {
                isinsAndOrders.put(isin, tmpList);
            }
        } else {
            LOGGER.debug("{} Isin not found in the volatility call list : {}", STARTING_LABEL, isin);
        }
    }

    private void removeIsin(String isin) {
        List<Operation> tmpList = isinsAndOrders.get(isin);

        if (tmpList != null) {
            isinsAndOrders.remove(isin);
            isinsStatus.remove(isin);
        } else {
            LOGGER.debug("{} Isin not found in the volatility call list: {}", STARTING_LABEL, isin);
        }

    }

    /**
     * Gets the num operations for isin.
     * 
     * @param isin
     *            the isin
     * @return the num operations for isin
     */
    public int getNumOperationsForIsin(String isin) {
        return isinsAndOrders.get(isin).size();
    }

    @Override
    public boolean isOperationAlreadyRegistered(Operation operation) {
        Set<Entry<String, List<Operation>>> entries = isinsAndOrders.entrySet();
        for(Entry<String, List<Operation>> entry : entries){
        	if(entry.getValue().contains(operation))
        		return true;
        }
    	return false;
    }

    /*
     * This method is called by the market connections on receiving a quoting status message. It can be on an already registered isin or on
     * a new one. In the former case we will have a null operations list (Maps behaviour) and we will have to add the new isin in the isins
     * status map and in the isins/operations map. In the latter case we have to check if there's a passage from PVOL -> other status or
     * other status -> PVOL, if so it is an end or a start of a volatility call. We have to register the new status for this isin and, if we
     * have operations mapped to it, we must notify the event.
     */
    @Override
    public synchronized void quotingStatusChanged(String isin, QuotingStatus quotingStatus) {
        List<Operation> operations = null;
        operations = isinsAndOrders.get(isin);
        
        /*
         * It is possible that the isin isn't in the list, so we have to check it and check which is the new quoting status. We take the
         * lock on the isinsStatus to avoid status changes while working on the isin.
         */

        // 1st if : isin already registered, saved status is NOT PVOL and new status is PVOL : start volatility call
        if (operations != null && !isinsStatus.get(isin).equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)
                && quotingStatus.name().equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)) {
            LOGGER.debug("Isin gone into volatility call.");
            // notify all the operations
            if (operations.size() > 0) {
                for (final Operation operation : operations) {
                    instrStatusNotifierDAO.updateStateDescForStartVolatilityCall(operation.getOrder().getFixOrderId());
                }
            }
            // update the quoting status of the isin
            isinsStatus.put(isin, quotingStatus.name());
        }
        // 2nd if : isin already registered, registered status is PVOL and new status different from PVOL : end of volatiliy call
        else if (operations != null && isinsStatus.get(isin).equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)
                && !quotingStatus.name().equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)) {
            LOGGER.debug("Isin exited from volatility call.");
            // notify all the operations
            if (operations.size() > 0) {
                for (final Operation operation : operations) {
                    instrStatusNotifierDAO.updateStateDescForEndVolatilityCall(operation.getOrder().getFixOrderId());
                }
            }
            // update the quoting status of the isin
            isinsStatus.put(isin, quotingStatus.name());
        }
        // 3rd if : isin not registered, new status PVOL : save it for possible operations on it.
        else if (operations == null && quotingStatus.name().equals(RegulatedMessageFields.VALUE_TRADING_STATUS_VOLATILITY_AUCTION)) {
            LOGGER.debug("Isin not registered is in volatility call. ADd it with an empty operations list.");
            addIsin(isin);
            // update the quoting status of the isin
            isinsStatus.put(isin, quotingStatus.name());
        }
        // in all the other cases we have nothing to do
    }

    /**
     * Gets the instr status notifier dao.
     * 
     * @return the instr status notifier dao
     */
    public InstrumentStatusNotifierDAO getInstrStatusNotifierDAO() {
        return instrStatusNotifierDAO;
    }

    /**
     * Sets the instr status notifier dao.
     * 
     * @param instrStatusNotifierDAO
     *            the new instr status notifier dao
     */
    public void setInstrStatusNotifierDAO(InstrumentStatusNotifierDAO instrStatusNotifierDAO) {
        this.instrStatusNotifierDAO = instrStatusNotifierDAO;
    }
}