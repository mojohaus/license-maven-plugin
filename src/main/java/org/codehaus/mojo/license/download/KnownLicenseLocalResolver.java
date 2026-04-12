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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Resolves selected well-known license URLs to local resources bundled in the plugin.
 */
public final class KnownLicenseLocalResolver {

    private KnownLicenseLocalResolver() {}

    /**
     * @param licenseUrl URL from dependency POM
     * @return classpath resource path under {@code META-INF/licenses} or {@code null} if no local fallback applies
     */
    public static String resolveResourcePath(String licenseUrl) {
        if (licenseUrl == null || licenseUrl.isEmpty()) {
            return null;
        }
        final URI uri;
        try {
            uri = new URI(licenseUrl);
        } catch (URISyntaxException e) {
            return null;
        }

        final String host = uri.getHost();
        if (host == null) {
            return null;
        }
        final String hostLower = host.toLowerCase(Locale.ROOT);
        if (!"gnu.org".equals(hostLower) && !hostLower.endsWith(".gnu.org")) {
            return null;
        }

        final String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }
        final String p = path.toLowerCase(Locale.ROOT);

        if (p.contains("agpl") && p.contains("3")) {
            return "META-INF/licenses/agpl_v3/license.txt";
        }
        if (p.contains("lgpl") && (p.contains("2.1") || p.contains("2_1") || p.contains("2-1"))) {
            return "META-INF/licenses/lgpl_v2_1/license.txt";
        }
        if (p.contains("lgpl") && p.contains("3")) {
            return "META-INF/licenses/lgpl_v3/license.txt";
        }
        if (p.contains("gpl") && p.contains("1")) {
            return "META-INF/licenses/gpl_v1/license.txt";
        }
        if (p.contains("gpl") && p.contains("2")) {
            return "META-INF/licenses/gpl_v2/license.txt";
        }
        if (p.contains("gpl") && p.contains("3")) {
            return "META-INF/licenses/gpl_v3/license.txt";
        }
        if (p.contains("fdl") && p.contains("1.3")) {
            return "META-INF/licenses/fdl_v1_3/license.txt";
        }
        return null;
    }
}
