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

import javax.inject.Inject;

import java.io.File;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;

/**
 * The goal to check if there are license files in project.
 *
 * @author swalendzik
 * @since 2.7
 */
@Mojo(name = "check-project-license", threadSafe = true)
public class CheckProjectLicenseMojo extends AbstractLicenseMojo {

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Project license file to check.
     *
     * @since 2.7
     */
    @Parameter(property = "license.licenceFile", defaultValue = "LICENSE.txt")
    private File licenseFile;
    /**
     * A flag to fail the build if there is missing any license file.
     *
     * @since 2.7
     */
    @Parameter(property = "license.failOnMissingLicense", defaultValue = "false")
    private boolean failOnMissingLicense;

    /**
     * A flag to skip the goal.
     *
     * @since 2.7
     */
    @Parameter(property = "license.skipCheckLicense", defaultValue = "false")
    private boolean skipCheckLicense;

    @Inject
    public CheckProjectLicenseMojo(MavenProjectHelper projectHelper) {
        super(projectHelper);
    }

    // ----------------------------------------------------------------------
    // AbstractLicenseFileNameMojo Implementation
    // ----------------------------------------------------------------------

    @Override
    public boolean isSkip() {
        return skipCheckLicense;
    }

    @Override
    protected void init() throws Exception {
        // nothing to do
    }

    @Override
    protected void doAction() throws Exception {
        if (!licenseFile.exists()) {
            String message = "There is module without license " + this.licenseFile.getAbsolutePath()
                    + ". Use license:update-project-license goal to generate";
            if (failOnMissingLicense) {
                throw new MojoFailureException(message);
            } else {
                getLog().warn(message);
            }
        }
    }
}
