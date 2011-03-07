/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2010 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A helper for artifacts.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class ArtifactHelper
{

    protected static Comparator<MavenProject> projectComparator;

    public static final String INVALIDE_PATTERN_MESSAGE =
        "The pattern specified by expression <%s> seems to be invalid.";

    /**
     * Get the list of project dependencies after applying transitivity and filtering rules.
     * 
     * @param mojo
     * @param log
     * @param cache
     * @return
     */
    public static SortedMap<String, MavenProject> loadProjectDependencies( MavenProjectDependenciesLoader mojo, Log log,
                                                                           SortedMap<String, MavenProject> cache )
    {

        boolean haveNoIncludedGroups = StringUtils.isEmpty( mojo.getIncludedGroups() );
        boolean haveNoIncludedArtifacts = StringUtils.isEmpty( mojo.getIncludedArtifacts() );

        boolean haveExcludedGroups = StringUtils.isNotEmpty( mojo.getExcludedGroups() );
        boolean haveExcludedArtifacts = StringUtils.isNotEmpty( mojo.getExcludedArtifacts() );
        boolean haveExclusions = haveExcludedGroups || haveExcludedArtifacts;

        Pattern includedGroupPattern = null;
        Pattern includedArtifactPattern = null;
        Pattern excludedGroupPattern = null;
        Pattern excludedArtifactPattern = null;

        if ( !haveNoIncludedGroups )
        {
            includedGroupPattern = Pattern.compile( mojo.getIncludedGroups() );
        }
        if ( !haveNoIncludedArtifacts )
        {
            includedArtifactPattern = Pattern.compile( mojo.getIncludedArtifacts() );
        }
        if ( haveExcludedGroups )
        {
            excludedGroupPattern = Pattern.compile( mojo.getExcludedGroups() );
        }
        if ( haveExcludedArtifacts )
        {
            excludedArtifactPattern = Pattern.compile( mojo.getExcludedArtifacts() );
        }

        MavenProject project = mojo.getProject();

        Set<?> depArtifacts;

        if ( mojo.isIncludeTransitiveDependencies() )
        {
            // All project dependencies
            depArtifacts = project.getArtifacts();
        }
        else
        {
            // Only direct project dependencies
            depArtifacts = project.getDependencyArtifacts();
        }

        ArtifactRepository localRepository = mojo.getLocalRepository();
        List remoteRepositories = mojo.getRemoteRepositories();
        MavenProjectBuilder projectBuilder = mojo.getMavenProjectBuilder();

        List<String> excludeScopes = mojo.getExcludeScopes();

        boolean verbose = mojo.isVerbose();

        SortedMap<String, MavenProject> result = new TreeMap<String, MavenProject>();

        for ( Object o : depArtifacts )
        {
            Artifact artifact = (Artifact) o;

            if ( excludeScopes.contains( artifact.getScope() ) )
            {

                // never treate system artifacts (they are mysterious and
                // no information can be retrive from anywhere)...
                continue;
            }

            String id = getArtifactId( artifact );

            if ( verbose )
            {
                log.info( "detected artifact " + id );
            }

            // Check if the project should be included
            // If there is no specified artifacts and group to include, include all
            boolean isToInclude = haveNoIncludedArtifacts && haveNoIncludedGroups ||
                isIncludable( log, artifact, includedGroupPattern, includedArtifactPattern );

            // Check if the project should be excluded
            boolean isToExclude = isToInclude && haveExclusions &&
                isExcludable( log, artifact, excludedGroupPattern, excludedArtifactPattern );

            if ( !isToInclude || isToExclude )
            {
                if ( verbose )
                {
                    log.info( "skip artifact " + id );
                }
                continue;
            }

            MavenProject depMavenProject = null;

            if ( cache != null )
            {

                // try to get project from cache
                depMavenProject = cache.get( id );
            }

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
                        projectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository, true );
                }
                catch ( ProjectBuildingException e )
                {
                    log.warn( "Unable to obtain POM for artifact : " + artifact );
                    log.warn( e );
                    continue;
                }

                if ( verbose )
                {
                    log.info( "add dependency [" + id + "]" );
                }
                if ( cache != null )
                {

                    // store it also in cache
                    cache.put( id, depMavenProject );
                }
            }

            // keep the project
            result.put( id, depMavenProject );
        }

        return result;
    }

    public static String getArtifactId( Artifact artifact )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( artifact.getGroupId() );
        sb.append( "--" );
        sb.append( artifact.getArtifactId() );
        sb.append( "--" );
        sb.append( artifact.getVersion() );
        String type = artifact.getType();
        if ( !StringUtils.isEmpty( type ) && !"pom".equals( type ) )
        {
            sb.append( "--" );
            sb.append( artifact.getType() );
        }
        if ( !StringUtils.isEmpty( artifact.getClassifier() ) )
        {
            sb.append( "--" );
            sb.append( artifact.getClassifier() );
        }
        return sb.toString();
    }

    public static String getArtifactName( MavenProject project )
    {
        StringBuilder sb = new StringBuilder();
        if ( project.getName().startsWith( "Unnamed -" ) )
        {

            // as in Maven 3, let's use the artifact id
            sb.append( project.getArtifactId() );
        }
        else
        {
            sb.append( project.getName() );
        }
        sb.append( " (" );
        sb.append( project.getGroupId() );
        sb.append( ":" );
        sb.append( project.getArtifactId() );
        sb.append( ":" );
        sb.append( project.getVersion() );
        sb.append( " - " );
        String url = project.getUrl();
        sb.append( url == null ? "no url defined" : url );
        sb.append( ")" );

        return sb.toString();
    }

    public static Comparator<MavenProject> getProjectComparator()
    {
        if ( projectComparator == null )
        {
            projectComparator = new Comparator<MavenProject>()
            {
                public int compare( MavenProject o1, MavenProject o2 )
                {

                    String id1 = getArtifactId( o1.getArtifact() );
                    String id2 = getArtifactId( o2.getArtifact() );
                    return id1.compareTo( id2 );
                }
            };
        }
        return projectComparator;
    }

    protected static boolean isIncludable( Log log, Artifact project, Pattern includedGroupPattern,
                                           Pattern includedArtifactPattern )
    {

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
                log.warn( String.format( INVALIDE_PATTERN_MESSAGE, includedGroupPattern.pattern() ) );
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
                log.warn( String.format( INVALIDE_PATTERN_MESSAGE, includedArtifactPattern.pattern() ) );
            }
        }

        return false;
    }

    protected static boolean isExcludable( Log log, Artifact project, Pattern excludedGroupPattern,
                                           Pattern excludedArtifactPattern )
    {

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
                log.warn( String.format( INVALIDE_PATTERN_MESSAGE, excludedGroupPattern.pattern() ) );
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
                log.warn( String.format( INVALIDE_PATTERN_MESSAGE, excludedArtifactPattern.pattern() ) );
            }
        }

        return false;
    }
}
