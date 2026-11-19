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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CacheContentDedupTest {

    @TempDir
    Path tempDir;

    @Test
    void findsTheFirstResultWithTheSameNormalizedContent() throws Exception {
        File first = Files.write(
                        tempDir.resolve("apache-2.txt"), "Apache License\nVersion 2.0".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File second = Files.write(
                        tempDir.resolve("2.0.txt"), "apache license version 2.0".getBytes(StandardCharsets.UTF_8))
                .toFile();

        Cache cache = new Cache(false);
        LicenseDownloadResult firstResult = LicenseDownloadResult.success(first, "sha1-first", false);
        cache.put("https://first.example/license", firstResult);

        LicenseDownloadResult secondResult = LicenseDownloadResult.success(second, "sha1-second", false);

        assertSame(firstResult, cache.getByNormalizedContentChecksum(secondResult.getNormalizedContentChecksum()));
        assertEquals(
                "Apache License\nVersion 2.0",
                new String(
                        Files.readAllBytes(
                                cache.getByNormalizedContentChecksum(secondResult.getNormalizedContentChecksum())
                                        .getFile()
                                        .toPath()),
                        StandardCharsets.UTF_8));
    }

    @Test
    void differentContentDoesNotMatch() throws Exception {
        File first = Files.write(tempDir.resolve("a.txt"), "license A".getBytes(StandardCharsets.UTF_8))
                .toFile();
        File second = Files.write(tempDir.resolve("b.txt"), "license B".getBytes(StandardCharsets.UTF_8))
                .toFile();

        Cache cache = new Cache(false);
        cache.put("https://first.example/license", LicenseDownloadResult.success(first, "sha1-a", false));
        LicenseDownloadResult secondResult = LicenseDownloadResult.success(second, "sha1-b", false);

        assertNull(cache.getByNormalizedContentChecksum(secondResult.getNormalizedContentChecksum()));
    }
}
