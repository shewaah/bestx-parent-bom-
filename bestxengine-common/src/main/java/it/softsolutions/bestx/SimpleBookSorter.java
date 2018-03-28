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
package it.softsolutions.bestx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import it.softsolutions.bestx.bestexec.BookSorter;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Quote;
import it.softsolutions.bestx.model.SortedBook;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine-common 
* First created by: davide.rossoni 
* Creation date: 19/feb/2013 
* 
**/
public class SimpleBookSorter implements BookSorter {
    
    @Override
	public SortedBook getSortedBook(ClassifiedBook classifiedBook) {
        SortedBook sortedBook = new SortedBook();
        sortedBook.setInstrument(classifiedBook.getInstrument());
        sortedBook.setAskProposals(sortProposals(classifiedBook.getAskProposals(), new Comparator<ClassifiedProposal>() {
            @Override
			public int compare(ClassifiedProposal proposal1, ClassifiedProposal proposal2) {
                return proposal1.getPrice().getAmount().compareTo((proposal2).getPrice().getAmount()); // lower price ranks first
            }
        }));
        sortedBook.setBidProposals(sortProposals(classifiedBook.getBidProposals(), new Comparator<ClassifiedProposal>() {
            @Override
			public int compare(ClassifiedProposal proposal1, ClassifiedProposal proposal2) {
                return proposal2.getPrice().getAmount().compareTo((proposal1).getPrice().getAmount()); // higher price ranks first
            }
        }));
        // Create the best quote
        Quote bestQuote = new Quote();
        bestQuote.setInstrument(sortedBook.getInstrument());
        if (sortedBook.getAskProposals().size() > 0 && sortedBook.getBidProposals().size() > 0) {
            bestQuote.setAskProposal(sortedBook.getAskProposals().get(0));
            bestQuote.setBidProposal(sortedBook.getBidProposals().get(0));
            bestQuote.setFutSettDate(bestQuote.getAskProposal().getFutSettDate()); // they should always be equal
            if (bestQuote.getAskProposal().getExpiration() != null && bestQuote.getBidProposal().getExpiration() != null) {
                if (bestQuote.getAskProposal().getExpiration().compareTo(bestQuote.getBidProposal().getExpiration()) < 0) {
                    bestQuote.setExpiration(bestQuote.getAskProposal().getExpiration());
                } else {
                    bestQuote.setExpiration(bestQuote.getBidProposal().getExpiration());
                }
            }
        }
        return sortedBook;
    }

    private List<ClassifiedProposal> sortProposals(Collection<? extends ClassifiedProposal> proposals, Comparator<ClassifiedProposal> comparator) {
        ArrayList<ClassifiedProposal> classifiedProposals = new ArrayList<ClassifiedProposal>();
        classifiedProposals.addAll(proposals);
        Collections.sort(classifiedProposals, comparator);
        return classifiedProposals;
    }
}
