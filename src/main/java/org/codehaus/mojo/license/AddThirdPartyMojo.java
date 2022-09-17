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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.api.ArtifactFilters;
import org.codehaus.mojo.license.api.DependenciesToolException;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.api.ResolvedProjectDependencies;
import org.codehaus.mojo.license.api.ThirdPartyToolException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// CHECKSTYLE_OFF: LineLength
/**
 * Goal to generate the third-party license file.
 * <p>
 * This file contains a list of the dependencies and their licenses.  Each dependency and its
 * license is displayed on a single line in the format
 * <pre>
 *   (&lt;license-name&gt;) &lt;project-name&gt; &lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt; - &lt;project-url&gt;
 * </pre>
 * The directory containing the license database file is added to the classpath as an additional resource.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
// CHECKSTYLE_ON: LineLength
@Mojo( name = "add-third-party", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true )
public class AddThirdPartyMojo extends AbstractAddThirdPartyMojo implements MavenProjectDependenciesConfigurator
{
    private static final Logger LOG = LoggerFactory.getLogger( AddThirdPartyMojo.class );

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * To skip execution of this mojo.
     *
     * @since 1.5
     */
    @Parameter( property = "license.skipAddThirdParty", defaultValue = "false" )
    private boolean skipAddThirdParty;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Internal flag to know if missing file must be generated.
     */
    private boolean doGenerateMissing;

    /**
     * Whether this is an aggregate build, or a single-project goal. This setting determines which dependency artifacts
     * will be examined by the plugin. AddThirdParty needs to load dependencies only for the single project it is run
     * for, while AggregateAddThirdParty needs to load dependencies for the parent project, as well as all child
     * projects in the reactor.
     */
    private boolean isAggregatorBuild = false;

    /**
     * The reactor projects. When resolving dependencies, the aggregator goal needs to do custom handling
     * of sibling dependencies for projects in the reactor,
     * to avoid trying to load artifacts for projects that haven't been built/published yet.
     */
    private List<MavenProject> reactorProjectDependencies;

    /**
     * Copies of the project's dependency sets. AddThirdParty needs to load dependencies only for the single project it
     * is run for, while AggregateAddThirdParty needs to load dependencies for the parent project, as well as all child
     * projects in the reactor.
     *
     * In cases where one child project A in a reactor depends on another project B in the same reactor,
     * B is not necessarily built/published. The plugin needs to resolve B's dependencies manually.
     * This field stores the result of that manual resolution.
     */
    private ResolvedProjectDependencies dependencyArtifacts;

    private ArtifactFilters artifactFilters;

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkip()
    {
        return skipAddThirdParty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkPackaging()
    {
        if ( acceptPomPackaging )
        {

            // rejects nothing
            return true;
        }

        // can reject pom packaging
        return rejectPackaging( "pom" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkSkip()
    {
        if ( !doGenerate && !doGenerateBundle && !doGenerateMissing )
        {

            LOG.info( "All files are up to date, skip goal execution." );
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doAction()
            throws Exception
    {

        consolidate();

        checkUnsafeDependencies();

        boolean safeLicense = checkForbiddenLicenses();

        checkBlacklist( safeLicense );

        writeThirdPartyFile();

        if ( doGenerateMissing )
        {

            writeMissingFile();
        }

        boolean unsafe = CollectionUtils.isNotEmpty( unsafeDependencies );

        checkMissing( unsafe );

        if ( !unsafe && useMissingFile && MapUtils.isEmpty( unsafeMappings ) && missingFile.exists() )
        {

            // there is no missing dependencies, but still a missing file, delete it
            LOG.info( "There is no dependency to put in missing file, delete it at {}", missingFile );
            FileUtil.deleteFile( missingFile );
        }

        if ( !unsafe && deployMissingFile && MapUtils.isNotEmpty( unsafeMappings ) )
        {

            // can deploy missing file
            LOG.info( "Will attach third party file from {}", missingFile );
            getHelper().attachThirdPartyDescriptor( missingFile );
        }

        addResourceDir( outputDirectory, "**/*.txt" );
    }

    // ----------------------------------------------------------------------
    // AbstractAddThirdPartyMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedMap<String, MavenProject> loadDependencies() throws DependenciesToolException
    {
        return getHelper().loadDependencies( this, resolveDependencyArtifacts() );
    }

    /**
     * Resolves the transitive and direct dependency sets for this project.
     *
     * @return The set of all dependencies, and the set of only direct dependency artifacts.
     * @throws org.codehaus.mojo.license.api.DependenciesToolException if the dependencies could not be resolved
     */
    protected ResolvedProjectDependencies resolveDependencyArtifacts() throws DependenciesToolException
    {
        if ( dependencyArtifacts != null )
        {
            return dependencyArtifacts;
        }
        if ( isAggregatorBuild )
        {
            dependencyArtifacts =
                    new ResolvedProjectDependencies( project.getArtifacts(), project.getDependencyArtifacts() );
        }
        else
        {
            dependencyArtifacts = new ResolvedProjectDependencies( project.getArtifacts(),
                    project.getDependencyArtifacts() );
        }
        return dependencyArtifacts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedProperties createUnsafeMapping()
      throws ProjectBuildingException, IOException, ThirdPartyToolException,
            MojoExecutionException, DependenciesToolException
    {

        SortedProperties unsafeMappings =
                getHelper().createUnsafeMapping( licenseMap, missingFile, missingFileUrl,
                                                 useRepositoryMissingFiles, unsafeDependencies,
                                                 projectDependencies,
                                                 resolveDependencyArtifacts().getAllDependencies() );
        if ( isVerbose() )
        {
            LOG.info( "found {} unsafe mappings", unsafeMappings.size() );
        }

        // compute if missing file should be (re)-generate
        doGenerateMissing = computeDoGenerateMissingFile( unsafeMappings, unsafeDependencies );

        if ( doGenerateMissing && isVerbose() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Will use " );
            sb.append( unsafeMappings.size() );
            sb.append( " dependencies from missingFile:" );
            for ( Map.Entry<Object, Object> entry : unsafeMappings.entrySet() )
            {
                String id = (String) entry.getKey();
                String license = (String) entry.getValue();
                sb.append( "\n - " ).append( id ).append( " - " ).append( license );
            }
            LOG.info( "{}", sb );
        }
        else
        {
            if ( useMissingFile && !unsafeMappings.isEmpty() )
            {
                LOG.info( "Missing file {} is up-to-date.", missingFile );
            }
        }
        return unsafeMappings;
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementaton
    // ----------------------------------------------------------------------

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

    /** {@inheritDoc} */
    public ArtifactFilters getArtifactFilters()
    {
        if ( artifactFilters == null )
        {
            artifactFilters = ArtifactFilters.of( includedGroups, excludedGroups, includedArtifacts, excludedArtifacts,
                                                  includedScopes, excludedScopes, includedTypes, excludedTypes,
                                                  includeOptional, artifactFiltersUrl , getEncoding() );
        }
        return artifactFilters;
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    /**
     * @param unsafeMappings     the unsafe mapping coming from the missing file
     * @param unsafeDependencies the unsafe dependencies from the project
     * @return {@code true} if missing ifle should be (re-)generated, {@code false} otherwise
     * @throws IOException if any IO problem
     * @since 1.0
     */
    private boolean computeDoGenerateMissingFile( SortedProperties unsafeMappings,
                                                  SortedSet<MavenProject> unsafeDependencies ) throws IOException
    {

        if ( !useMissingFile )
        {

            // never use the missing file
            return false;
        }

        if ( force )
        {

            // the mapping for missing file is not empty, regenerate it
            return !CollectionUtils.isEmpty( unsafeMappings.keySet() );
        }

        if ( !CollectionUtils.isEmpty( unsafeDependencies ) )
        {

            // there is some unsafe dependencies from the project, must
            // regenerate missing file
            return true;
        }

        if ( !missingFile.exists() )
        {

            // the missing file does not exists, this happens when
            // using remote missing file from dependencies
            return true;
        }

        // check if the missing file has changed
        SortedProperties oldUnsafeMappings = new SortedProperties( getEncoding() );
        oldUnsafeMappings.load( missingFile );
        return !unsafeMappings.equals( oldUnsafeMappings );
    }

    /**
     * Write the missing file ({@link #getMissingFile()}.
     *
     * @throws IOException if error while writing missing file
     */
    private void writeMissingFile()
            throws IOException
    {

        FileUtil.createDirectoryIfNecessary( missingFile.getParentFile() );
        LOG.info( "Regenerate missing license file {}", missingFile );

        FileOutputStream writer = new FileOutputStream( missingFile );
        try
        {
            StringBuilder sb = new StringBuilder( " Generated by " + getClass().getName() );
            List<String> licenses = new ArrayList<>( licenseMap.keySet() );
            licenses.remove( LicenseMap.UNKNOWN_LICENSE_MESSAGE );
            if ( !licenses.isEmpty() )
            {
                sb.append( "\n-------------------------------------------------------------------------------" );
                sb.append( "\n Already used licenses in project :" );
                for ( String license : licenses )
                {
                    sb.append( "\n - " ).append( license );
                }
            }
            sb.append( "\n-------------------------------------------------------------------------------" );
            sb.append( "\n Please fill the missing licenses for dependencies :\n\n" );
            unsafeMappings.store( writer, sb.toString() );
        }
        finally
        {
            writer.close();
        }
    }

    void initFromMojo( AggregatorAddThirdPartyMojo mojo, MavenProject mavenProject,
            List<MavenProject> reactorProjects ) throws Exception
    {
        project = mavenProject;
        deployMissingFile = mojo.deployMissingFile;
        useRepositoryMissingFiles = mojo.useRepositoryMissingFiles;
        acceptPomPackaging = mojo.acceptPomPackaging;
        includeOptional = mojo.includeOptional;
        excludedScopes = mojo.excludedScopes;
        includedScopes = mojo.includedScopes;
        excludedGroups = mojo.excludedGroups;
        includedGroups = mojo.includedGroups;
        excludedArtifacts = mojo.excludedArtifacts;
        includedArtifacts = mojo.includedArtifacts;
        includeTransitiveDependencies = mojo.includeTransitiveDependencies;
        excludeTransitiveDependencies = mojo.excludeTransitiveDependencies;
        thirdPartyFilename = mojo.thirdPartyFilename;
        useMissingFile = mojo.useMissingFile;
        String absolutePath = mojo.getProject().getBasedir().getAbsolutePath();

        missingFile = new File( project.getBasedir(),
                mojo.missingFile.getAbsolutePath().substring( absolutePath.length() ) );
        resolvedOverrideUrl  = mojo.resolvedOverrideUrl;
        missingLicensesFileArtifact = mojo.missingLicensesFileArtifact;
        localRepository = mojo.localRepository;
        dependencies = new HashSet<>( mavenProject.getDependencyArtifacts() );
        licenseMerges = mojo.licenseMerges;
        licenseMergesFile = mojo.licenseMergesFile;
        includedLicenses = mojo.includedLicenses;
        excludedLicenses = mojo.excludedLicenses;
        bundleThirdPartyPath = mojo.bundleThirdPartyPath;
        generateBundle = mojo.generateBundle;
        force = mojo.force;
        failIfWarning = mojo.failIfWarning;
        failOnMissing = mojo.failOnMissing;
        failOnBlacklist = mojo.failOnBlacklist;
        sortArtifactByName = mojo.sortArtifactByName;
        fileTemplate = mojo.fileTemplate;
        session = mojo.session;
        verbose = mojo.verbose;
        encoding = mojo.encoding;

        setLog( mojo.getLog() );

        isAggregatorBuild = true;
        reactorProjectDependencies = reactorProjects;

        init();

        consolidate();
    }
}
