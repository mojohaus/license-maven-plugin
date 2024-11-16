package org.codehaus.mojo.license;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LicenseMojoUtilsTest {
    private String resolvedUrl;
    private File deprecatedFile;
    private String url;
    private File basedir = new File("");
    private MockLogger log = new MockLogger();

    @Test
    void testIsValidNull() {
        assertFalse(LicenseMojoUtils.isValid(null));
    }

    @Test
    void testIsValidEmpty() {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue(LicenseMojoUtils.isValid(""));
    }

    @Test
    void testIsValidBlank() {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue(LicenseMojoUtils.isValid("   "));
    }

    @Test
    void testIsValidNonexistingClasspathResource() {
        assertTrue(LicenseMojoUtils.isValid("classpath:noSuchResource"));
    }

    @Test
    void testIsValidClasspathResource() {
        assertTrue(LicenseMojoUtils.isValid("classpath:log4j.properties"));
    }

    @Test
    void testIsValidHttpResource() {
        assertTrue(LicenseMojoUtils.isValid("http://foo/bar/baz"));
    }

    @Test
    void testPrepareThirdPartyOverrideUrlNull() {
        String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, deprecatedFile, url, basedir, log);
        assertEquals(LicenseMojoUtils.NO_URL, actual);
    }

    @Test
    void testPrepareThirdPartyOverrideUrlBothOverrides() {
        deprecatedFile = new File("src/test/resources/overrides.properties");
        url = "classpath:overrides.properties";
        try {
            LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, deprecatedFile, url, basedir, log);

            fail("Missing exception");
        } catch (IllegalArgumentException e) {
            assertEquals("You can't use both overrideFile and overrideUrl", e.getMessage());
        }
    }

    @Test
    void testPrepareThirdPartyOverrideUrlNonExistingDeprecatedFile() {
        deprecatedFile = new File("foo");
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:///inexistent\n"
                        + "valid=false\n"
                        + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n"
                        + "WARN overrideFile [.../foo] was configured but doesn't exist\n"
                        + "DEBUG No (valid) URL and no file [.../override-THIRD-PARTY.properties] found; not loading any overrides\n",
                actual);
    }

    @Test
    void testPrepareThirdPartyOverrideUrlDeprecatedFile() {
        deprecatedFile = new File("src/test/resources/overrides.properties");
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:/.../overrides.properties\n"
                        + "valid=true\n"
                        + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n"
                        + "DEBUG Loading overrides from file file:/.../overrides.properties\n",
                actual);
    }

    @Test
    void testPrepareThirdPartyOverrideClasspathResource() {
        url = "classpath:overrides.properties";
        String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, deprecatedFile, url, basedir, log);
        assertEquals(url, actual);
        assertTrue(LicenseMojoUtils.isValid(actual));
        assertEquals("DEBUG Loading overrides from URL classpath:overrides.properties\n", log.dump());
    }

    @Test
    void testPrepareThirdPartyOverrideInvalidUrl() {
        url = "foo://localhost/bar";
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:///inexistent\n"
                        + "valid=false\n"
                        + "WARN Unsupported or invalid URL [foo://localhost/bar] found in overrideUrl; supported are 'classpath:' URLs and  anything your JVM supports (file:, http: and https: should always work)\n"
                        + "DEBUG No (valid) URL and no file [.../override-THIRD-PARTY.properties] found; not loading any overrides\n",
                actual);
    }

    @Test
    void testPrepareThirdPartyOverridePreventReinit() {
        resolvedUrl = "classpath:overrides.properties";
        deprecatedFile = new File("foo");
        url = "classpath:bar";
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=classpath:overrides.properties\n"
                        + "valid=true\n"
                        + "WARN 'overrideFile' mojo parameter is deprecated. Use 'overrideUrl' instead.\n",
                actual);
    }

    /** Allow to validate several test results in one assert */
    private String runTestAndJoinResults() {
        String result = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, deprecatedFile, url, basedir, log);
        File defaultOverride = new File(LicenseMojoUtils.DEFAULT_OVERRIDE_THIRD_PARTY);
        String dump = log.dump().replace(defaultOverride.getAbsolutePath(), ".../" + defaultOverride.getName());

        if (deprecatedFile != null) {
            dump = dump.replace(deprecatedFile.toURI().toString(), "file:/.../" + deprecatedFile.getName())
                    .replace(deprecatedFile.getAbsolutePath(), ".../" + deprecatedFile.getName());
            result = result.replace(deprecatedFile.toURI().toString(), "file:/.../" + deprecatedFile.getName());
        }

        String actual = "resolved=" + result + "\n" + "valid=" + LicenseMojoUtils.isValid(result) + "\n" + dump;
        return actual;
    }
}
