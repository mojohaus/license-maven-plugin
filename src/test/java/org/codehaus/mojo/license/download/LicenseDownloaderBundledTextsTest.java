package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 MojoHaus
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.download.BundledLicenseTexts.Mode;
import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.ContentSanitizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requests go through a local proxy that counts them, so no test reaches the license hosts: a request that should not
 * have happened shows up as a hit, and one that should have happened is answered with a marker text.
 */
class LicenseDownloaderBundledTextsTest {

    private static final String NETWORK = "text from the network";

    private static final String GPL3_TXT = "http://www.gnu.org/licenses/gpl-3.0.txt";

    private static final String GPL3_HTML = "http://www.gnu.org/licenses/gpl-3.0.html";

    @Test
    void textUrlIsWrittenWithoutRequest(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.text, Collections.emptyMap())) {
                final LicenseDownloadResult result =
                        downloader.downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, null));

                assertTrue(result.isSuccess(), result.getErrorMessage());
                assertEquals(0, hits.get());
                assertEquals(1, downloader.getBundledTextsServed());
                assertEquals("gpl.txt", result.getFile().getName());
                final String written =
                        new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8);
                assertTrue(written.contains("Version 3, 29 June 2007"));
                assertEquals(
                        DigestUtils.sha1Hex(Files.readAllBytes(result.getFile().toPath())), result.getSha1());
            }
        });
    }

    @Test
    void extensionFollowsPlainTextUnlessNamePreferred(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.text, Collections.emptyMap())) {
                assertEquals(
                        "a.txt",
                        downloader
                                .downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "a.html"), false, null))
                                .getFile()
                                .getName());
                assertEquals(
                        "b.html",
                        downloader
                                .downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "b.html"), true, null))
                                .getFile()
                                .getName());
                assertEquals(0, hits.get());
            }
        });
    }

    @Test
    void modeNoneDownloads(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.none, Collections.emptyMap())) {
                final LicenseDownloadResult result =
                        downloader.downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, null));

                assertEquals(1, hits.get());
                assertEquals(0, downloader.getBundledTextsServed());
                assertEquals(
                        NETWORK, new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8));
            }
        });
    }

    @Test
    void downloaderDefaultsToDownloading(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, null, Collections.emptyMap())) {
                downloader.downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, null));
                assertEquals(1, hits.get());
            }
        });
    }

    @Test
    void pageUrlIsDownloadedUnlessAllMode(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.text, Collections.emptyMap())) {
                downloader.downloadLicense(GPL3_HTML, new FileNameEntry(new File(dir, "text.txt"), false, null));
                assertEquals(1, hits.get());
                assertEquals(0, downloader.getBundledTextsServed());
            }
            try (LicenseDownloader downloader = downloader(proxy, Mode.all, Collections.emptyMap())) {
                final LicenseDownloadResult result = downloader.downloadLicense(
                        GPL3_HTML, new FileNameEntry(new File(dir, "all.html"), false, null));
                assertEquals(1, hits.get(), "no further request");
                assertEquals(1, downloader.getBundledTextsServed());
                assertEquals("all.txt", result.getFile().getName());
                assertTrue(new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8)
                        .contains("Version 3, 29 June 2007"));
            }
        });
    }

    @Test
    void expectedSha1OfTheHostFallsBackToDownload(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.text, Collections.emptyMap())) {
                final String sha1OfNetwork = DigestUtils.sha1Hex(NETWORK.getBytes(StandardCharsets.UTF_8));
                final LicenseDownloadResult result = downloader.downloadLicense(
                        GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, sha1OfNetwork));

                assertTrue(result.isSuccess(), result.getErrorMessage());
                assertEquals(1, hits.get());
                assertEquals(0, downloader.getBundledTextsServed());
            }
        });
    }

    @Test
    void expectedSha1OfTheBundledTextIsHonored(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = downloader(proxy, Mode.text, Collections.emptyMap())) {
                final File first = new File(dir, "first.txt");
                final String sha1 = downloader
                        .downloadLicense(GPL3_TXT, new FileNameEntry(first, false, null))
                        .getSha1();
                final LicenseDownloadResult result = downloader.downloadLicense(
                        GPL3_TXT, new FileNameEntry(new File(dir, "second.txt"), false, sha1));

                assertTrue(result.isSuccess(), result.getErrorMessage());
                assertEquals(0, hits.get());
                assertEquals(2, downloader.getBundledTextsServed());
            }
        });
    }

    @Test
    void urlOfTheAuthenticatedServerIsDownloaded(@TempDir File dir) throws Exception {
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader = new LicenseDownloader(
                    proxy,
                    5000,
                    5000,
                    5000,
                    Collections.emptyMap(),
                    StandardCharsets.UTF_8,
                    "user",
                    "secret",
                    "http://www.gnu.org/licenses/",
                    Collections.emptyMap())) {
                downloader.setBundledTextsMode(Mode.text);
                downloader.downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, null));
                assertEquals(1, hits.get());
            }
        });
    }

    @Test
    void sanitizersApplyToTheBundledText(@TempDir File dir) throws Exception {
        final ContentSanitizer sanitizer =
                ContentSanitizer.compile("test", ".*gpl-3\\.0\\.txt", "GNU GENERAL PUBLIC LICENSE", "SANITIZED");
        withProxy((proxy, hits) -> {
            try (LicenseDownloader downloader =
                    downloader(proxy, Mode.text, Collections.singletonMap("test", sanitizer))) {
                final LicenseDownloadResult result =
                        downloader.downloadLicense(GPL3_TXT, new FileNameEntry(new File(dir, "gpl.txt"), false, null));

                assertEquals(0, hits.get());
                final String written =
                        new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8);
                assertTrue(written.contains("SANITIZED"));
                assertFalse(written.contains("GNU GENERAL PUBLIC LICENSE"));
            }
        });
    }

    private interface ProxyTest {
        void run(Proxy proxy, AtomicInteger hits) throws Exception;
    }

    private static void withProxy(ProxyTest test) throws Exception {
        final AtomicInteger hits = new AtomicInteger();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            final byte[] body = NETWORK.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            final Proxy proxy = new Proxy();
            proxy.setHost("127.0.0.1");
            proxy.setPort(server.getAddress().getPort());
            proxy.setProtocol("http");
            test.run(proxy, hits);
        } finally {
            server.stop(0);
        }
    }

    private static LicenseDownloader downloader(Proxy proxy, Mode mode, java.util.Map<String, ContentSanitizer> s) {
        final LicenseDownloader downloader = new LicenseDownloader(proxy, 5000, 5000, 5000, s, StandardCharsets.UTF_8);
        if (mode != null) {
            downloader.setBundledTextsMode(mode);
        }
        return downloader;
    }
}
