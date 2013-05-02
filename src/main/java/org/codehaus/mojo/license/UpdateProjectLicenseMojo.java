package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.utils.FileUtil;

import java.io.File;

/**
 * Updates (or creates) the main project license file according to the given
 * license defines as {@link #licenseName}.
 * <p/>
 * Can also generate a bundled license file (to avoid collision names in
 * class-path). This file is by default generated in
 * {@code META-INF class-path directory}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
@Mojo( name = "update-project-license", requiresProject = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES )
public class UpdateProjectLicenseMojo
    extends AbstractLicenseNameMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Project license file to synchronize with main license defined in
     * descriptor file.
     *
     * @required
     * @since 1.0
     */
    @Parameter( property = "license.licenceFile", defaultValue = "${basedir}/LICENSE.txt" )
    protected File licenseFile;

    /**
     * The directory where to generate license resources.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @since 1.0
     */
    @Parameter( property = "license.outputDirectory", defaultValue = "target/generated-sources/license" )
    protected File outputDirectory;

    /**
     * A flag to copy the main license file in a bundled place.
     * <p/>
     * This is usefull for final application to have a none confusing location
     * to seek for the application license.
     * <p/>
     * If Sets to {@code true}, will copy the license file to the
     * {@link #bundleLicensePath} to {@link #outputDirectory}.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @since 1.0
     */
    @Parameter( property = "license.generateBundle", defaultValue = "false" )
    protected boolean generateBundle;

    /**
     * The path of the bundled license file to produce when
     * {@link #generateBundle} is on.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @since 1.0
     */
    @Parameter( property = "license.bundleLicensePath", defaultValue = "META-INF/${project.artifactId}-LICENSE.txt" )
    protected String bundleLicensePath;

    /**
     * A flag to force to generate project license file even if it is up-to-date.
     *
     * @since 1.0.0
     */
    @Parameter( property = "license.force", defaultValue = "false" )
    protected boolean force;

    /**
     * A flag to skip the goal.
     *
     * @since 1.0
     */
    @Parameter( property = "license.skipUpdateProjectLicense", defaultValue = "false" )
    protected boolean skipUpdateProjectLicense;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Flag to known if generate is needed.
     */
    private boolean doGenerate;

    // ----------------------------------------------------------------------
    // AbstractLicenceMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip()
    {
        return skipUpdateProjectLicense;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
        throws Exception
    {
        super.init();

        // must generate if file does not exist or pom never thant license file
        if ( licenseFile != null )
        {
            File pomFile = getProject().getFile();

            this.doGenerate = force || !licenseFile.exists() || licenseFile.lastModified() <= pomFile.lastModified();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doAction()
        throws Exception
    {

        License license = getLicense();

        if ( doGenerate )
        {

            getLog().info( "Will create or update license file [" + license.getName() + "] to " + licenseFile );
            if ( isVerbose() )
            {
                getLog().info( "detail of license :\n" + license );
            }

            if ( licenseFile.exists() && isKeepBackup() )
            {
                if ( isVerbose() )
                {
                    getLog().info( "backup " + licenseFile );
                }
                // copy it to backup file
                FileUtil.backupFile( licenseFile );
            }
        }

        // obtain license content
        String licenseContent = license.getLicenseContent( getEncoding() );

        if ( doGenerate )
        {

            // writes it root main license file
            FileUtil.writeString( licenseFile, licenseContent, getEncoding() );
        }

        if ( hasClassPath() )
        {

            // copy the license file to the resources directory
            File resourceTarget = new File( outputDirectory, licenseFile.getName() );
            FileUtil.copyFile( this.licenseFile, resourceTarget );

            addResourceDir( outputDirectory, "**/" + resourceTarget.getName() );

            if ( generateBundle )
            {

                // creates the bundled license file
                File bundleTarget = FileUtil.getFile( outputDirectory, bundleLicensePath );
                FileUtil.copyFile( licenseFile, bundleTarget );

                if ( !resourceTarget.getName().equals( bundleTarget.getName() ) )
                {

                    addResourceDir( outputDirectory, "**/" + bundleTarget.getName() );
                }
            }
        }
    }
}
