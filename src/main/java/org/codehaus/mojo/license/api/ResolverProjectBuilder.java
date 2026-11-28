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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a {@link MavenProject} for a dependency from its POM using Resolver, so no legacy
 * {@code ArtifactRepository} is needed to read dependency licenses.
 */
@Named
@Singleton
public class ResolverProjectBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ResolverProjectBuilder.class);

    private static final String CONTEXT = "license-maven-plugin";

    @Inject
    private RepositorySystem repositorySystem;

    @Inject
    private ModelBuilder modelBuilder;

    @Inject
    private Provider<MavenSession> mavenSessionProvider;

    /**
     * Builds the effective project for a dependency artifact. When the POM cannot be read, a stub carrying the
     * dependency coordinates is returned, matching the old {@code allowStubModel} behaviour.
     *
     * @param artifact     the dependency artifact
     * @param repositories the repositories to resolve the POM and its parents from
     * @return the dependency project, never {@code null}
     */
    public MavenProject build(Artifact artifact, List<RemoteRepository> repositories) {
        try {
            RepositorySystemSession session = mavenSessionProvider.get().getRepositorySession();
            File pomFile = resolvePom(newPomArtifact(artifact), repositories, session);

            DefaultModelBuildingRequest request = new DefaultModelBuildingRequest()
                    .setModelSource(new FileModelSource(pomFile))
                    .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                    .setProcessPlugins(false)
                    .setSystemProperties(properties(session.getSystemProperties()))
                    .setUserProperties(properties(session.getUserProperties()))
                    .setModelResolver(new ResolverModelResolver(repositories));
            MavenProject project = new MavenProject(modelBuilder.build(request).getEffectiveModel());
            project.setFile(pomFile);
            project.setArtifact(newArtifact(artifact));
            return project;
        } catch (ArtifactResolutionException | ModelBuildingException e) {
            LOG.debug("Unable to read the POM of {}", artifact.getId(), e);
            return stub(artifact);
        }
    }

    /**
     * Converts a repository declared in a POM to its Resolver counterpart, so repositories declared by ancestor POMs
     * can be handed to Resolver.
     *
     * @param repository the POM repository
     * @return the equivalent remote repository
     */
    public static RemoteRepository toRemoteRepository(Repository repository) {
        RemoteRepository.Builder builder =
                new RemoteRepository.Builder(repository.getId(), "default", repository.getUrl());
        org.apache.maven.model.RepositoryPolicy releases = repository.getReleases();
        if (releases != null) {
            builder.setReleasePolicy(toPolicy(releases));
        }
        org.apache.maven.model.RepositoryPolicy snapshots = repository.getSnapshots();
        if (snapshots != null) {
            builder.setSnapshotPolicy(toPolicy(snapshots));
        }
        return builder.build();
    }

    private static RepositoryPolicy toPolicy(org.apache.maven.model.RepositoryPolicy policy) {
        String updatePolicy = policy.getUpdatePolicy();
        String checksumPolicy = policy.getChecksumPolicy();
        return new RepositoryPolicy(
                policy.isEnabled(),
                updatePolicy == null || updatePolicy.isEmpty() ? RepositoryPolicy.UPDATE_POLICY_DAILY : updatePolicy,
                checksumPolicy == null || checksumPolicy.isEmpty()
                        ? RepositoryPolicy.CHECKSUM_POLICY_WARN
                        : checksumPolicy);
    }

    private static org.eclipse.aether.artifact.Artifact newPomArtifact(Artifact artifact) {
        return new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), "", "pom", artifact.getVersion());
    }

    private File resolvePom(
            org.eclipse.aether.artifact.Artifact artifact,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws ArtifactResolutionException {
        ArtifactRequest request = new ArtifactRequest(artifact, repositories, CONTEXT);
        ArtifactResult result = repositorySystem.resolveArtifact(session, request);
        return result.getArtifact().getFile();
    }

    private static Properties properties(java.util.Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }

    private static Artifact newArtifact(Artifact artifact) {
        DefaultArtifactHandler handler = new DefaultArtifactHandler(artifact.getType());
        Artifact result = new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getScope(),
                artifact.getType(),
                artifact.getClassifier(),
                handler);
        result.setFile(artifact.getFile());
        return result;
    }

    private static MavenProject stub(Artifact artifact) {
        MavenProject project = new MavenProject();
        project.setArtifact(newArtifact(artifact));
        return project;
    }

    /** Resolves parent and dependency POMs from the same repositories as the dependency itself. */
    private final class ResolverModelResolver implements ModelResolver {
        private final List<RemoteRepository> repositories;

        private ResolverModelResolver(List<RemoteRepository> repositories) {
            this.repositories = new ArrayList<>(repositories);
        }

        @Override
        public ModelSource2 resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            try {
                org.eclipse.aether.artifact.Artifact pom =
                        new org.eclipse.aether.artifact.DefaultArtifact(groupId, artifactId, "", "pom", version);
                return new FileModelSource(
                        resolvePom(pom, repositories, mavenSessionProvider.get().getRepositorySession()));
            } catch (ArtifactResolutionException e) {
                throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
            }
        }

        @Override
        public ModelSource2 resolveModel(Parent parent) throws UnresolvableModelException {
            parent.setVersion(resolveVersion(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()));
            return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        @Override
        public ModelSource2 resolveModel(Dependency dependency) throws UnresolvableModelException {
            dependency.setVersion(
                    resolveVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        @Override
        public void addRepository(Repository repository) {
            addRepository(repository, false);
        }

        @Override
        public void addRepository(Repository repository, boolean replace) {
            RemoteRepository remote = toRemoteRepository(repository);
            repositories.removeIf(existing -> existing.getId().equals(remote.getId()));
            repositories.add(remote);
        }

        @Override
        public ModelResolver newCopy() {
            return new ResolverModelResolver(repositories);
        }

        private String resolveVersion(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            org.eclipse.aether.artifact.Artifact artifact =
                    new org.eclipse.aether.artifact.DefaultArtifact(groupId, artifactId, "", "pom", version);
            VersionRangeRequest request = new VersionRangeRequest(artifact, repositories, CONTEXT);
            try {
                VersionRangeResult result = repositorySystem.resolveVersionRange(
                        mavenSessionProvider.get().getRepositorySession(), request);
                if (result.getHighestVersion() == null) {
                    throw new UnresolvableModelException(
                            "No versions matched the requested range '" + version + "'", groupId, artifactId, version);
                }
                return result.getHighestVersion().toString();
            } catch (VersionRangeResolutionException e) {
                throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
            }
        }
    }
}
