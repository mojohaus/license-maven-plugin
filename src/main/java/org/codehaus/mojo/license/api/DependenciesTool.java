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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tool to deal with dependencies of a project.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Named
@Singleton
public class DependenciesTool {
    private static final Logger LOG = LoggerFactory.getLogger(DependenciesTool.class);

    /**
     * Message used when an invalid expression pattern is found.
     */
    public static final String INVALID_PATTERN_MESSAGE =
            "The pattern specified by expression <%s> seems to be invalid.";

    /**
     * Project builder.
     */
    @Inject
    private ProjectBuilder mavenProjectBuilder;

    @Inject
    private Provider<MavenSession> mavenSessionProvider;

    // CHECKSTYLE_OFF: MethodLength
    /**
     * For a given {@code project}, obtain the universe of its dependencies after applying transitivity and
     * filtering rules given in the {@code configuration} object.
     *
     * Result is given in a map where keys are unique artifact id
     *
     * @param artifacts       the project dependencies
     * @param configuration      the configuration
     * @param remoteRepositories remote repositories used to resolve dependencies
     * @param cache              a optional cache where to keep resolved dependencies
     * @return the map of resolved dependencies indexed by their unique id.
     * @see MavenProjectDependenciesConfigurator
     */
    public SortedMap<String, MavenProject> loadProjectDependencies(
            ResolvedProjectDependencies artifacts,
            MavenProjectDependenciesConfigurator configuration,
            List<ArtifactRepository> remoteRepositories,
            SortedMap<String, MavenProject> cache) {

        final ArtifactFilters artifactFilters = configuration.getArtifactFilters();

        final boolean excludeTransitiveDependencies = configuration.isExcludeTransitiveDependencies();

        final Set<Artifact> depArtifacts;

        if (configuration.isIncludeTransitiveDependencies()) {
            // All project dependencies
            depArtifacts = artifacts.getAllDependencies();
        } else {
            // Only direct project dependencies
            depArtifacts = artifacts.getDirectDependencies();
        }

        boolean verbose = configuration.isVerbose();

        SortedMap<String, MavenProject> result = new TreeMap<>();

        Map<String, Artifact> excludeArtifacts = new HashMap<>();
        Map<String, Artifact> includeArtifacts = new HashMap<>();

        SortedMap<String, MavenProject> localCache = new TreeMap<>();
        if (cache != null) {
            synchronized (cache) {
                localCache.putAll(cache);
            }
        }
        ProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest(
                        mavenSessionProvider.get().getProjectBuildingRequest())
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                // We already have the relevant part of the dependency tree
                // Re-resolving risks including e.g. excluded artifacts
                .setResolveDependencies(false)
                // We don't care about plugin licensing
                .setProcessPlugins(false)
                .setRemoteRepositories(remoteRepositories);

        // The projects built so far by artifact id: the POM of a dependency may declare the repository of its own
        // dependencies, so the ancestors of an artifact must be built before the artifact.
        final Map<String, MavenProject> builtProjects = new HashMap<>();

        final List<Artifact> sortedArtifacts = new ArrayList<>(depArtifacts);
        sortedArtifacts.sort(Comparator.comparingInt(DependenciesTool::trailLength));

        for (Artifact artifact : sortedArtifacts) {

            excludeArtifacts.put(artifact.getId(), artifact);

            if (DefaultThirdPartyTool.LICENSE_DB_TYPE.equals(artifact.getType())) {
                // the special dependencies for license databases don't count.
                // Note that this will still see transitive deps of a license db; so using the build helper inside of
                // another project to make them will be noisy.
                continue;
            }

            if (!artifactFilters.isIncluded(artifact)) {
                LOG.debug("Excluding artifact {}", artifact);
                continue;
            }

            String id = MojoHelper.getArtifactId(artifact);

            if (verbose) {
                LOG.info("detected artifact {}", id);
            }

            MavenProject depMavenProject;

            // try to get project from cache
            depMavenProject = localCache.get(id);

            if (depMavenProject != null) {
                if (verbose) {
                    LOG.info("add dependency [{}] (from cache)", id);
                }
            } else {
                // build project

                try {
                    depMavenProject = mavenProjectBuilder
                            .build(artifact, true, requestFor(artifact, projectBuildingRequest, builtProjects))
                            .getProject();
                    depMavenProject.getArtifact().setScope(artifact.getScope());

                    // In case maven-metadata.xml has different artifactId, groupId or version.
                    if (!depMavenProject.getGroupId().equals(artifact.getGroupId())) {
                        depMavenProject.setGroupId(artifact.getGroupId());
                        depMavenProject.getArtifact().setGroupId(artifact.getGroupId());
                    }
                    if (!depMavenProject.getArtifactId().equals(artifact.getArtifactId())) {
                        depMavenProject.setArtifactId(artifact.getArtifactId());
                        depMavenProject.getArtifact().setArtifactId(artifact.getArtifactId());
                    }
                    if (!depMavenProject.getVersion().equals(artifact.getVersion())) {
                        depMavenProject.setVersion(artifact.getVersion());
                        depMavenProject.getArtifact().setVersion(artifact.getVersion());
                    }
                } catch (ProjectBuildingException e) {
                    LOG.warn("Unable to obtain POM for artifact: {}", artifact, e);
                    continue;
                }

                if (verbose) {
                    LOG.info("add dependency [{}]", id);
                }

                // store it also in cache
                localCache.put(id, depMavenProject);
            }

            // keep the project
            result.put(id, depMavenProject);
            builtProjects.put(artifact.getId(), depMavenProject);

            excludeArtifacts.remove(artifact.getId());
            includeArtifacts.put(artifact.getId(), artifact);
        }

        // exclude artifacts from the result that contain excluded artifacts in the dependency trail
        if (excludeTransitiveDependencies) {
            for (Map.Entry<String, Artifact> entry : includeArtifacts.entrySet()) {
                List<String> dependencyTrail = entry.getValue().getDependencyTrail();
                if (dependencyTrail == null) {
                    continue;
                }

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
    // CHECKSTYLE_ON: MethodLength

    private static int trailLength(Artifact artifact) {
        final List<String> trail = artifact.getDependencyTrail();
        return trail == null ? 0 : trail.size();
    }

    /**
     * Maven resolves a dependency from the repositories that the POMs on its way declare. Reading the POM of the
     * dependency again with the repositories of the project only would not find a POM that is hosted by such a
     * repository, so the repositories of the artifact and of the projects that lead to it are added to the project
     * repositories.
     */
    private static ProjectBuildingRequest requestFor(
            Artifact artifact, ProjectBuildingRequest request, Map<String, MavenProject> builtProjects) {
        final Map<String, ArtifactRepository> repositories = new LinkedHashMap<>();
        addRepositories(repositories, request.getRemoteRepositories());
        if (artifact.getRepository() != null) {
            addRepositories(repositories, Collections.singletonList(artifact.getRepository()));
        }
        final List<String> trail = artifact.getDependencyTrail();
        if (trail != null) {
            // the first element of the trail is the project itself, the last one the artifact
            for (int i = 1; i < trail.size() - 1; i++) {
                final MavenProject ancestor = builtProjects.get(trail.get(i));
                if (ancestor != null) {
                    addRepositories(repositories, ancestor.getRemoteArtifactRepositories());
                }
            }
        }
        final List<ArtifactRepository> known = request.getRemoteRepositories();
        if (known != null && repositories.size() == known.size()) {
            return request;
        }
        return new DefaultProjectBuildingRequest(request).setRemoteRepositories(new ArrayList<>(repositories.values()));
    }

    private static void addRepositories(Map<String, ArtifactRepository> target, List<ArtifactRepository> repositories) {
        if (repositories != null) {
            for (ArtifactRepository repository : repositories) {
                target.putIfAbsent(repository.getId(), repository);
            }
        }
    }
}
