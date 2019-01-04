package it.softsolutions.bestx.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 
 * Purpose: XML documents manipulation utilities
 * 
 * Project Name : mq-service First created by: ruggero.rizzo Creation date: 24/gen/2013
 * 
 **/
public class XMLUtils {

    /**
     * Convert an xml in a String to a DOM Document
     * 
     * @param xmlSource
     *            xml origin
     * @param nameSpaceAware
     *            true if the document contains namespaces we must be aware of (used for xpath queries)
     * @return the Document
     * @throws SAXException a SAXException
     * @throws ParserConfigurationException a ParserConfigurationException
     * @throws IOException an IOException
     */
    public static Document stringToDom(String xmlSource, boolean nameSpaceAware) throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(nameSpaceAware);
        DocumentBuilder parser = factory.newDocumentBuilder();
        
        return parser.parse(new ByteArrayInputStream(xmlSource.getBytes()));
    }

    /**
     * Convert an xml DOM Document in an xml String
     * 
     * @param doc
     *            : original DOM document
     * @return the xml String
     * @throws TransformerException a TransformerException
     */
    public static String getXMLDocString(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        
        // initialize StreamResult with File object to save to file
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        String xmlString = result.getWriter().toString();
        return xmlString;
    }
}
