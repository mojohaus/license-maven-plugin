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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LicenseFileDeduplicatorTest {

    @TempDir
    Path tempDir;

    @Test
    void duplicateFileIsDeletedAndTheOriginalIsReused() throws Exception {
        File first = Files.write(
                        tempDir.resolve("apache-2.txt"), "Apache License\nVersion 2.0".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File duplicate = Files.write(
                        tempDir.resolve("2.0.txt"), "apache license version 2.0".getBytes(StandardCharsets.UTF_8))
                .toFile();

        Cache cache = new Cache(false);
        LicenseDownloadResult firstResult = LicenseDownloadResult.success(first, "sha1-first", false);
        cache.put("https://first.example/license", firstResult);
        LicenseDownloadResult secondResult = LicenseDownloadResult.success(duplicate, "sha1-second", false);

        LicenseDownloadResult result = new LicenseFileDeduplicator(cache, mock(Logger.class))
                .reuseFileWithSameContent(secondResult, "https://second.example/license", "2.0");

        assertSame(first, result.getFile());
        assertFalse(duplicate.exists());
    }

    @Test
    void bundledTextsKeepTheirConfiguredNames() throws Exception {
        File first = Files.write(tempDir.resolve("text.txt"), "GPL text".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File second = Files.write(tempDir.resolve("page.txt"), "gpl text".getBytes(StandardCharsets.UTF_8))
                .toFile();

        Cache cache = new Cache(false);
        cache.put("https://first.example/license", LicenseDownloadResult.bundled(first, "sha1-a", false));
        LicenseDownloadResult bundled = LicenseDownloadResult.bundled(second, "sha1-b", false);

        LicenseDownloadResult result = new LicenseFileDeduplicator(cache, mock(Logger.class))
                .reuseFileWithSameContent(bundled, "https://second.example/page", "GPL page");

        assertEquals(second, result.getFile());
    }

    @Test
    void distinctContentIsKept() throws Exception {
        File first = Files.write(tempDir.resolve("a.txt"), "license A".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File second = Files.write(tempDir.resolve("b.txt"), "license B".getBytes(StandardCharsets.UTF_8))
                .toFile();

        Cache cache = new Cache(false);
        cache.put("https://first.example/license", LicenseDownloadResult.success(first, "sha1-a", false));
        LicenseDownloadResult secondResult = LicenseDownloadResult.success(second, "sha1-b", false);

        LicenseDownloadResult result = new LicenseFileDeduplicator(cache, mock(Logger.class))
                .reuseFileWithSameContent(secondResult, "https://second.example/license", "B");

        assertEquals(second, result.getFile());
    }

    @Test
    void writesNothingToTheConsole() throws Exception {
        File first = Files.write(tempDir.resolve("a.txt"), "Apache License".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File second = Files.write(tempDir.resolve("b.txt"), "apache license".getBytes(StandardCharsets.UTF_8))
                .toFile();
        Cache cache = new Cache(false);
        cache.put("https://first.example/license", LicenseDownloadResult.success(first, "sha1-a", false));
        LicenseFileDeduplicator deduplicator = new LicenseFileDeduplicator(cache, mock(Logger.class));

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true));
            System.setErr(new PrintStream(err, true));
            deduplicator.reuseFileWithSameContent(
                    LicenseDownloadResult.success(second, "sha1-b", false), "https://second.example/license", "B");
            deduplicator.reuseFileWithSameContent(
                    LicenseDownloadResult.bundled(first, "sha1-c", false), "https://third.example/license", "C");
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        assertTrue(out.size() == 0 && err.size() == 0, "unexpected console output: " + out + err);
    }
}
