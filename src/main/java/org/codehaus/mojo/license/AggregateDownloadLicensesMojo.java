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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

/**
 * Download the license files of all aggregated dependencies of the current project, and generate a summary file
 * containing a list of all dependencies and their licenses.
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
    protected Set<MavenProject> getDependencies()
    {
        Set<MavenProject> result = new HashSet<>();

        for ( MavenProject reactorProject : reactorProjects )
        {
            SortedMap<String, MavenProject> dependencies = getDependencies( reactorProject );
            result.addAll( dependencies.values() );
        }
        return result;
    }
}
