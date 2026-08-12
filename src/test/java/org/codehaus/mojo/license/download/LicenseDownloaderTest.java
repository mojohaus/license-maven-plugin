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
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

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
