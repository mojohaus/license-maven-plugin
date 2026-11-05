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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BundledLicenseTextsTest {

    private static final BundledLicenseTexts TEXTS = BundledLicenseTexts.getDefault();

    @Test
    void normalizeIgnoresSchemeWwwHostCaseFragmentAndWhitespace() {
        final String expected = "gnu.org/licenses/gpl-3.0.txt";
        assertEquals(expected, BundledLicenseTexts.normalize("https://www.gnu.org/licenses/gpl-3.0.txt"));
        assertEquals(expected, BundledLicenseTexts.normalize("http://www.gnu.org/licenses/gpl-3.0.txt"));
        assertEquals(expected, BundledLicenseTexts.normalize("HTTP://WWW.GNU.ORG/licenses/gpl-3.0.txt"));
        assertEquals(expected, BundledLicenseTexts.normalize("https://gnu.org/licenses/gpl-3.0.txt"));
        assertEquals(expected, BundledLicenseTexts.normalize("  https://www.gnu.org/licenses/gpl-3.0.txt\n"));
        assertEquals(expected, BundledLicenseTexts.normalize("https://www.gnu.org/licenses/gpl-3.0.txt#top"));
    }

    @Test
    void normalizeKeepsPathCase() {
        assertEquals(
                "apache.org/licenses/LICENSE-2.0.txt",
                BundledLicenseTexts.normalize("http://www.apache.org/licenses/LICENSE-2.0.txt"));
        assertEquals(
                "apache.org/licenses/license-2.0.txt",
                BundledLicenseTexts.normalize("http://www.apache.org/licenses/license-2.0.txt"));
    }

    @Test
    void normalizeRejectsWhatMayServeOtherContent() {
        assertNull(BundledLicenseTexts.normalize(null));
        assertNull(BundledLicenseTexts.normalize(""));
        assertNull(BundledLicenseTexts.normalize("file:///licenses/gpl-3.0.txt"));
        assertNull(BundledLicenseTexts.normalize("ftp://www.gnu.org/licenses/gpl-3.0.txt"));
        assertNull(BundledLicenseTexts.normalize("https://www.gnu.org/licenses/gpl-3.0.txt?download=1"));
        assertNull(BundledLicenseTexts.normalize("https://www.gnu.org:8443/licenses/gpl-3.0.txt"));
        assertNull(BundledLicenseTexts.normalize("https://user@www.gnu.org/licenses/gpl-3.0.txt"));
        assertNull(BundledLicenseTexts.normalize("https://www.gnu.org"));
        assertNull(BundledLicenseTexts.normalize("not a url"));
    }

    @Test
    void textUrlsAreFoundInTextAndAllModes() {
        for (String url : new String[] {
            "https://www.gnu.org/licenses/gpl-3.0.txt",
            "http://www.gnu.org/licenses/gpl-3.0.txt",
            "https://gnu.org/licenses/gpl-3.0.txt",
            "https://www.apache.org/licenses/LICENSE-2.0.txt",
            "http://www.apache.org/licenses/LICENSE-2.0",
            "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt",
            "https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt"
        }) {
            assertNotNull(TEXTS.find(url, BundledLicenseTexts.Mode.text), url);
            assertNotNull(TEXTS.find(url, BundledLicenseTexts.Mode.all), url);
            assertNull(TEXTS.find(url, BundledLicenseTexts.Mode.none), url);
            assertNull(TEXTS.find(url, null), url);
        }
        assertEquals(
                "gpl_v3",
                TEXTS.find("https://www.gnu.org/licenses/gpl-3.0.txt", BundledLicenseTexts.Mode.text)
                        .getLicenseId());
        assertEquals(
                "apache_v2",
                TEXTS.find("http://www.apache.org/licenses/LICENSE-2.0", BundledLicenseTexts.Mode.text)
                        .getLicenseId());
    }

    @Test
    void pageUrlsAreFoundOnlyInAllMode() {
        for (String url : new String[] {
            "https://www.gnu.org/licenses/gpl-3.0.html",
            "http://www.apache.org/licenses/LICENSE-2.0.html",
            "http://www.eclipse.org/legal/epl-v10.html"
        }) {
            assertNull(TEXTS.find(url, BundledLicenseTexts.Mode.text), url);
            assertNull(TEXTS.find(url, BundledLicenseTexts.Mode.none), url);
            assertNotNull(TEXTS.find(url, BundledLicenseTexts.Mode.all), url);
        }
    }

    @Test
    void unknownUrlsAreNotFound() {
        for (String url : new String[] {
            "https://www.gnu.org/licenses/gpl.txt", // moves with the latest version, not mapped
            "https://www.gnu.org/licenses/gpl-3.0.txt?x=1",
            "https://www.gnu.org/licenses/GPL-3.0.txt",
            "https://example.com/licenses/gpl-3.0.txt",
            "https://www.gnu.org.example.com/licenses/gpl-3.0.txt",
            "https://opensource.org/licenses/MIT",
            "file:///tmp/gpl-3.0.txt"
        }) {
            assertNull(TEXTS.find(url, BundledLicenseTexts.Mode.all), url);
        }
    }

    @Test
    void tableIsConsistent() throws Exception {
        final Map<String, BundledLicenseTexts.Entry> entries = TEXTS.entries();
        assertTrue(entries.size() > 20, "table was not loaded");
        for (Map.Entry<String, BundledLicenseTexts.Entry> e : entries.entrySet()) {
            final String key = e.getKey();
            assertEquals(key, BundledLicenseTexts.normalize("https://" + key), "key is not normalized: " + key);
            assertTrue(!key.startsWith("www."), key);
            final String resource = "META-INF/licenses/" + e.getValue().getLicenseId() + "/license.txt";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
                assertNotNull(in, "no bundled text for " + key + ": " + resource);
            }
            assertTrue(e.getValue().readText().length > 1000, key);
        }
    }

    @Test
    void bundledTextIsTheLicense() throws Exception {
        final String gpl3 = new String(
                TEXTS.find("https://www.gnu.org/licenses/gpl-3.0.txt", BundledLicenseTexts.Mode.text)
                        .readText(),
                StandardCharsets.UTF_8);
        assertTrue(gpl3.contains("GNU GENERAL PUBLIC LICENSE"));
        assertTrue(gpl3.contains("Version 3, 29 June 2007"));
        final String asl = new String(
                TEXTS.find("https://www.apache.org/licenses/LICENSE-2.0.txt", BundledLicenseTexts.Mode.text)
                        .readText(),
                StandardCharsets.UTF_8);
        assertTrue(asl.contains("Apache License"));
        assertTrue(asl.contains("Version 2.0, January 2004"));
    }
}
