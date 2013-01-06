package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 Codehaus, Tony Chemit
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

import org.junit.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.plexus.util.IOUtil;
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
    private final String LS = System.getProperty( "line.separator" );

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
        properties.put( "projectName", "projectName" );
        properties.put( "inceptionYear", "inceptionYear" );
        properties.put( "organizationName", "organizationName" );
        properties.put( "addSvnKeyWords", true );

        String s =
            helper.renderTemplate( "/org/codehaus/mojo/license/default-file-header-description.ftl", properties );
        Assert.assertEquals( "projectName" + LS +
                                 "$Id:" + "$" + LS +
                                 "$HeadURL:" + "$", s );
        if ( log.isInfoEnabled() )
        {
            log.info( s );
        }
    }
}
