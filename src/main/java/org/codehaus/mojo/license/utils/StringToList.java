package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2013 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


/**
 * Object to convert in mojo a parameter from a some simple String to a List.
 *
 * See (http://jira.codehaus.org/browse/MLICENSE-53).
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.4
 */
public class StringToList
{

    /**
     * List of data.
     */
    private final List<String> data;

    public StringToList()
    {
        data = new ArrayList<String>();
    }

   /**
    * @param data a list of licenses or a URL
    */
    public StringToList(String data) throws MojoExecutionException
    {
      this();
      if (!isStringUrl(data))
      {
        for ( String s : data.split("\\s*\\|\\s*") )
        {
          addEntryToList(s);
        }
      }
      else
      {
        for ( String license : downloadLicenseList(data) )
        {
          if (data != null && StringUtils.isNotBlank(license) && !data.contains(license))
          {
            this.data.add(license);
          }
        }
      }
    }

    public List<String> getData()
    {
        return data;
    }

    protected void addEntryToList( String data )
    {
        this.data.add( data );
    }

    /**
     * checks if the input in the {@link org.codehaus.mojo.license.AbstractAddThirdPartyMojo#includedLicenses}
     * is a URL value
     *
     * @param data the license string or a URL
     * @return true if URL, false else
     */
    private boolean isStringUrl(String data)
    {
      try
      {
        new URL(data);
        return true;
      }
      catch (MalformedURLException e)
      {
        return false;
      }
    }

    /**
     * will download a external resource and read the content of the file that will then be translated into a
     * new list. <br>
     * <br>
     * <b>NOTE:</b><br>
     * certificate checking for this request will be disabled because some resources might be present on some
     * local servers in the internal network that do not use a safe connection
     *
     * @param url the URL to the external resource
     * @return a new list with all license entries from the remote resource
     */
    private List<String> downloadLicenseList(String url) throws MojoExecutionException
    {
      List<String> licenses = new ArrayList<String>();
      CloseableHttpClient httpClient = HttpClientBuilder.create().build();
      HttpGet get = new HttpGet(url);
      CloseableHttpResponse response = null;
      BufferedReader bufferedReader = null;
      try
      {
        response = httpClient.execute(get);
        bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                                                                  Charset.forName("UTF-8")));
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
          if (StringUtils.isNotBlank(line))
          {
            if (!licenses.contains(line))
            {
              licenses.add(line);
            }
          }
        }
      }
      catch (IOException e)
      {
        throw new MojoExecutionException("could not open connection to URL: " + url, e);
      }
      finally
      {
        if (response != null)
        {
          try
          {
            response.close();
          }
          catch (IOException e)
          {
            throw new MojoExecutionException(e.getMessage(), e);
          }
        }
        if (bufferedReader != null)
        {
          try
          {
            bufferedReader.close();
          }
          catch (IOException e)
          {
            throw new MojoExecutionException(e.getMessage(), e);
          }
        }
      }
      return licenses;
    }

}
