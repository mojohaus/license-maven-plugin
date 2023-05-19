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
import java.util.Map;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.Dependency;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;

/**
 * A tool to deal with dependencies of a project.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public interface DependenciesTool
{

    /**
     * For a given {@code project}, obtain the universe of its dependencies after applying transitivity and
     * filtering rules given in the {@code configuration} object.
     * Result is given in a map where keys are unique artifact id
     *
     * @param project            the project to scann
     * @param configuration      the configuration
     * @param localRepository    local repository used to resolv dependencies
     * @param remoteRepositories remote repositories used to resolv dependencies
     * @param cache              a optional cache where to keep resolved dependencies
     * @return the map of resolved dependencies indexed by their unique id.
     * @see MavenProjectDependenciesConfigurator
     */
    SortedMap<String, MavenProject> loadProjectDependencies( MavenProject project,
                                                             MavenProjectDependenciesConfigurator configuration,
                                                             ArtifactRepository localRepository,
                                                             List<ArtifactRepository> remoteRepositories,
                                                             SortedMap<String, MavenProject> cache );

    /**
     * Load project artifacts.
     *
     * @param localRepository    local repository used to resolv dependencies
     * @param remoteRepositories remote repositories used to resolv dependencies
     * @param project            the project to scann
     * @param reactorProjectDependencies optional reactor projects dependencies indexed by their gav to resolve artifacts without fork mode (means artifacts may not exist)
     * @throws DependenciesToolException if could not load project dependencies
     */
    void loadProjectArtifacts(ArtifactRepository localRepository, List remoteRepositories, MavenProject project , Map<String, List<org.apache.maven.model.Dependency>> reactorProjectDependencies )
        throws DependenciesToolException;

    void writeThirdPartyDependenciesFile(File file, String listedDependenciesFilePath, Set<Dependency> listedDependencies) throws IOException;
}
