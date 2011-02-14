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
     * License loaded from the {@link #licenseName}.
     */
    private License license;

    /**
     * Store of available licenses.
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
        licenseStore = LicenseStore.createLicenseStore( getLog(), getLicenseResolver() );

        // check licenseName exists
        license = getLicense( licenseName, true );
    }

    public License getLicense( String licenseName, boolean checkIfExists )
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
        License license = licenseStore.getLicense( licenseName );
        if ( checkIfExists && license == null )
        {
            throw new IllegalArgumentException( "License named '" + licenseName + "' is unknown, use one of " +
                                                    Arrays.toString( licenseStore.getLicenseNames() ) );
        }
        return license;
    }

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

    public License getLicense()
    {
        return license;
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
