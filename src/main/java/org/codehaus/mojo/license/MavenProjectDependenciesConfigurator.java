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
package org.codehaus.mojo.license;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.api.DependenciesTool;

import java.util.List;
import java.util.SortedMap;

/**
 * Contract to configure which dependencies will be loaded by the dependency tool via the method
 * {@link DependenciesTool#loadProjectDependencies(MavenProject, MavenProjectDependenciesConfigurator, ArtifactRepository, List, SortedMap)}
 *
 * @author tchemit <chemit@codelutin.com>
 * @see DependenciesTool
 * @since 1.0
 */
public interface MavenProjectDependenciesConfigurator
{

    /**
     * @return {@code true} if should include transitive dependencies, {@code false} to include only direct
     *         dependencies.
     */
    boolean isIncludeTransitiveDependencies();

    /**
     * @return list of scopes to include while loading dependencies, if {@code null} is setted, then include all scopes.
     */
    List<String> getIncludedScopes();

    /**
     * @return list of scopes to exclude while loading dependencies, if {@code null} is setted, then include all scopes.
     */
    List<String> getExcludedScopes();

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
     * @return a pattern to exclude dependencies by their {@code groupId}, if {@code null} is setted then no exclude
     *         is done on groupId.
     */
    String getExcludedArtifacts();

    /**
     * @return {@code true} if verbose mode is on, {@code false} otherwise.
     */
    boolean isVerbose();
}
