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
import org.codehaus.mojo.license.download.ProjectLicense;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailOnMissingLicenseFileTest {

    @Test
    void findsDependenciesWithoutAnyFile() {
        final ProjectLicenseInfo withFile = dependency("a", "with-file", "LICENSE.txt");
        final ProjectLicenseInfo withoutFile = dependency("b", "without-file", null);
        final ProjectLicenseInfo noLicense = new ProjectLicenseInfo("c", "no-license", "1.0", false);
        final ProjectLicenseInfo approved = dependency("d", "approved", null);
        approved.setApproved(true);

        final ProjectLicenseInfo oneOfTwo = dependency("e", "one-of-two", null);
        final ProjectLicense second = new ProjectLicense();
        second.setFile("second.txt");
        oneOfTwo.addLicense(second);

        assertEquals(
                Arrays.asList("b:without-file:1.0", "c:no-license:1.0"),
                AbstractDownloadLicensesMojo.findWithoutLicenseFile(
                        Arrays.asList(withFile, withoutFile, noLicense, approved, oneOfTwo)));
    }

    @Test
    void failsNamingEveryDependency() {
        final MojoFailureException e = assertThrows(
                MojoFailureException.class,
                () -> AbstractDownloadLicensesMojo.failOnMissingLicenseFile(
                        true, Arrays.asList("b:without-file:1.0", "c:no-license:1.0")));
        assertTrue(e.getMessage().contains("b:without-file:1.0, c:no-license:1.0"), e.getMessage());
    }

    @Test
    void passesWhenDisabledOrComplete() throws Exception {
        final List<String> missing = Collections.singletonList("b:without-file:1.0");
        AbstractDownloadLicensesMojo.failOnMissingLicenseFile(false, missing);
        AbstractDownloadLicensesMojo.failOnMissingLicenseFile(true, Collections.emptyList());
    }

    private static ProjectLicenseInfo dependency(String groupId, String artifactId, String file) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo(groupId, artifactId, "1.0", false);
        final ProjectLicense license = new ProjectLicense();
        license.setName("BSD");
        license.setFile(file);
        info.addLicense(license);
        return info;
    }
}
