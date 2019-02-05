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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

/**
 * An url -&gt; {@link FileNameEntry} mapping.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class PreferredFileNames
{
    /**
     * @param licensesOutputDirectory
     * @param licenseUrlFileNames
     * @param log
     * @return a new {@link PreferredFileNames} built of AbstractDownloadLicensesMojo.licenseUrlFileNames
     */
    public static PreferredFileNames build( File licensesOutputDirectory, Map<String, String> licenseUrlFileNames,
                                              Log log )
    {
        final Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns = new LinkedHashMap<>();
        final Map<String, String> sha1TofileName = new LinkedHashMap<String, String>();
        if ( licenseUrlFileNames != null )
        {
            for ( Entry<String, String> en : licenseUrlFileNames.entrySet() )
            {
                final String fileName = en.getKey();
                if ( fileName != null && !fileName.isEmpty() )
                {
                    if ( en.getValue() != null )
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
                            if ( sha1 == null )
                            {
                                throw new IllegalStateException( "sha1 undefined for licenseFileName '" + fileName
                                    + "'. Add 'sha1:<expected-sha1>' to the list of patterns '" + en.getValue()
                                    + "'" );
                            }
                            fileNameToUrlPatterns.put( fileName,
                                    new AbstractMap.SimpleImmutableEntry<>(
                                                    sha1,
                                                    Collections.unmodifiableList( patterns ) ) );
                            sha1TofileName.put( sha1, fileName );
                        }
                    }
                }
            }
        }
        return new PreferredFileNames( licensesOutputDirectory, fileNameToUrlPatterns, sha1TofileName, log );

    }

    private final File licensesOutputDirectory;
    private final Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns;
    private final Map<String, String> sha1ToFileName;
    private final Log log;

    public PreferredFileNames( File licensesOutputDirectory,
                               Map<String, Map.Entry<String, List<Pattern>>> fileNameToUrlPatterns,
                               Map<String, String> sha1ToFileName, Log log )
    {
        super();
        this.licensesOutputDirectory = licensesOutputDirectory;
        this.fileNameToUrlPatterns = fileNameToUrlPatterns;
        this.sha1ToFileName = sha1ToFileName;
        this.log = log;
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
                    log.debug( "Using file name '" + fn.getKey() + "' for URL '" + url + "' that matched pattern '"
                        + pat.pattern() + "'" );
                    return new FileNameEntry( new File( licensesOutputDirectory, fn.getKey() ), true,
                                              fn.getValue().getKey() );
                }
            }
        }
        return null;
    }
}