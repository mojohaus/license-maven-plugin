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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailOnForbiddenLicenseTest {

    private static final LicenseClassifier CLASSIFIER = new LicenseClassifier(
            Arrays.asList("GPL 3.0"), Collections.emptyList(), Arrays.asList("Apache License, Version 2.0"));

    @Test
    void findsDependenciesByForbiddenLicenseName() {
        final List<ProjectLicenseInfo> dependencies = Arrays.asList(
                dependency("ok", "Apache License, Version 2.0"),
                dependency("bad", " gpl 3.0 "),
                dependency("dual", "Apache License, Version 2.0", "GPL 3.0"),
                dependency("unknown", "Some House License"),
                dependency("unlicensed"));

        assertEquals(
                Arrays.asList("org.test:bad:1.0 (gpl 3.0)", "org.test:dual:1.0 (GPL 3.0)"),
                AbstractDownloadLicensesMojo.findForbidden(dependencies, CLASSIFIER));
    }

    @Test
    void failsNamingEveryDependency() {
        final MojoFailureException e = assertThrows(
                MojoFailureException.class,
                () -> AbstractDownloadLicensesMojo.failOnForbidden(
                        Arrays.asList("a:b:1 (GPL 3.0)", "c:d:2 (GPL 3.0)")));
        assertTrue(e.getMessage().contains("a:b:1 (GPL 3.0); c:d:2 (GPL 3.0)"), e.getMessage());
    }

    @Test
    void passesWhenDisabledOrClean() throws Exception {
        AbstractDownloadLicensesMojo.failOnForbidden(null);
        AbstractDownloadLicensesMojo.failOnForbidden(Collections.emptyList());
    }

    private static ProjectLicenseInfo dependency(String artifactId, String... licenseNames) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", artifactId, "1.0", false);
        for (String name : licenseNames) {
            final ProjectLicense license = new ProjectLicense();
            license.setName(name);
            info.addLicense(license);
        }
        return info;
    }
}
