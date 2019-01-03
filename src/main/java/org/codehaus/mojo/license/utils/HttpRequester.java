package org.codehaus.mojo.license.utils;

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
public class HttpRequester
{

  /**
   * checks if the input in the {@link org.codehaus.mojo.license.AbstractAddThirdPartyMojo#includedLicenses}
   * is a URL value
   *
   * @param data the license string or a URL
   * @return true if URL, false else
   */
  public static boolean isStringUrl(String data)
  {
    if (StringUtils.isBlank(data))
    {
      return false;
    }
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
   * this method will send a simple GET-request to the given destination and will return the result as a
   * string
   *
   * @param url the resource destination that is expected to contain pure text
   * @return the string representation of the resource at the given URL
   */
  public static String getFromUrl(String url) throws MojoExecutionException
  {
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    HttpGet get = new HttpGet(url);
    CloseableHttpResponse response = null;

    String result = null;
    try
    {
      response = httpClient.execute(get);
      result = IOUtils.toString(response.getEntity().getContent(), Charset.forName("UTF-8"));
    }
    catch (ClientProtocolException e)
    {
      throw new MojoExecutionException(e.getMessage(), e);
    }
    catch (IOException e)
    {
      throw new MojoExecutionException(e.getMessage(), e);
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
  public static List<String> downloadList(String url) throws MojoExecutionException
  {
    List<String> list = new ArrayList<>();
    BufferedReader bufferedReader = null;
    try
    {
      bufferedReader = new BufferedReader(new CharArrayReader(getFromUrl(url).toCharArray()));
      String line;
      while ((line = bufferedReader.readLine()) != null)
      {
        if (StringUtils.isNotBlank(line))
        {
          if (!StringUtils.startsWith(line, "#") && !list.contains(line))
          {
            list.add(line);
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
    return list;
  }
}
