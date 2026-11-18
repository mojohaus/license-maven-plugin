package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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

import javax.inject.Provider;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DependenciesToolTest {

    @Test
    void dependencyBuildRequestDoesNotCarryTheReactorProject() throws Exception {
        DefaultProjectBuildingRequest sessionRequest = new DefaultProjectBuildingRequest();
        sessionRequest.setProject(new MavenProject());
        MavenSession session = mock(MavenSession.class);
        when(session.getProjectBuildingRequest()).thenReturn(sessionRequest);
        @SuppressWarnings("unchecked")
        Provider<MavenSession> sessionProvider = mock(Provider.class);
        when(sessionProvider.get()).thenReturn(session);

        MavenProject dependencyProject = new MavenProject();
        dependencyProject.setArtifact(artifact());
        ProjectBuildingResult buildingResult = mock(ProjectBuildingResult.class);
        when(buildingResult.getProject()).thenReturn(dependencyProject);
        ProjectBuilder projectBuilder = mock(ProjectBuilder.class);
        when(projectBuilder.build(any(Artifact.class), anyBoolean(), any(ProjectBuildingRequest.class)))
                .thenReturn(buildingResult);

        ArtifactFilters filters = mock(ArtifactFilters.class);
        when(filters.isIncluded(any(Artifact.class))).thenReturn(true);
        MavenProjectDependenciesConfigurator configuration = mock(MavenProjectDependenciesConfigurator.class);
        when(configuration.getArtifactFilters()).thenReturn(filters);
        when(configuration.isIncludeTransitiveDependencies()).thenReturn(true);

        DependenciesTool tool = new DependenciesTool();
        inject(tool, "mavenProjectBuilder", projectBuilder);
        inject(tool, "mavenSessionProvider", sessionProvider);

        Artifact dependency = artifact();
        tool.loadProjectDependencies(
                new ResolvedProjectDependencies(Collections.singleton(dependency), Collections.singleton(dependency)),
                configuration,
                Collections.emptyList(),
                new TreeMap<>());

        ArgumentCaptor<ProjectBuildingRequest> requests = ArgumentCaptor.forClass(ProjectBuildingRequest.class);
        verify(projectBuilder).build(any(Artifact.class), anyBoolean(), requests.capture());
        assertNull(requests.getValue().getProject());
    }

    private static Artifact artifact() {
        return new DefaultArtifact(
                "org.test", "dependency", "1.0", "compile", "jar", null, new DefaultArtifactHandler("jar"));
    }

    private static void inject(DependenciesTool tool, String fieldName, Object value) throws Exception {
        Field field = DependenciesTool.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(tool, value);
    }
}
