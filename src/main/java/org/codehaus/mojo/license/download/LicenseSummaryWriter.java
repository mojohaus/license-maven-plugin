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

import org.codehaus.mojo.license.Eol;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Eol eol, boolean writeVersions )
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
            dependenciesNode.appendChild( createDependencyNode( doc, dep, writeVersions ) );
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

    public static Node createDependencyNode( Document doc, ProjectLicenseInfo dep, boolean writeVersions )
    {
        final List<String> messages = dep.getDownloaderMessages();
        final boolean hasDownloaderMessages = messages != null && !messages.isEmpty();

        final Node depNode = doc.createElement( "dependency" );

        final Node groupIdNode = doc.createElement( "groupId" );
        groupIdNode.appendChild( doc.createTextNode( patternOrText( dep.getGroupId(), hasDownloaderMessages ) ) );
        depNode.appendChild( groupIdNode );

        final Node artifactIdNode = doc.createElement( "artifactId" );
        artifactIdNode.appendChild( doc.createTextNode( patternOrText( dep.getArtifactId(), hasDownloaderMessages ) ) );
        depNode.appendChild( artifactIdNode );

        if ( writeVersions )
        {
            final Node versionNode = doc.createElement( "version" );
            versionNode.appendChild( doc.createTextNode( patternOrText( dep.getVersion(), hasDownloaderMessages ) ) );
            depNode.appendChild( versionNode );
        }
        else if ( hasDownloaderMessages  )
        {
            depNode.appendChild( doc.createComment( " <version>" + dep.getVersion() + "</version> " ) );
        }

        if ( hasDownloaderMessages  )
        {
            Node matchLicensesNode = doc.createElement( "matchLicenses" );
            if ( dep.getLicenses() == null || dep.getLicenses().size() == 0 )
            {
                matchLicensesNode.appendChild( doc.createComment( " Match dependency with no licenses " ) );
            }
            else
            {
                for ( ProjectLicense lic : dep.getLicenses() )
                {
                    matchLicensesNode.appendChild( createLicenseNode( doc, lic, true ) );
                }
            }
            depNode.appendChild( matchLicensesNode );
        }


        Node licensesNode = doc.createElement( "licenses" );
        if ( dep.getLicenses() == null || dep.getLicenses().size() == 0 )
        {
            final String comment =
                hasDownloaderMessages ? " Manually add license elements here: " : " No license information available. ";
            licensesNode.appendChild( doc.createComment( comment ) );
        }
        else
        {
            if ( hasDownloaderMessages )
            {
                licensesNode.appendChild( doc.createComment( " Manually fix the existing license nodes: " ) );
            }
            for ( ProjectLicense lic : dep.getLicenses() )
            {
                licensesNode.appendChild( createLicenseNode( doc, lic, false ) );
            }
        }
        depNode.appendChild( licensesNode );

        if ( hasDownloaderMessages )
        {
            final Node downloaderMessagesNode = doc.createElement( "downloaderMessages" );
            for ( String msg : messages )
            {
                final Node downloaderMessageNode = doc.createElement( "downloaderMessage" );
                downloaderMessageNode.appendChild( doc.createTextNode( msg ) );
                downloaderMessagesNode.appendChild( downloaderMessageNode );
            }
            depNode.appendChild( downloaderMessagesNode );
        }

        return depNode;

    }

    public static Node createLicenseNode( Document doc, ProjectLicense lic, boolean isMatcher )
    {
        Node licenseNode = doc.createElement( "license" );

        if ( lic.getName() != null )
        {
            Node licNameNode = doc.createElement( "name" );
            licNameNode.appendChild( doc.createTextNode( patternOrText( lic.getName(), isMatcher ) ) );
            licenseNode.appendChild( licNameNode );
        }

        if ( lic.getUrl() != null )
        {
            Node licUrlNode = doc.createElement( "url" );
            licUrlNode.appendChild( doc.createTextNode( patternOrText( lic.getUrl(), isMatcher ) ) );
            licenseNode.appendChild( licUrlNode );
        }

        if ( lic.getDistribution() != null )
        {
            Node licDistNode = doc.createElement( "distribution" );
            licDistNode.appendChild( doc.createTextNode( patternOrText( lic.getDistribution(), isMatcher ) ) );
            licenseNode.appendChild( licDistNode );
        }

        if ( lic.getFile() != null )
        {
            Node licFileNode = doc.createElement( "file" );
            licFileNode.appendChild( doc.createTextNode( patternOrText( lic.getFile(), isMatcher ) ) );
            licenseNode.appendChild( licFileNode );
        }

        if ( lic.getComments() != null )
        {
            Node licCommentsNode = doc.createElement( "comments" );
            licCommentsNode.appendChild( doc.createTextNode( patternOrText( lic.getComments(), isMatcher ) ) );
            licenseNode.appendChild( licCommentsNode );
        }

        return licenseNode;
    }

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile( "\\s{2,}" );

    static String patternOrText( String value, boolean isMatcher )
    {
        if ( value != null && !value.isEmpty() && isMatcher )
        {
            final StringBuilder result = new StringBuilder();
            final Matcher m = WHITESPACE_PATTERN.matcher( value );
            int offset = 0;
            while ( m.find() )
            {
                if ( m.start() > offset )
                {
                    result.append( Pattern.quote( value.substring( offset, m.start() ) ) );
                }
                result.append( "\\s+" );
                offset = m.end();
            }
            if ( offset < value.length() )
            {
                result.append( Pattern.quote( value.substring( offset ) ) );
            }
            return result.toString();
        }
        else
        {
            return value;
        }
    }

}
