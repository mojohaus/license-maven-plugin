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
 * The goal to check if the state of header on project source files.
 * <p/>
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.2
 */
@Mojo( name = "check-file-header", requiresProject = true )
public class CheckFileHeaderMojo
    extends AbstractFileHeaderMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A flag to fail the build if there is some files with no header are detected.
     *
     * @since 1.2
     */
    @Parameter( property = "license.failOnMissingHeader", defaultValue = "false" )
    protected boolean failOnMissingHeader;

    /**
     * A flag to fail the build if there is some files with headers to update.
     *
     * @since 1.2
     */
    @Parameter( property = "license.failOnNotUptodateHeader", defaultValue = "false" )
    protected boolean failOnNotUptodateHeader;

    /**
     * A flag to skip the goal.
     *
     * @since 1.2
     */
    @Parameter( property = "license.skipCheckLicense", defaultValue = "false" )
    protected boolean skipCheckLicense;

    // ----------------------------------------------------------------------
    // AbstractLicenseFileNameMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip()
    {
        return skipCheckLicense;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSkip( boolean skip )
    {
        this.skipCheckLicense = skip;
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
        // this mojo should never update any files.
        return true;
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
