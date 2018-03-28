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
package it.softsolutions.bestx.services.pricediscovery;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.ThreadPoolExecutor;
import it.softsolutions.bestx.model.AggregatedBook;
import it.softsolutions.bestx.model.Book;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Instrument;
import it.softsolutions.bestx.model.Proposal.ProposalSide;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-common 
 * First created by: davide.rossoni 
 * Creation date: 16/ago/2013 
 * 
 **/
public class ProposalAggregator {
    
    private String isin;
    private Book book;
    private List<ProposalAggregatorListener> proposalAggregatorListeners = new CopyOnWriteArrayList<ProposalAggregatorListener>();
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor();
    Instrument instrument;
    
    static {
        threadPoolExecutor.setCorePoolSize(64);
        threadPoolExecutor.setMaxPoolSize(128);
        threadPoolExecutor.setThreadNamePrefix("ProposalAggregator");
        threadPoolExecutor.initialize();
    }
    
    public ProposalAggregator(Instrument instrument) {
        this.isin = instrument.getIsin();
        this.instrument = instrument;
        initializeBook();
    }
    
    public void initializeBook() {
    	this.book = new AggregatedBook(instrument);
    }
    
    public String getIsin() {
        return isin;
    }

    public Book getBook() {
        return book.clone();
    }

    public void addProposalAggregatorListener(ProposalAggregatorListener proposalAggregatorListener) {
        if (proposalAggregatorListeners.isEmpty()) {
        	initializeBook();
        }
        proposalAggregatorListeners.add(proposalAggregatorListener);
    }

    public void removeProposalAggregatorListener(ProposalAggregatorListener proposalAggregatorListener) {
        proposalAggregatorListeners.remove(proposalAggregatorListener);
    }

    public void onProposal(final ClassifiedProposal classifiedProposal) throws BestXException {
        // Add proposal to this book
        book.addProposal(classifiedProposal);
                
        // Notify the listeners
        threadPoolExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				ProposalSide side = classifiedProposal.getSide();
		        String marketMaker = classifiedProposal.getMarketMarketMaker().getMarketSpecificCode();
		        
				Iterator<ProposalAggregatorListener> iter = proposalAggregatorListeners.iterator();
		        while (iter.hasNext()) {
		            iter.next().onProposal(side, marketMaker);
		        }
			}
		});
    }

    public ProposalAggregatorListener getProposalAggregatorListener(String orderID) {
        
        Iterator<ProposalAggregatorListener> iter = proposalAggregatorListeners.iterator();
        while (iter.hasNext()) {
            ProposalAggregatorListener proposalAggregatorListener = (ProposalAggregatorListener) iter.next();
            
            if (proposalAggregatorListener.getOrderID().equals(orderID)) {
                return proposalAggregatorListener;
            }
        }
        
        return null;
    }

    public Collection<ProposalAggregatorListener> getProposalAggregatorListeners() {
    	return proposalAggregatorListeners;
    }

}
