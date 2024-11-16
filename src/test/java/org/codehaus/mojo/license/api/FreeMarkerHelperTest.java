package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 Codehaus, Tony Chemit
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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.LicenseMap;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link FreeMarkerHelper} and given templates.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.1
 */
class FreeMarkerHelperTest {
    private static final Logger LOG = LoggerFactory.getLogger(FreeMarkerHelperTest.class);

    @Test
    void testRenderTemplateForThirdPartyFile() throws Exception {

        FreeMarkerHelper helper = FreeMarkerHelper.newDefaultHelper();

        LicenseMap licenseMap = new LicenseMap();

        MavenProject deps = new MavenProject();
        deps.setArtifact(new DefaultArtifact(
                "groupId",
                "artifactId",
                VersionRange.createFromVersionSpec("0"),
                "compile",
                "type",
                "classifier",
                null));
        deps.setGroupId("groupId");
        deps.setArtifactId("artifactId");
        deps.setVersion("version");
        deps.setUrl("url");
        MavenProject deps2 = new MavenProject();
        deps2.setArtifact(new DefaultArtifact(
                "groupId2",
                "artifactId2",
                VersionRange.createFromVersionSpec("2"),
                "compile",
                "type",
                "classifier",
                null));
        deps2.setGroupId("groupId2");
        deps2.setArtifactId("artifactId2");
        deps2.setVersion("version2");
        licenseMap.put("license 1", deps);
        licenseMap.put("license 1", deps2);
        licenseMap.put("license 2", deps2);
        Map<String, Object> properties = new HashMap<>();
        properties.put("licenseMap", licenseMap.entrySet());
        properties.put("dependencyMap", licenseMap.toDependencyMap().entrySet());

        String s = helper.renderTemplate("/org/codehaus/mojo/license/third-party-file.ftl", properties);
        LOG.info("{}", s);
    }

    @Test
    void testRenderTemplateForThirdPartyFileGroupByLicense() throws Exception {

        FreeMarkerHelper helper = FreeMarkerHelper.newDefaultHelper();

        LicenseMap licenseMap = new LicenseMap();

        MavenProject deps = new MavenProject();
        deps.setArtifact(new DefaultArtifact(
                "groupId",
                "artifactId",
                VersionRange.createFromVersionSpec("0"),
                "compile",
                "type",
                "classifier",
                null));
        deps.setGroupId("groupId");
        deps.setArtifactId("artifactId");
        deps.setVersion("version");
        MavenProject deps2 = new MavenProject();
        deps2.setArtifact(new DefaultArtifact(
                "groupId2",
                "artifactId2",
                VersionRange.createFromVersionSpec("2"),
                "compile",
                "type",
                "classifier",
                null));
        deps2.setGroupId("groupId2");
        deps2.setArtifactId("artifactId2");
        deps2.setVersion("version2");
        deps2.setUrl("url2");
        licenseMap.put("license 1", deps);
        licenseMap.put("license 1", deps2);
        licenseMap.put("license 2", deps2);
        Map<String, Object> properties = new HashMap<>();
        properties.put("licenseMap", licenseMap.entrySet());
        properties.put("dependencyMap", licenseMap.toDependencyMap().entrySet());

        String s = helper.renderTemplate("/org/codehaus/mojo/license/third-party-file-groupByLicense.ftl", properties);
        LOG.info("{}", s);
    }

    @Test
    void testRenderTemplateForUpdateFileHeader() throws Exception {

        FreeMarkerHelper helper = FreeMarkerHelper.newDefaultHelper();

        MavenProject project = new MavenProject();
        project.setArtifact(new DefaultArtifact(
                "groupId",
                "artifactId",
                VersionRange.createFromVersionSpec("0"),
                "compile",
                "type",
                "classifier",
                null));
        project.setGroupId("groupId");
        project.setArtifactId("artifactId");
        project.setVersion("version");

        Map<String, Object> properties = new HashMap<>();
        properties.put("project", project);
        properties.put("projectName", "projectName");
        properties.put("inceptionYear", "inceptionYear");
        properties.put("organizationName", "organizationName");
        properties.put("addSvnKeyWords", true);

        String s = helper.renderTemplate("/org/codehaus/mojo/license/default-file-header-description.ftl", properties);
        assertEquals("projectName\n$Id:$\n$HeadURL:$", s);
        LOG.info("{}", s);
    }
}
