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
package it.softsolutions.bestx.connections.ib4j;

import it.softsolutions.bestx.Operation;
import it.softsolutions.bestx.OperationIdType;
import it.softsolutions.bestx.services.DateService;
import it.softsolutions.ib4j.clientserver.IBcsMessage;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-protocol-api First created by: davide.rossoni Creation date: 19/ott/2012
 * 
 **/
public class NewOperationMessage extends IBcsMessage {
    private static final long serialVersionUID = 7667282875160147707L;


    public NewOperationMessage(Operation operation) {
        // AMC 20101004 Nota che stato gestione viene usato e modificato esclusivamente dalla Web Application

        setIBPubSubSubject(IB4JOperatorConsoleMessage.NOTIFY_OP_INSERT);
        setStringProperty(IB4JOperatorConsoleMessage.FLD_ORDER_ID, operation.getIdentifier(OperationIdType.ORDER_ID));
        setStringProperty(IB4JOperatorConsoleMessage.FLD_RECEIVE_TIME, DateService.format(DateService.timeISO, operation.getState().getEnteredTime()));
        setIntProperty(IB4JOperatorConsoleMessage.FLD_REVOKED, operation.getRevocationState().ordinal());
        
        if (operation.getLastAttempt() != null && operation.getLastAttempt().getExecutionProposal() != null) {
            setStringProperty(IB4JOperatorConsoleMessage.FLD_MARKET_NAME, operation.getLastAttempt().getExecutionProposal().getMarket().getName());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_TRADER, operation.getLastAttempt().getExecutionProposal().getMarketMarketMaker().getMarketMaker().getCode());
        }
        
        if (operation.getOrder() != null && operation.getOrder().getInstrument() != null) {
            setStringProperty(IB4JOperatorConsoleMessage.FLD_ISIN, operation.getOrder().getInstrument().getIsin());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_INSTRUMENT_DESCRIPTION, operation.getOrder().getInstrument().getDescription());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_CUSTOMER_CODE, operation.getOrder().getCustomer().getName());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_SIDE, operation.getOrder().getSide().name());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_CURRENCY, operation.getOrder().getCurrency());
            setDoubleProperty(IB4JOperatorConsoleMessage.FLD_QTY, operation.getOrder().getQty().doubleValue());
            setStringProperty(IB4JOperatorConsoleMessage.FLD_SETTLEMENT_DATE, DateService.format(DateService.dateISO, operation.getOrder().getFutSettDate()));
            
            if (operation.getOrder().getLimit() != null) {
                setDoubleProperty(IB4JOperatorConsoleMessage.FLD_ORDER_PRICE, operation.getOrder().getLimit().getAmount().doubleValue());
            }
        }
    }

}
