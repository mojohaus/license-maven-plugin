package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 - Falco Nikolas
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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * This facade should be used to retrieve the content from a resource behind an URL.
 *
 * @author Nikolas Falco
 */
public class RequesterFacade
{

    /**
     * Build a new {@link URLRequester} for the given url.
     *
     * @param url for which create the requester
     * @return an {@link URLRequester} that is able to handle the given URL.
     */
    public static URLRequester newRequesterFor( String url )
    {
        if ( url == null )
        {
            return null;
        }

        String urlLC = url.toLowerCase();

        if ( HttpRequester.handle( urlLC ) )
        {
            return new HttpRequester();
        }
        else if ( ClasspathRequester.handle( urlLC ) )
        {
            return new ClasspathRequester( RequesterFacade.class.getClassLoader() );
        }
        else
        {
            return new GenericRequester();
        }
    }

    /**
     * Checks if the given input is a URL value.
     *
     * @param data the URL to test
     * @return true if URL, false else
     */
    public static boolean isStringUrl( String data )
    {
        if ( StringUtils.isBlank( data ) )
        {
            return false;
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
     * Will download a external resource and read the content of the file that will then be translated into a
     * new list.
     * <p>
     * Lines starting with the character '#' will be omitted from the list
     * <p>
     * <b>NOTE:</b>
     * Certificate checking for this request will be disabled because some resources might be present on some
     * local servers in the internal network that do not use a safe connection.
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
            String content = newRequesterFor( url ).getFromUrl( url );
            bufferedReader = new BufferedReader( new CharArrayReader( content.toCharArray() ) );
            String line;
            while ( ( line = bufferedReader.readLine() ) != null )
            {
                if ( StringUtils.isNotBlank( line ) )
                {
                    if ( !StringUtils.startsWith( line, "#" ) && !list.contains( line ) )
                    {
                        list.add( line );
                    }
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "could not open connection to URL: " + url, e );
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

    /**
     * This method retrieve the content of the URL as a string.
     *
     * @param url
     *            the resource destination that is expected to contain pure text
     * @return the string representation of the resource at the given URL
     */
    public static String getFromUrl( String url ) throws MojoExecutionException
    {
        return newRequesterFor( url ).getFromUrl( url );
    }

}
