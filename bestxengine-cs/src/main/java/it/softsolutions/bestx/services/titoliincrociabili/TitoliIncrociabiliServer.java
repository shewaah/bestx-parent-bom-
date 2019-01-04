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
package it.softsolutions.bestx.services.titoliincrociabili;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.dao.TitoliIncrociabiliDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.services.TitoliIncrociabiliService;

import java.sql.SQLException;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-cs First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class TitoliIncrociabiliServer implements TitoliIncrociabiliService {

    private TitoliIncrociabiliDao titoliIncrociabiliDao;

    /**
     * @param titoliIncrociabiliDao
     */
    public void setTitoliIncrociabiliDao(TitoliIncrociabiliDao titoliIncrociabiliDao) {
        this.titoliIncrociabiliDao = titoliIncrociabiliDao;
    }

    @Override
    public boolean isAMatch(Instrument instrument, Customer customer) throws BestXException {
        checkPreRequisites();
        try {
            return titoliIncrociabiliDao.isAMatch(instrument, customer);
        } catch (SQLException e) {
            throw new BestXException("A system error occurred while accessing DB: " + e.getMessage(), e);
        }
    }

    private void checkPreRequisites() {
        if (titoliIncrociabiliDao == null) {
            throw new ObjectNotInitializedException("DAO Titoli Incrociabili not set");
        }
    }

    @Override
    public boolean isAMatch(Order order) throws BestXException {
        checkPreRequisites();
        try {
            // TODO metterei qui una cache degli ordini incrociabili per evitare di fare una select per ogni proposal.
            // L'ordine, una volta che e' definito matchable la prima volta, lo rimane.
            return titoliIncrociabiliDao.isAMatch(order);
        } catch (SQLException e) {
            throw new BestXException("A system error occurred while accessing DB: " + e.getMessage(), e);
        }
    }

    @Override
    public void resetMatchingOperation(Operation operation) throws BestXException {
        checkPreRequisites();
        try {
            titoliIncrociabiliDao.resetMatchingOperation(operation);
        } catch (SQLException e) {
            throw new BestXException("A system error occurred while accessing DB: " + e.getMessage(), e);
        }
    }

    @Override
    public void setMatchingOperation(Operation operation) throws BestXException {
        checkPreRequisites();
        try {
            titoliIncrociabiliDao.setMatchingOperation(operation);
        } catch (SQLException e) {
            throw new BestXException("A system error occurred while accessing DB: " + e.getMessage(), e);
        }
    }
}
