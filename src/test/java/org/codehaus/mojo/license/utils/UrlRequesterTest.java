package org.codehaus.mojo.license.utils;

import java.io.File;
import java.net.URL;

import org.codehaus.mojo.license.utils.UrlRequester;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UrlRequesterTest
{

    private static final String RESOURCE_NAME = "org/codehaus/mojo/license/utils/licenses.properties";

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();

    @Test
    public void testClasspathRequester() throws Exception
    {
        String licenseContent = UrlRequester.getFromUrl( "classpath:" + RESOURCE_NAME );
        Assert.assertEquals( "license1=This is mine!", licenseContent.trim() );
    }


    @Test
    public void testGenericRequester() throws Exception
    {
        URL res = getClass().getClassLoader().getResource( RESOURCE_NAME );
        File testFile = fileRule.newFile();
        FileUtils.copyURLToFile( res, testFile );

        String licenseContent = UrlRequester.getFromUrl( testFile.toURI().toURL().toString() );
        Assert.assertEquals( "license1=This is mine!", licenseContent.trim() );
    }

    @Test
    public void testClasspathIsAValidUrl()
    {
        Assert.assertTrue( "classpath protocol not registered", UrlRequester.isStringUrl( "classpath:" + RESOURCE_NAME ) );
    }

}