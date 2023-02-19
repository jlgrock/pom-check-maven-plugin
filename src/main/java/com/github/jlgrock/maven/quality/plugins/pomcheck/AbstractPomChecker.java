package com.github.jlgrock.maven.quality.plugins.pomcheck;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Stack;

/**
 *
 */
public abstract class AbstractPomChecker extends AbstractMojo {

    private static final String LINE_NUM_ATTR_NAME = "LINE_NUM";

    protected abstract MavenProject getMavenProject();

    protected abstract BuildContext getBuildContext();

    protected abstract void checkFile(final File pomFile) throws MojoExecutionException, MojoFailureException;

    /**
     * The entry point for a maven mojo.  This kicks off the maven scanning against the pom file.
     *
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkFile(getMavenProject().getFile());
    }

    public Document readXML(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<Element>();
        final StringBuilder textBuffer = new StringBuilder();
        DefaultHandler handler = new SaxHandler(doc, LINE_NUM_ATTR_NAME, elementStack, textBuffer);
        parser.parse(is, handler);

        return doc;
    }

    protected Document readXML(final File fileIn) throws IOException, SAXException {
        return readXML(new FileInputStream(fileIn));
    }

    protected String getLineNumber(final Node node) {
        return (String) node.getUserData(LINE_NUM_ATTR_NAME);
    }

    /**
     * Generate the string representing the artifact coordinates.
     *
     * @param node the node representing a single dependency
     * @return the string representing the artifact coordinates
     */
    protected String artifactBuilder(final Node node) {
        Element element = (Element) node;
        return artifactBuilder(element);
    }

    /**
     * Generate the string representing the artifact coordinates.
     *
     * @param element the element representing a single dependency
     * @return the string representing the artifact coordinates
     */
    protected String artifactBuilder(final Element element) {
        getLog().debug("ELEMENT: " + element.toString());
        String groupId = "[unknown-groupid]";
        String artifactId = "[unknown-artifactid]";
        String version = "[unknown-version]";

        NodeList groupIdNodeList = element.getElementsByTagName("groupId");
        if (groupIdNodeList.getLength() != 0) {
            groupId = element.getElementsByTagName("groupId").item(0).getTextContent();
        }
        NodeList artifactNodeList = element.getElementsByTagName("artifactId");
        if (artifactNodeList.getLength() != 0) {
            artifactId = artifactNodeList.item(0).getTextContent();
        }
        NodeList versionNodeList = element.getElementsByTagName("version");
        if (versionNodeList.getLength() != 0) {
            version = versionNodeList.item(0).getTextContent();
        }
        return groupId + ":" + artifactId + ":" + version;
    }

    /**
     * Using an xpath expression, gather the appropriate collection of tags
     *
     * @param doc             the document to search
     * @param path            the path to find the dependencies collection
     * @throws MojoExecutionException if there are any errors in execution
     * @throws MojoFailureException   if there are any properties invalidly used or where they shouldn't be
     */
    protected NodeList findByPath(final Document doc, final String path)
            throws MojoExecutionException, MojoFailureException {
        getLog().debug("finding By Path: " + path);
        NodeList nodeCollection;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            nodeCollection = (NodeList) xPath.evaluate(path, doc.getDocumentElement(), XPathConstants.NODESET);
            getLog().debug("size of collection matching \"" + path + "\": " + nodeCollection.getLength());
        } catch (XPathExpressionException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        return nodeCollection;
    }
}
