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
package org.codehaus.mojo.license;

import java.io.*;
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

    public static void downloadLicense( String licenseUrlString, File outputFile )
        throws IOException
    {
        InputStream licenseInputStream = null;
        FileOutputStream fos = null;

        try
        {
            URL licenseUrl = new URL( licenseUrlString );
            URLConnection connection = licenseUrl.openConnection();
            connection.setConnectTimeout( DEFAULT_CONNECTION_TIMEOUT );
            connection.setReadTimeout( DEFAULT_CONNECTION_TIMEOUT );
            licenseInputStream = connection.getInputStream();
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

}
