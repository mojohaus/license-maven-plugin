package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 Codehaus
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A LicenseSummaryReader.
 *
 * @author Paul Gier
 * @version $Revision$
 * @since 1.0
 */
public class LicenseSummaryReader {

    public static List<ProjectLicenseInfo> parseLicenseSummary(File licSummaryFile)
            throws IOException, ParserConfigurationException, SAXException {
        if (licSummaryFile.exists()) {
            try (InputStream in = Files.newInputStream(licSummaryFile.toPath())) {
                return parseLicenseSummary(in);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Read a component-info.xml from an input stream into a ComponentInfo object.
     *
     * @param licSummaryIS Input stream containing the license data
     * @return List of DependencyProject objects
     * @throws IOException                  if there is a problem reading the InputStream
     * @throws ParserConfigurationException if there is a problem parsing the XML stream
     * @throws SAXException                 if there is a problem parsing the XML stream
     */
    public static List<ProjectLicenseInfo> parseLicenseSummary(InputStream licSummaryIS)
            throws IOException, ParserConfigurationException, SAXException {
        List<ProjectLicenseInfo> dependencies = new ArrayList<>();

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(licSummaryIS);

        // normalize text representation
        doc.getDocumentElement().normalize();
        Element documentElement = doc.getDocumentElement();

        Node dependenciesNode =
                documentElement.getElementsByTagName("dependencies").item(0);
        NodeList dependencyNodes = dependenciesNode.getChildNodes();

        for (int i = 0; i < dependencyNodes.getLength(); ++i) {
            Node dependencyNode = dependencyNodes.item(i);
            if (dependencyNode.getNodeType() == Node.ELEMENT_NODE) {
                dependencies.add(parseDependencyNode(dependencyNode));
            }
        }

        return dependencies;
    }

    private static ProjectLicenseInfo parseDependencyNode(Node dependencyNode) {
        ProjectLicenseInfo dependency = new ProjectLicenseInfo();
        NodeList depElements = dependencyNode.getChildNodes();
        for (int i = 0; i < depElements.getLength(); ++i) {
            Node node = depElements.item(i);

            if (node.getNodeName().equals("groupId")) {
                dependency.setGroupId(node.getTextContent());
            } else if (node.getNodeName().equals("artifactId")) {
                dependency.setArtifactId(node.getTextContent());
            } else if (node.getNodeName().equals("version")) {
                dependency.setVersion(node.getTextContent());
            } else if (node.getNodeName().equals("scope")) {
                dependency.setScope(node.getTextContent());
            } else if (node.getNodeName().equals("licenses")) {
                Map.Entry<Boolean, List<ProjectLicense>> entry = parseLicenses(node);
                dependency.setLicenses(entry.getValue());
                dependency.setApproved(entry.getKey());
            } else if (node.getNodeName().equals("matchLicenses")) {
                dependency.setHasMatchLicenses(true);
                dependency.setMatchLicenses(parseLicenses(node).getValue());
            }
        }
        return dependency;
    }

    private static Map.Entry<Boolean, List<ProjectLicense>> parseLicenses(Node node) {
        final List<ProjectLicense> result = new ArrayList<ProjectLicense>();
        final NodeList licensesChildNodes = node.getChildNodes();
        final Node approvedNode = node.getAttributes().getNamedItem("approved");
        boolean approved = Boolean.parseBoolean(approvedNode != null ? approvedNode.getNodeValue() : "false");
        for (int j = 0; j < licensesChildNodes.getLength(); ++j) {
            final Node licensesChildNode = licensesChildNodes.item(j);
            final String nodeName = licensesChildNode.getNodeName();
            if (nodeName.equals("license")) {
                if (approved) {
                    throw new IllegalStateException("Cannot combine approved=\"true\" with <license> elements");
                }
                result.add(parseLicense(licensesChildNode));
            }
        }
        return new AbstractMap.SimpleImmutableEntry<Boolean, List<ProjectLicense>>(approved, result);
    }

    private static ProjectLicense parseLicense(Node licenseNode) {
        ProjectLicense license = new ProjectLicense();
        NodeList licenseElements = licenseNode.getChildNodes();
        for (int i = 0; i < licenseElements.getLength(); ++i) {
            Node node = licenseElements.item(i);
            if (node.getNodeName().equals("name")) {
                license.setName(node.getTextContent());
            } else if (node.getNodeName().equals("url")) {
                license.setUrl(node.getTextContent());
            } else if (node.getNodeName().equals("distribution")) {
                license.setDistribution(node.getTextContent());
            } else if (node.getNodeName().equals("comments")) {
                license.setComments(node.getTextContent());
            } else if (node.getNodeName().equals("file")) {
                license.setFile(node.getTextContent());
            }
        }
        return license;
    }
}
