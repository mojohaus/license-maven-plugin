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
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.mojo.license.api.DefaultThirdPartyDetails;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.api.ThirdPartyDetails;
import org.codehaus.mojo.license.api.ThirdPartyToolException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.MojoHelper;

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
 * Generates a report of all third-parties detected in the module.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
@Mojo( name = "third-party-report", requiresProject = true, requiresDependencyResolution = ResolutionScope.RUNTIME )
public class ThirdPartyReportMojo
    extends AbstractLicenseReportMojo
    implements MavenProjectDependenciesConfigurator
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
    @Parameter( property = "license.includedScopes", defaultValue = "" )
    private String includedScopes;

    /**
     * A filter to exclude some GroupIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedGroups", defaultValue = "" )
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedGroups", defaultValue = "" )
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedArtifacts", defaultValue = "" )
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedArtifacts", defaultValue = "" )
    private String includedArtifacts;

    /**
     * Include transitive dependencies when looking for missing licenses and downloading license files.
     *
     * @since 1.1
     */
    @Parameter( defaultValue = "true" )
    private boolean includeTransitiveDependencies;

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
     * Load from repositories third party missing files.
     *
     * @since 1.0
     */
    @Parameter( property = "license.useRepositoryMissingFiles", defaultValue = "true" )
    private boolean useRepositoryMissingFiles;

    /**
     * To merge licenses in final file.
     * <p/>
     * Each entry represents a merge (first license is main license to keep), licenses are separated by {@code |}.
     * <p/>
     * Example :
     * <p/>
     * <pre>
     * &lt;licenseMerges&gt;
     * &lt;licenseMerge&gt;The Apache Software License|Version 2.0,Apache License, Version 2.0&lt;/licenseMerge&gt;
     * &lt;/licenseMerges&gt;
     * &lt;/pre&gt;
     *
     * @parameter
     * @since 1.0
     */
    @Parameter
    private List<String> licenseMerges;

    // ----------------------------------------------------------------------
    // MavenReport Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getOutputName()
    {
        return "third-party-report";
    }

    // ----------------------------------------------------------------------
    // AbstractLicenseReportMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGenerateReport( Locale locale, Sink sink )
        throws MavenReportException, MojoExecutionException, MojoFailureException
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

        ThirdPartyReportRenderer renderer =
            new ThirdPartyReportRenderer( sink, getI18n(), getOutputName(), locale, details );
        renderer.render();
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

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private Collection<ThirdPartyDetails> createThirdPartyDetails()
        throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException
    {

        // load dependencies of the project
        SortedMap<String, MavenProject> projectDependencies = getHelper().loadDependencies( this );

        // create licenseMap from it
        LicenseMap licenseMap = getHelper().createLicenseMap( projectDependencies );

        // Get unsafe dependencies (dependencies with no license in pom)
        SortedSet<MavenProject> dependenciesWithNoLicense = getHelper().getProjectsWithNoLicense( licenseMap );

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
                getHelper().createUnsafeMapping( licenseMap, missingFile, useRepositoryMissingFiles,
                                                 dependenciesWithNoLicense, projectDependencies.values() );
            }
        }

        // LicenseMap is now complete, let's merge licenses if necessary
        getHelper().mergeLicenses( licenseMerges, licenseMap );

        // let's build thirdparty details for each dependencies
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
