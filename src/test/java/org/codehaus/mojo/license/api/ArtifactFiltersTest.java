package org.codehaus.mojo.license.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.mojo.license.api.ArtifactFilters.Builder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactFiltersTest {
    @Test
    void isIncluded() throws InvalidVersionSpecificationException {
        final Artifact jar1Compile = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact jar2Compile = new DefaultArtifact(
                "org.group1", "artifact2", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact jar3Compile = new DefaultArtifact(
                "org.group1", "artifact3", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact war1Compile = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "compile", "war", "", null);
        final Artifact jar1Test = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "test", "jar", "", null);

        final Artifact jar4CompileOptional = new DefaultArtifact(
                "org.group1", "artifact4", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        jar4CompileOptional.setOptional(true);

        final ArtifactFilters defaultFilters = ArtifactFilters.buidler().build();
        assertTrue(defaultFilters.isIncluded(jar1Compile));
        assertTrue(defaultFilters.isIncluded(jar2Compile));
        assertTrue(defaultFilters.isIncluded(war1Compile));
        assertTrue(defaultFilters.isIncluded(jar1Test));
        assertTrue(defaultFilters.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeScope =
                ArtifactFilters.buidler().includeScope("compile").build();
        assertTrue(includeScope.isIncluded(jar1Compile));
        assertTrue(includeScope.isIncluded(jar2Compile));
        assertTrue(includeScope.isIncluded(war1Compile));
        assertFalse(includeScope.isIncluded(jar1Test));
        assertTrue(includeScope.isIncluded(jar4CompileOptional));

        final ArtifactFilters excludeScope =
                ArtifactFilters.buidler().excludeScope("test").build();
        assertTrue(excludeScope.isIncluded(jar1Compile));
        assertTrue(excludeScope.isIncluded(jar2Compile));
        assertTrue(excludeScope.isIncluded(war1Compile));
        assertFalse(excludeScope.isIncluded(jar1Test));
        assertTrue(excludeScope.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeType =
                ArtifactFilters.buidler().includeType("jar").build();
        assertTrue(includeType.isIncluded(jar1Compile));
        assertTrue(includeType.isIncluded(jar2Compile));
        assertFalse(includeType.isIncluded(war1Compile));
        assertTrue(includeType.isIncluded(jar1Test));
        assertTrue(includeType.isIncluded(jar4CompileOptional));

        final ArtifactFilters excludeType =
                ArtifactFilters.buidler().excludeType("war").build();
        assertTrue(excludeType.isIncluded(jar1Compile));
        assertTrue(excludeType.isIncluded(jar2Compile));
        assertFalse(excludeType.isIncluded(war1Compile));
        assertTrue(excludeType.isIncluded(jar1Test));
        assertTrue(excludeType.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeGa =
                ArtifactFilters.buidler().includeGa("org\\.group1:artifact[^2]").build();
        assertTrue(includeGa.isIncluded(jar1Compile));
        assertFalse(includeGa.isIncluded(jar2Compile));
        assertTrue(includeGa.isIncluded(war1Compile));
        assertTrue(includeGa.isIncluded(jar1Test));
        assertTrue(includeGa.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeGas = ArtifactFilters.buidler()
                .includeGas("org\\.group1:artifact1", "org\\.group1:artifact3")
                .build();
        assertTrue(includeGas.isIncluded(jar1Compile));
        assertFalse(includeGas.isIncluded(jar2Compile));
        assertTrue(includeGas.isIncluded(jar3Compile));
        assertTrue(includeGas.isIncluded(war1Compile));
        assertTrue(includeGas.isIncluded(jar1Test));
        assertFalse(includeGas.isIncluded(jar4CompileOptional));

        final ArtifactFilters excludeGas = ArtifactFilters.buidler()
                .excludeGas("org\\.group1:artifact2", "org\\.group1:artifact3")
                .build();
        assertTrue(excludeGas.isIncluded(jar1Compile));
        assertFalse(excludeGas.isIncluded(jar2Compile));
        assertFalse(excludeGas.isIncluded(jar3Compile));
        assertTrue(excludeGas.isIncluded(war1Compile));
        assertTrue(excludeGas.isIncluded(jar1Test));
        assertTrue(excludeGas.isIncluded(jar4CompileOptional));

        final ArtifactFilters excludeGa =
                ArtifactFilters.buidler().excludeGa("org\\.group1:artifact2").build();
        assertTrue(excludeGa.isIncluded(jar1Compile));
        assertFalse(excludeGa.isIncluded(jar2Compile));
        assertTrue(excludeGa.isIncluded(war1Compile));
        assertTrue(excludeGa.isIncluded(jar1Test));
        assertTrue(excludeGa.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeExcludeGas = ArtifactFilters.buidler()
                .includeGa("org\\.group1:.*")
                .excludeGa("org\\.group1:artifact3")
                .build();
        assertTrue(includeExcludeGas.isIncluded(jar1Compile));
        assertTrue(includeExcludeGas.isIncluded(jar2Compile));
        assertFalse(includeExcludeGas.isIncluded(jar3Compile));
        assertTrue(includeExcludeGas.isIncluded(war1Compile));
        assertTrue(includeExcludeGas.isIncluded(jar1Test));
        assertTrue(includeExcludeGas.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeExcludeGasType = ArtifactFilters.buidler()
                .includeGa("org\\.group1:.*")
                .excludeGa("org\\.group1:artifact3")
                .excludeType("war")
                .build();
        assertTrue(includeExcludeGasType.isIncluded(jar1Compile));
        assertTrue(includeExcludeGasType.isIncluded(jar2Compile));
        assertFalse(includeExcludeGasType.isIncluded(jar3Compile));
        assertFalse(includeExcludeGasType.isIncluded(war1Compile));
        assertTrue(includeExcludeGasType.isIncluded(jar1Test));
        assertTrue(includeExcludeGasType.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeExcludeGasTypeScope = ArtifactFilters.buidler()
                .includeGa("org\\.group1:.*")
                .excludeGa("org\\.group1:artifact3")
                .excludeType("war")
                .excludeScope("test")
                .build();
        assertTrue(includeExcludeGasTypeScope.isIncluded(jar1Compile));
        assertTrue(includeExcludeGasTypeScope.isIncluded(jar2Compile));
        assertFalse(includeExcludeGasTypeScope.isIncluded(jar3Compile));
        assertFalse(includeExcludeGasTypeScope.isIncluded(war1Compile));
        assertFalse(includeExcludeGasTypeScope.isIncluded(jar1Test));
        assertTrue(includeExcludeGasTypeScope.isIncluded(jar4CompileOptional));

        final ArtifactFilters includeOptional =
                ArtifactFilters.buidler().includeOptional(true).build();
        assertTrue(includeOptional.isIncluded(jar1Compile));
        assertTrue(includeOptional.isIncluded(jar2Compile));
        assertTrue(includeOptional.isIncluded(war1Compile));
        assertTrue(includeOptional.isIncluded(jar1Test));
        assertTrue(includeOptional.isIncluded(jar4CompileOptional));

        final ArtifactFilters excludeOptional =
                ArtifactFilters.buidler().includeOptional(false).build();
        assertTrue(excludeOptional.isIncluded(jar1Compile));
        assertTrue(excludeOptional.isIncluded(jar2Compile));
        assertTrue(excludeOptional.isIncluded(war1Compile));
        assertTrue(excludeOptional.isIncluded(jar1Test));
        assertFalse(excludeOptional.isIncluded(jar4CompileOptional));
    }

    @Test
    void urlContent() throws IOException, InvalidVersionSpecificationException {
        final Builder builder = ArtifactFilters.buidler();
        final URL url = getClass().getClassLoader().getResource("org/codehaus/mojo/license/api/atifact-filters.txt");
        try (InputStream in = url.openStream()) {
            final String content = IOUtils.toString(in, StandardCharsets.UTF_8);
            builder.script("file:///path/to/my-file.txt", content);
        }

        final ArtifactFilters filters = builder.build();

        final Artifact jar1Compile = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact jar2Compile = new DefaultArtifact(
                "org.group1", "artifact2", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact jar3Compile = new DefaultArtifact(
                "org.group3", "artifact3", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        final Artifact war1Compile = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "compile", "war", "", null);
        final Artifact jar1Test = new DefaultArtifact(
                "org.group1", "artifact1", VersionRange.createFromVersionSpec("1.0"), "test", "jar", "", null);
        final Artifact jar4CompileOptional = new DefaultArtifact(
                "org.group1", "artifact4", VersionRange.createFromVersionSpec("1.0"), "compile", "jar", "", null);
        jar4CompileOptional.setOptional(false);

        assertTrue(filters.isIncluded(jar1Compile));
        assertFalse(filters.isIncluded(jar2Compile));
        assertFalse(filters.isIncluded(jar3Compile));
        assertFalse(filters.isIncluded(war1Compile));
        assertFalse(filters.isIncluded(jar1Test));
        assertFalse(filters.isIncluded(jar4CompileOptional));
    }
}
