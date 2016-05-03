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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utilities for downloading remote license files.
 *
 * @author pgier
 * @since 1.0
 */
public class LicenseDownloader
{

    /**
     * Defines the connection timeout in milliseconds when attempting to download license files.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    public static void downloadLicense( String licenseUrlString, String loginPassword, File outputFile )
        throws IOException
    {
    	if ( licenseUrlString == null || licenseUrlString.length() == 0 )
    		return;
    	
        InputStream licenseInputStream = null;
        FileOutputStream fos = null;

        try
        {
            URL licenseUrl = new URL( licenseUrlString );
            URLConnection connection = licenseUrl.openConnection();

            if ( loginPassword != null )
            {
                connection.setRequestProperty( "Proxy-Authorization", loginPassword );
            }
            connection.setConnectTimeout( DEFAULT_CONNECTION_TIMEOUT );
            connection.setReadTimeout( DEFAULT_CONNECTION_TIMEOUT );
            
            boolean redirect = false;
            if ( connection instanceof HttpURLConnection ) 
            {
            	int status = ((HttpURLConnection)connection).getResponseCode();

            	if ( status != HttpURLConnection.HTTP_OK ) 
            	{
            		if ( status == HttpURLConnection.HTTP_MOVED_TEMP
            				|| status == HttpURLConnection.HTTP_MOVED_PERM
            				|| status == HttpURLConnection.HTTP_SEE_OTHER )
            		{
            			redirect = true;
            		}
            	}
            }
            
            if ( redirect ) {

        		// get redirect url from "location" header field
        		String newUrl = connection.getHeaderField( "Location" );
        		
        		// open the new connnection again
        		connection = new URL( newUrl ).openConnection();
        		if ( loginPassword != null )
                {
                    connection.setRequestProperty( "Proxy-Authorization", loginPassword );
                }
                connection.setConnectTimeout( DEFAULT_CONNECTION_TIMEOUT );
                connection.setReadTimeout( DEFAULT_CONNECTION_TIMEOUT );
        	}

            licenseInputStream = connection.getInputStream();
            fos = new FileOutputStream( updateFileExtension( outputFile, connection.getContentType() ) );
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
    
    private static File updateFileExtension ( final File outputFile, final String mimeType )
    {
    	final String realExtension = getFileExtension(mimeType);
    	
    	if ( realExtension != null )
    	{
    		if ( !outputFile.getName().endsWith(realExtension) ) 
    		{
    			return new File( outputFile.getAbsolutePath() + realExtension );
    		}
    	}
    	return outputFile;
    }
    
    private static String getFileExtension( final String mimeType ) 
    {
    	if ( mimeType == null )
    		return null;
    	
    	final String lowerMimeType = mimeType.toLowerCase();
    	if ( lowerMimeType.contains( "plain" ) )
    		return ".txt";
    	
    	if ( lowerMimeType.contains( "html" ) )
    		return ".html";
    	
    	if ( lowerMimeType.contains( "pdf" ) )
    		return ".pdf";
    	
    	return null;
    }

}
