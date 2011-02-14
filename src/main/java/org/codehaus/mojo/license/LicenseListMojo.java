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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.model.LicenseStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Display all available licenses.
 *
 * @author tchemit <chemit@codelutin.com>
 * @goal license-list
 * @requiresProject false
 * @requiresDirectInvocation
 * @since 1.0
 */
public class LicenseListMojo
    extends AbstractLicenseMojo
{

    /**
     * the url of an extra license repository.
     *
     * @parameter expression="${extraResolver}"
     * @since 1.0
     */
    private String extraResolver;

    /**
     * A flag to display also the content of each license.
     *
     * @parameter expression="${detail}"
     * @since 1.0
     */
    private boolean detail;

    /**
     * store of licenses
     */
    protected LicenseStore licenseStore;

    @Override
    protected void init()
        throws Exception
    {

        // obtain licenses store
        licenseStore = LicenseStore.createLicenseStore( getLog(), getExtraResolver() );
    }

    @Override
    public void doAction()
        throws MojoExecutionException, MojoFailureException
    {
        StringBuilder buffer = new StringBuilder();

        if ( isVerbose() )
        {
            buffer.append( "\n\n-------------------------------------------------------------------------------\n" );
            buffer.append( "                           maven-license-plugin\n" );
            buffer.append( "-------------------------------------------------------------------------------\n\n" );
        }
        buffer.append( "Available licenses :\n\n" );

        List<String> names = Arrays.asList( licenseStore.getLicenseNames() );

        int maxLength = 0;
        for ( String name : names )
        {
            if ( name.length() > maxLength )
            {
                maxLength = name.length();
            }
        }
        Collections.sort( names );

        String pattern = " * %1$-" + maxLength + "s : %2$s\n";
        for ( String licenseName : names )
        {
            License license = licenseStore.getLicense( licenseName );
            buffer.append( String.format( pattern, licenseName, license.getDescription() ) );
            if ( isDetail() )
            {
                try
                {
                    buffer.append( "\n" );
                    buffer.append( license.getHeaderContent( getEncoding() ) );
                    buffer.append( "\n\n" );
                }
                catch ( IOException ex )
                {
                    throw new MojoExecutionException(
                        "could not instanciate license with name " + licenseName + " for reason " + ex.getMessage(),
                        ex );
                }
            }
        }
        getLog().info( buffer.toString() );
    }

    public String getExtraResolver()
    {
        return extraResolver;
    }

    public boolean isDetail()
    {
        return detail;
    }

    public void setExtraResolver( String extraResolver )
    {
        this.extraResolver = extraResolver;
    }

    public void setDetail( boolean detail )
    {
        this.detail = detail;
    }
}
