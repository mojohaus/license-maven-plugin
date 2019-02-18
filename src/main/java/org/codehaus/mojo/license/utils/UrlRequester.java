package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Captain-P-Goldfish, Falco Nikolas
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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * This class should be used to load the content from a URL.
 * <p>
 * Supported URL protocols are those standards plus classpath protocol.
 */
public class UrlRequester
{

    public static final String CLASSPATH_PROTOCOL = "classpath";

    /**
     * Checks if the given input is a URL value.
     *
     * @param data the license string or a URL
     * @return true if URL, false else
     */
    public static boolean isStringUrl( String data )
    {
        if ( StringUtils.isBlank( data ) )
        {
            return false;
        }
        if ( data.startsWith( CLASSPATH_PROTOCOL + ':' ) )
        {
            return true;
        }
        try
        {
            new URL( data );
            return true;
        }
        catch ( MalformedURLException e )
        {
            return false;
        }
    }

    /**
     * Returns the content of the resource pointed by the given URL as a string.
     *
     * @param url the resource destination that is expected to contain pure text
     * @return the string representation of the resource at the given URL
     * @throws IOException If an I/O error occurs when retrieve of the content URL
     */
    public static String getFromUrl( String url ) throws IOException
    {
        return getFromUrl( url, "UTF-8" );
    }

    /**
     * Returns the content of the resource pointed by the given URL as a string.
     *
     * @param url the resource destination that is expected to contain pure text
     * @param encoding the resource content encoding
     * @return the string representation of the resource at the given URL
     * @throws IOException If an I/O error occurs when retrieve of the content URL
     */
    public static String getFromUrl( String url, String encoding ) throws IOException
    {
        // by RFC url is composed by <protocol>:<schema-part>.
        // Here could not be used the URL parser because classpath does not have a registered Handler
        String protocol = StringUtils.substringBefore( url, ":" ).toLowerCase();
        Charset charset = Charset.forName( encoding );

        String result = null;
        if ( CLASSPATH_PROTOCOL.equals( protocol ) )
        {
            ClassLoader classLoader = UrlRequester.class.getClassLoader();

            String resource = url.substring( CLASSPATH_PROTOCOL.length() + 1 );
            URL resourceUrl = classLoader.getResource( resource );
            if ( resourceUrl != null )
            {
                result = IOUtils.toString( resourceUrl, charset );
            }
            else
            {
                throw new IOException( "The resource " + resource
                        + " was not found in the maven plugin classpath" );
            }
        }
        else if ( "http".equals( protocol ) || "https".equals( protocol ) )
        {
            try ( CloseableHttpClient httpClient = HttpClientBuilder.create().build() )
            {
                HttpGet get = new HttpGet( url );
                try ( CloseableHttpResponse response = httpClient.execute( get ) )
                {
                    int responseCode = response.getStatusLine().getStatusCode();
                    // CHECKSTYLE_OFF: MagicNumber
                    if ( responseCode >= 200 && responseCode < 300 )
                    // CHECKSTYLE_ON: MagicNumber
                    {
                        // server has response and there might be a not empty payload
                        HttpEntity entity = response.getEntity();
                        ContentType contentType = ContentType.get( entity );
                        if ( contentType != null )
                        {
                            charset = contentType.getCharset();
                        }

                        result = IOUtils.toString( entity.getContent(),  charset );
                    }
                    else
                    {
                        throw new IOException( "For the URL (" + url + ") the server responded with "
                                + response.getStatusLine() );
                    }
                }
            }
        }
        else
        {
            result = IOUtils.toString( new URL( url ).openStream(), charset );
        }
        return result;
    }

    /**
     * will download a external resource and read the content of the file that will then be translated into a
     * new list. <br>
     * Lines starting with the character '#' will be omitted from the list <br>
     * <br>
     * <b>NOTE:</b><br>
     * certificate checking for this request will be disabled because some resources might be present on some
     * local servers in the internal network that do not use a safe connection
     *
     * @param url the URL to the external resource
     * @return a new list with all license entries from the remote resource
     */
    public static List<String> downloadList( String url ) throws MojoExecutionException
    {
        List<String> list = new ArrayList<>();
        BufferedReader bufferedReader = null;
        try
        {
            bufferedReader = new BufferedReader( new CharArrayReader( getFromUrl( url ).toCharArray() ) );
            String line;
            while ( ( line = bufferedReader.readLine() ) != null )
            {
                if ( StringUtils.isNotBlank( line ) && !StringUtils.startsWith( line, "#" ) && !list.contains( line ) )
                {
                    list.add( line );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not open connection to URL: " + url, e );
        }
        finally
        {
            if ( bufferedReader != null )
            {
                try
                {
                    bufferedReader.close();
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            }
        }
        return list;
    }
}
