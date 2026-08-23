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

import java.util.Arrays;
import java.util.Collections;

import org.codehaus.mojo.license.download.LicenseClassifier.LicenseMatch;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseClassifierTest {

    private static final LicenseClassifier CLASSIFIER = new LicenseClassifier(
            Arrays.asList("GPL 3.0", " AGPL 3.0 "),
            Collections.singletonList("EPL 2.0"),
            Arrays.asList("Apache License, Version 2.0", null, "  "));

    @Test
    void classifiesByList() {
        assertEquals(LicenseMatch.forbidden, CLASSIFIER.classify(license("GPL 3.0")));
        assertEquals(LicenseMatch.problematic, CLASSIFIER.classify(license("EPL 2.0")));
        assertEquals(LicenseMatch.ok, CLASSIFIER.classify(license("Apache License, Version 2.0")));
        assertEquals(LicenseMatch.unknown, CLASSIFIER.classify(license("Some House License")));
    }

    @Test
    void ignoresCaseAndSurroundingWhitespace() {
        assertEquals(LicenseMatch.forbidden, CLASSIFIER.classify(license("  gpl 3.0")));
        assertEquals(LicenseMatch.forbidden, CLASSIFIER.classify(license("AGPL 3.0")));
        assertEquals(LicenseMatch.ok, CLASSIFIER.classify(license("APACHE LICENSE, VERSION 2.0 ")));
    }

    @Test
    void treatsMissingLicensesAsUnknown() {
        assertEquals(LicenseMatch.unknown, CLASSIFIER.classify((ProjectLicense) null));
        assertEquals(LicenseMatch.unknown, CLASSIFIER.classify(license(null)));
        assertEquals(LicenseMatch.unknown, CLASSIFIER.classify(dependency()));
    }

    @Test
    void classifiesADependencyByItsMostWorryingLicense() {
        assertEquals(LicenseMatch.forbidden, CLASSIFIER.classify(dependency("Apache License, Version 2.0", "GPL 3.0")));
        assertEquals(LicenseMatch.problematic, CLASSIFIER.classify(dependency("EPL 2.0", "Some House License")));
        assertEquals(
                LicenseMatch.ok, CLASSIFIER.classify(dependency("Some House License", "Apache License, Version 2.0")));
    }

    @Test
    void isEmptyWithoutAnyConfiguredName() {
        assertTrue(new LicenseClassifier(null, null, null).isEmpty());
        assertTrue(new LicenseClassifier(Collections.emptyList(), null, Arrays.asList(null, " ")).isEmpty());
        assertFalse(CLASSIFIER.isEmpty());
    }

    private static ProjectLicense license(String name) {
        return new ProjectLicense(name, null, null, null, null);
    }

    private static ProjectLicenseInfo dependency(String... licenseNames) {
        final ProjectLicenseInfo info = new ProjectLicenseInfo("org.test", "test", "1.0", (ExtendedInfo) null);
        for (String licenseName : licenseNames) {
            info.addLicense(license(licenseName));
        }
        return info;
    }
}
