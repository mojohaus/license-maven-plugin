package org.codehaus.mojo.license.extended;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Jan-Hendrik Diederich
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

import java.util.HashSet;
import java.util.Set;

import org.codehaus.mojo.license.download.LicenseDownloader;

/**
 * Information about a NOTICE or LICENSE file.
 */
public class InfoFile {
    /**
     * The type of the source for the info file.
     */
    public enum Type {
        /**
         * Generic ...NOTICE....txt file.
         */
        NOTICE,
        /**
         * Generic ...LICENSE...txt file.
         */
        LICENSE,
        /**
         * File name matches a SPDX license id.
         */
        SPDX_LICENSE
    }

    private String fileName;
    private String content;
    private Set<String> extractedCopyrightLines = new HashSet<>();
    private Type type;

    private String normalizedContent;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.normalizedContent = LicenseDownloader.calculateStringChecksum(content);
    }

    public Set<String> getExtractedCopyrightLines() {
        return extractedCopyrightLines;
    }

    public void setExtractedCopyrightLines(Set<String> extractedCopyrightLines) {
        this.extractedCopyrightLines = extractedCopyrightLines;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getNormalizedContent() {
        return normalizedContent;
    }
}
