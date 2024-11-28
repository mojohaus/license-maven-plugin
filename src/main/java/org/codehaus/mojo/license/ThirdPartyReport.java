package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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

import java.io.IOException;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.DependenciesToolException;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.mojo.license.api.ThirdPartyToolException;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generates a report of all third-parties detected in the module.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.1
 */
@Mojo(name = "third-party-report", requiresDependencyResolution = ResolutionScope.TEST)
public class ThirdPartyReport extends AbstractThirdPartyReport {

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Skip to generate the report.
     *
     * @since 1.1
     */
    @Parameter(property = "license.skipThirdPartyReport", defaultValue = "false")
    private boolean skipThirdPartyReport;

    @Inject
    public ThirdPartyReport(I18N i18n, DependenciesTool dependenciesTool, ThirdPartyTool thirdPartyTool) {
        super(i18n, dependenciesTool, thirdPartyTool);
    }

    // ----------------------------------------------------------------------
    // MavenReport Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public String getOutputName() {
        return "third-party-report";
    }

    @Override
    public boolean canGenerateReport() {
        return !skipThirdPartyReport;
    }

    // ----------------------------------------------------------------------
    // AbstractThirdPartyReport Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection<ThirdPartyDetails> createThirdPartyDetails()
            throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException,
                    DependenciesToolException, MojoExecutionException {
        return createThirdPartyDetails(getProject(), false);
    }
}
