/**
 * 
 */
package it.softsolutions.bestx.markets.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.ConnectionHelper;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.CmfConnection;
import it.softsolutions.bestx.connections.CmfConnectionListener;
import it.softsolutions.bestx.connections.Connection;
import it.softsolutions.bestx.connections.ConnectionListener;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.connections.TradingConsoleConnection;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.exceptions.OperationNotExistingException;
import it.softsolutions.bestx.finders.CustomerFinder;
import it.softsolutions.bestx.finders.InstrumentFinder;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.finders.MarketMakerFinder;
import it.softsolutions.bestx.finders.VenueFinder;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.management.statistics.StatisticsSnapshot;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.model.Trader;
import it.softsolutions.bestx.model.Venue;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.jsscommon.Money;

/**
 * @author anna
 * 
 */
public class InternalMarket extends MarketConnection implements Connection, ConnectionListener, CmfConnectionListener, MarketBuySideConnection, MarketMXBean, TradingConsoleConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalMarket.class);

    private OperationRegistry operationRegistry;
    private Executor executor;
    private CustomerFinder customerFinder;
    private InstrumentFinder instrumentFinder;
    private CmfConnection cmfConnection;
    private ConnectionListener connectionListener;
    private ConnectionHelper connectionHelper;
    private volatile long statTrades;
    private volatile long statTotalRequestAcks;
    private volatile long statTotalRequestNacks;
    private volatile long statTotalRequestReplies;
    private volatile long statFailedRequestAcks;
    private volatile long statFailedRequestNacks;
    private volatile long statFailedRequestReplies;
    private volatile long statExceptions;
    private volatile boolean statConnected;

    private Date statStart;
    private Date statLastStartup;
    private String btsAccount;
    private int sourceCode;
    private long pendExpiration;
    private String internalMarketMaker;
    private VenueFinder venueFinder;
    private MarketFinder marketFinder;
    private SerialNumberService serialNumberService;
    private MarketMakerFinder marketMakerFinder;
    private String name;

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (cmfConnection == null) {
            throw new ObjectNotInitializedException("CMF connection not set");
        }
        if (marketMakerFinder == null) {
            throw new ObjectNotInitializedException("Market maker finder not set");
        }
        if (venueFinder == null) {
            throw new ObjectNotInitializedException("Venue finder not set");
        }
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
        if (executor == null) {
            throw new ObjectNotInitializedException("Executor not set");
        }
        if (customerFinder == null) {
            throw new ObjectNotInitializedException("Operation finder not set");
        }
        if (instrumentFinder == null) {
            throw new ObjectNotInitializedException("Instrument finder not set");
        }
        if (connectionHelper == null) {
            throw new ObjectNotInitializedException("Connection helper not set");
        }
        if (btsAccount == null) {
            throw new ObjectNotInitializedException("BTS Account not set");
        }
        if (venueFinder == null) {
            throw new ObjectNotInitializedException("Venue finder not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("Market Finder not set");
        }
    }

    public double getAvgInputPerSecond() {
        if (statStart == null) {
            return 0.0;
        }
        long currentTime = DateService.currentTimeMillis();
        long startTime = statStart.getTime();
        // avoid div per zero
        return (double) (statTotalRequestAcks + statTotalRequestNacks + statTotalRequestReplies) / (currentTime - startTime + 1) * 1000.0;
    }

    public double getAvgOutputPerSecond() {
        if (statStart == null) {
            return 0.0;
        }
        long currentTime = DateService.currentTimeMillis();
        long startTime = statStart.getTime();
        return (double) statTrades / (currentTime - startTime + 1) * 1000.0; // avoid div per zero
    }

    public double getPctOfFailedInput() {
        long totInput = statTotalRequestAcks + statTotalRequestNacks + statTotalRequestReplies;
        if (totInput > 0) {
            return (double) (statFailedRequestAcks + statFailedRequestNacks + statFailedRequestReplies) / totInput * 100;
        } else {
            return 0.0;
        }
    }

    public long getOutTrades() {
        return statTrades;
    }

    public long getInRequestAcks() {
        return statTotalRequestAcks;
    }

    public long getInRequestNacks() {
        return statTotalRequestNacks;
    }

    public long getInRequestReplies() {
        return statTotalRequestReplies;
    }

    public void resetStats() {
        statTrades = 0L;
        statTotalRequestAcks = 0L;
        statTotalRequestNacks = 0L;
        statTotalRequestReplies = 0L;
        statFailedRequestAcks = 0L;
        statFailedRequestNacks = 0L;
        statFailedRequestReplies = 0L;
        statExceptions = 0L;
        statLastStartup = DateService.newLocalDate();
        statStart = DateService.newLocalDate();
    }

    /**
     * @return the pendExpiration
     */
    public long getPendExpiration() {
        return pendExpiration;
    }

    /**
     * @param pendExpiration
     *            the pendExpiration to set
     */
    public void setPendExpiration(long pendExpiration) {
        this.pendExpiration = pendExpiration;
    }

    /**
     * @return the internalMarketMaker
     */
    public String getInternalMarketMaker() {
        return internalMarketMaker;
    }

    /**
     * @param internalMarketMaker
     *            the internalMarketMaker to set
     */
    public void setInternalMarketMaker(String internalMarketMaker) {
        this.internalMarketMaker = internalMarketMaker;
    }

    /**
     * @return the marketFinder
     */
    public MarketFinder getMarketFinder() {
        return marketFinder;
    }

    /**
     * @param marketFinder
     *            the marketFinder to set
     */
    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    public boolean isConnected() {
        return statConnected;
    }

    // statistics
    public long getUptime() {
        if (statLastStartup == null) {
            return 0L;
        }
        return (new Date().getTime() - statLastStartup.getTime());
    }

    public long getNumberOfExceptions() {
        return statExceptions;
    }

    private void checkBuySideConnection() throws BestXException {
        if (!isBuySideConnectionAvailable()) {
            throw new MarketNotAvailableException("Internal buy side connection is off");
        }
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("Internal buy side connection is disabled");
        }
    }

    public void init() throws BestXException {
        checkPreRequisites();
        connectionHelper.setConnection(cmfConnection);
        connectionHelper.setConnectionListener(this);
    }

    @Override
    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    @Override
    public void onConnection(Connection source) {
        LOGGER.info("CMF Connected");
        statConnected = true;
        if (connectionListener != null) {
            connectionListener.onConnection(this);
        }
    }

    @Override
    public void onDisconnection(Connection source, String reason) {
        LOGGER.info("CMF Disconnected");
        statConnected = false;
        if (connectionListener != null) {
            connectionListener.onDisconnection(source, reason);
        }
    }

    @Override
    public void connect() throws BestXException {
        LOGGER.info("Connecting to CMF");
        connectionHelper.connect();
    }

    @Override
    public void disconnect() throws BestXException {
        LOGGER.info("Disconnecting from CMF");
        connectionHelper.disconnect();
    }

    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
        return true;
    }

    private void addMarketExecutionToOperation(Operation operation, String ticketNum, String marketMaker, BigDecimal price) throws BestXException {
        MarketExecutionReport marketFill = new MarketExecutionReport();
        marketFill.setId(Long.getLong(ticketNum));
        marketFill.setTicket(ticketNum);
        marketFill.setAveragePrice(price);
        marketFill.setExecBroker(operation.getOrder().getInstrument().getInstrumentAttributes().getPortfolio().getDescription());
        marketFill.setAccount(operation.getOrder().getCustomer().getFixId());
        marketFill.setActualQty(operation.getOrder().getQty());
        marketFill.setInstrument(operation.getOrder().getInstrument());
        marketFill.setLastPx(price);
        marketFill.setPrice(operation.getOrder().getLimit());
        marketFill.setSide(operation.getOrder().getSide());
        marketFill.setMarket(marketFinder.getMarketByCode(MarketCode.INTERNALIZZAZIONE, null));
        List<MarketExecutionReport> marketExecutionReports = new ArrayList<MarketExecutionReport>();
        marketExecutionReports.add(marketFill);
        operation.getLastAttempt().setMarketExecutionReports(marketExecutionReports);
    }

    @Override
    public void onRequestReply(final String orderId, final int errorCode, final String errorMessage) {
        LOGGER.info("Reply from CMF interface for order: {} - error: {}", orderId, errorCode);

        statTotalRequestReplies++;
        // If the CMF replies with a -3 it means that there is no connection to the Bloomberg server, we've to manage this event and put the
        // order in the WarningState
        if (errorCode == -3) {
            try {
                final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
                // In this situation the order must be allowed to try to execute on the internal market, so we reset the internalized flag
                operation.setInternalized(false);
                LOGGER.debug("Forward event to operation in new thread, received a reply with an error code = " + errorCode);
                executor.execute(new Runnable() {
                    public void run() {
                        operation.onCmfErrorReply(InternalMarket.this, errorMessage, errorCode);
                    }
                });
            } catch (OperationNotExistingException e) {
                LOGGER.error("An CMF autoexecution without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
            } catch (BestXException e) {
                statExceptions++;
                LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
            }
        } else if (orderId == null) {
            LOGGER.error("Reply without Order ID arrived! Discard it.");
            statFailedRequestReplies++;
            return;
        }
    }

    @Override
    public void onRequestAck(final String orderId, String ticketNum, String traderId) {
        LOGGER.info("Acknowledge from CMF interface for order: {}", orderId);
        statTotalRequestAcks++;
        if (orderId == null) {
            LOGGER.error("Acknowledge without Order ID arrived! Discard it.");
            statFailedRequestAcks++;
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
            // iff the operation has a market execution (i.e. a trade has been sent, and this is not a request ack on a sent ORDER
            if (!(operation.getLastAttempt().getMarketExecutionReports() == null || operation.getLastAttempt().getMarketExecutionReports().size() == 0)) {
                // this information was not available when the trade was sent
                operation.getLastAttempt().getMarketExecutionReports().get(0).setTicket(ticketNum);
            }
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleTradeAcknowledged(InternalMarket.this);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF autoexecution without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }

    }

    @Override
    public void onRequestNack(final String orderId) {
        LOGGER.info("Not-acknowledge from CMF interface for order: " + orderId);
        statTotalRequestNacks++;
        if (orderId == null) {
            LOGGER.error("Not-acknowledge without Order ID arrived! Discard it.");
            statFailedRequestNacks++;
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleTradeNotAcknowledged(InternalMarket.this);
                }
            });
        } catch (OperationNotExistingException e) {
            statFailedRequestNacks++;
            LOGGER.error("An CMF not-acknowledge without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statFailedRequestNacks++;
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    /**
     * @param source
     *            the operation requesting this order
     * @param trader
     *            the trader to be contacted with this order: if trader is null, no traderName is specified to the CMF, allowing the order
     *            to be sent to the configured book in Bloomberg
     * @param order
     *            the requested order
     */
    public void sendOrderForAutoExecution(Operation source, Trader trader, Order order) throws BestXException {
        String orderId = source.getIdentifier(OperationIdType.ORDER_ID);
        LOGGER.info("Send Trade to CMF - {}", order);
        long tsn;
        Money price = order.getLimit();
        tsn = serialNumberService.getSerialNumber("BTS_TSN", DateService.newLocalDate());
        operationRegistry.bindOperation(source, OperationIdType.BTS_TSN, Long.toString(tsn));
        cmfConnection.sendRequest(Long.toString(tsn), trader == null ? null : trader.getTraderName(),
                order.getCustomer().getBbgName(), 
                order.getInstrument().getIsin(), order.getQty(), (price == null || BigDecimal.ZERO.compareTo(price.getAmount()) == 0) ? null : price.getAmount(), "2", "2", order.getSide(), orderId,
                "000" + pendExpiration, null, sourceCode, null); // ticket number
    }

    public void sendOrderPending(Operation source, Trader trader, Order order) throws BestXException {
        String orderId = source.getIdentifier(OperationIdType.ORDER_ID);
        LOGGER.info("Send Trade to CMF - {}", order);
        long tsn;
        Money price = order.getLimit();
        tsn = serialNumberService.getSerialNumber("BTS_TSN", DateService.newLocalDate());
        operationRegistry.bindOperation(source, OperationIdType.BTS_TSN, Long.toString(tsn));
        cmfConnection.sendRequest(Long.toString(tsn), trader == null ? null : trader.getTraderName(), order.getCustomer().getBbgName(), order.getInstrument().getIsin(), order.getQty(),
                (BigDecimal.ZERO.compareTo(price.getAmount()) == 0) ? null : price.getAmount(), "4", "4", order.getSide(), orderId, "000" + pendExpiration, null, sourceCode, null);
    }

    // Unimplemented methods - Not necessary for TDS
    public void onAutoExecution(String orderId, String traderId, String pendingTicket, String btsTicket, BigDecimal price) {
        LOGGER.info("auto execution event received for order: " + orderId);
        if (orderId == null) {
            LOGGER.error("AutoExecution without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BTS_TSN, orderId);

            if (operation.getLastAttempt() != null) {
                if (operation.getLastAttempt().getMarketOrder() != null) {
                    operation.getLastAttempt().getMarketOrder().setVenue(this.getInternalVenue());
                } else {
                    LOGGER.error("Problems setting the venue in the market order : the market order is null");
                }
            } else {
                LOGGER.error("Problems setting the venue in the market order : the last attempt is null");
            }

            addMarketExecutionToOperation(operation, btsTicket, internalMarketMaker, operation.getLastAttempt().getExecutionProposal().getPrice().getAmount());
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleOrderAutoExecution(InternalMarket.this);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF autoexecution without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    public void onPendingAccept(String orderId, String traderId, String pendingTicket, String btsTicket, BigDecimal price) {
        LOGGER.info("Pending accept event received for order: " + orderId);
        if (orderId == null) {
            LOGGER.error("pending accept without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BTS_TSN, orderId);
            BigDecimal execPrice = price;
            if (execPrice == null) {
                execPrice = operation.getLastAttempt().getExecutionProposal().getPrice().getAmount();
            }

            addMarketExecutionToOperation(operation, btsTicket, internalMarketMaker, execPrice);
            // operation.getLastAttempt().getMarketExecutionReports().get(0).setPrice(new Money(operation.getOrder().getCurrency(), price));
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleOrderPendingAccepted(InternalMarket.this);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF pending accept without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    public void onPendingCounter(final String orderId, final String traderId, final String pendingTicket, final String btsTicket, final BigDecimal price, final String side) {
        LOGGER.info("Pending counter event received for order: " + orderId + " new price: " + price);
        if (orderId == null) {
            LOGGER.error("pending counter without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BTS_TSN, orderId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    ClassifiedProposal proposal = new ClassifiedProposal();
                    Instrument instrument = operation.getOrder().getInstrument();
                    Trader trader = new Trader();
                    trader.setTraderName(traderId);
                    trader.setTraderId(traderId);
                    proposal.setTrader(trader);
                    MarketMaker marketMaker = null;
                    MarketMarketMaker marketMarketMaker = null;
                    try {
                        marketMaker = new MarketMaker();
                        marketMaker.setCode(internalMarketMaker);
                        marketMarketMaker = new MarketMarketMaker();
                        marketMarketMaker.setMarket(marketFinder.getMarketByCode(MarketCode.INTERNALIZZAZIONE, null));
                        marketMarketMaker.setMarketMaker(marketMaker);
                        marketMarketMaker.setMarketSpecificCode(traderId);
                    } catch (BestXException e) {
                        LOGGER.error("Error while finding Market Maker with Internal code: " + traderId + " : " + e.getMessage(), e);
                        return;
                    }
                    Venue venue = null;
                    try {
                        venue = venueFinder.getMarketMakerVenue(marketMaker);
                        proposal.setMarket(marketFinder.getMarketByCode(MarketCode.INTERNALIZZAZIONE, null));
                    } catch (BestXException e) {
                        LOGGER.error("Error while finding Venue for Market Maker: " + marketMaker + " internal market." + " : " + e.getMessage(), e);
                        return;
                    }
                    proposal.setProposalState(ProposalState.NEW);
                    proposal.setSide("2".equalsIgnoreCase(side) ? Proposal.ProposalSide.ASK : Proposal.ProposalSide.BID);
                    proposal.setType(Proposal.ProposalType.COUNTER);
                    proposal.setPrice(new Money(instrument.getCurrency(), price));
                    proposal.setQty(operation.getOrder().getQty());
                    proposal.setMarketMarketMaker(marketMarketMaker);
                    proposal.setVenue(venue);
                    proposal.setFutSettDate(operation.getOrder().getFutSettDate());
                    proposal.setNonStandardSettlementDateAllowed(true);
                    proposal.setSenderQuoteId(btsTicket);
                    operation.onTradingConsoleOrderPendingCounter(InternalMarket.this, proposal);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF pending accept without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    private Venue getInternalVenue() {
        MarketMaker marketMaker = null;
        marketMaker = new MarketMaker();
        marketMaker.setCode(internalMarketMaker);
        Venue venue = null;
        try {
            venue = venueFinder.getMarketMakerVenue(marketMaker);
        } catch (BestXException e) {
            LOGGER.error("Error while finding Venue for Market Maker: " + marketMaker + " internal market." + " : " + e.getMessage(), e);
            return null;
        }
        return venue;
    }

    public void onPendingExpire(String orderId, String traderId) {
        LOGGER.info("Pending expire event received for order: " + orderId);
        if (orderId == null) {
            LOGGER.error("pending accept without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BTS_TSN, orderId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleOrderPendingExpired(InternalMarket.this);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF pending expire without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    public void onPendingReject(String orderId, String traderId, final String reason) {
        LOGGER.info("Pending reject event received for order: " + orderId);
        if (orderId == null) {
            LOGGER.error("pending accept without Order ID arrived! Discard it.");
            return;
        }
        try {
            final Operation operation = operationRegistry.getExistingOperationById(OperationIdType.BTS_TSN, orderId);
            LOGGER.debug("Forward event to operation in new thread");
            executor.execute(new Runnable() {
                public void run() {
                    operation.onTradingConsoleOrderPendingRejected(InternalMarket.this, reason);
                }
            });
        } catch (OperationNotExistingException e) {
            LOGGER.error("An CMF pending reject without valid order was found: order id (" + orderId + ")." + " : " + e.getMessage(), e);
        } catch (BestXException e) {
            statExceptions++;
            LOGGER.error("Error occurred while retrieving operation by order id (" + orderId + ")." + " : " + e.getMessage(), e);
        }
    }

    public void sendTrade(Operation source, Trader trader, String btsAcc, Order order, Money price, String tsn, String orderId, String status, String reisChoice) throws BestXException {
        LOGGER.info("Send Trade to CMF - {}", order);
        statTrades++;
        operationRegistry.bindOperation(source, OperationIdType.BTS_TSN, tsn);

        cmfConnection.sendRequest(tsn, trader.getTraderName(), btsAcc, order.getInstrument().getIsin(), order.getQty(), price.getAmount(), status, reisChoice, order.getSide(), orderId, "000"
                + pendExpiration, order.getFutSettDate(), sourceCode, null); // ticket number
    }

    @Override
    public void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        LOGGER.info("acceptProposal from InternalMarket. proposal trader: " + proposal.getTrader());
        checkBuySideConnection();
        Order order = listener.getOrder();
        Customer customer = order.getCustomer();
        addMarketExecutionToOperation(listener, listener.getIdentifier(OperationIdType.BTS_TSN), internalMarketMaker, proposal.getPrice().getAmount());
        if (isBuySideConnectionAvailable()) {
            this.sendTrade(listener, proposal.getTrader(), customer.getBbgName(), order, proposal.getPrice(), listener.getIdentifier(OperationIdType.BTS_TSN),
                    listener.getIdentifier(OperationIdType.ORDER_ID), "1", "2");
        } else {
            LOGGER.error("Internal Market: Buy Side Market not available when sending proposalAccept.");
        }
    }

    @Override
    public boolean isPriceConnectionAvailable() {
        return true;
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return isConnected();
    }

    @Override
    public boolean isBuySideConnectionProvided() {
        return true;
    }

    @Override
    public boolean isPriceConnectionProvided() {
        return false;
    }

    @Override
    public void matchOperations(Operation listener, Operation matching, Money ownPrice, Money matchingPrice) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        LOGGER.info("Reject proposal from Internal Market. Proposal Trader : " + proposal.getTrader());
        // TODO aggiungere il messaggio di reject per CMF
        checkBuySideConnection();
    }

    @Override
    public void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException {
        // to use when no price has been detected
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        marketOrder.setVenue(this.getInternalVenue());
        // always sending for Autoexecution, though the trader can bypass this configuring always pending for my order
        // trader set to null to let Bloomberg choose the correct one
        LOGGER.info("sendSubjectOrder from Internal market: {}", marketOrder);
        checkBuySideConnection();
        this.sendOrderForAutoExecution(listener, null, marketOrder);
    }

    @Override
    public void startBuySideConnection() throws BestXException {
        if (!isConnected()) {
            this.connect();
        }
    }

    @Override
    public void stopBuySideConnection() throws BestXException {
        if (isConnected()) {
            this.disconnect();
        }
    }

    /**
     * @return the marketMakerFinder
     */
    public MarketMakerFinder getMarketMakerFinder() {
        return marketMakerFinder;
    }

    /**
     * @param marketMakerFinder
     *            the marketMakerFinder to set
     */
    public void setMarketMakerFinder(MarketMakerFinder marketMakerFinder) {
        this.marketMakerFinder = marketMakerFinder;
    }

    /**
     * @return the venueFinder
     */
    public VenueFinder getVenueFinder() {
        return venueFinder;
    }

    /**
     * @param venueFinder
     *            the venueFinder to set
     */
    public void setVenueFinder(VenueFinder venueFinder) {
        this.venueFinder = venueFinder;
    }

    /**
     * @return the cmfConnection
     */

    public String getConnectionName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.INTERNALIZZAZIONE;
    }

    @Override
    public MarketBuySideConnection getBuySideConnection() {
        return this;
    }

    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();

    }

    /**
     * @return the connectionHelper
     */
    public ConnectionHelper getConnectionHelper() {
        return connectionHelper;
    }

    /**
     * @param connectionHelper
     *            the connectionHelper to set
     */
    public void setConnectionHelper(ConnectionHelper connectionHelper) {
        this.connectionHelper = connectionHelper;
    }

    /**
     * @return the connectionListener
     */
    public ConnectionListener getConnectionListener() {
        return connectionListener;
    }

    /**
     * @return the btsAccount
     */
    public String getBtsAccount() {
        return btsAccount;
    }

    /**
     * @param btsAccount
     *            the btsAccount to set
     */
    public void setBtsAccount(String btsAccount) {
        this.btsAccount = btsAccount;
    }

    /**
     * @param operationRegistry
     *            the operationRegistry to set
     */
    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * @param customerFinder
     *            the customerFinder to set
     */
    public void setCustomerFinder(CustomerFinder customerFinder) {
        this.customerFinder = customerFinder;
    }

    /**
     * @param instrumentFinder
     *            the instrumentFinder to set
     */
    public void setInstrumentFinder(InstrumentFinder instrumentFinder) {
        this.instrumentFinder = instrumentFinder;
    }

    /**
     * @param cmfConnection
     *            the cmfConnection to set
     */
    public void setCmfConnection(CmfConnection cmfConnection) {
        this.cmfConnection = cmfConnection;
        this.cmfConnection.setListener(this);
        this.cmfConnection.setConnectionListener(connectionHelper);
    }

    /**
     * @param serialNumberService
     *            the serialNumberService to set
     */
    public void setSerialNumberService(SerialNumberService serialNumberService) {
        this.serialNumberService = serialNumberService;
    }

    /**
     * @param sourceCode
     *            the sourceCode to set
     */
    public void setSourceCode(int sourceCode) {
        this.sourceCode = sourceCode;
    }

    /**
     * @return the executor
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * @param executor
     *            the executor to set
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void sendTrade(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendTradeChange(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId, String ticketNum)
            throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendTradeDelete(Operation source, Trader trader, OrderSide orderSide, Instrument instrument, BigDecimal qty, Date futSettDate, Money price, String tsn, String orderId, String ticketNum)
            throws BestXException {
        throw new UnsupportedOperationException();
    }

    public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    public int countOrders() {
        int result = -1;
        return result;
    }

    public double getAverageOrderResponseTimeInMillis() {
        return 0;
    }

    public double getAveragePricesResponseTimeInMillis() {
        return 0;
    }

    @Override
    public boolean isAMagnetMarket() {
        return false;
    }

    @Override
    public void revokeOrder(Operation listener, MarketOrder marketOrder, String reason, long sendOrderCancelTimeout) throws BestXException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanBook() {
        SimpleMarketProposalAggregator.getInstance().clearBooks();
    }

    @Override
    public void ackProposal(Operation listener, Proposal proposal) throws BestXException {
        // TODO Auto-generated method stub
        
    }

	@Override
	public int getActiveTimersNum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public StatisticsSnapshot getPriceDiscoveryTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatisticsSnapshot getOrderResponseTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getExecutionCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getUnexecutionCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getExecutionVolume() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getExecutionRatio() {
		// TODO Auto-generated method stub
		return 0;
	}

}
