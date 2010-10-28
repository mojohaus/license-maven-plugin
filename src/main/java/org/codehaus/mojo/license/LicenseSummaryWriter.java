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

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.model.License;
import org.codehaus.mojo.license.model.DependencyProject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * A LicenseSummaryWriter.
 * 
 * @author Paul Gier
 * @version $Revision$
 */
public class LicenseSummaryWriter
{
    public static void writeLicenseSummary( List<DependencyProject> dependencies, File outputFile )
        throws ParserConfigurationException, TransformerException
    {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = fact.newDocumentBuilder();
        Document doc = parser.newDocument();

        Node root = doc.createElement( "licenseSummary" );
        doc.appendChild( root );
        Node dependenciesNode = doc.createElement( "dependencies" );
        root.appendChild( dependenciesNode );

        for ( DependencyProject dep : dependencies )
        {
            dependenciesNode.appendChild( createDependencyNode( doc, dep ) );
        }

        // Prepare the output file File
        Result result = new StreamResult( outputFile.toURI().getPath() );

        // Write the DOM document to the file
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        xformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
        xformer.transform( new DOMSource( doc ), result );
    }

    public static Node createDependencyNode( Document doc, DependencyProject dep )
    {
        Node depNode = doc.createElement( "dependency" );

        Node groupIdNode = doc.createElement( "groupId" );
        groupIdNode.appendChild( doc.createTextNode( dep.getGroupId() ) );
        depNode.appendChild( groupIdNode );

        Node artifactIdNode = doc.createElement( "artifactId" );
        artifactIdNode.appendChild( doc.createTextNode( dep.getArtifactId() ) );
        depNode.appendChild( artifactIdNode );

        Node versionNode = doc.createElement( "version" );
        versionNode.appendChild( doc.createTextNode( dep.getVersion() ) );
        depNode.appendChild( versionNode );

        Node licensesNode = doc.createElement( "licenses" );
        if ( dep.getLicenses() == null || dep.getLicenses().size() == 0 )
        {
            licensesNode.appendChild( doc.createComment( "No license information available. " ) );
        }
        else
        {
            for ( License lic : dep.getLicenses() )
            {
                licensesNode.appendChild( createLicenseNode( doc, lic ) );
            }
        }
        depNode.appendChild( licensesNode );
        return depNode;

    }

    public static Node createLicenseNode( Document doc, License lic )
    {
        Node licenseNode = doc.createElement( "license" );

        if ( lic.getName() != null )
        {
            Node licNameNode = doc.createElement( "name" );
            licNameNode.appendChild( doc.createTextNode( lic.getName() ) );
            licenseNode.appendChild( licNameNode );
        }

        if ( lic.getUrl() != null )
        {
            Node licUrlNode = doc.createElement( "url" );
            licUrlNode.appendChild( doc.createTextNode( lic.getUrl() ) );
            licenseNode.appendChild( licUrlNode );
        }

        if ( lic.getDistribution() != null )
        {
            Node licDistNode = doc.createElement( "distribution" );
            licDistNode.appendChild( doc.createTextNode( lic.getDistribution() ) );
            licenseNode.appendChild( licDistNode );            
        }
        
        if ( lic.getComments() != null )
        {
            Node licCommentsNode = doc.createElement( "comments" );
            licCommentsNode.appendChild( doc.createTextNode( lic.getComments() ) );
            licenseNode.appendChild( licCommentsNode );
        }

        return licenseNode;
    }

}
