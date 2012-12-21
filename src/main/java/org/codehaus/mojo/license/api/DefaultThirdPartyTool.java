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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of the third party tool.
 *
 * @author <a href="mailto:tchemit@codelutin.com">Tony Chemit</a>
 * @version $Id$
 * @plexus.component role="org.codehaus.mojo.license.api.ThirdPartyTool" role-hint="default"
 */
public class DefaultThirdPartyTool
    extends AbstractLogEnabled
    implements ThirdPartyTool
{
    /**
     * Classifier of the third-parties descriptor attached to a maven module.
     */
    public static final String DESCRIPTOR_CLASSIFIER = "third-party";

    /**
     * Type of the the third-parties descriptor attached to a maven module.
     */
    public static final String DESCRIPTOR_TYPE = "properties";

    /**
     * Pattern of a GAV plus a type.
     */
    private static final Pattern GAV_PLUS_TYPE_PATTERN = Pattern.compile( "(.+)--(.+)--(.+)--(.+)" );

    /**
     * Pattenr of a GAV plus a type plus a classifier.
     */
    private static final Pattern GAV_PLUS_TYPE_AND_CLASSIFIER_PATTERN =
        Pattern.compile( "(.+)--(.+)--(.+)--(.+)--(.+)" );

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
     * Maven ProjectHelper.
     *
     * @plexus.requirement
     */
    private MavenProjectHelper projectHelper;

    /**
     * @plexus.requirement
     */
    private FreeMarkerHelper freeMarkerHelper;

    /**
     * Maven project comparator.
     */
    private final Comparator<MavenProject> projectComparator = MojoHelper.newMavenProjectComparator();

    /**
     * {@inheritDoc}
     */
    public void attachThirdPartyDescriptor( MavenProject project, File file )
    {
        projectHelper.attachArtifact( project, DESCRIPTOR_TYPE, DESCRIPTOR_CLASSIFIER, file );
    }

    /**
     * {@inheritDoc}
     */
    public SortedSet<MavenProject> getProjectsWithNoLicense( LicenseMap licenseMap, boolean doLog )
    {

        Logger log = getLogger();

        // get unsafe dependencies (says with no license)
        SortedSet<MavenProject> unsafeDependencies = licenseMap.get( LicenseMap.UNKNOWN_LICENSE_MESSAGE );

        if ( doLog )
        {
            if ( CollectionUtils.isEmpty( unsafeDependencies ) )
            {
                log.debug( "There is no dependency with no license from poms." );
            }
            else
            {
                log.debug( "There is " + unsafeDependencies.size() + " dependencies with no license from poms : " );
                for ( MavenProject dep : unsafeDependencies )
                {

                    // no license found for the dependency
                    log.debug( " - " + MojoHelper.getArtifactId( dep.getArtifact() ) );
                }
            }
        }

        return unsafeDependencies;
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadThirdPartyDescriptorsForUnsafeMapping( String encoding,
                                                                       Collection<MavenProject> projects,
                                                                       SortedSet<MavenProject> unsafeDependencies,
                                                                       LicenseMap licenseMap,
                                                                       ArtifactRepository localRepository,
                                                                       List<ArtifactRepository> remoteRepositories )
        throws ThirdPartyToolException, IOException
    {

        SortedProperties result = new SortedProperties( encoding );
        Map<String, MavenProject> unsafeProjects = new HashMap<String, MavenProject>();
        for ( MavenProject unsafeDependency : unsafeDependencies )
        {
            String id = MojoHelper.getArtifactId( unsafeDependency.getArtifact() );
            unsafeProjects.put( id, unsafeDependency );
        }

        for ( MavenProject mavenProject : projects )
        {

            if ( CollectionUtils.isEmpty( unsafeDependencies ) )
            {

                // no more unsafe dependencies to find
                break;
            }

            File thirdPartyDescriptor = resolvThirdPartyDescriptor(mavenProject, localRepository, remoteRepositories);

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

                        addLicense( licenseMap, resolvedProject, license );
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public File resolvThirdPartyDescriptor( MavenProject project, ArtifactRepository localRepository,
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

    /**
     * {@inheritDoc}
     */
    public void addLicense( LicenseMap licenseMap, MavenProject project, String licenseName )
    {
        License license = new License();
        license.setName( licenseName.trim() );
        license.setUrl( licenseName.trim() );
        addLicense( licenseMap, project, license );
    }

    /**
     * {@inheritDoc}
     */
    public void addLicense( LicenseMap licenseMap, MavenProject project, License license )
    {
        addLicense( licenseMap, project, Arrays.asList( license ) );
    }

    /**
     * {@inheritDoc}
     */
    public void addLicense( LicenseMap licenseMap, MavenProject project, List<?> licenses )
    {

        if ( Artifact.SCOPE_SYSTEM.equals( project.getArtifact().getScope() ) )
        {

            // do NOT treate system dependency
            return;
        }

        if ( CollectionUtils.isEmpty( licenses ) )
        {

            // no license found for the dependency
            licenseMap.put( LicenseMap.UNKNOWN_LICENSE_MESSAGE, project );
            return;
        }

        for ( Object o : licenses )
        {
            String id = MojoHelper.getArtifactId( project.getArtifact() );
            if ( o == null )
            {
                getLogger().warn( "could not acquire the license for " + id );
                continue;
            }
            License license = (License) o;
            String licenseKey = license.getName();

            // tchemit 2010-08-29 Ano #816 Check if the License object is well formed

            if ( StringUtils.isEmpty( license.getName() ) )
            {
                getLogger().warn( "No license name defined for " + id );
                licenseKey = license.getUrl();
            }

            if ( StringUtils.isEmpty( licenseKey ) )
            {
                getLogger().warn( "No license url defined for " + id );
                licenseKey = LicenseMap.UNKNOWN_LICENSE_MESSAGE;
            }
            licenseMap.put( licenseKey, project );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mergeLicenses( LicenseMap licenseMap, String... licenses )
    {
        if ( licenses.length == 0 )
        {
            return;
        }

        String mainLicense = licenses[0].trim();
        SortedSet<MavenProject> mainSet = licenseMap.get( mainLicense );
        if ( mainSet == null )
        {
            getLogger().warn( "No license [" + mainLicense + "] found, will create it." );
            mainSet = new TreeSet<MavenProject>( projectComparator );
            licenseMap.put( mainLicense, mainSet );
        }
        int size = licenses.length;
        for ( int i = 1; i < size; i++ )
        {
            String license = licenses[i].trim();
            SortedSet<MavenProject> set = licenseMap.get( license );
            if ( set == null )
            {
                getLogger().warn( "No license [" + license + "] found, skip this merge." );
                continue;
            }
            getLogger().info( "Merge license [" + license + "] (" + set.size() + " depedencies)." );
            mainSet.addAll( set );
            set.clear();
            licenseMap.remove( license );
        }
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadUnsafeMapping( LicenseMap licenseMap, SortedMap<String, MavenProject> artifactCache,
                                               String encoding, File missingFile )
        throws IOException
    {
        SortedSet<MavenProject> unsafeDependencies = getProjectsWithNoLicense( licenseMap, false );

        SortedProperties unsafeMappings = new SortedProperties( encoding );

        if ( missingFile.exists() )
        {
            // there is some unsafe dependencies

            getLogger().info( "Load missing file " + missingFile );

            // load the missing file
            unsafeMappings.load( missingFile );
        }

        // get from the missing file, all unknown dependencies
        List<String> unknownDependenciesId = new ArrayList<String>();

        // coming from maven-license-plugin, we used in id type and classifier, now we remove
        // these informations since GAV is good enough to qualify a license of any artifact of it...
        Map<String, String> migrateKeys = migrateMissingFileKeys( unsafeMappings.keySet() );

        for ( Object o : migrateKeys.keySet() )
        {
            String id = (String) o;
            String migratedId = migrateKeys.get( id );

            MavenProject project = artifactCache.get( migratedId );
            if ( project == null )
            {
                // now we are sure this is a unknown dependency
                unknownDependenciesId.add( id );
            }
            else
            {
                if ( !id.equals( migratedId ) )
                {

                    // migrates id to migratedId
                    getLogger().info( "Migrates [" + id + "] to [" + migratedId + "] in the missing file." );
                    Object value = unsafeMappings.get( id );
                    unsafeMappings.remove( id );
                    unsafeMappings.put( migratedId, value );
                }
            }
        }

        if ( !unknownDependenciesId.isEmpty() )
        {

            // there is some unknown dependencies in the missing file, remove them
            for ( String id : unknownDependenciesId )
            {
                getLogger().warn(
                    "dependency [" + id + "] does not exist in project, remove it from the missing file." );
                unsafeMappings.remove( id );
            }

            unknownDependenciesId.clear();
        }

        // push back loaded dependencies
        for ( Object o : unsafeMappings.keySet() )
        {
            String id = (String) o;

            MavenProject project = artifactCache.get( id );
            if ( project == null )
            {
                getLogger().warn( "dependency [" + id + "] does not exist in project." );
                continue;
            }

            String license = (String) unsafeMappings.get( id );
            if ( StringUtils.isEmpty( license ) )
            {

                // empty license means not fill, skip it
                continue;
            }

            // add license in map
            addLicense( licenseMap, project, license );

            // remove unknown license
            unsafeDependencies.remove( project );
        }

        if ( unsafeDependencies.isEmpty() )
        {

            // no more unknown license in map
            licenseMap.remove( LicenseMap.UNKNOWN_LICENSE_MESSAGE );
        }
        else
        {

            // add a "with no value license" for missing dependencies
            for ( MavenProject project : unsafeDependencies )
            {
                String id = MojoHelper.getArtifactId( project.getArtifact() );
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "dependency [" + id + "] has no license, add it in the missing file." );
                }
                unsafeMappings.setProperty( id, "" );
            }
        }
        return unsafeMappings;
    }

    /**
     * {@inheritDoc}
     */
    public void writeThirdPartyFile( LicenseMap licenseMap, File thirdPartyFile, boolean verbose, String encoding,
                                     String lineFormat )
        throws IOException
    {

        Logger log = getLogger();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "licenseMap", licenseMap.entrySet() );
        properties.put( "dependencyMap", licenseMap.toDependencyMap().entrySet() );
        String content = freeMarkerHelper.renderTemplate( lineFormat, properties );

        log.info( "Writing third-party file to " + thirdPartyFile );
        if ( verbose )
        {
            log.info( content );
        }

        FileUtil.writeString( thirdPartyFile, content, encoding );

    }

    /**
     * {@inheritDoc}
     */
    public void writeBundleThirdPartyFile( File thirdPartyFile, File outputDirectory, String bundleThirdPartyPath )
        throws IOException
    {

        // creates the bundled license file
        File bundleTarget = FileUtil.getFile( outputDirectory, bundleThirdPartyPath );
        getLogger().info( "Writing bundled third-party file to " + bundleTarget );
        FileUtil.copyFile( thirdPartyFile, bundleTarget );
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
                                                                          project.getVersion(), DESCRIPTOR_TYPE,
                                                                          DESCRIPTOR_CLASSIFIER );
        try
        {
            artifactResolver.resolve( artifact, repositories, localRepository );

            result = artifact.getFile();

            // we use zero length files to avoid re-resolution (see below)
            if ( result.length() == 0 )
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

            FileUtil.createNewFile( result );
        }

        return result;
    }

    private Map<String, String> migrateMissingFileKeys( Set<Object> missingFileKeys )
    {
        Map<String, String> migrateKeys = new HashMap<String, String>();
        for ( Object object : missingFileKeys )
        {
            String id = (String) object;
            Matcher matcher;

            String newId = id;
            matcher = GAV_PLUS_TYPE_AND_CLASSIFIER_PATTERN.matcher( id );
            if ( matcher.matches() )
            {
                newId = matcher.group( 1 ) + "--" + matcher.group( 2 ) + "--" + matcher.group( 3 );

            }
            else
            {
                matcher = GAV_PLUS_TYPE_PATTERN.matcher( id );
                if ( matcher.matches() )
                {
                    newId = matcher.group( 1 ) + "--" + matcher.group( 2 ) + "--" + matcher.group( 3 );

                }
            }
            migrateKeys.put( id, newId );
        }
        return migrateKeys;
    }
}
