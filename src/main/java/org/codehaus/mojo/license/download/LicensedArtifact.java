package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 MojoHaus
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.codehaus.mojo.license.spdx.SpdxLicenseInfo;
import org.codehaus.mojo.license.spdx.SpdxLicenseList;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>, Jan-Hendrik Diederich (for the extended information)
 * @since 1.20
 */
public class LicensedArtifact
{
    private static final Logger LOG = LoggerFactory.getLogger( LicensedArtifact.class );

    public static Builder builder( Artifact artifact, boolean useNonMavenData )
    {
        return new Builder( artifact, useNonMavenData );
    }

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final List<License> licenses;

    private final List<String> errorMessages;

    private final ExtendedInfo extendedInfos;

    LicensedArtifact( String groupId, String artifactId, String version, List<License> licenses,
                      List<String> errorMessages, ExtendedInfo extendedInfos )
    {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.licenses = licenses;
        this.errorMessages = errorMessages;
        this.extendedInfos = extendedInfos;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = prime * result + ( ( licenses == null ) ? 0 : licenses.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        // CHECKSTYLE_OFF: NeedBraces
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        LicensedArtifact other = (LicensedArtifact) obj;
        if ( artifactId == null )
        {
            if ( other.artifactId != null )
                return false;
        }
        else if ( !artifactId.equals( other.artifactId ) )
            return false;
        if ( groupId == null )
        {
            if ( other.groupId != null )
                return false;
        }
        else if ( !groupId.equals( other.groupId ) )
            return false;
        if ( licenses == null )
        {
            if ( other.licenses != null )
                return false;
        }
        else if ( !licenses.equals( other.licenses ) )
            return false;
        if ( version == null )
        {
            if ( other.version != null )
                return false;
        }
        else if ( !version.equals( other.version ) )
            return false;
        return true;
        // CHECKSTYLE_ON: NeedBraces
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public List<License> getLicenses()
    {
        return licenses;
    }

    public List<String> getErrorMessages()
    {
        return errorMessages;
    }

    /**
     * Gets the extended information.
     *
     * @return Extended information.
     * @since 2.1.0
     */
    public ExtendedInfo getExtendedInfos()
    {
        return extendedInfos;
    }

    /**
     * A {@link LicensedArtifact} builder.
     */
    public static class Builder
    {
        public Builder( Artifact artifact, boolean useNonMavenData )
        {
            this.groupId = artifact.getGroupId();
            this.artifactId = artifact.getArtifactId();
            this.version = artifact.getVersion();
            this.extendedInfos = useNonMavenData
                                        ? extraInfosFromArtifact( artifact )
                                        : null;
        }

        private final String groupId;

        private final String artifactId;

        private final String version;

        private List<License> licenses = new ArrayList<>();

        private List<String> errorMessages = new ArrayList<>();

        private final ExtendedInfo extendedInfos;

        public Builder errorMessage( String errorMessage )
        {
            this.errorMessages.add( errorMessage );
            return this;
        }

        public Builder license( License license )
        {
            this.licenses.add( license );
            return this;
        }

        public LicensedArtifact build()
        {
            final List<License> lics = Collections.unmodifiableList( licenses );
            licenses = null;
            final List<String> msgs = Collections.unmodifiableList( errorMessages );
            errorMessages = null;
            return new LicensedArtifact( groupId, artifactId, version, lics, msgs, extendedInfos );
        }

        private ExtendedInfo extraInfosFromArtifact( Artifact artifact )
        {
            if ( artifact.getFile() == null )
            {
                LOG.error( "Artifact " + artifact + " has no valid file set" );
                return null;
            }
            ExtendedInfo result = new ExtendedInfo();
            result.setArtifact( artifact );
            try ( ZipFile zipFile = new ZipFile( artifact.getFile() ) )
            {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while ( entries.hasMoreElements() )
                {
                    ZipEntry zipEntry = entries.nextElement();
                    final String fileName = zipEntry.getName().toLowerCase();
                    if ( textFileMatcher( fileName, "notice" ) )
                    {
                        // Should match "NOTICE.txt", "NOTICES.txt"...,
                        // if someone decides to go slightly against convention.
                        final InfoFile infoFile = buildInfoFile( zipFile, zipEntry, InfoFile.Type.NOTICE );
                        result.getInfoFiles().add( infoFile );
                    }
                    else if ( textFileMatcher( fileName, "license", "licence" ) )
                    {
                        // Match against british and american english writing type of "license"
                        final InfoFile infoFile = buildInfoFile( zipFile, zipEntry, InfoFile.Type.LICENSE );
                        result.getInfoFiles().add( infoFile );
                    }
                    else if ( fileMatchesSpdxId( fileName ) )
                    {
                        final InfoFile infoFile = buildInfoFile( zipFile, zipEntry, InfoFile.Type.SPDX_LICENSE );
                        result.getInfoFiles().add( infoFile );
                    }
                    else if ( fileName.equals( "meta-inf/manifest.mf" ) )
                    {
                        try ( InputStream inputStream = zipFile.getInputStream( zipEntry ) )
                        {
                            Manifest manifest = new Manifest( inputStream );
                            final Attributes mainAttributes = manifest.getMainAttributes();
                            // Fetch Java standard JAR manifest attributes.
                            final Object implementationVendor = mainAttributes
                                    .get( Attributes.Name.IMPLEMENTATION_VENDOR );
                            if ( implementationVendor instanceof String )
                            {
                                result.setImplementationVendor( (String) implementationVendor );
                            }
                            // Fetch OSGI framework JAR manifest attributes.
                            final String bundleVendor = mainAttributes.getValue( Constants.BUNDLE_VENDOR );
                            result.setBundleVendor( bundleVendor );
                            final String bundleLicense = mainAttributes.getValue( Constants.BUNDLE_LICENSE );
                            result.setBundleLicense( bundleLicense );
                        }
                        catch ( IOException e )
                        {
                            LOG.warn( "Error at reading data from jar manifest", e );
                        }
                    }
                }
            }
            catch ( IOException e )
            {
                LOG.warn( "Can't open zip file \"" + artifact.getFile() + "\"", e );
            }
            return result;
        }

        private InfoFile buildInfoFile( ZipFile zipFile, ZipEntry zipEntry, InfoFile.Type type )
        {
            InfoFile infoFile = new InfoFile();
            infoFile.setFileName( zipEntry.getName() );
            infoFile.setType( type );
            Pair<String, String[]> contentWithLines = readZipEntryTextLines( zipFile, zipEntry );
            if ( contentWithLines != null )
            {
                Set<String> copyrights = scanForCopyrights( contentWithLines.getRight(), "(c)", "copyright" );
                if ( !CollectionUtils.isEmpty( copyrights ) )
                {
                    infoFile.getExtractedCopyrightLines().addAll( copyrights );
                }
                infoFile.setContent( contentWithLines.getLeft() );
            }
            return infoFile;
        }

        private boolean fileMatchesSpdxId( String fileName )
        {
            final SpdxLicenseList spdxList = SpdxLicenseList.getLatest();
            for ( Map.Entry<String, SpdxLicenseInfo> entry : spdxList.getLicenses().entrySet() )
            {
                if ( textFileMatcher( fileName, entry.getValue().getLicenseId().toLowerCase() ) )
                {
                    return true;
                }
            }
            return false;
        }

        private boolean textFileMatcher( String fileName, String... matchStrings )
        {
            for ( String matchString : matchStrings )
            {
                if ( fileName.matches( ".*" + matchString + ".*\\.txt" ) )
                {
                    return true;
                }
            }
            return false;
        }

        /**
         * Scans for given copyright matchers in param copyrightMatchers.
         *
         * @param lines             The lines to check
         * @param copyrightMatchers Lines containing one of these strings are returned. Arguments must be all lowercase.
         * @return The found lines containing copyright claims.
         */
        private Set<String> scanForCopyrights( String[] lines, String... copyrightMatchers )
        {
            if ( lines == null )
            {
                return null;
            }
            Set<String> result = new HashSet<>();
            for ( String line : lines )
            {
                for ( String copyrightMatcher : copyrightMatchers )
                {
                    final String trimmedLine = line.trim();
                    if ( trimmedLine.toLowerCase().contains( copyrightMatcher ) )
                    {
                        result.add( trimmedLine );
                    }
                }
            }
            return result;
        }

        private Pair<String, String[]> readZipEntryTextLines( ZipFile zipFile, ZipEntry zipEntry )
        {
            try ( InputStream inputStream = zipFile.getInputStream( zipEntry ) )
            {
                byte[] content = IOUtils.readFully( inputStream, (int) zipEntry.getSize() );
                String contentString = new String( content );
                return new ImmutablePair<>( contentString, contentString.split( "\\R+" ) );
            }
            catch ( IOException e )
            {
                LOG.warn( "Can't read zip file entry " + zipEntry, e );
                return null;
            }
        }

        public void setInceptionYear( String inceptionYear )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setInceptionYear( inceptionYear );
            }
        }

        public void setOrganization( Organization organization )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setOrganization( organization );
            }
        }

        public void setDevelopers( List<Developer> developers )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setDevelopers( developers );
            }
        }

        public void setUrl( String url )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setUrl( url );
            }
        }

        public void setScm( Scm scm )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setScm( scm );
            }
        }

        public void setName( String name )
        {
            if ( extendedInfos != null )
            {
                this.extendedInfos.setName( name );
            }
        }
    }
}