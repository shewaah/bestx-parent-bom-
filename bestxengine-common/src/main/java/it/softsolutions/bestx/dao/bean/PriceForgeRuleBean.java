/*
* Project Name : bestxengine-common
* First created by: stefano.pontillo
* Creation date: 10/mag/2012
*
* Copright 1997-2012 SoftSolutions! srl
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
*
*/
package it.softsolutions.bestx.dao.bean;

import java.math.BigDecimal;

public interface PriceForgeRuleBean {

   final public static String StrictlyLessThan = "<";
   final public static String LessThan = "<=";
   final public static String Equal = "=";
   final public static String MoreThan = ">=";
   final public static String StrictlyMoreThan = ">";

   final public static String Bid = "Buy";
   final public static String Ask = "Sell";
   final public static String Both = "Both";

   final public static String No_Price = "NONE";
   final public static String Add_Spread = "BEST";
   final public static String No_Price_Default = "NONE_DEFAULT";
   final public static String Check_Proposal = "CHECK_PROPOSAL";
   
   // nomi delle colonne sul DB
   final public static String ISIN = "ISIN";
   final public static  String SIDE = "Side";
   final public static  String CONDITION_OPERATOR = "ConditionOperator";
   final public static  String CONDITION_VALUE = "ConditionValue";
   final public static  String ACTION = "Strategy";
   final public static  String SPREAD = "Spread";
   final public static  String ACTIVE = "Active";
   
   final public static int STRICTLY_LESS_THAN = -1;
   final public static int LESS_THAN = -2;
   final public static int EQUAL = 0;
   final public static int MORE_THAN = 1;
   final public static int STRICTLY_MORE_THAN = 2;

   final public static int NO_PRICE = 0;
   final public static int ADD_SPREAD = 1;
   final public static int NO_PRICE_DEFAULT = 2;
   final public static int CHECK_PROPOSAL = 3;

   final public static BigDecimal DEFAULT_SPREAD = new BigDecimal("0.001").setScale(3);
   
   public static enum OperatorType {
      STRICTLY_LESS_THAN (PriceForgeRuleBean.STRICTLY_LESS_THAN, StrictlyLessThan),
      LESS_THAN (PriceForgeRuleBean.LESS_THAN, LessThan),
      EQUAL(PriceForgeRuleBean.EQUAL, Equal),
      MORE_THAN (PriceForgeRuleBean.MORE_THAN, MoreThan),
      STRICTLY_MORE_THAN (PriceForgeRuleBean.STRICTLY_MORE_THAN, StrictlyMoreThan);
      
      private OperatorType(int value, String op) {
         this.stringValue = op;
         this.value = value;
              }
      public  String getString() {
         return stringValue;
      }
      public  int getValue() {
         return value;
      }
      private int value;
      private String stringValue;
   };

   public static enum ProposalSide {
      BID(1, Bid),
      ASK(10, Ask),
      BOTH(11, Both);
      private ProposalSide(int value, String ps) {
         this.value = value;
         this.stringValue = ps;
      }
      public  String getString() {
         return stringValue;
      }
      public boolean isAsk() {
         return this.value%2 != 0;
      }
      public boolean isBid() {
         return this.value%2 == 0;
      }
      private int value;
      private String stringValue;
   };

   public static enum ActionType {
      NO_PRICE (PriceForgeRuleBean.NO_PRICE,No_Price),
      ADD_SPREAD(PriceForgeRuleBean.ADD_SPREAD, Add_Spread),
      NO_PRICE_DEFAULT (PriceForgeRuleBean.NO_PRICE_DEFAULT,No_Price_Default),
      CHECK_PROPOSAL (PriceForgeRuleBean.CHECK_PROPOSAL,Check_Proposal);
      private ActionType(int value, String at) {
         this.value = value;
         this.stringValue = at;
      }
      public  String getString() {
         return stringValue;
      }
      public  int getValue() {
         return value;
      }
      private int value;
      private String stringValue;
   };
   
   
   /**
    * Return the isin code of the instrument
    * 
    * @return isin code of the instrument
    */
   public String getIsin();

   /**
    * Set the isin code of the instrument
    * 
    * @param isin code of the instrument
    */
   public void setIsin(String isin);

   /**
    * Return the side of the proposal
    * 
    * @return the side of the proposal
    */
   public ProposalSide getSide();
   
   /**
    * Set the side of the proposal
    * 
    * @param side of the proposal
    */
   public void setSide(ProposalSide side);
   
   /**
    * Set the side of the proposal given a String that represent it
    * 
    * @param side of the proposal
    */
   public void setSide(String side);

   /**
    * Return the OperatorType of the rule
    * 
    * @return OperatorType of the rule
    */
   public OperatorType getConditionOperator();
   
   /**
    * Set the OperatorType of the rule
    * 
    * @param conditionOperator The OperatorType of the rule
    */
   public void setConditionOperator(OperatorType conditionOperator);
   
   /**
    * Set the OperatorType of the rule given the string representation of the OperatorType
    * 
    * @param conditionOperator of the rule
    */
   public void setConditionOperator(String conditionOperator);

   /**
    * Return the condition value of the rule
    * 
    * @return the condition value of the rule
    */
   public BigDecimal getConditionValue();
   
   /**
    * Set the condition value of the rule
    * 
    * @param conditionValue value of the condition of the rule
    */
   public void setConditionValue(BigDecimal conditionValue);

   /**
    * Return the action of the rule
    * 
    * @return the action that the rule perform
    */
   public ActionType getAction();

   /**
    * Set the action that the rule perform
    * 
    * @param action ActionType of the action that the rule perform
    */
   public void setAction(ActionType action);
   
   /**
    * Set the action that the rule perform given a String that represents the action 
    * 
    * @param action String name of the action that the rule perform
    */
   public void setAction(String action);
   
   /**
    * Return the spread of the rule
    * 
    * @return value of the spread
    */
   public BigDecimal getSpread();

   /**
    * Set the value of the spread for this rule
    * 
    * @param spread value for the rule
    */
   public void setSpread(BigDecimal spread);

   /**
    * Compare the given quantity with the condition value and return the result based on the condition operator
    * 
    * if conditionOperator is STRICTLY_LESS_THAN compare if the given quantity is less then the condition value
    * if conditionOperator is LESS_THAN compare if the given quantity is less or equal then the condition value
    * if conditionOperator is EQUAL compare if the given quantity is equals then the condition value
    * if conditionOperator is MORE_THAN compare if the given quantity is more or equal then the condition value
    * if conditionOperator is STRICTLY_MORE_THAN compare if the given quantity is more then the condition value
    * 
    * @param quantity the value of the quantity to check
    * @return the result of the comparision with the condition value
    */
   public boolean isQuantityInCondition(BigDecimal quantity);
   
   /**
    * Override of the standard toString method
    */
   @Override
   public String toString();

   /**
    * Return if the rule is active
    * 
    * @return true if the rule is active
    */
   public boolean isActive();
   
   /**
    * Set the activwe flag of the rule 
    * 
    * @param active true if the rule is active
    */
   public void setActive(boolean active);

}
