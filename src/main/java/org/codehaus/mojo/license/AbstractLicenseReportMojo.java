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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.mojo.license.api.DefaultThirdPartyHelper;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.ThirdPartyHelper;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.plexus.i18n.I18N;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Base class for all license reports.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public abstract class AbstractLicenseReportMojo
    extends AbstractMavenReport
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The output directory for the report. Note that this parameter is only evaluated if the goal is run directly from
     * the command line. If the goal is run indirectly as part of a site generation, the output directory configured in
     * the Maven Site Plugin is used instead.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "${project.reporting.outputDirectory}", required = true )
    private File outputDirectory;

    /**
     * Flag to activate verbose mode.
     * <p/>
     * <b>Note:</b> Verbose mode is always on if you starts a debug maven instance
     * (says via {@code -X}).
     *
     * @since 1.0
     */
    @Parameter( property = "license.verbose", defaultValue = "${maven.verbose}" )
    private boolean verbose;

    /**
     * Encoding used to read and writes files.
     * <p/>
     * <b>Note:</b> If nothing is filled here, we will use the system
     * property {@code file.encoding}.
     *
     * @since 1.0
     */
    @Parameter( property = "license.encoding", defaultValue = "${project.build.sourceEncoding}" )
    private String encoding;

    /**
     * Local Repository.
     *
     * @since 1.1
     */
    @Parameter( property = "localRepository", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @since 1.1
     */
    @Parameter( property = "project.remoteArtifactRepositories", required = true, readonly = true )
    private List remoteRepositories;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * The Maven Project.
     *
     * @since 1.1
     */
    @Component
    private MavenProject project;

    /**
     * Doxia Site Renderer component.
     *
     * @since 1.1
     */
    @Component
    private Renderer siteRenderer;

    /**
     * Internationalization component.
     *
     * @since 1.1
     */
    @Component
    private I18N i18n;

    /**
     * dependencies tool.
     *
     * @since 1.1
     */
    @Component
    private DependenciesTool dependenciesTool;

    /**
     * third party tool.
     *
     * @since 1.1
     */
    @Component
    private ThirdPartyTool thirdPartyTool;

    /**
     * Third-party helper (high level tool with common code for mojo and report).
     */
    private ThirdPartyHelper helper;

    // ----------------------------------------------------------------------
    // Abstract Methods
    // ----------------------------------------------------------------------

    /**
     * When is sets to {@code true}, will skip execution.
     * <p/>
     * This will take effect in at the very begin of the {@link #execute()}
     * before any initialisation of goal.
     *
     * @return {@code true} if goal will not be executed
     */
    public abstract boolean isSkip();

    /**
     * Generates the report.
     *
     * @param locale the locale to generate the report for.
     * @param sink   the report formatting tool.
     * @throws MavenReportException   when things go wrong.
     * @throws MojoExecutionException when things go wrong.
     * @throws MojoFailureException   when things go wrong.
     */
    protected abstract void doGenerateReport( Locale locale, Sink sink )
        throws MavenReportException, MojoExecutionException, MojoFailureException;
    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    protected ThirdPartyHelper getHelper()
    {
        if ( helper == null )
        {
            helper = new DefaultThirdPartyHelper( project, encoding, verbose, dependenciesTool, thirdPartyTool,
                                                  localRepository, remoteRepositories, getLog() );
        }
        return helper;
    }

    // ----------------------------------------------------------------------
    // AbstractMavenReport Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {

        boolean mustSkip = isSkip();

        if ( mustSkip )
        {
            getLog().info( "skip flag is on, will skip goal." );
            return;
        }
        try
        {
            doGenerateReport( locale, getSink() );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( MojoFailureException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * {@inheritDoc}
     */
    protected String getOutputDirectory()
    {
        if ( !outputDirectory.isAbsolute() )
        {
            outputDirectory = new File( project.getBasedir(), outputDirectory.getPath() );
        }

        return outputDirectory.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    protected Renderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription( Locale locale )
    {
        return getText( locale, "report.description" );
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return getText( locale, "report.title" );
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    /**
     * Gets the localized message for this report.
     *
     * @param locale the locale.
     * @param key    the message key.
     * @return the message.
     */
    public String getText( Locale locale, String key )
    {
        return i18n.getString( getOutputName(), locale, key );
    }

    public I18N getI18n()
    {
        return i18n;
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    public final String getEncoding()
    {
        return encoding;
    }
}
