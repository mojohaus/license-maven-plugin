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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.mojo.license.AbstractDownloadLicensesMojo.OrderBy;
import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectLicenseInfoComparatorsTest {

    private static final LicenseClassifier NO_LICENSES_CLASSIFIED = new LicenseClassifier(null, null, null);

    @Test
    void noneLeavesTheOrderAlone() {
        assertNull(ProjectLicenseInfoComparators.of(OrderBy.none, NO_LICENSES_CLASSIFIED));
        assertNull(ProjectLicenseInfoComparators.of(null, NO_LICENSES_CLASSIFIED));
    }

    @Test
    void byLicenseMatch() {
        final LicenseClassifier classifier = new LicenseClassifier(
                Arrays.asList("GPL 3.0"), Arrays.asList("EPL 2.0"), Arrays.asList("Apache License, Version 2.0"));
        final List<ProjectLicenseInfo> input = Arrays.asList(
                dependency("org.test", "unknown", "1.0", "Some House License"),
                dependency("org.test", "ok", "1.0", "Apache License, Version 2.0"),
                dependency("org.test", "unlicensed", "1.0"),
                dependency("org.test", "problematic", "1.0", "EPL 2.0"),
                dependency("org.test", "forbidden", "1.0", "GPL 3.0"),
                dependency("org.test", "both", "1.0", "Apache License, Version 2.0", "GPL 3.0"));

        final List<ProjectLicenseInfo> actual = new ArrayList<>(input);
        actual.sort(ProjectLicenseInfoComparators.of(OrderBy.licenseMatch, classifier));

        assertEquals(
                Arrays.asList(
                        "org.test:both:1.0",
                        "org.test:forbidden:1.0",
                        "org.test:problematic:1.0",
                        "org.test:ok:1.0",
                        "org.test:unknown:1.0",
                        "org.test:unlicensed:1.0"),
                actual.stream().map(ProjectLicenseInfo::toGavString).collect(Collectors.toList()));
    }

    @Test
    void byGav() {
        assertOrder(
                OrderBy.dependencyGav,
                Arrays.asList(
                        dependency("org.b", "one", "1.0"),
                        dependency("org.a", "two", "1.0"),
                        dependency("org.a", "one", "2.0"),
                        dependency("org.a", "one", "1.0")),
                "org.a:one:1.0",
                "org.a:one:2.0",
                "org.a:two:1.0",
                "org.b:one:1.0");
    }

    @Test
    void byNameFallsBackToArtifactIdWithoutExtendedInfo() {
        assertOrder(
                OrderBy.dependencyName,
                Arrays.asList(
                        named("org.test", "zzz", "Apache Commons IO"),
                        dependency("org.test", "guava", "1.0"),
                        named("org.test", "aaa", "SLF4J API")),
                "org.test:zzz:1.0",
                "org.test:guava:1.0",
                "org.test:aaa:1.0");
    }

    @Test
    void byNameIsCaseInsensitiveAndBreaksTiesByGav() {
        assertOrder(
                OrderBy.dependencyName,
                Arrays.asList(
                        named("org.test", "second", "commons-io"),
                        named("org.test", "first", "Commons-IO"),
                        named("org.test", "third", "commons-lang")),
                "org.test:first:1.0",
                "org.test:second:1.0",
                "org.test:third:1.0");
    }

    @Test
    void byLicenseNameTakesTheAlphabeticallyFirstLicenseAndSortsUnlicensedLast() {
        assertOrder(
                OrderBy.licenseName,
                Arrays.asList(
                        dependency("org.test", "unlicensed", "1.0"),
                        dependency("org.test", "mit", "1.0", "MIT License"),
                        dependency("org.test", "dual", "1.0", "MIT License", "Apache License, Version 2.0"),
                        dependency("org.test", "epl", "1.0", "EPL 2.0")),
                "org.test:dual:1.0",
                "org.test:epl:1.0",
                "org.test:mit:1.0",
                "org.test:unlicensed:1.0");
    }

    @Test
    void byLicenseNameToleratesLicensesWithoutAName() {
        final ProjectLicenseInfo urlOnly = dependency("org.test", "url-only", "1.0");
        urlOnly.addLicense(new ProjectLicense(null, "https://example.org/license.txt", null, null, null));

        assertOrder(
                OrderBy.licenseName,
                Arrays.asList(urlOnly, dependency("org.test", "mit", "1.0", "MIT License")),
                "org.test:mit:1.0",
                "org.test:url-only:1.0");
    }

    private static void assertOrder(OrderBy orderBy, List<ProjectLicenseInfo> input, String... expected) {
        final Comparator<ProjectLicenseInfo> comparator =
                ProjectLicenseInfoComparators.of(orderBy, NO_LICENSES_CLASSIFIED);
        final List<ProjectLicenseInfo> actual = new ArrayList<>(input);
        actual.sort(comparator);
        assertEquals(
                Arrays.asList(expected),
                actual.stream().map(ProjectLicenseInfo::toGavString).collect(Collectors.toList()));
    }

    private static ProjectLicenseInfo named(String groupId, String artifactId, String name) {
        final ExtendedInfo extendedInfo = new ExtendedInfo();
        extendedInfo.setName(name);
        return new ProjectLicenseInfo(groupId, artifactId, "1.0", extendedInfo);
    }

    private static ProjectLicenseInfo dependency(
            String groupId, String artifactId, String version, String... licenseNames) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo(groupId, artifactId, version, (ExtendedInfo) null);
        for (String licenseName : licenseNames) {
            if (licenseName != null) {
                info.addLicense(new ProjectLicense(licenseName, null, null, null, null));
            }
        }
        return info;
    }
}
