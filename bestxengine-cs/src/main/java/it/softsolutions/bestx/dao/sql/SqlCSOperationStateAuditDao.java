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
package it.softsolutions.bestx.dao.sql;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.Transactional;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.dao.OperationStateAuditDao;
import it.softsolutions.bestx.exceptions.SaveBookException;
import it.softsolutions.bestx.model.Attempt;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.ExecutablePrice;
import it.softsolutions.bestx.model.ExecutionReport;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.SortedBook;
import it.softsolutions.bestx.model.TradeFill;
import it.softsolutions.bestx.services.CSConfigurationPropertyLoader;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.states.CurandoState;
import it.softsolutions.bestx.states.LimitFileNoPriceState;
import it.softsolutions.bestx.states.OrderNotExecutableState;
import it.softsolutions.jsscommon.Money;

/**
 * 
 *
 * Purpose: Audit management
 *
 * Project Name : bestxengine-cs First created by: ruggero.rizzo Creation date: 19/ott/2012
 *
 **/
public class SqlCSOperationStateAuditDao implements OperationStateAuditDao {
    private static final String dateTimeForDb = "yyyyMMdd-HH:mm:ss.SSS";
	private static final Logger LOGGER = LoggerFactory.getLogger(SqlCSOperationStateAuditDao.class);
    private JdbcTemplate jdbcTemplate;
    private int counter;
    private String strDate;
    private boolean isFirst = true;
    public static final String USANDGLOBAL_MARKETOFISSUE_CONFIG_PROPERTY = "UsAndGlobal.marketsOfIssue";
    public static final String USANDGLOBAL_ISINCODEINITIAL_CONFIG_PROPERTY = "UsAndGlobal.isinCodeInitial";
    
    // Map<orderID, semaphore>
    private static ConcurrentMap<String, Semaphore> semaphores = new ConcurrentHashMap<String, Semaphore>();
    
    private static AtomicLong lastSaveTimeMillis = new AtomicLong();
    private SimpleJdbcCall jdbcCallSaveNewAttempt = null;
    private SimpleJdbcCall jdbcCallSaveMarketAttemptStatus = null;
    
    private DefaultTransactionDefinition def = null;
    
    private PlatformTransactionManager transactionManager;
    
    private org.springframework.jdbc.datasource.DataSourceTransactionManager dataSourceTransactionManager;
    

    /**
     * Set JdbcTemplate
     * 
     * @param jdbcTemplate
     */
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * init method
     */
    public void init() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        strDate = "convert(datetime, '" + DateFormatUtils.format(c.getTime(), "yyyy-MM-dd HH:mm:ss.SSS") + "', 121)";

        String sql = "SELECT receivedOrders FROM WorkingDates" + " WHERE workingDate = " + strDate;
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
        if (rowSet.next()) {
            counter = rowSet.getInt("receivedOrders");
            isFirst = false;
        } else {
            counter = 0;
            isFirst = true;
        }
    	jdbcCallSaveNewAttempt = new SimpleJdbcCall(jdbcTemplate)
			.withSchemaName("dbo")
				.withProcedureName("saveNewAttempt");
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@NUM_ORDINE", java.sql.Types.VARCHAR));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@ATTEMPT", java.sql.Types.INTEGER));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TSN", java.sql.Types.INTEGER));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@MARKETID", java.sql.Types.INTEGER));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@PREZZO_MEDIO", java.sql.Types.DECIMAL));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TRADER", java.sql.Types.VARCHAR));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TICKET_NUM", java.sql.Types.VARCHAR));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_PRICE", java.sql.Types.DECIMAL));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_SPREAD", java.sql.Types.BIGINT));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@BEST_BANK_CODE", java.sql.Types.VARCHAR));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@COUNTER_PRICE", java.sql.Types.DECIMAL));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_ATTEMPT", java.sql.Types.INTEGER));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_FAKE", java.sql.Types.BIT));
		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@REFERENCE_PRICE_BANK_CODE", java.sql.Types.VARCHAR));
    	
		jdbcCallSaveMarketAttemptStatus = new SimpleJdbcCall(jdbcTemplate)
			.withSchemaName("dbo")
				.withProcedureName("saveMarketAttemptStatus");
    	jdbcCallSaveMarketAttemptStatus.addDeclaredParameter(new SqlParameter("@NUM_ORDINE", java.sql.Types.VARCHAR));
    	jdbcCallSaveMarketAttemptStatus.addDeclaredParameter(new SqlParameter("@ATTEMPT", java.sql.Types.INTEGER));
    	jdbcCallSaveMarketAttemptStatus.addDeclaredParameter(new SqlParameter("@MARKETCODE", java.sql.Types.VARCHAR));
    	jdbcCallSaveMarketAttemptStatus.addDeclaredParameter(new SqlParameter("@DISABLED", java.sql.Types.TINYINT));
    	jdbcCallSaveMarketAttemptStatus.addDeclaredParameter(new SqlParameter("@DOWN_CAUSE", java.sql.Types.VARCHAR));
    	def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    	def.setName(this.getClass().getSimpleName()+"_TX");
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#saveMarketAttemptStatus(java.lang.String, int,
     * it.softsolutions.bestx.model.Market.MarketCode, boolean, java.lang.String)
     */
    @Override
    public void saveMarketAttemptStatus(final String orderId, final int attemptNo, final MarketCode marketCode, final boolean disabled, final String disabledComment) {
        // [RR20131018] BXMNT-373: with the internalization flow could happen that we must save the market status at an
        // earlier stage
        // and then re-save it in the normal order flow, thus we must go for a MERGE in order to let the database decide
        // if it is
        // and INSERT or an UPDATE
        LOGGER.debug("Start saveMarketAttemptStatus - SQL[{}]");
        long t0 = DateService.currentTimeMillis();
        Map<String, Object> parameters = new ConcurrentHashMap<String, Object>();
        parameters.put("@NUM_ORDINE", orderId);
        parameters.put("@ATTEMPT", attemptNo);
        parameters.put("@MARKETCODE", marketCode.name());
        parameters.put("@DISABLED", disabled?1:0);
        parameters.put("@DOWN_CAUSE", disabledComment != null ? disabledComment : "");
        TransactionStatus status = this.transactionManager.getTransaction(def);
         try {
        	 jdbcCallSaveMarketAttemptStatus.execute(parameters);
        	 this.transactionManager.commit(status);
        } catch(Exception e) {
        	 LOGGER.info("Exception {} got when trying to saveMarketAttemptStatus - If status is Curando or SendNotExecutionReportState this could be OK", e.getMessage());
        	 this.transactionManager.rollback(status);
        }
        LOGGER.info("[AUDIT],StoreTime={},Stop saveMarketAttemptStatus", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#saveNewState(java.lang.String,
     * it.softsolutions.bestx.OperationState, it.softsolutions.bestx.OperationState, int, java.lang.String)
     */
    @Transactional
    @Override
    public void saveNewState(final String orderId, final OperationState previousState, final OperationState currentState, final int attemptNo, final String comment) {
        final String sql = "INSERT INTO TabHistoryStati (" + "NumOrdine," + // 1
                        " SaveTime," + // 2
                        " Stato," + // 3
                        " StatoPrecedente," + // 4
                        " DescrizioneEvento," + // 5
                        " IdEvento," + // 6
                        " Attempt," + // 7
                        " UserVisible," + // 8
                        " CustomerVisible" + // 9
                        ") VALUES (?,?,?,?,?,?,?,?,?)";
        LOGGER.debug("Start saveNewState to TabHistoryStati - orderId={}, previousState={}, currentState={}, attemptNo={}, comment={}", orderId, previousState, currentState, attemptNo, comment);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // "NumOrdine," + // 1
                stmt.setString(1, orderId);
                // " SaveTime," + // 2
                if (currentState != null && currentState.getEnteredTime() != null) {
                    stmt.setTimestamp(2, new java.sql.Timestamp(currentState.getEnteredTime().getTime()));
                } else {
                    LOGGER.error("Order {}, the current state entered time is null, the database field SaveTime cannot be null! Setting now as the SaveTime.", orderId);
                    Date today = DateService.newLocalDate();
                    stmt.setTimestamp(2, new java.sql.Timestamp(today.getTime()));
                }
                // " Stato," + // 3
                if(currentState != null)
                	stmt.setString(3, currentState.getClass().getSimpleName());
                // " StatoPrecedente," + // 4
                if (previousState != null) {
                    stmt.setString(4, previousState.getClass().getSimpleName());
                } else {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                }
                // " DescrizioneEvento," + // 5
                if (comment != null) {
                    stmt.setString(5, comment);
                } else {
                    stmt.setNull(5, java.sql.Types.VARCHAR);
                }
                // " IdEvento," + // 6
                stmt.setNull(6, java.sql.Types.INTEGER);
                // " Attempt," + // 7
                stmt.setInt(7, attemptNo);
                // " UserVisible," + // 8
                stmt.setBoolean(8, true);
                // " CustomerVisible" + // 9
                stmt.setBoolean(9, true);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop saveNewState", (DateService.currentTimeMillis() - t0));
        //[RR20140910] CRSBXTEM-119 save the last price discovery starting time
        switch (currentState.getType()) {
            case CurandoRetry:
            case OrderNotExecutable:
            case LimitFileNoPrice:
            case WaitingPrice:
                t0 = DateService.currentTimeMillis();
                LOGGER.info("[AUDIT],UpdateTime={},Start update TabHistoryOrdini/LastUpdateTime", (DateService.currentTimeMillis() - t0));
                String update = "UPDATE TabHistoryOrdini SET LastUpdateTime=? WHERE NumOrdine=?";
                status = this.transactionManager.getTransaction(def);
                jdbcTemplate.update(update, new PreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement stmt) throws SQLException {
                        if (currentState != null && currentState.getEnteredTime() != null) {
                            stmt.setTimestamp(1, new java.sql.Timestamp(currentState.getEnteredTime().getTime()));
                        } else {
                            LOGGER.error("Order {}, the current state entered time is null, the database field LastUpdateTime cannot be null! Setting now as the LastUpdateTime.", orderId);
                            Date today = DateService.newLocalDate();
                            stmt.setTimestamp(1, new java.sql.Timestamp(today.getTime()));
                        }
                        stmt.setString(2, orderId);
                    }
                });
                this.transactionManager.commit(status);
                LOGGER.info("[AUDIT],UpdateTime={},Stop update TabHistoryOrdini/LastUpdateTime", (DateService.currentTimeMillis() - t0));
                break;
            default:
                break;
        }
    }

    @Override
    public void saveNewBook(final String orderId, final int attemptNo, final SortedBook sortedBook) throws SaveBookException {
        // LOGGER.debug("[{}] {}", orderId, sortedBook);
        LOGGER.debug("Start saveNewBook, order {}, attempt num {}, sortedBook is {}", orderId, attemptNo, (sortedBook == null ? "null" : "not null"));

        long t0 = DateService.currentTimeMillis();
        int count = -1;
        try {
            List<ClassifiedProposal> proposalToBeSaved = new ArrayList<ClassifiedProposal>();
            if(sortedBook != null && sortedBook.getAskProposals() != null && sortedBook.getBidProposals() != null){
	            for (int i = 0; i < sortedBook.getAskProposals().size(); i++) {
	                // assume that they have the same depth
	                count = i;
	                ClassifiedProposal pAsk = sortedBook.getAskProposals().get(i);
	                ClassifiedProposal pBid = sortedBook.getBidProposals().get(i);
	                if (BigDecimal.ZERO.compareTo(pAsk.getQty()) <= 0 || BigDecimal.ZERO.compareTo(pAsk.getPrice().getAmount()) <= 0 || BigDecimal.ZERO.compareTo(pBid.getQty()) <= 0
	                                || BigDecimal.ZERO.compareTo(pBid.getPrice().getAmount()) <= 0) {
	
	                    proposalToBeSaved.add(pAsk);
	                    proposalToBeSaved.add(pBid);
	                }
	            }
	            saveProposals(orderId, attemptNo, sortedBook.getInstrument(), proposalToBeSaved);
            }	
	        long sTime = (DateService.currentTimeMillis() - t0);
            // [CRSBXTEM-160] Modifica della velocità di scodamento degli ordini LF
            SqlCSOperationStateAuditDao.lastSaveTimeMillis.set(sTime);          
            LOGGER.info("[AUDIT],StoreTime={},Stop saveNewBook for {} proposals, orderId = {}", sTime, (sortedBook != null && sortedBook.getAskProposals() != null)? sortedBook.getAskProposals().size() : 0, orderId);
        } catch (Exception e) {
            LOGGER.error("{}", e.getMessage(), e);
            throw new SaveBookException(Messages.getString("SAVEBOOKERROR_MESSAGE"), orderId, attemptNo, sortedBook, count, e);
        }
    }
    
    /**
	 * @return the lastSaveTimeMillis
	 */
	public static AtomicLong getLastSaveTimeMillis() {
		return lastSaveTimeMillis;
	}

	private static final String sqlInsertIntoPriceTable = "INSERT INTO PriceTable (NumOrdine, Attempt, MarketId, BankCode, Side, Isin, Price, Qty, ArrivalTime, "
    		+ "FinalPrice, CustomerSpread, FlagScartato, Note, MarketBankCode, MarketMakerMarkup, MarketMakerClassId, AppliedTolerance, SeqOrder, "
    		+ "commissioniTick, PrezzoTelQuel, Bolli, ControvaloreConBollo, AuditQuoteState, QuoteType) " +
    		"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    private void saveProposals(final String orderId, final int attemptNo, final Instrument instrument, final List<ClassifiedProposal> classifiedProposals) throws InterruptedException {            
        
	   	Semaphore semaphore = null;
        if (semaphores.containsKey(orderId)) {
            semaphore = semaphores.get(orderId);
        } else {
            semaphore = new Semaphore(1);
            semaphores.put(orderId, semaphore);
        }

        try {
            // lock the orderID-related resource
            semaphore.acquire();
                     
            long t0 = DateService.currentTimeMillis();
            TransactionStatus status = this.transactionManager.getTransaction(def);
	        jdbcTemplate.batchUpdate(sqlInsertIntoPriceTable, new BatchPreparedStatementSetter() {
	            @Override
	            public void setValues(PreparedStatement stmt, int j) throws SQLException {
	                ClassifiedProposal proposal = classifiedProposals.get(j);
	                LOGGER.trace("saveProposal: orderId {}, attemptNo {}, proposal {} ", orderId, attemptNo, proposal);
	                stmt.setString(1, orderId);
	                LOGGER.trace("param1 orderId {}", orderId);
	                stmt.setInt(2, attemptNo);
	                LOGGER.trace("param2 attemptNo {}", attemptNo);
	                if (proposal != null && proposal.getMarket() != null && proposal.getMarket().getMarketId() != null) {
	                    stmt.setInt(3, proposal.getMarket().getMarketId().intValue());
	                   LOGGER.trace("param3 MarketId {}", proposal.getMarket().getMarketId().intValue());
	                } else {
	                    stmt.setInt(3, -1); // Primary key can't be null
	                   LOGGER.trace("param3 MarketId {}", -1);
	                }
	                if (proposal != null && proposal.getVenue() != null && proposal.getVenue().getCode() != null) {
	                    stmt.setString(4, proposal.getVenue().getCode());
	                   LOGGER.trace("param4 BankCode {}", proposal.getVenue().getCode());
	                } else {
	                    stmt.setNull(4, java.sql.Types.VARCHAR);
	                   LOGGER.trace("param4 BankCode null");
	                }
	                if(proposal != null && proposal.getSide() != null) {
	                   stmt.setString(5, proposal.getSide().getFixCode());
	                   LOGGER.trace("param5 Side {}", proposal.getSide().getFixCode());
	                } else {
	                    stmt.setString(5, "1");
	                	LOGGER.error("param5: Side not found in proposal {}", (proposal != null) ? proposal.toString(): "null");
	                }
	                if (instrument != null) {
	                    stmt.setString(6, instrument.getIsin());
	                   LOGGER.trace("param6 isin {}", instrument.getIsin());
	                } else {
	                    stmt.setNull(6, java.sql.Types.VARCHAR);
	                   LOGGER.trace("param6 isin null");
	                }
	                
	                if (proposal != null && proposal.getPrice() != null && proposal.getPrice().getAmount() != null) {
	                    stmt.setBigDecimal(7, proposal.getPrice().getAmount());
	                   LOGGER.trace("param7 price {}", proposal.getPrice().getAmount());
	                } else {
	                    stmt.setNull(7, java.sql.Types.DECIMAL);
	                   LOGGER.trace("param7 price null");
	                }
	                if (proposal != null && proposal.getQty() != null) {
	                    stmt.setBigDecimal(8, proposal.getQty());
	                   LOGGER.trace("param8 qty {}", proposal.getQty().floatValue());
	                } else {
	                    stmt.setNull(8, java.sql.Types.FLOAT);
	                   LOGGER.trace("param8 qty null");
	                }
	                if (proposal != null && proposal.getTimestamp() != null) {
	                    stmt.setTimestamp(9, new java.sql.Timestamp(proposal.getTimestamp().getTime()));
	                   LOGGER.trace("param9 arrivalTime {}", new java.sql.Timestamp(proposal.getTimestamp().getTime()));
	                } else {
	                    stmt.setNull(9, java.sql.Types.TIMESTAMP);
	                   LOGGER.trace("param9 arrivalTime null");
	                }
	                if (proposal != null && proposal.getPrice() != null && proposal.getPrice().getAmount() != null) {
	                    stmt.setBigDecimal(10, proposal.getPrice().getAmount());
	                   LOGGER.trace("param10 FinalPrice {}", proposal.getPrice().getAmount().floatValue());
	                } else {
	                    stmt.setNull(10, java.sql.Types.FLOAT);
	                   LOGGER.trace("param10 FinalPrice null");
	                }
	                stmt.setFloat(11, 0f);
	               LOGGER.trace("param11 CustomerSpread 0");
	                if (proposal != null && proposal.getProposalState() == Proposal.ProposalState.DROPPED) {
	                    stmt.setInt(12, 2);
	                   LOGGER.trace("param12 FlagScartato 2");
	                } else if (proposal != null && proposal.getProposalState() == Proposal.ProposalState.REJECTED) {
	                    stmt.setInt(12, 1);
	                   LOGGER.trace("param12 FlagScartato 1");
	                } else {
	                    stmt.setInt(12, 0);
	                   LOGGER.trace("param12 FlagScartato 0");
	                }
	                if (proposal != null && proposal.getReason() != null) {
	                    stmt.setString(13, proposal.getReason());
	                   LOGGER.trace("param13 Note {}", proposal.getReason());
	                } else {
	                    stmt.setNull(13, java.sql.Types.VARCHAR);
	                   LOGGER.trace("param13 Note null");
	                }
	                if (proposal != null && proposal.getMarketMarketMaker() != null) {
	                    stmt.setString(14, proposal.getMarketMarketMaker().getMarketSpecificCode());
	                   LOGGER.trace("param14 MarketBankCode {}", proposal.getMarketMarketMaker().getMarketSpecificCode());
	                } else {
	                    stmt.setString(14, ""); // Primary key can't be null
	                   LOGGER.trace("param14 MarketBankCode empty");
	                }
	                stmt.setInt(15, 0);
	               LOGGER.trace("param15 MarketMakerMarkup 0");
	                stmt.setNull(16, java.sql.Types.INTEGER);
	               LOGGER.trace("param16 MarketMakerClassId null");
	                stmt.setBigDecimal(17, BigDecimal.ZERO.setScale(5));
	               LOGGER.trace("param17 AppliedTolerance 0");
	                stmt.setInt(18, j);
	               LOGGER.trace("param18 SeqOrder {}", j);
	                if (proposal != null && proposal.getCommissionType() == CommissionType.TICKER) {
	                    stmt.setBoolean(19, true);
	                   LOGGER.trace("param19 commissioniTick true");
	                } else {
	                    stmt.setBoolean(19, false);
	                   LOGGER.trace("param19 commissioniTick false");
	                }
	                if (proposal != null && proposal.getPriceTelQuel() != null) {
	                    stmt.setBigDecimal(20, proposal.getPriceTelQuel().getAmount());
	                   LOGGER.trace("param20 PrezzoTelQuel {}", proposal.getPriceTelQuel().getAmount());
	                } else {
	                    stmt.setNull(20, java.sql.Types.DECIMAL);
	                   LOGGER.trace("param20 PrezzoTelQuel null");
	                }
	                stmt.setBigDecimal(21, BigDecimal.ZERO);
	               LOGGER.trace("param21 Bolli 0");
	                if (proposal != null && proposal.getPrice() != null && proposal.getPrice().getAmount() != null) {
	                    stmt.setBigDecimal(22, proposal.getPrice().getAmount());
	                   LOGGER.trace("param22 ControvaloreConBollo {}", proposal.getPrice().getAmount());
	                } else {
	                    stmt.setNull(22, java.sql.Types.DECIMAL);
	                   LOGGER.trace("param22 ControvaloreConBollo null");
	                }
					if (proposal != null && proposal.getAuditQuoteState() != null) {
						stmt.setString(23, proposal.getAuditQuoteState());
						LOGGER.trace("param23 AuditQuoteState {}", proposal.getAuditQuoteState());
					} else {
						stmt.setNull(23, java.sql.Types.VARCHAR);
						LOGGER.trace("param23 AuditQuoteState null");
					}
					if (proposal != null && proposal.getType() != null) {
						switch (proposal.getType()) {
							case INDICATIVE:
								stmt.setString(24, "IND");
								LOGGER.trace("param24 QuoteType IND");
								break;
							case COMPOSITE:
								stmt.setString(24, "CMP");
								LOGGER.trace("param24 QuoteType CMP");
								break;
							case TRADEABLE:
								stmt.setString(24, "FRM");
								LOGGER.trace("param24 QuoteType FRM");
								break;
							case AXE:
								stmt.setString(24, "AXE");
								LOGGER.trace("param24 QuoteType AXE");
								break;
							default:
								stmt.setNull(24, java.sql.Types.VARCHAR);
								LOGGER.trace("param24 QuoteType null");
						}
					} else {
						stmt.setNull(24, java.sql.Types.VARCHAR);
						LOGGER.trace("param24 QuoteType null");
					}					
	            }
	
	            @Override
	            public int getBatchSize() {
	                return classifiedProposals.size();
	            }
	        });
	        this.transactionManager.commit(status);
	        long sTime = (DateService.currentTimeMillis() - t0);
	        
	        // [CRSBXTEM-160] Modifica della velocità di scodamento degli ordini LF
	        SqlCSOperationStateAuditDao.lastSaveTimeMillis.set(sTime);
	        
            LOGGER.info("[AUDIT],StoreTime={},Stop saveProposals for {} proposals, orderId = {}", sTime, classifiedProposals.size(), orderId);	        
	        
        } finally {
            // release the orderID-related resource
            semaphore.release();
        }
    }

    
    @Override
    public void saveExecutablePrices(final String orderId, final int attemptNo, final String isin, final List<ExecutablePrice> executablePrices) throws Exception {
        LOGGER.debug("Start saveExecutablePrices, order {}, attempt num {}, executablePrices is {}", orderId, attemptNo, (executablePrices == null ? "null" : "not null"));
        long t0 = DateService.currentTimeMillis();
        try {
            if (!executablePrices.isEmpty()) {
            	this.saveInPriceTableExecutable(orderId, attemptNo, isin, executablePrices);	
            }  
	        long sTime = (DateService.currentTimeMillis() - t0);
            // [CRSBXTEM-160] Modifica della velocità di scodamento degli ordini LF
            SqlCSOperationStateAuditDao.lastSaveTimeMillis.set(sTime);          
            LOGGER.info("[AUDIT],StoreTime={},Stop saveExecutablePrices for {} proposals, orderId = {}", sTime, executablePrices != null ? executablePrices.size() : 0, orderId);
        } catch (Exception e) {
            LOGGER.error("{}", e.getMessage(), e);
            throw e;
        }
    }    

    
	private static final String SQL_INSERT_INTO_PRICETABLEEXECUTABLE = "INSERT INTO PriceTableExecutable "
			+ "(NumOrdine, Attempt, MarketId, BankCode, Side, Isin, Price, Qty, "
			+ "ArrivalTime, FlagScartato, Note, MarketBankCode, SeqOrder, AuditQuoteState, QuoteType) "
			+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    
    
	private void saveInPriceTableExecutable(final String orderId, final int attemptNo, final String isin,
			final List<ExecutablePrice> executablePrices) throws InterruptedException {

		Semaphore semaphore = null;
		if (semaphores.containsKey(orderId)) {
			semaphore = semaphores.get(orderId);
		} else {
			semaphore = new Semaphore(1);
			semaphores.put(orderId, semaphore);
		}

		try {
			// lock the orderID-related resource
			semaphore.acquire();

			long t0 = DateService.currentTimeMillis();
			TransactionStatus status = this.transactionManager.getTransaction(def);
			jdbcTemplate.batchUpdate(SQL_INSERT_INTO_PRICETABLEEXECUTABLE, new BatchPreparedStatementSetter() {
				@Override
				public void setValues(PreparedStatement stmt, int j) throws SQLException {
					ExecutablePrice executablePrice = executablePrices.get(j);
					ClassifiedProposal proposal = executablePrice.getClassifiedProposal();
					LOGGER.trace("saveInPriceTableExecutable: orderId {}, attemptNo {}, proposal {} ", orderId,
							attemptNo, proposal);
					stmt.setString(1, orderId);
					LOGGER.trace("param1 orderId {}", orderId);
					stmt.setInt(2, attemptNo);
					LOGGER.trace("param2 attemptNo {}", attemptNo);
					if (proposal != null && proposal.getMarket() != null
							&& proposal.getMarket().getMarketId() != null) {
						stmt.setInt(3, proposal.getMarket().getMarketId().intValue());
						LOGGER.trace("param3 MarketId {}", proposal.getMarket().getMarketId().intValue());
					} else {
						stmt.setInt(3, -1); // Primary key can't be null
						LOGGER.trace("param3 MarketId {}", -1);
					}
					if (proposal != null && proposal.getMarketMarketMaker() != null
						&& proposal.getMarketMarketMaker().getMarketMaker() != null
						&& proposal.getMarketMarketMaker().getMarketMaker().getCode() != null) {
						stmt.setString(4, proposal.getMarketMarketMaker().getMarketMaker().getCode());
						LOGGER.trace("param4 BankCode {}", proposal.getMarketMarketMaker().getMarketMaker().getCode());
					} else {
						stmt.setNull(4, java.sql.Types.VARCHAR);
						LOGGER.trace("param4 BankCode null");
					}
					if (proposal != null && proposal.getSide() != null) {
						stmt.setString(5, proposal.getSide().getFixCode());
						LOGGER.trace("param5 Side {}", proposal.getSide().getFixCode());
					} else {
						stmt.setString(5, "1");
						LOGGER.error("param5: Side not found in proposal {}",
								(proposal != null) ? proposal.toString() : "null");
					}
					if (isin != null) {
						stmt.setString(6, isin);
						LOGGER.trace("param6 isin {}", isin);
					} else {
						stmt.setNull(6, java.sql.Types.VARCHAR);
						LOGGER.trace("param6 isin null");
					}

					if (proposal != null && proposal.getPrice() != null && proposal.getPrice().getAmount() != null) {
						stmt.setBigDecimal(7, proposal.getPrice().getAmount());
						LOGGER.trace("param7 price {}", proposal.getPrice().getAmount());
					} else {
						stmt.setNull(7, java.sql.Types.DECIMAL);
						LOGGER.trace("param7 price null");
					}
					if (proposal != null && proposal.getQty() != null) {
						stmt.setBigDecimal(8, proposal.getQty());
						LOGGER.trace("param8 qty {}", proposal.getQty().floatValue());
					} else {
						stmt.setNull(8, java.sql.Types.FLOAT);
						LOGGER.trace("param8 qty null");
					}
					if (proposal != null && proposal.getTimestamp() != null) {
						stmt.setTimestamp(9, new java.sql.Timestamp(proposal.getTimestamp().getTime()));
						LOGGER.trace("param9 arrivalTime {}",
								new java.sql.Timestamp(proposal.getTimestamp().getTime()));
					} else {
						stmt.setNull(9, java.sql.Types.TIMESTAMP);
						LOGGER.trace("param9 arrivalTime null");
					}
					if (proposal != null && proposal.getProposalState() == Proposal.ProposalState.DROPPED) {
						stmt.setInt(10, 2);
						LOGGER.trace("param10 FlagScartato 2");
					} else if (proposal != null && proposal.getProposalState() == Proposal.ProposalState.REJECTED) {
						stmt.setInt(10, 1);
						LOGGER.trace("param10 FlagScartato 1");
					} else {
						stmt.setInt(10, 0);
						LOGGER.trace("param10 FlagScartato 0");
					}
					if (proposal != null && proposal.getReason() != null) {
						stmt.setString(11, proposal.getReason());
						LOGGER.trace("param11 Note {}", proposal.getReason());
					} else {
						stmt.setNull(11, java.sql.Types.VARCHAR);
						LOGGER.trace("param11 Note null");
					}
					if (proposal != null && proposal.getMarketMarketMaker() != null) {
						stmt.setString(12, proposal.getMarketMarketMaker().getMarketSpecificCode());
						LOGGER.trace("param12 MarketBankCode {}",
								proposal.getMarketMarketMaker().getMarketSpecificCode());
					} else {
						stmt.setString(12, ""); // Primary key can't be null
						LOGGER.trace("param12 MarketBankCode empty");
					}
					stmt.setInt(13, j);
					LOGGER.trace("param13 SeqOrder {}", j);
					if (executablePrice != null && executablePrice.getAuditQuoteState() != null) {
						stmt.setString(14, executablePrice.getAuditQuoteState());
						LOGGER.trace("param14 AuditQuoteState {}", executablePrice.getAuditQuoteState());
					} else {
						stmt.setString(14, ""); // Primary key can't be null
						LOGGER.trace("param14 AuditQuoteState empty");
					}
					stmt.setString(15, "FRM");
					LOGGER.trace("param15 SeqOrder {}", "FRM");
				}

				@Override
				public int getBatchSize() {
					return executablePrices.size();
				}
			});
			this.transactionManager.commit(status);
			long sTime = (DateService.currentTimeMillis() - t0);

			// [CRSBXTEM-160] Modifica della velocità di scodamento degli ordini LF
			SqlCSOperationStateAuditDao.lastSaveTimeMillis.set(sTime);

			LOGGER.info("[AUDIT],StoreTime={},Stop saveInPriceTableExecutable for {} proposals, orderId = {}", sTime,
					executablePrices.size(), orderId);

		} finally {
			// release the orderID-related resource
			semaphore.release();
		}
	}

    
    @Override
    public int saveNewAttempt(final String orderId, final Attempt attempt, final String tsn, final int attemptNo, final String ticketNum, int lastSavedAttempt) {
        // [RR20131018] BXMNT-373: with the internalization flow could happen that we must save the attempt at an
        // earlier stage and then re-save it in the normal order flow, thus we must go for a MERGE in order to let 
        // the database decide if it is an INSERT or an UPDATE
    	if(lastSavedAttempt < attemptNo) {
	        long t0 = DateService.currentTimeMillis();
	        LOGGER.debug("OrderID={}, saving attempt #{} - Attempt for the order {} - tsn={}, ticketNum={}", orderId, attemptNo, (attempt != null && attempt.getMarketOrder() != null ? attempt
	                        .getMarketOrder().toString() : null), tsn, ticketNum);
	        try{
	        
	        //prepare conditional params
	        Double prezzoMedio = null;
	        Long marketId = null;
	        if (attempt != null && attempt.getMarketOrder() != null && attempt.getMarketOrder().getMarket() != null)
	        	marketId = attempt.getMarketOrder().getMarket().getMarketId();
	        String trader = null;
	        if (attempt != null && attempt.getExecutionProposal() != null && attempt.getExecutionProposal().getVenue() != null && attempt.getExecutionProposal().getVenue().getMarketMaker() != null)
	        		trader = attempt.getExecutionProposal().getVenue().getMarketMaker().getCode();
	        Double customerPrice = null;
	        BigDecimal customerSpread = BigDecimal.ZERO;
	        String bestBankCode = null;
	        if (attempt != null && attempt.getExecutionProposal() != null && attempt.getExecutionProposal().getMarketMarketMaker() != null)
	        	bestBankCode = attempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode();
	        BigDecimal counterPrice = null;
	        if (attempt != null && attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null)
	        	counterPrice = attempt.getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount();
	        Integer customerAttempt = attemptNo;
	        Boolean customerFake = false;
	        String referencePriceBankCode = bestBankCode;
	
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@NUM_ORDINE", java.sql.Types.VARCHAR));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@ATTEMPT", java.sql.Types.INTEGER));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TSN", java.sql.Types.INTEGER));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@MARKETID", java.sql.Types.INTEGER));
		    jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@PREZZO_MEDIO", java.sql.Types.DECIMAL));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TRADER", java.sql.Types.VARCHAR));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@TICKET_NUM", java.sql.Types.VARCHAR));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_PRICE", java.sql.Types.DECIMAL));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_SPREAD", java.sql.Types.BIGINT));
	   		jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@BEST_BANK_CODE", java.sql.Types.VARCHAR));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@COUNTER_PRICE", java.sql.Types.DECIMAL));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_ATTEMPT", java.sql.Types.INTEGER));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@CUSTOMER_FAKE", java.sql.Types.BIT));
			jdbcCallSaveNewAttempt.addDeclaredParameter(new SqlParameter("@REFERENCE_PRICE_BANK_CODE", java.sql.Types.VARCHAR));
	
	        //call
	        //Map<String, Object> parameters = new ConcurrentHashMap<String, Object>();
			Map<String, Object> parameters = new HashMap<String, Object>();
	        parameters.put("@NUM_ORDINE", orderId);
	        parameters.put("@ATTEMPT", attemptNo);
	    	parameters.put("@TSN", tsn);
	       	parameters.put("@MARKETID", marketId);
	       	parameters.put("@PREZZO_MEDIO", prezzoMedio);
	       	parameters.put("@TRADER", trader);
	       	parameters.put("@TICKET_NUM", ticketNum);
	       	parameters.put("@CUSTOMER_PRICE", customerPrice);
	        parameters.put("@CUSTOMER_SPREAD", customerSpread);
	       	parameters.put("@BEST_BANK_CODE", bestBankCode);
	       	parameters.put("@COUNTER_PRICE", counterPrice);
	        parameters.put("@CUSTOMER_ATTEMPT", customerAttempt);
	        parameters.put("@CUSTOMER_FAKE", customerFake);
	       	parameters.put("@REFERENCE_PRICE_BANK_CODE", referencePriceBankCode);
	       	TransactionStatus status = this.transactionManager.getTransaction(def);
	        jdbcCallSaveNewAttempt.execute(parameters);
	        this.transactionManager.commit(status);
	        } catch(Exception e) {
	      	 LOGGER.info("Exception got when trying to saveNewAttempt", e);
	      	 e.printStackTrace();
	        }
	        LOGGER.info("[AUDIT],StoreTime={},Stop saveNewAttempt", (DateService.currentTimeMillis() - t0));
    	} else {
    		LOGGER.info("Order {}: requested to save new Attempt in TabHistoryOrdini index {}, but last saved attempt has index {})", orderId, attemptNo, lastSavedAttempt);
    	}
	    return attemptNo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateAttempt(java.lang.String,
     * it.softsolutions.bestx.model.Attempt, java.lang.String, int, java.lang.String,
     * it.softsolutions.bestx.model.ExecutionReport)
     */
    @Transactional
    @Override
    public void updateAttempt(final String orderId, final Attempt attempt, final String tsn, final int attemptNo, final String ticketNum, final ExecutionReport executionReport) {
        final String sql = "UPDATE TabTentativi SET" + " Tsn = ?," + // 1
                        " MarketId = ?," + // 2
                        " PrezzoMedio = ?," + // 3
                        " Trader = ?," + // 4
                        " TicketNum = ?," + // 5
                        " CustomerPrice = ?," + // 6
                        " CustomerSpread = ?," + // 7
                        " BestBankCode = ?," + // 8
                        " CounterPrice = ?," + // 9
                        " CustomerFake = ?," + // 10
                        " MatchOrderNumber = ?" + // 11
                        " WHERE NumOrdine = ?" + // 12
                        " AND Attempt = ?"; // 13
        LOGGER.debug("Start updateAttempt - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                // " Tsn," + // 1
                if (tsn != null) {
                    stmt.setString(1, tsn);
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                }
                // " MarketId," + // 2
                if (executionReport != null) {
                    if (executionReport.getMarket() != null) {
                        stmt.setLong(2, executionReport.getMarket().getMarketId());
                    } else {
                        stmt.setNull(2, java.sql.Types.INTEGER);
                    }
                } else if (attempt.getExecutionProposal() != null && attempt.getExecutionProposal().getMarket() != null) {
                    stmt.setLong(2, attempt.getExecutionProposal().getMarket().getMarketId());
                } else {
                    stmt.setNull(2, java.sql.Types.INTEGER);
                }
                // " PrezzoMedio," + // 3
                if (executionReport != null && executionReport.getLastPx() != null) {
                    stmt.setBigDecimal(3, executionReport.getLastPx());
                } else {
                    stmt.setNull(3, java.sql.Types.DECIMAL);
                }
                // " Trader," + // 4
                if (executionReport != null && executionReport.getExecBroker() != null) {
                    stmt.setString(4, executionReport.getExecBroker());
                } else if (attempt.getExecutionProposal() != null && attempt.getExecutionProposal().getVenue() != null && attempt.getExecutionProposal().getVenue().getMarketMaker() != null) {
                    stmt.setString(4, attempt.getExecutionProposal().getVenue().getMarketMaker().getCode());
                } else {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                }
                // " TicketNum," + // 5
                if (ticketNum != null) {
                    stmt.setString(5, ticketNum);
                } else {
                    stmt.setNull(5, java.sql.Types.VARCHAR);
                }
                // " CustomerPrice," + // 6
                if (executionReport != null && executionReport.getLastPx() != null) {
                    stmt.setBigDecimal(6, executionReport.getLastPx());
                } else {
                    stmt.setNull(6, java.sql.Types.DECIMAL);
                }
                // " CustomerSpread," + // 7
                stmt.setBigDecimal(7, BigDecimal.ZERO);
                // " BestBankCode," + // 8
                if (executionReport != null && executionReport.getExecBroker() != null) {
                    stmt.setString(8, executionReport.getExecBroker());
                } else if (attempt.getExecutionProposal() != null && attempt.getExecutionProposal().getMarketMarketMaker() != null
                                && attempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker() != null) {
                    stmt.setString(8, attempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());
                } else {
                    stmt.setNull(8, java.sql.Types.VARCHAR);
                }
                // " CounterPrice," + // 9
                if (attempt.getExecutablePrice(0) != null && attempt.getExecutablePrice(0).getClassifiedProposal() != null && attempt.getExecutablePrice(0).getClassifiedProposal().getPrice() != null) {
                    stmt.setBigDecimal(9, attempt.getExecutablePrice(0).getClassifiedProposal().getPrice().getAmount());
                } else {
                    stmt.setNull(9, java.sql.Types.DECIMAL);
                }
                // " CustomerFake" + // 10
                stmt.setBoolean(10, false);
                // " MatchOrderNumber" + // 11
                stmt.setNull(11, java.sql.Types.DECIMAL);
                // " NumOrdine," + // 12
                stmt.setString(12, orderId);
                // " Attempt," + // 13
                stmt.setInt(13, attemptNo);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateAttempt", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#saveNewOrder(it.softsolutions.bestx.model.Order,
     * it.softsolutions.bestx.OperationState, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String[], java.util.Date, java.lang.String)
     */
    @Transactional
    @Override
	public void saveNewOrder(final Order order, final OperationState currentState, final String propName, final String propCode, final String operatorCode, final String event,
                    final OperationStateAuditDao.Action[] availableActions, final Date receiveTime, final String sessionId, boolean notAutoExecute) {
        final String sql = "INSERT INTO TabHistoryOrdini (" + " NumOrdine," + // 1
                        " ISIN," + // 2
                        " DescrizioneStrumento," + // 3
                        " Cliente," + // 4
                        " TipoOrdine," + // 5
                        " Lato," + // 6
                        " Prezzo," + // 7
                        " Quantita," + // 8
                        " Valuta," + // 9
                        " DataValuta," + // 10
                        " DataOraRicezione," + // 11
                        " Note," + // 12
                        " Viewed," + // 13
                        " Stato," + // 14
                        " Azioni," + // 15
                        " PropCode," + // 16
                        " OperatorCode," + // 17
                        " CustomerOrderId," + // 18
                        " MeetingOrder," + // 19
                        " PropName," + // 20
                        " SessionId," + // 21
                        " ExecutionDestination," + // 22
                        " TicketOwner," + // 23
                        " LimitFileNextExecutionTime," + //24
                        " TimeInForce," + // 25
                        " NotAutoExecute" + // 26
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        LOGGER.debug("Start saveNewOrder - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        int res = jdbcTemplate.update(sql, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                // " NumOrdine," + // 1
                stmt.setString(1, order.getFixOrderId());
                // " ISIN," + // 2
                if (order.getInstrument() != null && order.getInstrument().getIsin() != null) {
                    stmt.setString(2, order.getInstrument().getIsin());
                } else if (order.getInstrumentCode() != null) {
                    stmt.setString(2, order.getInstrumentCode());
                } else {
                    stmt.setNull(2, java.sql.Types.VARCHAR);
                }
                // stmt.setString(2, order.getInstrument().getIsin());
                // " DescrizioneStrumento," + // 3
                if (order.getInstrument() != null && order.getInstrument().getDescription() != null) {
                    stmt.setString(3, order.getInstrument().getDescription());
                } else {
                    stmt.setNull(3, java.sql.Types.VARCHAR);
                }
                // " Cliente," + // 4
                if (order.getCustomer() != null) {
                    stmt.setString(4, order.getCustomer().getFixId());
                } else {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                }
                // " TipoOrdine," + // 5
                if (order.getType() != null) {
                    stmt.setString(5, order.getType().getFixCode());
                } else {
                    stmt.setNull(5, java.sql.Types.VARCHAR);
                }
                // " Lato," + // 6
                if (order.getType() == null) {
                    LOGGER.error("Order {} type not available, cannot get its side!!", order.getFixOrderId());
                }
                stmt.setString(6, order.getSide().getFixCode());
                // " Prezzo," + // 7
                if (order.getLimit() != null) {
                    stmt.setBigDecimal(7, order.getLimit().getAmount());
                } else {
                    stmt.setNull(7, java.sql.Types.DECIMAL);
                }
                // " Quantita," + // 8
                stmt.setBigDecimal(8, order.getQty());
                // " Valuta," + // 9
                if (order.getCurrency() != null) {
                    stmt.setString(9, order.getCurrency());
                } else {
                    stmt.setNull(9, java.sql.Types.VARCHAR);
                }
                // " DataValuta," + // 10
                if (order.getFutSettDate() != null) {
                    stmt.setDate(10, new java.sql.Date(order.getFutSettDate().getTime()));
                } else {
                    stmt.setNull(10, java.sql.Types.DATE);
                }
                // " DataOraRicezione," + // 11
                if (receiveTime != null) {
                    stmt.setTimestamp(11, new java.sql.Timestamp(receiveTime.getTime()));
                } else {
                    stmt.setNull(11, java.sql.Types.TIMESTAMP);
                }
                // " Note," + // 12
                if (order.getText() != null) {
                    stmt.setString(12, order.getText());
                } else {
                    stmt.setString(12, "");
                }
                // " Viewed," + // 13
                stmt.setBoolean(13, false);
                // " Stato," + // 14
                stmt.setString(14, currentState.getClass().getSimpleName());
                // " Azioni," + // 15
                stmt.setString(15, concatActions(availableActions));
                // " PropCode," + // 16
                stmt.setString(16, propCode);
                // " OperatorCode," + // 17
                stmt.setString(17, operatorCode);
                // " CustomerOrderId," + // 18
                if (order.getCustomerOrderId() != null) {
                    stmt.setString(18, order.getCustomerOrderId());
                } else {
                    stmt.setNull(18, java.sql.Types.VARCHAR);
                }
                // " MeetingOrder" + // 19
                stmt.setBoolean(19, false);
                // " PropName" + // 20
                stmt.setString(20, propName);
                // " SessionId" + // 21
                stmt.setString(21, sessionId);
                // " ExecutionDestination" + //22
                if (order.getExecutionDestination() != null) {
                    stmt.setString(22, order.getExecutionDestination());
                } else {
                    stmt.setNull(22, java.sql.Types.VARCHAR);
                }
                //23 TicketOwner 
                if (order.getTicketOwner()!= null) {
                    stmt.setString(23, order.getTicketOwner());
                } else {
                    stmt.setNull(23, java.sql.Types.VARCHAR);
                }
                //effective time dei limit files
                if (order.getEffectiveTime() != null) {
                   stmt.setTimestamp(24, new java.sql.Timestamp(order.getEffectiveTime().getTime()));
                } else {
                   stmt.setNull(24, java.sql.Types.TIMESTAMP);
                }
                // " TimeInForce" + // 25
                if (order.getTimeInForce() != null) {
                	stmt.setString(25, order.getTimeInForce().toString());
                } else {
                	stmt.setNull(25, java.sql.Types.VARCHAR);
                }
                // " NotAutoExecute" + // 26
                stmt.setBoolean(26, notAutoExecute);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop saveNewOrder", (DateService.currentTimeMillis() - t0));
        LOGGER.debug("res = {}", res);
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateRevokeState(java.lang.String, boolean,
     * java.util.Date, java.lang.String)
     */
    @Transactional
    @Override
	public void updateRevokeState(final String orderId, final boolean revoked, final Date revokeTime, final String revokeNumber) {
        final String sql = "UPDATE TabHistoryOrdini SET" + " NumRevoca = ?," + // 1
                        " Revocato = ?," + // 2
                        " OraRevoca = ?" + // 3
                        " WHERE NumOrdine = ?"; // 4
        LOGGER.debug("Start updateRevokeState - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
            @Override
			public void setValues(PreparedStatement stmt) throws SQLException {
                // " NumRevoca = ?," + // 1
                if (revokeNumber != null) {
                    stmt.setString(1, revokeNumber);
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                }
                // " Revocato = ?," + // 2
                stmt.setBoolean(2, revoked);
                // " OraRevoca = ?," + // 3
                if (revokeTime != null) {
                    stmt.setTimestamp(3, new java.sql.Timestamp(revokeTime.getTime()));
                } else {
                    stmt.setNull(3, java.sql.Types.TIMESTAMP);
                }
                // " WHERE NumOrdine = ?"; // 4
                stmt.setString(4, orderId);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateRevokeState", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateOrderFill(it.softsolutions.bestx.model.Order,
     * java.lang.String, it.softsolutions.jsscommon.Money, java.lang.Integer)
     */
    @Transactional
    @Override
    public void updateOrderFill(final Order order, final String ticketNum, final Money accruedInterest, final Integer accruedInterestDays) {
        final String sql = "UPDATE TabHistoryOrdini SET" + " TicketNum = ?," + // 1
                        " NumberOfDaysAccrued = ?," + // 2
                        " AccruedInterestAmt = ?" + // 3
                        " WHERE NumOrdine = ?"; // 4
        LOGGER.debug("Start updateOrderFill - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " TicketNum = ?" + // 1
                if (ticketNum != null) {
                    stmt.setString(1, ticketNum);
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                }
                // " NumberOfDaysAccrued = ?" + // 2
                if (accruedInterestDays != null) {
                    stmt.setInt(2, accruedInterestDays);
                } else {
                    stmt.setNull(2, java.sql.Types.INTEGER);
                }
                // " AccruedInterestAmt = ?" + // 3
                if (accruedInterest != null) {
                    stmt.setBigDecimal(3, accruedInterest.getAmount());
                } else {
                    stmt.setNull(3, java.sql.Types.FLOAT);
                }
                // " WHERE NumOrdine = ?"; // 4
                stmt.setString(4, order.getFixOrderId());
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateOrderFill", (DateService.currentTimeMillis() - t0));
    }
    
    @Override
    public void updateOrderNextExecutionTime(final Order order, final Date nextExecutionTime) {
       final String sql = "UPDATE TabHistoryOrdini SET LimitFileNextExecutionTime = ? " + // 1
             " WHERE NumOrdine = ?"; // 2
       LOGGER.debug("Start updateOrderNextExecutionTime - SQL[{}]", sql);
       long t0 = DateService.currentTimeMillis();
       TransactionStatus status = this.transactionManager.getTransaction(def);
       int row = jdbcTemplate.update(sql, new PreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement stmt) throws SQLException {
             // " LimitFileNextExecutionTime = ?" + // 1
             if (nextExecutionTime != null) {
                stmt.setTimestamp(1, new Timestamp(nextExecutionTime.getTime()));
             }
             else {
                stmt.setNull(1, java.sql.Types.TIMESTAMP);
             }
             // " WHERE NumOrdine = ?"; // 2
             stmt.setString(2, order.getFixOrderId());
          }
       });
       this.transactionManager.commit(status);
       LOGGER.info("[AUDIT],StoreTime={},Stop updateOrderNextExecutionTime", (DateService.currentTimeMillis() - t0));
    }
    

    // Stefano 20080701 - synchronized to prevent deadlock on audit tables
    @Transactional
    @Override
    public synchronized void updateOrder(final Order order, final OperationState currentState, final boolean handlingState, final boolean filter262Passed, final String event,
                    final OperationStateAuditDao.Action[] availableActions, final String notes, boolean notAutoExecute) {
        final String sql = "UPDATE TabHistoryOrdini SET" + " Stato = ?," + // 1
                        // " StatoGestione = ?," + // 2
                        " Filter262Passed = ?," + // 2
                        " DescrizioneEvento = ?," + // 3
                        " Azioni = ?," + // 4
                        " isCurando = ?," + // 5
                        " MeetingOrder = ?," + // 6
                        " Viewed = ?," + // 7
                        " Final = ?," + // 8
                        " StateClass = ?," + // 9
                        " AddCommissionToCustomerPrice = ?," + // 10
                        " ExecutionDestination = ?" + // 11
                        ", Note = ?" + // 12
                        ", BestAndLimitDelta = ?" + // 13
                        ", TimeInForce = ?" + // 14
                        ", NotAutoExecute = ?" + // 15
                        " WHERE NumOrdine = ?"; // 16

        LOGGER.debug("Start updateOrder - SQL[{}]", sql);
        long startTime = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        int res = jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " StatoGestione = ?," + // 2
                // stmt.setBoolean(2, handlingState);

                // " Stato = ?," + // 1
                stmt.setString(1, currentState.getClass().getSimpleName());
                // " Filter262Passed = ?," + // 2
                stmt.setBoolean(2, !filter262Passed);
                // " DescrizioneEvento = ?," + // 3
                if (event != null) {
                    stmt.setString(3, event);
                } else {
                    stmt.setNull(3, java.sql.Types.VARCHAR);
                }
                // " Azioni = ?," + // 4
                stmt.setString(4, concatActions(availableActions));
                // " isCurando = ?," + // 5
                if (currentState instanceof CurandoState) {
                    stmt.setBoolean(5, true);
                } else {
                    stmt.setBoolean(5, false);
                }
                // " MeetingOrder = ?" + // 6
                if (order.isMatchingOrder()) {
                    stmt.setBoolean(6, true);
                } else {
                    stmt.setBoolean(6, false);
                }
                // " viewed = ?"; // 7
                stmt.setBoolean(7, false);
                // " Final = ?" + // 8
                stmt.setBoolean(8, currentState.isTerminal());
                // " StateClass = ?," + // 9
                stmt.setString(9, currentState.getClass().getName());
                // " AddCommissionToCustomerPrice = ?," + //10
                stmt.setBoolean(10, order.isAddCommissionToCustomerPrice());
                // " ExecutionDestination = ? " //11
                stmt.setString(11, order.getExecutionDestination());
                if (notes != null) {
                    stmt.setString(12, notes);
                } else {
                    stmt.setString(12, "");
                }
                if (order.getBestPriceDeviationFromLimit() != null) {
                    stmt.setDouble(13, order.getBestPriceDeviationFromLimit());
                } else {
                    stmt.setNull(13, java.sql.Types.DECIMAL);
                }
                // ", TimeInForce = ?" + // 14
                if (order.getTimeInForce() != null) {
                	stmt.setString(14, order.getTimeInForce().toString());
                } else {
                	stmt.setNull(14, java.sql.Types.VARCHAR);
                }
                // ", NotAutoExecute = ?" + // 15
                stmt.setBoolean(15, notAutoExecute);
                // 16 "NumOrdine"
                stmt.setString(16, order.getFixOrderId());
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateOrder", (DateService.currentTimeMillis() - startTime));
        LOGGER.debug("res = {}", res);
    }

    @Transactional
    @Override
    public void updateOrderBestAndLimitDelta(final Order order, final Double bestAndLimitDelta) {
        final String sql = "UPDATE TabHistoryOrdini SET BestAndLimitDelta = ?" + // 1
                        " WHERE NumOrdine = ?"; // 2
        LOGGER.debug("Update best and limit prices delta for order {}: {} - SQL[{}]", order.getFixOrderId(), bestAndLimitDelta, sql);
        long startTime = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                if (bestAndLimitDelta == null) {
                    stmt.setNull(1, java.sql.Types.DECIMAL);
                } else {
                    stmt.setDouble(1, bestAndLimitDelta);
                }
                stmt.setString(2, order.getFixOrderId());
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop update limit and best delta for order {} to {}", (DateService.currentTimeMillis() - startTime), order.getFixOrderId(), bestAndLimitDelta);
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateOrderPropName(java.lang.String, java.lang.String)
     */
    @Transactional
        @Override
        public void updateOrderPropName(final String orderId, final String propName) {
        final String sql = "UPDATE TabHistoryOrdini SET" + " PropName = ?" + // 1
                        " WHERE NumOrdine = ?"; // 2
        LOGGER.debug("Start updateOrderPropName - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " PropName = ?" + // 1
                stmt.setString(1, propName);
                // " WHERE NumOrdine = ?"; // 2
                stmt.setString(2, orderId);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateOrderPropName", (DateService.currentTimeMillis() - t0));
    }

    /*
     * Nel seguente metodo viene popolata la colonna RATEO della tabella TabHistoryOrdini, il valore e' quello calcolato
     * nel momento in cui viene ricevuto il rapporto di esecuzione dal mercato. Si tratta di (quantita' * rateo / 100).
     */
        @Transactional
    @Override
    public void finalizeOrder(final Order order, final Attempt lastAttempt, final ExecutionReport executionReport, final Date fulfillingTime) {
        final String sql = "UPDATE TabHistoryOrdini SET" + " OraEvasione = ?," + // 1
                        " MarketId = ?," + // 2
                        " QuantitaEseguita = ?," + // 3
                        " PrezzoEseguito = ?," + // 4
                        " Rateo = ?," + // 5
                        " PrezzoRiferimento = ?," + // 6
                        " BancaRiferimento = ?," + // 7
                        " AccruedInterestAmt = ?," + // 8
                        " Commissioni = ?," + // 9
                        " commissioniTick = ?," + // 10
                        " ControvaloreCommissioni = ?" + // 11
                        " WHERE NumOrdine = ?"; // 12
        LOGGER.debug("Start finalizeOrder - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                LOGGER.debug("Fields that are NOT NULL in the DB : NumOrdine, Lato, Final (the last one is not in the update)");
                LOGGER.debug("Getting values from this execution report (no value means null) : {}", executionReport.toString());
                LOGGER.debug("The order is the ", (order != null ? order.getFixOrderId() : "ORDER NULL"));
                // " OraEvasione = ?," + // 1
                if (fulfillingTime != null) {
                    stmt.setTimestamp(1, new java.sql.Timestamp(fulfillingTime.getTime()));
                } else {
                    LOGGER.error("Null timestamp: {}", executionReport.toString());
                    stmt.setNull(1, java.sql.Types.TIMESTAMP);
                }
                // " MarketId = ?," + // 2
                if (executionReport.getMarket() != null) {
                    stmt.setLong(2, executionReport.getMarket().getMarketId());
                } else {
                    stmt.setNull(2, java.sql.Types.INTEGER);
                }
                // " QuantitaEseguita = ?," + // 3
                stmt.setBigDecimal(3, executionReport.getActualQty());
                // " PrezzoEseguito = ?," + // 4
                if (executionReport.getLastPx() != null) {
                    stmt.setBigDecimal(4, executionReport.getLastPx());
                } else {
                    stmt.setNull(4, java.sql.Types.DECIMAL);
                }
                // " Rateo = ?," + // 5
                if (executionReport.getAccruedInterestRate() != null) {
                    stmt.setBigDecimal(5, executionReport.getAccruedInterestRate());
                } else {
                    stmt.setNull(5, java.sql.Types.DECIMAL);
                }
                // " PrezzoRiferimento = ?," + // 6
                if (lastAttempt.getExecutionProposal() != null && lastAttempt.getExecutionProposal().getPrice() != null) {
                    stmt.setBigDecimal(6, lastAttempt.getExecutionProposal().getPrice().getAmount());
                } else {
                    stmt.setNull(6, java.sql.Types.DECIMAL);
                }
                // " BancaRiferimento = ?," + // 7
                if (lastAttempt.getExecutionProposal() != null && lastAttempt.getExecutionProposal().getMarketMarketMaker() != null) {
                    stmt.setString(7, lastAttempt.getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());
                } else {
                    stmt.setNull(7, java.sql.Types.VARCHAR);
                }
                // " AccruedInterestAmt = ?," + // 9
                if (executionReport.getAccruedInterestAmount() != null) {//stmt.setString(7, "PIPPO")
                    stmt.setBigDecimal(8, executionReport.getAccruedInterestAmount().getAmount());
                } else {
                    stmt.setNull(8, java.sql.Types.DECIMAL);
                }
                // " Commissioni = ?," + // 9
                if (executionReport.getCommission() != null) {
                    stmt.setBigDecimal(9, executionReport.getCommission());
                } else {
                    stmt.setNull(9, java.sql.Types.DECIMAL);
                }
                // " commissioniTick = ?," + // 10
                if (executionReport.getCommission() != null && executionReport.getCommissionType() != null && CommissionType.TICKER.equals(executionReport.getCommissionType())) {
                    stmt.setBoolean(10, true);
                } else {
                    stmt.setBoolean(10, false);
                }
                if (executionReport.getAmountCommission() != null) {
                    stmt.setBigDecimal(11, executionReport.getAmountCommission());
                } else {
                    stmt.setNull(11, java.sql.Types.DECIMAL);
                }
                // " WHERE NumOrdine = ?"; // 12
                if(order != null && order.getFixOrderId() != null) {
                	stmt.setString(12, order.getFixOrderId());
                } else {
                    stmt.setNull(12, java.sql.Types.VARCHAR);
                }
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop finalizeOrder", (DateService.currentTimeMillis() - t0));
    }

    /*
     * 03-07-2009 Ruggero Here we update the order state descriptiong adding informations about the order data : price,
     * quantity and side
     * 
     * @see
     * it.softsolutions.bestx.dao.AkrosOperationStateAuditDao#updateOrderStatusDescription(it.softsolutions.bestx.model
     * .Order, java.math.BigDecimal)
     */
        @Transactional
    @Override
    public void updateOrderStatusDescription(final Order order, final BigDecimal price) {
        final String orderId = order.getFixOrderId();
        final DecimalFormat numberFormat = new DecimalFormat("#,##0.000");
        final String TLX_START_MAGNET_STATE = "TLX_StartMagnetExecutionState";
        String sql = "SELECT CONVERT(varchar(500) ,DescrizioneEvento)  DescrizioneEvento" + " FROM TabHistoryStati" + " WHERE NumOrdine = '" + order.getFixOrderId() + "' " + " AND Stato = '"
                        + TLX_START_MAGNET_STATE + "'";
        LOGGER.debug(sql);
        String descStato = null;
        TransactionStatus status = this.transactionManager.getTransaction(def);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
        if (rowSet.next()) {
            descStato = rowSet.getString("DescrizioneEvento");
        }

        if (descStato != null) {
            String updateHistStati = "UPDATE TabHistoryStati " + "set DescrizioneEvento = ? " + // 1
                            "WHERE NumOrdine = ? " + // 2
                            "AND Stato = ? "; // 3
            LOGGER.debug("Updating order status description on the TabHistoryStati, the market sent us the price at which it put the order on the isin book.");
            LOGGER.debug("Order {}, Price {}", orderId, price.doubleValue());
            LOGGER.debug(updateHistStati);
            final String orderDescription = descStato + ". "
                            + Messages.getString("ORDER_DESCRIPTION", numberFormat.format(price.doubleValue()), numberFormat.format(order.getQty().doubleValue()), order.getSide().name());
            LOGGER.debug("Adding description: {}", orderDescription);
            jdbcTemplate.update(updateHistStati, new PreparedStatementSetter() {
            	@Override
                public void setValues(PreparedStatement stmt) throws SQLException {
                    // DescrizioneEvento = ? // 1
                    stmt.setString(1, orderDescription);
                    // NumOrdine = ? //2
                    stmt.setString(2, orderId);
                    // Stato = ? //3
                    stmt.setString(3, TLX_START_MAGNET_STATE);
                }
            });
            this.transactionManager.commit(status);
        } else {
            LOGGER.debug("TabHistoryStati : status not found for order {}", orderId);
        }

        sql = "SELECT CONVERT(varchar(500) ,DescrizioneEvento)  DescrizioneEvento" + " FROM TabHistoryOrdini" + " WHERE NumOrdine = '" + order.getFixOrderId() + "' ";
        LOGGER.debug(sql);
        descStato = null;
        rowSet = null;
         status = this.transactionManager.getTransaction(def);
        rowSet = jdbcTemplate.queryForRowSet(sql);
        if (rowSet.next()) {
            descStato = rowSet.getString("DescrizioneEvento");
        }
        if (descStato != null) {
            String updateHistOrdini = "UPDATE TabHistoryOrdini " + "set DescrizioneEvento = ? " + // 1
                            "WHERE NumOrdine = ? "; // 2
            LOGGER.debug("Updating order status description on the TabHistoryOrdini, the market sent us the price at which it put the order on the isin book.");
            LOGGER.debug("Order {}, Price ", orderId, price.doubleValue());
            LOGGER.debug(updateHistOrdini);
            final String orderDescription = descStato + ". "
                            + Messages.getString("ORDER_DESCRIPTION", numberFormat.format(price.doubleValue()), numberFormat.format(order.getQty().doubleValue()), order.getSide().name());
            LOGGER.debug("Adding description : {}", orderDescription);
            jdbcTemplate.update(updateHistOrdini, new PreparedStatementSetter() {
            	@Override
                public void setValues(PreparedStatement stmt) throws SQLException {
                    // DescrizioneEvento = ? // 1
                    stmt.setString(1, orderDescription);
                    // NumOrdine = ? //2
                    stmt.setString(2, orderId);
                }
            });
        } else {
            LOGGER.info("TabHistoryOrdini : status not found for order ", orderId);
        }
        this.transactionManager.commit(status);
        LOGGER.debug("Order {} processed for status description modify.", orderId);
    }

    private String concatActions(OperationStateAuditDao.Action[] actions) {
        StringBuilder sb = new StringBuilder();

        if (actions != null && actions.length > 0) {
            sb.append(actions[0]);
            for (int i = 1; i < actions.length; i++) {
                sb.append(';').append(actions[i]);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean usedOrderId(String id) {
        return !jdbcTemplate.queryForList("SELECT NumOrdine FROM TabHistoryOrdini WHERE NumOrdine = ?", new Object[] {id}).isEmpty();
    }

    @Transactional
    @Override
    public synchronized void addOrderCount() {
        counter++;
        TransactionStatus status = this.transactionManager.getTransaction(def);
        if (isFirst) {
            String sql = "INSERT INTO WorkingDates (" + " workingDate," + " receivedOrders)" + " VALUES(" + strDate + "," + counter + ")";
            jdbcTemplate.update(sql);
            isFirst = false;
        } else {
            String sql = "UPDATE WorkingDates SET" + " receivedOrders = " + counter + " WHERE workingDate = " + strDate;
            jdbcTemplate.update(sql);
        }
        this.transactionManager.commit(status);
    }

    // @Override
    // public void saveAllFills(Order order, List<MarketExecutionReport> fills) {
    // for (MarketExecutionReport fill : fills) {
    // saveNewFill(order, fill);
    // }
    // }

    @Transactional
    @Override
    public synchronized void saveNewFill(final Order order, final MarketExecutionReport fill) {
        int newFill = 1;
        String sqlSelect = "SELECT MAX(NumFill) FROM tabOrderFill WHERE numOrdine = '" + order.getFixOrderId() + "'";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
        if (rowSet.next()) {
            newFill = rowSet.getInt(1) + 1;
        }
        final int fillNumber = newFill;
        final String sql = "INSERT INTO tabOrderFill (" + "NumOrdine, " + // 1
                        "NumFill, " + // 2
                        "ISIN, " + // 3
                        "CD, " + // 4
                        "ContractNo, " + // 5
                        "Price, " + // 6
                        "Yield, " + // 7
                        "Qty, " + // 8
                        "OrdStatus, " + // 9
                        "Side, " + // 10
                        "OrderTrader, " + // 11
                        "Timestamp, " + // 12
                        "SourceMarket, " + // 13
                        "SecurityIDSource, " + // 14
                        "OrderID, " + // 15
                        "SettlDate, " + // 16
                        "TransactTime, " + // 17
                        "MarketId, " + // 18
                        "Counterpart) " + // 19
                        "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        LOGGER.debug("Start saveNewFill - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " NumOrdine," + // 1
                stmt.setString(1, order.getFixOrderId());
                // "NumFill, " + //2
                stmt.setInt(2, fillNumber);
                // "ISIN, " + //3
                if (order.getInstrument() != null) {
                    stmt.setString(3, order.getInstrument().getIsin());
                } else {
                    LOGGER.error("Order {}, the instrument is NULL!!", order.getFixOrderId());
                    stmt.setNull(3, java.sql.Types.VARCHAR);
                }
                // "CD, " + //4
                stmt.setString(4, fill.getSequenceId());
                // "ContractNo, " + //5
                stmt.setString(5, fill.getTicket());
                // "Price, " + //6
                if (fill.getLastPx() != null) {
                    stmt.setDouble(6, fill.getLastPx().doubleValue());
                } else {
                    LOGGER.error("Order {}, no price available!!", order.getFixOrderId());
                    stmt.setNull(6, java.sql.Types.VARCHAR);
                }
                // "Yield, " + //7
                stmt.setDouble(7, 0.0);
                // "Qty, " + //8
                stmt.setDouble(8, (fill.getActualQty() != null ? fill.getActualQty().doubleValue() : 0.0));
                // "OrdStatus, " + //9
                stmt.setString(9, (fill.getState() != null ? fill.getState().toString() : ""));
                // "Side, " + //10
                stmt.setString(10, (fill.getSide() != null ? fill.getSide().getFixCode() : ""));
                // "OrderTrader, " + //11
                stmt.setString(11, fill.getOrderTrader());
                // "Timestamp, " + //12
                stmt.setString(12, (fill.getTransactTime() != null ? DateService.format(dateTimeForDb, fill.getTransactTime()) : ""));
                // "SourceMarket, " + //13
                stmt.setString(13, (fill.getMarket() != null ? fill.getMarket().getMarketCode().name() : ""));
                // "SecurityIDSource, " + //14
                stmt.setString(14, fill.getSecurityIdSource());
                // "OrderID, " + //15
                stmt.setString(15, fill.getMarketOrderID());
                // "SettlDate, " + //16
                Date ts = fill.getFutSettDate();
                if (ts == null) {
                    stmt.setDate(16, null);
                } else {
                    stmt.setDate(16, new java.sql.Date(ts.getTime()));
                }
                // "TransactTime, " + //17
                java.sql.Timestamp dat1 = new java.sql.Timestamp(fill.getTransactTime().getTime());
                stmt.setTimestamp(17, dat1);
                // "MarketId, " + //18
                stmt.setInt(18, (fill.getMarket() != null ? fill.getMarket().getMarketId().intValue() : 0));
                // "Counterpart) " + // 19
                stmt.setString(19, fill.getCounterPart());

            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop saveNewFill", (DateService.currentTimeMillis() - t0));
    }

    /*
     * TimeStamp=1.246268289733E9, SourceMarketName=TLXFIX, OrdStatus=0, SecurityIDSource=4, OrderNum=11090629113848,
     * UserSessionName=PAOLO, Side=7, AvgPx=0.0, CD=11090629113848, OrderTrader=PAOLO, ExecType=0,
     * OrderID=199dc1c780867f66, Symbol=DE0001135176, ContractNo=11090629113848_35, CumQty=0.0, Isin=DE0001135176,
     * Price=99.8
     */
    @Transactional
    @Override
    public void savePriceFill(final Order order, final MarketExecutionReport fill) {
        int newFill = 1;
        String sqlSelect = "SELECT MAX(NumFill) FROM tabOrderFill WHERE numOrdine = '" + order.getFixOrderId() + "'";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
        if (rowSet.next()) {
            newFill = rowSet.getInt(1) + 1;
        }
        final int fillNumber = newFill;
        final String sql = "INSERT INTO tabOrderFill (" + "NumOrdine, " + // 1
                        "NumFill, " + // 2
                        "ISIN, " + // 3
                        "CD, " + // 4
                        "ContractNo, " + // 5
                        "Side, " + // 6
                        "OrderTrader, " + // 7
                        "Timestamp, " + // 8
                        "SourceMarket, " + // 9
                        "OrderID, " + // 10
                        "MarketId, " + // 11
                        "Counterpart, " + // 12
                        "Price)" + // 13
                        "values (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        LOGGER.debug("Start savePriceFill - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " NumOrdine," + // 1
                stmt.setString(1, order.getFixOrderId());
                // "NumFill, " + //2
                stmt.setInt(2, fillNumber);
                // "ISIN, " + //3
                if (order.getInstrument() != null) {
                    stmt.setString(3, order.getInstrument().getIsin());
                } else {
                    LOGGER.error("Order {}, the instrument is NULL!!", order.getFixOrderId());
                    stmt.setNull(3, java.sql.Types.VARCHAR);
                }
                // "CD, " + //4
                stmt.setString(4, fill.getSequenceId());
                // "ContractNo, " + //5
                stmt.setString(5, fill.getTicket());
                // "Side, " + //6
                if (fill.getSide() != null) {
                    stmt.setString(6, fill.getSide().getFixCode());
                } else {
                    LOGGER.error("Order {}, the side is NULL!!", order.getFixOrderId());
                    stmt.setNull(6, java.sql.Types.VARCHAR);
                }
                // "OrderTrader, " + //7
                stmt.setString(7, fill.getOrderTrader());
                // "Timestamp, " + //8
                // Es 20090305-10:58:01
                if (fill.getTransactTime() != null) {
                    stmt.setString(8,  DateService.format(dateTimeForDb, fill.getTransactTime()));
                } else {
                    LOGGER.error("Order {}, the timestamp is NULL!!", order.getFixOrderId());
                    stmt.setNull(8, java.sql.Types.VARCHAR);
                }
                // "SourceMarket, " + //9
                if (fill.getMarket() != null && fill.getMarket().getMarketCode() != null) {
                    stmt.setString(9, fill.getMarket().getMarketCode().name());
                } else {
                    LOGGER.error("Order {}, the source market is NULL!!", order.getFixOrderId());
                    stmt.setNull(9, java.sql.Types.VARCHAR);
                }
                // "OrderID, " + //10
                stmt.setString(10, fill.getMarketOrderID());
                // "MarketId, " + //11
                if (fill.getMarket() != null && fill.getMarket().getMarketId() != null) {
                    stmt.setInt(11, fill.getMarket().getMarketId().intValue());
                } else {
                    LOGGER.error("Order {}, the market id is NULL!!", order.getFixOrderId());
                    stmt.setNull(11, java.sql.Types.INTEGER);
                }
                // "Counterpart, " + // 12
                stmt.setString(12, fill.getCounterPart());
                // Price) //13
                if (fill.getLastPx() != null) {
                    stmt.setBigDecimal(13, fill.getLastPx());
                } else {
                    LOGGER.error("Order {}, the price is NULL!!", order.getFixOrderId());
                    stmt.setNull(13, java.sql.Types.DECIMAL);
                }
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop saveNewFill", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateMatchingOrderAttempt(java.lang.String, int,
     * java.lang.String)
     */
    @Transactional
    @Override
    public void updateMatchingOrderAttempt(final String orderId, final int attemptNo, final String matchingOrderId) {
        final String sql = "UPDATE TabTentativi SET" + " MatchOrderNumber = ? " + // 1
                        " WHERE NumOrdine = ?" + // 2
                        " AND Attempt = ?"; // 3
        LOGGER.debug("Start updateMatchingOrderAttempt - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " MatchOrderNumber," + // 1
                if (matchingOrderId != null) {
                    stmt.setString(1, matchingOrderId);
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                }
                // " NumOrdine," + // 2
                stmt.setString(2, orderId);
                // " Attempt," + // 3
                stmt.setInt(3, attemptNo);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateMatchingOrderAttempt", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#addBloombergTrade(it.softsolutions.bestx.model.TradeFill)
     */
    @Transactional
    @Override
    public void addBloombergTrade(final TradeFill trade) {
    	TransactionStatus status = this.transactionManager.getTransaction(def);
        String sqlSel = "SELECT Id FROM BloombergTrades WHERE ISIN = '" 
                        + trade.getInstrument().getIsin() + "' AND Ticket = '" + trade.getTicket() + "'";
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSel);
        if (rowSet.next()) {
            return;
        }

        final String sql = "INSERT INTO BloombergTrades (" + "Id," + // 1
                        " ISIN," + // 2
                        " Currency," + // 3
                        " ActualQuantity," + // 4
                        " ExecutionPrice," + // 5
                        " TransactionTime," + // 6
                        " Lato," + // 7
                        " Ticket," + // 8
                        " BankCode," + // 9
                        " SettlementDate" + // 10
                        ") VALUES (?,?,?,?,?,?,?,?,?,?)";
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // "Id," + // 1
                stmt.setString(1, trade.getSequenceId());
                // " ISIN," + // 2
                if (trade.getInstrument() != null) {
                    stmt.setString(2, trade.getInstrument().getIsin());
                } else {
                    stmt.setNull(2, java.sql.Types.VARCHAR);
                }
                // " Currency," + // 3
                if (trade.getPrice() != null) {
                    stmt.setString(3, trade.getPrice().getStringCurrency());
                } else {
                    stmt.setNull(3, java.sql.Types.VARCHAR);
                }
                // " ActualQuantity," + // 4
                if (trade.getActualQty() != null) {
                    stmt.setBigDecimal(4, trade.getActualQty());
                } else {
                    stmt.setNull(4, java.sql.Types.DECIMAL);
                }
                // " ExecutionPrice," + // 5
                if (trade.getLastPx() != null) {
                    stmt.setBigDecimal(5, trade.getLastPx());
                } else {
                    stmt.setNull(5, java.sql.Types.DECIMAL);
                }
                // " TransactionTime," + // 6
                if (trade.getTransactTime() != null) {
                    stmt.setTimestamp(6, new Timestamp(trade.getTransactTime().getTime()));
                } else {
                    stmt.setNull(6, java.sql.Types.TIMESTAMP);
                }
                // " Lato," + // 7
                if (trade.getSide() != null) {
                    stmt.setString(7, trade.getSide().getFixCode());
                } else {
                    stmt.setNull(7, java.sql.Types.VARCHAR);
                }
                // " Ticket," + // 8
                if (trade.getTicket() != null) {
                    stmt.setString(8, trade.getTicket());
                } else {
                    stmt.setNull(8, java.sql.Types.VARCHAR);
                }
                // " BankCode," + // 9
                if (trade.getMarketMaker() != null) {
                    stmt.setString(9, trade.getMarketMaker().getCode());
                } else {
                    stmt.setNull(9, java.sql.Types.VARCHAR);
                }
                // " SettlementDate" + // 10
                if (trade.getFutSettDate() != null) {
                    stmt.setDate(10, new java.sql.Date(trade.getFutSettDate().getTime()));
                } else {
                    stmt.setNull(10, java.sql.Types.DATE);
                }
            }
        });
        this.transactionManager.commit(status);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * it.softsolutions.bestx.dao.OperationStateAuditDao#assignBloombergTradeToOrder(it.softsolutions.bestx.model.TradeFill
     * , it.softsolutions.bestx.model.Order)
     */
    @Transactional
    @Override
    public void assignBloombergTradeToOrder(final TradeFill trade, final Order order) {
        final String sql = "UPDATE BloombergTrades SET" + " NumOrdine = ?" + // 1
                        " WHERE Id = ?"; // 2
        LOGGER.debug("Start assignBloombergTradeToOrder - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " NumOrdine = ?" + // 1
                stmt.setString(1, order.getFixOrderId());
                // " WHERE Id = ?"; // 2
                stmt.setString(2, trade.getSequenceId());
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop assignBloombergTradeToOrder", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see it.softsolutions.bestx.dao.OperationStateAuditDao#updateOrderExecutionDestination(java.lang.String,
     * java.lang.String)
     */
    @Transactional
    @Override
    public void updateOrderExecutionDestination(final String orderId, final String executionDestination) {
        final String sql = "UPDATE TabHistoryOrdini SET ExecutionDestination = ? " + " WHERE NumOrdine = ?"; // 2
        LOGGER.debug("Start updateOrderExecutionDestination - SQL[" + sql + "]");
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " PropName = ?" +/ 1
                stmt.setString(1, executionDestination);
                // " WHERE NumOrdine = ?";/ 2
                stmt.setString(2, orderId);
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateOrderExecutionDestination", (DateService.currentTimeMillis() - t0));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * it.softsolutions.bestx.dao.OperationStateAuditDao#updateOrderSettlementDate(it.softsolutions.bestx.model.Order)
     */
    @Transactional
    @Override
    public void updateOrderSettlementDate(final Order order) {
        final String sql = "UPDATE TabHistoryOrdini SET DataValuta = ? " + " WHERE NumOrdine = ?"; // 2
        LOGGER.debug("Start updateOrderSettlementDate - SQL[{}]", sql);
        long t0 = DateService.currentTimeMillis();
        TransactionStatus status = this.transactionManager.getTransaction(def);
        jdbcTemplate.update(sql, new PreparedStatementSetter() {
        	@Override
            public void setValues(PreparedStatement stmt) throws SQLException {
                // " DataValuta = ?" +/ 1
                stmt.setDate(1, new java.sql.Date(order.getFutSettDate().getTime()));
                // " WHERE NumOrdine = ?";/ 2
                stmt.setString(2, order.getFixOrderId());
            }
        });
        this.transactionManager.commit(status);
        LOGGER.info("[AUDIT],StoreTime={},Stop updateOrderSettlementDate", (DateService.currentTimeMillis() - t0));
    }

    @Override
    public List<String> getEndOfDayOrdersToClose() {
        List<String> ordersList = new ArrayList<String>();

        String filterDate = DateService.format(DateService.dateISO, Calendar.getInstance().getTime());
        String sqlSelect = "SELECT tho.NumOrdine " + "FROM TabHistoryOrdini tho " + "JOIN Rfq_Order ro " + "ON tho.NumOrdine = ro.FixOrderId " + "WHERE tho.Final= 0 "
                        + "AND tho.DataOraRicezione >= '" + filterDate + "' " + "AND ro.TimeInForce !='" + it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL + "' OR ro.TimeInForce is null";
        LOGGER.debug("Executing query {}", sqlSelect);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
        while (rowSet.next()) {
            String orderNum = rowSet.getString("NumOrdine");
            LOGGER.debug("Active EndOfDay order found : {}", orderNum);
            ordersList.add(orderNum);
        }

        return ordersList;
    }

    public List<String> getLimitFilesEndOfDayOrdersToClose(boolean usAndGlobals) {
        List<String> ordersList = new ArrayList<String>();

        String filterDate = DateService.format(DateService.dateISO, Calendar.getInstance().getTime());
        String sqlSelect = "SELECT tho.NumOrdine " + "FROM TabHistoryOrdini tho " + "JOIN InstrumentsTable it " + "ON tho.ISIN = it.ISIN " + "JOIN Rfq_Order ro " + "ON tho.NumOrdine = ro.FixOrderId "
                        // look only for orders not in terminal states but for LimitFileNoPrice and OrderNotExecutable ones
                        + "WHERE ( tho.Final= 0 OR Stato in ('" + LimitFileNoPriceState.class.getSimpleName() + "', '" + OrderNotExecutableState.class.getSimpleName() + "') ) "
                        + "AND tho.DataOraRicezione >= '" + filterDate + "' "
                        // an order is a Limit File if TimeInForce = GOOD TILL CANCEL and OrderType = LIMIT
                        + "AND ro.TimeInForce ='" + it.softsolutions.bestx.model.Order.TimeInForce.GOOD_TILL_CANCEL + "' AND OrderType = '" + Order.OrderType.LIMIT + "' ";
        if (usAndGlobals) {
            sqlSelect += createMarketOfIssueFilter(true, true);
        } else {
            sqlSelect += createMarketOfIssueFilter(false, false);
        }
        LOGGER.debug("Executing query {}", sqlSelect);
        SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sqlSelect);
        while (rowSet.next()) {
            String orderNum = rowSet.getString("NumOrdine");
            LOGGER.debug("Active EndOfDay order found : {}", orderNum);
            ordersList.add(orderNum);
        }

        return ordersList;
    }

    private String createMarketOfIssueFilter(boolean in, boolean isUsAndGlobal) {
        String filter = "";
        
        String marketOfIssue = CSConfigurationPropertyLoader.getStringProperty(CSConfigurationPropertyLoader.USANDGLOBAL_MARKETOFISSUE_CONFIG_PROPERTY);
        String isinCodeInitial = CSConfigurationPropertyLoader.getStringProperty(CSConfigurationPropertyLoader.USANDGLOBAL_ISINCODEINITIAL_CONFIG_PROPERTY);
       
        if ((marketOfIssue != null) && (!marketOfIssue.trim().isEmpty())) {
            String[] marketsOfIssueValues = marketOfIssue.trim().split(",");
            if (marketsOfIssueValues.length > 0) {
                filter += in ? "AND ( it.MarketOfIssue IN (" : "AND ( it.MarketOfIssue NOT IN (";
                for (int i = 0; i < marketsOfIssueValues.length; i++) {
                    filter += (i >= 1 ? "," : "") + "'" + marketsOfIssueValues[i] + "'";
                }

                filter += ") ";
                filter += in ? "" : "OR it.MarketOfIssue is null) ";

                if (isUsAndGlobal) {
                    filter += "OR it.ISIN LIKE '" + isinCodeInitial + "%' )";
                } else {
                    filter += "AND it.ISIN NOT LIKE '" + isinCodeInitial + "%'";
                }
            }
        }
        return filter;
    }

    @Override
    public List<String> getMagnetOperations() {
        // not used
        return null;
    }

    @Override
    public boolean attemptAlreadySaved(Order order, int newAttemptNo) {
        // Not used yet
        return false;
    }

	public org.springframework.jdbc.datasource.DataSourceTransactionManager getDataSourceTransactionManager() {
		return dataSourceTransactionManager;
	}

	public void setDataSourceTransactionManager(org.springframework.jdbc.datasource.DataSourceTransactionManager dataSourceTransactionManager) {
		this.dataSourceTransactionManager = dataSourceTransactionManager;
		this.transactionManager = dataSourceTransactionManager;
	}

	@Transactional
   @Override
   public void updateTabHistoryOperatorCode(String orderId, String operatorCode) {
      final String sql = "UPDATE TabHistoryOrdini SET OperatorCode = ? WHERE NumOrdine = ?";
      LOGGER.debug("Start updateTabHistoryOperatorCode - SQL[{}]", sql);
      TransactionStatus status = this.transactionManager.getTransaction(def);
      jdbcTemplate.update(sql, new PreparedStatementSetter() {
       @Override
          public void setValues(PreparedStatement stmt) throws SQLException {
              stmt.setString(1, operatorCode);
              stmt.setString(2, orderId);
          }
      });
      this.transactionManager.commit(status);
   }
}
