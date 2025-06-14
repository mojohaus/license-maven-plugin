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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.model.LicenseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display all available licenses.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Mojo(name = "license-list", requiresProject = false, requiresDirectInvocation = true)
public class LicenseListMojo extends AbstractLicenseMojo {
    private static final Logger LOG = LoggerFactory.getLogger(LicenseListMojo.class);

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The url of an extra license repository.
     * <p>
     * <strong>Note: </strong>If you want to refer to a file within this project, start the expression with
     * <code>${project.baseUri}</code>
     * </p>
     * @since 1.0
     */
    @Parameter(property = "extraResolver")
    private String extraResolver;

    /**
     * A flag to display also the content of each license.
     *
     * @since 1.0
     */
    @Parameter(property = "detail")
    private boolean detail;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Store of licenses.
     */
    private LicenseStore licenseStore;

    public LicenseListMojo(MavenProjectHelper projectHelper) {
        super(projectHelper);
    }

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip() {
        // can't skip this goal since direct invocation is required
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() throws Exception {

        // obtain licenses store
        licenseStore = LicenseStore.createLicenseStore(extraResolver);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doAction() throws MojoExecutionException, MojoFailureException {
        StringBuilder buffer = new StringBuilder();

        if (isVerbose()) {
            buffer.append("\n\n-------------------------------------------------------------------------------\n");
            buffer.append("                           maven-license-plugin\n");
            buffer.append("-------------------------------------------------------------------------------\n\n");
        }
        buffer.append("Available licenses :\n\n");

        List<String> names = Arrays.asList(licenseStore.getLicenseNames());

        int maxLength = 0;
        for (String name : names) {
            if (name.length() > maxLength) {
                maxLength = name.length();
            }
        }
        Collections.sort(names);

        String pattern = " * %1$-" + maxLength + "s : %2$s\n";
        for (String licenseName : names) {
            License license = licenseStore.getLicense(licenseName);
            buffer.append(String.format(pattern, licenseName, license.getDescription()));
            if (detail) {
                try {
                    buffer.append("\n");
                    buffer.append(license.getHeaderContent(getEncoding()));
                    buffer.append("\n\n");
                } catch (IOException ex) {
                    throw new MojoExecutionException(
                            "could not instanciate license with name " + licenseName + " for reason " + ex.getMessage(),
                            ex);
                }
            }
        }
        LOG.info("{}", buffer);
    }
}
