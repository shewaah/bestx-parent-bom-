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
package it.softsolutions.bestx.services;

import it.softsolutions.bestx.services.price.PriceService;
import it.softsolutions.bestx.services.price.PriceService.PriceDiscoveryType;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestxengine-product First created by: ruggero.rizzo Creation date: 02/ago/2013
 * 
 **/
public class PriceServiceProvider
{
   Map<PriceDiscoveryType, PriceService> priceServices = new HashMap<PriceDiscoveryType, PriceService>();

   public Map<PriceDiscoveryType, PriceService> getPriceServices()
   {
      return priceServices;
   }

   public void setPriceServices(Map<PriceDiscoveryType, PriceService> priceServices)
   {
      this.priceServices = priceServices;
   }

   public PriceService getPriceService(PriceDiscoveryType priceDiscType)
   {
      return priceServices.get(priceDiscType);
   }
}
