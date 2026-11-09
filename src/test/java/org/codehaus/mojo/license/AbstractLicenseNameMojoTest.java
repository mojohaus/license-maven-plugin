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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AbstractLicenseNameMojoTest {

    @TempDir
    Path projectDir;

    @Test
    void excludesDeclaredModulesWhenScanningProjectRoot() throws IOException {
        Files.createDirectories(projectDir.resolve("child"));
        Files.createDirectories(projectDir.resolve("src"));
        Files.createDirectories(projectDir.resolve("target"));
        Files.write(projectDir.resolve("pom.xml"), new byte[0]);
        Files.write(projectDir.resolve("child/pom.xml"), new byte[0]);
        Files.write(projectDir.resolve("src/Example.java"), new byte[0]);
        Files.write(projectDir.resolve("target/generated.java"), new byte[0]);

        MavenProject project = new MavenProject();
        project.setFile(projectDir.resolve("pom.xml").toFile());
        project.getModel().setModules(Arrays.asList("child"));

        TestMojo mojo = new TestMojo();
        mojo.project = project;

        Map<File, String[]> files = new HashMap<>();
        mojo.getFilesToTreatForRoots(
                new String[] {"**/*"}, new String[] {"target/**"}, Arrays.asList(projectDir.toString()), files);

        List<String> included = Arrays.asList(files.get(projectDir.toFile()));
        assertTrue(included.contains("pom.xml"));
        assertTrue(included.contains("src/Example.java"));
        assertFalse(included.contains("child/pom.xml"));
        assertFalse(included.contains("target/generated.java"));
    }

    private static class TestMojo extends AbstractLicenseNameMojo {

        TestMojo() {
            super(mock(MavenProjectHelper.class));
        }

        @Override
        public boolean isSkip() {
            return false;
        }

        @Override
        protected void init() {}

        @Override
        protected void doAction() {}
    }
}
