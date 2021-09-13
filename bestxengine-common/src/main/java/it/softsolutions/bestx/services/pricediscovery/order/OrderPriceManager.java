/*
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
package it.softsolutions.bestx.services.pricediscovery.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.connections.MarketPriceConnectionListener;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.Market.MarketCode;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregator;
import it.softsolutions.bestx.services.pricediscovery.ProposalAggregatorListener;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: davide.rossoni 
 * Creation date: 16/ago/2013 
 * 
 **/
public class OrderPriceManager implements ProposalAggregatorListener {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(OrderPriceManager.class);
    private String orderID;
    private MarketCode marketCode;
    private ProposalAggregator proposalAggregator;
    private MarketPriceConnectionListener marketPriceConnectionListener;
    
    private List<String> marketMakers = new ArrayList<String>(64);
    private List<String> missingMarketMakersBid = new CopyOnWriteArrayList<String>();
    private List<String> missingMarketMakersAsk = new CopyOnWriteArrayList<String>();
    
    public OrderPriceManager(String orderID, MarketCode marketCode, List<String> marketMakers, ProposalAggregator proposalAggregator, MarketPriceConnectionListener marketPriceConnectionListener) {
        super();
        this.orderID = orderID;
        this.marketCode = marketCode;
        this.proposalAggregator = proposalAggregator;
        this.marketPriceConnectionListener = marketPriceConnectionListener;
        
        this.marketMakers.addAll(marketMakers);
        this.missingMarketMakersBid.addAll(marketMakers);
        this.missingMarketMakersAsk.addAll(marketMakers);
    }

    @Override
    public String getOrderID() {
        return orderID;
    }

    @Override
    public synchronized void onProposal(ProposalSide side, String marketMaker) {
    	// Add proposal to this book
        // 1. check book status
        switch (side) {
        case BID:
            if (missingMarketMakersBid.contains(marketMaker)) {
                missingMarketMakersBid.remove(marketMaker);
            }
            break;
        case ASK:
            if (missingMarketMakersAsk.contains(marketMaker)) {
                missingMarketMakersAsk.remove(marketMaker);
            }
            break;
        default:
            break;
        }
        
        if (missingMarketMakersBid.isEmpty() && missingMarketMakersAsk.isEmpty()) {
            // 2. retrieve book
            Book book = proposalAggregator.getBook();
        
            // 1. remove itself from the proposalAggregator 
            proposalAggregator.removeProposalAggregatorListener(this);
            
            
            filterProposals(book.getAskProposals());
            filterProposals(book.getBidProposals());
            LOGGER.debug("Actual book: {}", book);
            
//            // 2b. choose the best price between multiple market makers for the same bank [RR20140926-CRSBXTEM-127]
//            Map<String, Proposal> receivedProposals = new HashMap<String, Proposal>();
//            List<Proposal> allProposals = new ArrayList<Proposal>();
//            allProposals.addAll(book.getAskProposals());
//            allProposals.addAll(book.getBidProposals());
//            for (Proposal proposal : allProposals) {
//                if (proposal.getVenue() != null && proposal.getSide() != null && proposal.getPrice() != null) {
//                    String key = proposal.getVenue().getMarket().getName() + "#" + proposal.getVenue().getCode() + "#" + proposal.getSide();
//                    if (receivedProposals.containsKey(key)) {
//                        Proposal availableProp = receivedProposals.get(key);
//                        // Money compareTo : zero if values are equal, -1 if other is greater, 1 if other is smaller than the object
//                        // from a buyer point of view the lesser the ask price the better
//                        // from a seller point of view the higher the bid price the better
//                        if ((proposal.getSide().equals(ProposalSide.ASK) && proposal.getPrice().compareTo(availableProp.getPrice()) > 0) ||
//                            (proposal.getSide().equals(ProposalSide.BID) && proposal.getPrice().compareTo(availableProp.getPrice()) < 0)) {
//                            receivedProposals.put(key, proposal);
//                        }
//                    } else {
//                        receivedProposals.put(key, proposal);
//                    }
//                } else {
//                    LOGGER.warn("Proposal with venue or side or price null, it is not suitable for a book, we ignore it: {}", proposal);
//                    continue;
//                }
//            }
//            book.getAskProposals().clear();
//            book.getBidProposals().clear();
//            for(Proposal bestProposal : receivedProposals.values()) {
//                book.addProposal(bestProposal);
//            }
//            LOGGER.debug("Duplicated removed book: {}", book);
            
            // 3. notify the MarketPriceConnectionListener
            marketPriceConnectionListener.onMarketBookComplete(marketCode, book);
        }
    }

    private void filterProposals(Collection<? extends Proposal> proposals) {
        Iterator<? extends Proposal> askIter = proposals.iterator();
        while (askIter.hasNext()) {
            Proposal proposal = (Proposal) askIter.next();
            String marketMaker = proposal.getMarketMarketMaker().getMarketSpecificCode();
            if (!marketMakers.contains(marketMaker)) {
                proposals.remove(proposal);
            }
        }
    }

    @Override
    public synchronized void onTimerExpired() {
        // 1. retrieve book
        Book book = proposalAggregator.getBook();

        // 4. remove itself from the proposalAggregator 
        proposalAggregator.removeProposalAggregatorListener(this);
        
        // 2. retrieve marketMakers with price on both sides
        List<String> marketMakersOnBothSides = new ArrayList<String>();
        Iterator<String> iter = marketMakers.iterator();
        while (iter.hasNext()) {
            String marketMaker = (String) iter.next();
            if (!missingMarketMakersAsk.contains(marketMaker) && !missingMarketMakersBid.contains(marketMaker)) {
                marketMakersOnBothSides.add(marketMaker);
            }
        }

        // 3. notify the PriceService
        String reason = Messages.getString("PRICE_TIMEOUT.0");
        marketPriceConnectionListener.onMarketBookPartial(marketCode, book, reason, marketMakersOnBothSides);
        
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("OrderPriceManager [orderID=");
        builder.append(orderID);
        builder.append(", marketCode=");
        builder.append(marketCode);
        builder.append(", marketMakers=");
        builder.append(marketMakers != null ? marketMakers.subList(0, Math.min(marketMakers.size(), maxLen)) : null);
        builder.append("]");
        return builder.toString();
    }
    
}
