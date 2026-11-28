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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DependenciesToolTest {

    @Test
    void dependencyPomsAreBuiltFromTheProvidedRepositories() throws Exception {
        ResolverProjectBuilder projectBuilder = mock(ResolverProjectBuilder.class);
        MavenProject dependencyProject = new MavenProject();
        dependencyProject.setArtifact(artifact());
        when(projectBuilder.build(any(Artifact.class), anyList())).thenReturn(dependencyProject);

        ArtifactFilters filters = mock(ArtifactFilters.class);
        when(filters.isIncluded(any(Artifact.class))).thenReturn(true);
        MavenProjectDependenciesConfigurator configuration = mock(MavenProjectDependenciesConfigurator.class);
        when(configuration.getArtifactFilters()).thenReturn(filters);
        when(configuration.isIncludeTransitiveDependencies()).thenReturn(true);

        DependenciesTool tool = new DependenciesTool();
        inject(tool, "projectBuilder", projectBuilder);

        Artifact dependency = artifact();
        List<RemoteRepository> repositories = Collections.singletonList(
                new RemoteRepository.Builder("central", "default", "https://example.org").build());
        tool.loadProjectDependencies(
                new ResolvedProjectDependencies(Collections.singleton(dependency), Collections.singleton(dependency)),
                configuration,
                repositories,
                new TreeMap<>());

        verify(projectBuilder).build(any(Artifact.class), anyList());
    }

    private static Artifact artifact() {
        return new DefaultArtifact(
                "org.test", "dependency", "1.0", "compile", "jar", null, new DefaultArtifactHandler("jar"));
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
