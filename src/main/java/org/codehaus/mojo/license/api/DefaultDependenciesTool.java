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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * Default implementation of the {@link DependenciesTool}.
 *
 * @author tchemit dev@tchemit.fr
 * @version $Id$
 * @since 1.0
 */
@Component( role = DependenciesTool.class, hint = "default" )
public class DefaultDependenciesTool
    extends AbstractLogEnabled
    implements DependenciesTool
{

    /**
     * Message used when an invalid expression pattern is found.
     */
    public static final String INVALID_PATTERN_MESSAGE =
        "The pattern specified by expression <%s> seems to be invalid.";

    /**
     * Project builder.
     */
    @Requirement
    private MavenProjectBuilder mavenProjectBuilder;

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private ArtifactMetadataSource artifactMetadataSource;

    // CHECKSTYLE_OFF: MethodLength
    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> loadProjectDependencies( ResolvedProjectDependencies artifacts,
                                                                    MavenProjectDependenciesConfigurator configuration,
                                                                    ArtifactRepository localRepository,
                                                                    List<ArtifactRepository> remoteRepositories,
                                                                    SortedMap<String, MavenProject> cache )
    {

        boolean haveNoIncludedGroups = StringUtils.isEmpty( configuration.getIncludedGroups() );
        boolean haveNoIncludedArtifacts = StringUtils.isEmpty( configuration.getIncludedArtifacts() );
        boolean excludeTransitiveDependencies = configuration.isExcludeTransitiveDependencies();

        boolean haveExcludedGroups = StringUtils.isNotEmpty( configuration.getExcludedGroups() );
        boolean haveExcludedArtifacts = StringUtils.isNotEmpty( configuration.getExcludedArtifacts() );
        boolean haveExclusions = haveExcludedGroups || haveExcludedArtifacts;

        Pattern includedGroupPattern = null;
        Pattern includedArtifactPattern = null;
        Pattern excludedGroupPattern = null;
        Pattern excludedArtifactPattern = null;

        if ( !haveNoIncludedGroups )
        {
            includedGroupPattern = Pattern.compile( configuration.getIncludedGroups() );
        }
        if ( !haveNoIncludedArtifacts )
        {
            includedArtifactPattern = Pattern.compile( configuration.getIncludedArtifacts() );
        }
        if ( haveExcludedGroups )
        {
            excludedGroupPattern = Pattern.compile( configuration.getExcludedGroups() );
        }
        if ( haveExcludedArtifacts )
        {
            excludedArtifactPattern = Pattern.compile( configuration.getExcludedArtifacts() );
        }

        Set<?> depArtifacts;

        if ( configuration.isIncludeTransitiveDependencies() )
        {
            // All project dependencies
            depArtifacts = artifacts.getAllDependencies();
        }
        else
        {
            // Only direct project dependencies
            depArtifacts = artifacts.getDirectDependencies();
        }

        List<String> includedScopes = configuration.getIncludedScopes();
        List<String> excludeScopes = configuration.getExcludedScopes();
        List<String> includedTypes = configuration.getIncludedTypes();
        List<String> excludeTypes = configuration.getExcludedTypes();

        boolean verbose = configuration.isVerbose();

        SortedMap<String, MavenProject> result = new TreeMap<>();

        Map<String, Artifact> excludeArtifacts = new HashMap<>();
        Map<String, Artifact> includeArtifacts = new HashMap<>();

        SortedMap<String, MavenProject> localCache = new TreeMap<>();
        if ( cache != null )
        {
            localCache.putAll( cache );
        }

        for ( Object o : depArtifacts )
        {
            Artifact artifact = (Artifact) o;

            excludeArtifacts.put( artifact.getId(), artifact );

            if ( DefaultThirdPartyTool.LICENSE_DB_TYPE.equals( artifact.getType() ) )
            {
                // the special dependencies for license databases don't count.
                // Note that this will still see transitive deps of a license db; so using the build helper inside of
                // another project to make them will be noisy.
                continue;
            }

            String scope = artifact.getScope();
            if ( CollectionUtils.isNotEmpty( includedScopes ) && !includedScopes.contains( scope ) )
            {

                // not in included scopes
                continue;
            }

            if ( excludeScopes.contains( scope ) )
            {

                // in excluded scopes
                continue;
            }

            String type = artifact.getType();
            if ( CollectionUtils.isNotEmpty( includedTypes ) && !includedTypes.contains( type ) )
            {

                // not in included scopes
                continue;
            }

            if ( excludeTypes.contains( type ) )
            {

                // in excluded types
                continue;
            }

            Logger log = getLogger();

            String id = MojoHelper.getArtifactId( artifact );

            if ( verbose )
            {
                log.info( "detected artifact " + id );
            }

            // Check if the project should be included
            // If there is no specified artifacts and group to include, include all
            boolean isToInclude = haveNoIncludedArtifacts && haveNoIncludedGroups
                    || isIncludable( artifact, includedGroupPattern, includedArtifactPattern );

            // Check if the project should be excluded
            boolean isToExclude = isToInclude && haveExclusions
                    && isExcludable( artifact, excludedGroupPattern, excludedArtifactPattern );

            if ( !isToInclude || isToExclude )
            {
                if ( verbose )
                {
                    log.info( "skip artifact " + id );
                }
                continue;
            }

            MavenProject depMavenProject;

            // try to get project from cache
            depMavenProject = localCache.get( id );

            if ( depMavenProject != null )
            {
                if ( verbose )
                {
                    log.info( "add dependency [" + id + "] (from cache)" );
                }
            }
            else
            {
                // build project

                try
                {
                    depMavenProject =
                        mavenProjectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository, true );
                    depMavenProject.getArtifact().setScope( artifact.getScope() );

                    // In case maven-metadata.xml has different artifactId, groupId or version.
                    if ( !depMavenProject.getGroupId().equals( artifact.getGroupId() ) )
                    {
                        depMavenProject.setGroupId( artifact.getGroupId() );
                        depMavenProject.getArtifact().setGroupId( artifact.getGroupId() );
                    }
                    if ( !depMavenProject.getArtifactId().equals( artifact.getArtifactId() ) )
                    {
                        depMavenProject.setArtifactId( artifact.getArtifactId() );
                        depMavenProject.getArtifact().setArtifactId( artifact.getArtifactId() );
                    }
                    if ( !depMavenProject.getVersion().equals( artifact.getVersion() ) )
                    {
                        depMavenProject.setVersion( artifact.getVersion() );
                        depMavenProject.getArtifact().setVersion( artifact.getVersion() );
                    }
                }
                catch ( ProjectBuildingException e )
                {
                    log.warn( "Unable to obtain POM for artifact : " + artifact, e );
                    continue;
                }

                if ( verbose )
                {
                    log.info( "add dependency [" + id + "]" );
                }

                // store it also in cache
                localCache.put( id, depMavenProject );
            }

            // keep the project
            result.put( id, depMavenProject );

            excludeArtifacts.remove( artifact.getId() );
            includeArtifacts.put( artifact.getId(), artifact );
        }

        // exclude artifacts from the result that contain excluded artifacts in the dependency trail
        if ( excludeTransitiveDependencies )
        {
            for ( Map.Entry<String, Artifact> entry : includeArtifacts.entrySet() )
            {
                List<String> dependencyTrail = entry.getValue().getDependencyTrail();

                boolean remove = false;

                for ( int i = 1; i < dependencyTrail.size() - 1; i++ )
                {
                    if ( excludeArtifacts.containsKey( dependencyTrail.get( i ) ) )
                    {
                        remove = true;
                        break;
                    }
                }

                if ( remove )
                {
                    result.remove( MojoHelper.getArtifactId( entry.getValue() ) );
                }
            }
        }

        if ( cache != null )
        {
            cache.putAll( result );
        }

        return result;
    }
    // CHECKSTYLE_ON: MethodLength

    /**
     * {@inheritDoc}
     */
    public ResolvedProjectDependencies loadProjectArtifacts( ArtifactRepository localRepository,
            List remoteRepositories, MavenProject project, List<MavenProject> reactorProjects )
        throws DependenciesToolException

    {
        Map<String, MavenProject> idToReactorProject = new HashMap<>();
        if ( reactorProjects != null )
        {
            for ( MavenProject reactorProject : reactorProjects )
            {
                idToReactorProject.put( String.format( "%s:%s", reactorProject.getGroupId(),
                        reactorProject.getArtifactId() ), reactorProject );
            }
        }

        /*
         * Find the list of dependencies to resolve transitively. Some projects may be in the reactor.
         * Reactor projects can't be resolved by the artifact resolver yet.
         * In order to still get the complete dependency tree for the project, we will add the transitive
         * dependencies of the reactor project to the list of dependencies to resolve.
         * Since the transitive dependencies could
         * also be reactor projects, we need to repeat this check for each of those.
         * Note that since the dependency reactor
         * project may specify its own list of repositories,
         * we need to keep track of which project the transitive dependency is declared in.
         */
        List<Dependency> directDependencies = new ArrayList<>( project.getDependencies() );
        Map<MavenProject, List<Dependency>> reactorProjectToTransitiveDependencies = new HashMap<>();
        Queue<Pair<MavenProject, Dependency>> dependenciesToCheck = new ArrayDeque<>();
        for ( Dependency dependency : directDependencies )
        {
            dependenciesToCheck.add( Pair.of( project, dependency ) );
        }
        if ( reactorProjects != null )
        {
            while ( !dependenciesToCheck.isEmpty() )
            {
                Pair<MavenProject, Dependency> pair = dependenciesToCheck.remove();
                Dependency dependency = pair.getRight();
                String id = String.format( "%s:%s", dependency.getGroupId(), dependency.getArtifactId() );
                MavenProject dependencyReactorProject = idToReactorProject.get( id );
                if ( dependencyReactorProject != null )
                {
                    /*
                     * Since the project is in the reactor, the artifact resolver may not be able to resolve
                     * the artifact plus transitive dependencies yet. In order to still get the
                     * complete dependency tree for the project, we will add the transitive
                     * dependencies of the reactor project to the list of dependencies to resolve.
                     * Since the transitive dependencies could
                     * also be reactor projects, we need to repeat this check for each of those.
                     * Note that since the dependency reactor
                     * project may specify its own list of repositories,
                     * we need to keep track of which project the transitive dependency is
                     * declared in.
                     */
                    for ( Dependency transitiveDependency
                            : ( List<Dependency> ) dependencyReactorProject.getDependencies() )
                    {
                        dependenciesToCheck.add( Pair.of( dependencyReactorProject, transitiveDependency ) );
                    }
                }
                if ( !directDependencies.contains( dependency ) )
                {
                    List<Dependency> transitiveForSameProject =
                            reactorProjectToTransitiveDependencies.get( pair.getLeft() );
                    if ( transitiveForSameProject == null )
                    {
                        transitiveForSameProject = new ArrayList<>();
                        reactorProjectToTransitiveDependencies.put( pair.getLeft(), transitiveForSameProject );
                    }
                    transitiveForSameProject.add( dependency );
                }
            }
        }

        //Create artifacts for all dependencies,
        //keep the transitive dependencies grouped by project they are declared in
        Set<Artifact> directDependencyArtifacts = createDependencyArtifacts( project, directDependencies );
        Map<MavenProject, Set<Artifact>> reactorProjectToDependencyArtifacts = new HashMap<>();
        for ( Entry<MavenProject, List<Dependency>> entry : reactorProjectToTransitiveDependencies.entrySet() )
        {
            reactorProjectToDependencyArtifacts.put( entry.getKey(),
                    createDependencyArtifacts( entry.getKey(), entry.getValue() ) );
        }

        //Resolve artifacts. Transitive dependencies are resolved with the settings of the POM they are declared in.
        //Skip reactor projects, since they can't necessarily be resolved yet.
        //The transitive handling above ensures we still get a complete list of dependencies.
        Set<Artifact> reactorArtifacts = new HashSet<>();
        Set<Artifact> directArtifactsToResolve = new HashSet<>();
        if ( reactorProjects == null )
        {
            directArtifactsToResolve.addAll( directDependencyArtifacts );
        }
        else
        {
            partitionByIsReactorProject( directDependencyArtifacts, reactorArtifacts,
                    directArtifactsToResolve, idToReactorProject.keySet() );
            for ( Entry<MavenProject, Set<Artifact>> entry : reactorProjectToDependencyArtifacts.entrySet() )
            {
                Set<Artifact> nonReactorArtifacts = new HashSet<>();
                partitionByIsReactorProject( entry.getValue(), reactorArtifacts,
                        nonReactorArtifacts, idToReactorProject.keySet() );
                entry.setValue( nonReactorArtifacts );
            }
        }
        Set<Artifact> allDependencies = new HashSet<>( reactorArtifacts );
        allDependencies.addAll( resolve( directArtifactsToResolve, project.getArtifact(), localRepository,
                remoteRepositories ).getArtifacts() );
        for ( Entry<MavenProject, Set<Artifact>> entry : reactorProjectToDependencyArtifacts.entrySet() )
        {
            MavenProject reactorProject = entry.getKey();
            Set<Artifact> toResolve = entry.getValue();
            Artifact reactorProjectArtifact = reactorProject.getArtifact();
            List<ArtifactRepository> reactorRemoteRepositories = reactorProject.getRemoteArtifactRepositories();
            allDependencies.addAll(
                    resolve( toResolve, reactorProjectArtifact, localRepository,
                            reactorRemoteRepositories ).getArtifacts() );
        }

        return new ResolvedProjectDependencies( allDependencies, directDependencyArtifacts );
    }

    private Set<Artifact> createDependencyArtifacts( MavenProject project, List<Dependency> dependencies )
            throws DependenciesToolException
    {
        try
        {
            return MavenMetadataSource.createArtifacts( artifactFactory, dependencies, null, null, project );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new DependenciesToolException( e );
        }
    }

    private void partitionByIsReactorProject( Set<Artifact> artifacts, Set<Artifact> reactorArtifacts,
            Set<Artifact> nonReactorArtifacts, Set<String> reactorProjectIds )
    {
        for ( Artifact dependencyArtifact : artifacts )
        {
                String artifactKey = String.format( "%s:%s", dependencyArtifact.getGroupId(),
                        dependencyArtifact.getArtifactId() );
                if ( reactorProjectIds.contains( artifactKey ) )
                {
                    reactorArtifacts.add( dependencyArtifact );
                }
                else
                {
                    nonReactorArtifacts.add( dependencyArtifact );
                }
            }
    }

    private ArtifactResolutionResult resolve( Set<Artifact> artifacts, Artifact projectArtifact,
            ArtifactRepository localRepository, List remoteRepositories )
        throws DependenciesToolException
    {
        try
        {
            return artifactResolver.resolveTransitively( artifacts, projectArtifact, remoteRepositories,
                                                           localRepository, artifactMetadataSource );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new DependenciesToolException( e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new DependenciesToolException( e );
            }
    }

    /**
     * Tests if the given project is includeable against a groupdId pattern and a artifact pattern.
     *
     * @param project                 the project to test
     * @param includedGroupPattern    the include group pattern
     * @param includedArtifactPattern the include artifact pattenr
     * @return {@code true} if the project is includavble, {@code false} otherwise
     */
    protected boolean isIncludable( Artifact project, Pattern includedGroupPattern, Pattern includedArtifactPattern )
    {

        Logger log = getLogger();

        // check if the groupId of the project should be included
        if ( includedGroupPattern != null )
        {
            // we have some defined license filters
            try
            {
                Matcher matchGroupId = includedGroupPattern.matcher( project.getGroupId() );
                if ( matchGroupId.find() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Include " + project.getGroupId() );
                    }
                    return true;
                }
            }
            catch ( PatternSyntaxException e )
            {
                log.warn( String.format( INVALID_PATTERN_MESSAGE, includedGroupPattern.pattern() ) );
            }
        }

        // check if the artifactId of the project should be included
        if ( includedArtifactPattern != null )
        {
            // we have some defined license filters
            try
            {
                Matcher matchGroupId = includedArtifactPattern.matcher( project.getArtifactId() );
                if ( matchGroupId.find() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Include " + project.getArtifactId() );
                    }
                    return true;
                }
            }
            catch ( PatternSyntaxException e )
            {
                log.warn( String.format( INVALID_PATTERN_MESSAGE, includedArtifactPattern.pattern() ) );
            }
        }
        return false;
    }

    /**
     * Tests if the given project is excludable against a groupdId pattern and a artifact pattern.
     *
     * @param project                 the project to test
     * @param excludedGroupPattern    the exlcude group pattern
     * @param excludedArtifactPattern the exclude artifact pattenr
     * @return {@code true} if the project is excludable, {@code false} otherwise
     */
    protected boolean isExcludable( Artifact project, Pattern excludedGroupPattern, Pattern excludedArtifactPattern )
    {

        Logger log = getLogger();

        // check if the groupId of the project should be included
        if ( excludedGroupPattern != null )
        {
            // we have some defined license filters
            try
            {
                Matcher matchGroupId = excludedGroupPattern.matcher( project.getGroupId() );
                if ( matchGroupId.find() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Exclude " + project.getGroupId() );
                    }
                    return true;
                }
            }
            catch ( PatternSyntaxException e )
            {
                log.warn( String.format( INVALID_PATTERN_MESSAGE, excludedGroupPattern.pattern() ) );
            }
        }

        // check if the artifactId of the project should be included
        if ( excludedArtifactPattern != null )
        {
            // we have some defined license filters
            try
            {
                Matcher matchGroupId = excludedArtifactPattern.matcher( project.getArtifactId() );
                if ( matchGroupId.find() )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "Exclude " + project.getArtifactId() );
                    }
                    return true;
                }
            }
            catch ( PatternSyntaxException e )
            {
                log.warn( String.format( INVALID_PATTERN_MESSAGE, excludedArtifactPattern.pattern() ) );
            }
        }
        return false;
    }
}
