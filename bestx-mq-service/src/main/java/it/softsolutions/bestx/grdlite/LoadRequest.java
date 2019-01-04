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
import it.softsolutions.bestx.mq.messages.MQRequest;
import it.softsolutions.bestx.xml.XPathHelper;

import java.util.Arrays;
import java.util.HashMap;

import javax.xml.namespace.NamespaceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**  
 *
 * Purpose: GRDLite load request. Typical xml is :
 <ns0:LoadRequest xmlns:ns0="BestX"  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:schemaLocation="BestX file:/H:/Trash/XSD/BestX%20%20Instrument%20Request%20Schema.xsd"  ns0:InitialLoadTypeCd="false" >
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="iNXGPwzWN4opmXjKUt0pT.uDcsKiS" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="J_7D2" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="RvU" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="ALH_BCHhSrTbcTbnf7KBKKOdCs" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="Il275bDnqB.keW" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="ZOYpvRPy3jcb-D" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="CK6DDjJ" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="r_llOjUtWV65oo" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="jATf" />
    <Instrument ns0:SecurityTypeCd="ISIN"  SecurityId="DfnJLR1ba9q1AF6coMYuHCmqyHe" />
</ns0:LoadRequest>
 *
 * Project Name : mq-service 
 * First created by: ruggero.rizzo 
 * Creation date: 24/gen/2013 
 * 
 **/
public class LoadRequest extends MQRequest {
    private static final Logger logger = LoggerFactory.getLogger(LoadRequest.class);
    
    public static final String TEMPLATE_SCHEMA = "XSD_SCHEMA";
    public static final String TEMPLATE_BODY = "BODY";
    public static final String TEMPLATE_INITIAL_LOAD_TYPE = "SECURITY_TYPE";
    public static final String TEMPLATE_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String TEMPLATE_SECURITY_ID = "SECURITY_ID";
    public static final String DEFAULT_SCHEMA = "grdLiteMessageSchema.xsd";
    private SecurityType securityType;
    private boolean initialLoad;
    private String[] securityIDs;
    private String messagesSchema;
    private String xmlMessageTemplate = "<?xml version='1.0' ?><ns0:LoadRequest xmlns:ns0='BestX'  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='" + TEMPLATE_SCHEMA + "' ns0:InitialLoadTypeCd='" + TEMPLATE_INITIAL_LOAD_TYPE + "'>" + TEMPLATE_BODY + "</ns0:LoadRequest>";    //private String xmlMessageTemplate = "<ns0:LoadRequest InitialLoadTypeCd=\"false\">" + TEMPLATE_BODY + "</ns0:LoadRequest>";
    private String xmlInstrumentTemplate = "<Instrument ns0:SecurityTypeCd='" + TEMPLATE_SECURITY_TYPE + "' SecurityId='" + TEMPLATE_SECURITY_ID + "'/>";
    
    /**
     * Constructor with an explicit messages schema
     * @param messagesSchema the schema used in the xml
     * @param securityType securityType
     * @param initialLoad initialLoad
     * @param securityIDs securityIDs
     */
    public LoadRequest(String messagesSchema, SecurityType securityType, boolean initialLoad, String... securityIDs) {
        this.messagesSchema = messagesSchema;
        this.securityType = securityType;
        this.securityIDs = securityIDs;
        this.initialLoad = initialLoad;
    }

    /**
     * Constructor with an implicit messages schema
     * @param securityType securityType
     * @param initialLoad initialLoad
     * @param securityIDs securityIDs
     */
    public LoadRequest(SecurityType securityType, boolean initialLoad, String... securityIDs) {
        this.securityType = securityType;
        this.securityIDs = securityIDs;
        this.initialLoad = initialLoad;
        this.messagesSchema = DEFAULT_SCHEMA;
    }

    /**
     * Build a new LoadRequest from an xml document.
     * @param xmlMessage xml origin
     * @return a new load request
     * @throws IllegalArgumentException if the xml is null
     * @throws Exception when something wrong happens 
     */
    public static LoadRequest fromXml(String xmlMessage) {
        if (xmlMessage == null) {
            throw new IllegalArgumentException("null xmlMessage");
        }
        HashMap<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("ns0", "BestX");
        NamespaceContext nsContext = new GRDLiteNameSpaceContext(namespaces);
        XPathHelper xpathHelper = new XPathHelper(nsContext);
        NodeList nodeList = xpathHelper.extractNodes(xmlMessage, "/ns0:LoadRequest/Instrument");
        LoadRequest loadRequest = null;
        int numSecurities = nodeList.getLength();
        String[] securityIDs = new String[numSecurities];
        if (numSecurities > 0) {
            // extract the securityTypeCd, it is unique for all
            Node node = nodeList.item(0);
            NamedNodeMap nodeMap = node.getAttributes();
            String secTypeCd = nodeMap.getNamedItem("ns0:SecurityTypeCd") != null ? nodeMap.getNamedItem("ns0:SecurityTypeCd").getNodeValue() : null;
            for (int count = 0; count < numSecurities; count++) {
                node = nodeList.item(count);
                nodeMap = node.getAttributes();
                String securityId = nodeMap.getNamedItem("SecurityId") != null ? nodeMap.getNamedItem("SecurityId").getNodeValue() : null;
                securityIDs[count] = securityId;

            }
            boolean initialLoadBool = false;
            nodeList = xpathHelper.extractNodes(xmlMessage, "/ns0:LoadRequest/@ns0:InitialLoadTypeCd");
            if (nodeList.getLength() > 0) {
                node = nodeList.item(0);
                String initialLoad = node.getNodeValue();
                initialLoadBool = Boolean.parseBoolean(initialLoad);
            }
            loadRequest = new LoadRequest(SecurityType.valueOf(secTypeCd), initialLoadBool, securityIDs);
        }
        logger.debug("Request: " + loadRequest);
        return loadRequest;
    }
   
    /**
     * Create an xml doc starting from this request values
     * 
     * @return the xml
     */
    public String toXml() {
        String xmlFinalMessage = xmlMessageTemplate.replaceAll(TEMPLATE_SCHEMA, messagesSchema).replaceAll(TEMPLATE_INITIAL_LOAD_TYPE, Boolean.toString(initialLoad));
        StringBuilder securities = new StringBuilder();
        if (securityIDs != null && securityIDs.length > 0) {
            for (int count = 0; count < securityIDs.length; count++) {
                securities.append(xmlInstrumentTemplate.replaceAll(TEMPLATE_SECURITY_TYPE, securityType.toString()).replaceAll(TEMPLATE_SECURITY_ID, securityIDs[count]));
            }
        }
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
     * @return the initialLoad
     */
    public boolean isInitialLoad() {
        return initialLoad;
    }

    /**
     * @param initialLoad
     *            the initialLoad to set
     */
    public void setInitialLoad(boolean initialLoad) {
        this.initialLoad = initialLoad;
    }

    /**
     * @return the securityIDs
     */
    public String[] getSecurityIDs() {
        return securityIDs;
    }

    /**
     * @param securityIDs
     *            the securityIDs to set
     */
    public void setSecurityIDs(String[] securityIDs) {
        this.securityIDs = securityIDs;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("LoadRequest [securityType=");
        builder.append(securityType);
        builder.append(", initialLoad=");
        builder.append(initialLoad);
        builder.append(", securityIDs=");
        builder.append(securityIDs != null ? Arrays.asList(securityIDs).subList(0, Math.min(securityIDs.length, maxLen)) : null);
        builder.append("]");
        return builder.toString();
    }

   @Override
   public String toTextMessage() {
      return this.toXml();
   }


}
