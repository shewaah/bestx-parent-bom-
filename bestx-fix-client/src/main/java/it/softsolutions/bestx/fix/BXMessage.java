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
package it.softsolutions.bestx.fix;

import it.softsolutions.bestx.fix.field.MsgType;

/**
 * 
 * Purpose: this class is mainly for ...
 * 
 * Project Name : bestx-fix-client 
 * First created by: davide.rossoni 
 * Creation date: June 11, 2012
 * 
 **/
public abstract class BXMessage<T41 extends quickfix.fix41.Message, T42 extends quickfix.fix42.Message> {

   private Integer msgSeqNum;
   private MsgType msgType;
   
   private String originalFixMessage;
   
   public BXMessage(MsgType msgType) {
      this.msgType = msgType;
   }

   public abstract T41 toFIX41Message();
   
   public abstract T42 toFIX42Message();

   public Integer getMsgSeqNum() {
      return msgSeqNum;
   }

   public void setMsgSeqNum(Integer msgSeqNum) {
      this.msgSeqNum = msgSeqNum;
   }

   public MsgType getMsgType() {
      return msgType;
   }

   public void setMsgType(MsgType msgType) {
      this.msgType = msgType;
   }

	public String getOriginalFixMessage() {
		return originalFixMessage;
	}

	public void setOriginalFixMessage(String originalFixMessage) {
		this.originalFixMessage = originalFixMessage;
	}
   
}
