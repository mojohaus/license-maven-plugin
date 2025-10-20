package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.mojo.license.model.LicenseMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link AbstractAddThirdPartyMojo#checkForbiddenLicenses()}.
 */
class AbstractAddThirdPartyMojoTest {

    private TestableAbstractAddThirdPartyMojo mojo;

    @BeforeEach
    void setUp() {
        ThirdPartyTool thirdPartyTool = mock(ThirdPartyTool.class);
        DependenciesTool dependenciesTool = mock(DependenciesTool.class);
        MavenProjectHelper projectHelper = mock(MavenProjectHelper.class);

        mojo = new TestableAbstractAddThirdPartyMojo(thirdPartyTool, dependenciesTool, projectHelper);
        mojo.licenseMap = new LicenseMap();
        // Initialize the included and excluded licenses lists to avoid NullPointerException
        mojo.includedLicenses = new AbstractAddThirdPartyMojo.IncludedLicenses();
        mojo.excludedLicenses = new AbstractAddThirdPartyMojo.ExcludedLicenses();
    }

    @Nested
    class EmptyBlacklist {

        @Test
        void findWhitelistedLicence() throws MojoExecutionException {
            // Given: all dependencies use whitelisted licenses
            mojo.setIncludedLicenses("Good|Safe");
            mojo.licenseMap.put("Good", createProject("artifact1"));
            mojo.licenseMap.put("Safe", createProject("artifact2"));

            assertTrue(mojo.checkForbiddenLicenses());
        }

        @Test
        void doNotFindWhitelistedLicence() throws MojoExecutionException {
            // Given: dependency uses non-whitelisted license
            mojo.setIncludedLicenses("Good|Safe");
            mojo.licenseMap.put("Good", createProject("artifact1"));
            mojo.licenseMap.put("unlisted", createProject("artifact2"));

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void findWhitelistedDualLicenced() throws MojoExecutionException {
            // Given: dependency has multiple licenses, one of which is whitelisted
            mojo.setIncludedLicenses("Good");
            MavenProject dualLicensedProject = createProject("artifact1");

            mojo.licenseMap.put("Good", dualLicensedProject);
            mojo.licenseMap.put("unlisted", dualLicensedProject); // Same project, different license

            assertTrue(mojo.checkForbiddenLicenses());
        }
    }

    @Nested
    class EmptyWhitelist {

        @Test
        void findBlacklistedLicence() throws MojoExecutionException {
            // Given: blacklisted license is present
            mojo.setExcludedLicenses("UNSAFE|FORBIDDEN");
            mojo.licenseMap.put("UNSAFE", createProject("artifact1"));
            mojo.licenseMap.put("Unlisted", createProject("artifact2"));

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void doNotFindForbiddenLicence() throws MojoExecutionException {
            // Given: blacklisted license is not present
            mojo.setExcludedLicenses("UNSAFE|FORBIDDEN");
            mojo.licenseMap.put("Unlisted", createProject("artifact1"));

            assertTrue(mojo.checkForbiddenLicenses());
        }
    }

    @Nested
    class WhitelistAndBlacklist {

        @Test
        void project1IsWhitelistedProject2IsBlacklisted() throws MojoExecutionException {
            // Given: both blacklist and whitelist configured
            mojo.setIncludedLicenses("Good|Safe");
            mojo.setExcludedLicenses("UNSAFE");

            mojo.licenseMap.put("Good", createProject("artifact1"));
            mojo.licenseMap.put("UNSAFE", createProject("artifact2"));

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void findWhitelistedDoNotFindBlacklistedLicence() throws MojoExecutionException {
            // Given: both blacklist and whitelist configured, all licenses comply
            mojo.setIncludedLicenses("Good|Safe");
            mojo.setExcludedLicenses("UNSAFE");

            mojo.licenseMap.put("Good", createProject("artifact1"));
            mojo.licenseMap.put("Safe", createProject("artifact2"));

            assertTrue(mojo.checkForbiddenLicenses());
        }

        @Test
        void findWhitelistedAndUnlisted() throws MojoExecutionException {

            mojo.setIncludedLicenses("Good|Safe");
            mojo.setExcludedLicenses("UNSAFE");

            mojo.licenseMap.put("Good", createProject("artifact1"));
            mojo.licenseMap.put("Unlisted", createProject("artifact2"));

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void findBlacklistedAndUnlisted() throws MojoExecutionException {

            mojo.setIncludedLicenses("Good|Safe");
            mojo.setExcludedLicenses("UNSAFE");

            mojo.licenseMap.put("UNSAFE", createProject("artifact1"));
            mojo.licenseMap.put("Unlisted", createProject("artifact2"));

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void dualLicensedProjectisBothWhitelistedAndUnlisted() throws MojoExecutionException {
            mojo.setIncludedLicenses("Good");
            mojo.setExcludedLicenses("UNSAFE");

            final MavenProject dualLicensedProject = createProject("artifact");
            mojo.licenseMap.put("Good", dualLicensedProject);
            mojo.licenseMap.put("Unlisted", dualLicensedProject);

            assertTrue(mojo.checkForbiddenLicenses());
        }

        @Test
        void dualLicensedProjectisBothBlacklistedAndUnlisted() throws MojoExecutionException {
            mojo.setIncludedLicenses("Good");
            mojo.setExcludedLicenses("UNSAFE");

            final MavenProject dualLicensedProject = createProject("artifact");
            mojo.licenseMap.put("UNSAFE", dualLicensedProject);
            mojo.licenseMap.put("Unlisted", dualLicensedProject);

            assertFalse(mojo.checkForbiddenLicenses());
        }

        @Test
        void dualLicencedProjectisBothWhitelistedAndBlacklisted() throws MojoExecutionException {
            mojo.setIncludedLicenses("Good");
            mojo.setExcludedLicenses("UNSAFE");

            final MavenProject dualLicencedProject = createProject("artifact");
            mojo.licenseMap.put("Good", dualLicencedProject);
            mojo.licenseMap.put("UNSAFE", dualLicencedProject);

            assertTrue(mojo.checkForbiddenLicenses());
        }
    }

    @Test
    void defaultStateIsTrue() {
        // Given: no dependencies

        assertTrue(mojo.checkForbiddenLicenses());
    }

    /**
     * Helper method to create a test MavenProject.
     */
    private MavenProject createProject(String artifactId) {
        MavenProject project = new MavenProject();
        project.setArtifact(new DefaultArtifact(
                "org.example", artifactId, "1.0.0", "compile", "jar", null, new DefaultArtifactHandler()));
        project.setName(artifactId);
        project.setGroupId("org.example");
        project.setArtifactId(artifactId);
        project.setVersion("1.0.0");
        return project;
    }

    /**
     * Testable concrete implementation of AbstractAddThirdPartyMojo.
     */
    private static class TestableAbstractAddThirdPartyMojo extends AbstractAddThirdPartyMojo {

        TestableAbstractAddThirdPartyMojo(
                ThirdPartyTool thirdPartyTool, DependenciesTool dependenciesTool, MavenProjectHelper projectHelper) {
            super(thirdPartyTool, dependenciesTool, projectHelper);
        }

        @Override
        protected SortedMap<String, MavenProject> loadDependencies() {
            return new TreeMap<>();
        }

        @Override
        protected org.codehaus.mojo.license.utils.SortedProperties createUnsafeMapping() {
            return new org.codehaus.mojo.license.utils.SortedProperties("UTF-8");
        }

        @Override
        public boolean isSkip() {
            return false;
        }

        @Override
        protected void init() {
            // Not needed for unit tests
        }

        @Override
        protected void doAction() {
            // Not needed for unit tests
        }
    }
}
