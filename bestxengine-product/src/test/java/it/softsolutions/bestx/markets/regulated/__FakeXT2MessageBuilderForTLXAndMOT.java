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
package it.softsolutions.bestx.markets.regulated;

import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.xt2.protocol.XT2Msg;

import java.math.BigDecimal;

/**
 * Here we build different kinds of XT2 messages books mocking those sent us by the TLX/MOT markets.
 * Both the markets sometimes send us proposals with price and quantity set to zero.
 * 
 * 
 * Example of an original XT2Msg :
 * 
 *    fakeMsg.setName("XT2CQueryPricesNotify");
      fakeMsg.setSubject("XT2CQueryPricesNotify");

      fakeMsg.setValue("NoMDEntries.ItemCount", 5);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.1", 1);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.2", 2);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.3", 3);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.4", 4);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.5", 5);

      fakeMsg.setValue("NoMDEntries.NumberOfOrders.1", 2);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.2", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.3", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.4", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.5", 0);

      fakeMsg.setValue("Side.1", 1);
      fakeMsg.setValue("Side.2", 1);
      fakeMsg.setValue("Side.3", 1);
      fakeMsg.setValue("Side.4", 1);
      fakeMsg.setValue("Side.5", 1);

      fakeMsg.setValue("Price.1", 96.4);
      fakeMsg.setValue("Price.2", 96.499);
      fakeMsg.setValue("Price.3", 0.0);
      fakeMsg.setValue("Price.4", 0.0);
      fakeMsg.setValue("Price.5", 0.0);

      fakeMsg.setValue("Qty.1", 177000.0);
      fakeMsg.setValue("Qty.2", 20000.0);
      fakeMsg.setValue("Qty.3", 0.0);
      fakeMsg.setValue("Qty.4", 0.0);
      fakeMsg.setValue("Qty.5", 0.0);

      fakeMsg.setValue("SourceMarketName", "TLXFIX");
      fakeMsg.setValue("SecurityIDSource", 4);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("CD", "DPSP01754264");
      fakeMsg.setValue("Symbol", "n/a");
      fakeMsg.setValue("Isin", "IT0004755390");
      fakeMsg.setValue("SubMarketName", "ETX");

 * @author ruggero.rizzo
 *
 */
public class __FakeXT2MessageBuilderForTLXAndMOT
{
   
   /**
    * Returns a TLX book with proposal of the requested side with 3 price/qty set to 0
    * and 2 proposals over the limit.
    * Remember that the book is returned as viewed by the ordering side.
    * If the order is a BUY then the proposal side must be ASK and the book
    * will have at his top the lowest price.
    * @param limitPrice
    * @param side
    * @return
    */
   public XT2Msg getPricesToZeroOrOverTheLimitMsg(BigDecimal limitPrice, ProposalSide side, 
         String marketName, String subMarketName)
   {
      XT2Msg fakeMsg = new XT2Msg();
      fakeMsg.setName("XT2CQueryPricesNotify");
      fakeMsg.setSubject("XT2CQueryPricesNotify");

      fakeMsg.setValue("NoMDEntries.ItemCount", 5);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.1", 1);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.2", 2);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.3", 3);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.4", 4);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.5", 5);

      fakeMsg.setValue("NoMDEntries.NumberOfOrders.1", 2);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.2", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.3", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.4", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.5", 0);

      int propSide = -1;
      BigDecimal price1 = null;
      BigDecimal price2 = null;
      
      if (side.equals(ProposalSide.ASK))
      {
         //the price 0 proposal are those with the lowest price
         //I am buying, the ordering puts the lowest prices ahead
         propSide = 1;
         price1 = limitPrice.add(new BigDecimal("0.02"));
         price2 = limitPrice.add(new BigDecimal("0.01"));
         fakeMsg.setValue("Price.1", 0.0);
         fakeMsg.setValue("Price.2", 0.0);
         fakeMsg.setValue("Price.3", 0.0);
         fakeMsg.setValue("Price.4", price2.doubleValue());
         fakeMsg.setValue("Price.5", price1.doubleValue());
         
         fakeMsg.setValue("Qty.1", 0.0);
         fakeMsg.setValue("Qty.2", 0.0);
         fakeMsg.setValue("Qty.3", 0.0);
         fakeMsg.setValue("Qty.4", 177000.0);
         fakeMsg.setValue("Qty.5", 20000.0);
      }
      else
      {
         //the price 0 proposal are those with the lowest price
         //I am selling, the ordering puts the lowest prices at the bottom
         propSide = 2;
         price1 = limitPrice.subtract(new BigDecimal("0.02"));
         price2 = limitPrice.subtract(new BigDecimal("0.01"));
         fakeMsg.setValue("Price.1", price1.doubleValue());
         fakeMsg.setValue("Price.2", price2.doubleValue());
         fakeMsg.setValue("Price.3", 0.0);
         fakeMsg.setValue("Price.4", 0.0);
         fakeMsg.setValue("Price.5", 0.0);
         
         fakeMsg.setValue("Qty.1", 177000.0);
         fakeMsg.setValue("Qty.2", 20000.0);
         fakeMsg.setValue("Qty.3", 0.0);
         fakeMsg.setValue("Qty.4", 0.0);
         fakeMsg.setValue("Qty.5", 0.0);
      }

      fakeMsg.setValue("Side.1", propSide);
      fakeMsg.setValue("Side.2", propSide);
      fakeMsg.setValue("Side.3", propSide);
      fakeMsg.setValue("Side.4", propSide);
      fakeMsg.setValue("Side.5", propSide);
      


      fakeMsg.setValue("SourceMarketName", marketName);
      fakeMsg.setValue("SecurityIDSource", 4);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("CD", "DPSP01754264");
      fakeMsg.setValue("Symbol", "n/a");
      fakeMsg.setValue("Isin", "IT0004755390");
      fakeMsg.setValue("SubMarketName", subMarketName);
      
      return fakeMsg;
   }  

   /**
    * Returns a TLX book with proposal of the requested side over the limit.
    * Remember that the book is returned as viewed by the ordering side.
    * If the order is a BUY, then the proposal side must be ASK and the book
    * will have at his top the lowest price.
    * @param limitPrice
    * @param side
    * @return
    */
   public XT2Msg getPricesOverTheLimitMsg(BigDecimal limitPrice, ProposalSide side, 
         String marketName, String subMarketName)
   {
      XT2Msg fakeMsg = new XT2Msg();
      fakeMsg.setName("XT2CQueryPricesNotify");
      fakeMsg.setSubject("XT2CQueryPricesNotify");

      fakeMsg.setValue("NoMDEntries.ItemCount", 5);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.1", 1);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.2", 2);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.3", 3);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.4", 4);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.5", 5);

      fakeMsg.setValue("NoMDEntries.NumberOfOrders.1", 2);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.2", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.3", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.4", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.5", 0);

      int propSide = -1;
      BigDecimal price1 = null;
      BigDecimal price2 = null;
      BigDecimal price3 = null;
      BigDecimal price4 = null;
      BigDecimal price5 = null;
      
      if (side.equals(ProposalSide.ASK))
      {
         /*
          * Proposal ASK
          * Order BUY
          * THe first price in the book must be the lowest
          */
         propSide = 1;
         price1 = limitPrice.add(new BigDecimal("0.01"));
         price2 = limitPrice.add(new BigDecimal("0.02"));
         price3 = limitPrice.add(new BigDecimal("0.03"));
         price4 = limitPrice.add(new BigDecimal("0.04"));
         price5 = limitPrice.add(new BigDecimal("0.05"));
      }
      else
      {
         /*
          * Proposal BUY
          * Order ASK
          * The first price in the book must be the greatest
          */
         propSide = 2;
         price1 = limitPrice.subtract(new BigDecimal("0.01"));
         price2 = limitPrice.subtract(new BigDecimal("0.02"));
         price3 = limitPrice.subtract(new BigDecimal("0.03"));
         price4 = limitPrice.subtract(new BigDecimal("0.04"));
         price5 = limitPrice.subtract(new BigDecimal("0.05"));
      }
      
      fakeMsg.setValue("Price.1", price1.doubleValue());
      fakeMsg.setValue("Price.2", price2.doubleValue());
      fakeMsg.setValue("Price.3", price3.doubleValue());
      fakeMsg.setValue("Price.4", price4.doubleValue());
      fakeMsg.setValue("Price.5", price5.doubleValue());
      
      fakeMsg.setValue("Side.1", propSide);
      fakeMsg.setValue("Side.2", propSide);
      fakeMsg.setValue("Side.3", propSide);
      fakeMsg.setValue("Side.4", propSide);
      fakeMsg.setValue("Side.5", propSide);

      //random quantities
      fakeMsg.setValue("Qty.1", 177000.0);
      fakeMsg.setValue("Qty.2", 20000.0);
      fakeMsg.setValue("Qty.3", 100000.0);
      fakeMsg.setValue("Qty.4", 25000.0);
      fakeMsg.setValue("Qty.5", 78000.0);

      fakeMsg.setValue("SourceMarketName", marketName);
      fakeMsg.setValue("SecurityIDSource", 4);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("CD", "DPSP01754264");
      fakeMsg.setValue("Symbol", "n/a");
      fakeMsg.setValue("Isin", "IT0004755390");
      fakeMsg.setValue("SubMarketName", subMarketName);
      
      return fakeMsg;
   }  

   /**
    * Returns a TLX book with proposal of the requested side all within the limit.
    * Remember that the book is returned as viewed by the ordering side.
    * If the order is a BUY, then the proposal side must be ASK and the book
    * will have at his top the lowest price.
    * @param limitPrice
    * @param side
    * @return
    */
   public XT2Msg getPricesWithinTheLimitMsg(BigDecimal limitPrice, ProposalSide side, 
         String marketName, String subMarketName)
   {
      XT2Msg fakeMsg = new XT2Msg();
      fakeMsg.setName("XT2CQueryPricesNotify");
      fakeMsg.setSubject("XT2CQueryPricesNotify");

      fakeMsg.setValue("NoMDEntries.ItemCount", 5);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.1", 1);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.2", 2);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.3", 3);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.4", 4);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.5", 5);

      fakeMsg.setValue("NoMDEntries.NumberOfOrders.1", 2);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.2", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.3", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.4", 0);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.5", 0);

      int propSide = -1;
      BigDecimal price1 = null;
      BigDecimal price2 = null;
      BigDecimal price3 = null;
      BigDecimal price4 = null;
      BigDecimal price5 = null;
      
      if (side.equals(ProposalSide.ASK))
      {
         /*
          * Proposal ASK
          * Order BUY
          * THe first price in the book must be the lowest
          */
         propSide = 1;
         price1 = limitPrice.subtract(new BigDecimal("0.05"));
         price2 = limitPrice.subtract(new BigDecimal("0.04"));
         price3 = limitPrice.subtract(new BigDecimal("0.03"));
         price4 = limitPrice.subtract(new BigDecimal("0.02"));
         price5 = limitPrice.subtract(new BigDecimal("0.01"));
      }
      else
      {
         /*
          * Proposal BUY
          * Order ASK
          * The first price in the book must be the greatest
          */
         propSide = 2;
         price1 = limitPrice.add(new BigDecimal("0.01"));
         price2 = limitPrice.add(new BigDecimal("0.02"));
         price3 = limitPrice.add(new BigDecimal("0.03"));
         price4 = limitPrice.add(new BigDecimal("0.04"));
         price5 = limitPrice.add(new BigDecimal("0.05"));
      }
      
      fakeMsg.setValue("Price.1", price1.doubleValue());
      fakeMsg.setValue("Price.2", price2.doubleValue());
      fakeMsg.setValue("Price.3", price3.doubleValue());
      fakeMsg.setValue("Price.4", price4.doubleValue());
      fakeMsg.setValue("Price.5", price5.doubleValue());
      
      fakeMsg.setValue("Side.1", propSide);
      fakeMsg.setValue("Side.2", propSide);
      fakeMsg.setValue("Side.3", propSide);
      fakeMsg.setValue("Side.4", propSide);
      fakeMsg.setValue("Side.5", propSide);

      //random quantities
      fakeMsg.setValue("Qty.1", 177000.0);
      fakeMsg.setValue("Qty.2", 20000.0);
      fakeMsg.setValue("Qty.3", 100000.0);
      fakeMsg.setValue("Qty.4", 25000.0);
      fakeMsg.setValue("Qty.5", 78000.0);

      fakeMsg.setValue("SourceMarketName", marketName);
      fakeMsg.setValue("SecurityIDSource", 4);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("CD", "DPSP01754264");
      fakeMsg.setValue("Symbol", "n/a");
      fakeMsg.setValue("Isin", "IT0004755390");
      fakeMsg.setValue("SubMarketName", subMarketName);
      
      return fakeMsg;
   }
   
   /**
    * Returns a MOT book. Used to test proposal timestamp generation.
    * If the order is a BUY, then the proposal side must be ASK and the book
    * will have at his top the lowest price.
    * @param limitPrice
    * @param side
    * @return
    */
   public XT2Msg getMOTPricesWithDateAndTime()
   {
      XT2Msg fakeMsg = new XT2Msg();
      fakeMsg.setName("XT2CQueryPricesNotify");
      fakeMsg.setSubject("XT2CQueryPricesNotify");

      fakeMsg.setValue("NoMDEntries.ItemCount", 5);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.1", 1);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.2", 2);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.3", 3);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.4", 4);
      fakeMsg.setValue("NoMDEntries.MDEntryPositionNo.5", 5);

      fakeMsg.setValue("NoMDEntries.NumberOfOrders.1", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.2", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.3", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.4", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.5", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.6", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.7", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.8", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.9", 1);
      fakeMsg.setValue("NoMDEntries.NumberOfOrders.10", 1);

      fakeMsg.setValue("Date.1", "20111110");
      fakeMsg.setValue("Date.2", "20111110");
      fakeMsg.setValue("Date.3", "20111110");
      fakeMsg.setValue("Date.4", "20111110");
      fakeMsg.setValue("Date.5", "20111110");
      fakeMsg.setValue("Date.6", "20111110");
      fakeMsg.setValue("Date.7", "20111110");
      fakeMsg.setValue("Date.8", "20111110");
      fakeMsg.setValue("Date.9", "20111110");
      fakeMsg.setValue("Date.10", "20111110");
      
      String timeStr = "12:36:07";
      fakeMsg.setValue("Time.1", timeStr);
      fakeMsg.setValue("Time.2", timeStr);
      fakeMsg.setValue("Time.3", timeStr);
      fakeMsg.setValue("Time.4", timeStr);
      fakeMsg.setValue("Time.5", timeStr);
      fakeMsg.setValue("Time.6", timeStr);
      fakeMsg.setValue("Time.7", timeStr);
      fakeMsg.setValue("Time.8", timeStr);
      fakeMsg.setValue("Time.9", timeStr);
      fakeMsg.setValue("Time.10", timeStr);
      
      fakeMsg.setValue("Price.1", 94.39);
      fakeMsg.setValue("Price.2", 94.4);
      fakeMsg.setValue("Price.3", 94.45);
      fakeMsg.setValue("Price.4", 94.5);
      fakeMsg.setValue("Price.5", 94.53);
      fakeMsg.setValue("Price.6", 94.56);
      fakeMsg.setValue("Price.7", 94.59);
      fakeMsg.setValue("Price.8", 94.62);
      fakeMsg.setValue("Price.9", 94.89);
      fakeMsg.setValue("Price.10", 96.51);
      
      fakeMsg.setValue("Side.1", 1);
      fakeMsg.setValue("Side.2", 1);
      fakeMsg.setValue("Side.3", 1);
      fakeMsg.setValue("Side.4", 1);
      fakeMsg.setValue("Side.5", 1);
      fakeMsg.setValue("Side.6", 1);
      fakeMsg.setValue("Side.7", 1);
      fakeMsg.setValue("Side.8", 1);
      fakeMsg.setValue("Side.9", 1);
      fakeMsg.setValue("Side.10", 1);
      
      fakeMsg.setValue("Qty.1", 177000.0);
      fakeMsg.setValue("Qty.2", 20000.0);
      fakeMsg.setValue("Qty.3", 100000.0);
      fakeMsg.setValue("Qty.4", 25000.0);
      fakeMsg.setValue("Qty.5", 78000.0);
      fakeMsg.setValue("Qty.6", 200000.0);
      fakeMsg.setValue("Qty.7", 200000.0);
      fakeMsg.setValue("Qty.8", 200000.0);
      fakeMsg.setValue("Qty.9", 150000.0);
      fakeMsg.setValue("Qty.10", 78000.0);

      fakeMsg.setValue("SourceMarketName", "MOTFIX");
      fakeMsg.setValue("SecurityIDSource", 4);
      fakeMsg.setValue("UserSessionName", "MIFID");
      fakeMsg.setValue("CD", "DPSP01242915");
      fakeMsg.setValue("Symbol", "n/a");
      fakeMsg.setValue("Isin", "IT0004750409");
      fakeMsg.setValue("SubMarketName", "MOT");
      
      return fakeMsg;
   }  
   
}
