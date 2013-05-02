package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2012 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The goal to update (or add) the header on project source files.
 * <p/>
 * This goal replace the {@code update-header} goal which can not deal with
 * Copyright.
 * <p/>
 * This goal use a specific project file descriptor {@code project.xml} to
 * describe all files to update for a whole project.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
@Mojo( name = "update-file-header", requiresProject = true )
public class UpdateFileHeaderMojo
    extends AbstractFileHeaderMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A flag to fail the build if {@link #dryRun} flag is on and some files with
     * no header are detected.
     * <p/>
     * <strong>Note:</strong> If {@link #dryRun} flag is not set, there is no effect.
     *
     * @since 1.2
     */
    @Parameter( property = "license.failOnMissingHeader", defaultValue = "false" )
    protected boolean failOnMissingHeader;

    /**
     * A flag to fail the build if {@link #dryRun} flag is on and some files with headers
     * to update.
     * <p/>
     * <strong>Note:</strong> If {@link #dryRun} flag is not set, there is no effect.
     *
     * @since 1.2
     */
    @Parameter( property = "license.failOnNotUptodateHeader", defaultValue = "false" )
    protected boolean failOnNotUptodateHeader;

    /**
     * A flag to skip the goal.
     *
     * @since 1.0
     */
    @Parameter( property = "license.skipUpdateLicense", defaultValue = "false" )
    protected boolean skipUpdateLicense;

    /**
     * A flag to test plugin but modify no file.
     *
     * @since 1.0
     */
    @Parameter( property = "dryRun", defaultValue = "false" )
    protected boolean dryRun;

    // ----------------------------------------------------------------------
    // AbstractLicenceMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip()
    {
        return skipUpdateLicense;
    }

    // ----------------------------------------------------------------------
    // AbstractFileHeaderMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFailOnMissingHeader()
    {
        return failOnMissingHeader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFailOnNotUptodateHeader()
    {
        return failOnNotUptodateHeader;
    }

}
