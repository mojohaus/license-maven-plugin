package org.codehaus.mojo.license.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.project.MavenProject;

public class ModuleHelper {

    /**
     * Filters a List of (Sub-)Modules based on their ArtifactId.
     * @param reactorProjects List of Modules to filter.
     * @param includedModules Array of ArtifactIds to include in the final list.
     * @param excludedModules Array of ArtifactIds to exclude in the final list.
     * @return Filtered List of (Sub-)Modules.
     */
    public static List<MavenProject> getFilteredModules(
            List<MavenProject> reactorProjects, String[] includedModules, String[] excludedModules) {
        return reactorProjects.stream()
                .filter(element -> (includedModules == null
                        || Arrays.stream(includedModules)
                                .anyMatch(value -> Objects.equals(value, element.getArtifactId()))))
                .filter(element -> (excludedModules == null
                        || Arrays.stream(excludedModules)
                                .noneMatch(value -> Objects.equals(value, element.getArtifactId()))))
                .collect(Collectors.toList());
    }
}
