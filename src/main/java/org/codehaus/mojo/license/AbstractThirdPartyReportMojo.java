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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.mojo.license.api.DefaultThirdPartyDetails;
import org.codehaus.mojo.license.api.DefaultThirdPartyHelper;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.DependenciesToolException;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.mojo.license.api.ThirdPartyHelper;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.mojo.license.api.ThirdPartyToolException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.i18n.I18N;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Base class for third-party reports.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.1
 */
public abstract class AbstractThirdPartyReportMojo extends AbstractMavenReport implements MavenProjectDependenciesConfigurator
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A filter to exclude some scopes.
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedScopes", defaultValue = "system" )
    private String excludedScopes;

    /**
     * A filter to include only some scopes, if let empty then all scopes will be used (no filter).
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedScopes" )
    private String includedScopes;

    /**
     * A filter to exclude some GroupIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedGroups" )
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedGroups" )
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedArtifacts" )
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedArtifacts" )
    private String includedArtifacts;

    /**
     * Include transitive dependencies when looking for missing licenses and downloading license files.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includeTransitiveDependencies", defaultValue = "true" )
    private boolean includeTransitiveDependencies;

    /**
     * A filter to exclude transitive dependencies from excluded artifacts.
     *
     * @since 1.13
     */
    @Parameter( property = "license.excludeTransitiveDependencies", defaultValue = "false" )
    private boolean excludeTransitiveDependencies;

    /**
     * A flag to use the missing licenses file to consolidate the THID-PARTY file.
     *
     * @since 1.1
     */
    @Parameter( property = "license.useMissingFile", defaultValue = "false" )
    private boolean useMissingFile;

    /**
     * The file where to fill the license for dependencies with unknwon license.
     *
     * @since 1.1
     */
    @Parameter( property = "license.missingFile", defaultValue = "src/license/THIRD-PARTY.properties" )
    private File missingFile;

    /**
     * The file where to fill the override license for dependencies.
     *
     * @since 1.11
     */
    @Parameter( property = "license.overrideFile", defaultValue = "src/license/override-THIRD-PARTY.properties" )
    private File overrideFile;

    /**
     * Load from repositories third party missing files.
     *
     * @since 1.0
     */
    @Parameter( property = "license.useRepositoryMissingFiles", defaultValue = "true" )
    private boolean useRepositoryMissingFiles;

    /**
     * To merge licenses in final file.
     * <p>
     * Each entry represents a merge (first license is main license to keep), licenses are separated by {@code |}.
     * <p>
     * Example :
     * <p>
     * <pre>
     * &lt;licenseMerges&gt;
     * &lt;licenseMerge&gt;The Apache Software License|Version 2.0,Apache License, Version 2.0&lt;/licenseMerge&gt;
     * &lt;/licenseMerges&gt;
     * &lt;/pre&gt;
     *
     * @since 1.0
     */
    @Parameter
    private List<String> licenseMerges;

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
     * <p>
     * <b>Note:</b> Verbose mode is always on if you starts a debug maven instance
     * (says via {@code -X}).
     *
     * @since 1.0
     */
    @Parameter( property = "license.verbose", defaultValue = "${maven.verbose}" )
    private boolean verbose;

    /**
     * Encoding used to read and writes files.
     * <p>
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
     * The Maven Project.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

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

    // ----------------------------------------------------------------------
    // Protected Abstract Methods
    // ----------------------------------------------------------------------

    protected abstract Collection<ThirdPartyDetails> createThirdPartyDetails()
            throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException,
                   InvalidDependencyVersionException, ArtifactNotFoundException, ArtifactResolutionException,
                   DependenciesToolException;

    // ----------------------------------------------------------------------
    // AbstractMavenReport Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected void executeReport( Locale locale )
            throws MavenReportException
    {

        Collection<ThirdPartyDetails> details;

        try
        {
            details = createThirdPartyDetails();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( ThirdPartyToolException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( MojoFailureException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }
        catch ( DependenciesToolException e )
        {
            throw new MavenReportException( e.getMessage(), e );
        }

        ThirdPartyReportRenderer renderer =
                new ThirdPartyReportRenderer( getSink(), i18n, getOutputName(), locale, details );
        renderer.render();

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
        return i18n.getString( getOutputName(), locale, "report.description" );
    }

    /**
     * {@inheritDoc}
     */
    public String getName( Locale locale )
    {
        return i18n.getString( getOutputName(), locale, "report.title" );
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<String> getExcludedScopes()
    {
        return MojoHelper.getParams( excludedScopes );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getIncludedScopes()
    {
        return MojoHelper.getParams( includedScopes );
    }

    /**
     * {@inheritDoc}
     */
    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    /**
     * {@inheritDoc}
     */
    public String getIncludedGroups()
    {
        return includedGroups;
    }

    /**
     * {@inheritDoc}
     */
    public String getExcludedArtifacts()
    {
        return excludedArtifacts;
    }

    /**
     * {@inheritDoc}
     */
    public String getIncludedArtifacts()
    {
        return includedArtifacts;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isIncludeTransitiveDependencies()
    {
        return includeTransitiveDependencies;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExcludeTransitiveDependencies()
    {
        return excludeTransitiveDependencies;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    Collection<ThirdPartyDetails> createThirdPartyDetails( MavenProject project, boolean loadArtifacts )
            throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException,
                   DependenciesToolException
    {

        if ( loadArtifacts )
        {
            dependenciesTool.loadProjectArtifacts( localRepository, project.getRemoteArtifactRepositories(), project , null);
        }

        ThirdPartyHelper thirdPartyHelper =
                new DefaultThirdPartyHelper( project, encoding, verbose, dependenciesTool, thirdPartyTool, localRepository,
                                             project.getRemoteArtifactRepositories(), getLog() );
        // load dependencies of the project
        SortedMap<String, MavenProject> projectDependencies = thirdPartyHelper.loadDependencies( this );

        // create licenseMap from it
        LicenseMap licenseMap = thirdPartyHelper.createLicenseMap( projectDependencies, null );

        // Get unsafe dependencies (dependencies with no license in pom)
        SortedSet<MavenProject> dependenciesWithNoLicense = thirdPartyHelper.getProjectsWithNoLicense( licenseMap );

        // compute safe dependencies (with pom licenses)
        Set<MavenProject> dependenciesWithPomLicense =
                new TreeSet<MavenProject>( MojoHelper.newMavenProjectComparator() );
        dependenciesWithPomLicense.addAll( projectDependencies.values() );

        if ( CollectionUtils.isNotEmpty( dependenciesWithNoLicense ) )
        {
            // there is some unsafe dependencies, remove them from safe dependencies
            dependenciesWithPomLicense.removeAll( dependenciesWithNoLicense );

            if ( useMissingFile )
            {
                // Resolve unsafe dependencies using missing files, this will update licenseMap and unsafeDependencies
                thirdPartyHelper.createUnsafeMapping( licenseMap, missingFile, useRepositoryMissingFiles,
                                                      dependenciesWithNoLicense, projectDependencies );
            }
        }

        // LicenseMap is now complete, let's merge licenses if necessary
        thirdPartyHelper.mergeLicenses( licenseMerges, licenseMap);

        // Add override licenses
        thirdPartyTool.overrideLicenses( licenseMap, projectDependencies, encoding, overrideFile );

        // let's build third party details for each dependencies
        Collection<ThirdPartyDetails> details = new ArrayList<ThirdPartyDetails>();

        for ( Map.Entry<MavenProject, String[]> entry : licenseMap.toDependencyMap().entrySet() )
        {
            MavenProject dependency = entry.getKey();
            String[] licenses = entry.getValue();
            ThirdPartyDetails detail = new DefaultThirdPartyDetails( dependency );
            details.add( detail );
            if ( dependenciesWithPomLicense.contains( dependency ) )
            {

                // this is a pom licenses
                detail.setPomLicenses( licenses );
            }
            else if ( !dependenciesWithNoLicense.contains( dependency ) )
            {

                // this is a third-party licenses
                detail.setThirdPartyLicenses( licenses );
            }
        }
        return details;
    }

}
