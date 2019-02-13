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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Requester implementation for an URL from the classpath.
 *
 * @author Nikolas Falco
 */
public class ClasspathRequester implements URLRequester
{

    private static class Handler extends URLStreamHandler
    {
        private final ClassLoader classLoader;

        public Handler( ClassLoader classLoader )
        {
            this.classLoader = classLoader;
        }

        /*
         * (non-Javadoc)
         * @see java.net.URLStreamHandler#openConnection(java.net.URL)
         */
        @Override
        protected URLConnection openConnection( URL url ) throws IOException
        {
            String path = url.toExternalForm().substring( PROTOCOL.length() );
            final URL resourceUrl = classLoader.getResource( path );
            if ( resourceUrl != null )
            {
                return resourceUrl.openConnection();
            }
            return null;
        }
    }

    public static final String PROTOCOL = "classpath://";

    private final ClassLoader classLoader;

    /**
     * Default constructor.
     *
     * @param classLoader
     *            to find resources from.
     */
    public ClasspathRequester( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    /*
     * (non-Javadoc)
     * @see org.codehaus.mojo.license.utils.URLRequester#getFromUrl(java.lang.String)
     */
    @Override
    public String getFromUrl( String url ) throws MojoExecutionException
    {
        try ( InputStream is = new URL( null, url, new Handler( classLoader ) ).openStream() )
        {
            return IOUtil.toString( is, "UTF-8" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * Returns if this {@link URLRequester} can handle the specified URL.
     *
     * @param url
     *            to handle.
     * @return {@code true} if this request is able to handle the URL,
     *         {@code false} otherwise.
     */
    public static boolean handle( String url )
    {
        return url.startsWith( PROTOCOL );
    }

}
