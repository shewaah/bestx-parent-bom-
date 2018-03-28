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
 * Date         : $Date: 2010-04-28 07:03:18 $
 * Header       : $Id: CommonFacilities.java,v 1.2 2010-04-28 07:03:18 anna.cochetti Exp $
 * Revision     : $Revision: 1.2 $
 * Source       : $Source: /root/scripts/BestXEngine_common/src/it/softsolutions/bestx/connections/fixgateway/CommonFacilities.java,v $
 * Tag name     : $Name: not supported by cvs2svn $
 * State        : $State: Exp $
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.connections.fixgateway;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CommonFacilities
{
   static BigDecimal setRequestedScale(BigDecimal number) {
      int scale = number.precision() - number.scale() + 2;
      if(scale <= 0) {         
         scale = 2;
      }
      number = number.round(new MathContext(scale, RoundingMode.HALF_UP));
      if (number.scale() > scale) {  // If the scale is > than the requested one, the first non-zero decimal digit is after the requested decimal digit:
         // e.g. requested scale is 2 and  first non zero digit is at 3rd place
         number = BigDecimal.ZERO.setScale(scale);
         }
      return number;
   }
   
   static BigDecimal decreasePriceScaleTo (BigDecimal px, int newScale) {
		if(px.scale() > newScale) return px.setScale(newScale, RoundingMode.HALF_UP);
		return px;
   }
}
