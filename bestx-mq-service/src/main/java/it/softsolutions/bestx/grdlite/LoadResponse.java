/*
* Copyright 1997-2013 SoftSolutions! srl 
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
package it.softsolutions.bestx.grdlite;

import it.softsolutions.bestx.grdlite.utils.GRDLiteNameSpaceContext;
import it.softsolutions.bestx.mq.messages.MQResponse;
import it.softsolutions.bestx.xml.XPathHelper;

import java.util.HashMap;

import javax.xml.namespace.NamespaceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



/**  
 *
 * Purpose: GRDLite load response. We work on xmls like this one :  
 *
 *<?xml version="1.0" encoding="UTF-8" ?>
    <ns0:LoadResponse xmlns:ns0="BestX" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="BestX file:/H:/Trash/XSD/BestX%20%20Instrument%20Request%20Schema.xsd">
        <Instrument ns0:SecurityTypeCd="ISIN" SecurityId="USU00292AB56" Status=”Received or Error” />
    </ns0:LoadResponse>
  

 * Project Name : mq-service 
 * First created by: ruggero.rizzo 
 * Creation date: 24/gen/2013 
 * 
 **/
public class LoadResponse extends MQResponse {
    private static final Logger logger = LoggerFactory.getLogger(LoadResponse.class);
    
    public static final String TEMPLATE_SCHEMA = "XSD_SCHEMA";
    public static final String TEMPLATE_BODY = "BODY";
    public static final String TEMPLATE_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String TEMPLATE_SECURITY_ID = "SECURITY_ID";
    public static final String TEMPLATE_STATUS = "STATUS";
    
    public static enum Status {
        Received, Error;
    }
    
    private SecurityType securityType;
    private Status status;
    private String securityId;
    private static String xmlMessageTemplate = "<ns0:LoadResponse xmlns:ns0='BestX'  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'  xsi:schemaLocation='BestX file:/H:/Trash/XSD/BestX%20%20Instrument%20Request%20Schema.xsd'>" + TEMPLATE_BODY + "</ns0:LoadResponse>";
    //private String xmlMessageTemplate = "<ns0:LoadRequest InitialLoadTypeCd=\"false\">" + TEMPLATE_BODY + "</ns0:LoadRequest>";
    private static String xmlInstrumentTemplate = "<Instrument ns0:SecurityTypeCd='" + TEMPLATE_SECURITY_TYPE +"'  SecurityId='" + TEMPLATE_SECURITY_ID +"'  Status='" + TEMPLATE_STATUS + "' />";

    /**
     * Class constructor
     * @param securityType
     * @param status
     * @param securityId
     */
    public LoadResponse(SecurityType securityType, Status status, String securityId) {
        super();
        this.securityType = securityType;
        this.status = status;
        this.securityId = securityId;
    }

    /**
     * Create a loadResponse from an xml document
     * 
     * @param xmlMessage
     *            : xml origin
     * @return a new LoadResponse
     * @throws IllegalArgumentException
     *             if the xml is null
     * @throws Exception
     *             when something wrong happens
     */
    public static LoadResponse fromXml(String xmlMessage) {
        if (xmlMessage == null) {
            throw new IllegalArgumentException("null xmlMessage");
        }
        HashMap<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("ns0", "BestX");
        NamespaceContext nsContext = new GRDLiteNameSpaceContext(namespaces);
        XPathHelper xpathHelper = new XPathHelper(nsContext);
        NodeList nodeList = xpathHelper.extractNodes(xmlMessage, "/ns0:LoadResponse/Instrument");
        LoadResponse loadResponse = null;
        for (int count = 0; count < nodeList.getLength(); count++) {
            Node node = nodeList.item(count);
            NamedNodeMap nodeMap = node.getAttributes();
            String secTypeCd = nodeMap.getNamedItem("ns0:SecurityTypeCd") != null ? nodeMap.getNamedItem("ns0:SecurityTypeCd").getNodeValue() : null;
            String status = nodeMap.getNamedItem("Status") != null ? nodeMap.getNamedItem("Status").getNodeValue() : null;
            String securityId = nodeMap.getNamedItem("SecurityId") != null ? nodeMap.getNamedItem("SecurityId").getNodeValue() : null;
            loadResponse = new LoadResponse(SecurityType.valueOf(secTypeCd), Status.valueOf(status), securityId);
        }
        logger.debug("Response: " + loadResponse);
        return loadResponse;
    }

    /**
     * Build an xml document starting from this loadResponse values
     * 
     * @return the xml
     */
    public String toXml() {
        String xmlFinalMessage = xmlMessageTemplate;
        StringBuilder securities = new StringBuilder();
        securities
                .append(xmlInstrumentTemplate.replaceAll(TEMPLATE_SECURITY_TYPE, securityType.toString()).replaceAll(TEMPLATE_SECURITY_ID, securityId).replaceAll(TEMPLATE_STATUS, status.toString()));
        xmlFinalMessage = xmlFinalMessage.replaceAll(TEMPLATE_BODY, securities.toString());
        return xmlFinalMessage;
    }

    /**
     * @return the securityType
     */
    public SecurityType getSecurityType() {
        return securityType;
    }

    /**
     * @param securityType
     *            the securityType to set
     */
    public void setSecurityType(SecurityType securityType) {
        this.securityType = securityType;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return the securityId
     */
    public String getSecurityId() {
        return securityId;
    }

    /**
     * @param securityId
     *            the securityId to set
     */
    public void setSecurityId(String securityId) {
        this.securityId = securityId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LoadResponse [securityType=");
        builder.append(securityType);
        builder.append(", status=");
        builder.append(status);
        builder.append(", securityId=");
        builder.append(securityId);
        builder.append("]");
        return builder.toString();
    }
    
    @Override
   public String toTextMessage() {
      return this.toXml();
   }
    
}
