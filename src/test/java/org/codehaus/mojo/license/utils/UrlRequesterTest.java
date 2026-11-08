package org.codehaus.mojo.license.utils;

import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void httpRequestGoesThroughTheProxy() throws Exception {
        // a plain HTTP proxy receives the request line with the absolute URL of the target
        AtomicReference<String> requestLine = new AtomicReference<>();
        HttpServer proxyServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        proxyServer.createContext("/", exchange -> {
            requestLine.set(exchange.getRequestURI().toString());
            byte[] body = "via proxy".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        proxyServer.start();
        try {
            Proxy proxy = new Proxy();
            proxy.setHost(proxyServer.getAddress().getHostString());
            proxy.setPort(proxyServer.getAddress().getPort());
            proxy.setProtocol("http");

            UrlRequester.setProxy(proxy);
            // .invalid never resolves, so the request can only succeed through the proxy
            assertEquals("via proxy", UrlRequester.getFromUrl("http://license-host.invalid/licenses.txt"));
            assertEquals("http://license-host.invalid/licenses.txt", requestLine.get());
        } finally {
            UrlRequester.setProxy(null);
            proxyServer.stop(0);
        }
    }

    @Test
    void activeHttpProxyIsFoundInSettings() {
        Proxy inactive = new Proxy();
        inactive.setActive(false);
        inactive.setProtocol("http");
        Proxy https = new Proxy();
        https.setProtocol("https");
        Proxy active = new Proxy();
        active.setProtocol("http");

        assertSame(active, HttpClientProxy.findActiveProxy(Arrays.asList(inactive, https, active)));
        assertNull(HttpClientProxy.findActiveProxy(Arrays.asList(inactive, https)));
        assertNull(HttpClientProxy.findActiveProxy(null));
    }
}
