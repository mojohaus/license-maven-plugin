package org.codehaus.mojo.license.utils;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringToListTest {

    @Test
    void splitsOnThePipe() throws MojoExecutionException {
        assertEquals(
                Arrays.asList("Apache License 2.0", "MIT License"),
                new StringToList("Apache License 2.0|MIT License").getData());
    }

    @Test
    void dropsTheWhitespaceAnXmlFormatterLeavesBehind() throws MojoExecutionException {
        assertEquals(
                Arrays.asList("Apache License 2.0", "MIT License", "EPL 2.0"),
                new StringToList("\n    Apache License\n    2.0\n    |MIT License\n    | EPL 2.0\n  ").getData());
    }

    @Test
    void ignoresEmptyEntries() throws MojoExecutionException {
        // An empty list used to hold one entry, the empty string, which matches no license.
        assertEquals(0, new StringToList("").getData().size());
        assertEquals(0, new StringToList("   ").getData().size());
        assertEquals(Collections.singletonList("MIT License"), new StringToList("|MIT License|").getData());
    }
}
