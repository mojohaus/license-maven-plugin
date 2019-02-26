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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 * A tool to deal with dependencies of a project.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Component( role = DependenciesTool.class, hint = "default" )
public class DependenciesTool
extends AbstractLogEnabled
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
     * For a given {@code project}, obtain the universe of its dependencies after applying transitivity and
     * filtering rules given in the {@code configuration} object.
     *
     * Result is given in a map where keys are unique artifact id
     *
     * @param dependencies       the project dependencies
     * @param configuration      the configuration
     * @param localRepository    local repository used to resolv dependencies
     * @param remoteRepositories remote repositories used to resolv dependencies
     * @param cache              a optional cache where to keep resolved dependencies
     * @return the map of resolved dependencies indexed by their unique id.
     * @see MavenProjectDependenciesConfigurator
     */
    public SortedMap<String, MavenProject> loadProjectDependencies( ResolvedProjectDependencies artifacts,
                                                                    MavenProjectDependenciesConfigurator configuration,
                                                                    ArtifactRepository localRepository,
                                                                    List<ArtifactRepository> remoteRepositories,
                                                                    SortedMap<String, MavenProject> cache )
    {

        final ArtifactFilters artifactFilters = configuration.getArtifactFilters();

        final boolean excludeTransitiveDependencies = configuration.isExcludeTransitiveDependencies();

        final Set<Artifact> depArtifacts;

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

        boolean verbose = configuration.isVerbose();

        SortedMap<String, MavenProject> result = new TreeMap<>();

        Map<String, Artifact> excludeArtifacts = new HashMap<>();
        Map<String, Artifact> includeArtifacts = new HashMap<>();

        SortedMap<String, MavenProject> localCache = new TreeMap<>();
        if ( cache != null )
        {
            localCache.putAll( cache );
        }
        final Logger log = getLogger();

        for ( Artifact artifact : depArtifacts )
        {

            excludeArtifacts.put( artifact.getId(), artifact );

            if ( DefaultThirdPartyTool.LICENSE_DB_TYPE.equals( artifact.getType() ) )
            {
                // the special dependencies for license databases don't count.
                // Note that this will still see transitive deps of a license db; so using the build helper inside of
                // another project to make them will be noisy.
                continue;
            }

            if ( !artifactFilters.isIncluded( artifact ) )
            {
                if ( verbose )
                {
                    log.debug( "Excluding artifact " + artifact );
                }
                continue;
            }

            String id = MojoHelper.getArtifactId( artifact );

            if ( verbose )
            {
                log.info( "detected artifact " + id );
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
     * Load project artifacts.
     *
     * @param localRepository    local repository used to resolv dependencies
     * @param remoteRepositories remote repositories used to resolv dependencies
     * @param project            the project to scan
     * @param reactorProjectDependencies reactor projects. Optional, only relevant if there is more than one)
     * @return the loaded project dependency artifacts
     * @throws DependenciesToolException if could not load project dependencies
     */
    @SuppressWarnings( "unchecked" )
    public ResolvedProjectDependencies loadProjectArtifacts( ArtifactRepository localRepository,
            List<ArtifactRepository> remoteRepositories, MavenProject project, List<MavenProject> reactorProjects )
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

    @SuppressWarnings( "unchecked" )
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
            ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
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


}
