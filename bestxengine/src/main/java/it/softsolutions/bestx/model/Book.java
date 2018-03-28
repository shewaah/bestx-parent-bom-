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
package it.softsolutions.bestx.model;

import java.util.Collection;

/**  
*
* Purpose: this class is the basic class containig a not necessarily sorted, not necessarily classified set of proposals, with a bid and an ask side
*
* Project Name : bestxengine 
* First created by: davide.rossoni 
* Creation date: 20/feb/2013 
* 
**/
public interface Book extends Cloneable {
    
    Book clone();
    
    /**
     * @return
     */
    Instrument getInstrument();

    /**
     * @return
     */
    Collection<? extends Proposal> getProposals(Proposal.ProposalSide side);
    
    /**
     * @return
     */
    Collection<? extends Proposal> getAskProposals();

    /**
     * @return
     */
    Collection<? extends Proposal> getBidProposals();

    /**
     * @param proposal
     */
    void addProposal(Proposal proposal);
}
