package org.codehaus.mojo.license.api;

import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.UpdateFileHeaderMojo;
import org.codehaus.mojo.license.model.LicenseMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the {@link FreeMarkerHelper} and given templates.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public class FreeMarkerHelperTest
{

    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog( FreeMarkerHelperTest.class );

    @Test
    public void testRenderTemplateForThirdPartyFile()
        throws Exception
    {

        FreeMarkerHelper helper = new FreeMarkerHelper();

        LicenseMap licenseMap = new LicenseMap();

        MavenProject deps = new MavenProject();
        deps.setArtifact(
            new DefaultArtifact( "groupId", "artifactId", VersionRange.createFromVersionSpec( "0" ), "compile", "type",
                                 "classifier", null ) );
        deps.setGroupId( "groupId" );
        deps.setArtifactId( "artifactId" );
        deps.setVersion( "version" );
        deps.setUrl( "url" );
        MavenProject deps2 = new MavenProject();
        deps2.setArtifact(
            new DefaultArtifact( "groupId2", "artifactId2", VersionRange.createFromVersionSpec( "2" ), "compile",
                                 "type", "classifier", null ) );
        deps2.setGroupId( "groupId2" );
        deps2.setArtifactId( "artifactId2" );
        deps2.setVersion( "version2" );
        licenseMap.put( "license 1", deps );
        licenseMap.put( "license 1", deps2 );
        licenseMap.put( "license 2", deps2 );
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "licenseMap", licenseMap.entrySet() );
        properties.put( "dependencyMap", licenseMap.toDependencyMap().entrySet() );

        String s = helper.renderTemplate( "/org/codehaus/mojo/license/third-party-file.ftl", properties );
        if ( log.isInfoEnabled() )
        {
            log.info( s );
        }
    }

    @Test
    public void testRenderTemplateForThirdPartyFileGroupByLicense()
        throws Exception
    {

        FreeMarkerHelper helper = new FreeMarkerHelper();

        LicenseMap licenseMap = new LicenseMap();

        MavenProject deps = new MavenProject();
        deps.setArtifact(
            new DefaultArtifact( "groupId", "artifactId", VersionRange.createFromVersionSpec( "0" ), "compile", "type",
                                 "classifier", null ) );
        deps.setGroupId( "groupId" );
        deps.setArtifactId( "artifactId" );
        deps.setVersion( "version" );
        MavenProject deps2 = new MavenProject();
        deps2.setArtifact(
            new DefaultArtifact( "groupId2", "artifactId2", VersionRange.createFromVersionSpec( "2" ), "compile",
                                 "type", "classifier", null ) );
        deps2.setGroupId( "groupId2" );
        deps2.setArtifactId( "artifactId2" );
        deps2.setVersion( "version2" );
        deps2.setUrl( "url2" );
        licenseMap.put( "license 1", deps );
        licenseMap.put( "license 1", deps2 );
        licenseMap.put( "license 2", deps2 );
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "licenseMap", licenseMap.entrySet() );
        properties.put( "dependencyMap", licenseMap.toDependencyMap().entrySet() );

        String s =
            helper.renderTemplate( "/org/codehaus/mojo/license/third-party-file-groupByLicense.ftl", properties );
        if ( log.isInfoEnabled() )
        {
            log.info( s );
        }
    }

    @Test
    public void testRenderTemplateForUpdateFileHeader()
        throws Exception
    {

        FreeMarkerHelper helper = new FreeMarkerHelper();

        MavenProject project = new MavenProject();
        project.setArtifact(
            new DefaultArtifact( "groupId", "artifactId", VersionRange.createFromVersionSpec( "0" ), "compile", "type",
                                 "classifier", null ) );
        project.setGroupId( "groupId" );
        project.setArtifactId( "artifactId" );
        project.setVersion( "version" );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "project", project );
        properties.put( "projectName", "projectName");
        properties.put( "inceptionYear", "inceptionYear" );
        properties.put( "organizationName",  "organizationName");
        properties.put( "addSvnKeyWords",  true);

        String s =
            helper.renderTemplate( "/org/codehaus/mojo/license/default-file-header-description.ftl", properties );
        Assert.assertEquals("projectName\n" +
                                "$Id:"+"$\n" +
                                "$HeadURL:"+"$", s);
        if ( log.isInfoEnabled() )
        {
            log.info( s );
        }
    }
}
