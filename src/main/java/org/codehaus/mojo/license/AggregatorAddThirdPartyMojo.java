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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This goal forks executions of the add-third-party goal for all the leaf projects
 * of the tree of modules below the point where it is executed. Note that this
 * plugin sets a specific name, 'add-third-party', for the forked executions in the
 * individual projects. From command level, then
 * even though the execution of this goal is named 'default-cli', the forked executions
 * have the name 'add-third-party'. Thus, to use the <tt>pluginManagement</tt> element of
 * the POM to set options, you have to name the execution 'add-third-party',
 * not 'default-cli'.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
@Mojo( name = "aggregate-add-third-party", aggregator = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class AggregatorAddThirdPartyMojo extends AbstractAddThirdPartyMojo
{
    private static final Logger LOG = LoggerFactory.getLogger( AggregatorAddThirdPartyMojo.class );

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * The projects in the reactor.
     *
     * @since 1.0
     */
    @Parameter( property = "reactorProjects", readonly = true, required = true )
    private List<MavenProject> reactorProjects;

    /**
     * To skip execution of this mojo.
     *
     * @since 1.5
     */
    @Parameter( property = "license.skipAggregateAddThirdParty", defaultValue = "false" )
    private boolean skipAggregateAddThirdParty;

    /**
     * To resolve third party licenses from an artifact.
     *
     * @since 1.11
     * @deprecated since 1.14, please use now {@link #missingLicensesFileArtifact}
     */
    @Deprecated
    @Parameter( property = "license.aggregateMissingLicensesFileArtifact" )
    private String aggregateMissingLicensesFileArtifact;

    /**
     * To resolve third party licenses from a file.
     *
     * @since 1.11
     * @deprecated since 1.14, please use now {@link #missingFile}.
     */
    @Deprecated
    @Parameter( property = "license.aggregateMissingLicensesFile" )
    private File aggregateMissingLicensesFile;

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip()
    {
        return skipAggregateAddThirdParty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkPackaging()
    {
        return acceptPackaging( "pom" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkSkip()
    {
        if ( !doGenerate && !doGenerateBundle )
        {

            LOG.info( "All files are up to date, skip goal execution." );
            return false;
        }
        return super.checkSkip();
    }

    @Override
    protected void init() throws Exception
    {
        // CHECKSTYLE_OFF: LineLength
        if ( aggregateMissingLicensesFile != null && !aggregateMissingLicensesFile.equals( missingFile ) )
        {
            LOG.warn( "" );
            LOG.warn( "You should use *missingFile* parameter instead of deprecated *aggregateMissingLicensesFile*." );
            LOG.warn( "" );
            missingFile = aggregateMissingLicensesFile;
        }

        if ( aggregateMissingLicensesFileArtifact != null
                && !aggregateMissingLicensesFileArtifact.equals( missingLicensesFileArtifact ) )
        {
            LOG.warn( "" );
            LOG.warn( "You should use *missingLicensesFileArtifact* parameter instead of deprecated *aggregateMissingLicensesFileArtifact*." );
            LOG.warn( "" );
            missingLicensesFileArtifact = aggregateMissingLicensesFileArtifact;
        }
        // CHECKSTYLE_ON: LineLength
        super.init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doAction()
            throws Exception
    {
        if ( isVerbose() )
        {
            LOG.info( "After executing on {} project(s)", reactorProjects.size() );
        }

        licenseMap = new LicenseMap();

        Artifact pluginArtifact = (Artifact) project.getPluginArtifactMap()
                .get( "org.codehaus.mojo:license-maven-plugin" );

        String groupId = null;
        String artifactId = null;
        String version = null;
        if ( pluginArtifact == null )
        {
            Plugin plugin = (Plugin) project.getPluginManagement().getPluginsAsMap()
                    .get( "org.codehaus.mojo:license-maven-plugin" );
            if ( plugin != null )
            {
                groupId = plugin.getGroupId();
                artifactId = plugin.getArtifactId();
                version = plugin.getVersion();
            }
        }
        else
        {
            groupId = pluginArtifact.getGroupId();
            artifactId = pluginArtifact.getArtifactId();
            version = pluginArtifact.getVersion();
        }
        if ( groupId == null )
        {
            throw new IllegalStateException( "Can't find license-maven-plugin" );
        }

        String addThirdPartyRoleHint = groupId + ":" + artifactId + ":" + version + ":" + "add-third-party";

        for ( MavenProject reactorProject : reactorProjects )
        {
            if ( getProject().equals( reactorProject ) && !acceptPomPackaging )
            {
                // does not process this pom unless specified
                continue;
            }

            AddThirdPartyMojo mojo = (AddThirdPartyMojo) getSession()
                    .lookup( AddThirdPartyMojo.ROLE, addThirdPartyRoleHint );

            mojo.initFromMojo( this, reactorProject, new ArrayList<>( this.reactorProjects ) );

            LicenseMap childLicenseMap = mojo.licenseMap;
            if ( isVerbose() )
            {
                LOG.info( "Found {} license(s) in module {}:{}",
                        childLicenseMap.size(), mojo.project.getGroupId(), mojo.project.getArtifactId() );
            }
            licenseMap.putAll( childLicenseMap );

        }

        LOG.info( "Detected {} license(s).", licenseMap.size() );
        if ( isVerbose() )
        {
            for ( Map.Entry<String, SortedSet<MavenProject>> entry: licenseMap.entrySet() )
            {
                LOG.info( " - {} for {} artifact(s).", entry.getKey(), entry.getValue().size() );
            }
        }

        consolidate();

        checkUnsafeDependencies();

        boolean safeLicense = checkForbiddenLicenses();

        checkBlacklist( safeLicense );

        writeThirdPartyFile();

        checkMissing( CollectionUtils.isNotEmpty( unsafeDependencies ) );
    }

    // ----------------------------------------------------------------------
    // AbstractAddThirdPartyMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedMap<String, MavenProject> loadDependencies()
    {
        // use the cache filled by modules in reactor
        return getHelper().getArtifactCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedProperties createUnsafeMapping()
      throws ProjectBuildingException, IOException, MojoExecutionException
    {

        String path =
            missingFile.getAbsolutePath().substring( getProject().getBasedir().getAbsolutePath().length() + 1 );

        if ( isVerbose() )
        {
            LOG.info( "Use missing file path: {}", path );
        }

        SortedProperties unsafeMappings = new SortedProperties( getEncoding() );

        for ( Object o : reactorProjects )
        {
            MavenProject p = (MavenProject) o;

            File file = new File( p.getBasedir(), path );

            if ( file.exists() )
            {

                SortedProperties tmp = getHelper().loadUnsafeMapping( licenseMap, file, null, projectDependencies );
                unsafeMappings.putAll( tmp );
            }

            SortedSet<MavenProject> unsafe = getHelper().getProjectsWithNoLicense( licenseMap );
            if ( CollectionUtils.isEmpty( unsafe ) )
            {

                // no more unsafe dependencies, can break
                break;
            }
        }
        return unsafeMappings;
    }

}
