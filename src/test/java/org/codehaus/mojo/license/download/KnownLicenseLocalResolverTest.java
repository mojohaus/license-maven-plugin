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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnownLicenseLocalResolverTest {

    @Test
    void resolvesGplV2() {
        assertEquals(
                "META-INF/licenses/gpl_v2/license.txt",
                KnownLicenseLocalResolver.resolveResourcePath("https://www.gnu.org/licenses/gpl-2.0.txt"));
    }

    @Test
    void resolvesLgplV3() {
        assertEquals(
                "META-INF/licenses/lgpl_v3/license.txt",
                KnownLicenseLocalResolver.resolveResourcePath("http://www.gnu.org/licenses/lgpl-3.0.html"));
    }

    @Test
    void resolvesAgplV3() {
        assertEquals(
                "META-INF/licenses/agpl_v3/license.txt",
                KnownLicenseLocalResolver.resolveResourcePath("https://www.gnu.org/licenses/agpl-3.0.en.html"));
    }

    @Test
    void resolvesOldLicensePath() {
        assertEquals(
                "META-INF/licenses/lgpl_v2_1/license.txt",
                KnownLicenseLocalResolver.resolveResourcePath("https://www.gnu.org/licenses/old-licenses/lgpl-2.1"));
    }

    @Test
    void ignoresOtherHosts() {
        assertNull(KnownLicenseLocalResolver.resolveResourcePath("https://opensource.org/licenses/MIT"));
    }
}
