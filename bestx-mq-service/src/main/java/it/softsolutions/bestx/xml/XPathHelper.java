package it.softsolutions.bestx.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**  
*
* Purpose: XPath utilities  
*
* Project Name : mq-service 
* First created by: ruggero.rizzo 
* Creation date: 24/gen/2013 
* 
**/
public class XPathHelper {
    private static final Logger logger = LoggerFactory.getLogger(XPathHelper.class);
    private XPathFactory factory = XPathFactory.newInstance();
    private XPath xpath = factory.newXPath();
    private XPathExpression expr;
    private NamespaceContext namespaceContext = null;

    public XPathHelper() {
    }

    /**
     * Constructor for custom namespace context. Used when the xml document
     * contains namespaces definitions. Needed also with a default namespace
     * defined for all the nodes.
     * 
     * @param namespaceContext
     */
    public XPathHelper(NamespaceContext namespaceContext) {
        super();
        xpath.setNamespaceContext(namespaceContext);
        this.namespaceContext = namespaceContext;
    }

    /**
     * Here we change a value of the node found using the xpath query. The change affects the DOM received as parameter, so we return it to allow the caller to use it in every way he wants.
     * 
     * @param xmlDOM
     *            : the DOM of the XML
     * @param path
     *            : xpath query string
     * @param value
     *            : new node value
     * @return xmlDOM : the changed (or not) DOM
     * @throws XPathExpressionException a XPathExpressionException
     */
    public Document changeNode(Document xmlDOM, String path, String value) throws XPathExpressionException {
        Object result = xpath.evaluate(path, xmlDOM, XPathConstants.NODE);
        if (result instanceof Node) {
            Node nodeResult = (Node) result;
            Node child = nodeResult.getFirstChild();
            /*
             * System.out.println("Node : " + child.getNodeName()); System.out.println("Old value : " + child.getNodeValue()); child.setNodeValue(value); System.out.println("New value : " +
             * child.getNodeValue());
             */
            CDATASection cdata = xmlDOM.createCDATASection(value);
            if (child != null) {
                nodeResult.removeChild(child);
            }
            nodeResult.appendChild(cdata);
        }
        return xmlDOM;
    }

    /**
     * Here we remove a node found using the xpath query. The change affects the DOM received as parameter, so we return it to allow the caller to use it in every way he wants.
     * 
     * @param xmlDOM
     *            : the DOM of the XML
     * @param path
     *            : path query string
     * @return xmlDOM : the changed (or not) DOM
     * @throws XPathExpressionException a XPathExpressionException
     */
    public Document removeNode(Document xmlDOM, String path) throws XPathExpressionException {
        Object result = xpath.evaluate(path, xmlDOM, XPathConstants.NODE);
        if (result instanceof Node) {
            Node nodeResult = (Node) result;
            nodeResult.getParentNode().removeChild(nodeResult);
        }
        return xmlDOM;
    }

    /**
     * Check if a node exists in the xml document with an xpath query
     * @param xmlDOM : document to look for the node in
     * @param path : xpath query
     * @return true if the node has been found, false otherwise
     * @throws XPathExpressionException a XPathExpressionException
     */
    public boolean nodeExists(Document xmlDOM, String path) throws XPathExpressionException {
        Object result = xpath.evaluate(path, xmlDOM);
        if (result != null && !result.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * In this method we extract the VALUES of the nodes result of the xpath query. Examples : consider the following xml fragment <applicationData>
     * <applicationStatus><![CDATA[ASD]]></applicationStatus> <applicationStatus><![CDATA[onlyForExample]]></applicationStatus> <physicalMessage><![CDATA[MSGTYPE_FIX]]></physicalMessage>
     * <applicationMessage><![CDATA[APPL_MSGTYPE_TRADE]]></applicationMessage> </applicationData>
     * 
     * - with the xpath query /message/applicationData/applicationStatus[text()="ASD"] we extract the applicationStatus node which contains the text "ASD", here we return the "ASD" value - with the
     * xpath query /message/applicationData/applicationStatus/text() we extract all the applicationStatus nodes and fetch their text value, here we return "ASD" and "onlyForExample"
     * 
     * @param sourceXml
     *            : the XML into which we must execute the xpath query
     * @param xpathQuery
     *            : the XPath query
     * @return results : a List of Strings containing the values found
     */
    public List<String> extractValues(String sourceXml, String xpathQuery) {
        List<String> results = new ArrayList<String>();
        try {
            expr = xpath.compile(xpathQuery);
            Document xmlDOM = XMLUtils.stringToDom(sourceXml, namespaceContext == null ? false : true);
            Object result = expr.evaluate(xmlDOM, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                // Check if we have a deeper node (a child), if so the value is inside it,
                // otherwise we can fetch the value from the current node.
                if (nodes.item(i).hasChildNodes()) {
                    results.add(nodes.item(i).getFirstChild().getNodeValue());
                } else {
                    results.add(nodes.item(i).getNodeValue());
                }
            }
        } catch (Exception e) {
            logger.error("Error while extracting node values from the XML using the xpath query {}", xpathQuery, e);
        }
        return results;
    }

    /**
     * In this method we extract the nodes requested in the xpath query Examples : consider the following xml fragment <applicationData> <applicationStatus><![CDATA[ASD]]></applicationStatus>
     * <applicationStatus><![CDATA[onlyForExample]]></applicationStatus> <physicalMessage><![CDATA[MSGTYPE_FIX]]></physicalMessage>
     * <applicationMessage><![CDATA[APPL_MSGTYPE_TRADE]]></applicationMessage> </applicationData>
     * 
     * - with the xpath query /message/applicationData/applicationStatus"] we extract all the applicationSTatus nodes without fetching their values
     * 
     * @param sourceXml
     *            : the XML into which we must execute the xpath query
     * @param xpathQuery
     *            : the XPath query
     * @return nodes : a NodeList containing all the Nodes found.
     */
    public NodeList extractNodes(String sourceXml, String xpathQuery) {
        NodeList nodes = null;
        try {
            expr = xpath.compile(xpathQuery);
            Object result = expr.evaluate(XMLUtils.stringToDom(sourceXml, namespaceContext == null ? false : true), XPathConstants.NODESET);
            nodes = (NodeList) result;
        } catch (Exception e) {
            logger.error("Error while extracting nodes from the XML using the xpath query {}", xpathQuery, e);
        }
        return nodes;
    }
}
