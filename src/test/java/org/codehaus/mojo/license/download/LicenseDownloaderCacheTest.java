package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.codehaus.mojo.license.download.PersistentLicenseCache.Source;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.ContentSanitizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseDownloaderCacheTest {

    private static final byte[] BODY = "The license ä text\r\nline two\r\n".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tmp;

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private final List<String> ifNoneMatch = new CopyOnWriteArrayList<>();
    private final List<String> ifModifiedSince = new CopyOnWriteArrayList<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<byte[]> body = new AtomicReference<>(BODY);
    private final AtomicReference<String> etag = new AtomicReference<>("\"v1\"");
    private final MutableClock clock = new MutableClock();
    private String url;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            final String inm = exchange.getRequestHeaders().getFirst("If-None-Match");
            final String ims = exchange.getRequestHeaders().getFirst("If-Modified-Since");
            if (inm != null) {
                ifNoneMatch.add(inm);
            }
            if (ims != null) {
                ifModifiedSince.add(ims);
            }
            int code = status.get();
            if (code == 200 && etag.get() != null && etag.get().equals(inm)) {
                code = 304;
            }
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            if (etag.get() != null) {
                exchange.getResponseHeaders().add("ETag", etag.get());
            }
            exchange.getResponseHeaders().add("Last-Modified", "Wed, 01 Jan 2025 00:00:00 GMT");
            if (code == 200) {
                final byte[] b = body.get();
                exchange.sendResponseHeaders(200, b.length);
                exchange.getResponseBody().write(b);
            } else {
                exchange.sendResponseHeaders(code, -1);
            }
            exchange.close();
        });
        server.start();
        url = "http://localhost:" + server.getAddress().getPort() + "/lgpl.txt";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private PersistentLicenseCache cache(boolean readEnabled) {
        return new PersistentLicenseCache(tmp.resolve("cache"), Duration.ofDays(30), readEnabled, clock);
    }

    private static LicenseDownloader downloader(PersistentLicenseCache cache, ContentSanitizer... sanitizers) {
        final java.util.Map<String, ContentSanitizer> map = new java.util.LinkedHashMap<>();
        for (ContentSanitizer s : sanitizers) {
            map.put(s.toString() + map.size(), s);
        }
        return new LicenseDownloader(
                null, 5000, 5000, 5000, map, StandardCharsets.UTF_8, null, null, null, Collections.emptyMap(), cache);
    }

    private LicenseDownloadResult download(LicenseDownloader d, String u, String name) throws Exception {
        final File out = tmp.resolve("out").resolve(name).toFile();
        Files.createDirectories(out.getParentFile().toPath());
        return d.downloadLicense(u, new FileNameEntry(out, false, null));
    }

    @Test
    void secondDownloadIsServedWithoutARequest() throws Exception {
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            final LicenseDownloadResult first = download(d, url, "one.txt");
            assertTrue(first.isSuccess(), first.getErrorMessage());
            assertEquals(1, requests.get());
        }
        /* a later build: new downloader, new cache object, same directory */
        final PersistentLicenseCache second = cache(true);
        try (LicenseDownloader d = downloader(second)) {
            final LicenseDownloadResult result = download(d, url, "two.txt");
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertEquals(1, requests.get(), "no request for a fresh entry");
            assertArrayEquals(BODY, Files.readAllBytes(result.getFile().toPath()));
            assertEquals(1, second.count(Source.CACHE));
        }
    }

    @Test
    void outputIsIdenticalToUncachedDownload() throws Exception {
        final LicenseDownloadResult plain;
        try (LicenseDownloader d = downloader(null)) {
            plain = download(d, url, "plain.txt");
        }
        final byte[] expected = Files.readAllBytes(plain.getFile().toPath());
        try (LicenseDownloader d = downloader(cache(true))) {
            final LicenseDownloadResult viaNetwork = download(d, url, "net.txt");
            final LicenseDownloadResult viaCache = download(d, url + "", "cached.txt");
            assertArrayEquals(expected, Files.readAllBytes(viaNetwork.getFile().toPath()));
            assertArrayEquals(expected, Files.readAllBytes(viaCache.getFile().toPath()));
            assertEquals(plain.getSha1(), viaCache.getSha1());
            assertEquals(plain.getFile().getName().replaceAll("^plain", "x"), "x.txt");
        }
    }

    @Test
    void sanitizersApplyToCachedText() throws Exception {
        final ContentSanitizer sanitizer = ContentSanitizer.compile("s", ".*", "line two", "LINE 2");
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "warm.txt");
        }
        try (LicenseDownloader d = downloader(cache(true), sanitizer)) {
            final LicenseDownloadResult result = download(d, url, "sanitized.txt");
            assertEquals(1, requests.get());
            final String content =
                    new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8);
            assertTrue(content.contains("LINE 2"), content);
            assertFalse(content.contains("line two"), content);
        }
    }

    @Test
    void expiredEntryIsRevalidatedAndRefreshedOn304() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        clock.advance(Duration.ofDays(31));
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertArrayEquals(BODY, Files.readAllBytes(result.getFile().toPath()));
            assertEquals(2, requests.get());
            assertEquals(java.util.Arrays.asList("\"v1\""), ifNoneMatch);
            assertEquals(java.util.Arrays.asList("Wed, 01 Jan 2025 00:00:00 GMT"), ifModifiedSince);
            assertEquals(1, cache.count(Source.REVALIDATED));

            /* the 304 restarted the clock */
            download(d, url, "c.txt");
            assertEquals(2, requests.get());
        }
    }

    @Test
    void expiredEntryIsReplacedWhenTheServerHasNewContent() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        clock.advance(Duration.ofDays(31));
        body.set("new text".getBytes(StandardCharsets.UTF_8));
        etag.set("\"v2\"");
        try (LicenseDownloader d = downloader(cache(true))) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertEquals(
                    "new text", new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8));
        }
        assertEquals("\"v2\"", cache(true).find(url).getEtag());
    }

    @Test
    void staleEntryIsUsedWhenTheServerAnswers429() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        clock.advance(Duration.ofDays(31));
        status.set(429);
        etag.set(null);
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertArrayEquals(BODY, Files.readAllBytes(result.getFile().toPath()));
            assertEquals(1, cache.count(Source.STALE));

            /* the host is stopped now: the next stale text costs no request */
            final int before = requests.get();
            assertTrue(download(d, url, "c.txt").isSuccess());
            assertEquals(before, requests.get());
        }
    }

    @Test
    void staleEntryIsUsedWhenTheServerIsUnreachable() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        clock.advance(Duration.ofDays(31));
        server.stop(0);
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertEquals(1, cache.count(Source.STALE));
        }
    }

    @Test
    void notFoundIsNotMaskedByAStaleEntry() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        clock.advance(Duration.ofDays(31));
        status.set(404);
        etag.set(null);
        try (LicenseDownloader d = downloader(cache(true))) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("returned 404"), result.getErrorMessage());
        }
    }

    @Test
    void failuresAreNotCached() throws Exception {
        status.set(404);
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            assertFalse(download(d, url, "a.txt").isSuccess());
        }
        assertNull(cache.find(url));
        assertEquals(0, cache.count(Source.NETWORK));
    }

    @Test
    void unreachableServerWithoutEntryStillFails() throws Exception {
        final int port;
        try (ServerSocket s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }
        try (LicenseDownloader d = downloader(cache(true))) {
            org.junit.jupiter.api.Assertions.assertThrows(
                    java.io.IOException.class, () -> download(d, "http://localhost:" + port + "/x", "a.txt"));
        }
    }

    @Test
    void forceDownloadBypassesTheCacheButRefreshesIt() throws Exception {
        try (LicenseDownloader d = downloader(cache(true))) {
            download(d, url, "a.txt");
        }
        body.set("forced".getBytes(StandardCharsets.UTF_8));
        etag.set("\"v2\"");
        try (LicenseDownloader d = downloader(cache(false))) {
            final LicenseDownloadResult result = download(d, url, "b.txt");
            assertEquals(
                    "forced", new String(Files.readAllBytes(result.getFile().toPath()), StandardCharsets.UTF_8));
            assertEquals(2, requests.get());
            assertTrue(ifNoneMatch.isEmpty(), "a forced download is unconditional");
        }
        assertEquals("forced", new String(cache(true).find(url).getBody(), StandardCharsets.UTF_8));
    }

    @Test
    void authenticatedDownloadsAreNotCached() throws Exception {
        final PersistentLicenseCache cache = cache(true);
        final String serverUrl = "http://localhost:" + server.getAddress().getPort();
        try (LicenseDownloader d = new LicenseDownloader(
                null,
                5000,
                5000,
                5000,
                Collections.emptyMap(),
                StandardCharsets.UTF_8,
                "user",
                "secret",
                serverUrl,
                Collections.emptyMap(),
                cache)) {
            assertTrue(download(d, url, "a.txt").isSuccess());
            assertTrue(download(d, url, "b.txt").isSuccess());
        }
        assertEquals(2, requests.get());
        assertNull(cache.find(url));
    }

    @Test
    void fileUrlsAreNotCached() throws Exception {
        final Path source = tmp.resolve("local-license.txt");
        Files.write(source, BODY);
        final PersistentLicenseCache cache = cache(true);
        try (LicenseDownloader d = downloader(cache)) {
            final LicenseDownloadResult result = download(d, source.toUri().toString(), "f.txt");
            assertTrue(result.isSuccess(), result.getErrorMessage());
        }
        assertFalse(Files.exists(cache.getDirectory()), "nothing was written");
        assertNotNull(url);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
