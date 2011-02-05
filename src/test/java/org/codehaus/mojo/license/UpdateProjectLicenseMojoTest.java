/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
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

package org.codehaus.mojo.license;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

@RunWith( JUnit4.class )
public class UpdateProjectLicenseMojoTest
    extends AbstractMojoTestCase
{

    public static final String GOAL = "update-project-license";

    public static File testPomDir;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        testPomDir =
            new File( getBasedir(), "target/test-classes/org/codehaus/mojo/license/updateProjectLicenseMojoTest/" );
    }

    @Test
    public void testOne()
        throws Exception
    {
        // Mojo initialization
        File pomFile = new File( testPomDir, "testOne.xml" );
        UpdateProjectLicenseMojo mojo = (UpdateProjectLicenseMojo) this.lookupMojo( GOAL, pomFile );

        File basedir = pomFile.getParentFile();
        mojo.setOutputDirectory( basedir );
        mojo.setLicenseFile( new File( basedir, mojo.getLicenseFile().toString() ) );
        MavenProject project = LicensePluginTestHelper.buildProject( getContainer(), pomFile );
        setVariableValueToObject( mojo, "project", project );

        File licenseFile = mojo.getLicenseFile();
        long t0 = licenseFile.lastModified();

        // always assume pom is older than any file
        // since we can not ensure order of copy test resources
        LicensePluginTestHelper.setLastModified( project.getFile(), 0 );

        mojo.setVerbose( true );
        // then executing the mojo, will do NOT change the licence file
        mojo.execute();

        long t1 = licenseFile.lastModified();

        assertEquals( t0, t1 );

        // force to override the license file
        mojo.setForce( true );
        mojo.setVerbose( false );
        mojo.execute();
        t1 = licenseFile.lastModified();

        assertTrue( t1 > t0 );
    }

    @Test
    public void testTwo()
        throws Exception
    {

        // Mojo initialization
        File pomFile = new File( testPomDir, "testTwo.xml" );
        UpdateProjectLicenseMojo mojo = (UpdateProjectLicenseMojo) this.lookupMojo( GOAL, pomFile );

        File basedir = pomFile.getParentFile();
        mojo.setOutputDirectory( basedir );
        mojo.setLicenseFile( new File( basedir, mojo.getLicenseFile().toString() ) );
        MavenProject project = LicensePluginTestHelper.buildProject( getContainer(), pomFile );
        setVariableValueToObject( mojo, "project", project );

        File licenseFile = mojo.getLicenseFile();
        long t0 = licenseFile.lastModified();

        // always assume pom is older than any file
        // since we can not ensure order of copy test resources
        LicensePluginTestHelper.setLastModified( mojo.getProject().getFile(), 0 );

        mojo.setVerbose( true );

        // then executing the mojo, will do NOT change the licence file
        mojo.execute();

        long t1 = licenseFile.lastModified();

        assertEquals( t0, t1 );

        // force to override the license file
        mojo.setForce( true );
        mojo.setVerbose( false );

        mojo.execute();
        t1 = licenseFile.lastModified();

        assertTrue( t1 > t0 );
    }

    @Test
    public void testSkip()
        throws Exception
    {
        File pom = new File( testPomDir, "skip.xml" );
        UpdateProjectLicenseMojo mojo = (UpdateProjectLicenseMojo) this.lookupMojo( GOAL, pom );
        mojo.execute();
        Assert.assertTrue( mojo.isSkip() );
    }
}
