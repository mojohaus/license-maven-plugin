package org.codehaus.mojo.license;

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

import java.util.Comparator;

import org.codehaus.mojo.license.AbstractDownloadLicensesMojo.OrderBy;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;

/**
 * Comparators behind the {@code orderBy} parameter of the license download goals.
 *
 * <p>Every comparator falls back to the group id, artifact id and version, so that dependencies which compare equal
 * on the chosen criterion still come out in a stable order.
 *
 * @since 2.8.0
 */
final class ProjectLicenseInfoComparators {

    private static final Comparator<String> TEXT = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    private static final Comparator<ProjectLicenseInfo> GAV = Comparator.comparing(ProjectLicenseInfo::getGroupId, TEXT)
            .thenComparing(ProjectLicenseInfo::getArtifactId, TEXT)
            .thenComparing(ProjectLicenseInfo::getVersion, TEXT);

    private ProjectLicenseInfoComparators() {}

    /**
     * Returns the comparator for the given order.
     *
     * @param orderBy the requested order, may be {@code null}
     * @return the comparator, or {@code null} if the dependencies should be left in the order they were resolved in
     */
    static Comparator<ProjectLicenseInfo> of(OrderBy orderBy) {
        if (orderBy == null) {
            return null;
        }
        switch (orderBy) {
            case none:
                return null;
            case dependencyName:
                return Comparator.comparing(ProjectLicenseInfoComparators::name, TEXT)
                        .thenComparing(GAV);
            case dependencyGav:
                return GAV;
            case licenseName:
                return Comparator.comparing(ProjectLicenseInfoComparators::firstLicenseName, TEXT)
                        .thenComparing(GAV);
            default:
                throw new IllegalArgumentException("Unexpected " + OrderBy.class.getName() + ": " + orderBy);
        }
    }

    /**
     * The name a dependency gives itself in its POM. Only available with {@code extendedInfo}, so this falls back to
     * the artifact id rather than lumping every dependency together under no name at all.
     */
    private static String name(ProjectLicenseInfo info) {
        final ExtendedInfo extendedInfo = info.getExtendedInfo();
        final String name = extendedInfo != null ? extendedInfo.getName() : null;
        return name != null ? name : info.getArtifactId();
    }

    /**
     * The alphabetically first license of a dependency, so that the order does not depend on how the licenses happen
     * to be listed in the POM. Dependencies without a license sort last.
     */
    private static String firstLicenseName(ProjectLicenseInfo info) {
        String first = null;
        for (ProjectLicense license : info.getLicenses()) {
            final String name = license.getName();
            if (name != null && (first == null || TEXT.compare(name, first) < 0)) {
                first = name;
            }
        }
        return first;
    }
}
