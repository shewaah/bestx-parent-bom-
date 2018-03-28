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
package it.softsolutions.bestx.services.pricediscovery.worker;

import it.softsolutions.bestx.model.Proposal.ProposalSide;
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
public class ProposalWorker implements Runnable {

    private ProposalAggregatorListener proposalAggregatorListener;
    private ProposalSide side;
    private String marketMaker;
    
    public ProposalWorker(ProposalAggregatorListener proposalAggregatorListener, ProposalSide side, String marketMaker) {
        super();
        this.proposalAggregatorListener = proposalAggregatorListener;
        this.side = side;
        this.marketMaker = marketMaker;
    }

    @Override
    public void run() {
        proposalAggregatorListener.onProposal(side, marketMaker);
    }
    
}
