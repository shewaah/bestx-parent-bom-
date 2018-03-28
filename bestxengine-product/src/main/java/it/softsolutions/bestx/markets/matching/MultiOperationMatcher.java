/**
 * 
 */
package it.softsolutions.bestx.markets.matching;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.dao.TitoliIncrociabiliDao;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author otc-go
 * 
 */
public class MultiOperationMatcher implements OperationMatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiOperationMatcher.class);
    private TitoliIncrociabiliDao titoliIncrociabiliDao;
    private OperationRegistry operationRegistry;

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (titoliIncrociabiliDao == null) {
            throw new ObjectNotInitializedException("Titoli Incrociabili Dao not set");
        }
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
    }

    @Override
    public boolean operationsMatch(Operation operationA, Operation operationB) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean operationsMatch(Operation operation) {
        checkPreRequisites();
        try {
            return titoliIncrociabiliDao.allOrdersArrives(titoliIncrociabiliDao.getMatchId(operation));
        } catch (SQLException e) {
            LOGGER.error("Error checking matching orders arrivals", e);
            return false;
        }
    }

    @Override
    public List<Operation> getOperationsMatched(Operation operation) {
        List<Operation> operations = new ArrayList<Operation>();
        checkPreRequisites();
        try {
            List<String> list = titoliIncrociabiliDao.getMatchOrdersList(titoliIncrociabiliDao.getMatchId(operation));
            for (String orderId : list) {
                operations.add(operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId));
            }
        } catch (SQLException e) {
            LOGGER.error("Error checking matching orders arrivals", e);
            return null;
        } catch (OperationNotExistingException e) {
            LOGGER.error("Matched operation not exist", e);
            return null;
        } catch (BestXException e) {
            LOGGER.error("Generic exception while retrieve matched operation", e);
            return null;
        }
        return operations;
    }

    public TitoliIncrociabiliDao getTitoliIncrociabiliDao() {
        return titoliIncrociabiliDao;
    }

    public void setTitoliIncrociabiliDao(TitoliIncrociabiliDao titoliIncrociabiliDao) {
        this.titoliIncrociabiliDao = titoliIncrociabiliDao;
    }

    public OperationRegistry getOperationRegistry() {
        return operationRegistry;
    }

    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }
}
