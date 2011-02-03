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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.model.LicenseStore;
import org.nuiton.plugin.AbstractPlugin;

import java.io.IOException;

/**
 * Abstract license mojo.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 2.1
 */
public abstract class AbstractLicenseMojo
    extends AbstractPlugin
{

    /**
     * Current maven session. (used to launch certain mojo once by build).
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     * @since 2.3
     */
    private MavenSession session;

    /**
     * The reacted project.
     *
     * @parameter default-value="${project}"
     * @required
     * @since 2.1
     */
    private MavenProject project;

    /**
     * Flag to activate verbose mode.
     * <p/>
     * <b>Note:</b> Verbose mode is always on if you starts a debug maven instance
     * (says via {@code -X}).
     *
     * @parameter expression="${license.verbose}"  default-value="${maven.verbose}"
     * @since 2.1
     */
    private boolean verbose;

    public final MavenProject getProject()
    {
        return project;
    }

    public final void setProject( MavenProject project )
    {
        this.project = project;
    }

    public final boolean isVerbose()
    {
        return verbose;
    }

    public final void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    public final MavenSession getSession()
    {
        return session;
    }

    public final void setSession( MavenSession session )
    {
        this.session = session;
    }

    public final long getBuildTimestamp()
    {
        return session.getStartTime().getTime();
    }

    protected LicenseStore createLicenseStore( String... extraResolver )
        throws MojoExecutionException
    {
        LicenseStore store;
        try
        {
            store = new LicenseStore();
            store.addJarRepository();
            if ( extraResolver != null )
            {
                for ( String s : extraResolver )
                {
                    if ( s != null && !s.trim().isEmpty() )
                    {
                        getLog().info( "adding extra resolver " + s );
                        store.addRepository( s );
                    }
                }
            }
            store.init();
        }
        catch ( IllegalArgumentException ex )
        {
            throw new MojoExecutionException( "could not obtain the license repository", ex );
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( "could not obtain the license repository", ex );
        }
        return store;
    }

}
