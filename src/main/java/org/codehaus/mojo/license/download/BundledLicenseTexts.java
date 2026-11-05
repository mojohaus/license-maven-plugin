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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * A curated table of well-known license URLs whose content is available as a text bundled with the plugin, so that
 * the plugin does not have to request them from the license host.
 *
 * <p>The table is the resource {@value #TABLE_RESOURCE}. Every entry maps one exact URL, without scheme and without
 * a leading {@code www.}, to a {@link Kind} and the id of a bundled license, which is the directory name under
 * {@code META-INF/licenses/}. It is a URL to text mapping and not a URL to identity mapping: it says "this URL
 * serves this document", not "the license of this project is X".
 *
 * @since 2.8.0
 */
public final class BundledLicenseTexts {

    /** The class path location of the URL table. */
    public static final String TABLE_RESOURCE = "META-INF/licenses/bundled-license-urls.properties";

    /** Where the license ids of the table are resolved. */
    private static final String LICENSES_ROOT = "META-INF/licenses/";

    /**
     * Which of the bundled texts replace a download.
     *
     * @since 2.8.0
     */
    public enum Mode {
        /** Download every license, as in previous versions of the plugin. */
        none,
        /** Serve bundled texts only for URLs that return plain text. This is the default. */
        text,
        /** Serve bundled texts also for URLs of HTML pages, which changes the name, content and checksum of the file. */
        all;

        boolean admits(Kind kind) {
            switch (this) {
                case text:
                    return kind == Kind.text;
                case all:
                    return true;
                default:
                    return false;
            }
        }
    }

    /** What the URL returns. */
    enum Kind {
        /** A plain text document, the same one as the bundled text. */
        text,
        /** An HTML page that contains the license text next to site navigation. */
        page
    }

    /** A matched table entry. */
    public static final class Entry {
        private final String licenseId;
        private final Kind kind;

        Entry(String licenseId, Kind kind) {
            this.licenseId = licenseId;
            this.kind = kind;
        }

        /** @return the directory name of the bundled license under {@code META-INF/licenses/} */
        public String getLicenseId() {
            return licenseId;
        }

        Kind getKind() {
            return kind;
        }

        /**
         * @return the bundled text
         * @throws IOException if the resource cannot be read
         */
        public byte[] readText() throws IOException {
            final String name = LICENSES_ROOT + licenseId + "/license.txt";
            try (InputStream in = BundledLicenseTexts.class.getClassLoader().getResourceAsStream(name)) {
                if (in == null) {
                    throw new IOException("Bundled license text not found: " + name);
                }
                return readAll(in);
            }
        }
    }

    private static final BundledLicenseTexts DEFAULT = new BundledLicenseTexts(loadTable(TABLE_RESOURCE));

    private final Map<String, Entry> entries;

    BundledLicenseTexts(Map<String, Entry> entries) {
        this.entries = entries;
    }

    /** @return the table shipped with the plugin */
    public static BundledLicenseTexts getDefault() {
        return DEFAULT;
    }

    /**
     * Finds the bundled text for a URL.
     *
     * @param url the license URL, already rewritten by the configured URL replacements
     * @param mode which kinds of URLs may be answered from a bundled text
     * @return the entry, or {@code null} if the URL is not in the table or {@code mode} does not admit its kind
     */
    public Entry find(String url, Mode mode) {
        if (mode == null || mode == Mode.none) {
            return null;
        }
        final String key = normalize(url);
        final Entry entry = key != null ? entries.get(key) : null;
        return entry != null && mode.admits(entry.kind) ? entry : null;
    }

    /**
     * Reduces a URL to the form of the table keys: no scheme (http and https are the same document), lower case
     * host without a leading {@code www.}, no fragment and no surrounding white space. The path keeps its case. A URL
     * with a query, user info or a port is never normalized, because it may serve something else.
     *
     * @param url the URL to normalize
     * @return the table key or {@code null} if the URL cannot match any table entry
     */
    static String normalize(String url) {
        if (url == null) {
            return null;
        }
        final URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            return null;
        }
        final String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : null;
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        if (uri.getRawQuery() != null || uri.getRawUserInfo() != null || uri.getPort() != -1) {
            return null;
        }
        String host = uri.getHost();
        final String path = uri.getRawPath();
        if (host == null || path == null || path.isEmpty()) {
            return null;
        }
        host = host.toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring("www.".length());
        }
        return host + path;
    }

    static Map<String, Entry> loadTable(String resource) {
        final Properties props = new Properties();
        try (InputStream in = BundledLicenseTexts.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + resource);
            }
            props.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final Map<String, Entry> result = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            final String value = props.getProperty(key).trim();
            final int colon = value.indexOf(':');
            if (colon < 0) {
                throw new IllegalStateException("Entry '" + key + "' of " + resource + " must be <kind>:<licenseId>");
            }
            result.put(key, new Entry(value.substring(colon + 1), Kind.valueOf(value.substring(0, colon))));
        }
        return result;
    }

    /** @return the number of URLs in the table */
    int size() {
        return entries.size();
    }

    Map<String, Entry> entries() {
        return entries;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // CHECKSTYLE_OFF: MagicNumber
        final byte[] buf = new byte[8192];
        // CHECKSTYLE_ON: MagicNumber
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        return out.toByteArray();
    }
}
