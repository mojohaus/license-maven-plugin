package org.codehaus.mojo.license.utils;

import java.io.File;
import java.net.URL;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RequesterFacadeTest {

    private static final String RESOURCE_NAME = "org/codehaus/mojo/license/utils/licenses.properties";

    @Rule
    public TemporaryFolder fileRule = new TemporaryFolder();

    @Test
    public void test_classpath_requester() throws Exception
    {
        String licenseContent = RequesterFacade.getFromUrl( "classpath://" + RESOURCE_NAME );
        Assert.assertEquals( "license1=This is mine!", licenseContent.trim() );
    }


    @Test
    public void test_generic_requester() throws Exception
    {
        URL res = getClass().getClassLoader().getResource( RESOURCE_NAME );
        File testFile = fileRule.newFile();
        FileUtils.copyURLToFile( res, testFile );

        String licenseContent = RequesterFacade.getFromUrl( testFile.toURI().toURL().toExternalForm() );
        Assert.assertEquals( "license1=This is mine!", licenseContent.trim() );
    }

}