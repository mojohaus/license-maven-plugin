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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.model.LicenseStore;

import java.util.Arrays;

/**
 * Abstract mojo which using a {@link #licenseName} and owns a
 * {@link #licenseStore}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public abstract class AbstractLicenseNameMojo
    extends AbstractLicenseMojo
{

//    /**
//     * Encoding used to read and writes files.
//     * <p/>
//     * <b>Note:</b> If nothing is filled here, we will use the system
//     * property {@code file.encoding}.
//     *
//     * @parameter expression="${license.encoding}" default-value="${project.build.sourceEncoding}"
//     * @required
//     * @since 1.0
//     */
//    private String encoding;

    /**
     * To specify an external extra licenses repository resolver (says the base
     * url of the repository where the {@code license.properties} is present).
     *
     * @parameter expression="${license.licenseResolver}"
     * @since 1.0
     */
    private String licenseResolver;

    /**
     * A flag to keep a backup of every modified file.
     *
     * @parameter expression="${license.keepBackup}"  default-value="false"
     * @since 1.0
     */
    private boolean keepBackup;

    /**
     * Name of the license to use in the project.
     *
     * @parameter expression="${license.licenseName}"
     * @since 1.0
     */
    private String licenseName;

    /**
     * store of licenses
     */
    private LicenseStore licenseStore;

    /**
     * When is sets to {@code true}, will skip execution.
     * <p/>
     * This will take effects in method {@link #checkSkip()}.
     * So the method {@link #doAction()} will never be invoked.
     *
     * @return {@code true} if goal will not be executed
     */
    public abstract boolean isSkip();

    /**
     * Changes internal state {@code skip} to execute (or not) goal.
     *
     * @param skip new state value
     */
    public abstract void setSkip( boolean skip );

    @Override
    protected boolean checkSkip()
    {
        if ( isSkip() )
        {
            getLog().info( "skip flag is on, will skip goal." );
            return false;
        }
        return super.checkSkip();
    }

    @Override
    protected void init()
        throws Exception
    {

        if ( isSkip() )
        {
            return;
        }

        // init licenses store
        LicenseStore licenseStore = createLicenseStore( getLicenseResolver() );
        setLicenseStore( licenseStore );

        // check licenseName exists
        checkLicense( licenseName );
    }

    public License getMainLicense()
        throws IllegalArgumentException, IllegalStateException, MojoFailureException
    {

        // check license exists
        checkLicense( licenseName );

        // obtain license from his name
        License mainLicense = getLicense( licenseName );
        return mainLicense;
    }

    public License getLicense( String licenseName )
        throws IllegalArgumentException, IllegalStateException
    {
        if ( StringUtils.isEmpty( licenseName ) )
        {
            throw new IllegalArgumentException( "licenseName can not be null, nor empty" );
        }
        LicenseStore licenseStore = getLicenseStore();
        if ( licenseStore == null )
        {
            throw new IllegalStateException( "No license store initialized!" );
        }
        License mainLicense = licenseStore.getLicense( licenseName );
        return mainLicense;
    }

    /**
     * Check if the given license name is valid (not null, nor empty) and
     * exists in the license store.
     *
     * @param licenseName the name of the license to check
     * @throws IllegalArgumentException if license is not valid
     * @throws IllegalStateException    if license store is not initialized
     * @throws MojoFailureException     if license does not exist
     * @since 1.0
     */
    protected void checkLicense( String licenseName )
        throws IllegalArgumentException, IllegalStateException, MojoFailureException
    {
        if ( StringUtils.isEmpty( licenseName ) )
        {
            throw new IllegalArgumentException( "licenseName can not be null, nor empty" );
        }
        LicenseStore licenseStore = getLicenseStore();
        if ( licenseStore == null )
        {
            throw new IllegalStateException( "No license store initialized!" );
        }
        License mainLicense = licenseStore.getLicense( licenseName );
        if ( mainLicense == null )
        {
            throw new MojoFailureException( "License named '" + mainLicense + "' is unknown, use one of " +
                                                Arrays.toString( licenseStore.getLicenseNames() ) );
        }
    }

//    public final String getEncoding()
//    {
//        return encoding;
//    }
//
//    public final void setEncoding( String encoding )
//    {
//        this.encoding = encoding;
//    }

    public boolean isKeepBackup()
    {
        return keepBackup;
    }

    public String getLicenseName()
    {
        return licenseName;
    }

    public String getLicenseResolver()
    {
        return licenseResolver;
    }

    public LicenseStore getLicenseStore()
    {
        return licenseStore;
    }

    public void setKeepBackup( boolean keepBackup )
    {
        this.keepBackup = keepBackup;
    }

    public void setLicenseResolver( String licenseResolver )
    {
        this.licenseResolver = licenseResolver;
    }

    public void setLicenseName( String licenseName )
    {
        this.licenseName = licenseName;
    }

    public void setLicenseStore( LicenseStore licenseStore )
    {
        this.licenseStore = licenseStore;
    }

}
