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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;

/**
 * A simple {@link HashMap} based in-memory cache for storing {@link LicenseDownloadResult}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class Cache
{
    private final Map<String, LicenseDownloadResult> urlToFile = new HashMap<>();

    private final Map<String, LicenseDownloadResult> sha1ToFile = new HashMap<>();

    private final boolean enforcingUniqueSha1s;

    public Cache( boolean enforcingUniqueSha1s )
    {
        super();
        this.enforcingUniqueSha1s = enforcingUniqueSha1s;
    }

    /**
     * @param url the cache key to seek
     * @return the {@link LicenseDownloadResult} bound to the given {@code url} or {@code null} if no entry is bound to
     *         the given {@code url}
     */
    public LicenseDownloadResult get( String url )
    {
        return urlToFile.get( url );
    }

    /**
     * Binds the given {@code url} to the give {@link LicenseDownloadResult}. If both {@link #enforcingUniqueSha1s} and
     * {@link LicenseDownloadResult#isSuccess()} are {@code true} and an entry with the given
     * {@link LicenseDownloadResult#getSha1()} already exists in {@link #sha1ToFile}, asserts that both the added and
     * the available entry point at the same file.
     *
     * @param url the URL the given {@code entry} comes from
     * @param entry the result of downloading from {@code url}
     */
    public void put( String url, LicenseDownloadResult entry )
    {
        if ( entry.isSuccess() )
        {
            final String sha1 = entry.getSha1();
            final File entryFile = entry.getFile();
            final LicenseDownloadResult existing = sha1ToFile.get( sha1 );
            if ( existing == null )
            {
                sha1ToFile.put( sha1, entry );
            }
            else if ( enforcingUniqueSha1s && !existing.getFile().equals( entryFile ) )
            {
                final File existingFile = existing.getFile();
                final StringBuilder sb = new StringBuilder();
                for ( Entry<String, LicenseDownloadResult> en : urlToFile.entrySet() )
                {
                    if ( existingFile.equals( en.getValue().getFile() ) )
                    {
                        if ( sb.length() > 0 )
                        {
                            sb.append( ", " );
                        }
                        sb.append( en.getKey() );
                    }
                }
                throw new IllegalStateException( "URL '" + url
                    + "' should belong to licenseUrlFileName having key '" + existingFile.getName()
                    + "' together with URLs '" + sb.toString() + "'" );
            }
        }
        urlToFile.put( url, entry );
    }

}