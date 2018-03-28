package it.softsolutions.bestx.connections;

import java.math.BigDecimal;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.OperationState;
import it.softsolutions.bestx.model.UserModel;
import it.softsolutions.jsscommon.Money;

public interface OperatorConsoleConnectionListener {

    void onOperatorRetryState(OperatorConsoleConnection source, String comment);

    void onOperatorTerminateState(OperatorConsoleConnection source, String comment);

    void onOperatorAbortState(OperatorConsoleConnection source, String comment);

    void onOperatorSuspendState(OperatorConsoleConnection source, String comment);

    void onOperatorRestoreState(OperatorConsoleConnection source, String comment);

    void onOperatorReinitiateProcess(OperatorConsoleConnection source, String comment);

    void onOperatorForceState(OperatorConsoleConnection source, OperationState newState, String comment);

    void onOperatorSendDDECommand(OperatorConsoleConnection source, String comment);

    void onOperatorExecuteState(OperatorConsoleConnection source, Money execPrice, String comment);

    void onOperatorMatchOrders(OperatorConsoleConnection source, Money ownPrice, Money matchingPrice, String comment);

    // add by Stefano -- 18/04/2008
    void onOperatorForceReceived(OperatorConsoleConnection source, String comment);

    void onOperatorAcceptOrder(OperatorConsoleConnection source, String comment);

    void onOperatorForceNotExecution(OperatorConsoleConnection source, String comment);

    void onOperatorManualManage(OperatorConsoleConnection source, String comment);

    void onOperatorManualExecution(OperatorConsoleConnection source, String comment, Money price, BigDecimal qty, String mercato, String marketMaker, Money prezzoRif);

    void onOperatorForceNotExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment);

    void onOperatorForceExecutedWithoutExecutionReport(OperatorConsoleConnection source, String comment);

    void onOperatorResendExecutionReport(OperatorConsoleConnection source, String comment);

    void onOperatorStopOrderExecution(OperatorConsoleConnection source, String comment, Money price);

    // added by LS -- 02/05/2008
    void onOperatorRevokeAccepted(OperatorConsoleConnection source, String comment);

    void onOperatorRevokeRejected(OperatorConsoleConnection source, String comment);

    // added by AMC 02/05/2008
    void onOperatorExceedFilterRecalculate(OperatorConsoleConnection source, String comment);

    void onOperatorMoveToNotExecutable(OperatorConsoleConnection source, String comment);

    // Toronto Dominion modification of executed orders - LS 20080519
    void onOperatorExecutedOperationModify(OperatorConsoleConnection source, BigDecimal priceAmount, BigDecimal qty, String sDate, String comment);

    void onOperatorExecutedOperationDelete(OperatorConsoleConnection source, String comment);

    void onOperatorSendDESCommand(OperatorConsoleConnection source, String comment);

    /**
     * The unreconciled trade has been matched manually by the dashboard operator, now we must save the trade data in the operation
     */
    void onOperatorUnreconciledTradeMatched(OperatorConsoleConnection source, BigDecimal executionPrice, String executionMarketMaker, String ticketNumber) throws BestXException;
    
    void onOperatorMoveToLimitFileNoPrice(OperatorConsoleConnection source, String comment);
    
    void onOperatorPriceDiscovery(OperatorConsoleConnection source);
   
    void onOperatorTakeOwnership(OperatorConsoleConnection source, UserModel userToAssign);
    
    void onRevoke();
}
