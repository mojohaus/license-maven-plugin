package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the {@link org.codehaus.mojo.license.api.ThirdPartyHelper}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.1
 */
public class DefaultThirdPartyHelper
        implements ThirdPartyHelper
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultThirdPartyHelper.class );

    /**
     * DependenciesTool to load dependencies.
     *
     * @see DependenciesTool
     */
    private final DependenciesTool dependenciesTool;

    /**
     * ThirdPartyTool to load third-parties descriptors.
     *
     * @see ThirdPartyTool
     */
    private final ThirdPartyTool thirdPartyTool;

    /**
     * List of remote repositories. Same as remoteRepositories, just a different API.
     */
    private final List<ArtifactRepository> remoteRepositoriesCoreApi;

    /**
     * List of remote repositories.
     */
    private final List<RemoteRepository> remoteRepositories;

    /**
     * Current maven project.
     */
    private final MavenProject project;

    /**
     * Encoding used to read and write files.
     */
    private final String encoding;

    /**
     * Verbose flag.
     */
    private final boolean verbose;

    /**
     * Cache of dependencies (as maven project) loaded.
     */
    private static volatile SortedMap<String, MavenProject> artifactCache;

    // Mutex to guard lazy initialization of artifactCache
    private static final Object artifactCacheMutex = new Object();

    /**
     * Constructor of the helper.
     *
     * @param project            Current maven project
     * @param encoding           Encoding used to read and write files
     * @param verbose            Verbose flag
     * @param dependenciesTool   tool to load dependencies
     * @param thirdPartyTool     tool to load third-parties descriptors
     * @param remoteRepositoriesCoreApi maven remote repositories, in the core api format
     * @param remoteRepositories maven remote repositories
     */
    // CHECKSTYLE_OFF: ParameterNumber
    public DefaultThirdPartyHelper( MavenProject project, String encoding, boolean verbose,
                                    DependenciesTool dependenciesTool, ThirdPartyTool thirdPartyTool,
                                    List<ArtifactRepository> remoteRepositoriesCoreApi,
                                    List<RemoteRepository> remoteRepositories )
    {
        // CHECKSTYLE_ON: ParameterNumber
        this.project = project;
        this.encoding = encoding;
        this.verbose = verbose;
        this.dependenciesTool = dependenciesTool;
        this.thirdPartyTool = thirdPartyTool;
        this.remoteRepositoriesCoreApi = remoteRepositoriesCoreApi;
        this.remoteRepositories = remoteRepositories;
        this.thirdPartyTool.setVerbose( verbose );
    }

    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> getArtifactCache()
    {
        if ( artifactCache == null )
        {
            synchronized ( artifactCacheMutex )
            {
                if ( artifactCache == null )
                {
                    artifactCache = Collections.synchronizedSortedMap( new TreeMap<String, MavenProject>() );
                }
            }
        }
        return artifactCache;
    }

    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> loadDependencies( MavenProjectDependenciesConfigurator configuration,
                                                             ResolvedProjectDependencies dependencyArtifacts )
    {
        return dependenciesTool.loadProjectDependencies( dependencyArtifacts, configuration,
                remoteRepositoriesCoreApi, getArtifactCache() );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadThirdPartyDescriptorForUnsafeMapping( Set<Artifact> topLevelDependencies,
                                                                      SortedSet<MavenProject> unsafeDependencies,
                                                                      Collection<MavenProject> projects,
                                                                      LicenseMap licenseMap )
            throws ThirdPartyToolException, IOException
    {
        return thirdPartyTool.loadThirdPartyDescriptorsForUnsafeMapping( topLevelDependencies, encoding, projects,
                unsafeDependencies, licenseMap, remoteRepositories );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadUnsafeMapping( LicenseMap licenseMap, File missingFile, String missingFileUrl,
                                               SortedMap<String, MavenProject> projectDependencies )
      throws IOException, MojoExecutionException
    {
        return thirdPartyTool.loadUnsafeMapping( licenseMap, projectDependencies, encoding, missingFile,
                missingFileUrl );
    }

    /**
     * {@inheritDoc}
     */
    public LicenseMap createLicenseMap( SortedMap<String, MavenProject> dependencies )
    {

        LicenseMap licenseMap = new LicenseMap();

        synchronized ( dependencies )
        {
            for ( MavenProject project : dependencies.values() )
            {
                thirdPartyTool.addLicense( licenseMap, project, project.getLicenses() );
            }
        }
        return licenseMap;
    }

    /**
     * {@inheritDoc}
     */
    public void attachThirdPartyDescriptor( File file )
    {

        thirdPartyTool.attachThirdPartyDescriptor( project, file );
    }


    /**
     * {@inheritDoc}
     */
    public SortedSet<MavenProject> getProjectsWithNoLicense( LicenseMap licenseMap )
    {
        return thirdPartyTool.getProjectsWithNoLicense( licenseMap );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties createUnsafeMapping( LicenseMap licenseMap, File missingFile, String missingFileUrl,
                                                 boolean useRepositoryMissingFiles,
                                                 SortedSet<MavenProject> unsafeDependencies,
                                                 SortedMap<String, MavenProject> projectDependencies,
                                                 Set<Artifact> dependencyArtifacts )
      throws ProjectBuildingException, IOException, ThirdPartyToolException, MojoExecutionException
    {

        SortedProperties unsafeMappings = loadUnsafeMapping( licenseMap, missingFile, missingFileUrl,
                                                             projectDependencies );

        if ( CollectionUtils.isNotEmpty( unsafeDependencies ) )
        {

            // there is some unresolved license

            if ( useRepositoryMissingFiles )
            {

                // try to load missing third party files from dependencies

                Collection<MavenProject> projects = new ArrayList<>( projectDependencies.values() );
                projects.remove( project );
                projects.removeAll( unsafeDependencies );

                SortedProperties resolvedUnsafeMapping =
                        loadThirdPartyDescriptorForUnsafeMapping( dependencyArtifacts, unsafeDependencies, projects,
                                                                  licenseMap );

                // push back resolved unsafe mappings (only for project dependencies)
                for ( Object coord : resolvedUnsafeMapping.keySet() )
                {
                    String s = (String) coord;
                    if ( projectDependencies.containsKey( s ) )
                    {
                        unsafeMappings.put( s, resolvedUnsafeMapping.get( s ) );
                    }
                }
            }
        }

        return unsafeMappings;
    }

    /**
     * {@inheritDoc}
     */
    public void mergeLicenses( List<String> licenseMerges, LicenseMap licenseMap )
            throws MojoFailureException
    {

        Set<String> licenseFound = new HashSet<>();

        if ( !CollectionUtils.isEmpty( licenseMerges ) )
        {

            // check where is not multi licenses merged main licenses (see MLICENSE-23)
            Map<String, Set<String>> mergedLicenses = new HashMap<>();

            for ( String merge : licenseMerges )
            {
                merge = merge.trim();
                String[] split = merge.split( "\\s*\\|\\s*" );

                String mainLicense = split[0];

                Set<String> mergeList;

                if ( mergedLicenses.containsKey( mainLicense ) )
                {

                    mergeList = mergedLicenses.get( mainLicense );
                }
                else
                {
                    mergeList = new HashSet<>();
                }

                for ( int i = 0; i < split.length; i++ )
                {
                    String licenseToAdd = split[i];
                    if ( i == 0 )
                    {
                        // mainLicense will not be merged (to itself)
                        continue;
                    }

                    // check license not already described to be merged
                    if ( mergeList.contains( licenseToAdd ) || licenseFound.contains( licenseToAdd )
                            || mainLicense.equals( licenseToAdd ) )
                    {

                        // this license to merge was already described, fail the build...

                        throw new MojoFailureException(
                                "The license " + licenseToAdd + " was already registered in the "
                                        + "configuration, please use only one such entry as described in the example "
                                        + "http://mojohaus.org/license-maven-plugin/examples/"
                                        + "example-thirdparty.html#Merge_licenses" );
                    }

                    // can add this license for merge
                    mergeList.add( licenseToAdd );
                    licenseFound.add( licenseToAdd );
                }

                // push back licenses to merge for this main license
                mergedLicenses.put( mainLicense, mergeList );
            }

            // merge licenses in license map

            for ( Map.Entry<String, Set<String>> entry : mergedLicenses.entrySet() )
            {
                String mainLicense = entry.getKey();
                Set<String> mergedLicense = entry.getValue();
                if ( verbose )
                {
                    LOG.info( "Will merge to '{}', licenses: {}", mainLicense, mergedLicense );
                }

                thirdPartyTool.mergeLicenses( licenseMap, mainLicense, mergedLicense );
            }
        }
    }

}
