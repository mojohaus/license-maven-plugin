package org.codehaus.mojo.license.api;

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class DependenciesToolNullTrailTest {
    @Test
    void reactorArtifactsMayHaveNullDependencyTrail() {
        DefaultArtifact artifact = new DefaultArtifact(
                "g", "a", VersionRange.createFromVersion("1.0"), "compile", "jar", "", null);
        assertNull(artifact.getDependencyTrail());
        List<String> trail = artifact.getDependencyTrail();
        assertDoesNotThrow(() -> {
            if (trail == null) {
                return;
            }
            for (int i = 1; i < trail.size() - 1; i++) {
                trail.get(i);
            }
        });
    }
}
