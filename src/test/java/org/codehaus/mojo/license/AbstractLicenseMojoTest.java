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

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractLicenseMojoTest {

    private static final File BASEDIR = new File("target/basedir").getAbsoluteFile();

    private static final File DEFAULT = new File(BASEDIR, "LICENSE.txt");

    @Test
    void legacyPropertyIsHonouredWhileTheParameterIsDefault() {
        assertEquals(
                new File(BASEDIR, "legacy/LICENSE.md"),
                AbstractLicenseMojo.resolveLicenseFile(DEFAULT, BASEDIR, "legacy/LICENSE.md"));

        File absolute = new File(BASEDIR, "elsewhere/LICENSE").getAbsoluteFile();
        assertEquals(absolute, AbstractLicenseMojo.resolveLicenseFile(DEFAULT, BASEDIR, absolute.getPath()));
    }

    @Test
    void explicitValueWinsOverLegacyProperty() {
        File configured = new File(BASEDIR, "configured.txt");

        assertEquals(configured, AbstractLicenseMojo.resolveLicenseFile(configured, BASEDIR, "legacy.txt"));
    }

    @Test
    void parameterIsUntouchedWithoutLegacyProperty() {
        assertEquals(DEFAULT, AbstractLicenseMojo.resolveLicenseFile(DEFAULT, BASEDIR, null));
        assertEquals(DEFAULT, AbstractLicenseMojo.resolveLicenseFile(DEFAULT, BASEDIR, " "));
    }
}
