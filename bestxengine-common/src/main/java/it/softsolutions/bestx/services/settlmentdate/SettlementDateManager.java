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
 * Author       : $Author$
 * Date         : $Date$
 * Header       : $Id$
 * Revision     : $Revision$
 * Source       : $Source$
 * Tag name     : $Name$
 * State        : $State$
 *
 * For more details see cvs web documentation for project: BestXEngine_common
 */
package it.softsolutions.bestx.services.settlmentdate;

import it.softsolutions.bestx.model.Order;

import java.util.Date;

/**
 * This interface defines which services should provide a settlement date manager.
 * Here we should be able to find out a settlement date for an instrument.
 * 
 * @author ruggero.rizzo
 *
 */
public interface SettlementDateManager
{
   public Date getSettlementDate(Order order);   
}
