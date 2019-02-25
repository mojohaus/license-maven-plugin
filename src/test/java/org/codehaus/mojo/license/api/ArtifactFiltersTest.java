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
import org.codehaus.mojo.license.api.ArtifactFilters;
import org.codehaus.mojo.license.api.ArtifactFilters.Builder;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactFiltersTest
{
    @Test
    public void isIncluded()
        throws InvalidVersionSpecificationException
    {
        final Artifact jar1Compile =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact jar2Compile =
            new DefaultArtifact( "org.group1", "artifact2", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact jar3Compile =
            new DefaultArtifact( "org.group1", "artifact3", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact war1Compile =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "war", "", null );
        final Artifact jar1Test =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "test", "jar",
                                 "", null );

        final Artifact jar4CompileOptional =
            new DefaultArtifact( "org.group1", "artifact4", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        jar4CompileOptional.setOptional( true );

        final ArtifactFilters defaultFilters = ArtifactFilters.buidler().build();
        Assert.assertTrue( defaultFilters.isIncluded( jar1Compile ) );
        Assert.assertTrue( defaultFilters.isIncluded( jar2Compile ) );
        Assert.assertTrue( defaultFilters.isIncluded( war1Compile ) );
        Assert.assertTrue( defaultFilters.isIncluded( jar1Test ) );
        Assert.assertTrue( defaultFilters.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeScope = ArtifactFilters.buidler().includeScope( "compile" ).build();
        Assert.assertTrue( includeScope.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeScope.isIncluded( jar2Compile ) );
        Assert.assertTrue( includeScope.isIncluded( war1Compile ) );
        Assert.assertFalse( includeScope.isIncluded( jar1Test ) );
        Assert.assertTrue( includeScope.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters excludeScope = ArtifactFilters.buidler().excludeScope( "test" ).build();
        Assert.assertTrue( excludeScope.isIncluded( jar1Compile ) );
        Assert.assertTrue( excludeScope.isIncluded( jar2Compile ) );
        Assert.assertTrue( excludeScope.isIncluded( war1Compile ) );
        Assert.assertFalse( excludeScope.isIncluded( jar1Test ) );
        Assert.assertTrue( excludeScope.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeType = ArtifactFilters.buidler().includeType( "jar" ).build();
        Assert.assertTrue( includeType.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeType.isIncluded( jar2Compile ) );
        Assert.assertFalse( includeType.isIncluded( war1Compile ) );
        Assert.assertTrue( includeType.isIncluded( jar1Test ) );
        Assert.assertTrue( includeType.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters excludeType = ArtifactFilters.buidler().excludeType( "war" ).build();
        Assert.assertTrue( excludeType.isIncluded( jar1Compile ) );
        Assert.assertTrue( excludeType.isIncluded( jar2Compile ) );
        Assert.assertFalse( excludeType.isIncluded( war1Compile ) );
        Assert.assertTrue( excludeType.isIncluded( jar1Test ) );
        Assert.assertTrue( excludeType.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeGa = ArtifactFilters.buidler().includeGa( "org\\.group1:artifact[^2]" ).build();
        Assert.assertTrue( includeGa.isIncluded( jar1Compile ) );
        Assert.assertFalse( includeGa.isIncluded( jar2Compile ) );
        Assert.assertTrue( includeGa.isIncluded( war1Compile ) );
        Assert.assertTrue( includeGa.isIncluded( jar1Test ) );
        Assert.assertTrue( includeGa.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeGas =
            ArtifactFilters.buidler().includeGas( "org\\.group1:artifact1", "org\\.group1:artifact3" ).build();
        Assert.assertTrue( includeGas.isIncluded( jar1Compile ) );
        Assert.assertFalse( includeGas.isIncluded( jar2Compile ) );
        Assert.assertTrue( includeGas.isIncluded( jar3Compile ) );
        Assert.assertTrue( includeGas.isIncluded( war1Compile ) );
        Assert.assertTrue( includeGas.isIncluded( jar1Test ) );
        Assert.assertFalse( includeGas.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters excludeGas =
            ArtifactFilters.buidler().excludeGas( "org\\.group1:artifact2", "org\\.group1:artifact3" ).build();
        Assert.assertTrue( excludeGas.isIncluded( jar1Compile ) );
        Assert.assertFalse( excludeGas.isIncluded( jar2Compile ) );
        Assert.assertFalse( excludeGas.isIncluded( jar3Compile ) );
        Assert.assertTrue( excludeGas.isIncluded( war1Compile ) );
        Assert.assertTrue( excludeGas.isIncluded( jar1Test ) );
        Assert.assertTrue( excludeGas.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters excludeGa = ArtifactFilters.buidler().excludeGa( "org\\.group1:artifact2" ).build();
        Assert.assertTrue( excludeGa.isIncluded( jar1Compile ) );
        Assert.assertFalse( excludeGa.isIncluded( jar2Compile ) );
        Assert.assertTrue( excludeGa.isIncluded( war1Compile ) );
        Assert.assertTrue( excludeGa.isIncluded( jar1Test ) );
        Assert.assertTrue( excludeGa.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeExcludeGas =
            ArtifactFilters.buidler().includeGa( "org\\.group1:.*" ).excludeGa( "org\\.group1:artifact3" ).build();
        Assert.assertTrue( includeExcludeGas.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeExcludeGas.isIncluded( jar2Compile ) );
        Assert.assertFalse( includeExcludeGas.isIncluded( jar3Compile ) );
        Assert.assertTrue( includeExcludeGas.isIncluded( war1Compile ) );
        Assert.assertTrue( includeExcludeGas.isIncluded( jar1Test ) );
        Assert.assertTrue( includeExcludeGas.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeExcludeGasType =
            ArtifactFilters.buidler().includeGa( "org\\.group1:.*" ).excludeGa( "org\\.group1:artifact3" ).excludeType( "war" ).build();
        Assert.assertTrue( includeExcludeGasType.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeExcludeGasType.isIncluded( jar2Compile ) );
        Assert.assertFalse( includeExcludeGasType.isIncluded( jar3Compile ) );
        Assert.assertFalse( includeExcludeGasType.isIncluded( war1Compile ) );
        Assert.assertTrue( includeExcludeGasType.isIncluded( jar1Test ) );
        Assert.assertTrue( includeExcludeGasType.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeExcludeGasTypeScope =
            ArtifactFilters.buidler().includeGa( "org\\.group1:.*" ).excludeGa( "org\\.group1:artifact3" ).excludeType( "war" ).excludeScope( "test" ).build();
        Assert.assertTrue( includeExcludeGasTypeScope.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeExcludeGasTypeScope.isIncluded( jar2Compile ) );
        Assert.assertFalse( includeExcludeGasTypeScope.isIncluded( jar3Compile ) );
        Assert.assertFalse( includeExcludeGasTypeScope.isIncluded( war1Compile ) );
        Assert.assertFalse( includeExcludeGasTypeScope.isIncluded( jar1Test ) );
        Assert.assertTrue( includeExcludeGasTypeScope.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters includeOptional = ArtifactFilters.buidler().includeOptional( true ).build();
        Assert.assertTrue( includeOptional.isIncluded( jar1Compile ) );
        Assert.assertTrue( includeOptional.isIncluded( jar2Compile ) );
        Assert.assertTrue( includeOptional.isIncluded( war1Compile ) );
        Assert.assertTrue( includeOptional.isIncluded( jar1Test ) );
        Assert.assertTrue( includeOptional.isIncluded( jar4CompileOptional ) );

        final ArtifactFilters excludeOptional = ArtifactFilters.buidler().includeOptional( false ).build();
        Assert.assertTrue( excludeOptional.isIncluded( jar1Compile ) );
        Assert.assertTrue( excludeOptional.isIncluded( jar2Compile ) );
        Assert.assertTrue( excludeOptional.isIncluded( war1Compile ) );
        Assert.assertTrue( excludeOptional.isIncluded( jar1Test ) );
        Assert.assertFalse( excludeOptional.isIncluded( jar4CompileOptional ) );

    }

    @Test
    public void urlContent()
        throws IOException, InvalidVersionSpecificationException
    {
        final Builder builder = ArtifactFilters.buidler();
        final URL url = getClass().getClassLoader().getResource( "org/codehaus/mojo/license/api/atifact-filters.txt" );
        try (InputStream in = url.openStream())
        {
            final String content = IOUtils.toString( in, StandardCharsets.UTF_8 );
            builder.script( "file:///path/to/my-file.txt", content );
        }

        final ArtifactFilters filters = builder.build();

        final Artifact jar1Compile =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact jar2Compile =
            new DefaultArtifact( "org.group1", "artifact2", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact jar3Compile =
            new DefaultArtifact( "org.group3", "artifact3", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        final Artifact war1Compile =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "war", "", null );
        final Artifact jar1Test =
            new DefaultArtifact( "org.group1", "artifact1", VersionRange.createFromVersionSpec( "1.0" ), "test", "jar",
                                 "", null );
        final Artifact jar4CompileOptional =
            new DefaultArtifact( "org.group1", "artifact4", VersionRange.createFromVersionSpec( "1.0" ), "compile",
                                 "jar", "", null );
        jar4CompileOptional.setOptional( false );

        Assert.assertTrue( filters.isIncluded( jar1Compile ) );
        Assert.assertFalse( filters.isIncluded( jar2Compile ) );
        Assert.assertFalse( filters.isIncluded( jar3Compile ) );
        Assert.assertFalse( filters.isIncluded( war1Compile ) );
        Assert.assertFalse( filters.isIncluded( jar1Test ) );
        Assert.assertFalse( filters.isIncluded( jar4CompileOptional ) );
    }

}
