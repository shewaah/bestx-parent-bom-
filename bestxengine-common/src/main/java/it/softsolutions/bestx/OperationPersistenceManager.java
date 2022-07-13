/**
 * Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.PersistenceException;
import it.softsolutions.bestx.exceptions.SaveBookException;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.states.ErrorState;

/**
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common
 * First created by: davide.rossoni
 * Creation date: 06/nov/2013
 * 
 */
public class OperationPersistenceManager implements OperationStateListener, Initializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationPersistenceManager.class);
    
    private static final String LOGSTRING = "Retrieved {} operations";

    private boolean keepTerminalOperation;
    private SessionFactory sessionFactory;
    private OperationFactory operationFactory;
    private SimpleOperationRestorer operationRestore;
    private JdbcTemplate jdbcTemplate;

    private static final String OPERATION_EXISTENCE_SQL = "SELECT count(*)" +
                    " FROM OPERATION op" +
                    " join OperationBinding bind on op.OperationId=bind.OperationId" +
                    " join OperationState st on op.OperationStateId=st.OperationStateId" +
                    " where bind.ExternalId = ?" +
                    " and   bind.ExternalIdType = ?" +
                    " and   st.EnteredTime >= ?" +
                    " and   st.EnteredTime <= ?";

    private static final String OPERATION_COUNT_SQL = "SELECT count(*)" +
                    " FROM OPERATION op" +
                    " join OperationState st on op.OperationStateId=st.OperationStateId" +
                    " where st.EnteredTime >= ?" +
                    " and   st.EnteredTime <= ?" +
                    " and   st.IsTerminal = 0";
    
    @SuppressWarnings("unused")
	private static final String OPERATION_TOTAL_COUNT_SQL = "SELECT count(*)" +
            " FROM OPERATION op" +
            " join OperationState st on op.OperationStateId=st.OperationStateId" +
            " where st.EnteredTime >= ?" +
            " and   st.EnteredTime <= ?";

    private static final String OPID_FROM_BINDING_SQL = "SELECT OperationId" +
                    " FROM OperationBinding" +
                    " where ExternalIdType = ?" +
                    " and   ExternalId = ?";
    
    /*private static final String opsForStateSql = "SELECT O.OperationId "
          + "FROM OperationState OS "
          + "INNER JOIN Operation O "
          + "ON O.OperationStateId = OS.OperationStateId "
          + "WHERE 1=1 "
          + "AND OS.EnteredTime BETWEEN :from AND :to "
          + "AND OS.StateClassName = :scn";*/
    
    private static final String OPS_FOR_STATE_SQL = "FROM Operation WHERE "
         + "currentState.stateClassName IN ( :scn ) AND "
         + "currentState.enteredTime > :from "
         + "AND currentState.enteredTime < :to";
    
    private Date operationExistenceStartDate;
    private Date operationExistenceEndDate;

    private Timer saveOperationTimer;
    private Timer onOperationStateChangedTimer;

    public void init() {
        checkPreRequisites();

        // filter dates are inited at startup, if Bestx is not restarted at start of day
        // the operations will not be found!
        Calendar tempCal;
        tempCal = Calendar.getInstance();
        tempCal.setTimeInMillis(DateService.currentTimeMillis());
        tempCal = DateUtils.truncate(tempCal, Calendar.DAY_OF_MONTH);
        operationExistenceStartDate = tempCal.getTime(); // today 00:00:00

        tempCal.set(Calendar.HOUR_OF_DAY, 23);
        tempCal.set(Calendar.MINUTE, 59);
        tempCal.set(Calendar.SECOND, 59);
        operationExistenceEndDate = tempCal.getTime(); // today 23:59:59
        
        try {
            saveOperationTimer = CommonMetricRegistry.INSTANCE.getMonitorRegistry().timer(MetricRegistry.name(OperationPersistenceManager.class, "saveOperation"));
            onOperationStateChangedTimer = CommonMetricRegistry.INSTANCE.getMonitorRegistry().timer(MetricRegistry.name(OperationPersistenceManager.class, "onOperationStateChanged"));
            
            final BasicDataSource dataSource = (BasicDataSource) jdbcTemplate.getDataSource();
            CommonMetricRegistry.INSTANCE.getMonitorRegistry().register(MetricRegistry.name(OperationPersistenceManager.class, "dbcp.NumActive"), new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return dataSource.getNumActive();
				}
			});
			
			CommonMetricRegistry.INSTANCE.getMonitorRegistry().register(MetricRegistry.name(OperationPersistenceManager.class, "dbcp.NumIdle"), new Gauge<Integer>() {
				@Override
				public Integer getValue() {
					return dataSource.getNumIdle();
				}
			});
            
        } catch (IllegalArgumentException e) {
        }
    }

    public void setKeepTerminalOperation(boolean keepTerminalOperation) {
        this.keepTerminalOperation = keepTerminalOperation;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    private final Map<String, Date> startSaveTimeInMillis = new ConcurrentHashMap<String, Date>();

    @Override
    public void onOperationStateChanged(Operation operation, OperationState oldState, OperationState newState) throws SaveBookException, BestXException {
//    	LOGGER.debug("operationID={}, oldState={}, newState={}", operation.getId(), oldState, newState);
        Session session = null;
        Transaction tx = null;
        
        if (operation.isVolatile()) {
        	return;
        }
        
		if (operation.getIdentifier(OperationIdType.ORDER_ID) != null) {
			startSaveTimeInMillis.put(operation.getIdentifier(OperationIdType.ORDER_ID), DateService.newLocalDate());
		}
		
		final Timer.Context context = onOperationStateChangedTimer.time();
        long start = DateService.currentTimeMillis();
		try {
            LOGGER.debug("[PERSISTENCE]-START onOperationStateChanged for operationID={}", operation.getId());

			session = sessionFactory.openSession();
			tx = session.beginTransaction();
			SortedBook sortedBook = operation.getLastAttempt() != null ? operation.getLastAttempt().getSortedBook() : null;
			if (newState.mustSaveBook() && sortedBook != null) {
				for (ClassifiedProposal prop : sortedBook.getAskProposals()) {
//					LOGGER.error("{}", prop);
					session.saveOrUpdate(prop);
//					LOGGER.error("done");
				}
				for (ClassifiedProposal prop : sortedBook.getBidProposals()) {
					session.saveOrUpdate(prop);
				}
				session.saveOrUpdate(sortedBook);
			}
			session.saveOrUpdate(operation);
			tx.commit();
		} catch (org.hibernate.StaleStateException hibex) {
		   if (tx != null) {
		      tx.rollback();
		   }
		   
		   if (operation.getState() instanceof ErrorState) {
		      operation.getState().setComment(Messages.getString("Warning.RestoredAfterKill", operation.getState().getClass().getSimpleName()));
		   }
		   operationRestore.killAndRestoreOperation(operation.getOrder().getFixOrderId(), Messages.getString("Warning.RestoredAfterKill", operation.getState().getClass().getSimpleName()));
		   LOGGER.warn("Order {}. Operation restored after Hibernate StaleStateException", operation.getOrder().getFixOrderId());
		} catch (RuntimeException e) {
//            LOGGER.error("An error occurred while saving operation state (operationID={}): {}", operation.getId(), e.getMessage(), e);
            if (tx != null) {
                tx.rollback();
            }

            // [DR20120629] Qui si deve propagare un'Exception in quanto occorre notificare un problema di salvataggio dei dati su DB.
            throw new BestXException(e.getMessage(), e); 

        } finally {
        	context.close();
            if (session != null) {
                session.close();
            }
        }
		long end = DateService.currentTimeMillis();
        LOGGER.debug("[PERSISTENCE],StoreTime={},STOP onOperationStateChanged, operationID={}", (end-start), operation.getId());
        
		if (newState.isTerminal() && !keepTerminalOperation) {
			LOGGER.info("Deleting operation with operationID={} from DB", operation.getId());
			try {
				session = sessionFactory.openSession();
				tx = session.beginTransaction();
				session.delete(operation);
				tx.commit();
			} catch (RuntimeException e) {
				LOGGER.error("An error occurred while deleting operation from database: {}", e.getMessage(), e);
				if (tx != null) {
					tx.rollback();
				}
			} finally {
				if (session != null) {
					session.close();
				}
			}
		}
    }

    /**
     * Calling this method we persist the operation. It could be useful when we receive
     * identifiers that will be needed in case of a restart, but that are not persisted
     * because the operation they belong to does not change its status.
     * We will need them at the restart, so it is mandatory to persist them as soon as they
     * arrive.
     * 
     * @param operation
     */
    public void saveOperation(Operation operation) {
    	if (operation.isVolatile()) return;
    	
        Session session = null;
        Transaction tx = null;
        
        final Timer.Context context = saveOperationTimer.time();
        long t0 = DateService.currentTimeMillis();
        try {
            LOGGER.info("[PERSISTENCE]-START saveOperationBindings for operationID={}", operation.getId());
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.saveOrUpdate(operation);
            tx.commit();
            
        } catch (org.hibernate.StaleStateException hibex) {
           if (tx != null) {
              tx.rollback();
           }
           operationRestore.killAndRestoreOperation(operation.getOrder().getFixOrderId(), Messages.getString("Warning.RestoredAfterKill", operation.getState().getClass().getName()));
           LOGGER.warn("Order {}. Operation restored after Hibernate StaleStateException", operation.getOrder().getFixOrderId());
        } catch (RuntimeException e) {
            LOGGER.error("An error occurred while saving operation bindings for operationID={} to database: {}", operation.getId(), e.getMessage(), e);
            if (tx != null) {
                tx.rollback();
            }
        } finally {
        	context.stop();
            if (session != null) {
                session.close();
            }
        }
        LOGGER.info("[PERSISTENCE]-STOP,StoreTime={}, saveOperationBindings for operationID={}", (System.currentTimeMillis() - t0), operation.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Operation> getSystemState() throws BestXException {
        List<Operation> operations = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();

            Calendar dateFrom = Calendar.getInstance();
            dateFrom.set(Calendar.HOUR_OF_DAY, 0);
            dateFrom.set(Calendar.MINUTE, 0);
            dateFrom.set(Calendar.SECOND, 0);

            Calendar dateTo = Calendar.getInstance();
            dateTo.set(Calendar.HOUR_OF_DAY, 23);
            dateTo.set(Calendar.MINUTE, 59);
            dateTo.set(Calendar.SECOND, 59);

            // load only operations in state not terminal
            String limitFileStates = 
                            "it.softsolutions.bestx.states.LimitFileNoPriceState, "
                            + "it.softsolutions.bestx.states.OrderNotExecutableState, "
                            + "it.softsolutions.bestx.states.CurandoRetryState, "
                            + "it.softsolutions.bestx.states.CurandoState, "
                            + "it.softsolutions.bestx.states.WarningState, "
//                            + "it.softsolutions.bestx.states.ParkedOrderState, "
                            + "it.softsolutions.bestx.states.ErrorState"
                            ;

            String queryStr = 
                            "FROM Operation WHERE "
                                            + "currentState.stateClassName NOT IN (" + limitFileStates + ") AND "
                                            + "currentState.enteredTime > :from "
                                            + "AND currentState.enteredTime < :to "
                                            + "AND IsTerminal = 0 "
                                            + "ORDER BY order.transactTime ASC";

            Query query = session.createQuery(queryStr);
            query.setTimestamp("from", new Timestamp(dateFrom.getTimeInMillis()));
            query.setTimestamp("to", new Timestamp(dateTo.getTimeInMillis()));
            operations = query.list();

            if (operations != null && !operations.isEmpty()) {
                for (Operation operation : operations) {
                	try {
                		operationFactory.initOperation(operation);
                	} catch(org.hibernate.HibernateException hibex) {
                		LOGGER.warn("Unable to retrieve operation for order {}, due to exception", operation.getOrder().getFixOrderId(), hibex);
                		// FIXME try to call the operation restore
                		
                	}
                }
            } else {
                LOGGER.warn("No operations retrieved");
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error while loading system state restoring the operations: {}", e.getMessage(), e);
            throw new PersistenceException("An error occurred while retrieving operations from database: " + e.getMessage(), e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
        LOGGER.info(LOGSTRING, (operations != null ? operations.size() : 0));
        return operations;
    }

    @SuppressWarnings("unchecked")
    public Operation getOperationForCurrentDate(long operationId) throws BestXException {
        List<Operation> operations = null;
        Session session = null;
        try {
            session = sessionFactory.openSession();

            Calendar dataDaCal = Calendar.getInstance();
            dataDaCal.set(Calendar.HOUR_OF_DAY, 0);
            dataDaCal.set(Calendar.MINUTE, 0);
            dataDaCal.set(Calendar.SECOND, 0);

            Calendar dataACal = Calendar.getInstance();
            dataACal.set(Calendar.HOUR_OF_DAY, 23);
            dataACal.set(Calendar.MINUTE, 59);
            dataACal.set(Calendar.SECOND, 59);

            Query query = session.createQuery("from Operation ops where id = :opId and currentState.enteredTime > :dataDa and currentState.enteredTime < :dataA order by order.transactTime asc");
            query.setInteger("opId", (int)operationId);
            query.setTimestamp("dataDa", new Timestamp(dataDaCal.getTimeInMillis()));
            query.setTimestamp("dataA", new Timestamp(dataACal.getTimeInMillis()));
            operations = query.list();

            int numOp = operations.size();
            if (numOp > 0) {
                if (numOp > 1) {
                    LOGGER.warn("Possible error, {} operations related to operationId {} in the same date", numOp, operationId);
                }
                Operation op = operations.get(0);
                operationFactory.initOperation(op);
                return op;
            } else {
                LOGGER.warn("No operations retrieved");
                return null;
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error while loading operation: {}", e.getMessage(), e);
            throw new PersistenceException("An error occurred while retrieving operation from database: " + e.getMessage(), e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public boolean operationExistsById(OperationIdType idType, String id) {
        int countRows = 0;
        LOGGER.debug("id {}, idType {}, startDate {}, endDate {}, query {}", id, idType, operationExistenceStartDate, operationExistenceEndDate, OPERATION_EXISTENCE_SQL);
        try {
            countRows = jdbcTemplate.queryForObject(OPERATION_EXISTENCE_SQL, Integer.class, id, idType.toString(), operationExistenceStartDate, operationExistenceEndDate);
        } catch (Exception e) {
            LOGGER.error("Error during check of existence of order: {}, assuming it is new", e.getMessage(), e);
            return false;
        }
        if (countRows >= 1) {
            return true;
        }
        else {
            return false;
        }
    }


    public int getNumberOfDailyOperations() {
        int countRows = 0;
        try {
           countRows = jdbcTemplate.queryForObject(OPERATION_COUNT_SQL, Integer.class, operationExistenceStartDate, operationExistenceEndDate);
        } catch (Exception e) {
            LOGGER.error("Error during count of daily orders: {}, assuming it is new", e.getMessage(), e);
            return 0;
        }

        return countRows;
    }
    
    // [BXMNT-321] Come richiesto da AMC utilizziamo la stessa query del getNumberOfDailyOperations ma includendo gli ordini in stato terminale
    public int getTotalNumberOfDailyOperations() {
        int countRows = 0;
        try {
           countRows = jdbcTemplate.queryForObject(OPERATION_COUNT_SQL, Integer.class, operationExistenceStartDate, operationExistenceEndDate);
        } catch (Exception e) {
            LOGGER.error("Error during count of daily orders: {}, assuming it is new", e.getMessage(), e);
            return 0;
        }

        return countRows;
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (sessionFactory == null) {
            throw new ObjectNotInitializedException("Session factory not set");
        }
        if (operationFactory == null) {
            throw new ObjectNotInitializedException("Operation factory not set");
        }
        if (jdbcTemplate == null) {
            throw new ObjectNotInitializedException("JDBC template not set");
        }
    }

    public void setOperationFactory(OperationFactory operationFactory) {
        this.operationFactory = operationFactory;
    }

    /**
     * Set the jdbcTemplate used to execute queries. 
     * @param jdbcTemplate
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long getOperationIdFromBinding(OperationIdType externalIdType, String externalId) {
        long operationId = 0;
        try {
            operationId = (long) jdbcTemplate.queryForObject(OPID_FROM_BINDING_SQL, Integer.class, externalIdType.toString(), externalId);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.info("No binding found for {}-{}", externalIdType, externalId);
        } catch (Exception e) {
            LOGGER.error("Error during decoding of binding {}-{} : {}", externalIdType, externalId, e.getMessage(), e);
        }
        
        return operationId;
    }
    
    /**
     * This method returns all the operations of current date in operationStateCanonicalNames states
     * @param operationStateCanonicalNames: a list of states
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<Operation> getActiveOperationForStates(List<String> operationStateCanonicalNames) {
       List<Operation> operations = null;
       Session session = null;
       
       /*String inClause   = "";
       String separator  = ", ";
       if (operationStateCanonicalNames != null && operationStateCanonicalNames.size() > 0) {
          for (String opStateCanonicalName:operationStateCanonicalNames)
             inClause += opStateCanonicalName + separator;
          inClause = inClause.substring(0, inClause.length() - separator.length());
       }*/
       
       try {
           session = sessionFactory.openSession();
           
           Calendar dateFrom = Calendar.getInstance();
           dateFrom.set(Calendar.HOUR_OF_DAY, 0);
           dateFrom.set(Calendar.MINUTE, 0);
           dateFrom.set(Calendar.SECOND, 0);
           dateFrom.set(Calendar.MILLISECOND, 0);

           Calendar dateTo = Calendar.getInstance();
           dateTo.set(Calendar.HOUR_OF_DAY, 23);
           dateTo.set(Calendar.MINUTE, 59);
           dateTo.set(Calendar.SECOND, 59);
           dateTo.set(Calendar.MILLISECOND, 999);

           Query query = session.createQuery(OPS_FOR_STATE_SQL);
           query.setTimestamp("from", new Timestamp(dateFrom.getTimeInMillis()));
           query.setTimestamp("to", new Timestamp(dateTo.getTimeInMillis()));
           query.setParameterList("scn", operationStateCanonicalNames);
           operations = query.list();

           for (Operation operation:operations) {
              operationFactory.initOperation(operation);
           }
           
           
       } catch (RuntimeException | BestXException e) {
           LOGGER.error("Error while getActiveOperationForState: {}", e.getMessage(), e);
       } finally {
           if (session != null) {
               session.close();
           }
       }
       LOGGER.info(LOGSTRING, (operations != null ? operations.size() : 0));
       return operations;
    }

	public Operation loadOperationById(String orderId) throws PersistenceException {
	      Operation operation = null;
	        Session session = null;
	        try {
	        	 List<Operation> operations = null;
	            session = sessionFactory.openSession();

	            String queryStr = "FROM Operation o WHERE o.order.fixOrderId = :orderId";

	            Query query = session.createQuery(queryStr);
	            query.setString("orderId", orderId);
	            operations = query.list();

	            if (operations != null && operations.size() > 0) {
	            	if (operations.size() > 1) {
	            		throw new PersistenceException("More than an record found on orderId " + orderId);
	            	}
	            	operation = operations.get(0);
	            	try {
						operationFactory.initOperation(operation);
					} catch (BestXException e) {

						
						// TODO
					}
	            	
	    	        LOGGER.info(LOGSTRING, (operations != null ? operations.size() : 0));

	            } else {
	                LOGGER.warn("No operations retrieved");
	            }
	        } catch (RuntimeException e) {
	            LOGGER.error("Error while loading system state restoring the operations: {}", e.getMessage(), e);
	            throw new PersistenceException("An error occurred while retrieving operations from database: " + e.getMessage(), e);
	        } finally {
	            if (session != null) {
	                session.close();
	            }
	        }

	        return operation;		
	}

   
   public void setOperationRestore(SimpleOperationRestorer operationRestore) {
      this.operationRestore = operationRestore;
   }
}
