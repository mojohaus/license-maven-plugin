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
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.2
 */
@Mojo( name = "check-file-header", threadSafe = true )
public class CheckFileHeaderMojo extends AbstractFileHeaderMojo
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
    private boolean failOnMissingHeader;

    /**
     * A flag to fail the build if there is some files with headers to update.
     *
     * @since 1.2
     */
    @Parameter( property = "license.failOnNotUptodateHeader", defaultValue = "false" )
    private boolean failOnNotUptodateHeader;

    /**
     * A flag to skip the goal.
     *
     * @since 1.2
     */
    @Parameter( property = "license.skipCheckLicense", defaultValue = "false" )
    private boolean skipCheckLicense;

    // ----------------------------------------------------------------------
    // AbstractLicenseFileNameMojo Implementation
    // ----------------------------------------------------------------------

    @Override
    public boolean isSkip()
    {
        return skipCheckLicense;
    }

    // ----------------------------------------------------------------------
    // AbstractFileHeaderMojo Implementation
    // ----------------------------------------------------------------------

    @Override
    protected boolean isDryRun()
    {
        // this mojo should never update any files.
        return true;
    }

    @Override
    protected boolean isFailOnMissingHeader()
    {
        return failOnMissingHeader;
    }

    @Override
    protected boolean isFailOnNotUptodateHeader()
    {
        return failOnNotUptodateHeader;
    }
}
