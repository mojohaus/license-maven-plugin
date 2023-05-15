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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.LicenseMojoUtils;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.mojo.license.utils.UrlRequester;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the third party tool.
 *
 * @author <a href="mailto:tchemit@codelutin.com">Tony Chemit</a>
 * @version $Id$
 */
@Component( role = ThirdPartyTool.class, hint = "default" )
public class DefaultThirdPartyTool
        implements ThirdPartyTool
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultThirdPartyTool.class );

    /**
     * Classifier of the third-parties descriptor attached to a maven module.
     */
    private static final String DESCRIPTOR_CLASSIFIER = "third-party";

    /**
     * Type of the the third-parties descriptor attached to a maven module.
     */
    private static final String DESCRIPTOR_TYPE = "properties";

    /**
     * Pattern of a GAV plus a type.
     */
    private static final Pattern GAV_PLUS_TYPE_PATTERN = Pattern.compile( "(.+)--(.+)--(.+)--(.+)" );

    /**
     * Pattern of a GAV plus a type plus a classifier.
     */
    private static final Pattern GAV_PLUS_TYPE_AND_CLASSIFIER_PATTERN =
            Pattern.compile( "(.+)--(.+)--(.+)--(.+)--(.+)" );

    public static final String LICENSE_DB_TYPE = "license.properties";

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    /**
     * Maven Artifact Resolver repoSystem
     */
    @Requirement
    private org.eclipse.aether.RepositorySystem aetherRepoSystem;

    @Requirement
    private MavenSession mavenSession;

    /**
     * Maven ProjectHelper.
     */
    @Requirement
    private MavenProjectHelper projectHelper;

    /**
     * freeMarker helper.
     */
    private FreeMarkerHelper freeMarkerHelper = FreeMarkerHelper.newDefaultHelper();

    /**
     * Maven project comparator.
     */
    private final Comparator<MavenProject> projectComparator = MojoHelper.newMavenProjectComparator();

    private boolean verbose;

    /**
     * {@inheritDoc}
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * {@inheritDoc}
     */
    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

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
    public SortedSet<MavenProject> getProjectsWithNoLicense( LicenseMap licenseMap )
    {

        // get unsafe dependencies (says with no license)
        SortedSet<MavenProject> unsafeDependencies = licenseMap.get( LicenseMap.UNKNOWN_LICENSE_MESSAGE );

        if ( CollectionUtils.isEmpty( unsafeDependencies ) )
        {
            LOG.debug( "There is no dependency with no license from poms." );
        }
        else
        {
            if ( LOG.isDebugEnabled() )
            {
                boolean plural = unsafeDependencies.size() > 1;
                String message = String.format( "There %s %d %s with no license from poms :",
                    plural ? "are" : "is",
                    unsafeDependencies.size(),
                    plural ? "dependencies" : "dependency" );
                LOG.debug( message );
                for ( MavenProject dep : unsafeDependencies )
                {

                    // no license found for the dependency
                    LOG.debug( " - {}", MojoHelper.getArtifactId( dep.getArtifact() ) );
                }
            }
        }

        return unsafeDependencies;
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadThirdPartyDescriptorsForUnsafeMapping( Set<Artifact> topLevelDependencies,
                                                                       String encoding,
                                                                       Collection<MavenProject> projects,
                                                                       SortedSet<MavenProject> unsafeDependencies,
                                                                       LicenseMap licenseMap,
                                                                       List<RemoteRepository> remoteRepositories )
            throws ThirdPartyToolException, IOException
    {

        SortedProperties result = new SortedProperties( encoding );
        Map<String, MavenProject> unsafeProjects = new HashMap<>();
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

            File thirdPartyDescriptor = resolvThirdPartyDescriptor( mavenProject, remoteRepositories );

            if ( thirdPartyDescriptor != null && thirdPartyDescriptor.exists() && thirdPartyDescriptor.length() > 0 )
            {

                LOG.info( "Detects third party descriptor {}", thirdPartyDescriptor );

                // there is a third party file detected form the given dependency
                SortedProperties unsafeMappings = new SortedProperties( encoding );

                if ( thirdPartyDescriptor.exists() )
                {

                    LOG.info( "Load missing file {}", thirdPartyDescriptor );

                    // load the missing file
                    unsafeMappings.load( thirdPartyDescriptor );
                }
                resolveUnsafe( unsafeDependencies, licenseMap, unsafeProjects, unsafeMappings, result );
            }

        }
        try
        {
            loadGlobalLicenses( topLevelDependencies, remoteRepositories, unsafeDependencies,
                                licenseMap, unsafeProjects, result );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new ThirdPartyToolException( "Failed to load global licenses", e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ThirdPartyToolException( "Failed to load global licenses", e );
        }
        return result;
    }

    private void resolveUnsafe( SortedSet<MavenProject> unsafeDependencies, LicenseMap licenseMap,
                                Map<String, MavenProject> unsafeProjects, SortedProperties unsafeMappings,
                                SortedProperties result )
    {
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

    /**
     * {@inheritDoc}
     */
    public File resolvThirdPartyDescriptor( MavenProject project, List<RemoteRepository> remoteRepositories )
            throws ThirdPartyToolException
    {
        if ( project == null )
        {
            throw new IllegalArgumentException( "The parameter 'project' can not be null" );
        }
        if ( remoteRepositories == null )
        {
            throw new IllegalArgumentException( "The parameter 'remoteRepositories' can not be null" );
        }

        try
        {
            return resolveThirdPartyDescriptor( project, remoteRepositories );
        }
        catch ( ArtifactNotFoundException e )
        {
            LOG.debug( "ArtifactNotFoundException: Unable to locate third party descriptor", e );
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
    public void addLicense( LicenseMap licenseMap, MavenProject project, String... licenseNames )
    {
        List<License> licenses = new ArrayList<>();
        for ( String licenseName : licenseNames )
        {
            License license = new License();
            license.setName( licenseName.trim() );
            license.setUrl( licenseName.trim() );
            licenses.add( license );
        }
        addLicense( licenseMap, project, licenses );
    }

    /**
     * {@inheritDoc}
     */
    public void addLicense( LicenseMap licenseMap, MavenProject project, License license )
    {
        addLicense( licenseMap, project, Collections.singletonList( license ) );
    }

    /**
     * {@inheritDoc}
     */
    public void addLicense( LicenseMap licenseMap, MavenProject project, List<?> licenses )
    {

        if ( Artifact.SCOPE_SYSTEM.equals( project.getArtifact().getScope() ) )
        {

            // do NOT treat system dependency
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
                LOG.warn( "could not acquire the license for {}", id );
                continue;
            }
            License license = (License) o;
            String licenseKey = license.getName();

            // tchemit 2010-08-29 Ano #816 Check if the License object is well formed

            if ( StringUtils.isEmpty( license.getName() ) )
            {
                LOG.warn( "The license for {} has no name (but exist)", id );
                licenseKey = license.getUrl();
            }

            if ( StringUtils.isEmpty( licenseKey ) )
            {
                LOG.warn( "No license url defined for {}", id );
                licenseKey = LicenseMap.UNKNOWN_LICENSE_MESSAGE;
            }
            licenseMap.put( licenseKey, project );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mergeLicenses( LicenseMap licenseMap, String mainLicense, Set<String> licenses )
    {

        if ( licenses.isEmpty() )
        {

            // nothing to merge, is this can really happen ?
            return;
        }

        SortedSet<MavenProject> mainSet = licenseMap.get( mainLicense );
        if ( mainSet == null )
        {
            if ( isVerbose() )
            {
                LOG.warn( "No license [{}] found, will create it.", mainLicense );
            }
            mainSet = new TreeSet<>( projectComparator );
            licenseMap.put( mainLicense, mainSet );
        }
        for ( String license : licenses )
        {
            SortedSet<MavenProject> set = licenseMap.get( license );
            if ( set == null )
            {
                if ( isVerbose() )
                {
                    LOG.warn( "No license [{}] found, skip the merge to [{}]", license, mainLicense );
                }
                continue;
            }
            if ( isVerbose() )
            {
                LOG.info(
                        "Merge license [{}] to [{}] ({} dependencies).", license, mainLicense, set.size() );
            }
            mainSet.addAll( set );
            set.clear();
            licenseMap.remove( license );
        }
    }

    /**
     * {@inheritDoc}
     */
    public SortedProperties loadUnsafeMapping( LicenseMap licenseMap,
                                               SortedMap<String, MavenProject> artifactCache,
                                               String encoding,
                                               File missingFile,
                                               String missingFileUrl ) throws IOException, MojoExecutionException
    {
        Map<String, MavenProject> snapshots = new HashMap<>();

        synchronized ( artifactCache )
        {
            //find snapshot dependencies
            for ( Map.Entry<String, MavenProject> entry : artifactCache.entrySet() )
            {
                MavenProject mavenProject = entry.getValue();
                if ( mavenProject.getVersion().endsWith( Artifact.SNAPSHOT_VERSION ) )
                {
                    snapshots.put( entry.getKey(), mavenProject );
                }
            }
        }

        for ( Map.Entry<String, MavenProject> snapshot : snapshots.entrySet() )
        {
            // remove invalid entries, which contain timestamps in key
            artifactCache.remove( snapshot.getKey() );
            // put corrected keys/entries into artifact cache
            MavenProject mavenProject = snapshot.getValue();

            String id = MojoHelper.getArtifactId( mavenProject.getArtifact() );
            artifactCache.put( id, mavenProject );

        }
        SortedSet<MavenProject> unsafeDependencies = getProjectsWithNoLicense( licenseMap );

        SortedProperties unsafeMappings = new SortedProperties( encoding );

        if ( missingFile.exists() )
        {
            // there is some unsafe dependencies

            LOG.info( "Load missingFile {}", missingFile );

            // load the missing file
            unsafeMappings.load( missingFile );
        }
        if ( UrlRequester.isStringUrl( missingFileUrl ) )
        {
            String httpRequestResult = UrlRequester.getFromUrl( missingFileUrl );
            unsafeMappings.load( new ByteArrayInputStream( httpRequestResult.getBytes() ) );
        }

        // get from the missing file, all unknown dependencies
        List<String> unknownDependenciesId = new ArrayList<>();

        // coming from maven-license-plugin, we used the full g/a/v/c/t. Now we remove classifier and type
        // since GAV is good enough to qualify a license of any artifact of it...
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
                    LOG.info( "Migrates [{}] to [{}] in the missing file.", id, migratedId );
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
                LOG.warn(
                        "dependency [{}] does not exist in project, remove it from the missing file.", id );
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
                LOG.warn( "dependency [{}] does not exist in project.", id );
                continue;
            }

            String license = (String) unsafeMappings.get( id );

            String[] licenses = StringUtils.split( license, '|' );

            if ( ArrayUtils.isEmpty( licenses ) )
            {

                // empty license means not fill, skip it
                continue;
            }

            // add license in map
            addLicense( licenseMap, project, licenses );

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
                LOG.debug( "dependency [{}] has no license, add it in the missing file.", id );
                unsafeMappings.setProperty( id, "" );
            }
        }
        return unsafeMappings;
    }

    /**
     * {@inheritDoc}
     */
    public void overrideLicenses( LicenseMap licenseMap, SortedMap<String, MavenProject> artifactCache, String encoding,
            String overrideUrl ) throws IOException
    {
        if ( LicenseMojoUtils.isValid( overrideUrl ) )
        {
            final SortedProperties overrideMappings = new SortedProperties( encoding );
            try ( Reader reader = new StringReader( UrlRequester.getFromUrl( overrideUrl, encoding ) ) )
            {
                overrideMappings.load( reader );
            }
            for ( Object o : overrideMappings.keySet() )
            {
                String id = (String) o;

                MavenProject project = artifactCache.get( id );
                if ( project == null )
                {
                    // Log at warn for local override files, but at debug for remote (presumably shared) override files.
                    String protocol = UrlRequester.findProtocol( overrideUrl );
                    if ( "http".equals( protocol ) || "https".equals( protocol ) )
                    {
                        LOG.debug( "dependency [{}] does not exist in project.", id );
                    }
                    else
                    {
                        LOG.warn( "dependency [{}] does not exist in project.", id );
                    }
                    continue;
                }

                String license = (String) overrideMappings.get( id );

                String[] licenses = StringUtils.split( license, '|' );

                if ( ArrayUtils.isEmpty( licenses ) )
                {

                    // empty license means not fill, skip it
                    continue;
                }

                licenseMap.removeProject( project );

                // add license in map
                addLicense( licenseMap, project, licenses );

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeThirdPartyFile( LicenseMap licenseMap, File thirdPartyFile, boolean verbose, String encoding,
                                     String lineFormat )
            throws IOException
    {

        Map<String, Object> properties = new HashMap<>();
        properties.put( "licenseMap", licenseMap.entrySet() );
        properties.put( "dependencyMap", licenseMap.toDependencyMap().entrySet() );
        String content = freeMarkerHelper.renderTemplate( lineFormat, properties );

        LOG.info( "Writing third-party file to " + thirdPartyFile );
        if ( verbose )
        {
            LOG.info( content );
        }

        FileUtil.printString( thirdPartyFile, content, encoding );

    }

    /**
     * {@inheritDoc}
     */
    public void writeBundleThirdPartyFile( File thirdPartyFile, File outputDirectory, String bundleThirdPartyPath )
            throws IOException
    {

        // creates the bundled license file
        File bundleTarget = FileUtil.getFile( outputDirectory, bundleThirdPartyPath );
        LOG.info( "Writing bundled third-party file to {}", bundleTarget );
        FileUtil.copyFile( thirdPartyFile, bundleTarget );
    }

    private void loadGlobalLicenses( Set<Artifact> dependencies, List<RemoteRepository> remoteRepositories,
            SortedSet<MavenProject> unsafeDependencies,
            LicenseMap licenseMap, Map<String, MavenProject> unsafeProjects,
            SortedProperties result )
            throws IOException, ArtifactNotFoundException, ArtifactResolutionException
    {
        for ( Artifact dep : dependencies )
        {
            if ( LICENSE_DB_TYPE.equals( dep.getType() ) )
            {
                loadOneGlobalSet( unsafeDependencies, licenseMap, unsafeProjects, dep, remoteRepositories,
                                  result );
            }
        }
    }

    private void loadOneGlobalSet( SortedSet<MavenProject> unsafeDependencies, LicenseMap licenseMap,
                                   Map<String, MavenProject> unsafeProjects, Artifact dep,
                                   List<RemoteRepository> remoteRepositories,
                                   SortedProperties result )
            throws IOException, ArtifactNotFoundException, ArtifactResolutionException
    {
        File propFile = resolveArtifact( dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType(),
                dep.getClassifier(), remoteRepositories );
        LOG.info(
                "Loading global license map from {}: {}", dep.toString(), propFile.getAbsolutePath() );
        SortedProperties props = new SortedProperties( "utf-8" );

        try ( InputStream propStream = new FileInputStream( propFile ) )
        {
            props.load( propStream );
        }
        catch ( IOException e )
        {
            throw new IOException( "Unable to load " + propFile.getAbsolutePath(), e );
        }

        for ( Object keyObj : props.keySet() )
        {
            String key = (String) keyObj;
            String val = (String) props.get( key );
            result.put( key, val );
        }

        resolveUnsafe( unsafeDependencies, licenseMap, unsafeProjects, props, result );
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
    private File resolveThirdPartyDescriptor( MavenProject project, List<RemoteRepository> remoteRepositories )
            throws IOException, ArtifactResolutionException, ArtifactNotFoundException
    {
        File result;
        try
        {
            result = resolveArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                      DESCRIPTOR_TYPE, DESCRIPTOR_CLASSIFIER, remoteRepositories );

            // we use zero length files to avoid re-resolution (see below)
            if ( result.length() == 0 )
            {
                LOG.debug( "Skipped third party descriptor" );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            if ( e.getCause() instanceof ArtifactNotFoundException )
            {
                ArtifactNotFoundException artifactNotFoundException = ( ArtifactNotFoundException ) e.getCause();
                LOG.debug( "Unable to locate third party files descriptor", artifactNotFoundException );

                org.eclipse.aether.artifact.Artifact artifact;
                if ( artifactNotFoundException.getArtifact() == null )
                {
                    artifact = new DefaultArtifact( project.getGroupId(), project.getArtifactId(),
                            DESCRIPTOR_CLASSIFIER, null, project.getVersion(),
                            new DefaultArtifactType( DESCRIPTOR_TYPE ) );
                }
                else
                {
                    artifact = artifactNotFoundException.getArtifact();
                }

                /*
                 * we can afford to write an empty descriptor here
                 * as we don't expect it to turn up later in the remote
                 * repository, because the parent was already released
                 * (and snapshots are updated automatically if changed)
                 */
                RepositorySystemSession aetherSession = mavenSession.getRepositorySession();
                result = new File( aetherSession.getLocalRepository().getBasedir(),
                        aetherSession.getLocalRepositoryManager().getPathForLocalArtifact( artifact ) );
            }
            else
            {
                throw e;
            }
        }

        return result;
    }

    public File resolveMissingLicensesDescriptor( String groupId, String artifactId, String version,
            List<RemoteRepository> remoteRepositories )
            throws IOException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return resolveArtifact( groupId, artifactId, version, DESCRIPTOR_TYPE,
                DESCRIPTOR_CLASSIFIER, remoteRepositories );
    }

    private File resolveArtifact( String groupId, String artifactId, String version,
            String type, String classifier, List<RemoteRepository> remoteRepositories )
                    throws ArtifactResolutionException
    {
        org.eclipse.aether.artifact.Artifact artifact2
                = new DefaultArtifact( groupId, artifactId, classifier, null,
                        version, new DefaultArtifactType( type ) );
        ArtifactRequest artifactRequest = new ArtifactRequest()
                .setArtifact( artifact2 )
                .setRepositories( remoteRepositories );
        ArtifactResult result = aetherRepoSystem.resolveArtifact( mavenSession.getRepositorySession(),
                artifactRequest );

        return result.getArtifact().getFile();
    }

    private Map<String, String> migrateMissingFileKeys( Set<Object> missingFileKeys )
    {
        Map<String, String> migrateKeys = new HashMap<>();
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
