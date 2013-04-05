package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 CodeLutin, Codehaus, Tony Chemit
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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.api.*;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.model.LicenseStore;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.mojo.license.utils.*;
import org.codehaus.plexus.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Download the license files of all the current project's dependencies, and generate a summary file containing a list
 * of all dependencies and their licenses.
 *
 * @author Paul Gier
 * @version $Revision$
 * @since 1.0
 */
@Mojo( name = "download-licenses", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PACKAGE )
public class DownloadLicensesMojo
    extends AbstractMojo
    implements MavenProjectDependenciesConfigurator
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Location of the local repository.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${localRepository}", readonly = true )
    private ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${project.remoteArtifactRepositories}", readonly = true )
    private List remoteRepositories;

    /**
     * Input file containing a mapping between each dependency and it's license information.
     *
     * @since 1.0
     */
    @Parameter( property = "licensesConfigFile", defaultValue = "${project.basedir}/src/license/licenses.xml" )
    private File licensesConfigFile;

    /**
     * The directory to which the dependency licenses should be written.
     *
     * @since 1.0
     */
    @Parameter( property = "licensesOutputDirectory",
                defaultValue = "${project.build.directory}/generated-resources/licenses" )
    private File licensesOutputDirectory;

    /**
     * The output file containing a mapping between each dependency and it's license information.
     *
     * @since 1.0
     */
    @Parameter( property = "licensesOutputFile",
                defaultValue = "${project.build.directory}/generated-resources/licenses.xml" )
    private File licensesOutputFile;

    /**
     * A filter to exclude some scopes.
     *
     * @since 1.0
     */
    @Parameter( property = "license.excludedScopes", defaultValue = "system" )
    private String excludedScopes;

    /**
     * A filter to include only some scopes, if let empty then all scopes will be used (no filter).
     *
     * @since 1.0
     */
    @Parameter( property = "license.includedScopes", defaultValue = "" )
    private String includedScopes;

    /**
     * Settings offline flag (will not download anything if setted to true).
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${settings.offline}" )
    private boolean offline;

    /**
     * Don't show warnings about bad or missing license files.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "false" )
    private boolean quiet;

    /**
     * Include transitive dependencies when downloading license files.
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "true" )
    private boolean includeTransitiveDependencies;

    /**
     * Get declared proxies from the {@code settings.xml} file.
     *
     * @since 1.4
     */
    @Parameter( defaultValue = "${settings.proxies}", readonly = true )
    private List<Proxy> proxies;

    /**
     * A flag to skip the goal.
     *
     * @since 1.5
     */
    @Parameter( property = "license.skipDownloadLicenses", defaultValue = "false")
    protected boolean skipDownloadLicenses;

    /**
     * A flag to use the missing licenses file to consolidate the THID-PARTY file.
     *
     * @since 1.5
     */
    @Parameter( property = "license.useMissingFile", defaultValue = "false" )
    private boolean useMissingFile;

    /**
     * The file where to fill the license for dependencies with unknwon license.
     *
     * @since 1.5
     */
    @Parameter( property = "license.missingFile", defaultValue = "src/license/THIRD-PARTY.properties" )
    private File missingFile;

    /**
     * Load files supplying information for missing third party licenses from repositories.
     * The plugin looks for Maven artifacts with coordinates of the form G:A:V:properties:third-party, where
     * the group, artifact, and version are those for dependencies of your project,
     * while the type is 'properties' and the classifier is 'third-party'.
     *
     * @since 1.5
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
     * @since 1.5
     */
    @Parameter
    private List<String> licenseMerges;

    /**
     * To specify an external extra licenses repository resolver (says the base
     * url of the repository where the {@code license.properties} is present).
     *
     * <p>
     * <strong>Note: </strong>If you want to refer to a file within this project, start the expression with <code>${project.baseUri}</code>
     * </p>
     *
     * @since 1.5
     */
    @Parameter( property = "license.licenseResolver" )
    private String licenseResolver;

    /**
     * Encoding used to read and writes files.
     *
     * @since 1.5
     */
    @Parameter(property = "license.encoding", defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * The Maven Project Object
     *
     * @since 1.0
     */
    @Component
    private MavenProject project;

    /**
     * Dependencies tool.
     *
     * @component
     * @readonly
     * @since 1.0
     */
    @Component
    private DependenciesTool dependenciesTool;

    /**
     * third party tool.
     *
     * @component
     * @readonly
     * @since 1.5
     */
    @Component
    private ThirdPartyTool thirdPartyTool;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Keeps a collection of the URLs of the licenses that have been downlaoded. This helps the plugin to avoid
     * downloading the same license multiple times.
     */
    private Set<String> downloadedLicenseURLs = new HashSet<String>();

    /**
     * Proxy Login/Password encoded(only if usgin a proxy with authentication).
     *
     * @since 1.4
     */
    private String proxyLoginPasswordEncoded;

    /**
     * Third-party helper (high level tool with common code for mojo and report).
     *
     * @since 1.5
     */
    private ThirdPartyHelper helper;

    /**
     * Store of available licenses.
     *
     * @since 1.5
     */
    private LicenseStore licenseStore;

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {

        if ( offline )
        {

            getLog().warn( "Offline flag is on, download-licenses goal is skip." );
            return;
        }
        if ( skipDownloadLicenses )
        {
            getLog().info( "skip flag is on, will skip goal." );
            return;
        }

        initDirectories();

        initProxy();

        // init licenses store
        licenseStore = LicenseStore.createLicenseStore( getLog(), licenseResolver );

        helper = new DefaultThirdPartyHelper( project, encoding, isVerbose(), dependenciesTool, thirdPartyTool,
                                              localRepository, remoteRepositories, getLog() );

        Map<String, ProjectLicenseInfo> configuredDepLicensesMap = new HashMap<String, ProjectLicenseInfo>();

        // License info from previous build
        if ( licensesOutputFile.exists() )
        {
            loadLicenseInfo( configuredDepLicensesMap, licensesOutputFile, true );
        }

        // Manually configured license info, loaded second to override previously loaded info
        if ( licensesConfigFile.exists() )
        {
            loadLicenseInfo( configuredDepLicensesMap, licensesConfigFile, false );
        }

        SortedMap<String, MavenProject> dependencies =
            dependenciesTool.loadProjectDependencies( project, this, localRepository, remoteRepositories, null );

        // The resulting list of licenses after dependency resolution
        List<ProjectLicenseInfo> depProjectLicenses = new ArrayList<ProjectLicenseInfo>();

        for ( MavenProject project : dependencies.values() )
        {
            Artifact artifact = project.getArtifact();
            getLog().debug( "Checking licenses for project " + artifact );
            String artifactProjectId = getArtifactProjectId( artifact );
            ProjectLicenseInfo depProject;
            if ( configuredDepLicensesMap.containsKey( artifactProjectId ) )
            {
                depProject = configuredDepLicensesMap.get( artifactProjectId );
                depProject.setVersion( artifact.getVersion() );
            }
            else
            {
                depProject = createDependencyProject( project );
            }
            downloadLicenses( depProject );
            depProjectLicenses.add( depProject );
        }

        try
        {
            LicenseSummaryWriter.writeLicenseSummary( depProjectLicenses, licensesOutputFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to write license summary file: " + licensesOutputFile, e );
        }
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean isVerbose()
    {
        return !quiet;
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
    public List<String> getExcludedScopes()
    {
        String[] split = excludedScopes == null ? new String[0] : excludedScopes.split( "," );
        return Arrays.asList( split );
    }

    public void setExcludedScopes( String excludedScopes )
    {
        this.excludedScopes = excludedScopes;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getIncludedScopes()
    {
        String[] split = includedScopes == null ? new String[0] : includedScopes.split( "," );
        return Arrays.asList( split );
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedArtifacts()
    {
        return null;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedGroups()
    {
        return null;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getExcludedGroups()
    {
        return null;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getExcludedArtifacts()
    {
        return null;
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private Collection<ThirdPartyDetails> createThirdPartyDetails()
            throws IOException, ThirdPartyToolException, ProjectBuildingException, MojoFailureException {

        // load dependencies of the project
        SortedMap<String, MavenProject> projectDependencies = helper.loadDependencies( this );

        // create licenseMap from it
        LicenseMap licenseMap = helper.createLicenseMap( projectDependencies );

        // Get unsafe dependencies (dependencies with no license in pom)
        SortedSet<MavenProject> dependenciesWithNoLicense = helper.getProjectsWithNoLicense( licenseMap );

        // compute safe dependencies (with pom licenses)
        Set<MavenProject> dependenciesWithPomLicense =
                new TreeSet<MavenProject>(MojoHelper.newMavenProjectComparator());
        dependenciesWithPomLicense.addAll(projectDependencies.values());

        if (CollectionUtils.isNotEmpty(dependenciesWithNoLicense)) {
            // there is some unsafe dependencies, remove them from safe dependencies
            dependenciesWithPomLicense.removeAll(dependenciesWithNoLicense);

            if (useMissingFile) {
                // Resolve unsafe dependencies using missing files, this will update licenseMap and unsafeDependencies
                helper.createUnsafeMapping( licenseMap, missingFile, useRepositoryMissingFiles,
                                            dependenciesWithNoLicense, projectDependencies.values() );
            }
        }

        // LicenseMap is now complete, let's merge licenses if necessary
        helper.mergeLicenses( licenseMerges, licenseMap );

        // let's build thirdparty details for each dependencies
        Collection<ThirdPartyDetails> details = new ArrayList<ThirdPartyDetails>();

        for (Map.Entry<MavenProject, String[]> entry : licenseMap.toDependencyMap().entrySet()) {
            MavenProject dependency = entry.getKey();
            String[] licenses = entry.getValue();
            ThirdPartyDetails detail = new DefaultThirdPartyDetails(dependency);
            details.add(detail);
            if (dependenciesWithPomLicense.contains(dependency)) {

                // this is a pom licenses
                detail.setPomLicenses(licenses);
            } else if (!dependenciesWithNoLicense.contains(dependency)) {

                // this is a third-party licenses
                detail.setThirdPartyLicenses(licenses);
            }
        }
        return details;
    }


    private void initDirectories()
        throws MojoExecutionException
    {
        try
        {
            FileUtil.createDirectoryIfNecessary( licensesOutputDirectory );

            FileUtil.createDirectoryIfNecessary( licensesOutputFile.getParentFile() );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to create a directory...", e );
        }
    }

    private void initProxy()
        throws MojoExecutionException
    {
        Proxy proxyToUse = null;
        for ( Proxy proxy : proxies )
        {
            if ( proxy.isActive() && "http".equals( proxy.getProtocol() ) )
            {

                // found our proxy
                proxyToUse = proxy;
                break;
            }
        }
        if ( proxyToUse != null )
        {

            System.getProperties().put( "proxySet", "true" );
            System.setProperty( "proxyHost", proxyToUse.getHost() );
            System.setProperty( "proxyPort", String.valueOf( proxyToUse.getPort() ) );
            if(proxyToUse.getNonProxyHosts() != null)
            {
                System.setProperty( "nonProxyHosts", proxyToUse.getNonProxyHosts() );
            }
            if ( proxyToUse.getUsername() != null )
            {
                String loginPassword = proxyToUse.getUsername() + ":" + proxyToUse.getPassword();
                proxyLoginPasswordEncoded = new String( Base64.encodeBase64( loginPassword.getBytes() ) );
            }
        }
    }

    /**
     * Load the license information contained in a file if it exists. Will overwrite existing license information in the
     * map for dependencies with the same id. If the config file does not exist, the method does nothing.
     *
     * @param configuredDepLicensesMap A map between the dependencyId and the license info
     * @param licenseConfigFile        The license configuration file to load
     * @param previouslyDownloaded     Whether these licenses were already downloaded
     * @throws MojoExecutionException if could not load license infos
     */
    private void loadLicenseInfo( Map<String, ProjectLicenseInfo> configuredDepLicensesMap, File licenseConfigFile,
                                  boolean previouslyDownloaded )
        throws MojoExecutionException
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream( licenseConfigFile );
            List<ProjectLicenseInfo> licensesList = LicenseSummaryReader.parseLicenseSummary( fis );
            for ( ProjectLicenseInfo dep : licensesList )
            {
                configuredDepLicensesMap.put( dep.getId(), dep );
                if ( previouslyDownloaded )
                {
                    for ( License license : dep.getLicenses() )
                    {
                        // Save the URL so we don't download it again
                        downloadedLicenseURLs.add( license.getUrl() );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to parse license summary output file: " + licenseConfigFile, e );
        }
        finally
        {
            FileUtil.tryClose( fis );
        }
    }

    /**
     * Returns the project ID for the artifact
     *
     * @param artifact the artifact
     * @return groupId:artifactId
     */
    private String getArtifactProjectId( Artifact artifact )
    {
        return artifact.getGroupId() + ":" + artifact.getArtifactId();
    }

    /**
     * Create a simple DependencyProject object containing the GAV and license info from the Maven Artifact and LicenseStore.
     *
     * @param depMavenProject the dependency maven project
     * @return DependencyProject with artifact and license info
     */
    private ProjectLicenseInfo createDependencyProject(MavenProject depMavenProject) throws MojoExecutionException {
        ProjectLicenseInfo dependencyProject =
            new ProjectLicenseInfo( depMavenProject.getGroupId(), depMavenProject.getArtifactId(),
                                    depMavenProject.getVersion() );
        List<?> licenses = depMavenProject.getLicenses();
        if (licenses != null && !licenses.isEmpty()) {
            getLog().debug("Fully defined license found");
            for (Object license : licenses) {
                dependencyProject.addLicense((License) license);
            }

        } else {
            getLog().debug("No license found, relying on licensestore");

            Collection<ThirdPartyDetails> details;

            try {
                details = createThirdPartyDetails();
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            for (ThirdPartyDetails detail : details) {

                if (detail.hasThirdPartyLicenses()) {
                    for (String licenseStr : detail.getThirdPartyLicenses()) {

                        org.codehaus.mojo.license.model.License licenseFromStore = licenseStore.getLicense( licenseStr );
                        License license = new License();
                        license.setName(licenseStr);
                        license.setUrl(licenseFromStore.getLicenseURL().toExternalForm());

                        dependencyProject.addLicense(license);
                    }
                }
            }
        }
        return dependencyProject;
    }

    /**
     * Determine filename to use for downloaded license file. The file name is based on the configured name of the
     * license (if available) and the remote filename of the license.
     *
     * @param license the license
     * @return A filename to be used for the downloaded license file
     * @throws MalformedURLException if the license url is malformed
     */
    private String getLicenseFileName( License license )
        throws MalformedURLException
    {
        URL licenseUrl = new URL( license.getUrl() );
        File licenseUrlFile = new File( licenseUrl.getPath() );
        String licenseFileName = licenseUrlFile.getName();

        if ( license.getName() != null )
        {
            licenseFileName = license.getName() + " - " + licenseUrlFile.getName();
        }

        // Check if the file has a valid file extention
        final String defaultExtension = ".txt";
        int extensionIndex = licenseFileName.lastIndexOf( "." );
        if ( extensionIndex == -1 || extensionIndex > ( licenseFileName.length() - 3 ) )
        {
            // This means it isn't a valid file extension, so append the default
            licenseFileName = licenseFileName + defaultExtension;
        }

        // Force lower case so we don't end up with multiple copies of the same license
        licenseFileName = licenseFileName.toLowerCase();

        return licenseFileName;
    }

    /**
     * Download the licenses associated with this project
     *
     * @param depProject The project which generated the dependency
     */
    private void downloadLicenses( ProjectLicenseInfo depProject )
    {

        getLog().debug( "Downloading license(s) for project " + depProject );

        List<License> licenses = depProject.getLicenses();

        if ( depProject.getLicenses() == null || depProject.getLicenses().isEmpty() )
        {
            if ( !quiet )
            {
                getLog().warn( "No license information available for: " + depProject );
            }
            return;
        }

        for ( License license : licenses )
        {
            try
            {
                String licenseFileName = getLicenseFileName( license );

                File licenseOutputFile = new File( licensesOutputDirectory, licenseFileName );
                if ( licenseOutputFile.exists() )
                {
                    continue;
                }

                if ( !downloadedLicenseURLs.contains( license.getUrl() ) )
                {
                    LicenseDownloader.downloadLicense( license.getUrl(), proxyLoginPasswordEncoded, licenseOutputFile );
                    downloadedLicenseURLs.add( license.getUrl() );
                }
            }
            catch ( MalformedURLException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString() + " has an invalid license URL: " +
                                       license.getUrl() );
                }
            }
            catch ( FileNotFoundException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString() +
                                       " has a license URL that returns file not found: " + license.getUrl() );
                }
            }
            catch ( IOException e )
            {
                getLog().warn( "Unable to retrieve license for dependency: " + depProject.toString() );
                getLog().warn( license.getUrl() );
                getLog().warn( e.getMessage() );
            }

        }

    }

}
