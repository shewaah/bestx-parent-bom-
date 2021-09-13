package it.softsolutions.bestx.services.rest.dto;
/*
* Copyright 1997-2021 SoftSolutions! srl 
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

import java.util.ArrayList;
import java.util.List;

public class GetRoutingProposalResponse {

	private List<MessageElement> messages = new ArrayList<>();
	private GetRoutingProposalResponseData data = new GetRoutingProposalResponseData();

	public List<MessageElement> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageElement> messages) {
		this.messages = messages;
	}

	public GetRoutingProposalResponseData getData() {
		return data;
	}

	public void setData(GetRoutingProposalResponseData data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "GetRoutingProposalResponse [messages=" + messages + ", data=" + data + "]";
	}

}
