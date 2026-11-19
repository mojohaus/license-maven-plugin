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

import org.codehaus.mojo.license.download.LicenseDownloader.LicenseDownloadResult;
import org.slf4j.Logger;

/**
 * Reuses a previously downloaded license file when a different license name yields byte-identical text apart from
 * formatting. The report keeps the license name; only one file is written to the output directory.
 *
 * @since 2.9.1
 */
public final class LicenseFileDeduplicator {

    private final Cache cache;

    private final Logger log;

    public LicenseFileDeduplicator(Cache cache, Logger log) {
        this.cache = cache;
        this.log = log;
    }

    /**
     * Returns a result pointing at the existing file when the same normalized text was seen before.
     *
     * @param result      the freshly downloaded result
     * @param licenseUrl  the URL it came from, for the log message
     * @param licenseName the name in the POM, kept in the report
     * @return the original result, or one bound to the existing file
     */
    public LicenseDownloadResult reuseFileWithSameContent(
            LicenseDownloadResult result, String licenseUrl, String licenseName) {
        if (!result.isSuccess() || result.getNormalizedContentChecksum() == null) {
            return result;
        }
        LicenseDownloadResult existing = cache.getByNormalizedContentChecksum(result.getNormalizedContentChecksum());
        if (existing == null || existing.getFile().equals(result.getFile())) {
            return result;
        }
        File duplicate = result.getFile();
        if (!duplicate.delete() && duplicate.exists()) {
            log.debug("Could not delete duplicate license file {}", duplicate);
            return result;
        }
        log.info(
                "License '{}' has the same text as an earlier download from {}; reusing {} instead of keeping {}",
                licenseName,
                licenseUrl,
                existing.getFile().getName(),
                duplicate.getName());
        return result.withFile(existing.getFile());
    }
}
