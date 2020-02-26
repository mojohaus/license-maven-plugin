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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.api.ResolvedProjectDependencies;
import org.codehaus.mojo.license.download.LicensedArtifact;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Download the license files of all aggregated dependencies of the current project, and generate a summary file
 * containing a list of all dependencies and their licenses.
 * <p>
 * The license files will be downloaded to {@link AbstractDownloadLicensesMojo#licensesOutputDirectory} to be included
 * in the final packaging of the project if desired. The licenses are downloaded from the url field of the dependency
 * POM.
 * <p>
 * If the license information (license name and license URL) is missing or otherwise broken in a dependency POM, this
 * mojo offers several fallback options:
 * <ul>
 * <li>{@link AbstractDownloadLicensesMojo#licensesConfigFile}</li>
 * <li>{@link AbstractDownloadLicensesMojo#errorRemedy}</li>
 * <li>{@link AbstractDownloadLicensesMojo#licenseUrlReplacements}</li>
 * <li>{@link AbstractDownloadLicensesMojo#licenseUrlFileNames}</li>
 * </ul>
 *
 * Created on 23/05/16.
 *
 * @author Tony Chemit - dev@tchemit.fr
 * @since 1.10
 */
@Mojo( name = "aggregate-download-licenses", requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase = LifecyclePhase.PACKAGE, aggregator = true )
public class AggregateDownloadLicensesMojo
    extends AbstractDownloadLicensesMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Skip to generate the report.
     *
     * @since 1.10
     */
    @Parameter( property = "license.skipAggregateDownloadLicenses", defaultValue = "false" )
    private boolean skipAggregateDownloadLicenses;

    /**
     * To generate report only on root module.
     *
     * Default value is {@code true}, since aggregate mojo should only be executed on root module.
     *
     * @since 1.10
     */
    @Parameter( property = "license.executeOnlyOnRootModule",
            alias = "aggregateDownloadLicenses.executeOnlyOnRootModule", defaultValue = "true" )
    private boolean executeOnlyOnRootModule;

    /**
     * The projects in the reactor.
     *
     * @since 1.10
     */
    @Parameter( property = "reactorProjects", readonly = true, required = true )
    private List<MavenProject> reactorProjects;

    // ----------------------------------------------------------------------
    // AbstractDownloadLicensesMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected boolean isSkip()
    {
        return skipAggregateDownloadLicenses || ( executeOnlyOnRootModule && !getProject().isExecutionRoot() );
    }

    /**
     * {@inheritDoc}
     */
    protected Map<String, LicensedArtifact> getDependencies()
    {
        final Map<String, LicensedArtifact> result = new TreeMap<>();

        for ( MavenProject p : reactorProjects )
        {
            licensedArtifactResolver.loadProjectDependencies( new ResolvedProjectDependencies( p.getArtifacts(),
                                                                                       p.getDependencyArtifacts() ),
                                                      this, remoteRepositories, result );
        }
        return result;
    }
}
