package org.codehaus.mojo.license;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.model.License;
import org.codehaus.mojo.license.model.DependencyProject;
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
 */
public class LicenseSummaryReader
{

    /**
     * Read a component-info.xml from an input stream into a ComponentInfo object.
     * 
     * @param licSummaryIS Input stream containing the license data
     * @return List of DependencyProject objects
     * @throws IOException if there is a problem reading the InputStream
     * @throws ParserConfigurationException if there is a problem parsing the XML stream
     * @throws SAXException if there is a problem parsing the XML stream
     */
    public static List<DependencyProject> parseLicenseSummary( InputStream licSummaryIS )
        throws IOException, ParserConfigurationException, SAXException
    {
        List<DependencyProject> dependencies = new ArrayList<DependencyProject>();

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse( licSummaryIS );

        // normalize text representation
        doc.getDocumentElement().normalize();
        Element documentElement = doc.getDocumentElement();

        Node dependenciesNode = documentElement.getElementsByTagName( "dependencies" ).item( 0 );
        NodeList dependencyNodes = dependenciesNode.getChildNodes();

        for ( int i = 0; i < dependencyNodes.getLength(); ++i )
        {
            Node dependencyNode = dependencyNodes.item( i );
            if ( dependencyNode.getNodeType() == Node.ELEMENT_NODE )
            {
                dependencies.add( parseDependencyNode( dependencyNode ) );
            }
        }

        return dependencies;

    }

    private static DependencyProject parseDependencyNode( Node dependencyNode )
    {
        DependencyProject dependency = new DependencyProject();
        NodeList depElements = dependencyNode.getChildNodes();
        for ( int i = 0; i < depElements.getLength(); ++i )
        {
            Node node = depElements.item( i );

            if ( node.getNodeName().equals( "groupId" ) )
            {
                dependency.setGroupId( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "artifactId" ) )
            {
                dependency.setArtifactId( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "version" ) )
            {
                dependency.setVersion( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "licenses" ) )
            {
                NodeList licensesChildNodes = node.getChildNodes();
                for ( int j = 0; j < licensesChildNodes.getLength(); ++j )
                {
                    Node licensesChildNode = licensesChildNodes.item( j );
                    if ( licensesChildNode.getNodeName().equals( "license" ) )
                    {
                        License license = parseLicense( licensesChildNode );
                        dependency.addLicense( license );
                    }
                }
            }
        }
        return dependency;
    }

    private static License parseLicense( Node licenseNode )
    {
        License license = new License();
        NodeList licenseElements = licenseNode.getChildNodes();
        for ( int i = 0; i < licenseElements.getLength(); ++i )
        {
            Node node = licenseElements.item( i );
            if ( node.getNodeName().equals( "name" ) )
            {
                license.setName( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "url" ) )
            {
                license.setUrl( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "distribution" ) )
            {
                license.setDistribution( node.getTextContent() );
            }
            else if ( node.getNodeName().equals( "comments" ) )
            {
                license.setComments( node.getTextContent() );
            }
        }
        return license;
    }

}
