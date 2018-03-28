/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved. 
 * 
 * This file is part of the QuickFIX FIX Engine 
 * 
 * This file may be distributed under the terms of the quickfixengine.org 
 * license as defined by quickfixengine.org and appearing in the file 
 * LICENSE included in the packaging of this file. 
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 * 
 * See http://www.quickfixengine.org/LICENSE for licensing information. 
 * 
 * Contact ask@quickfixengine.org if any conditions of this licensing 
 * are not clear to you.
 ******************************************************************************/

package it.softsolutions.bestx.fix.field;
import quickfix.DoubleField;


public class PT_DealerQuotePrice extends DoubleField 
{ 
  static final long serialVersionUID = 20050617;

  public static final int FIELD = 9694; 
  
  public PT_DealerQuotePrice() 
  { 
    super(9694);
  } 
   
  public PT_DealerQuotePrice(double data) 
  { 
     super(9694, data);
  } 
  
  
}

  