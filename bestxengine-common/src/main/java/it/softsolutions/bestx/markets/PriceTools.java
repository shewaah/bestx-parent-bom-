/*
 * Project Name : BestXEngine_common
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 *
 * CVS information
 * File name    : $RCSfile $
 * Author       : $Author: anna.cochetti $
 * Date         : $Date: 2010-04-07 12:27:28 $
 * Header       : $Id: PriceTools.java,v 1.1 2010-04-07 12:27:28 anna.cochetti Exp $
 * Revision     : $Revision: 1.1 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/markets/PriceTools.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.markets;

import java.math.BigDecimal;

public class PriceTools {
    
    public static final BigDecimal ONE_ON_256 = new BigDecimal(0.00390625);
    public static final BigDecimal ONE_ON_32 = new BigDecimal(0.03125);

    public static BigDecimal convertPriceFrom32(BigDecimal price) {
        BigDecimal integerPart = price.setScale(0, BigDecimal.ROUND_DOWN);
        BigDecimal decimalPart = price.subtract(integerPart);
        BigDecimal _32Part = decimalPart.setScale(2, BigDecimal.ROUND_DOWN);
        BigDecimal _256Part = decimalPart.subtract(_32Part).movePointRight(3).setScale(9, BigDecimal.ROUND_DOWN);
        _32Part = _32Part.movePointRight(2);

        BigDecimal _32 = _32Part.multiply(ONE_ON_32);
        BigDecimal _256 = _256Part.multiply(ONE_ON_256);
        BigDecimal convertedPrice = _256.add(_32).add(integerPart);
        return convertedPrice;
    }
}
