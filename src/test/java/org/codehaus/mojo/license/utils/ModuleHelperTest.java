package org.codehaus.mojo.license.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModuleHelperTest {
    List<MavenProject> projects = new ArrayList<>();
    MavenProject project0 = new MavenProject();
    MavenProject project1 = new MavenProject();
    MavenProject project2 = new MavenProject();
    MavenProject project3 = new MavenProject();
    MavenProject project4 = new MavenProject();

    Model model0 = new Model();
    Model model1 = new Model();
    Model model2 = new Model();
    Model model3 = new Model();
    Model model4 = new Model();

    final String groupId = "org.codehaus.mojo";
    final String artifactId0 = "test0";
    final String artifactId1 = "test1";
    final String artifactId2 = "test2";
    final String artifactId3 = "test3";
    final String artifactId4 = "test4";

    @BeforeEach
    void setUp() {
        model0.setGroupId(groupId);
        model0.setArtifactId(artifactId0);
        model1.setGroupId(groupId);
        model1.setArtifactId(artifactId1);
        model2.setGroupId(groupId);
        model2.setArtifactId(artifactId2);
        model3.setGroupId(groupId);
        model3.setArtifactId(artifactId3);
        model4.setGroupId(groupId);
        model4.setArtifactId(artifactId4);

        project0 = new MavenProject(model0);

        project1 = new MavenProject(model1);

        project2 = new MavenProject(model2);

        project3 = new MavenProject(model3);

        project4 = new MavenProject(model4);

        projects.clear();
        projects.add(new MavenProject(project0));
        projects.add(new MavenProject(project1));
        projects.add(new MavenProject(project2));
        projects.add(new MavenProject(project3));
        projects.add(new MavenProject(project4));
    }

    @Test
    void testNoFilters() {
        String[] includedModules = null;
        String[] excludedModules = null;

        List<MavenProject> result = ModuleHelper.getFilteredModules(projects, includedModules, excludedModules);

        assertEquals(projects, result);
        assertEquals(5, projects.size());
    }

    @Test
    void testIncludeFilterOnly() {
        String[] includedModules = {artifactId2, artifactId4};
        String[] excludedModules = null;

        List<MavenProject> result = ModuleHelper.getFilteredModules(projects, includedModules, excludedModules);

        assertEquals(2, result.size());
        assertEquals(artifactId2, result.get(0).getArtifactId());
        assertEquals(artifactId4, result.get(1).getArtifactId());
    }

    @Test
    void testExcludeFilterOnly() {
        String[] includedModules = null;
        String[] excludedModules = {artifactId2, artifactId4};

        List<MavenProject> result = ModuleHelper.getFilteredModules(projects, includedModules, excludedModules);

        assertEquals(3, result.size());
        assertEquals(artifactId0, result.get(0).getArtifactId());
        assertEquals(artifactId1, result.get(1).getArtifactId());
        assertEquals(artifactId3, result.get(2).getArtifactId());
    }

    @Test
    void testIncludeAndExcludeFilter() {
        String[] includedModules = {artifactId0, artifactId2, artifactId3};
        String[] excludedModules = {artifactId2, artifactId4};

        List<MavenProject> result = ModuleHelper.getFilteredModules(projects, includedModules, excludedModules);

        assertEquals(2, result.size());
        assertEquals(artifactId0, result.get(0).getArtifactId());
        assertEquals(artifactId3, result.get(1).getArtifactId());
    }
}
