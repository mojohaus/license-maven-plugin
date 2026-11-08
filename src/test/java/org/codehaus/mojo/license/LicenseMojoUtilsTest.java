package org.codehaus.mojo.license;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class LicenseMojoUtilsTest {
    private String resolvedUrl;
    private File overrideFile;
    private String url;
    private File basedir = new File("");
    private MockLogger log = new MockLogger();

    @Test
    void isValidNull() {
        assertFalse(LicenseMojoUtils.isValid(null));
    }

    @Test
    void isValidEmpty() {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue(LicenseMojoUtils.isValid(""));
    }

    @Test
    void isValidBlank() {
        // This might be wrong; feel free to change the test when it starts to fail
        assertTrue(LicenseMojoUtils.isValid("   "));
    }

    @Test
    void isValidNonexistingClasspathResource() {
        assertTrue(LicenseMojoUtils.isValid("classpath:noSuchResource"));
    }

    @Test
    void isValidClasspathResource() {
        assertTrue(LicenseMojoUtils.isValid("classpath:log4j.properties"));
    }

    @Test
    void isValidHttpResource() {
        assertTrue(LicenseMojoUtils.isValid("http://foo/bar/baz"));
    }

    @Test
    void prepareThirdPartyOverrideUrlNull() {
        String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, overrideFile, url, basedir, log);
        assertEquals(LicenseMojoUtils.NO_URL, actual);
    }

    @Test
    void prepareThirdPartyOverrideUrlBothOverrides() {
        overrideFile = new File("src/test/resources/overrides.properties");
        url = "classpath:overrides.properties";
        try {
            LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, overrideFile, url, basedir, log);

            fail("Missing exception");
        } catch (IllegalArgumentException e) {
            assertEquals("You can't use both overrideFile and overrideUrl", e.getMessage());
        }
    }

    @Test
    void prepareThirdPartyOverrideUrlNonExistingOverrideFile() {
        overrideFile = new File("foo");
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:///inexistent\n"
                        + "valid=false\n"
                        + "WARN overrideFile [.../foo] was configured but doesn't exist\n"
                        + "DEBUG No (valid) URL and no file [.../override-THIRD-PARTY.properties] found; not loading any overrides\n",
                actual);
    }

    @Test
    void prepareThirdPartyOverrideUrlOverrideFile() {
        overrideFile = new File("src/test/resources/overrides.properties");
        String actual = runTestAndJoinResults();
        assertEquals(
                "resolved=file:/.../overrides.properties\n"
                        + "valid=true\n"
                        + "DEBUG Loading overrides from file file:/.../overrides.properties\n",
                actual);
    }

    @Test
    void prepareThirdPartyOverrideClasspathResource() {
        url = "classpath:overrides.properties";
        String actual = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, overrideFile, url, basedir, log);
        assertEquals(url, actual);
        assertTrue(LicenseMojoUtils.isValid(actual));
        assertEquals("DEBUG Loading overrides from URL classpath:overrides.properties\n", log.dump());
    }

    @Test
    void prepareThirdPartyOverrideInvalidUrl() {
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
    void prepareThirdPartyOverridePreventReinit() {
        resolvedUrl = "classpath:overrides.properties";
        overrideFile = new File("foo");
        url = "classpath:bar";
        String actual = runTestAndJoinResults();
        assertEquals("resolved=classpath:overrides.properties\n" + "valid=true\n", actual);
    }

    /** Allow to validate several test results in one assert */
    private String runTestAndJoinResults() {
        String result = LicenseMojoUtils.prepareThirdPartyOverrideUrl(resolvedUrl, overrideFile, url, basedir, log);
        File defaultOverride = new File(LicenseMojoUtils.DEFAULT_OVERRIDE_THIRD_PARTY);
        String dump = log.dump().replace(defaultOverride.getAbsolutePath(), ".../" + defaultOverride.getName());

        if (overrideFile != null) {
            dump = dump.replace(overrideFile.toURI().toString(), "file:/.../" + overrideFile.getName())
                    .replace(overrideFile.getAbsolutePath(), ".../" + overrideFile.getName());
            result = result.replace(overrideFile.toURI().toString(), "file:/.../" + overrideFile.getName());
        }

        String actual = "resolved=" + result + "\n" + "valid=" + LicenseMojoUtils.isValid(result) + "\n" + dump;
        return actual;
    }
}
