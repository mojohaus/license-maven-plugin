package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2016 CodeLutin, Codehaus, Tony Chemit
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
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.api.DependenciesToolException;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.mojo.license.api.ThirdPartyToolException;

/**
 * Generates a report of all third-parties detected in the module.
 *
 * Created on 22/05/16.
 *
 * @author Tony Chemit - dev@tchemit.fr
 * @since 1.10
 */
@Mojo(name = "aggregate-third-party-report", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST)
public class AggregatorThirdPartyReport extends AbstractThirdPartyReport {

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Skip to generate the report.
     *
     * @since 1.10
     */
    @Parameter(property = "license.skipAggregateThirdPartyReport", defaultValue = "false")
    private boolean skipAggregateThirdPartyReport;

    /**
     * To generate report only on root module.
     *
     * Default value is {@code true}, since aggregate report should only be executed on root module.
     *
     * @since 1.10
     */
    @Parameter(
            property = "license.executeOnlyOnRootModule",
            alias = "aggregateThirdPartyReport.executeOnlyOnRootModule",
            defaultValue = "true")
    private boolean executeOnlyOnRootModule;

    // ----------------------------------------------------------------------
    // MavenReport Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getOutputName() {
        return "aggregate-third-party-report";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canGenerateReport() {
        return !skipAggregateThirdPartyReport
                && (!executeOnlyOnRootModule || getProject().isExecutionRoot());
    }

    // ----------------------------------------------------------------------
    // AbstractThirdPartyReport Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected Collection<ThirdPartyDetails> createThirdPartyDetails()
            throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException,
                    DependenciesToolException, MojoExecutionException {

        Collection<ThirdPartyDetails> details = new LinkedHashSet<>();

        for (MavenProject reactorProject : reactorProjects) {

            Collection<ThirdPartyDetails> thirdPartyDetails = createThirdPartyDetails(reactorProject, true);
            details.addAll(thirdPartyDetails);
        }

        return details;
    }
}
