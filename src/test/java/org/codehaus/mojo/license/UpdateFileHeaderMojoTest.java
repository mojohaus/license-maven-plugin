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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.codehaus.mojo.license.UpdateFileHeaderMojo.FileState;

/**
 * Tests the {@link UpdateFileHeaderMojo}.
 * 
 * @author tchemit <chemit@codelutin.com>
 * @since 2.1
 */
@RunWith( JUnit4.class )
public class UpdateFileHeaderMojoTest
    extends AbstractMojoTestCase
{

    public static final String GOAL = "update-file-header";

    @Rule
    public TestName name = new TestName();

    public static File testPomDir;

    private Set<File> uptodates;

    private Set<File> updates;

    private Set<File> adds;

    private Set<File> fails;

    private Set<File> ignores;

    private Set<File> process;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        testPomDir = new File( getBasedir(), "target/test-classes/org/codehaus/mojo/license/updateFileHeaderMojoTest/" );
    }

    public UpdateFileHeaderMojo initMojo( String testName )
        throws Exception
    {
        File pomFile = new File( testPomDir, testName + ".xml" );
        UpdateFileHeaderMojo mojo = (UpdateFileHeaderMojo) this.lookupMojo( GOAL, pomFile );

        File basedir = pomFile.getParentFile();
        MavenProject project = LicensePluginTestHelper.buildProject( getContainer(), pomFile );
        project.setBasedir( new File( basedir, name.getMethodName() ) );
        setVariableValueToObject( mojo, "project", project );

        return mojo;
    }

    @Test
    public void addLicense()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );

        File beanFile = getMyBeanFile( name.getMethodName() );

        String content;

        // content = PluginHelper.readAsString(f, mojo.getEncoding());
        //
        // // check no header
        // checkPattern(f, content, "Copyright (C) 2010", false);
        // checkPattern(f, content, "Project name : maven-license-plugin-java", false);
        // checkPattern(f, content, "Organization is CodeLutin", false);

        execute( true, mojo );
        Assert.assertEquals( 1, process.size() );

        if ( uptodates == null )
        {
            Assert.assertEquals( 1, adds.size() );
        }
        else
        {
            Assert.assertEquals( 1, uptodates.size() );
            Assert.assertNull( adds );
        }
        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header
        checkPattern( beanFile, content, "Copyright (C) 2010", true );
        checkPattern( beanFile, content, "Project name : maven-license-plugin-java", true );
        checkPattern( beanFile, content, "Organization is CodeLutin", true );

        // re execute mojo to make sure we are in uptodate
        execute( false, mojo );

        Assert.assertEquals( 1, process.size() );
        Assert.assertEquals( 1, uptodates.size() );

        int oldLength = content.length();
        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );
        int newLength = content.length();

        Assert.assertEquals( oldLength, newLength );
    }

    @Test
    public void all()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );

        mojo.setClearAfterOperation( false );
        execute( true, mojo );
        Assert.assertEquals( 4, process.size() );

        if ( uptodates == null )
        {
            // first invocation
            Assert.assertEquals( 1, updates.size() );
            Assert.assertEquals( 1, adds.size() );
            Assert.assertNull( uptodates );
        }
        else
        {
            Assert.assertNotNull( uptodates );
            Assert.assertEquals( 2, uptodates.size() );
            Assert.assertNull( adds );
            Assert.assertNull( updates );
        }
        Assert.assertEquals( 1, fails.size() );
        Assert.assertEquals( 1, ignores.size() );

        execute( false, mojo );

        Assert.assertEquals( 4, process.size() );
        Assert.assertNotNull( uptodates );
        Assert.assertNull( updates );
        // Assert.assertEquals(2, uptodates.size());
        Assert.assertEquals( 2, uptodates.size() );
        Assert.assertNull( adds );
        Assert.assertNull( updates );
        Assert.assertEquals( 1, fails.size() );
        Assert.assertEquals( 1, ignores.size() );

        mojo.setClearAfterOperation( true );
        mojo.execute();
        Assert.assertEquals( 0, process.size() );
        for ( FileState state : FileState.values() )
        {

            Assert.assertNull( mojo.getFiles( state ) );
        }
    }

    @Test
    public void updateLicense()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );

        File beanFile = getMyBeanFile( name.getMethodName() );

        String content;

        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header
        checkPattern( beanFile, content, "Copyright (C) 2000 Codelutin do NOT update!", true );
        checkPattern( beanFile, content, "License Test :: do NOT update!", true );
        // checkPattern(f, content, "Fake to be removed!", true);

        execute( true, mojo );

        Assert.assertEquals( 1, process.size() );
        if ( uptodates != null )
        {

            Assert.assertEquals( 1, uptodates.size() );
            Assert.assertNull( updates );
        }
        else
        {
            Assert.assertNull( uptodates );
            Assert.assertEquals( 1, updates.size() );
        }
        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header (description + copyright) does not changed
        checkPattern( beanFile, content, "Copyright (C) 2000 Codelutin do NOT update!", true );
        checkPattern( beanFile, content, "License Test :: do NOT update!", true );

        // check license changed
        checkPattern( beanFile, content, "Fake to be removed!", false );

        execute( false, mojo );

        Assert.assertEquals( 1, process.size() );
        Assert.assertNotNull( uptodates );
        Assert.assertEquals( 1, uptodates.size() );
        Assert.assertNull( updates );
        // Assert.assertEquals(1, uptodates.size());
        // Assert.assertNull(updates);

        int oldLength = content.length();
        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );
        int newLength = content.length();

        Assert.assertEquals( oldLength, newLength );
    }

    @Test
    public void failLicense()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );

        File beanFile = getMyBeanFile( name.getMethodName() );

        String content;

        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header
        checkPattern( beanFile, content, "Copyright (C) 2000 Codelutin do NOT update!", true );
        checkPattern( beanFile, content, "License Test :: do NOT update!", true );
        checkPattern( beanFile, content, "License content do NOT update!", true );
        // checkPattern(f, content, "Fake to be removed!", true);

        execute( true, mojo );
        Assert.assertEquals( 1, process.size() );
        Assert.assertEquals( 1, fails.size() );

        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header does not changed
        checkPattern( beanFile, content, "Copyright (C) 2000 Codelutin do NOT update!", true );
        checkPattern( beanFile, content, "License Test :: do NOT update!", true );
        checkPattern( beanFile, content, "License content do NOT update!", true );

        execute( false, mojo );
        Assert.assertEquals( 1, process.size() );
        Assert.assertEquals( 1, fails.size() );

        content = LicensePluginTestHelper.readAsString( beanFile, mojo.getEncoding() );

        // check header does not changed
        checkPattern( beanFile, content, "Copyright (C) 2000 Codelutin do NOT update!", true );
        checkPattern( beanFile, content, "License Test :: do NOT update!", true );
        checkPattern( beanFile, content, "License content do NOT update!", true );
    }

    @Test
    public void ignoreLicense()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );

        File f = getMyBeanFile( name.getMethodName() );

        String content;

        content = LicensePluginTestHelper.readAsString( f, mojo.getEncoding() );

        // check header
        checkPattern( f, content, "Copyright (C) 2000 Codelutin Do not update!", true );
        checkPattern( f, content, " * %" + "%Ignore-License", true );
        checkPattern( f, content, "yet another license", true );
        checkPattern( f, content, "NEVER_FINd_ME!", false );

        execute( true, mojo );

        Assert.assertEquals( 1, process.size() );
        Assert.assertEquals( 1, ignores.size() );

        content = LicensePluginTestHelper.readAsString( f, mojo.getEncoding() );

        // check header (description + copyright) does not changed
        checkPattern( f, content, "Copyright (C) 2000 Codelutin Do not update!", true );
        checkPattern( f, content, " * %" + "%Ignore-License", true );
        checkPattern( f, content, "yet another license", true );
        checkPattern( f, content, "NEVER_FINd_ME!", false );

        execute( false, mojo );

        Assert.assertEquals( 1, process.size() );
        Assert.assertEquals( 1, ignores.size() );

        content = LicensePluginTestHelper.readAsString( f, mojo.getEncoding() );

        // check header (description + copyright) does not changed
        checkPattern( f, content, "Copyright (C) 2000 Codelutin Do not update!", true );
        checkPattern( f, content, " * %" + "%Ignore-License", true );
        checkPattern( f, content, "yet another license", true );
        checkPattern( f, content, "NEVER_FINd_ME!", false );
    }

    @Test
    public void skip()
        throws Exception
    {

        UpdateFileHeaderMojo mojo = initMojo( name.getMethodName() );
        mojo.execute();
        Assert.assertTrue( mojo.isSkip() );
    }

    protected void execute( boolean verbose, UpdateFileHeaderMojo mojo )
        throws Exception
    {
        mojo.setVerbose( verbose );
        mojo.execute();

        process = mojo.getProcessedFiles();
        uptodates = mojo.getFiles( FileState.uptodate );
        updates = mojo.getFiles( FileState.update );
        adds = mojo.getFiles( FileState.add );
        fails = mojo.getFiles( FileState.fail );
        ignores = mojo.getFiles( FileState.ignore );
    }

    protected File getMyBeanFile( String testName )
    {
        return new File( getBasedir(), "target/test-classes/org/codehaus/mojo/license/updateFileHeaderMojoTest/"
            + testName + "/src/MyBean.java" );
    }

    public void checkPattern( File file, String content, String pattern, boolean required )
        throws IOException
    {

        String errorMessage = required ? "could not find the pattern : " : "should not have found pattern :";

        // checks pattern found (or not) in file's content
        assertEquals( errorMessage + pattern + " in '" + file + "'", required, content.contains( pattern ) );
    }

}
