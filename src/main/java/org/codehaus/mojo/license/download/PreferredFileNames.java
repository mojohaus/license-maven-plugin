package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Codehaus
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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.codehaus.mojo.license.spdx.SpdxLicenseInfo;
import org.codehaus.mojo.license.spdx.SpdxLicenseInfo.Attachments.UrlInfo;
import org.codehaus.mojo.license.spdx.SpdxLicenseList;
import org.codehaus.mojo.license.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An url -&gt; {@link FileNameEntry} mapping.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
// CHECKSTYLE_OFF: MethodLength
public class PreferredFileNames
{
    private static final Logger LOG = LoggerFactory.getLogger( PreferredFileNames.class );
    /**
     * @param licensesOutputDirectory
     * @param licenseUrlFileNames
     * @return a new {@link PreferredFileNames} built of AbstractDownloadLicensesMojo.licenseUrlFileNames
     */
    public static PreferredFileNames build( File licensesOutputDirectory, Map<String, String> licenseUrlFileNames )
    {
        final Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns = new LinkedHashMap<>();
        final Map<String, String> sha1TofileName = new LinkedHashMap<String, String>();


        if ( licenseUrlFileNames != null )
        {
            if ( licenseUrlFileNames.containsKey( "spdx" ) )
            {
                spdx( fileNameToUrlPatterns, sha1TofileName );
            }

            for ( Entry<String, String> en : licenseUrlFileNames.entrySet() )
            {
                final String fileName = en.getKey();
                if ( fileName != null && !fileName.isEmpty() )
                {
                    if ( "spdx".equals( fileName ) )
                    {
                        // ignore
                    }
                    else if ( en.getValue() != null )
                    {
                        String[] rawPatters = en.getValue().split( "\\s+" );
                        if ( rawPatters != null )
                        {
                            final List<Pattern> patterns = new ArrayList<>();
                            String sha1 = null;
                            for ( String rawPattern : rawPatters )
                            {
                                if ( rawPattern.startsWith( "sha1:" ) )
                                {
                                    if ( sha1 != null )
                                    {
                                        throw new IllegalStateException( "sha1 defined twice for licenseFileName '"
                                            + fileName + "'" );
                                    }
                                    sha1 = rawPattern.substring( 5 );
                                }
                                else
                                {
                                    patterns.add( Pattern.compile( rawPattern, Pattern.CASE_INSENSITIVE ) );
                                }
                            }
                            if ( sha1 != null )
                            {
                                sha1TofileName.put( sha1, fileName );
                            }
                            fileNameToUrlPatterns.put( fileName,
                                    new AbstractMap.SimpleImmutableEntry<>(
                                                    sha1,
                                                    Collections.unmodifiableList( patterns ) ) );
                        }
                    }
                }
            }
        }
        return new PreferredFileNames( licensesOutputDirectory, fileNameToUrlPatterns, sha1TofileName );

    }

    private static <V, K> void spdx( Map<String, Entry<String, List<Pattern>>> result,
                              Map<String, String> sha1TofileName )
    {
        final Map<String, Entry<String, List<Pattern>>> fileNameToUrlPatterns = new TreeMap<>();
        final SpdxLicenseList spdxList = SpdxLicenseList.getLatest();
        final Map<String, SpdxLicenseInfo> lics = spdxList.getLicenses();
        LOG.info( "Honoring {} SPDX licenses", lics.size() );

        /* Count in how many licenses is the given sha1 used */
        final Map<String, Set<String>> sha1ToLicenseIds = new HashMap<>();
        final Map<String, Set<String>> sha1ToUrls = new HashMap<>();
        final Set<String> stableSha1s = new HashSet<>();
        for ( SpdxLicenseInfo lic : lics.values() )
        {
            if ( !lic.isDeprecatedLicenseId() )
            {
                for ( Entry<String, UrlInfo> urlInfoEntry : lic.getAttachments().getUrlInfos().entrySet() )
                {
                    final UrlInfo urlInfo = urlInfoEntry.getValue();
                    if ( urlInfo.getSha1() != null && urlInfo.getMimeType() != null )
                    {
                        final String sha1 = urlInfo.getSha1();
                        if ( urlInfo.isStable() )
                        {
                            stableSha1s.add( sha1 );
                        }
                        Set<String> licIds = sha1ToLicenseIds.get( sha1 );
                        if ( licIds == null )
                        {
                            licIds = new TreeSet<>();
                            sha1ToLicenseIds.put( sha1, licIds );
                        }
                        licIds.add( lic.getLicenseId() );

                        Set<String> urls = sha1ToUrls.get( sha1 );
                        if ( urls == null )
                        {
                            urls = new TreeSet<>();
                            sha1ToUrls.put( sha1, urls );
                        }
                        urls.add( urlInfoEntry.getKey() );
                    }
                }
            }
        }
        /* For license sets in which a given sha1 is used, check if a <licenseId(s)>.<typeExt> name would be unique */
        for ( Entry<String, Set<String>> sha1LicenseId : sha1ToLicenseIds.entrySet() )
        {
            final Set<String> licIds = sha1LicenseId.getValue();
            final StringBuilder baseNameBuilder = new StringBuilder();
            final Map<String, Set<String>> mimeTypeToSha1 = new HashMap<>();
            for ( String licId : licIds )
            {
                if ( baseNameBuilder.length() > 0 )
                {
                    baseNameBuilder.append( "-OR-" );
                }
                baseNameBuilder.append( licId );

                final SpdxLicenseInfo lic = lics.get( licId );
                for ( Entry<String, UrlInfo> urlInfoEntry : lic.getAttachments().getUrlInfos().entrySet() )
                {
                    final UrlInfo urlInfo = urlInfoEntry.getValue();
                    final String mimeType = urlInfo.getMimeType();
                    if ( sha1ToLicenseIds.containsKey( urlInfo.getSha1() ) && mimeType != null )
                    {
                        Set<String> sha1s = mimeTypeToSha1.get( mimeType );
                        if ( sha1s == null )
                        {
                            sha1s = new LinkedHashSet<>();
                            mimeTypeToSha1.put( mimeType, sha1s );
                        }
                        sha1s.add( urlInfo.getSha1() );
                    }
                }
            }
            final String baseName = baseNameBuilder.toString();

            for ( Entry<String, Set<String>> mimeTypeSha1 : mimeTypeToSha1.entrySet() )
            {
                final String mimeType = mimeTypeSha1.getKey();
                if ( mimeType == null )
                {
                    throw new IllegalStateException( "mimeType must not be null" );
                }
                final Set<String> sha1s = mimeTypeSha1.getValue();
                for ( String sha1 : sha1s )
                {
                    final String uniqueName =
                                    sha1s.size() == 1 ? baseName : ( baseName + "-" + sha1.substring( 0, 7 ) );
                    final String fileName = uniqueName + FileUtil.toExtension( mimeType, true );
                    final List<Pattern> patterns = new ArrayList<>();

                    Set<String> urls = sha1ToUrls.get( sha1 );
                    for ( String url : urls )
                    {
                        patterns.add( Pattern.compile( Pattern.quote( url ), Pattern.CASE_INSENSITIVE ) );
                    }

                    final String useSha1 = stableSha1s.contains( sha1 ) ? sha1 : null;
                    if ( useSha1 != null )
                    {
                        sha1TofileName.put( useSha1, fileName );
                    }
                    final List<Pattern> usePatterns = Collections.unmodifiableList( patterns );

                    final Entry<String, List<Pattern>> avail = fileNameToUrlPatterns.get( fileName );
                    if ( avail == null )
                    {
                        fileNameToUrlPatterns.put( fileName,
                                                   new AbstractMap.SimpleImmutableEntry<>( useSha1, usePatterns ) );
                    }
                    else if ( !( avail.getKey() == useSha1
                        || ( avail.getKey() != null && avail.getKey().equals( useSha1 ) ) )
                        || !isEqual( usePatterns, avail.getValue() ) )
                    {
                        LOG.warn( "Available: {}, {}, {}", fileName, avail.getKey(), avail.getValue() );
                        LOG.warn( "To add   : {}, {}, {}", fileName, useSha1, patterns );
                        throw new IllegalStateException( fileName + " already present" );
                    }

                }
            }
        }

        if ( LOG.isDebugEnabled() )
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "<licenseUrlFileNames>\n" );

            for ( Entry<String, Entry<String, List<Pattern>>> en : fileNameToUrlPatterns.entrySet() )
            {
                final String fileName = en.getKey();
                final String sha1 = en.getValue().getKey();
                final List<Pattern> patterns = en.getValue().getValue();
                sb.append( "  <" + fileName + ">\n" );
                if ( sha1 != null )
                {
                    sb.append( "    sha1:" + sha1 + "\n" );
                }
                for ( Pattern pattern : patterns )
                {
                    sb.append( "    " + pattern.pattern() + "\n" );
                }
                sb.append( "  </" + fileName + ">\n" );

            }
            sb.append( "</licenseUrlFileNames>\n" );
            LOG.debug( "SPDX licenseUrlFileNames:" );
            LOG.debug( sb.toString() );
        }
        result.putAll( fileNameToUrlPatterns );
    }

    private static boolean isEqual( final List<Pattern> newPatterns, List<Pattern> oldPatterns )
    {
        if ( oldPatterns.size() != newPatterns.size() )
        {
            return false;
        }
        final Iterator<Pattern> newIt = newPatterns.iterator();
        final Iterator<Pattern> oldIt = oldPatterns.iterator();
        while ( newIt.hasNext() )
        {
            if ( !newIt.next().pattern().equals( oldIt.next().pattern() ) )
            {
                return false;
            }
        }
        return true;
    }

    private final File licensesOutputDirectory;
    private final Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns;
    private final Map<String, String> sha1ToFileName;

    public PreferredFileNames( File licensesOutputDirectory,
                               Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns,
                               Map<String, String> sha1ToFileName )
    {
        super();
        this.licensesOutputDirectory = licensesOutputDirectory;
        this.fileNameToUrlPatterns = fileNameToUrlPatterns;
        this.sha1ToFileName = sha1ToFileName;
    }

    /**
     * @param sha1 the checksum to search by
     * @return a file name bound the given {@code sha1} checksum or {@code null}
     */
    public String getFileNameBySha1( String sha1 )
    {
        return sha1ToFileName.get( sha1 );
    }

    /**
     * @param url the URL to query
     * @return the preferred {@link FileNameEntry} for the given {@code url} or {@code null}
     */
    public FileNameEntry getEntryByUrl( String url )
    {
        for ( Entry<String, Map.Entry<String, List<Pattern>>> fn : fileNameToUrlPatterns.entrySet() )
        {
            for ( Pattern pat : fn.getValue().getValue() )
            {
                if ( pat.matcher( url ).matches() )
                {
                    LOG.debug( "Using file name '{}' for URL '{}' that matched pattern '{}'",
                        fn.getKey(),
                        url,
                        pat.pattern() );
                    return new FileNameEntry( new File( licensesOutputDirectory, fn.getKey() ), true,
                                              fn.getValue().getKey() );
                }
            }
        }
        return null;
    }
}