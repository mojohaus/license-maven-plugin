package org.codehaus.mojo.license;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.api.*;
import org.codehaus.mojo.license.model.Dependency;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.LicenseRegistryClient;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.mojo.license.utils.StringToList;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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

/**
 * Abstract mojo for all third-party mojos.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public abstract class AbstractAddThirdPartyMojo
        extends AbstractLicenseMojo
{
    protected static final String DEP_SEPARATOR = "--";

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Directory where to generate files.
     *
     * @since 1.0
     */
    @Parameter( property = "license.outputDirectory",
            defaultValue = "${project.build.directory}/generated-resources/licenses", required = true )
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
    @Parameter( property = "license.includedScopes")
    String includedScopes;

    /**
     * A filter to exclude some GroupIds
     * This is a regular expression that is applied to groupIds (not an ant pattern).
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedGroups")
    String excludedGroups;

    /**
     * A filter to include only some GroupIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedGroups")
    String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedArtifacts")
    String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedArtifacts")
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

    @Parameter( property = "license.thirdPartyDepsFilename", defaultValue = "THIRD-PARTY-DEPS", required = true )
    String thirdPartyDepsJsonFilename;

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
     * @since 1.0
     */
    @Parameter
    List<String> licenseMerges;

    /**
     * To specify some licenses to include.
     * <p>
     * If this parameter is filled and a license is not in this {@code whitelist} then build will failed when property
     * {@link #failIfWarning} is <tt>true</tt>.
     * <p>
     * Since version {@code 1.4}, there is two ways to fill this parameter :
     * <ul>
     * <li>A simple string (separated by {@code |}), the way to use by property configuration:
     * <pre>&lt;includedLicenses&gt;licenseA|licenseB&lt;/includedLicenses&gt;</pre> or
     * <pre>-Dlicense.includedLicenses=licenseA|licenseB</pre>
     * </li>
     * <li>A list of string (can only be used in plugin configuration, not via property configuration)
     * <pre>
     * &lt;includedLicenses&gt;
     *   &lt;includedLicense&gt;licenseA&lt;/includedLicense&gt;
     *   &lt;includedLicenses&gt;licenseB&lt;/includedLicense&gt;
     * &lt;/includedLicenses&gt;</pre>
     * </li>
     * </ul>
     *
     * @since 1.1
     */
    @Parameter( property = "license.includedLicenses")
    IncludedLicenses includedLicenses;

    @Parameter( property = "license.hiddenLicenses")
    HiddenLicenses hiddenLicenses;

    Map<String, List<Dependency>> includedDependencies = new HashMap<>();
    Set<Dependency> listedDependencies;

    /**
     * To specify some licenses to exclude.
     * <p>
     * If a such license is found then build will failed when property
     * {@link #failIfWarning} is <tt>true</tt>.
     * <p>
     * Since version {@code 1.4}, there is two ways to fill this parameter :
     * <ul>
     * <li>A simple string (separated by {@code |}), the way to use by property configuration:
     * <pre>&lt;excludedLicenses&gt;licenseA|licenseB&lt;/excludedLicenses&gt;</pre> or
     * <pre>-Dlicense.excludedLicenses=licenseA|licenseB</pre>
     * </li>
     * <li>A list of string (can only be used in plugin configuration, not via property configuration)
     * <pre>
     * &lt;excludedLicenses&gt;
     *   &lt;excludedLicense&gt;licenseA&lt;/excludedLicense&gt;
     *   &lt;excludedLicense&gt;licenseB&lt;/excludedLicense&gt;
     * &lt;/excludedLicenses&gt;</pre>
     * </li>
     * </ul>
     *
     * @since 1.1
     */
    @Parameter( property = "license.excludedLicenses")
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

    @Parameter(property = "license.failOnNotWhitelistedDependency", defaultValue = "false")
    boolean failOnNotWhitelistedDependency;

    /**
     * A flag to sort artifact by name in the generated third-party file.
     * <p>
     * If not then artifacts are sorted by <pre>groupId:artifactId:version</pre>
     *
     * @since 1.6
     */
    @Parameter( property = "license.sortArtifactByName", defaultValue = "false" )
    boolean sortArtifactByName;

    @Parameter(property = "license.includedDependenciesWhitelist")
    private String includedDependenciesWhitelist;

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

    @Parameter(property = "license.fileWhitelist", defaultValue = "licenses-whitelist.txt")
    private String licenseFileWhitelist;

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
            throws ProjectBuildingException, IOException, ThirdPartyToolException;

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

        getLog().info(String.format("Loading WHITE licenses: %s", licenseFileWhitelist));
        final LicenseRegistryClient licenseRegistryClient = LicenseRegistryClient.getInstance();
        this.includedLicenses = new IncludedLicenses(licenseRegistryClient.getFileContent(licenseFileWhitelist));
        getLog().info("Loading HIDDEN licenses: licenses-hidden.txt");
        this.hiddenLicenses = new HiddenLicenses(licenseRegistryClient.getFileContent("licenses-hidden.txt"));

        if (StringUtils.isNotBlank(includedDependenciesWhitelist)) {
            getLog().info(String.format("Loading Dependencies Whitelist: %s", includedDependenciesWhitelist));
            includedDependencies.putAll(new ObjectMapper().readValue(licenseRegistryClient.getFileContent(includedDependenciesWhitelist), new TypeReference<Map<String, List<Dependency>>>() {}));
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

        licenseMap = getHelper().createLicenseMap( projectDependencies, proxyUrl );

        getLog().info("failOnBlackList=" + this.isFailOnBlacklist() +
                ", failOnMissing=" + this.isFailOnMissing() +
                ", failOnNotWhitelistedDependency=" + this.failOnNotWhitelistedDependency);

    }

    void consolidate() throws IOException, ArtifactNotFoundException, ArtifactResolutionException, MojoFailureException, ProjectBuildingException, ThirdPartyToolException {

        unsafeDependencies = getHelper().getProjectsWithNoLicense( licenseMap );

        if ( !CollectionUtils.isEmpty( unsafeDependencies ) )
        {
            if ( isUseMissingFile() && isDoGenerate() )
            {
                // load unsafeMapping from local file and/or third-party classified items.
                unsafeMappings = createUnsafeMapping();
            }
        }

        overrideLicenses();

        if (licenseMerges == null) {
            licenseMerges = new ArrayList<>();
        }

        getHelper().mergeLicenses( licenseMerges, licenseMap);

        if ( checkUnsafeDependencies() )
        {
            resolveUnsafeDependenciesFromFile( missingFile );
        }

        if ( !StringUtils.isBlank( missingLicensesFileArtifact ) && checkUnsafeDependencies() )
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


    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    File getOutputDirectory() {
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

    private List<String> getHiddenLicenses()
    {
        return hiddenLicenses.getData();
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
    public void setIncludedLicenses( String includedLicenses )
    {
        this.includedLicenses = new IncludedLicenses( includedLicenses );
    }

    /**
     * Fill the {@link #excludedLicenses} parameter from a simple string to split.
     *
     * @param excludedLicenses license to excludes separated by a {@code |}.
     */
    public void setExcludedLicenses( String excludedLicenses )
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
            helper =
                    new DefaultThirdPartyHelper( getProject(), getEncoding(), isVerbose(), dependenciesTool, thirdPartyTool,
                                                 localRepository, remoteRepositories, getLog() );
        }
        return helper;
    }

    void resolveUnsafeDependenciesFromArtifact(String groupId, String artifactId, String version)
            throws ArtifactNotFoundException, IOException, ArtifactResolutionException
    {
        File missingLicensesFromArtifact = thirdPartyTool.resolveMissingLicensesDescriptor( groupId, artifactId, version, localRepository, remoteRepositories );
        resolveUnsafeDependenciesFromFile( missingLicensesFromArtifact );
    }

    void resolveUnsafeDependenciesFromFile(File missingLicenses) throws IOException
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

            Set<MavenProject> resolvedDependencies = new HashSet<MavenProject>();
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

    boolean checkUnsafeDependencies()
    {
        SortedSet<MavenProject> unsafeDeps = getUnsafeDependencies();
        boolean unsafe = !CollectionUtils.isEmpty( unsafeDeps );
        if ( unsafe )
        {
            Log log = getLog();
            log.debug( "There is " + unsafeDeps.size() + " dependencies with no license :" );
            for ( MavenProject dep : unsafeDeps )
            {
                // no license found for the dependency
                log.debug( " - " + MojoHelper.getArtifactId( dep.getArtifact() ) );
            }
        }
        return unsafe;
    }

    boolean checkForbiddenLicenses()
    {
        List<String> whiteLicenses = getIncludedLicenses();
        List<String> blackLicenses = getExcludedLicenses();
        List<String> hiddenLicenses = getHiddenLicenses();

        Set<String> unsafeLicenses = new HashSet<>();
        if ( CollectionUtils.isNotEmpty( blackLicenses ) )
        {
            Set<String> licenses = getLicenseMap().keySet();
            getLog().info( "Excluded licenses (blacklist): " + prettyString(blackLicenses) );

            for ( String excludeLicense : blackLicenses )
            {
                if ( licenses.contains( excludeLicense ) &&
                        CollectionUtils.isNotEmpty( getLicenseMap().get( excludeLicense ) ) )
                {
                    //bad license found
                    unsafeLicenses.add( excludeLicense );
                }
            }
        }

        if ( CollectionUtils.isNotEmpty( whiteLicenses ) )
        {
            Set<String> dependencyLicenses = getLicenseMap().keySet();
            getLog().info("Included licenses (whitelist): " + prettyString(whiteLicenses));
            getLog().info( "Hidden licenses (HIDDEN): " + prettyString(hiddenLicenses));

            for ( String dependencyLicense : dependencyLicenses )
            {
                getLog().debug( "Testing license '" + dependencyLicense + "'" );
                SortedSet<MavenProject> artifactsWithLicense = getLicenseMap().get(dependencyLicense);
                if ( !whiteLicenses.contains( dependencyLicense ) &&
                        CollectionUtils.isNotEmpty(artifactsWithLicense) )
                {
                    getLog().debug( "Testing dependency license '" + dependencyLicense + "' against all other licenses" );
                    Set<MavenProject> dependenciesWithAllowedLicenses = new HashSet<>();
                    for ( MavenProject dependency : artifactsWithLicense)
                    {
                        getLog().debug( "  testing dependency " + dependency );

                        boolean forbiddenLicenseUsed = true;

                        for ( String otherLicense : dependencyLicenses )
                        {
                            //getLog().warn("Processing " + otherLicense + " license");
                            // skip this license if it is the same as the dependency license
                            // skip this license if it has no projects assigned
                            if ( otherLicense.equals( dependencyLicense ) || artifactsWithLicense.isEmpty() )
                            {
                                continue;
                            }

                            // skip this license if it isn't one of the whitelisted
                            if ( !whiteLicenses.contains( otherLicense ) )
                            {
                                continue;
                            }

                            if ( hiddenLicenses.contains( dependencyLicense ) ) {
                                getLog().warn("License '" + dependencyLicense + "' for '" + dependency + "'is HIDDEN'");
                                forbiddenLicenseUsed = false;
                                break;
                            }

                            if ( getLicenseMap().get( otherLicense ).contains( dependency ) )
                            {
                                getLog().info( "License '" + dependencyLicense + "' for '" + dependency + "'is OK since it is also licensed under '" + otherLicense + "'" );
                                // this dependency is licensed under another license from white list
                                forbiddenLicenseUsed = false;
                                break;
                            }
                        }

                        //bad license found
                        if ( forbiddenLicenseUsed )
                        {
                            //getLog().warn("Unsafe license: " + dependencyLicense);
                            unsafeLicenses.add( dependencyLicense );
                        } else {
                            dependenciesWithAllowedLicenses.add(dependency);
                        }
                    }
                    if (!dependenciesWithAllowedLicenses.isEmpty()) {
                        getLog().info(dependenciesWithAllowedLicenses.toString() + " has forbidden '" + dependencyLicense + "' but are also licensed with good one");
                        artifactsWithLicense.removeAll(dependenciesWithAllowedLicenses);
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
                    sb.append( "License \"" ).append( unsafeLicense ).append( "\" used by " ).append( deps.size() ).append(
                            " dependencies:" );
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

    private static String prettyString(List<String> whiteLicenses) {
        return whiteLicenses.stream().map(it -> "'" + it + "'").collect(Collectors.joining(","));
    }

    boolean checkUnlistedDependencies() {
        if (includedDependencies.isEmpty()) {
            return false;
        }

        getLog().info("Included dependencies (whitelist): " + includedDependencies);

        final Map<Boolean, Map<String, MavenProject>> partedDependencyMavenProjects = projectDependencies.entrySet()
                .stream()
                .collect(Collectors.partitioningBy(kebabedGav ->
                        calculateDependency(kebabedGav.getKey()).isPresent(),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );

        listedDependencies = partedDependencyMavenProjects.getOrDefault(true, Collections.emptyMap())
                .entrySet()
                .stream()
                .map((entry) ->
                        calculateDependency(entry.getKey())
                                .map(d -> {
                                    final MavenProject dependency = entry.getValue();
                                    d.update(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
                                    return d;
                                }).orElse(null)
                ).filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<String, MavenProject> unlistedDependencies = partedDependencyMavenProjects.getOrDefault(false, Collections.emptyMap());
        final boolean unlisted = !unlistedDependencies.isEmpty();

        if (unlisted) {
            final String mainErrorMessage = "There are " + unlistedDependencies.size() + " unlisted dependencies used:";
            logError.accept(isFailOnNotWhitelistedDependency(), mainErrorMessage);
            unlistedDependencies.values()
                    .forEach((ud) -> logError.accept(isFailOnNotWhitelistedDependency(),
                                    String.format("unlisted dep: %s:%s:%s",
                                            ud.getGroupId(),
                                            ud.getArtifactId(),
                                            ud.getVersion()
                                    )
                            )
                    );
        }
        return unlisted;
    }

    private Optional<Dependency> calculateDependency(String kebabGav) {
        final String groupIdArtifactId = kebabGav.substring(0, kebabGav.lastIndexOf(DEP_SEPARATOR));
        final String version = kebabGav.substring(kebabGav.lastIndexOf(DEP_SEPARATOR) + DEP_SEPARATOR.length());
        return includedDependencies.getOrDefault(groupIdArtifactId, Collections.emptyList())
                .stream()
                .filter(dependency -> matchesVersion(version, dependency.getVersion()))
                .findAny();
    }

    private boolean matchesVersion(String search, String pattern) {
        return Optional.ofNullable(pattern)
                .map(search::matches)
                .orElse(true);
    }

    private final BiConsumer<Boolean, String> logError = (error, message) -> {
        final Log log = getLog();
        if (error) {
            log.error(message);

        } else {
            log.warn(message);
        }
    };

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
            thirdPartyTool.writeThirdPartyFile( licenseMap1, thirdPartyFile, isVerbose(), getEncoding(), "templates/third-party-file.ftl", true );
        }

        if ( doGenerateBundle )
        {

            thirdPartyTool.writeBundleThirdPartyFile( thirdPartyFile, getOutputDirectory(), bundleThirdPartyPath );
        }
    }

    void writeThirdPartyDependenciesFile() throws IOException {
        if (!includedDependencies.isEmpty() || failOnNotWhitelistedDependency) {
            dependenciesTool.writeThirdPartyDependenciesFile(getOutputDirectory(), thirdPartyDepsJsonFilename + ".json", listedDependencies);
        }
    }

    void overrideLicenses() throws IOException {
        thirdPartyTool.overrideLicenses( licenseMap, projectDependencies, getEncoding(), "thirdparty-licenses.properties");
    }

    private boolean isFailOnMissing() {
        return failOnMissing;
    }

    private boolean isFailOnBlacklist() {
        return failOnBlacklist;
    }

    private boolean isFailOnNotWhitelistedDependency() {
        return failOnNotWhitelistedDependency;
    }

    void checkMissing(boolean unsafe) throws MojoFailureException {

        if ( unsafe && (isFailOnMissing() || isFailIfWarning()) )
        {
            throw new MojoFailureException(
                    "There are some dependencies with no license, please specify them in thirdparty-licenses.properties");
        }
    }

    void checkBlacklist(boolean safeLicense ) throws MojoFailureException {
        if ( !safeLicense && (isFailOnBlacklist() || isFailIfWarning()) )
        {
            throw new MojoFailureException( "There are some forbidden licenses used, please check your dependencies." );
        }
    }

    void checkDependenciesWhiteList(boolean unlisted) throws MojoFailureException {
        if (unlisted && isFailOnNotWhitelistedDependency()) {
            throw new MojoFailureException("There are some unlisted dependencies used, please check those ones.");
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
        IncludedLicenses(String data)
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
     * Class to fill the {@link #hiddenLicenses} parameter, from a simple string to split, or a list of string.
     */
    public static class HiddenLicenses
            extends StringToList
    {

        /**
         * Default constructor used when {@link #hiddenLicenses} parameter is configured by a list.
         */
        public HiddenLicenses()
        {
        }

        /**
         * Constructor used when {@link #hiddenLicenses} parameter is configured by a string to split.
         *
         * @param data the string to split to fill the list of data of the object.
         */
        HiddenLicenses(String data)
        {
            super( data );
        }

        /**
         * Add a simple a include license to the list.
         *
         * @param hiddenLicense the include license to add.
         */
        public void setHiddenLicense( String hiddenLicense )
        {
            addEntryToList( hiddenLicense );
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
        ExcludedLicenses(String data)
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

    public static class IncludedDependencies extends StringToList {

        public IncludedDependencies() {}

        public IncludedDependencies(String data) {
            super(data);
        }

        public IncludedDependencies(Collection<String> data) {
            super();
            getData().addAll(data);
        }
    }
}
