package org.codehaus.mojo.license.model;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2020 CodeLutin, Codehaus, Tony Chemit
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

import static org.junit.Assert.assertEquals;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

public class LicenseMapTest {
    // licenses must be equal
    private static final String LICENSE_KEY = LicenseMap.UNKNOWN_LICENSE_MESSAGE;
    // project names must be equal
    private static final String PROJECT_NAME = "SameName";
    // maven coordinates must differ
    private static final String ARTIFACT_ID_FIRST = "artifactId1";
    private static final String ARTIFACT_ID_SECOND = "artifactId2";

    //unit test for #361
    @Test
    public void licenseMapOrderByName() {
        LicenseMap licenseMap = new LicenseMap();
        licenseMap.put(LICENSE_KEY, projectForArtifact(PROJECT_NAME, ARTIFACT_ID_FIRST));
        licenseMap.put(LICENSE_KEY, projectForArtifact(PROJECT_NAME, ARTIFACT_ID_SECOND));
        LicenseMap orderedMap = licenseMap.toLicenseMapOrderByName();
        assertEquals(2, orderedMap.get(LICENSE_KEY).size());
    }

    //it's a re-assurance test for default unsorted behavior
    @Test
    public void licenseMapDefaultState() {
        LicenseMap licenseMap = new LicenseMap();
        licenseMap.put(LICENSE_KEY, projectForArtifact(PROJECT_NAME, ARTIFACT_ID_FIRST));
        licenseMap.put(LICENSE_KEY, projectForArtifact(PROJECT_NAME, ARTIFACT_ID_SECOND));
        assertEquals(2, licenseMap.get(LICENSE_KEY).size());
    }

    private MavenProject projectForArtifact(String projectName, String artifactId1) {
        MavenProject project = new MavenProject();
        project.setArtifact(new DefaultArtifact("groupId", artifactId1, "1.0.0", "compile", "jar", null, new DefaultArtifactHandler()));
        project.setName(projectName);
        return project;
    }
}
