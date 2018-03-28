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

package it.softsolutions.bestx.services.booksorter;

import java.util.Comparator;

import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Proposal.ProposalType;


/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine-product 
 * First created by: stefano 
 * Creation date: 19-ott-2012 
 * 
 **/
public class ClassifiedSpreadComparator implements Comparator<ClassifiedProposal> {

    private Comparator<ClassifiedProposal> comparator;

    ClassifiedSpreadComparator(Comparator<ClassifiedProposal> comparator) {
        this.comparator = comparator;
    }

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(ClassifiedProposal o1, ClassifiedProposal o2) {
        int result = 0;
        if(o1.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0){
            if(o2.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0){
                return  o1.getSpread().compareTo(o2.getSpread());
            }
            return -1;
        } else {
            if(o2.getType().compareTo(ProposalType.SPREAD_ON_BEST) == 0){
                return 1;
            }
        }
        return comparator.compare(o1, o2);
    }
}
