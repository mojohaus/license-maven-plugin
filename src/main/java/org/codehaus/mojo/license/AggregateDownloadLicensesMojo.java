package org.codehaus.mojo.license;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.*;

/**
 * Download the license files of all aggregated dependencies of the current project, and generate a summary file containing a list
 * of all dependencies and their licenses.
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
    @Parameter( property = "license.executeOnlyOnRootModule", alias = "aggregateDownloadLicenses.executeOnlyOnRootModule", defaultValue = "true" )
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
    protected SortedMap<String, MavenProject> getDependencies()
    {
        SortedMap<String, MavenProject>  result = new TreeMap<>();

        for ( MavenProject reactorProject : reactorProjects )
        {
            SortedMap<String, MavenProject> dependencies = getDependencies( reactorProject );
            result.putAll( dependencies);
        }
        return result;
    }
}
