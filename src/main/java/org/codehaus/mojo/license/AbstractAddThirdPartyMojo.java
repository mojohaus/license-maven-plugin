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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.api.DefaultThirdPartyHelper;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.ThirdPartyHelper;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.mojo.license.api.ThirdPartyToolException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.HttpRequester;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.mojo.license.utils.StringToList;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Abstract mojo for all third-party mojos.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public abstract class AbstractAddThirdPartyMojo
        extends AbstractLicenseMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Directory where to generate files.
     *
     * @since 1.0
     */
    @Parameter( property = "license.outputDirectory",
            defaultValue = "${project.build.directory}/generated-sources/license", required = true )
    private File outputDirectory;

    /**
     * Attach the 'missing' file as an additional artifact so that it is deployed in the deploy phase.
     *
     * @since 1.0
     */
    @Parameter( property = "license.deployMissingFile", defaultValue = "true" )
    boolean deployMissingFile;

    /**
     * Load files supplying information for missing third party licenses from repositories.
     * The plugin looks for Maven artifacts with coordinates of the form G:A:V:properties:third-party, where
     * the group, artifact, and version are those for dependencies of your project,
     * while the type is 'properties' and the classifier is 'third-party'.
     *
     * @since 1.0
     */
    @Parameter( property = "license.useRepositoryMissingFiles", defaultValue = "true" )
    boolean useRepositoryMissingFiles;

    /**
     * To execute or not this mojo if project packaging is pom.
     * <p>
     * <strong>Note:</strong> The default value is {@code false}.
     *
     * @since 1.1
     */
    @Parameter( property = "license.acceptPomPackaging", defaultValue = "false" )
    boolean acceptPomPackaging;

    /**
     * A filter to exclude some scopes.
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedScopes", defaultValue = "system" )
    String excludedScopes;

    /**
     * A filter to include only some scopes, if let empty then all scopes will be used (no filter).
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedScopes" )
    String includedScopes;

    /**
     * A filter to exclude some types.
     *
     * @since 1.15
     */
    @Parameter( property = "license.excludedTypes" )
    String excludedTypes;

    /**
     * A filter to include only some types, if let empty then all types will be used (no filter).
     *
     * @since 1.15
     */
    @Parameter( property = "license.includedTypes" )
    String includedTypes;

    /**
     * A filter to exclude some GroupIds
     * This is a regular expression that is applied to groupIds (not an ant pattern).
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedGroups" )
    String excludedGroups;

    /**
     * A filter to include only some GroupIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedGroups" )
    String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedArtifacts" )
    String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedArtifacts" )
    String includedArtifacts;

    /**
     * Include transitive dependencies when checking for missing licenses and downloading license files.
     * If this is <tt>false</tt>, then only direct dependencies are examined.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includeTransitiveDependencies", defaultValue = "true" )
    boolean includeTransitiveDependencies;

    /**
     * Exclude transitive dependencies from excluded Artifacts
     *
     * @since 1.13
     */
    @Parameter( property = "license.excludeTransitiveDependencies", defaultValue = "false" )
    boolean excludeTransitiveDependencies;

    /**
     * File where to write the third-party file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.thirdPartyFilename", defaultValue = "THIRD-PARTY.txt", required = true )
    String thirdPartyFilename;

    /**
     * A flag to use the missing licenses file to consolidate the THID-PARTY file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.useMissingFile", defaultValue = "false" )
    boolean useMissingFile;

    /**
     * The file to write with a license information template for dependencies with unknown license.
     *
     * @since 1.0
     */
    @Parameter( property = "license.missingFile", defaultValue = "src/license/THIRD-PARTY.properties" )
    File missingFile;

    /**
     * The Url that holds the missing license dependency entries. This is an extension to {@link #missingFile}.
     * If set then the entries that will be found at this URL will be added additionally to the entries of the
     * missing file.<br>
     * <br>
     * <b>NOTE:</b><br>
     * the response of the URL endpoint must return content that matches the THIRD-PARTY.properties file!
     *
     * @since 1.15
     */
    @Parameter( property = "license.missingFileUrl" )
    String missingFileUrl;

    /**
     * To resolve third party licenses from an artifact.
     *
     * @since 1.14
     */
    @Parameter( property = "license.missingLicensesFileArtifact" )
    String missingLicensesFileArtifact;

    /**
     * The file to write with a license information template for dependencies to override.
     *
     * @since 1.12
     */
    @Parameter( property = "license.overrideFile", defaultValue = "src/license/override-THIRD-PARTY.properties" )
    File overrideFile;

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
     * <b>Note:</b> This option will be overridden by {@link #licenseMergesUrl} if it is used by command line.
     * @since 1.0
     */
    @Parameter
    List<String> licenseMerges;

   /**
     * The file with the merge licenses in order to be used by command line.
     * <b>Note:</b> This option overrides {@link #licenseMerges}.
     *
     * @since 1.15
    * @deprecated prefer use now {@link #licenseMergesUrl}
     */
   @Deprecated
    @Parameter( property = "license.licenseMergesFile" )
    String licenseMergesFile;

   /**
     * Location of file with the merge licenses in order to be used by command line.
     * <b>Note:</b> This option overrides {@link #licenseMerges}.
     *
     * @since 1.17
     */
    @Parameter( property = "license.licenseMergesUrl" )
    String licenseMergesUrl;

  /**
   * To specify some licenses to include.
   * <p>
   * If this parameter is filled and a license is not in this {@code whitelist} then build will fail when
   * property {@link #failOnBlacklist} is <tt>true</tt>.
   * <p>
   * Since version {@code 1.4}, there are three ways to fill this parameter :
   * <ul>
   * <li>A simple string (separated by {@code |}), the way to use by property configuration:
   *
   * <pre>
   * &lt;includedLicenses&gt;licenseA|licenseB&lt;/includedLicenses&gt;
   * </pre>
   *
   * or
   *
   * <pre>
   * -Dlicense.includedLicenses=licenseA|licenseB
   * </pre>
   *
   * </li>
   * <li>A list of string (can only be used in plugin configuration, not via property configuration)
   *
   * <pre>
   * &lt;includedLicenses&gt;
   *   &lt;includedLicense&gt;licenseA&lt;/includedLicense&gt;
   *   &lt;includedLicenses&gt;licenseB&lt;/includedLicense&gt;
   * &lt;/includedLicenses&gt;
   * </pre>
   *
   * </li>
   * <li>Since version {@code 1.15}<br>
   * a URL that contains a set of license names at the target source (only a single URL is accepted as
   * parameter)
   *
   * <pre>
   *    &lt;includedLicenses&gt;http://my.license.host.com/my-whitelist&lt;/includedLicenses&gt;
   * </pre>
   *
   * the license-list on the given URL is expected to be list with a line-break after every entry e.g.:
   * <ul style="list-style-type:none;">
   * <li>The Apache Software License, Version 2.0</li>
   * <li>Apache License, Version 2.0</li>
   * <li>Bouncy Castle Licence</li>
   * <li>MIT License</li>
   * </ul>
   * empty lines will be ignored.</li>
   * </ul>
   *
   * @since 1.1
   */
  @Parameter( property = "license.includedLicenses" )
  IncludedLicenses includedLicenses;

  /**
   * To specify some licenses to exclude.
   * <p>
   * If a such license is found then build will fail when property {@link #failOnBlacklist} is <tt>true</tt>.
   * <p>
   * Since version {@code 1.4}, there are three ways to fill this parameter :
   * <ul>
   * <li>A simple string (separated by {@code |}), the way to use by property configuration:
   *
   * <pre>
   * &lt;excludedLicenses&gt;licenseA|licenseB&lt;/excludedLicenses&gt;
   * </pre>
   *
   * or
   *
   * <pre>
   * -Dlicense.excludedLicenses=licenseA|licenseB
   * </pre>
   *
   * </li>
   * <li>A list of string (can only be used in plugin configuration, not via property configuration)
   *
   * <pre>
   * &lt;excludedLicenses&gt;
   *   &lt;excludedLicense&gt;licenseA&lt;/excludedLicense&gt;
   *   &lt;excludedLicense&gt;licenseB&lt;/excludedLicense&gt;
   * &lt;/excludedLicenses&gt;
   * </pre>
   *
   * </li>
   * <li>Since version {@code 1.15}<br>
   * a URL that contains a set of license names at the target source (only a single URL is accepted as
   * parameter)
   *
   * <pre>
   *    &lt;includedLicenses&gt;http://my.license.host.com/my-blacklist&lt;/includedLicenses&gt;
   * </pre>
   *
   * the license-list on the given URL is expected to be list with a line-break after every entry e.g.:
   * <ul style="list-style-type:none;">
   * <li>The Apache Software License, Version 2.0</li>
   * <li>Apache License, Version 2.0</li>
   * <li>Bouncy Castle Licence</li>
   * <li>MIT License</li>
   * </ul>
   * empty lines will be ignored.</li>
   * </ul>
   *
   * @since 1.1
   */
  @Parameter( property = "license.excludedLicenses" )
  ExcludedLicenses excludedLicenses;

    /**
     * The path of the bundled third party file to produce when
     * {@link #generateBundle} is on.
     * <p>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @since 1.0
     */
    @Parameter( property = "license.bundleThirdPartyPath",
            defaultValue = "META-INF/${project.artifactId}-THIRD-PARTY.txt" )
    String bundleThirdPartyPath;

    /**
     * A flag to copy a bundled version of the third-party file. This is useful
     * to avoid for a final application collision name of third party file.
     * <p>
     * The file will be copied at the {@link #bundleThirdPartyPath} location.
     *
     * @since 1.0
     */
    @Parameter( property = "license.generateBundle", defaultValue = "false" )
    boolean generateBundle;

    /**
     * To force generation of the third-party file even if everything is up to date.
     *
     * @since 1.0
     */
    @Parameter( property = "license.force", defaultValue = "false" )
    boolean force;

    /**
     * A flag to fail the build if at least one dependency was detected without a license.
     *
     * @since 1.0
     * @deprecated since 1.14, use now {@link #failOnMissing} or {@link #failOnBlacklist}.
     */
    @Deprecated
    @Parameter( property = "license.failIfWarning", defaultValue = "false" )
    boolean failIfWarning;

    /**
     * A flag to fail the build if at least one dependency was detected without a license.
     *
     * @since 1.14
     */
    @Parameter( property = "license.failOnMissing", defaultValue = "false" )
    boolean failOnMissing;

    /**
     * A flag to fail the build if at least one dependency was blacklisted.
     *
     * @since 1.14
     */
    @Parameter( property = "license.failOnBlacklist", defaultValue = "false" )
    boolean failOnBlacklist;

    /**
     * A flag to sort artifact by name in the generated third-party file.
     * <p>
     * If not then artifacts are sorted by <pre>groupId:artifactId:version</pre>
     *
     * @since 1.6
     */
    @Parameter( property = "license.sortArtifactByName", defaultValue = "false" )
    boolean sortArtifactByName;

    /**
     * Template used to build the third-party file.
     * <p>
     * (This template uses freemarker).
     * <p>
     * <b>Note:</b> This property can either point to a file or a resource on
     * the classpath. In case it points to a file and this plugin is used within
     * a sub-module as part of a multi-module build, you need to make this path
     * resolvable, e.g. by prepending {@code basedir}.
     *
     * @since 1.1
     */
    @Parameter( property = "license.fileTemplate", defaultValue = "/org/codehaus/mojo/license/third-party-file.ftl" )
    String fileTemplate;

    /**
     * Local Repository.
     *
     * @since 1.0.0
     */
    @Parameter( property = "localRepository", required = true, readonly = true )
    ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @since 1.0.0
     */
    @Parameter( property = "project.remoteArtifactRepositories", required = true, readonly = true )
    List<ArtifactRepository> remoteRepositories;

    /**
     * The set of dependencies for the current project, used to locate license databases.
     */
    @Parameter( property = "project.artifacts", required = true, readonly = true )
    Set<Artifact> dependencies;

    // ----------------------------------------------------------------------
    // Plexus components
    // ----------------------------------------------------------------------

    /**
     * Third party tool (much of the logic of these mojos is implemented here).
     *
     * @since 1.0
     */
    @Component
    private ThirdPartyTool thirdPartyTool;

    /**
     * Dependencies tool. (pluggable component to find dependencies that match up with
     * criteria).
     *
     * @since 1.1
     */
    @Component
    DependenciesTool dependenciesTool;

    // ----------------------------------------------------------------------
    // Private fields
    // ----------------------------------------------------------------------

    /**
     * Third-party helper (high level tool with common code for mojo and report).
     */
    private ThirdPartyHelper helper;

    private SortedMap<String, MavenProject> projectDependencies;

    LicenseMap licenseMap;

    SortedSet<MavenProject> unsafeDependencies;

    private File thirdPartyFile;

    SortedProperties unsafeMappings;

    /**
     * Flag computed in the {@link #init()} method to know if there is something has to be generated.
     */
    private boolean doGenerate;

    /**
     * Flag computed in the {@link #init()} method to know if a bundle version has to be generated.
     */
    private boolean doGenerateBundle;

    /**
     * Map from G/A/V as string to license key, obtained from global dependencies of type=.ld.properties.
     * This could probably be refactored to have more in common with the classifier-based loader.
     *
     * @since 1.4
     */
    private Map<String, String> globalKnownLicenses;

    // ----------------------------------------------------------------------
    // Abstract Methods
    // ----------------------------------------------------------------------

    /**
     * Loads the dependencies of the project (as {@link MavenProject}, indexed by their gav.
     *
     * @return the map of dependencies of the maven project indexed by their gav.
     */
    protected abstract SortedMap<String, MavenProject> loadDependencies();

    /**
     * Creates the unsafe mapping (says dependencies with no license given by their pom).
     * <p>
     * Can come from loaded missing file or from dependencies with no license at all.
     *
     * @return the map of usafe mapping indexed by their gav.
     * @throws ProjectBuildingException if could not create maven porject for some dependencies
     * @throws IOException              if could not load missing file
     * @throws ThirdPartyToolException  for third party tool error
     */
    protected abstract SortedProperties createUnsafeMapping()
      throws ProjectBuildingException, IOException, ThirdPartyToolException, MojoExecutionException;

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
            throws Exception
    {

        Log log = getLog();

        if ( log.isDebugEnabled() )
        {

            // always be verbose in debug mode
            setVerbose( true );
        }

        thirdPartyFile = new File( getOutputDirectory(), thirdPartyFilename );

        long buildTimestamp = getBuildTimestamp();

        if ( isVerbose() || getLog().isDebugEnabled() )
        {
            log.debug( "Build start   at : " + buildTimestamp );
            log.debug( "third-party file : " + thirdPartyFile.lastModified() );
        }

        doGenerate = isForce() || !thirdPartyFile.exists() || buildTimestamp > thirdPartyFile.lastModified();

        if ( generateBundle )
        {

            File bundleFile = FileUtil.getFile( getOutputDirectory(), bundleThirdPartyPath );

            if ( isVerbose() || getLog().isDebugEnabled() )
            {
                log.debug( "bundle third-party file : " + bundleFile.lastModified() );
            }
            doGenerateBundle = isForce() || !bundleFile.exists() || buildTimestamp > bundleFile.lastModified();
        }
        else
        {

            // not generating bundled file
            doGenerateBundle = false;
        }

        projectDependencies = loadDependencies();

        licenseMap = getHelper().createLicenseMap( projectDependencies );

        if ( licenseMergesFile != null )
        {
            if ( licenseMergesUrl != null )
            {
                throw new MojoExecutionException( "You can't use both licenseMergesFile and licenseMergesUrl" );
            }
            getLog().warn( "" );
            getLog().warn( "!!! licenseMergesFile is deprecated, use now licenseMergesUrl !!!" );
            getLog().warn( "" );
            getLog().warn( "licenseMerges will be overridden by licenseMergesFile." );
            getLog().warn( "" );
            licenseMerges = FileUtils.readLines( new File( licenseMergesFile ), "utf-8" );
        }
        else if ( licenseMergesUrl != null )
        {
            getLog().warn( "" );
            getLog().warn( "licenseMerges will be overridden by licenseMergesUrl." );
            getLog().warn( "" );
            if ( HttpRequester.isStringUrl( licenseMergesUrl ) )
            {
                InputStream input = new URL( licenseMergesUrl ).openStream();
                String httpRequestResult = IOUtil.toString( input );
                licenseMerges = Arrays.asList( httpRequestResult.split( "\n" ) );
                input.close();
            }
        }
    }

    void consolidate() throws IOException, ArtifactNotFoundException, ArtifactResolutionException, MojoFailureException,
                              ProjectBuildingException, ThirdPartyToolException, MojoExecutionException
    {

        unsafeDependencies = getHelper().getProjectsWithNoLicense( licenseMap );

        if ( !CollectionUtils.isEmpty( unsafeDependencies ) )
        {
            if ( isUseMissingFile() && isDoGenerate() )
            {
                // load unsafeMapping from local file and/or third-party classified items.
                unsafeMappings = createUnsafeMapping();
            }
        }

        getHelper().mergeLicenses( licenseMerges, licenseMap );

        if ( CollectionUtils.isNotEmpty( unsafeDependencies ) )
        {
            resolveUnsafeDependenciesFromFile( missingFile );
        }

        if ( !StringUtils.isBlank( missingLicensesFileArtifact ) && CollectionUtils.isNotEmpty( unsafeDependencies ) )
        {
            String[] tokens = StringUtils.split( missingLicensesFileArtifact, ":" );
            if ( tokens.length != 3 )
            {
                throw new MojoFailureException(
                        "Invalid missing licenses artifact, you must specify groupId:artifactId:version "
                                + missingLicensesFileArtifact );
            }
            String groupId = tokens[0];
            String artifactId = tokens[1];
            String version = tokens[2];

            resolveUnsafeDependenciesFromArtifact( groupId, artifactId, version );
        }

        overrideLicenses();
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean isFailIfWarning()
    {
        return failIfWarning;
    }

    SortedMap<String, MavenProject> getProjectDependencies()
    {
        return projectDependencies;
    }

    SortedSet<MavenProject> getUnsafeDependencies()
    {
        return unsafeDependencies;
    }

    public LicenseMap getLicenseMap()
    {
        return licenseMap;
    }

    public boolean isUseMissingFile()
    {
        return useMissingFile;
    }

    File getMissingFile()
    {
        return missingFile;
    }

    File getOverrideFile()
    {
        return overrideFile;
    }

    String getLicenseMergesFile()
    {
        return licenseMergesFile;
    }

    public String getLicenseMergesUrl()
    {
        return licenseMergesUrl;
    }

    SortedProperties getUnsafeMappings()
    {
        return unsafeMappings;
    }

    boolean isForce()
    {
        return force;
    }

    boolean isDoGenerate()
    {
        return doGenerate;
    }

    boolean isDoGenerateBundle()
    {
        return doGenerateBundle;
    }

    /**
     * @return list of license to exclude.
     */
    private List<String> getExcludedLicenses()
    {
        return excludedLicenses.getData();
    }

    /**
     * @return list of license to include.
     */
    private List<String> getIncludedLicenses()
    {
        return includedLicenses.getData();
    }

    /**
     * Fill the {@link #includedLicenses} parameter from a simple string to split.
     *
     * @param includedLicenses license to excludes separated by a {@code |}.
     */
    public void setIncludedLicenses( String includedLicenses ) throws MojoExecutionException
    {
        this.includedLicenses = new IncludedLicenses( includedLicenses );
    }

    /**
     * Fill the {@link #excludedLicenses} parameter from a simple string to split.
     *
     * @param excludedLicenses license to excludes separated by a {@code |}.
     */
    public void setExcludedLicenses( String excludedLicenses ) throws MojoExecutionException
    {
        this.excludedLicenses = new ExcludedLicenses( excludedLicenses );
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    protected ThirdPartyHelper getHelper()
    {
        if ( helper == null )
        {
            helper = new DefaultThirdPartyHelper( getProject(), getEncoding(), isVerbose(), dependenciesTool,
                    thirdPartyTool, localRepository, remoteRepositories, getLog() );
        }
        return helper;
    }

    void resolveUnsafeDependenciesFromArtifact( String groupId, String artifactId, String version )
      throws ArtifactNotFoundException, IOException, ArtifactResolutionException, MojoExecutionException
    {
        File missingLicensesFromArtifact = thirdPartyTool.resolveMissingLicensesDescriptor( groupId, artifactId,
                version, localRepository, remoteRepositories );
        resolveUnsafeDependenciesFromFile( missingLicensesFromArtifact );
    }

    void resolveUnsafeDependenciesFromFile( File missingLicenses ) throws IOException, MojoExecutionException
    {
        SortedSet<MavenProject> unsafeDeps = getUnsafeDependencies();
        if ( missingLicenses != null && missingLicenses.exists() && missingLicenses.length() > 0 )
        {
            // there are missing licenses available from the artifact
            SortedProperties unsafeMappings = new SortedProperties( getEncoding() );

            if ( missingLicenses.exists() )
            {
                // load the missing file
                unsafeMappings.load( missingLicenses );
            }
            if ( HttpRequester.isStringUrl( missingFileUrl ) )
            {
                String httpRequestResult = HttpRequester.getFromUrl( missingFileUrl );
                unsafeMappings.load( new ByteArrayInputStream( httpRequestResult.getBytes() ) );
            }

            Set<MavenProject> resolvedDependencies = new HashSet<>();
            for ( MavenProject unsafeDependency : unsafeDeps )
            {
                String id = MojoHelper.getArtifactId( unsafeDependency.getArtifact() );

                if ( unsafeMappings.containsKey( id ) && StringUtils.isNotBlank( unsafeMappings.getProperty( id ) ) )
                {
                    // update license map
                    thirdPartyTool.addLicense( licenseMap, unsafeDependency, unsafeMappings.getProperty( id ) );

                    // remove
                    resolvedDependencies.add( unsafeDependency );
                }
            }

            // remove resolvedDependencies from unsafeDeps;
            unsafeDeps.removeAll( resolvedDependencies );
        }
    }

    void checkUnsafeDependencies()
    {
        SortedSet<MavenProject> unsafeDeps = getUnsafeDependencies();
        if ( CollectionUtils.isNotEmpty( unsafeDeps ) )
        {
            Log log = getLog();
            if ( log.isWarnEnabled() )
            {
                boolean plural = unsafeDeps.size() > 1;
                String message = String.format( "There %s %d %s with no license :",
                    plural ? "are" : "is",
                    unsafeDeps.size(),
                    plural ? "dependencies" : "dependency" );
                log.warn( message );
                for ( MavenProject dep : unsafeDeps )
                {

                    // no license found for the dependency
                    log.warn( " - " + MojoHelper.getArtifactId( dep.getArtifact() ) );
                }
            }
        }
    }

    boolean checkForbiddenLicenses()
    {
        List<String> whiteLicenses = getIncludedLicenses();
        List<String> blackLicenses = getExcludedLicenses();
        Set<String> unsafeLicenses = new HashSet<>();
        if ( CollectionUtils.isNotEmpty( blackLicenses ) )
        {
            Set<String> licenses = getLicenseMap().keySet();
            getLog().info( "Excluded licenses (blacklist): " + blackLicenses );

            for ( String excludeLicense : blackLicenses )
            {
                if ( licenses.contains( excludeLicense )
                        && CollectionUtils.isNotEmpty( getLicenseMap().get( excludeLicense ) ) )
                {
                    //bad license found
                    unsafeLicenses.add( excludeLicense );
                }
            }
        }

        if ( CollectionUtils.isNotEmpty( whiteLicenses ) )
        {
            Set<String> dependencyLicenses = getLicenseMap().keySet();
            getLog().info( "Included licenses (whitelist): " + whiteLicenses );

            for ( String dependencyLicense : dependencyLicenses )
            {
                getLog().debug( "Testing license '" + dependencyLicense + "'" );
                if ( !whiteLicenses.contains( dependencyLicense )
                        && CollectionUtils.isNotEmpty( getLicenseMap().get( dependencyLicense ) ) )
                {
                    getLog().debug( "Testing dependency license '" + dependencyLicense
                            + "' against all other licenses" );

                    for ( MavenProject dependency : getLicenseMap().get( dependencyLicense ) )
                    {
                        getLog().debug( "  testing dependency " + dependency );

                        boolean forbiddenLicenseUsed = true;

                        for ( String otherLicense : dependencyLicenses )
                        {
                            // skip this license if it is the same as the dependency license
                            // skip this license if it has no projects assigned
                            if ( otherLicense.equals( dependencyLicense )
                                    || getLicenseMap().get( dependencyLicense ).isEmpty() )
                            {
                                continue;
                            }

                            // skip this license if it isn't one of the whitelisted
                            if ( !whiteLicenses.contains( otherLicense ) )
                            {
                                continue;
                            }

                            if ( getLicenseMap().get( otherLicense ).contains( dependency ) )
                            {
                                getLog().info( "License: '" + dependencyLicense + "' for '" + dependency + "'is OK "
                                                 + "since it is also licensed under '" + otherLicense + "'" );
                                // this dependency is licensed under another license from white list
                                forbiddenLicenseUsed = false;
                                break;
                            }
                        }

                        //bad license found
                        if ( forbiddenLicenseUsed )
                        {
                            unsafeLicenses.add( dependencyLicense );
                            break;
                        }
                    }
                }
            }
        }

        boolean safe = CollectionUtils.isEmpty( unsafeLicenses );

        if ( !safe )
        {
            Log log = getLog();
            log.warn( "There are " + unsafeLicenses.size() + " forbidden licenses used:" );
            for ( String unsafeLicense : unsafeLicenses )
            {

                SortedSet<MavenProject> deps = getLicenseMap().get( unsafeLicense );
                if ( !deps.isEmpty() )
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append( "License: '" ).append( unsafeLicense ).append( "' used by " ).append( deps.size() )
                        .append( " dependencies:" );
                    for ( MavenProject dep : deps )
                    {
                        sb.append( "\n -" ).append( MojoHelper.getArtifactName( dep ) );
                    }
                    log.warn( sb.toString() );
                }
            }
        }
        return safe;
    }

    void writeThirdPartyFile()
            throws IOException
    {

        if ( doGenerate )
        {

            LicenseMap licenseMap1 = getLicenseMap();

            if ( sortArtifactByName )
            {
                licenseMap1 = licenseMap.toLicenseMapOrderByName();
            }
            thirdPartyTool.writeThirdPartyFile( licenseMap1, thirdPartyFile, isVerbose(), getEncoding(), fileTemplate );
        }

        if ( doGenerateBundle )
        {

            thirdPartyTool.writeBundleThirdPartyFile( thirdPartyFile, getOutputDirectory(), bundleThirdPartyPath );
        }
    }

    void overrideLicenses() throws IOException
    {
        LicenseMap licenseMap1 = getLicenseMap();
        thirdPartyTool.overrideLicenses( licenseMap1, projectDependencies, getEncoding(), overrideFile );
    }

    private boolean isFailOnMissing()
    {
        return failOnMissing;
    }

    private boolean isFailOnBlacklist()
    {
        return failOnBlacklist;
    }

    void checkMissing( boolean unsafe ) throws MojoFailureException
    {

        if ( unsafe && ( isFailOnMissing() || isFailIfWarning() ) )
        {
            throw new MojoFailureException(
                    "There are some dependencies with no license, please fill the file " + getMissingFile() );
        }
    }

    void checkBlacklist( boolean safeLicense ) throws MojoFailureException
    {
        if ( !safeLicense && ( isFailOnBlacklist() || isFailIfWarning() ) )
        {
            throw new MojoFailureException( "There are some forbidden licenses used, please check your dependencies." );
        }
    }

    /**
     * Class to fill the {@link #includedLicenses} parameter, from a simple string to split, or a list of string.
     * <p>
     * TODO-tchemit We should find a way to create a plexus convertor.
     *
     * @since 1.4
     */
    public static class IncludedLicenses
            extends StringToList
    {

        /**
         * Default constructor used when {@link #includedLicenses} parameter is configured by a list.
         */
        public IncludedLicenses()
        {
        }

        /**
         * Constructor used when {@link #includedLicenses} parameter is configured by a string to split.
         *
         * @param data the string to split to fill the list of data of the object.
         */
        IncludedLicenses( String data ) throws MojoExecutionException
        {
            super( data );
        }

        /**
         * Add a simple a include license to the list.
         *
         * @param includeLicense the include license to add.
         */
        public void setIncludedLicense( String includeLicense )
        {
            addEntryToList( includeLicense );
        }
    }

    /**
     * Class to fill the {@link #excludedLicenses} parameter, from a simple string to split, or a list of string.
     * <p>
     * TODO-tchemit We should find a way to create a plexus convertor.
     *
     * @since 1.4
     */
    public static class ExcludedLicenses
            extends StringToList
    {

        /**
         * Default constructor used when {@link #excludedLicenses} parameter is configured by a list.
         */
        public ExcludedLicenses()
        {
        }

        /**
         * Constructor used when {@link #excludedLicenses} parameter is configured by a string to split.
         *
         * @param data the string to split to fill the list of data of the object.
         */
        ExcludedLicenses( String data ) throws MojoExecutionException
        {
            super( data );
        }

        /**
         * Add a simple exclude License to the list.
         *
         * @param excludeLicense the excludelicense to add.
         */
        public void setExcludedLicense( String excludeLicense )
        {
            addEntryToList( excludeLicense );
        }
    }

}
