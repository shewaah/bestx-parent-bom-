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

package it.softsolutions.bestx.services.rest.dto;

import java.util.List;

public class HealthCheckResponse {

	private List<MessageElement> messages;
	private HealthCheckResponseData data;

	public List<MessageElement> getMessages() {
		return messages;
	}

	public void setMessages(List<MessageElement> messages) {
		this.messages = messages;
	}

	public HealthCheckResponseData getData() {
		return data;
	}

	public void setData(HealthCheckResponseData data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "HealthCheckResponse [messages=" + messages + ", data=" + data + "]";
	}

}
