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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.mojo.license.model.Dependency;
import org.codehaus.mojo.license.utils.FileUtil;
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
    protected static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * {@inheritDoc}
     */
    public SortedMap<String, MavenProject> loadProjectDependencies( MavenProject project,
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
            depArtifacts = project.getArtifacts();
        }
        else
        {
            // Only direct project dependencies
            depArtifacts = project.getDependencyArtifacts();
        }

        List<String> includedScopes = configuration.getIncludedScopes();
        List<String> excludeScopes = configuration.getExcludedScopes();

        boolean verbose = configuration.isVerbose();

        SortedMap<String, MavenProject> result = new TreeMap<>();

        Map<String, Artifact> excludeArtifacts = new HashMap<>();
        Map<String, Artifact> includeArtifacts = new HashMap<>();

        SortedMap<String, MavenProject> localCache = new TreeMap<>();
        if (cache != null)
        {
            localCache.putAll(cache);
        }

        for ( Object o : depArtifacts )
        {
            Artifact artifact = (Artifact) o;

            excludeArtifacts.put(artifact.getId(), artifact);

            if ( DefaultThirdPartyTool.LICENSE_DB_TYPE.equals( artifact.getType() ) )
            {
                // the special dependencies for license databases don't count.
                // Note that this will still see transitive deps of a license db; so using the build helper inside of another project
                // to make them will be noisy.
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

            Logger log = getLogger();

            String id = MojoHelper.getArtifactId( artifact );

            if ( verbose )
            {
                log.info( "detected artifact " + id );
            }

            // Check if the project should be included
            // If there is no specified artifacts and group to include, include all
            boolean isToInclude = haveNoIncludedArtifacts && haveNoIncludedGroups ||
                isIncludable( artifact, includedGroupPattern, includedArtifactPattern );

            // Check if the project should be excluded
            boolean isToExclude = isToInclude && haveExclusions &&
                isExcludable( artifact, excludedGroupPattern, excludedArtifactPattern );

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
                localCache.put(id, depMavenProject);
            }

            // keep the project
            result.put(id, depMavenProject);

            excludeArtifacts.remove(artifact.getId());
            includeArtifacts.put(artifact.getId(), artifact);
        }

        // exclude artifacts from the result that contain excluded artifacts in the dependency trail
        if (excludeTransitiveDependencies) {
            for (Map.Entry<String, Artifact> entry : includeArtifacts.entrySet()) {
                List<String> dependencyTrail = entry.getValue().getDependencyTrail();

                boolean remove = false;

                for (int i = 1; i < dependencyTrail.size() - 1; i++) {
                    if (excludeArtifacts.containsKey(dependencyTrail.get(i))) {
                        remove = true;
                        break;
                    }
                }

                if (remove) {
                    result.remove(MojoHelper.getArtifactId(entry.getValue()));
                }
            }
        }

        if (cache != null) {
            cache.putAll(result);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void loadProjectArtifacts( ArtifactRepository localRepository, List remoteRepositories,
                                      MavenProject project , Map<String,List<org.apache.maven.model.Dependency>> reactorProjectDependencies )
        throws DependenciesToolException

    {

        if ( CollectionUtils.isEmpty( project.getDependencyArtifacts() ) )
        {

            Set dependenciesArtifacts;
            try
            {
                List<org.apache.maven.model.Dependency> dependencies = new ArrayList<org.apache.maven.model.Dependency>(project.getDependencies());
                if (reactorProjectDependencies!=null) {

                    for (org.apache.maven.model.Dependency dependency : new ArrayList<>(dependencies)) {
                        String id = String.format("%s:%s", dependency.getGroupId(), dependency.getArtifactId());
                        List<org.apache.maven.model.Dependency> projectDependencies = reactorProjectDependencies.get(id);
                        if (projectDependencies!=null) {
                            dependencies.remove(dependency);
                            dependencies.addAll(projectDependencies);
                        }
                    }
                }
                dependenciesArtifacts =
                    MavenMetadataSource.createArtifacts(artifactFactory, dependencies, null, null,
                                                        project );
            }
            catch ( InvalidDependencyVersionException e )
            {
                throw new DependenciesToolException( e );
            }
            project.setDependencyArtifacts( dependenciesArtifacts );


        }

        Artifact artifact = project.getArtifact();

        ArtifactResolutionResult result;
        try
        {
            result = artifactResolver.resolveTransitively( project.getDependencyArtifacts(), artifact, remoteRepositories,
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

        project.setArtifacts( result.getArtifacts() );

    }

    @Override
    public void writeThirdPartyDependenciesFile(File outputDirectory, String listedDependenciesFilePath, Set<Dependency> listedDependencies) throws IOException {
        final File thirdPartyDepsFile = FileUtil.getFile(outputDirectory, listedDependenciesFilePath);

        if (listedDependencies.isEmpty()) {
            getLogger().warn("There is no dependencies for write to " + thirdPartyDepsFile);
            return;
        }

        getLogger().info( "Writing third-party dependencies file to " + thirdPartyDepsFile );
        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(thirdPartyDepsFile, listedDependencies);
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
