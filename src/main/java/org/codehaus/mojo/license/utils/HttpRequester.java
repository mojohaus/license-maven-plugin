package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Captain-P-Goldfish
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
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;


/**
 * project: license-maven-plugin <br>
 * author: Pascal Knueppel <br>
 * created at: 25.01.2018 - 09:27 <br>
 * <br>
 * this class should be used to send HTTP requests to some destinations and return the content from these
 * resources.
 */
public class HttpRequester implements URLRequester
{

    /**
     * this method will send a simple GET-request to the given destination and will return the result as a
     * string
     *
     * @param url the resource destination that is expected to contain pure text
     * @return the string representation of the resource at the given URL
     */
    public String getFromUrl( String url ) throws MojoExecutionException
    {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet( url );
        CloseableHttpResponse response = null;

        String result = null;
        try
        {
            response = httpClient.execute( get );
            result = IOUtils.toString( response.getEntity().getContent(), Charset.forName( "UTF-8" ) );
        }
        catch ( ClientProtocolException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            if ( response != null )
            {
                try
                {
                    response.close();
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( e.getMessage(), e );
                }
            }
        }
        return result;
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
        return url.startsWith( "http://" ) || url.startsWith( "https://" );
    }

}
