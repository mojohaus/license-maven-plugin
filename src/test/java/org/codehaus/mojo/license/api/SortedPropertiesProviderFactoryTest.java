package org.codehaus.mojo.license.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.codehaus.mojo.license.LicenseMojoUtilsTest;
import org.codehaus.mojo.license.api.impl.SortedPropertiesFileProviderTest;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.junit.Test;

public class SortedPropertiesProviderFactoryTest {

    private String encoding = "UTF-8";

    @Test
    public void testNull() throws Exception
    {
        SortedProperties actual = new SortedPropertiesProviderFactory( encoding )
                .build( null, null )
                .get();

        assertEquals( "{}", actual.toString() );
    }

    @Test
    public void testNullModify() throws Exception
    {
        SortedPropertiesProvider provider = new SortedPropertiesProviderFactory( encoding )
                .build( null, null );
        SortedProperties actual = provider.get();

        assertEquals( "{}", actual.toString() );

        actual.setProperty( "foo", "bar" );
        assertEquals( "{foo=bar}", actual.toString() );

        SortedProperties another = provider.get();
        assertEquals( "{}", another.toString() );
    }

    @Test
    public void testFile() throws Exception
    {
        File file = LicenseMojoUtilsTest.OVERRIDES_PROPERTIES;
        SortedProperties actual = new SortedPropertiesProviderFactory( encoding )
            .build( file, null )
            .get();

        assertEquals( SortedPropertiesFileProviderTest.EXPECTED_OVERRIDES, actual.toString() );

        // TODO Port RedirectLogger
//        String log = logger.dump()
//                .replace( file.getAbsolutePath(), "/.../" + file.getName() );
//        assertEquals( "INFO Load missing file /.../overrides.properties", log );
    }

    @Test
    public void testNoSuchFile() throws Exception
    {
        File file = new File( "foo" );
        SortedProperties actual = new SortedPropertiesProviderFactory( encoding )
                .build( file, null )
                .get();

        assertEquals( "{}", actual.toString() );

        // TODO Port RedirectLogger
//        String log = logger.dump()
//                .replace( file.getAbsolutePath(), "/.../" + file.getName() );
//        assertEquals( "DEBUG No such file: /.../foo", log );
    }

    @Test
    public void testUrl() throws Exception
    {
        String url = "classpath:overrides.properties";
        SortedProperties actual = new SortedPropertiesProviderFactory( encoding )
            .build( null, url)
            .get();

        assertEquals( SortedPropertiesFileProviderTest.EXPECTED_OVERRIDES, actual.toString() );

        // TODO Port RedirectLogger
//        String log = logger.dump();
//        assertEquals( "INFO Load missing licenses from URL classpath:overrides.properties", log );
    }

    @Test
    public void testNoSuchUrl() throws Exception
    {
        String url = "classpath:doesntexist.properties";
        SortedPropertiesProvider provider = new SortedPropertiesProviderFactory( encoding )
                .build( null, url);

        try
        {
            provider.get();

            fail( "Missing exception" );
        }
        catch( IOException e )
        {
            assertEquals( "The resource doesntexist.properties was not found in the maven plugin classpath",
                    e.getMessage() );
        }
    }

    /**
     * Test which one is preferred: File or URL?
     *
     * @throws Exception
     */
    @Test
    public void testFileAndUrl() throws Exception
    {
        File file = LicenseMojoUtilsTest.OVERRIDES_PROPERTIES;
        String url = "classpath:overrides.properties";
        SortedProperties actual = new SortedPropertiesProviderFactory( encoding )
                .build( file, url )
                .get();

        assertEquals( SortedPropertiesFileProviderTest.EXPECTED_OVERRIDES, actual.toString() );

        // TODO Port RedirectLogger
//        String log = logger.dump()
//                .replace( file.getAbsolutePath(), "/.../" + file.getName() );
//        assertEquals( "INFO Load missing file /.../overrides.properties", log );
    }

}
