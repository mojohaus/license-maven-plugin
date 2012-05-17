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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Default implementation of the {@link org.codehaus.mojo.license.api.ThirdPartyHelper}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public class DefaultThirdPartyHelper
    implements ThirdPartyHelper
{

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
     * Local repository used.
     */
    private final ArtifactRepository localRepository;

    /**
     * List of remote repositories.
     */
    private final List<ArtifactRepository> remoteRepositories;

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
     * Instance logger.
     */
    private final Log log;

    private static SortedMap<String, MavenProject> artifactCache;

    public DefaultThirdPartyHelper( MavenProject project, String encoding, boolean verbose,
                                    DependenciesTool dependenciesTool, ThirdPartyTool thirdPartyTool,
                                    ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
                                    Log log )
    {
        this.project = project;
        this.encoding = encoding;
        this.verbose = verbose;
        this.dependenciesTool = dependenciesTool;
        this.thirdPartyTool = thirdPartyTool;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> getArtifactCache()
    {
        if ( artifactCache == null )
        {
            artifactCache = new TreeMap<String, MavenProject>();
        }
        return artifactCache;
    }

    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> loadDependencies( MavenProjectDependenciesConfigurator configuration )
    {
        return dependenciesTool.loadProjectDependencies( project, configuration, localRepository, remoteRepositories,
                                                         getArtifactCache() );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadThirdPartyDescriptorForUnsafeMapping( SortedSet<MavenProject> unsafeDependencies,
                                                                      Collection<MavenProject> projects,
                                                                      LicenseMap licenseMap )
        throws ThirdPartyToolException, IOException
    {
        return thirdPartyTool.loadThirdPartyDescriptorsForUnsafeMapping( encoding, projects, unsafeDependencies,
                                                                         licenseMap, localRepository,
                                                                         remoteRepositories );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadUnsafeMapping( LicenseMap licenseMap, File missingFile )
        throws IOException
    {
        return thirdPartyTool.loadUnsafeMapping( licenseMap, getArtifactCache(), encoding, missingFile );
    }

    /**
     * {@inheritDoc}
     */
    public LicenseMap createLicenseMap( SortedMap<String, MavenProject> dependencies )
    {

        LicenseMap licenseMap = new LicenseMap();

        for ( MavenProject project : dependencies.values() )
        {
            thirdPartyTool.addLicense( licenseMap, project, project.getLicenses() );
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
        return thirdPartyTool.getProjectsWithNoLicense( licenseMap, verbose );
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties createUnsafeMapping( LicenseMap licenseMap, File missingFile,
                                                 boolean useRepositoryMissingFiles,
                                                 SortedSet<MavenProject> unsafeDependencies,
                                                 Collection<MavenProject> projectDependencies )
        throws ProjectBuildingException, IOException, ThirdPartyToolException
    {

        SortedProperties unsafeMappings = loadUnsafeMapping( licenseMap, missingFile );

        if ( CollectionUtils.isNotEmpty( unsafeDependencies ) )
        {

            // there is some unresolved license

            if ( useRepositoryMissingFiles )
            {

                // try to load missing third party files from dependencies

                Collection<MavenProject> projects = new ArrayList<MavenProject>( projectDependencies );
                projects.remove( project );
                projects.removeAll( unsafeDependencies );

                SortedProperties resolvedUnsafeMapping =
                    loadThirdPartyDescriptorForUnsafeMapping( unsafeDependencies, projects, licenseMap );

                // push back resolved unsafe mappings
                unsafeMappings.putAll( resolvedUnsafeMapping );
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

        if ( !CollectionUtils.isEmpty( licenseMerges ) )
        {

            // check where is not multi licenses merged main licenses (see OJO-1723)
            Map<String, String[]> mergedLicenses = new HashMap<String, String[]>();

            for ( String merge : licenseMerges )
            {
                merge = merge.trim();
                String[] split = merge.split( "\\|" );

                String mainLicense = split[0];

                if ( mergedLicenses.containsKey( mainLicense ) )
                {

                    // this license was already describe, fail the build...

                    throw new MojoFailureException(
                        "The merge main license " + mainLicense + " was already registred in the " +
                            "configuration, please use only one such entry as describe in example " +
                            "http://mojo.codehaus.org/license-maven-plugin/examples/example-thirdparty.html#Merge_licenses." );
                }
                mergedLicenses.put( mainLicense, split );
            }

            // merge licenses in license map

            for ( String[] mergedLicense : mergedLicenses.values() )
            {
                if ( verbose )
                {
                    log.info( "Will merge " + Arrays.toString( mergedLicense ) + "" );
                }

                thirdPartyTool.mergeLicenses( licenseMap, mergedLicense );
            }
        }
    }

}
