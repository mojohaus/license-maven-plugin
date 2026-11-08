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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.LicenseMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyFileProjectTest {

    private static final String TEMPLATE = "/project-in-template.ftl";

    @TempDir
    Path tempDir;

    @Test
    void templateSeesTheProject() throws Exception {
        final MavenProject project = new MavenProject();
        project.setGroupId("com.example");
        project.setArtifactId("my-project");
        project.setVersion("1.2.3");
        project.setName("MyProject");
        final File file = tempDir.resolve("THIRD-PARTY.txt").toFile();

        new DefaultThirdPartyTool(null, null, null)
                .writeThirdPartyFile(new LicenseMap(), file, false, "UTF-8", TEMPLATE, project);

        assertEquals(
                "Dependencies of MyProject (com.example:my-project:1.2.3):",
                new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim());
    }

    @Test
    void signatureWithoutProjectStillWorksForTemplatesNotUsingIt() throws Exception {
        final File file = tempDir.resolve("THIRD-PARTY.txt").toFile();

        new DefaultThirdPartyTool(null, null, null)
                .writeThirdPartyFile(
                        new LicenseMap(), file, false, "UTF-8", "/org/codehaus/mojo/license/third-party-file.ftl");

        assertTrue(file.isFile());
    }
}
