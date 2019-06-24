/*
 * Project Name : BestXEngine-cs
 *
 * Copyright    : Copyright (c) 2002 SoftSolutions!.  All rights reserved.
 *                This source code constitutes the unpublished,
 *                confidential and proprietary information of SoftSolutions!.
 *
 * Created by   : SoftSolutions! Italy.
 */
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.services.serial.SerialNumberService;

/**  

*
* Purpose: this class is mainly for providing SerialNumberService
*          across all the project
*
* Project Name : bestxengine-cs 
* First created by: paolo.midali 
* Creation date: 30/mag/2012 
* 
**/
public class SerialNumberServiceProvider
{
	private static SerialNumberService serialNumberService;

	public static SerialNumberService getSerialNumberService() {
		return serialNumberService;
	}

	public static void setSerialNumberService(SerialNumberService serialNumberService) {
	    SerialNumberServiceProvider.serialNumberService = serialNumberService;
	}
}
