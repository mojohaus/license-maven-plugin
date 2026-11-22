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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs {@link UpdateFileHeaderMojo} on the projects of the former integration tests of the same names. The mojo gets
 * its parameters from the plugin descriptor, see {@link MojoTestSupport}.
 */
class UpdateFileHeaderMojoTest {

    @TempDir
    Path basedir;

    private void run(MavenProject project, String root, Map<String, Object> configuration) throws Exception {
        UpdateFileHeaderMojo mojo =
                new UpdateFileHeaderMojo(MojoTestSupport.transformers(), MojoTestSupport.projectHelper());
        Map<String, Object> parameters = new HashMap<>(configuration);
        parameters.putIfAbsent("roots", new String[] {basedir.resolve(root).toString()});
        MojoTestSupport.configure(mojo, "update-file-header", project, parameters);
        mojo.execute();
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(basedir.resolve(path)), StandardCharsets.UTF_8);
    }

    /** The project of the ISSUE-7, ISSUE-22 and ISSUE-38 tests: a license from a property and an inception year. */
    private MavenProject mojoHausProject(String name) {
        MavenProject project = MojoTestSupport.project(basedir, name, "1.0", "Test " + name, "MojoHaus", "2016");
        project.getProperties().setProperty("license.licenseName", "gpl_v3");
        return project;
    }

    @Test
    void issue7JavaHeaderStartsWithNoReformatTag() throws Exception {
        MojoTestSupport.copyFixture("ISSUE-7", basedir);

        run(mojoHausProject("ISSUE-7"), "src", new HashMap<>());

        assertTrue(read("src/main/java/org/codehaus/mojo/HelloWorld.java").contains("/*-"));
    }

    @Test
    void issue22HeaderGoesBelowTheDoctype() throws Exception {
        MojoTestSupport.copyFixture("ISSUE-22", basedir);

        run(mojoHausProject("ISSUE-22"), "src", new HashMap<>());

        String newLine = System.lineSeparator();
        assertTrue(read("src/main/resources/HelloWorld.html").contains("<!DOCTYPE html>" + newLine + "<!--"));
        assertTrue(read("src/main/resources/HelloWorld2.html")
                .contains("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" "
                        + "\"http://www.w3.org/TR/html4/strict.dtd\">" + newLine + "<!--"));
        assertTrue(read("src/main/resources/HelloWorld3.html")
                .contains("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" "
                        + "\"http://www.w3.org/TR/html4/loose.dtd\">" + newLine + "<!--"));
        assertTrue(read("src/main/resources/HelloWorld4.xhtml")
                .contains("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" + newLine + "<!--"));
    }

    @Test
    void issue38ExtraFileGetsPropertiesStyleHeader() throws Exception {
        MojoTestSupport.copyFixture("ISSUE-38", basedir);
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("extraFiles", Collections.singletonMap("DockerFile", "properties"));

        run(mojoHausProject("ISSUE-38"), "src", configuration);

        String content = read("src/main/docker/DockerFile");
        assertTrue(content.contains("# #%L"));
        assertTrue(content.contains("# Test ISSUE-38"));
        assertTrue(content.contains("License"));
        assertTrue(content.contains("# RUN TODO"));
    }

    @Test
    void issue166ScriptKeepsItsExecutableBitAndSoDoesItsBackup() throws Exception {
        assumeTrue(basedir.getFileSystem().supportedFileAttributeViews().contains("posix"));
        MojoTestSupport.copyFixture("ISSUE-166", basedir);
        Path script = basedir.resolve("src/main/bash/license-test.sh");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        MavenProject project =
                MojoTestSupport.project(basedir, "ISSUE-166", "1.0", "Test ISSUE-166", "MojoHaus", "2019");
        project.getProperties().setProperty("license.licenseName", "gpl_v3");
        project.getProperties().setProperty("license.keepBackup", "true");

        run(project, "src", new HashMap<>());

        Path backup = basedir.resolve("src/main/bash/license-test.sh~");
        assertTrue(read("src/main/bash/license-test.sh").contains("# #%L"), "No header was added to " + script);
        assertTrue(Files.isExecutable(script), "Executable bit of the file " + script + " was cleared");
        assertTrue(Files.isExecutable(backup), "Executable bit of the backup file " + backup + " was cleared");
    }

    /** The configuration of the MLICENSE-3, MLICENSE-27 and MLICENSE-30 tests. */
    private Map<String, Object> mlicense3Configuration() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("addSvnKeyWords", true);
        configuration.put("licenseName", "gpl_v3");
        configuration.put("inceptionYear", 2112);
        return configuration;
    }

    @Test
    void mlicense3UpdatesCopyrightAndKeepsIgnoredFiles() throws Exception {
        MojoTestSupport.copyFixture("MLICENSE-3", basedir);
        MavenProject project = MojoTestSupport.project(
                basedir, "MLICENSE-3", "1.0", "License Test :: MLICENSE-3", "License Test", null);
        Map<String, Object> configuration = mlicense3Configuration();
        configuration.put("canUpdateCopyright", true);

        run(project, "src", configuration);

        String dir = "src/main/java/org/codehaus/mojo/license/plugin/test/";
        assertTrue(read(dir + "MyBean.java").contains("Copyright (C) 2112 License Test"));

        String bean2 = read(dir + "MyBean2.java");
        assertFalse(bean2.contains("Copyright (C) 2010 Tony Update me"));
        assertTrue(bean2.contains("Copyright (C) 2112 License Test"));
        assertTrue(bean2.contains("do NOT update!"));
        assertFalse(bean2.contains("Fake to be removed!"));

        String bean3 = read(dir + "MyBean3.java");
        assertTrue(bean3.contains("%%Ignore-License"));
        assertTrue(bean3.contains("Copyright (C) 2000 Codelutin Do not update!"));
    }

    @Test
    void mlicense27UsesTheConfiguredProcessTags() throws Exception {
        MojoTestSupport.copyFixture("MLICENSE-27", basedir);
        MavenProject project = MojoTestSupport.project(
                basedir, "MLICENSE-27", "1.0", "License Test :: MLICENSE-27", "License Test", null);
        Map<String, Object> configuration = mlicense3Configuration();
        configuration.put("canUpdateCopyright", true);
        configuration.put("processStartTag", " ? # # #  ");
        configuration.put("processEndTag", " # # # ? ");
        configuration.put("sectionDelimiter", " # - # ");

        run(project, "src", configuration);

        String dir = "src/main/java/org/codehaus/mojo/license/test/";
        String bean = read(dir + "MyBean.java");
        assertTrue(bean.contains("Copyright (C) 2112 License Test"));
        assertTrue(bean.contains("?###"));
        assertTrue(bean.contains("###?"));
        assertTrue(bean.contains("#-#"));

        String bean2 = read(dir + "MyBean2.java");
        assertFalse(bean2.contains("Copyright (C) 2010 Tony Update me"));
        assertTrue(bean2.contains("Copyright (C) 2112 License Test"));
        assertTrue(bean2.contains("do NOT update!"));
        assertFalse(bean2.contains("Fake to be removed!"));
        assertTrue(bean2.contains("?###"));
        assertTrue(bean2.contains("###?"));
        assertTrue(bean2.contains("#-#"));

        String bean3 = read(dir + "MyBean3.java");
        assertTrue(bean3.contains("%%Ignore-License"));
        assertTrue(bean3.contains("Copyright (C) 2000 Codelutin Do not update!"));
        assertFalse(bean3.contains("?###"));
        assertFalse(bean3.contains("###?"));
        assertFalse(bean3.contains("#-#"));
    }

    @Test
    void mlicense30UpdatesTheDescriptionFromATemplate() throws Exception {
        MojoTestSupport.copyFixture("MLICENSE-30", basedir);
        MavenProject project = MojoTestSupport.project(
                basedir, "MLICENSE-30", "1.1", "License Test :: MLICENSE-30", "License Test", null);
        Map<String, Object> configuration = mlicense3Configuration();
        configuration.put("canUpdateDescription", true);
        configuration.put(
                "descriptionTemplate",
                basedir.resolve("src/main/license/descriptionTemplate.ftl").toString());

        run(project, "src", configuration);

        String dir = "src/main/java/org/codehaus/mojo/license/test/";
        String bean = read(dir + "MyBean.java");
        assertTrue(bean.contains("Copyright (C) 2112 License Test"));
        assertTrue(bean.contains("MyBean.java - License Test :: MLICENSE-30 - License Test - 2112"));
        assertTrue(bean.contains("org.codehaus.mojo.license.test-MLICENSE-30-1.1"));
        assertTrue(bean.contains("#%L"));
        assertTrue(bean.contains("%%"));
        assertTrue(bean.contains("#L%"));

        String bean2 = read(dir + "MyBean2.java");
        assertTrue(bean2.contains("Copyright (C) 2010 Tony Do not update!"));
        assertFalse(bean2.contains("License Test :: Will be updated!"));
        assertFalse(bean2.contains("Copyright (C) 2112 License Test"));
        assertTrue(bean2.contains("MyBean2.java - License Test :: MLICENSE-30 - License Test - 2112"));
        assertTrue(bean2.contains("org.codehaus.mojo.license.test-MLICENSE-30-1.1"));
        assertFalse(bean2.contains("Fake to be removed!"));
        assertTrue(bean2.contains("#%L"));
        assertTrue(bean2.contains("%%"));
        assertTrue(bean2.contains("#L%"));

        String bean3 = read(dir + "MyBean3.java");
        assertTrue(bean3.contains("%%Ignore-License"));
        assertTrue(bean3.contains("Copyright (C) 2000 Codelutin Do not update!"));
        assertFalse(bean3.contains("#%L"));
        assertFalse(bean3.contains("#L%"));
    }

    @Test
    void mlicense44UsesALicenseAndTemplateFromTheProject() throws Exception {
        MojoTestSupport.copyFixture("MLICENSE-44", basedir);
        MavenProject project = MojoTestSupport.project(basedir, "MLICENSE-44", "1.1", "Test MLICENSE-44", null, null);
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("encoding", "UTF-8");
        configuration.put("includes", new String[] {"**/*.java"});
        configuration.put("licenseName", "test");
        configuration.put("organizationName", "CodeHaus");
        configuration.put(
                "licenseResolver", basedir.resolve("src/license").toUri().toString());
        configuration.put(
                "descriptionTemplate",
                basedir.resolve("src/license/test/descriptionTemplate.ftl").toString());
        configuration.put("inceptionYear", 2010);

        run(project, "src", configuration);

        for (String name : new String[] {"File1", "File2", "File3", "File4"}) {
            String content = read("src/main/java/test/" + name + ".java");
            assertTrue(content.contains("Name        : " + name + ".java"));
            assertTrue(content.contains("Part of     : MLICENSE-44"));
            assertTrue(content.contains("Description : Test MLICENSE-44"));
        }
    }

    @Test
    void issue347SecondRunKeepsTheBlankLinesOfTheLicense() throws Exception {
        MojoTestSupport.copyFixture("ISSUE-347", basedir);
        MavenProject project = MojoTestSupport.project(
                basedir, "issue-347", "1.0", "License Test :: ISSUE-347", "License Test", "2012");
        Map<String, Object> first = new HashMap<>();
        first.put("licenseName", "gpl_v3");
        first.put("trimHeaderLine", true);
        first.put("projectName", "First name");
        Map<String, Object> second = new HashMap<>(first);
        second.put("projectName", "Second name");
        second.put("canUpdateDescription", true);

        run(project, "src/files", first);
        run(project, "src/files", second);

        String content = read("src/files/test.html").replace("\r\n", "\n");
        assertTrue(content.startsWith("<!--"));
        assertEquals(1, content.split("#%L", -1).length - 1);
        assertTrue(content.contains("Second name"));
        assertFalse(content.contains("First name"));
        assertTrue(content.contains("Copyright (C)"));
        assertTrue(content.contains("any later version.\n\n  This program is distributed"));
        assertTrue(content.contains("<p>Test</p>"));
    }
}
