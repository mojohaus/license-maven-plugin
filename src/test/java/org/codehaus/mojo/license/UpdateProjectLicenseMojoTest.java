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

import org.junit.Assert;
import org.junit.Test;
import org.nuiton.plugin.AbstractMojoTest;
import org.nuiton.plugin.PluginHelper;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateProjectLicenseMojoTest
    extends AbstractMojoTest<UpdateProjectLicenseMojo>
{

    @Override
    protected String getGoalName( String methodName )
    {
        return "update-project-license";
    }

    @Override
    protected void setUpMojo( UpdateProjectLicenseMojo mojo, File pomFile )
        throws Exception
    {
        super.setUpMojo( mojo, pomFile );
        if ( mojo.isSkip() )
        {
            return;
        }
        // license is where the pom is
        File outputDirectory = pomFile.getParentFile();
        mojo.setOutputDirectory( outputDirectory );
//        mojo.setDescriptor(new File(outputDirectory, mojo.getDescriptor().getName()));
        if ( !outputDirectory.exists() )
        {
            if ( !outputDirectory.mkdirs() )
            {
                throw new IOException( "could not create directory : " + outputDirectory );
            }
        }
        mojo.setLicenseFile( new File( pomFile.getParentFile(), mojo.getLicenseFile().toString() ) );

        log.info( "pom             : " + getRelativePathFromBasedir( mojo.getProject().getFile() ) );
        log.info( "outputDirectory : " + getRelativePathFromBasedir( mojo.getOutputDirectory() ) );
        log.info( "licenseFile     : " + getRelativePathFromBasedir( mojo.getLicenseFile() ) );
    }

    @Test
    public void testOne()
        throws Exception
    {

        UpdateProjectLicenseMojo mojo = getMojo();

        File licenseFile = mojo.getLicenseFile();
        long t0 = licenseFile.lastModified();

        // always assume pom is older than any file
        // since we can not ensure order of copy test resources
        PluginHelper.setLastModified( mojo.getProject().getFile(), 0 );

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

        UpdateProjectLicenseMojo mojo = getMojo();

        File licenseFile = mojo.getLicenseFile();
        long t0 = licenseFile.lastModified();

        // always assume pom is older than any file
        // since we can not ensure order of copy test resources
        PluginHelper.setLastModified( mojo.getProject().getFile(), 0 );

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
    public void skip()
        throws Exception
    {

        UpdateProjectLicenseMojo mojo = getMojo();
        mojo.execute();
        Assert.assertTrue( mojo.isSkip() );

    }
}
