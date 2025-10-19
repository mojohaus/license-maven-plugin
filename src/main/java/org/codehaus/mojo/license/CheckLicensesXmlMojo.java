package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2025 MojoHaus
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

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.download.LicenseSummaryReader;
import org.codehaus.mojo.license.download.ProjectLicenseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that the licenses.xml file is complete and minimal with respect to JAR files in specified directories.
 * <p>
 * This goal verifies:
 * <ul>
 * <li>Completeness: All JAR files in the specified directories have corresponding entries in licenses.xml</li>
 * <li>Minimality: All entries in licenses.xml correspond to JAR files present in the specified directories</li>
 * </ul>
 *
 * @author MojoHaus
 * @since 2.8.0
 */
@Mojo(name = "check-licenses-xml", threadSafe = true)
public class CheckLicensesXmlMojo extends AbstractLicenseMojo {

    private static final Logger LOG = LoggerFactory.getLogger(CheckLicensesXmlMojo.class);

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The licenses.xml file to check.
     *
     * @since 2.8.0
     */
    @Parameter(
            property = "license.licensesXmlFile",
            defaultValue = "${project.build.directory}/generated-resources/licenses.xml")
    private File licensesXmlFile;

    /**
     * Directories containing JAR files to check against licenses.xml.
     * Each JAR file found in these directories should have a corresponding entry in licenses.xml.
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.jarDirectories")
    private List<File> jarDirectories;

    /**
     * A flag to fail the build if the licenses.xml file is incomplete or not minimal.
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.failOnError", defaultValue = "true")
    private boolean failOnError;

    /**
     * A flag to skip the goal.
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.skipCheckLicensesXml", defaultValue = "false")
    private boolean skipCheckLicensesXml;

    /**
     * Pattern to extract artifact information from JAR file names.
     * The pattern should contain named groups: groupId, artifactId, and version.
     * Default pattern matches standard Maven JAR naming: artifactId-version.jar
     * <p>
     * For more complex scenarios, you can specify a custom pattern like:
     * {@code (?<groupId>[^-]+)-(?<artifactId>[^-]+)-(?<version>[^-]+)\.jar}
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.jarNamePattern")
    private String jarNamePattern;

    /**
     * Perform minimality check (check if licenses.xml has entries not present in JAR directories).
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.checkMinimality", defaultValue = "true")
    private boolean checkMinimality;

    /**
     * Perform completeness check (check if all JARs have entries in licenses.xml).
     *
     * @since 2.8.0
     */
    @Parameter(property = "license.checkCompleteness", defaultValue = "true")
    private boolean checkCompleteness;

    // Standard Maven JAR naming pattern: artifactId-version.jar
    private static final Pattern DEFAULT_JAR_PATTERN = Pattern.compile(
            "(?<artifactId>.+?)-(?<version>\\d+(?:\\.\\d+)*(?:[.-].*)?)(?:-(?<classifier>[^.]+))?\\.jar");

    @Inject
    public CheckLicensesXmlMojo(MavenProjectHelper projectHelper) {
        super(projectHelper);
    }

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementation
    // ----------------------------------------------------------------------

    @Override
    public boolean isSkip() {
        return skipCheckLicensesXml;
    }

    @Override
    protected void init() throws Exception {
        // Validate parameters
        if (!licensesXmlFile.exists()) {
            throw new MojoFailureException("Licenses XML file does not exist: " + licensesXmlFile.getAbsolutePath());
        }

        if (jarDirectories == null || jarDirectories.isEmpty()) {
            throw new MojoFailureException("No JAR directories specified. "
                    + "Use the 'jarDirectories' parameter to specify directories containing JAR files.");
        }

        for (File dir : jarDirectories) {
            if (!dir.exists()) {
                throw new MojoFailureException("JAR directory does not exist: " + dir.getAbsolutePath());
            }
            if (!dir.isDirectory()) {
                throw new MojoFailureException("JAR directory is not a directory: " + dir.getAbsolutePath());
            }
        }
    }

    @Override
    protected void doAction() throws Exception {
        LOG.info("Checking licenses.xml completeness and minimality");
        LOG.info("Licenses XML file: " + licensesXmlFile.getAbsolutePath());
        LOG.info("JAR directories: " + jarDirectories);

        // Parse licenses.xml
        List<ProjectLicenseInfo> licensesInfo = parseLicensesXml();
        LOG.info("Found " + licensesInfo.size() + " dependencies in licenses.xml");

        // Scan JAR directories
        Set<ArtifactInfo> jarArtifacts = scanJarDirectories();
        LOG.info("Found " + jarArtifacts.size() + " JAR files in directories");

        // Perform checks
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (checkCompleteness) {
            checkCompletenessErrors(jarArtifacts, licensesInfo, errors);
        }

        if (checkMinimality) {
            checkMinimalityWarnings(jarArtifacts, licensesInfo, warnings);
        }

        // Report results
        reportResults(errors, warnings);
    }

    private List<ProjectLicenseInfo> parseLicensesXml() throws MojoExecutionException {
        try {
            return LicenseSummaryReader.parseLicenseSummary(licensesXmlFile);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to parse licenses.xml: " + licensesXmlFile, e);
        }
    }

    private Set<ArtifactInfo> scanJarDirectories() throws MojoExecutionException {
        Set<ArtifactInfo> artifacts = new HashSet<>();
        Pattern pattern = getJarNamePattern();

        for (File dir : jarDirectories) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir.toPath(), "*.jar")) {
                for (Path jarPath : stream) {
                    String jarName = jarPath.getFileName().toString();
                    ArtifactInfo artifact = parseJarFileName(jarName, pattern);
                    if (artifact != null) {
                        artifacts.add(artifact);
                        LOG.debug("Found JAR: " + artifact);
                    } else {
                        LOG.warn("Could not parse JAR file name: " + jarName);
                    }
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to scan directory: " + dir, e);
            }
        }

        return artifacts;
    }

    private Pattern getJarNamePattern() {
        if (jarNamePattern != null && !jarNamePattern.isEmpty()) {
            return Pattern.compile(jarNamePattern);
        }
        return DEFAULT_JAR_PATTERN;
    }

    private ArtifactInfo parseJarFileName(String jarName, Pattern pattern) {
        Matcher matcher = pattern.matcher(jarName);
        if (matcher.matches()) {
            String artifactId = matcher.group("artifactId");
            String version = matcher.group("version");

            // Try to get groupId from pattern, otherwise null
            String groupId = null;
            try {
                groupId = matcher.group("groupId");
            } catch (IllegalArgumentException e) {
                // groupId group not present in pattern
            }

            return new ArtifactInfo(groupId, artifactId, version);
        }
        return null;
    }

    private void checkCompletenessErrors(
            Set<ArtifactInfo> jarArtifacts, List<ProjectLicenseInfo> licensesInfo, List<String> errors) {
        Set<ArtifactInfo> licensedArtifacts = new HashSet<>();
        for (ProjectLicenseInfo info : licensesInfo) {
            licensedArtifacts.add(new ArtifactInfo(info.getGroupId(), info.getArtifactId(), info.getVersion()));
        }

        for (ArtifactInfo jarArtifact : jarArtifacts) {
            if (!isArtifactInLicenses(jarArtifact, licensedArtifacts)) {
                errors.add("Missing license entry for JAR: " + jarArtifact);
            }
        }
    }

    private void checkMinimalityWarnings(
            Set<ArtifactInfo> jarArtifacts, List<ProjectLicenseInfo> licensesInfo, List<String> warnings) {
        for (ProjectLicenseInfo info : licensesInfo) {
            ArtifactInfo licenseArtifact = new ArtifactInfo(info.getGroupId(), info.getArtifactId(), info.getVersion());
            if (!isArtifactInJars(licenseArtifact, jarArtifacts)) {
                warnings.add("Extra license entry not matching any JAR: " + licenseArtifact);
            }
        }
    }

    private boolean isArtifactInLicenses(ArtifactInfo jarArtifact, Set<ArtifactInfo> licensedArtifacts) {
        // Try exact match first
        if (licensedArtifacts.contains(jarArtifact)) {
            return true;
        }

        // If JAR has no groupId, try matching by artifactId and version only
        if (jarArtifact.groupId == null) {
            for (ArtifactInfo licensed : licensedArtifacts) {
                if (licensed.artifactId.equals(jarArtifact.artifactId)
                        && licensed.version.equals(jarArtifact.version)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isArtifactInJars(ArtifactInfo licenseArtifact, Set<ArtifactInfo> jarArtifacts) {
        // Try exact match first
        if (jarArtifacts.contains(licenseArtifact)) {
            return true;
        }

        // Try matching by artifactId and version only (when JAR doesn't have groupId)
        for (ArtifactInfo jar : jarArtifacts) {
            if (jar.groupId == null
                    && jar.artifactId.equals(licenseArtifact.artifactId)
                    && jar.version.equals(licenseArtifact.version)) {
                return true;
            }
        }

        return false;
    }

    private void reportResults(List<String> errors, List<String> warnings) throws MojoFailureException {
        boolean hasErrors = !errors.isEmpty();
        boolean hasWarnings = !warnings.isEmpty();

        if (hasErrors) {
            LOG.error("========================================");
            LOG.error("Completeness Check Failed");
            LOG.error("========================================");
            for (String error : errors) {
                LOG.error("  " + error);
            }
            LOG.error("========================================");
        }

        if (hasWarnings) {
            LOG.warn("========================================");
            LOG.warn("Minimality Check Warnings");
            LOG.warn("========================================");
            for (String warning : warnings) {
                LOG.warn("  " + warning);
            }
            LOG.warn("========================================");
        }

        if (!hasErrors && !hasWarnings) {
            LOG.info("========================================");
            LOG.info("Check Passed");
            LOG.info("========================================");
            LOG.info("licenses.xml is complete and minimal with respect to JAR directories");
        }

        if (hasErrors && failOnError) {
            throw new MojoFailureException(
                    "licenses.xml completeness check failed. " + "See errors above for missing license entries.");
        }
    }

    // ----------------------------------------------------------------------
    // Helper Classes
    // ----------------------------------------------------------------------

    private static class ArtifactInfo {
        final String groupId;
        final String artifactId;
        final String version;

        ArtifactInfo(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ArtifactInfo that = (ArtifactInfo) o;

            if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) {
                return false;
            }
            if (!artifactId.equals(that.artifactId)) {
                return false;
            }
            return version.equals(that.version);
        }

        @Override
        public int hashCode() {
            int result = groupId != null ? groupId.hashCode() : 0;
            result = 31 * result + artifactId.hashCode();
            result = 31 * result + version.hashCode();
            return result;
        }

        @Override
        public String toString() {
            if (groupId != null) {
                return groupId + ":" + artifactId + ":" + version;
            }
            return artifactId + ":" + version;
        }
    }
}
