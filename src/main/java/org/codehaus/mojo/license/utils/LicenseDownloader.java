package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 CodeLutin, Codehaus, Tony Chemit
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.logging.Log;

/**
 * Utilities for downloading remote license files.
 *
 * @author pgier
 * @since 1.0
 */
public class LicenseDownloader implements AutoCloseable
{

    /**
     * Defines the connection timeout in milliseconds when attempting to download license files.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    private final CloseableHttpClient client;

    public LicenseDownloader()
    {
        final RequestConfig config = RequestConfig.copy( RequestConfig.DEFAULT )
            .setConnectTimeout( DEFAULT_CONNECTION_TIMEOUT )
            .setSocketTimeout( DEFAULT_CONNECTION_TIMEOUT )
            .setConnectionRequestTimeout( DEFAULT_CONNECTION_TIMEOUT )
            .build();
        this.client = HttpClients.custom().setDefaultRequestConfig( config ).build();
    }

    /**
     * Downloads a license file from the given {@code licenseUrlString} stores it locally and
     * returns the local path where the license file was stored. Note that the
     * {@code outputFile} name can be further modified by this method, esp. the file extension
     * can be adjusted based on the mime type of the HTTP response.
     *
     * @param licenseUrlString the URL
     * @param loginPassword the credentials part for the URL, can be {@code null}
     * @param outputFile a hint where to store the license file
     * @param log
     * @return the path to the file where the downloaded license file was stored
     * @throws IOException
     * @throws URISyntaxException
     */
    public LicenseDownloadResult downloadLicense( String licenseUrlString, String loginPassword, File outputFile,
                                                  Log log )
        throws IOException, URISyntaxException
    {
        if ( licenseUrlString == null || licenseUrlString.length() == 0 )
        {
            return LicenseDownloadResult.success( outputFile );
        }

        if ( licenseUrlString.startsWith( "file://" ) )
        {
            Files.copy( Paths.get( new URI( licenseUrlString ) ), outputFile.toPath() );
            return LicenseDownloadResult.success( outputFile );
        }
        else
        {
            try ( CloseableHttpResponse response = client.execute( new HttpGet( licenseUrlString ) ) )
            {
                final StatusLine statusLine = response.getStatusLine();
                if ( statusLine.getStatusCode() != HttpStatus.SC_OK )
                {
                    return LicenseDownloadResult.failure( "'" + licenseUrlString + "' returned "
                        + statusLine.getStatusCode()
                        + ( statusLine.getReasonPhrase() != null ? " " + statusLine.getReasonPhrase() : "" ) );
                }

                final HttpEntity entity = response.getEntity();
                if ( entity != null )
                {
                    final ContentType contentType = ContentType.get( entity );
                    File updatedFile =
                        updateFileExtension( outputFile, contentType != null ? contentType.getMimeType() : null );

                    try ( InputStream in = entity.getContent();
                                    FileOutputStream fos = new FileOutputStream( updatedFile ) )
                    {
                        copyStream( in, fos );
                    }
                    return LicenseDownloadResult.success( updatedFile );

                }
                else
                {
                    return LicenseDownloadResult.failure( "'" + licenseUrlString + "' returned no body." );
                }
            }
        }
    }

    /**
     * Copy data from one stream to another.
     *
     * @param inStream
     * @param outStream
     * @throws IOException
     */
    private static void copyStream( InputStream inStream, OutputStream outStream )
        throws IOException
    {
        byte[] buf = new byte[1024];
        int len;
        while ( ( len = inStream.read( buf ) ) > 0 )
        {
            outStream.write( buf, 0, len );
        }
    }

    private static File updateFileExtension( File outputFile, String mimeType )
    {
        final String realExtension = getFileExtension( mimeType );

        if ( realExtension != null )
        {
            if ( !outputFile.getName().endsWith( realExtension ) )
            {
                return new File( outputFile.getAbsolutePath() + realExtension );
            }
        }
        return outputFile;
    }

    private static String getFileExtension( String mimeType )
    {
        if ( mimeType == null )
        {
            return null;
        }

        final String lowerMimeType = mimeType.toLowerCase();
        if ( lowerMimeType.contains( "plain" ) )
        {
            return ".txt";
        }

        if ( lowerMimeType.contains( "html" ) )
        {
            return ".html";
        }

        if ( lowerMimeType.contains( "pdf" ) )
        {
            return ".pdf";
        }

        return null;
    }

    @Override
    public void close()
        throws Exception
    {
        client.close();
    }

    /**
     * A result of a license download operation.
     *
     * @since 1.18
     */
    public static class LicenseDownloadResult
    {
        public static LicenseDownloadResult success( File file )
        {
            return new LicenseDownloadResult( file, null );
        }

        public static LicenseDownloadResult failure( String errorMessage )
        {
            return new LicenseDownloadResult( null, errorMessage );
        }

        private LicenseDownloadResult( File file, String errorMessage )
        {
            super();
            this.file = file;
            this.errorMessage = errorMessage;
        }

        private final File file;

        private final String errorMessage;

        public File getFile()
        {
            return file;
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public boolean isSuccess()
        {
            return errorMessage == null;
        }
    }

}
