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

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * Utilities for downloading remote license files.
 *
 * @author pgier
 * @since 1.0
 */
public class LicenseDownloader
{
    private String proxyUrl;
    /**
     * Defines the connection timeout in milliseconds when attempting to download license files.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public LicenseDownloader(String proxyAddr) {
            this.proxyUrl = proxyAddr;
    }

    public void downloadFromGitLab(String licenseUrlString, File outputFile) {
        try {
            System.out.println("Downloading " + licenseUrlString);
            Response response = Request.Get(licenseUrlString)
                    .execute();
            response.saveContent(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadLicense( String licenseUrlString, String loginPassword, File outputFile )
        throws IOException
    {
//        download2(licenseUrlString, outputFile);

        if ( licenseUrlString == null || licenseUrlString.length() == 0 )
        {
            return;
        }

        final InputStream licenseInputStream;
        if (!licenseUrlString.startsWith("http") ) {
            licenseInputStream = new ByteArrayInputStream(LicenseRegistryClient.getInstance().getFileContent(licenseUrlString).getBytes(StandardCharsets.UTF_8));
        } else {
            URLConnection connection = newConnection( licenseUrlString, loginPassword );

            boolean redirect = false;
            if ( connection instanceof HttpURLConnection )
            {
                int status = ( (HttpURLConnection) connection ).getResponseCode();

                redirect = HttpURLConnection.HTTP_MOVED_TEMP == status || HttpURLConnection.HTTP_MOVED_PERM == status ||
                        HttpURLConnection.HTTP_SEE_OTHER == status;
            }

            if ( redirect )
            {
                // get redirect url from "location" header field
                String newUrl = connection.getHeaderField( "Location" );

                // open the new connnection again
                connection = newConnection( newUrl, loginPassword );

            }

            licenseInputStream = connection.getInputStream();
            outputFile = updateFileExtension( outputFile, connection.getContentType() );

        }

        FileOutputStream fos = null;

        try
        {
            fos = new FileOutputStream( outputFile );
            copyStream( licenseInputStream, fos );
            licenseInputStream.close();
            fos.close();
        }
        finally
        {
            FileUtil.tryClose( licenseInputStream );
            FileUtil.tryClose( fos );
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

    private URLConnection newConnection( String url, String loginPassword )
        throws IOException
    {

        URL licenseUrl = new URL( url );
        final Proxy proxy;
        if (proxyUrl != null) {
            URL purl = new URL(proxyUrl);
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(purl.getHost(), purl.getPort()));
        } else {
            proxy = Proxy.NO_PROXY;
        }
        URLConnection connection = licenseUrl.openConnection(proxy);

        if ( loginPassword != null )
        {
            connection.setRequestProperty( "Proxy-Authorization", loginPassword );
        }
        connection.setConnectTimeout( DEFAULT_CONNECTION_TIMEOUT );
        connection.setReadTimeout( DEFAULT_CONNECTION_TIMEOUT );

        return connection;

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

}
