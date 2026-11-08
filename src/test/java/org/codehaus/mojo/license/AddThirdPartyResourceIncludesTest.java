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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AddThirdPartyResourceIncludesTest {

    @Test
    void defaultsKeepTxtOnly() {
        assertArrayEquals(
                new String[] {"**/*.txt"},
                AddThirdPartyMojo.resourceIncludes("THIRD-PARTY.txt", true, "META-INF/x-THIRD-PARTY.txt"));
    }

    @Test
    void includesFileNameWithoutTxtExtension() {
        assertArrayEquals(
                new String[] {"**/*.txt", "THIRD-PARTY.LICENSE"},
                AddThirdPartyMojo.resourceIncludes("THIRD-PARTY.LICENSE", false, "META-INF/x-THIRD-PARTY.txt"));
    }

    @Test
    void includesBundlePathOnlyWhenBundleIsGenerated() {
        assertArrayEquals(
                new String[] {"**/*.txt", "META-INF/third-party.LICENSE"},
                AddThirdPartyMojo.resourceIncludes("THIRD-PARTY.txt", true, "META-INF/third-party.LICENSE"));
        assertArrayEquals(
                new String[] {"**/*.txt"},
                AddThirdPartyMojo.resourceIncludes("THIRD-PARTY.txt", false, "META-INF/third-party.LICENSE"));
    }
}
