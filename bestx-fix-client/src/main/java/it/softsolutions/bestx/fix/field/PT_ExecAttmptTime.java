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
import java.util.Date;

import quickfix.UtcTimeStampField;

public class PT_ExecAttmptTime extends UtcTimeStampField 
{ 
  static final long serialVersionUID = 20050617;

  public static final int FIELD = 9689; 
  
  public PT_ExecAttmptTime() 
  { 
    super(9689);
  } 
   
  public PT_ExecAttmptTime(Date data) 
  { 
     super(9689, data, true);
  } 
  
  
}

  