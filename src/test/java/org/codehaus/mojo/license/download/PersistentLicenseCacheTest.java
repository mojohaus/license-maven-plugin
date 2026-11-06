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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentLicenseCacheTest {

    private static final String URL = "https://www.gnu.org/licenses/lgpl-3.0.txt";

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock();

    private PersistentLicenseCache cache(boolean readEnabled) {
        return new PersistentLicenseCache(dir.resolve("cache"), Duration.ofDays(30), readEnabled, clock);
    }

    @Test
    void missWhenNothingStored() {
        assertNull(cache(true).find(URL));
    }

    @Test
    void storedEntryIsFound() {
        final byte[] body = "text ä\r\n\n\nwith blank lines".getBytes(StandardCharsets.UTF_8);
        cache(true).store(URL, body, "text/plain; charset=UTF-8", "\"abc\"", "Wed, 01 Jan 2025 00:00:00 GMT");

        final PersistentLicenseCache.Entry entry = cache(true).find(URL);
        assertNotNull(entry);
        assertEquals(URL, entry.getUrl());
        assertArrayEquals(body, entry.getBody());
        assertEquals("text/plain; charset=UTF-8", entry.getContentType());
        assertEquals("\"abc\"", entry.getEtag());
        assertEquals("Wed, 01 Jan 2025 00:00:00 GMT", entry.getLastModified());
        assertEquals(clock.instant(), entry.getDownloaded());
    }

    @Test
    void optionalHeadersMayBeAbsent() {
        cache(true).store(URL, new byte[0], null, null, null);

        final PersistentLicenseCache.Entry entry = cache(true).find(URL);
        assertNotNull(entry);
        assertEquals(0, entry.getBody().length);
        assertNull(entry.getContentType());
        assertNull(entry.getEtag());
    }

    @Test
    void entryExpiresAfterTimeToLive() {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "x".getBytes(StandardCharsets.UTF_8), null, null, null);
        final PersistentLicenseCache.Entry entry = cache.find(URL);

        clock.advance(Duration.ofDays(29));
        assertTrue(cache.isFresh(entry));
        clock.advance(Duration.ofDays(1));
        assertFalse(cache.isFresh(entry));
        assertNotNull(cache.find(URL), "an expired entry is still returned for revalidation");
    }

    @Test
    void refreshRestartsTheTimeToLive() {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "x".getBytes(StandardCharsets.UTF_8), null, "\"e\"", null);
        clock.advance(Duration.ofDays(31));

        cache.refresh(cache.find(URL));

        final PersistentLicenseCache.Entry entry = cache.find(URL);
        assertTrue(cache.isFresh(entry));
        assertEquals("\"e\"", entry.getEtag());
    }

    @Test
    void readDisabledFindsNothingButStillWrites() {
        cache(false).store(URL, "x".getBytes(StandardCharsets.UTF_8), null, null, null);

        assertNull(cache(false).find(URL));
        assertNotNull(cache(true).find(URL));
    }

    @Test
    void entriesOfOtherUrlsAreSeparate() {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "a".getBytes(StandardCharsets.UTF_8), null, null, null);

        assertNull(cache.find(URL + "?other"));
    }

    @Test
    void truncatedEntryIsAMiss() throws IOException {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "0123456789".getBytes(StandardCharsets.UTF_8), null, null, null);
        final Path file = cache.entryPath(URL);
        final byte[] all = Files.readAllBytes(file);
        Files.write(file, java.util.Arrays.copyOf(all, all.length - 3));

        assertNull(cache.find(URL));
    }

    @Test
    void garbageIsAMiss() throws IOException {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "x".getBytes(StandardCharsets.UTF_8), null, null, null);
        Files.write(cache.entryPath(URL), "not an entry".getBytes(StandardCharsets.UTF_8));

        assertNull(cache.find(URL));
    }

    @Test
    void writeLeavesNoTemporaryFiles() throws IOException {
        final PersistentLicenseCache cache = cache(true);
        cache.store(URL, "x".getBytes(StandardCharsets.UTF_8), null, null, null);
        cache.store(URL, "y".getBytes(StandardCharsets.UTF_8), null, null, null);

        try (Stream<Path> files = Files.list(cache.getDirectory())) {
            final List<String> names =
                    files.map(p -> p.getFileName().toString()).collect(Collectors.toList());
            assertEquals(1, names.size(), names.toString());
            assertTrue(names.get(0).endsWith(".entry"), names.toString());
        }
    }

    @Test
    void concurrentWritersNeverLeaveAnUnreadableEntry() throws Exception {
        final int writers = 8;
        final int rounds = 50;
        final ExecutorService pool = Executors.newFixedThreadPool(writers + 1);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Future<?>> futures = new ArrayList<>();
            for (int w = 0; w < writers; w++) {
                final byte[] body = new byte[100_000];
                java.util.Arrays.fill(body, (byte) ('a' + w));
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < rounds; i++) {
                        cache(true).store(URL, body, "text/plain", null, null);
                    }
                    return null;
                }));
            }
            final Future<Integer> reader = pool.submit(() -> {
                start.await();
                int unreadable = 0;
                boolean seen = false;
                for (int i = 0; i < rounds * 4; i++) {
                    final PersistentLicenseCache.Entry entry = cache(true).find(URL);
                    if (entry == null) {
                        /* null is only legal before the first write finished */
                        if (seen) {
                            unreadable++;
                        }
                    } else {
                        seen = true;
                        final byte[] body = entry.getBody();
                        for (byte b : body) {
                            if (b != body[0]) {
                                unreadable++;
                                break;
                            }
                        }
                    }
                }
                return unreadable;
            });
            start.countDown();
            for (Future<?> f : futures) {
                f.get();
            }
            assertEquals(0, reader.get());
        } finally {
            pool.shutdownNow();
        }
        final PersistentLicenseCache.Entry last = cache(true).find(URL);
        assertNotNull(last);
        assertEquals(100_000, last.getBody().length);
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
