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
package it.softsolutions.bestx.exceptions;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.SortedBook;

import java.util.Iterator;
import java.util.List;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 26/ott/2012 
* 
**/
public class SaveBookException extends Exception {

    private static final long serialVersionUID = 5400138582876414367L;

    private String orderId;
    private int attemptNo;
    private SortedBook sortedBook;
    private int loopIndex;
    private Exception originalExc;

    public SaveBookException(String message, String orderId, int attemptNo, SortedBook sortedBook, int loopIndex, Exception originalExc) {
        super(message);
        this.orderId = orderId;
        this.attemptNo = attemptNo;
        this.sortedBook = sortedBook;
        this.loopIndex = loopIndex;
        this.originalExc = originalExc;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getAttemptNo() {
        return attemptNo;
    }

    public SortedBook getSortedBook() {
        return sortedBook;
    }

    public int getLoopIndex() {
        return loopIndex;
    }

    public String toString() {
        String result = "[SAVEBOOKERR],OrderId=" + getOrderId() + ",AttemptNo=" + getAttemptNo() + ",ErrorIndex=" + getLoopIndex() + ",";

        if (sortedBook == null) {
            result += " THE BOOK IS EMPTY, NULL RECEIVED ";
        } else {
            result += "BookSide=BID,Size=" + (sortedBook.getBidProposals() != null ? sortedBook.getBidProposals().size() : null);
            result += ",BookSide=ASK,Size=" + (sortedBook.getAskProposals() != null ? sortedBook.getAskProposals().size() : null);
            result += ",BookBID:";
            result += printBookSide(sortedBook.getBidProposals());
            result += ",BookASK:";
            result += printBookSide(sortedBook.getAskProposals());
        }
        return result;
    }

    private String printBookSide(List<ClassifiedProposal> bookSide) {
        String side = "";
        Iterator<ClassifiedProposal> iterator = bookSide.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            ClassifiedProposal cprop = iterator.next();
            side += ",STATE" + count + "=" + (cprop.getProposalState() == null ? "" : cprop.getProposalState().name());
            side += ",REASON" + count + "=" + (cprop.getReason() == null ? "" : cprop.getReason());
            side += ",MARKET" + count + "=" + (cprop.getMarket() == null ? "" : cprop.getMarket().getName());
            side += ",VENUE" + count + "=" + (cprop.getVenue() == null ? "" : cprop.getVenue().getCode());
            side += ",MM"
                    + count
                    + "="
                    + ((cprop.getMarketMarketMaker() != null && cprop.getMarketMarketMaker().getMarketMaker() != null) ? cprop.getMarketMarketMaker() + "/"
                            + cprop.getMarketMarketMaker().getMarketMaker().getName() : "");
            side += ",PRICE" + count + "=" + (cprop.getPrice() == null ? "" : cprop.getPrice().getAmount().toPlainString());
            side += ",QTY" + count + "=" + (cprop.getQty() == null ? "" : cprop.getQty().toPlainString());
            side += ",SIDE" + count++ + "=" + (cprop.getSide() == null ? "" : cprop.getSide().getFixCode());
        }
        return side;
    }

    public Exception getOriginalExc() {
        return originalExc;
    }
}
