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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.codehaus.mojo.license.extended.ExtendedInfo;
import org.codehaus.mojo.license.extended.InfoFile;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicensedArtifactTest {

    @TempDir
    File tempDir;

    /** Dependencies are not always archives; a {@code pom} or native dependency must not lose its extended info. */
    @Test
    void extendedInfoOfNonArchiveArtifact() throws IOException {
        final File file = new File(tempDir, "test-1.0.pom");
        Files.write(file.toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));

        final ExtendedInfo extendedInfo = extendedInfoOf(file, "pom");

        assertNotNull(extendedInfo);
        assertTrue(extendedInfo.getInfoFiles().isEmpty());
    }

    @Test
    void extendedInfoOfJarArtifact() throws IOException {
        final File file = new File(tempDir, "test-1.0.jar");
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "The Test Project");
        try (OutputStream out = Files.newOutputStream(file.toPath());
                JarOutputStream jar = new JarOutputStream(out, manifest)) {
            jar.putNextEntry(new JarEntry("META-INF/LICENSE.txt"));
            jar.write("Apache License, Version 2.0".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        final ExtendedInfo extendedInfo = extendedInfoOf(file, "jar");

        assertNotNull(extendedInfo);
        assertEquals("The Test Project", extendedInfo.getImplementationVendor());
        assertEquals(1, extendedInfo.getInfoFiles().size());
        final InfoFile infoFile = extendedInfo.getInfoFiles().get(0);
        assertEquals("META-INF/LICENSE.txt", infoFile.getFileName());
        assertEquals(InfoFile.Type.LICENSE, infoFile.getType());
    }

    @NonNull
    private static ExtendedInfo extendedInfoOf(File file, String type) {
        final Artifact artifact =
                new DefaultArtifact("org.test", "test", "1.0", "compile", type, null, new DefaultArtifactHandler(type));
        artifact.setFile(file);
        final ExtendedInfo extendedInfo =
                new LicensedArtifact.Builder(artifact, true).build().getExtendedInfos();
        assertNotNull(extendedInfo);
        assertSame(artifact, extendedInfo.getArtifact());
        return extendedInfo;
    }
}
