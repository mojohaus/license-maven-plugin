package org.codehaus.mojo.license;

import static org.junit.Assert.*;

import java.io.File;

import org.codehaus.mojo.license.logback.RedirectLogger;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class LicenseMojoUtilsTest
{
    private String resolvedUrl;
    private File deprecatedFile;
    private String url;
    private File basedir = new File( "" );

    @Test
    public void testIsValidNull()
    {
        assertFalse( LicenseMojoUtils.isValid( null ) );
    }

    @Test
    public void testIsValidEmpty()
    {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue( LicenseMojoUtils.isValid( "" ) );
    }

    @Test
    public void testIsValidBlank()
    {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue( LicenseMojoUtils.isValid( "   " ) );
    }

    @Test
    public void testIsValidNonexistingClasspathResource()
    {
        assertTrue( LicenseMojoUtils.isValid( "classpath:noSuchResource" ) );
    }

    @Test
    public void testIsValidClasspathResource()
    {
        assertTrue( LicenseMojoUtils.isValid( "classpath:log4j.properties" ) );
    }

    @Test
    public void testIsValidHttpResource()
    {
        assertTrue( LicenseMojoUtils.isValid( "http://foo/bar/baz" ) );
    }

    @Test
    public void testPrepareThirdPartyOverrideUrlNull()
    {
        String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl( resolvedUrl, deprecatedFile, url, basedir );
        assertEquals( LicenseMojoUtils.NO_URL, actual );
    }

    @Test
    public void testPrepareThirdPartyOverrideUrlBothOverrides()
    {
        deprecatedFile = new File( "src/test/resources/overrides.properties" );
        url = "classpath:overrides.properties";
        try
        {
            LicenseMojoUtils.prepareThirdPartyOverrideUrl( resolvedUrl, deprecatedFile, url, basedir );

            fail( "Missing exception" );
        }
        catch( IllegalArgumentException e )
        {
            assertEquals( "You can't use both overrideFile and overrideUrl", e.getMessage() );
        }
    }

    @Test
    public void testPrepareThirdPartyOverrideUrlNonExistingDeprecatedFile()
    {
        deprecatedFile = new File( "foo" );
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:///inexistent\n"
                + "valid=false\n"
                + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n"
                + "WARN overrideFile [.../foo] was configured but doesn't exist\n"
                + "DEBUG No (valid) URL and no file [.../override-THIRD-PARTY.properties] found; not loading any overrides\n"
                , actual );
    }

    @Test
    public void testPrepareThirdPartyOverrideUrlDeprecatedFile()
    {
        deprecatedFile = new File( "src/test/resources/overrides.properties" );
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:/.../overrides.properties\n"
                + "valid=true\n"
                + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n"
                + "DEBUG Loading overrides from file file:/.../overrides.properties\n"
                , actual );
    }

    @Test
    public void testPrepareThirdPartyOverrideClasspathResource()
    {
        RedirectLogger rl = new RedirectLogger( LicenseMojoUtils.class );
        try
        {
            rl.install();
            
            url = "classpath:overrides.properties";
            String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl( resolvedUrl, deprecatedFile, url, basedir );
            assertEquals( url, actual );
            assertTrue( LicenseMojoUtils.isValid(actual) );
        }
        finally
        {
            rl.deinstall();
        }
        
        assertEquals(
                "DEBUG Loading overrides from URL classpath:overrides.properties\n",
                rl.dump( Level.DEBUG ) );
    }

    @Test
    public void testPrepareThirdPartyOverrideInvalidUrl()
    {
        url = "foo://localhost/bar";
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:///inexistent\n"
                + "valid=false\n"
                + "WARN Unsupported or invalid URL [foo://localhost/bar] found in overrideUrl; supported are 'classpath:' URLs and  anything your JVM supports (file:, http: and https: should always work)\n"
                + "DEBUG No (valid) URL and no file [.../override-THIRD-PARTY.properties] found; not loading any overrides\n"
                , actual );
    }

    @Test
    public void testPrepareThirdPartyOverridePreventReinit()
    {
        resolvedUrl = "classpath:overrides.properties";
        deprecatedFile = new File( "foo" );
        url = "classpath:bar";
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=classpath:overrides.properties\n"
                + "valid=true\n"
                + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n"
                , actual );
    }

    /** Allow to validate several test results in one assert */
    private String runTestAndJoinResults()
    {
        RedirectLogger rl = new RedirectLogger( LicenseMojoUtils.class );
        
        String result;
        try
        {
            rl.install();
            
            result = LicenseMojoUtils.prepareThirdPartyOverrideUrl( resolvedUrl, deprecatedFile, url, basedir );
        }
        finally
        {
            rl.deinstall();
        }
        File defaultOverride = new File ( LicenseMojoUtils.DEFAULT_OVERRIDE_THIRD_PARTY );
        String dump = rl.dump( Level.DEBUG )
                .replace( defaultOverride.getAbsolutePath(), ".../" + defaultOverride.getName() );

        if ( deprecatedFile != null )
        {
            dump = dump
                    .replace( deprecatedFile.toURI().toString(), "file:/.../" + deprecatedFile.getName() )
                    .replace( deprecatedFile.getAbsolutePath(), ".../" + deprecatedFile.getName() );
            result = result
                    .replace( deprecatedFile.toURI().toString(), "file:/.../" + deprecatedFile.getName() );
        }

        String actual = "resolved=" + result + "\n"
                + "valid=" + LicenseMojoUtils.isValid( result ) + "\n"
                + dump;
        return actual;
    }
}
