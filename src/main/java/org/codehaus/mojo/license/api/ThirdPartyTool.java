package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2011 CodeLutin, Codehaus, Tony Chemit
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * A tool to load third party files missing files.
 * <p>
 * We should put here all the logic code written in some mojo and licenseMap...
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public interface ThirdPartyTool {

    /**
     * Is log should be verbose?
     *
     * @return {@code true} if verbose log should be produced, {@code false} otherwise.
     * @since 1.4
     */
    boolean isVerbose();

    /**
     * Sets the verbose mode.
     *
     * @param verbose new verbose mode to set
     * @since 1.4
     */
    void setVerbose(boolean verbose);

    /**
     * Collect license information from property file, 'third-party' classified artifacts, and .license.properties
     * dependencies.
     *
     * @param dependencies       top-level dependencies to scan for .license.properties files.
     * @param encoding           encoding used to read or write properties files
     * @param projects           all projects where to read third parties descriptors
     * @param unsafeProjects     all unsafe projects
     * @param licenseMap         license map where to store new licenses
     * @param remoteRepositories remote repositories
     * @return the map of loaded missing from the remote missing third party files
     * @throws ThirdPartyToolException if any
     * @throws IOException             if any
     */
    SortedProperties loadThirdPartyDescriptorsForUnsafeMapping(
            Set<Artifact> dependencies,
            String encoding,
            Collection<MavenProject> projects,
            SortedSet<MavenProject> unsafeProjects,
            LicenseMap licenseMap,
            List<RemoteRepository> remoteRepositories)
            throws ThirdPartyToolException, IOException;

    /**
     * For the given {@code project}, attach the given {@code file} as a third-party file.
     * <p>
     * The file will be attached as with a classifier {@code third-parties} and a type {@code properties}.
     *
     * @param project the project on which to attch the third-party file
     * @param file    the third-party file to attach.
     */
    void attachThirdPartyDescriptor(MavenProject project, File file);

    /**
     * Obtain the third party file from the repository.
     * <p>
     * Will first search in the local repository, then into the remote repositories and will resolve it.
     *
     * @param project         the project
     * @param remoteRepositories    the remote repositories
     * @return the locale file resolved into the local repository
     * @throws ThirdPartyToolException if any
     */
    File resolvThirdPartyDescriptor(MavenProject project, List<RemoteRepository> remoteRepositories)
            throws ThirdPartyToolException;

    File resolveMissingLicensesDescriptor(
            String groupId, String artifactId, String version, List<RemoteRepository> remoteRepositories)
            throws IOException, ArtifactResolutionException, ArtifactNotFoundException;

    /**
     * From the given {@code licenseMap}, obtain all the projects with no license.
     *
     * @param licenseMap the license map to query
     * @param doLog      a flag to add debug logs
     * @return the set of projects with no license
     */
    SortedSet<MavenProject> getProjectsWithNoLicense(LicenseMap licenseMap);

    /**
     * Loads unsafe mapping and returns it.
     *
     * @param licenseMap      license map
     * @param artifactCache   cache of dependencies (used for id migration from missing file)
     * @param encoding        encoding used to load missing file
     * @param missingFile     location of the optional missing file
     * @param missingFileUrl  location of an optional missing file extension that can be downloaded from some
     *                        resource hoster and that will be merged with the content of the missing file.
     * @return the unsafe mapping
     * @throws IOException if pb while reading missing file
     */
    SortedProperties loadUnsafeMapping(
            LicenseMap licenseMap,
            SortedMap<String, MavenProject> artifactCache,
            String encoding,
            File missingFile,
            String missingFileUrl)
            throws IOException, MojoExecutionException;

    /**
     * Override licenses from override file.
     *
     * @param licenseMap    license map
     * @param artifactCache cache of dependencies (used for id migration from missing file)
     * @param encoding      encoding used to load override file
     * @param overrideUrl   location of an optional override file extension that can be downloaded from some resource
     *                      hoster
     * @throws IOException if pb while reading override file
     */
    void overrideLicenses(
            LicenseMap licenseMap, SortedMap<String, MavenProject> artifactCache, String encoding, String overrideUrl)
            throws IOException;

    /**
     * Add one or more licenses (name and url are {@code licenseNames}) to the given {@code licenseMap} for the given
     * {@code project}.
     *
     * @param licenseMap   the license map where to add the license
     * @param project      the project
     * @param licenseNames the names of the licenses
     */
    void addLicense(LicenseMap licenseMap, MavenProject project, String... licenseNames);

    /**
     * Add a given {@code license} to the given {@code licenseMap} for the given {@code project}.
     *
     * @param licenseMap the license map where to add the license
     * @param project    the project
     * @param license    the license to add
     */
    void addLicense(LicenseMap licenseMap, MavenProject project, License license);

    /**
     * Add a given {@code licenses} to the given {@code licenseMap} for the given {@code project}.
     *
     * @param licenseMap the license map where to add the licenses
     * @param project    the project
     * @param licenses   the licenses to add
     */
    void addLicense(LicenseMap licenseMap, MavenProject project, List<?> licenses);

    /**
     * For a given {@code licenseMap}, merge all {@code licenses}.
     * <p>
     * The first value of the {@code licenses} is the license to keep and all other values will be merged into the
     * first one.
     *
     * @param licenseMap      the license map to merge
     * @param mainLicense     the main license to keep
     * @param licensesToMerge all the licenses to merge
     */
    void mergeLicenses(LicenseMap licenseMap, String mainLicense, Set<String> licensesToMerge);

    /**
     * Write the content of the third-party file.
     *
     * @param licenseMap     map of all license to use
     * @param thirdPartyFile location of file to generate
     * @param verbose        verbose flag
     * @param encoding       encoding used to generate file
     * @param template       the location of the freemarker template used to generate the file content
     * @throws IOException if any probem while writing file
     */
    void writeThirdPartyFile(
            LicenseMap licenseMap, File thirdPartyFile, boolean verbose, String encoding, String template)
            throws IOException;

    /**
     * Writes the bundled version of the third-party file.
     *
     * @param thirdPartyFile       location of normal third-party file
     * @param outputDirectory      where to generate bundled version of the third-party file
     * @param bundleThirdPartyPath relative end path of the file to generate
     * @throws IOException if any problem while writing file
     */
    void writeBundleThirdPartyFile(File thirdPartyFile, File outputDirectory, String bundleThirdPartyPath)
            throws IOException;
}
