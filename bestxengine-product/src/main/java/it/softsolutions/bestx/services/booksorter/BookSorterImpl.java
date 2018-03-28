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

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by:  
 * Creation date: 19-ott-2012 
 * 
 **/
package it.softsolutions.bestx.services.booksorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.softsolutions.bestx.bestexec.BookSorter;
import it.softsolutions.bestx.model.ClassifiedBook;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.SortedBook;

/**
 * The Class BookSorterImpl.
 */
public class BookSorterImpl implements BookSorter {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BookSorterImpl.class);

    /* (non-Javadoc)
     * @see it.softsolutions.bestx.bestexec.BookSorter#getSortedBook(it.softsolutions.bestx.model.ClassifiedBook)
     */
    @Override
	public SortedBook getSortedBook(ClassifiedBook classifiedBook) {
        SortedBook sortedBook = new SortedBook();
        sortedBook.setInstrument(classifiedBook.getInstrument());
        LOGGER.info("  -----------------------------------------------------------------------------------------");
        sortedBook.setAskProposals(sortProposals(classifiedBook.getAskProposals(), new ClassifiedAskComparator()));
        LOGGER.info("-----------------------------------------------------------------------------------------");
        sortedBook.setBidProposals(sortProposals(classifiedBook.getBidProposals(), new ClassifiedBidComparator()));
        LOGGER.info("  -----------------------------------------------------------------------------------------");
        return sortedBook;
    }
    private List<ClassifiedProposal> sortProposals(Collection<? extends ClassifiedProposal> proposals, Comparator<ClassifiedProposal> comparator) {
        ArrayList<ClassifiedProposal> classifiedProposals = new ArrayList<ClassifiedProposal>();
        classifiedProposals.addAll(proposals);
        try {
        Collections.sort(classifiedProposals, comparator);
    	for(int i = 0; i < classifiedProposals.size(); i++)
    		LOGGER.info("{}{}", i, classifiedProposals.get(i).toStringShort());
        } catch(IllegalArgumentException e) {
        	System.out.println(e.toString());
        	e.printStackTrace(System.out);
        }
        return classifiedProposals;
    }
}
