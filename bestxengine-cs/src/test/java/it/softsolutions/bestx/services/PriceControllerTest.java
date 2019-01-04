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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.softsolutions.bestx.BestXException;
import it.softsolutions.bestx.CustomerAttributes;
import it.softsolutions.bestx.Messages;
import it.softsolutions.bestx.model.ClassifiedProposal;
import it.softsolutions.bestx.model.Customer;
import it.softsolutions.bestx.model.Proposal.ProposalSide;
import it.softsolutions.bestx.model.Proposal.ProposalState;
import it.softsolutions.bestx.services.proposalclassifiers.DiscardWorstPriceProposalClassifier;
import it.softsolutions.jsscommon.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


/**  
*
* Purpose: test class for the PriceController  
*
* Project Name : bestxengine-cs 
* First created by: ruggero.rizzo 
* Creation date: 25/mag/2012 
* 
**/
public class PriceControllerTest
{
   //sequence of tests for the checkMaxDeviation method
   
   @Test(expected = IllegalArgumentException.class)
   public void checkMaxDeviationNullPrice() throws BestXException {
      PriceController.INSTANCE.checkMaxDeviation(null, new ArrayList<ClassifiedProposal>(), new Customer());
   }
   
   @Test(expected = IllegalArgumentException.class)
   public void checkMaxDeviationNullProposals() throws BestXException {
      PriceController.INSTANCE.checkMaxDeviation(new BigDecimal(100.2), null, new Customer());
   } 

   @Test(expected = IllegalArgumentException.class)
   public void checkMaxDeviationNullCustomer() throws BestXException {
      PriceController.INSTANCE.checkMaxDeviation(new BigDecimal(100.2), new ArrayList<ClassifiedProposal>(), null);
   } 

   @Test(expected = BestXException.class)
   public void checkMaxDeviationInvalidPrice() throws BestXException {
      BigDecimal orderPrice = new BigDecimal(-100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      
      PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, new Customer());
   }
   
   @Test(expected = BestXException.class)
   public void checkMaxDeviationInvalidProposals() throws BestXException{
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      ClassifiedProposal noPriceProposal = new ClassifiedProposal();
      worstPriceDiscardedProposals.add(noPriceProposal);
      PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, new Customer());
   }
   
   @Test(expected = BestXException.class)
   public void checkMaxDeviationInvalidCustomerAttributes() throws BestXException{
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      ClassifiedProposal noPriceProposal = new ClassifiedProposal();
      worstPriceDiscardedProposals.add(noPriceProposal);
      Customer customer = new Customer();
      customer.setCustomerAttributes(null);
      PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
   }
   
   @Test(expected = BestXException.class)
   public void checkMaxDeviationNullValueConfigured() throws BestXException{
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      ClassifiedProposal noPriceProposal = new ClassifiedProposal();
      worstPriceDiscardedProposals.add(noPriceProposal);
      Customer customer = new Customer();
      CustomerAttributes csCustAttr = new CustomerAttributes();
      csCustAttr.setLimitPriceOffMarket(null);
      customer.setCustomerAttributes(csCustAttr);
      PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
   }
   
   @Test
   public void checkMaxDeviationNoProposals() throws BestXException{
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      ClassifiedProposal validProposal = PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, new Customer());
      assertEquals(null, validProposal);
   }
   
   @Test
   public void checkMaxDeviationAllRejected() throws BestXException {      
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      
      ClassifiedProposal rejectedProposal = new ClassifiedProposal();
      rejectedProposal.setPrice(new Money("EUR", new BigDecimal(90.02)));
      rejectedProposal.setProposalState(ProposalState.REJECTED);
      //proposal price lesser than the limit, to be worse than the limit it should be and ASK
      //proposal
      rejectedProposal.setSide(ProposalSide.ASK);
      worstPriceDiscardedProposals.add(rejectedProposal);
      
      Customer customer = new Customer();
      CustomerAttributes csCustAttr = new CustomerAttributes();
      csCustAttr.setLimitPriceOffMarket(new BigDecimal(1.0));
      customer.setCustomerAttributes(csCustAttr);
      
      ClassifiedProposal validProposal = PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
      assertEquals(null, validProposal);
   }
   
   @Test
   public void checkMaxDeviationOneValidAsk() throws BestXException
   {
      checkMaxDeviationOneValid(ProposalSide.ASK, new BigDecimal(90.02), new BigDecimal(100.01));
   }

   @Test
   public void checkMaxDeviationOneValidBid() throws BestXException
   {
      checkMaxDeviationOneValid(ProposalSide.BID, new BigDecimal(130.02), new BigDecimal(100.04));
   }

   private void checkMaxDeviationOneValid(ProposalSide proposalSide, BigDecimal proposalPrice, BigDecimal validProposalPrice) throws BestXException {
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      
      ClassifiedProposal rejectedProposal = new ClassifiedProposal();
      rejectedProposal.setPrice(new Money("EUR", proposalPrice));
      rejectedProposal.setProposalState(ProposalState.REJECTED);
      //proposal price lesser than the limit, to be worse than the limit it should be and ASK
      //proposal
      rejectedProposal.setSide(proposalSide);
      worstPriceDiscardedProposals.add(rejectedProposal);

      ClassifiedProposal validProposal = new ClassifiedProposal();
      validProposal.setPrice(new Money("EUR", validProposalPrice));
      validProposal.setProposalState(ProposalState.REJECTED);
      //proposal price lesser than the limit, to be worse than the limit it should be and ASK
      //proposal
      validProposal.setSide(proposalSide);
      worstPriceDiscardedProposals.add(validProposal);
      
      Customer customer = new Customer();
      CustomerAttributes csCustAttr = new CustomerAttributes();
      csCustAttr.setLimitPriceOffMarket(new BigDecimal(1.0));
      customer.setCustomerAttributes(csCustAttr);
      
      ClassifiedProposal resultProposal = PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
      assertEquals(validProposal, resultProposal);
   }

   @Test
   public void checkMaxDeviationZeroPriceAsk() throws BestXException {
      checkMaxDeviationZeroPrice(ProposalSide.ASK);
   }

   @Test
   public void checkMaxDeviationZeroPriceBid() throws BestXException {
      checkMaxDeviationZeroPrice(ProposalSide.BID);
   }

   private void checkMaxDeviationZeroPrice(ProposalSide proposalSide) throws BestXException {
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      
      ClassifiedProposal rejectedProposal = new ClassifiedProposal();
      rejectedProposal.setPrice(new Money("EUR", new BigDecimal(0.0)));
      rejectedProposal.setProposalState(ProposalState.REJECTED);
      rejectedProposal.setSide(proposalSide);
      worstPriceDiscardedProposals.add(rejectedProposal);

      ClassifiedProposal validProposal = new ClassifiedProposal();
      validProposal.setPrice(new Money("EUR", new BigDecimal(0.0)));
      validProposal.setProposalState(ProposalState.REJECTED);
      validProposal.setSide(proposalSide);
      worstPriceDiscardedProposals.add(validProposal);
      
      Customer customer = new Customer();
      CustomerAttributes csCustAttr = new CustomerAttributes();
      csCustAttr.setLimitPriceOffMarket(new BigDecimal(1.0));
      customer.setCustomerAttributes(csCustAttr);
      
      ClassifiedProposal resultProposal = PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
      assertEquals(null, resultProposal);
   }
   
   @Test
   public void checkMaxDeviationCustomerDeviationZeroAsk() throws BestXException {      
      checkMaxDeviationCustomerDeviationZero(ProposalSide.ASK, new BigDecimal(90.02));
   }
   
   @Test
   public void checkMaxDeviationCustomerDeviationZeroBid() throws BestXException {      
      checkMaxDeviationCustomerDeviationZero(ProposalSide.BID, new BigDecimal(130.03));
   }
   
   private void checkMaxDeviationCustomerDeviationZero(ProposalSide proposalSide, BigDecimal proposalPrice) throws BestXException {      
      BigDecimal orderPrice = new BigDecimal(100.2);
      List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
      
      ClassifiedProposal rejectedProposal = new ClassifiedProposal();
      rejectedProposal.setPrice(new Money("EUR", proposalPrice));
      rejectedProposal.setProposalState(ProposalState.REJECTED);
      rejectedProposal.setSide(ProposalSide.ASK);
      worstPriceDiscardedProposals.add(rejectedProposal);
      
      Customer customer = new Customer();
      CustomerAttributes csCustAttr = new CustomerAttributes();
      csCustAttr.setLimitPriceOffMarket(BigDecimal.ZERO);
      customer.setCustomerAttributes(csCustAttr);
      
      ClassifiedProposal validProposal = PriceController.INSTANCE.checkMaxDeviation(orderPrice, worstPriceDiscardedProposals, customer);
      assertTrue(validProposal != null);
   }
   
 //END sequence of tests for the checkMaxDeviation method
   
 
   // BEGIN getBestProposalDelta tests sequence
   @Test (expected=IllegalArgumentException.class)
   public void testGetBestProposalDeltaOrderPriceNull() throws BestXException {
       PriceController.INSTANCE.getBestProposalDelta(null, new ArrayList<ClassifiedProposal>(), new Customer());
   }
   
   @Test (expected=IllegalArgumentException.class)
   public void testGetBestProposalDeltaBookProposalsNull() throws BestXException {
       PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(100.0), null, new Customer());
   }

   
   @Test (expected=IllegalArgumentException.class)
   public void testGetBestProposalDeltaCustomerNull() throws BestXException {
       PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(100.0), new ArrayList<ClassifiedProposal>(), null);
   }

   @Test (expected=IllegalArgumentException.class)
   public void testGetBestProposalDeltaOrderPriceNegative() throws BestXException {
       PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(-100.0), new ArrayList<ClassifiedProposal>(), null);
   }

   @Test (expected=IllegalArgumentException.class)
   public void testGetBestProposalDeltaBookProposalsEmpty() throws BestXException {
       PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(100.0), new ArrayList<ClassifiedProposal>(), null);
   }
   
   
   @Test
   public void testGetBestProposalDeltaNoValidProposal() throws BestXException {
       List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
       
       Messages messages = new Messages();
       messages.setBundleName("messages");
       messages.setLanguage("it");
       messages.setCountry("IT");
       
       ClassifiedProposal rejectedProposal = new ClassifiedProposal();
       rejectedProposal.setPrice(new Money("EUR", new BigDecimal(100.0)));
       rejectedProposal.setProposalState(ProposalState.REJECTED);
       //reason different from the expected one for limit price exceedingness
       rejectedProposal.setReason("Trading venue already used");
       rejectedProposal.setSide(ProposalSide.ASK);
       worstPriceDiscardedProposals.add(rejectedProposal);
       Double delta = PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(100.0), worstPriceDiscardedProposals, new Customer());
       //expected 0 because there are no valid proposals
       assertTrue(BigDecimal.ZERO.compareTo(new BigDecimal(delta)) == 0);
   }
   
   @Test
   public void testGetBestProposalDeltaProposalsWithNegativePrice() throws BestXException {
       List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
       
       ClassifiedProposal rejectedProposal = new ClassifiedProposal();
       rejectedProposal.setPrice(new Money("EUR", new BigDecimal(-100.0)));
       rejectedProposal.setProposalState(ProposalState.REJECTED);
       //reason different from the expected one for limit price exceedingness
       rejectedProposal.setReason("Trading venue already used");
       rejectedProposal.setSide(ProposalSide.ASK);
       rejectedProposal.setReason(DiscardWorstPriceProposalClassifier.REJECT_REASON);
       worstPriceDiscardedProposals.add(rejectedProposal);
       
       rejectedProposal = new ClassifiedProposal();
       rejectedProposal.setPrice(new Money("EUR", new BigDecimal(-100.0)));
       rejectedProposal.setProposalState(ProposalState.REJECTED);
       //reason different from the expected one for limit price exceedingness
       rejectedProposal.setReason(DiscardWorstPriceProposalClassifier.REJECT_REASON);
       rejectedProposal.setSide(ProposalSide.ASK);
       
       Double delta = PriceController.INSTANCE.getBestProposalDelta(new BigDecimal(100.0), worstPriceDiscardedProposals, new Customer());
       //expected 0 because there are no valid proposals prices
       assertTrue(BigDecimal.ZERO.compareTo(new BigDecimal(delta)) == 0);
   }
   
   @Test
   public void testGetBestProposalDeltaValidProposal() throws BestXException {
       List<ClassifiedProposal> worstPriceDiscardedProposals = new ArrayList<ClassifiedProposal>();
       BigDecimal proposalPrice = new BigDecimal(102.0);
       BigDecimal orderPrice = new BigDecimal(100.0);
       double expectedDeltaDeviation = ( proposalPrice.doubleValue() - orderPrice.doubleValue() )/ proposalPrice.doubleValue() * 100;
       ClassifiedProposal rejectedProposal = new ClassifiedProposal();
       rejectedProposal.setPrice(new Money("EUR", proposalPrice));
       rejectedProposal.setProposalState(ProposalState.REJECTED);
       //reason different from the expected one for limit price exceedingness
       rejectedProposal.setReason("Trading venue already used");
       rejectedProposal.setSide(ProposalSide.ASK);
       rejectedProposal.setReason(DiscardWorstPriceProposalClassifier.REJECT_REASON);
       worstPriceDiscardedProposals.add(rejectedProposal);
       
       double deltaDeviation = PriceController.INSTANCE.getBestProposalDelta(orderPrice, worstPriceDiscardedProposals, new Customer());
       assertTrue(expectedDeltaDeviation == deltaDeviation);
   }
// END getBestProposalDelta tests sequence
}


   
