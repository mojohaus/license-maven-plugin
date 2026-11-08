package org.codehaus.mojo.license.api;

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

import java.util.Collections;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultThirdPartyHelperTest {

    @Test
    void artifactCacheBelongsToTheHelper() {
        final DefaultThirdPartyHelper first = helper();
        final DefaultThirdPartyHelper second = helper();

        first.getArtifactCache().put("org.test--a--1.0", new MavenProject());

        assertNotSame(first.getArtifactCache(), second.getArtifactCache());
        assertTrue(second.getArtifactCache().isEmpty());
    }

    @Test
    void artifactCacheIsStableWithinOneHelper() {
        final DefaultThirdPartyHelper helper = helper();
        helper.getArtifactCache().put("org.test--a--1.0", new MavenProject());

        assertSame(helper.getArtifactCache(), helper.getArtifactCache());
        assertEquals(1, helper.getArtifactCache().size());
    }

    private static DefaultThirdPartyHelper helper() {
        return new DefaultThirdPartyHelper(
                new MavenProject(),
                "UTF-8",
                false,
                mock(DependenciesTool.class),
                mock(ThirdPartyTool.class),
                Collections.emptyList(),
                Collections.emptyList());
    }
}
