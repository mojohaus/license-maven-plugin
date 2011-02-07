/*
 * #%L
 * License Maven Plugin
 *
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2010 - 2011 Codehaus
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.util.List;

/**
 * Contract of an object which contains everything to build Maven project dependencies.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public interface MavenProjectDependenciesLoader
{

    /**
     * @return the maven project
     */
    MavenProject getProject();

    /**
     * @return the maven project builder
     */
    MavenProjectBuilder getMavenProjectBuilder();

    /**
     * @return the local repository
     */
    ArtifactRepository getLocalRepository();

    /**
     * @return the list of remote repositories configured for the given project
     */
    List getRemoteRepositories();

    /**
     * @return {@code true} if should include transitive dependencies, {@code false} to include only direct
     *         dependencies.
     */
    boolean isIncludeTransitiveDependencies();

    /**
     * @return list of scopes to exclude while loading dependencies, if {@code null} is setted, then include all scopes.
     */
    List<String> getExcludeScopes();

    /**
     * @return a pattern to include dependencies by thier {@code artificatId}, if {@code null} is setted then include
     *         all artifacts.
     */
    String getIncludedArtifacts();

    /**
     * @return a pattern to include dependencies by their {@code groupId}, if {@code null} is setted then include
     *         all artifacts.
     */
    String getIncludedGroups();

    /**
     * @return a pattern to exclude dependencies by their {@code artifactId}, if {@code null} is setted the no exclude is
     *         done on artifactId.
     */
    String getExcludedGroups();

    /**
     * @return a pattern to exclude dependencies by theire {@code groupId}, if {@code null} is setted then no exclude
     *         is done on groupId.
     */
    String getExcludedArtifacts();

    boolean isVerbose();

}
