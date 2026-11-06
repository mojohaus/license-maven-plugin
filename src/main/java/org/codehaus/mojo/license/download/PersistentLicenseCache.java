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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cache of downloaded license texts that outlives the build. One file per URL, named after the SHA-256 of the URL,
 * holds a small header (URL, download time, content type, validators) followed by the bytes exactly as the server sent
 * them. Because everything is in one file, replacing it with an atomic move is enough for builds that share the cache
 * directory: a reader sees the old entry or the new one, never half of either.
 *
 * @since 2.8.0
 */
public class PersistentLicenseCache {
    private static final Logger LOG = LoggerFactory.getLogger(PersistentLicenseCache.class);

    private static final String FORMAT = "license-maven-plugin-cache-1";

    private static final String SUFFIX = ".entry";

    /** Where the text of a license came from. */
    public enum Source {
        /** An entry younger than the time to live; no request was made. */
        CACHE,
        /** An expired entry that the server confirmed with {@code 304 Not Modified}. */
        REVALIDATED,
        /** An expired entry used because the server could not be reached or refused the request. */
        STALE,
        /** A response from the server. */
        NETWORK
    }

    /** A cache entry. */
    public static final class Entry {
        private final String url;
        private final Instant downloaded;
        private final String contentType;
        private final String etag;
        private final String lastModified;
        private final byte[] body;

        Entry(String url, Instant downloaded, String contentType, String etag, String lastModified, byte[] body) {
            this.url = url;
            this.downloaded = downloaded;
            this.contentType = contentType;
            this.etag = etag;
            this.lastModified = lastModified;
            this.body = body;
        }

        public String getUrl() {
            return url;
        }

        public Instant getDownloaded() {
            return downloaded;
        }

        /** @return the {@code Content-Type} header value, or {@code null} */
        public String getContentType() {
            return contentType;
        }

        /** @return the {@code ETag} header value, or {@code null} */
        public String getEtag() {
            return etag;
        }

        /** @return the {@code Last-Modified} header value, or {@code null} */
        public String getLastModified() {
            return lastModified;
        }

        public byte[] getBody() {
            return body.clone();
        }

        /** @return the body as an HTTP entity with the original content type */
        public ByteArrayEntity toEntity() {
            return entityOf(body, contentType);
        }
    }

    static ByteArrayEntity entityOf(byte[] body, String contentType) {
        final ByteArrayEntity entity = new ByteArrayEntity(body);
        if (contentType != null) {
            entity.setContentType(contentType);
        }
        return entity;
    }

    private final Path directory;
    private final Duration timeToLive;
    private final boolean readEnabled;
    private final Clock clock;
    private final Map<Source, AtomicInteger> counters = new EnumMap<>(Source.class);

    /**
     * @param directory where the entries live; created on the first write
     * @param timeToLive how long an entry is served without asking the server
     * @param readEnabled when {@code false}, {@link #find(String)} finds nothing, but {@link #store} still refreshes
     *        the entries
     */
    public PersistentLicenseCache(Path directory, Duration timeToLive, boolean readEnabled) {
        this(directory, timeToLive, readEnabled, Clock.systemUTC());
    }

    PersistentLicenseCache(Path directory, Duration timeToLive, boolean readEnabled, Clock clock) {
        this.directory = directory;
        this.timeToLive = timeToLive;
        this.readEnabled = readEnabled;
        this.clock = clock;
        for (Source source : Source.values()) {
            counters.put(source, new AtomicInteger());
        }
    }

    public Path getDirectory() {
        return directory;
    }

    Path entryPath(String url) {
        return directory.resolve(DigestUtils.sha256Hex(url) + SUFFIX);
    }

    /**
     * @param url the final download URL
     * @return the entry for {@code url}, fresh or not, or {@code null} if there is none, it cannot be read or reading
     *         is disabled
     */
    public Entry find(String url) {
        if (!readEnabled) {
            return null;
        }
        final Path path = entryPath(url);
        try {
            final byte[] bytes = readAllBytes(path);
            if (bytes == null) {
                return null;
            }
            final Entry entry = parse(bytes);
            if (entry == null || !url.equals(entry.url)) {
                LOG.debug("Ignoring unusable license cache entry {}", path);
                return null;
            }
            return entry;
        } catch (IOException | RuntimeException e) {
            LOG.debug("Ignoring unreadable license cache entry {}: {}", path, e.toString());
            return null;
        }
    }

    private static byte[] readAllBytes(Path path) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                return Files.readAllBytes(path);
            } catch (NoSuchFileException e) {
                return null;
            } catch (FileSystemException e) {
                /* Windows briefly denies access while another thread or process replaces the file */
                if (attempt >= 5) {
                    throw e;
                }
                pause(attempt);
            }
        }
    }

    /**
     * @param entry an entry returned by {@link #find(String)}
     * @return {@code true} if {@code entry} may be used without asking the server
     */
    public boolean isFresh(Entry entry) {
        final Duration age = Duration.between(entry.downloaded, clock.instant());
        return age.compareTo(timeToLive) < 0;
    }

    /**
     * Writes the entry for {@code url}, replacing an older one. A failure to write is logged and otherwise ignored:
     * the cache is an optimization.
     */
    public void store(String url, byte[] body, String contentType, String etag, String lastModified) {
        final Path target = entryPath(url);
        Path temp = null;
        try {
            Files.createDirectories(directory);
            temp = Files.createTempFile(directory, ".tmp-", ".part");
            Files.write(temp, serialize(new Entry(url, clock.instant(), contentType, etag, lastModified, body)));
            moveIntoPlace(temp, target);
            temp = null;
        } catch (IOException | RuntimeException e) {
            LOG.debug("Could not write license cache entry {}", target, e);
            LOG.warn("Could not write to the license cache '{}': {}", directory, e.toString());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    /* nothing more to do */
                }
            }
        }
    }

    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (FileSystemException e) {
                /*
                 * Windows refuses MoveFileEx when another thread or build has the target open or is
                 * replacing it at the same instant. Do not fall back to non-atomic REPLACE_EXISTING,
                 * which deletes the target before moving the temp file and leaves readers with a miss.
                 */
                if (attempt >= 5) {
                    if (Files.isRegularFile(target)) {
                        LOG.debug("Leaving existing license cache entry {} in place: {}", target, e.toString());
                        return;
                    }
                    throw e;
                }
                pause(attempt);
            }
        }
    }

    private static void pause(int attempt) {
        try {
            Thread.sleep(attempt + 1L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Restarts the time to live of {@code entry} after the server confirmed it. */
    public void refresh(Entry entry) {
        store(entry.url, entry.body, entry.contentType, entry.etag, entry.lastModified);
    }

    public void record(Source source) {
        counters.get(source).incrementAndGet();
    }

    public int count(Source source) {
        return counters.get(source).get();
    }

    static byte[] serialize(Entry entry) {
        final StringBuilder header = new StringBuilder();
        header.append(FORMAT).append('\n');
        appendHeader(header, "url", entry.url);
        appendHeader(header, "downloaded", Long.toString(entry.downloaded.toEpochMilli()));
        appendHeader(header, "length", Integer.toString(entry.body.length));
        appendHeader(header, "content-type", entry.contentType);
        appendHeader(header, "etag", entry.etag);
        appendHeader(header, "last-modified", entry.lastModified);
        header.append('\n');
        final byte[] head = header.toString().getBytes(StandardCharsets.UTF_8);
        final byte[] all = Arrays.copyOf(head, head.length + entry.body.length);
        System.arraycopy(entry.body, 0, all, head.length, entry.body.length);
        return all;
    }

    private static void appendHeader(StringBuilder sb, String name, String value) {
        if (value != null) {
            sb.append(name)
                    .append(": ")
                    .append(value.replace('\r', ' ').replace('\n', ' '))
                    .append('\n');
        }
    }

    /** @return the entry, or {@code null} if {@code data} is not a complete entry */
    static Entry parse(byte[] data) {
        /* the header ends at the first empty line */
        int end = -1;
        for (int i = 0; i + 1 < data.length; i++) {
            if (data[i] == '\n' && data[i + 1] == '\n') {
                end = i;
                break;
            }
        }
        if (end < 0) {
            return null;
        }
        final String[] lines = new String(data, 0, end, StandardCharsets.UTF_8).split("\n");
        if (!FORMAT.equals(lines[0])) {
            return null;
        }
        String url = null;
        String downloaded = null;
        String length = null;
        String contentType = null;
        String etag = null;
        String lastModified = null;
        for (int i = 1; i < lines.length; i++) {
            final int colon = lines[i].indexOf(": ");
            if (colon < 0) {
                continue;
            }
            final String value = lines[i].substring(colon + 2);
            switch (lines[i].substring(0, colon)) {
                case "url":
                    url = value;
                    break;
                case "downloaded":
                    downloaded = value;
                    break;
                case "length":
                    length = value;
                    break;
                case "content-type":
                    contentType = value;
                    break;
                case "etag":
                    etag = value;
                    break;
                case "last-modified":
                    lastModified = value;
                    break;
                default:
                    break;
            }
        }
        if (url == null || downloaded == null || length == null) {
            return null;
        }
        final int bodyStart = end + 2;
        final byte[] body = Arrays.copyOfRange(data, bodyStart, data.length);
        if (body.length != Integer.parseInt(length)) {
            return null;
        }
        return new Entry(url, Instant.ofEpochMilli(Long.parseLong(downloaded)), contentType, etag, lastModified, body);
    }
}
