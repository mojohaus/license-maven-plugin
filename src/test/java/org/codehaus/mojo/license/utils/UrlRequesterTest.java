package org.codehaus.mojo.license.utils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlRequesterTest {

    private static final String RESOURCE_NAME = "org/codehaus/mojo/license/utils/licenses.properties";

    @Test
    void classpathRequester() throws Exception {
        String licenseContent = UrlRequester.getFromUrl("classpath:" + RESOURCE_NAME);
        assertEquals("license1=This is mine!", licenseContent.trim());
    }

    @Test
    void genericRequester(@TempDir Path tempDir) throws Exception {
        URL res = getClass().getClassLoader().getResource(RESOURCE_NAME);
        File testFile = Files.createTempFile(tempDir, "requester", "test").toFile();
        FileUtils.copyURLToFile(res, testFile);

        String licenseContent = UrlRequester.getFromUrl(testFile.toURI().toURL().toString());
        assertEquals("license1=This is mine!", licenseContent.trim());
    }

    @Test
    void classpathIsAValidUrl() {
        assertTrue(UrlRequester.isStringUrl("classpath:" + RESOURCE_NAME), "classpath protocol not registered");
    }

    @Test
    void classpathIsAExternalUrl() {
        assertTrue(UrlRequester.isExternalUrl("classpath:" + RESOURCE_NAME), "classpath protocol as external");
    }

    @Test
    void fileUrlWithWindowsPathIsAValidUrl() throws Exception {
        String url = "file:///C:\\Repositories\\build\\target/artifact-filters.txt";
        assertTrue(UrlRequester.isStringUrl(url));
        assertEquals(
                "file:/C:/Repositories/build/target/artifact-filters.txt",
                UrlRequester.toUrl(url).toString());
    }

    @Test
    void fileUrlWithSpaceIsQuoted() throws Exception {
        assertEquals(
                "file:/tmp/my%20dir/file.txt",
                UrlRequester.toUrl("file:///tmp/my dir/file.txt").toString());
    }

    @Test
    void fileUrlWithBackslashesIsRead(@TempDir Path tempDir) throws Exception {
        URL res = getClass().getClassLoader().getResource(RESOURCE_NAME);
        File testFile = tempDir.resolve("my dir").resolve("licenses.properties").toFile();
        FileUtils.copyURLToFile(res, testFile);

        String url = "file:///" + testFile.getAbsolutePath().replace('/', '\\');
        assertEquals("license1=This is mine!", UrlRequester.getFromUrl(url).trim());
    }

    @Test
    void otherInvalidUrlIsNotAUrl() {
        assertFalse(UrlRequester.isStringUrl("http://host/a\\b"));
        assertThrows(IllegalArgumentException.class, () -> UrlRequester.toUrl("http://host/a\\b"));
    }
}
