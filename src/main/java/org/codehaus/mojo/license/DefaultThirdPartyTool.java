/*
 * #%L
 * License Maven Plugin
 *
 * $Id$
 * $HeadURL$
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
package org.codehaus.mojo.license;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Default implementation of the third party tool.
 *
 * @author <a href="mailto:tchemit@codelutin.com">Tony Chemit</a>
 * @version $Id$
 * @plexus.component role="org.codehaus.mojo.license.ThirdPartyTool" role-hint="default"
 */
public class DefaultThirdPartyTool
    extends AbstractLogEnabled
    implements ThirdPartyTool
{

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    /**
     * The component that is used to resolve additional artifacts required.
     *
     * @plexus.requirement
     */
    private ArtifactResolver artifactResolver;

    /**
     * The component used for creating artifact instances.
     *
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * Project builder.
     *
     * @plexus.requirement
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * Maven ProjectHelper.
     *
     * @plexus.requirement
     */
    private MavenProjectHelper projectHelper;

    /**
     * {@inheritDoc}
     */
    public void deployThirdPartyDescriptor( MavenProject project, File file )
    {

        projectHelper.attachArtifact( project, "properties", "third-party", file );
    }

    public SortedProperties loadThirdPartyDescriptorsForUnsafeMapping( MavenProject project, String encoding,
                                                           Collection<MavenProject> projects,
                                                           SortedSet<MavenProject> unsafeDependencies,
                                                           LicenseMap licenseMap,
                                                           ArtifactRepository localRepository,
                                                           List<ArtifactRepository> repositories )
        throws ThirdPartyToolException, IOException
    {

        SortedProperties result = new SortedProperties( encoding );
        Map<String, MavenProject> unsafeProjects = new HashMap<String, MavenProject>();
        for ( MavenProject unsafeDependency : unsafeDependencies )
        {
            String id = ArtifactHelper.getArtifactId( unsafeDependency.getArtifact() );
            unsafeProjects.put( id, unsafeDependency );
        }

        for ( MavenProject mavenProject : projects )
        {

            if ( CollectionUtils.isEmpty( unsafeDependencies ) )
            {

                // no more unsafe dependencies to find
                break;
            }

            File thirdPartyDescriptor = getThirdPartyDescriptor( mavenProject, localRepository, repositories );

            if ( thirdPartyDescriptor != null && thirdPartyDescriptor.exists() && thirdPartyDescriptor.length() > 0 )
            {

                if ( getLogger().isInfoEnabled() )
                {
                    getLogger().info( "Detects third party descriptor " + thirdPartyDescriptor );
                }
                // there is a third party file detected form the given dependency

                SortedProperties unsafeMappings = new SortedProperties( encoding );

                if ( thirdPartyDescriptor.exists() )
                {

                    getLogger().info( "Load missing file " + thirdPartyDescriptor );

                    // load the missing file
                    unsafeMappings.load( thirdPartyDescriptor );
                }

                for ( String id : unsafeProjects.keySet() )
                {

                    if ( unsafeMappings.containsKey( id ) )
                    {

                        String license = (String) unsafeMappings.get( id );
                        if ( StringUtils.isEmpty( license ) )
                        {

                            // empty license means not fill, skip it
                            continue;
                        }

                        // found a resolved unsafe dependency in the missing third party file
                        MavenProject resolvedProject = unsafeProjects.get( id );
                        unsafeDependencies.remove( resolvedProject );

                        // push back to
                        result.put( id, license.trim() );
                        
                        License l = new License();
                        l.setName( license.trim() );
                        l.setUrl( license.trim() );
                        licenseMap.addLicense( resolvedProject, Arrays.asList( l ) );
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public File getThirdPartyDescriptor( MavenProject project, ArtifactRepository localRepository,
                                         List<ArtifactRepository> repositories )
        throws ThirdPartyToolException
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "The parameter 'project' can not be null" );
        }
        if ( localRepository == null )
        {
            throw new IllegalArgumentException( "The parameter 'localRepository' can not be null" );
        }
        if ( repositories == null )
        {
            throw new IllegalArgumentException( "The parameter 'remoteArtifactRepositories' can not be null" );
        }

        try
        {
            return resolveThirdPartyDescriptor( project, localRepository, repositories );
        }
        catch ( ArtifactNotFoundException e )
        {
            getLogger().debug( "ArtifactNotFoundException: Unable to locate third party descriptor: " + e );
            return null;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ThirdPartyToolException(
                "ArtifactResolutionException: Unable to locate third party descriptor: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ThirdPartyToolException(
                "IOException: Unable to locate third party descriptor: " + e.getMessage(), e );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param project         not null
     * @param localRepository not null
     * @param repositories    not null
     * @return the resolved site descriptor
     * @throws IOException                 if any
     * @throws ArtifactResolutionException if any
     * @throws ArtifactNotFoundException   if any
     */
    private File resolveThirdPartyDescriptor( MavenProject project, ArtifactRepository localRepository,
                                              List<ArtifactRepository> repositories )
        throws IOException, ArtifactResolutionException, ArtifactNotFoundException
    {
        File result;

        // TODO: this is a bit crude - proper type, or proper handling as metadata rather than an artifact in 2.1?
        Artifact artifact = artifactFactory.createArtifactWithClassifier( project.getGroupId(), project.getArtifactId(),
                                                                          project.getVersion(), "properties",
                                                                          "third-party" );

        boolean found = false;
        try
        {
            artifactResolver.resolve( artifact, repositories, localRepository );

            result = artifact.getFile();

            // we use zero length files to avoid re-resolution (see below)
            if ( result.length() > 0 )
            {
                found = true;
            }
            else
            {
                getLogger().debug( "Skipped third party descriptor" );
            }
        }
        catch ( ArtifactNotFoundException e )
        {
            getLogger().debug( "Unable to locate third party files descriptor : " + e );

            // we can afford to write an empty descriptor here as we don't expect it to turn up later in the remote
            // repository, because the parent was already released (and snapshots are updated automatically if changed)
            result = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            result.getParentFile().mkdirs();
            result.createNewFile();
        }

        return result;
    }
}
