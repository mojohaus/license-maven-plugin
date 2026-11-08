package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Codehaus
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

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseDownloaderTest {

    @Test
    void updateFileExtension() {
        assertExtension("path/to/file.html", "path/to/file.php", "text/html");
        assertExtension("path/to/file.txt", "path/to/file", null);
    }

    private static void assertExtension(String expected, String input, String mimeType) {
        final File in = new File(input);
        final File result = LicenseDownloader.updateFileExtension(in, mimeType);
        assertEquals(expected, result.getPath().replace('\\', '/'));
    }

    // -----------------------------------------------------------------------
    // shouldAuthenticate tests
    // -----------------------------------------------------------------------

    @Test
    void shouldAuthenticateMatchingUrl() {
        LicenseDownloader downloader =
                downloaderWithAuth("testuser", "testpass", "https://artifactory.example.com/artifactory");
        assertTrue(downloader.shouldAuthenticate(
                "https://artifactory.example.com/artifactory/libs-release/licenses/apache-2.0.txt"));
    }

    @Test
    void shouldNotAuthenticateNonMatchingHost() {
        LicenseDownloader downloader =
                downloaderWithAuth("testuser", "testpass", "https://artifactory.example.com/artifactory");
        assertFalse(downloader.shouldAuthenticate("https://www.apache.org/licenses/LICENSE-2.0.txt"));
    }

    @Test
    void shouldAuthenticateUrlExactlyMatchingServerUrl() {
        LicenseDownloader downloader =
                downloaderWithAuth("testuser", "testpass", "https://artifactory.example.com/artifactory");
        assertTrue(downloader.shouldAuthenticate("https://artifactory.example.com/artifactory"));
    }

    @Test
    void shouldNotAuthenticateWhenServerUrlIsNull() {
        LicenseDownloader downloader = downloaderWithAuth("testuser", "testpass", null);
        assertFalse(downloader.shouldAuthenticate("https://artifactory.example.com/artifactory/license.txt"));
    }

    @Test
    void shouldNotAuthenticateWhenServerUrlIsEmpty() {
        LicenseDownloader downloader = downloaderWithAuth("testuser", "testpass", "");
        assertFalse(downloader.shouldAuthenticate("https://artifactory.example.com/artifactory/license.txt"));
    }

    @Test
    void shouldNotAuthenticateWhenUserNameIsEmpty() {
        LicenseDownloader downloader =
                downloaderWithAuth("", "testpass", "https://artifactory.example.com/artifactory");
        assertFalse(downloader.shouldAuthenticate("https://artifactory.example.com/artifactory/license.txt"));
    }

    @Test
    void shouldNotAuthenticateWhenUserNameIsNull() {
        LicenseDownloader downloader =
                downloaderWithAuth(null, "testpass", "https://artifactory.example.com/artifactory");
        assertFalse(downloader.shouldAuthenticate("https://artifactory.example.com/artifactory/license.txt"));
    }

    @Test
    void shouldNotAuthenticateWhenServerUrlIsSubstringButNotPrefix() {
        LicenseDownloader downloader =
                downloaderWithAuth("testuser", "testpass", "https://artifactory.example.com/artifactory");
        // Different path that merely contains the serverUrl as a substring but doesn't start with it
        assertFalse(downloader.shouldAuthenticate(
                "https://other.example.com/?ref=https://artifactory.example.com/artifactory"));
    }

    // -----------------------------------------------------------------------
    // per-host stop on 429 and 503
    // -----------------------------------------------------------------------

    @Test
    void hostThatAnswered429IsNotRequestedAgain(@TempDir File dir) throws Exception {
        runWithServer(429, null, (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                LicenseDownloader.LicenseDownloadResult first = download(downloader, server, "a", dir);
                LicenseDownloader.LicenseDownloadResult second = download(downloader, server, "b", dir);

                assertFalse(first.isSuccess());
                assertTrue(first.getErrorMessage().contains("returned 429"), first.getErrorMessage());
                assertFalse(second.isSuccess());
                assertTrue(second.getErrorMessage().contains("not requested"), second.getErrorMessage());
                assertTrue(second.getErrorMessage().contains("host 'localhost' answered 429"));
                assertEquals(1, hits.get());
            }
        });
    }

    @Test
    void otherHostIsStillRequested(@TempDir File dir) throws Exception {
        runWithServer(429, null, (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                download(downloader, server, "a", dir);
                String otherHost = "http://127.0.0.1:" + server.getAddress().getPort() + "/b";
                LicenseDownloader.LicenseDownloadResult other =
                        downloader.downloadLicense(otherHost, new FileNameEntry(new File(dir, "b.txt"), false, null));

                assertTrue(other.getErrorMessage().contains("returned 429"), other.getErrorMessage());
                assertEquals(2, hits.get());
            }
        });
    }

    @Test
    void hostThatAnswered503IsNotRequestedAgain(@TempDir File dir) throws Exception {
        runWithServer(503, null, (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                download(downloader, server, "a", dir);
                LicenseDownloader.LicenseDownloadResult second = download(downloader, server, "b", dir);

                assertTrue(second.getErrorMessage().contains("host 'localhost' answered 503"));
                assertEquals(1, hits.get());
            }
        });
    }

    @Test
    void otherStatusDoesNotStopTheHost(@TempDir File dir) throws Exception {
        runWithServer(404, null, (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                download(downloader, server, "a", dir);
                LicenseDownloader.LicenseDownloadResult second = download(downloader, server, "b", dir);

                assertTrue(second.getErrorMessage().contains("returned 404"), second.getErrorMessage());
                assertEquals(2, hits.get());
            }
        });
    }

    @Test
    void retryAfterSecondsIsReported(@TempDir File dir) throws Exception {
        runWithServer(429, "120", (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                download(downloader, server, "a", dir);
                LicenseDownloader.LicenseDownloadResult second = download(downloader, server, "b", dir);

                assertTrue(second.getErrorMessage().contains("(Retry-After: 120 seconds)"), second.getErrorMessage());
            }
        });
    }

    @Test
    void retryAfterHttpDateIsReported(@TempDir File dir) throws Exception {
        runWithServer(503, "Wed, 21 Oct 2026 07:28:00 GMT", (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                download(downloader, server, "a", dir);
                LicenseDownloader.LicenseDownloadResult second = download(downloader, server, "b", dir);

                assertTrue(
                        second.getErrorMessage().contains("(Retry-After: Wed, 21 Oct 2026 07:28:00 GMT)"),
                        second.getErrorMessage());
            }
        });
    }

    // -----------------------------------------------------------------------
    // HTML responses are not license texts
    // -----------------------------------------------------------------------

    @Test
    void htmlContentTypeIsNotALicenseTextWhenRejected(@TempDir File dir) throws Exception {
        runWithBody("text/html; charset=UTF-8", "Permission is hereby granted", (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                downloader.setRejectHtml(true);
                LicenseDownloader.LicenseDownloadResult result = download(downloader, server, "a", dir);

                assertFalse(result.isSuccess());
                assertTrue(result.getErrorMessage().contains("did not return a license text"));
                assertEquals(0, dir.list().length);
            }
        });
    }

    @Test
    void htmlBodyIsNotALicenseTextWhenRejected(@TempDir File dir) throws Exception {
        runWithBody("text/plain", "\uFEFF \n<!DOCTYPE HTML>\n<html><title>MIT</title></html>", (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                downloader.setRejectHtml(true);
                LicenseDownloader.LicenseDownloadResult result = download(downloader, server, "a", dir);

                assertFalse(result.isSuccess());
                assertTrue(result.getErrorMessage().contains("did not return a license text"));
                assertEquals(0, dir.list().length);
            }
        });
    }

    @Test
    void htmlIsKeptByDefault(@TempDir File dir) throws Exception {
        runWithBody("text/html; charset=UTF-8", "<html><body>GPL page</body></html>", (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                LicenseDownloader.LicenseDownloadResult result = download(downloader, server, "a", dir);

                assertTrue(result.isSuccess(), result.getErrorMessage());
                assertEquals(1, dir.list().length);
            }
        });
    }

    @Test
    void plainTextIsStillDownloaded(@TempDir File dir) throws Exception {
        final String text = "MIT License. Use <html> tags freely.\nPermission is hereby granted";
        runWithBody("text/plain", text, (server, hits) -> {
            try (LicenseDownloader downloader = downloader()) {
                LicenseDownloader.LicenseDownloadResult result = download(downloader, server, "a", dir);

                assertTrue(result.isSuccess(), result.getErrorMessage());
                assertEquals(
                        text,
                        new String(
                                java.nio.file.Files.readAllBytes(new File(dir, "a.txt").toPath()),
                                StandardCharsets.UTF_8));
            }
        });
    }

    private static void runWithBody(String contentType, String body, ServerTest test) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            test.run(server, new AtomicInteger());
        } finally {
            server.stop(0);
        }
    }

    private interface ServerTest {
        void run(HttpServer server, AtomicInteger hits) throws Exception;
    }

    private static void runWithServer(int status, String retryAfter, ServerTest test) throws Exception {
        final AtomicInteger hits = new AtomicInteger();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            if (retryAfter != null) {
                exchange.getResponseHeaders().add("Retry-After", retryAfter);
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            test.run(server, hits);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void relativeFileUrlsAreResolvedAgainstTheBasedir(@TempDir File dir) throws Exception {
        final File basedir = new File(dir, "project");
        final File source = new File(basedir, "licenses/company.html");
        assertTrue(source.getParentFile().mkdirs());
        Files.write(source.toPath(), "<p>company</p>".getBytes(StandardCharsets.UTF_8));

        int i = 0;
        for (String url : new String[] {
            "file:licenses/company.html", "file://licenses/company.html", "file://./licenses/company.html"
        }) {
            try (LicenseDownloader d = downloader()) {
                d.setBasedir(basedir.toPath());
                final File out = new File(dir, "out" + i++ + "/company");
                assertTrue(out.getParentFile().mkdirs());

                final LicenseDownloader.LicenseDownloadResult result =
                        d.downloadLicense(url, new FileNameEntry(out, false, null));

                assertTrue(result.isSuccess(), url);
                assertEquals("company.html", result.getFile().getName(), url);
                assertEquals(
                        "<p>company</p>",
                        new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void absoluteFileUrlIsStillRead(@TempDir File dir) throws Exception {
        final File source = new File(dir, "elsewhere.txt");
        Files.write(source.toPath(), "text".getBytes(StandardCharsets.UTF_8));

        try (LicenseDownloader d = downloader()) {
            d.setBasedir(new File(dir, "project").toPath());
            final LicenseDownloader.LicenseDownloadResult result = d.downloadLicense(
                    source.toURI().toString().replace("file:/", "file:///"),
                    new FileNameEntry(new File(dir, "my license"), false, null));

            assertTrue(result.isSuccess());
            assertEquals("my license.txt", result.getFile().getName());
        }
    }

    @Test
    void fileUrlTakesTheExtensionOfTheSourceFile(@TempDir File dir) throws Exception {
        final File html = new File(dir, "a.html");
        final File noExtension = new File(dir, "COPYING");
        Files.write(html.toPath(), "x".getBytes(StandardCharsets.UTF_8));
        Files.write(noExtension.toPath(), "x".getBytes(StandardCharsets.UTF_8));

        try (LicenseDownloader d = downloader()) {
            assertEquals(
                    "mit - a.html",
                    d.downloadLicense(html.toURI().toString().replace("file:/", "file:///"), entry(dir, "mit - a.html"))
                            .getFile()
                            .getName());
            assertEquals(
                    "mit - a.html",
                    d.downloadLicense(html.toURI().toString().replace("file:/", "file:///"), entry(dir, "mit - a"))
                            .getFile()
                            .getName());
            assertEquals(
                    "mit - copying.txt",
                    d.downloadLicense(
                                    noExtension.toURI().toString().replace("file:/", "file:///"),
                                    entry(dir, "mit - copying"))
                            .getFile()
                            .getName());
        }
    }

    @Test
    void preferredFileNameKeepsItsExtensionForAFileUrl(@TempDir File dir) throws Exception {
        final File html = new File(dir, "a.html");
        Files.write(html.toPath(), "x".getBytes(StandardCharsets.UTF_8));

        try (LicenseDownloader d = downloader()) {
            final LicenseDownloader.LicenseDownloadResult result = d.downloadLicense(
                    html.toURI().toString().replace("file:/", "file:///"),
                    new FileNameEntry(new File(dir, "preferred.license"), true, null));

            assertEquals("preferred.license", result.getFile().getName());
        }
    }

    private static FileNameEntry entry(File dir, String name) {
        final File out = new File(new File(dir, "out" + System.nanoTime()), name);
        assertTrue(out.getParentFile().mkdirs());
        return new FileNameEntry(out, false, null);
    }

    private static LicenseDownloader.LicenseDownloadResult download(
            LicenseDownloader downloader, HttpServer server, String name, File dir) throws Exception {
        return downloader.downloadLicense(
                "http://localhost:" + server.getAddress().getPort() + "/" + name,
                new FileNameEntry(new File(dir, name + ".txt"), false, null));
    }

    private static LicenseDownloader downloader() {
        return downloaderWithAuth(null, null, null);
    }

    private static LicenseDownloader downloaderWithAuth(String userName, String password, String serverUrl) {
        return new LicenseDownloader(
                null,
                5000,
                5000,
                5000,
                Collections.emptyMap(),
                StandardCharsets.UTF_8,
                userName,
                password,
                serverUrl,
                Collections.emptyMap());
    }
}
