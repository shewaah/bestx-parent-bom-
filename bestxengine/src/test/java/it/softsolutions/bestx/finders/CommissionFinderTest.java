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
package it.softsolutions.bestx.finders;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import it.softsolutions.bestx.model.Commission;
import it.softsolutions.bestx.model.Commission.CommissionType;
import it.softsolutions.bestx.model.CommissionRow;

/**  
 *
 * Purpose: this class is mainly for ...  
 *
 * Project Name : bestxengine 
 * First created by: davide.rossoni 
 * Creation date: 26/ott/2012 
 * 
 **/
public class CommissionFinderTest {

    
    @Test
    public void commissionRow() {
        BigDecimal amount = new BigDecimal(3.75);
        CommissionType commissionType = CommissionType.TICKER;
        
        BigDecimal minQty = new BigDecimal(30.0);
        BigDecimal maxQty = new BigDecimal(5000.0);
        Commission commission = new Commission(amount, commissionType);
        BigDecimal minimumFee = new BigDecimal(4.25);
        BigDecimal minimumFeeMaxSize = BigDecimal.valueOf(128.68);
        
        CommissionRow commissionRow = new CommissionRow(minQty, maxQty, commission, minimumFee, minimumFeeMaxSize);
        
        assertEquals(minQty, commissionRow.getMinQty());
        assertEquals(maxQty, commissionRow.getMaxQty());
        assertEquals(minimumFee, commissionRow.getMinimumFee());
        assertEquals(minimumFeeMaxSize, commissionRow.getMinimumFeeMaxSize());
        assertEquals(amount, commissionRow.getCommission().getAmount());
        assertEquals(commissionType, commissionRow.getCommission().getCommissionType());
    }
    
}
