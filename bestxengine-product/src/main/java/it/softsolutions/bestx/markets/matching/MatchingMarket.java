package it.softsolutions.bestx.markets.matching;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.OperationRegistry;
import it.softsolutions.bestx.connections.MarketBuySideConnection;
import it.softsolutions.bestx.connections.MarketConnection;
import it.softsolutions.bestx.dao.TitoliIncrociabiliDao;
import it.softsolutions.bestx.exceptions.MarketNotAvailableException;
import it.softsolutions.bestx.exceptions.ObjectNotInitializedException;
import it.softsolutions.bestx.finders.MarketFinder;
import it.softsolutions.bestx.management.MarketMXBean;
import it.softsolutions.bestx.management.statistics.StatisticsSnapshot;
import it.softsolutions.bestx.model.ExecutionReport.ExecutionReportState;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Market;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.MarketExecutionReport;
import it.softsolutions.bestx.model.MarketMaker;
import it.softsolutions.bestx.model.MarketMarketMaker;
import it.softsolutions.bestx.model.MarketOrder;
import it.softsolutions.bestx.model.Order;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Rfq.OrderSide;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.bestx.services.price.SimpleMarketProposalAggregator;
import it.softsolutions.bestx.services.serial.SerialNumberService;
import it.softsolutions.jsscommon.Money;

public class MatchingMarket extends MarketConnection implements MarketBuySideConnection, OperationMatchingServerListener, MarketMXBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchingMarket.class);

    private OperationMatchingServer operationMatchingServer;
    private SerialNumberService executionReportIdServer;
    private Executor executor;
    private MarketFinder marketFinder;
    private Market matchingMarket;
    private TitoliIncrociabiliDao titoliIncrociabiliDao;
    private OperationRegistry operationRegistry;

    private void checkBuySideConnection() throws BestXException {
        if (!isBuySideConnectionAvailable()) {
            throw new MarketNotAvailableException("Matching market is off");
        }
        if (!isBuySideConnectionEnabled()) {
            throw new MarketNotAvailableException("Matching market is disabled");
        }
    }

    private void checkPreRequisites() throws ObjectNotInitializedException {
        if (executor == null) {
            throw new ObjectNotInitializedException("Executor not set");
        }
        if (marketFinder == null) {
            throw new ObjectNotInitializedException("Market finder not set");
        }
        if (executionReportIdServer == null) {
            throw new ObjectNotInitializedException("Execution Report Id server not set");
        }
        if (operationMatchingServer == null) {
            throw new ObjectNotInitializedException("Operation Matching server not set");
        }
        if (titoliIncrociabiliDao == null) {
            throw new ObjectNotInitializedException("Titoli Incrociabili Dao not set");
        }
        if (operationRegistry == null) {
            throw new ObjectNotInitializedException("Operation registry not set");
        }
    }

    public void init() throws BestXException {
        checkPreRequisites();
        matchingMarket = marketFinder.getMarketByCode(MarketCode.MATCHING, null);
    }

    public void setOperationMatchingServer(OperationMatchingServer operationMatchingServer) {
        this.operationMatchingServer = operationMatchingServer;
    }

    public void setExecutionReportIdServer(SerialNumberService executionReportIdServer) {
        this.executionReportIdServer = executionReportIdServer;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setMarketFinder(MarketFinder marketFinder) {
        this.marketFinder = marketFinder;
    }

    @Override
    public boolean isInstrumentTradableWithMarketMaker(Instrument instrument, MarketMarketMaker marketMaker) {
        return true;
    }

    public void acceptProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void rejectProposal(Operation listener, Instrument instrument, Proposal proposal) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void requestOrderStatus(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void revokeOrder(final Operation listener, final MarketOrder marketOrder, final String reason) throws BestXException {
        checkBuySideConnection();
        if (listener.getMatchingOperation() == null) { // Matching multipli
            try {
                final String matchId = titoliIncrociabiliDao.getMatchId(listener);
                operationMatchingServer.removeOperation(listener);
                List<String> matchingOrders = titoliIncrociabiliDao.getMatchOrdersList(matchId);
                for (final String orderId : matchingOrders) {
                    final Operation currentOperation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);

                    executor.execute(new Runnable() {
                        public void run() {
                            currentOperation.onMarketOrderReject(MatchingMarket.this, marketOrder, reason, matchId);
                        }
                    });
                }
            } catch (SQLException e) {
                LOGGER.error("Error while retrieve matching order list", e);
            }
        } else {
            executor.execute(new Runnable() {
                public void run() {
                    listener.getMatchingOperation().onMarketOrderReject(MatchingMarket.this, marketOrder, reason, null);
                }
            });
        }
    }

    public void sendFokOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendFasOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        throw new UnsupportedOperationException();
    }

    public void sendRfq(Operation listener, MarketOrder marketOrder) throws BestXException {
        sendSubjectOrder(listener, marketOrder);
    }

    public void sendSubjectOrder(Operation listener, MarketOrder marketOrder) throws BestXException {
        LOGGER.info("Send order to matching market " + marketOrder.toString());
        checkBuySideConnection();
        Operation operation = listener;
        operationMatchingServer.addOperation(operation);
    }

    public void onOperationsMatch(final Operation operationA, final Operation operationB) {
        LOGGER.debug("Operation match event: " + operationA + " <-> " + operationB);
        executor.execute(new Runnable() {
            public void run() {
                operationA.onMarketMatchFound(MatchingMarket.this, operationB);
            }
        });
        executor.execute(new Runnable() {
            public void run() {
                operationB.onMarketMatchFound(MatchingMarket.this, operationA);
            }
        });
    }

    public void onOperationsMatch(List<Operation> matchedOperations) {
        // Stefano 20080807 - Nuova gestione matching multipli
        LOGGER.debug("Operation match event: " + matchedOperations.toString());
        for (final Operation operation : matchedOperations) {
            executor.execute(new Runnable() {
                public void run() {
                    operation.onMarketMatchFound(MatchingMarket.this, null);
                }
            });
        }
    }

    public void matchOperations(final Operation listener, final Operation matching, final Money ownPrice, final Money matchingPrice) throws BestXException {
        LOGGER.debug("Operation match accepted: " + listener + " <-> " + matching);
        checkBuySideConnection();

        if (matching == null) {
            try {
                String matchId = titoliIncrociabiliDao.getMatchId(listener);
                List<String> matchingOrders = titoliIncrociabiliDao.getMatchOrdersList(matchId);
                for (final String orderId : matchingOrders) {
                    final Operation currentOperation = operationRegistry.getExistingOperationById(OperationIdType.ORDER_ID, orderId);
                    Money orderPrice;
                    if (currentOperation.getOrder().getSide() == OrderSide.BUY) { // ownPrice == BUY Price for multi matching
                        orderPrice = ownPrice;
                    } else {
                        orderPrice = matchingPrice;
                    }
                    final MarketExecutionReport listenerExecutionReport = createMarketExecutionReport(currentOperation.getOrder(), orderPrice);

                    executor.execute(new Runnable() {
                        public void run() {
                            currentOperation.onMarketExecutionReport(MatchingMarket.this, currentOperation.getOrder(), listenerExecutionReport);
                        }
                    });
                }
                titoliIncrociabiliDao.deleteMatching(matchId);
            } catch (SQLException e) {
                LOGGER.error("Error while retrieve matching order list", e);
            }
        } else {
            final MarketExecutionReport listenerExecutionReport = createMarketExecutionReport(listener.getOrder(), ownPrice);
            final MarketExecutionReport matchingExecutionReport = createMarketExecutionReport(matching.getOrder(), matchingPrice);
            executor.execute(new Runnable() {
                public void run() {
                    listener.onMarketExecutionReport(MatchingMarket.this, listener.getOrder(), listenerExecutionReport);
                }
            });
            executor.execute(new Runnable() {
                public void run() {
                    matching.onMarketExecutionReport(MatchingMarket.this, matching.getOrder(), matchingExecutionReport);
                }
            });
        }
    }

    private MarketExecutionReport createMarketExecutionReport(Order order, Money price) throws BestXException {
        MarketExecutionReport marketExecutionReport = new MarketExecutionReport();
        marketExecutionReport.setActualQty(order.getQty());
        marketExecutionReport.setInstrument(order.getInstrument());
        marketExecutionReport.setMarket(matchingMarket);
        marketExecutionReport.setOrderQty(order.getQty());
        marketExecutionReport.setPrice(price);
        marketExecutionReport.setLastPx(price.getAmount());
        marketExecutionReport.setSequenceId(Long.toString(executionReportIdServer.getSerialNumber("EXECUTION_REPORT")));
        marketExecutionReport.setSide(order.getSide());
        marketExecutionReport.setState(ExecutionReportState.FILLED);
        marketExecutionReport.setTransactTime(DateService.newLocalDate());
        return marketExecutionReport;
    }

    @Override
    public MarketBuySideConnection getBuySideConnection() {
        return this;
    }

    @Override
    public void startBuySideConnection() {
        operationMatchingServer.setOperationMatchingServerListener(this);
        operationMatchingServer.start();
    }

    @Override
    public void stopBuySideConnection() {
        operationMatchingServer.stop();
    }

    @Override
    public MarketCode getMarketCode() {
        return MarketCode.MATCHING;
    }

    @Override
    public boolean isBuySideConnectionAvailable() {
        return operationMatchingServer.isOn();
    }

    @Override
    public boolean isPriceConnectionAvailable() {
        return true;
    }

    @Override
    public boolean isBuySideConnectionProvided() {
        return true;
    }

    @Override
    public boolean isPriceConnectionProvided() {
        return false;
    }

    public MarketExecutionReport getMatchingTrade(Order order, Money executionPrice, MarketMaker marketMaker, Date minArrivalDate) {
        throw new UnsupportedOperationException();
    }

    public void setTitoliIncrociabiliDao(TitoliIncrociabiliDao titoliIncrociabiliDao) {
        this.titoliIncrociabiliDao = titoliIncrociabiliDao;
    }

    public void setOperationRegistry(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
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
	public int getActiveTimersNum(){
		// TODO Monitoring-BX
		return 1;
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
