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
package it.softsolutions.bestx.dao;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Proposal;
import it.softsolutions.bestx.model.SortedBook;

import java.util.List;

/**  
*
* Purpose: this class is mainly for ...  
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface ProposalDao {
	/**
	 * Get Proposal from book sorted
	 * @param sortedBookId
	 * @param side
	 * @return List of <ClassifiedProposal> 
	 */
    List<ClassifiedProposal> getSortedBookProposals(Long sortedBookId, Proposal.ProposalSide side);
   
    /**
     * Get Proposal by ID
     * @param id
     * @return proposal
     */
    Proposal getProposalById(Long id);
    
    /**
     * Save SortedBook proposal by bookId
     * @param sortedBook
     * @param sortedBookId
     */
    void saveSortedBookProposals(SortedBook sortedBook, Long sortedBookId);
    
    /**
     * Delete sortedBook
     * @param sortedBook
     */
    void deleteSortedBookProposals(SortedBook sortedBook);
    
    /**
     * Delete proposal
     * @param proposal
     */
    void deleteProposal(Proposal proposal);
    
    /**
     * Save proposal
     * @param proposal
     */
    void saveProposal(Proposal proposal);
}
