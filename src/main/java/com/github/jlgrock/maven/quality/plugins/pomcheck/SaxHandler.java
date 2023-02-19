package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Stack;

/**
 * A Simple class for searching pom files via a SAX XML handler. This does make the assumption that
 * pom files are written in xml.
 */
public class SaxHandler extends DefaultHandler {
    private Locator locator;
    private final Document doc;
    private final String lineNumAttribName;
    private final Stack<Element> elementStack;
    private final StringBuilder textBuffer;

    public SaxHandler(final Document docIn, final String lineNumAttribNameIn,
               final Stack<Element> elementStackIn, final StringBuilder textBufferIn) {
        doc = docIn;
        lineNumAttribName = lineNumAttribNameIn;
        elementStack = elementStackIn;
        textBuffer = textBufferIn;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator; //Save the locator, so that it can be used later for line tracking when traversing nodes.
    }

    @Override
    public void startElement(final String uri, final String localName,
                             final String qName, final Attributes attributes) throws SAXException {
        addTextIfNeeded();
        Element el = doc.createElement(qName);
        for(int i = 0;i < attributes.getLength(); i++)
            el.setAttribute(attributes.getQName(i), attributes.getValue(i));
        el.setUserData(lineNumAttribName, String.valueOf(locator.getLineNumber()), null);
        elementStack.push(el);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName){
        addTextIfNeeded();
        Element closedEl = elementStack.pop();
        if (elementStack.isEmpty()) { // Is this the root element?
            doc.appendChild(closedEl);
        } else {
            Element parentEl = elementStack.peek();
            parentEl.appendChild(closedEl);
        }
    }

    @Override
    public void characters (final char ch[], final int start, int length) throws SAXException {
        textBuffer.append(ch, start, length);
    }

    // Outputs text accumulated under the current node
    private void addTextIfNeeded() {
        if (textBuffer.length() > 0) {
            Element el = elementStack.peek();
            Node textNode = doc.createTextNode(textBuffer.toString());
            el.appendChild(textNode);
            textBuffer.delete(0, textBuffer.length());
        }
    }
}
