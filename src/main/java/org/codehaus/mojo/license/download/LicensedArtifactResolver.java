package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 MojoHaus
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.mojo.license.api.ArtifactFilters;
import org.codehaus.mojo.license.api.DefaultThirdPartyTool;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.api.ResolvedProjectDependencies;
import org.codehaus.mojo.license.download.LicensedArtifact.Builder;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.StringToList;
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
public class LicensedArtifactResolver {
    private static final Logger LOG = LoggerFactory.getLogger(LicensedArtifactResolver.class);

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
     * For a given {@code project}, obtain the universe of its dependencies after applying transitivity and filtering
     * rules given in the {@code configuration} object. Result is given in a map where keys are unique artifact id
     *
     * @param artifacts          Dependencies
     * @param configuration      the configuration
     * @param remoteRepositories remote repositories used to resolve dependencies
     * @param result             Map with Key/Value = PluginID/LicensedArtifact
     * @param licenseMerges      List of license names to merge.
     * @see MavenProjectDependenciesConfigurator
     */
    public void loadProjectDependencies(
            ResolvedProjectDependencies artifacts,
            MavenProjectDependenciesConfigurator configuration,
            List<ArtifactRepository> remoteRepositories,
            Map<String, LicensedArtifact> result,
            boolean extendedInfo,
            List<String> licenseMerges) {
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

        final Map<String, Artifact> excludeArtifacts = new HashMap<>();
        final Map<String, Artifact> includeArtifacts = new HashMap<>();

        ProjectBuildingRequest projectBuildingRequest = new DefaultProjectBuildingRequest(
                        mavenSessionProvider.get().getProjectBuildingRequest())
                .setRemoteRepositories(remoteRepositories)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setResolveDependencies(false)
                .setProcessPlugins(false);

        final Map<String, String> mergedLicenses = buildMergedLicenses(licenseMerges);

        for (Artifact artifact : depArtifacts) {

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

            final String id = MojoHelper.getArtifactId(artifact);

            if (verbose) {
                LOG.info("detected artifact {}", id);
            }

            LicensedArtifact depMavenProject;

            // try to get project from cache
            depMavenProject = result.get(id);

            if (depMavenProject != null) {
                LOG.debug("Dependency [{}] already present in the result", id);
            } else {
                // build project
                final Builder laBuilder = LicensedArtifact.builder(artifact, extendedInfo);
                try {
                    final MavenProject project = mavenProjectBuilder
                            .build(artifact, true, projectBuildingRequest)
                            .getProject();
                    if (extendedInfo) {
                        laBuilder.setName(project.getName());
                        laBuilder.setInceptionYear(project.getInceptionYear());
                        laBuilder.setOrganization(project.getOrganization());
                        laBuilder.setDevelopers(project.getDevelopers());
                        laBuilder.setUrl(project.getUrl());
                        laBuilder.setScm(project.getScm());
                    }
                    List<org.apache.maven.model.License> lics = project.getLicenses();
                    if (lics != null) {
                        for (org.apache.maven.model.License lic : lics) {
                            final String mergedLicense = mergedLicenses.getOrDefault(lic.getName(), lic.getName());
                            laBuilder.license(
                                    new License(mergedLicense, lic.getUrl(), lic.getDistribution(), lic.getComments()));
                        }
                    }
                } catch (ProjectBuildingException e) {
                    laBuilder.errorMessage("Could not create effective POM for '" + id + "': "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                }

                depMavenProject = laBuilder.build();

                if (verbose) {
                    LOG.info("add dependency [{}]", id);
                }

                result.put(id, depMavenProject);
            }

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
    }
    // CHECKSTYLE_ON: MethodLength

    private static Map<String, String> buildMergedLicenses(List<String> licenseMerges) {
        final Map<String, String> mergedLicenses = new HashMap<>();
        if (licenseMerges != null) {
            for (String licenseMerge : licenseMerges) {
                String[] splited = StringToList.trimmedStringSplit(licenseMerge);
                for (String split : splited) {
                    // Map other occurrences to first occurrence.
                    mergedLicenses.put(split, splited[0]);
                }
            }
        }
        return mergedLicenses;
    }
}
