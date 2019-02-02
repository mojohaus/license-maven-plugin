package org.codehaus.mojo.license.utils;

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

import org.codehaus.mojo.license.Eol;
import org.codehaus.mojo.license.model.ProjectLicense;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

/**
 * A LicenseSummaryWriter.
 *
 * @author Paul Gier
 * @version $Revision$
 * @since 1.0
 */
public class LicenseSummaryWriter
{
    public static void writeLicenseSummary( List<ProjectLicenseInfo> dependencies, File outputFile, Charset charset,
            Eol eol )
        throws ParserConfigurationException, TransformerException, IOException
    {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = fact.newDocumentBuilder();
        Document doc = parser.newDocument();

        Node root = doc.createElement( "licenseSummary" );
        doc.appendChild( root );
        Node dependenciesNode = doc.createElement( "dependencies" );
        root.appendChild( dependenciesNode );

        for ( ProjectLicenseInfo dep : dependencies )
        {
            dependenciesNode.appendChild( createDependencyNode( doc, dep ) );
        }

        // Prepare the output file File
        try ( StringWriter sw = new StringWriter() )
        {
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            xformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
            xformer.transform( new DOMSource( doc ), new StreamResult( sw ) );

            final String platformEol = Eol.PLATFORM.getEolString();
            final String outputString = !platformEol.equals( eol.getEolString() )
                        ? sw.toString().replace( platformEol, eol.getEolString() )
                        : sw.toString();
            Files.write( outputFile.toPath(), outputString.getBytes( charset ) );
        }
    }

    public static Node createDependencyNode( Document doc, ProjectLicenseInfo dep )
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
            for ( ProjectLicense lic : dep.getLicenses() )
            {
                licensesNode.appendChild( createLicenseNode( doc, lic ) );
            }
        }
        depNode.appendChild( licensesNode );
        return depNode;

    }

    public static Node createLicenseNode( Document doc, ProjectLicense lic )
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

        if ( lic.getFile() != null )
        {
            Node licFileNode = doc.createElement( "file" );
            licFileNode.appendChild( doc.createTextNode( lic.getFile() ) );
            licenseNode.appendChild( licFileNode );
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