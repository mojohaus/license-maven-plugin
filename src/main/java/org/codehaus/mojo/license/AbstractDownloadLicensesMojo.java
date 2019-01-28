package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2016 Tony Chemit
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.model.ProjectLicense;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.LicenseDownloader;
import org.codehaus.mojo.license.utils.LicenseNotFoundException;
import org.codehaus.mojo.license.utils.LicenseSummaryReader;
import org.codehaus.mojo.license.utils.LicenseSummaryWriter;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import org.codehaus.mojo.license.api.ResolvedProjectDependencies;

/**
 * Created on 23/05/16.
 *
 * @author Tony Chemit - chemit@codelutin.com
 */
public abstract class AbstractDownloadLicensesMojo
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
     * An end of line constant name denoting the EOL string to use when redering the {@code licenses.xml} file.
     * Possible values are {@code LF}, {@code CRLF}, {@code AUTODETECT} and {@code PLATFORM}.
     * <p>
     * When the value {@code AUTODETECT} is used, the mojo will use whatever EOL value is used in the first existing of
     * the following files: {@link #licensesConfigFile}, <code>${basedir}/pom.xml</code>.
     * <p>
     * The value {@code PLATFORM} is deprecated but still kept for backwards compatibility reasons.
     *
     * @since 1.17
     */
    @Parameter( property = "licensesOutputFileEol", defaultValue = "AUTODETECT" )
    private Eol licensesOutputFileEol;

    /**
     * Encoding used to (1) read the file specified in {@link #licensesConfigFile} and (2) write the file specified in
     * {@link #licensesOutputFile}.
     *
     * @since 1.17
     */
    @Parameter( property = "licensesOutputFileEncoding", defaultValue = "${project.build.sourceEncoding}" )
    private String licensesOutputFileEncoding;


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
    @Parameter( property = "license.includedScopes" )
    private String includedScopes;

    /**
     * A filter to exclude some types.
     *
     * @since 1.15
     */
    @Parameter( property = "license.excludedTypes" )
    private String excludedTypes;

    /**
     * A filter to include only some types, if let empty then all types will be used (no filter).
     *
     * @since 1.15
     */
    @Parameter( property = "license.includedTypes" )
    private String includedTypes;

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
     * Exclude transitive dependencies from excluded artifacts.
     *
     * @since 1.13
     */
    @Parameter( property = "license.excludeTransitiveDependencies", defaultValue = "false" )
    private boolean excludeTransitiveDependencies;

    /**
     * Get declared proxies from the {@code settings.xml} file.
     *
     * @since 1.4
     */
    @Parameter( defaultValue = "${settings.proxies}", readonly = true )
    private List<Proxy> proxies;

    /**
     * A flag to organize the licenses by dependencies. When this is done, each dependency will
     * get its full license file, even if already downloaded for another dependency.
     *
     * @since 1.9
     */
    @Parameter( property = "license.organizeLicensesByDependencies", defaultValue = "false" )
    protected boolean organizeLicensesByDependencies;

    @Parameter( property = "license.sortByGroupIdAndArtifactId", defaultValue = "false" )
    private boolean sortByGroupIdAndArtifactId;

    /**
     * A filter to exclude some GroupIds
     * This is a regular expression that is applied to groupIds (not an ant pattern).
     *
     * @since 1.11
     */
    @Parameter( property = "license.excludedGroups" )
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter( property = "license.includedGroups" )
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter( property = "license.excludedArtifacts" )
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     * This is a regular expression applied to artifactIds.
     *
     * @since 1.11
     */
    @Parameter( property = "license.includedArtifacts" )
    private String includedArtifacts;

    /**
     * The Maven Project Object
     *
     * @since 1.0
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * List of regexps/replacements applied to the license urls prior to download.
     *
     * <p>License urls that match a regular expression will be replaced by the corresponding
     * replacement. Replacement is performed with {@link java.util.regex.Matcher#replaceAll(String)
     * java.util.regex.Matcher#replaceAll(String)} so you can take advantage of
     * capturing groups to facilitate flexible transformations.</p>
     *
     * <p>If the replacement element is omitted, this is equivalent to an empty replacement string.</p>
     *
     * <pre>
     * {@code
     *
     * <licenseUrlReplacements>
     *   <licenseUrlReplacement>
     *     <regexp>\Qhttps://glassfish.java.net/public/CDDL+GPL_1_1.html\E</regexp>
     *     <replacement>https://oss.oracle.com/licenses/CDDL+GPL-1.1</replacement>
     *   </licenseUrlReplacement>
     *   <licenseUrlReplacement>
     *      <regexp>https://(.*)</regexp>
     *      <replacement>http://$1</replacement>
     *   </licenseUrlReplacement>
     * </licenseUrlReplacements>
     * }
     * </pre>
     *
     * @since 1.17
     */
    @Parameter
    private List<LicenseUrlReplacement> licenseUrlReplacements;

    // ----------------------------------------------------------------------
    // Plexus Components
    // ----------------------------------------------------------------------

    /**
     * Dependencies tool.
     *
     * @since 1.0
     */
    @Component
    private DependenciesTool dependenciesTool;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * A map from the license URLs to file names (without path) where the
     * licenses were downloaded. This helps the plugin to avoid downloading
     * the same license multiple times.
     */
    private Map<String, File> downloadedLicenseURLs = new HashMap<>();

    /**
     * Proxy Login/Password encoded(only if usgin a proxy with authentication).
     *
     * @since 1.4
     */
    private String proxyLoginPasswordEncoded;

    protected abstract boolean isSkip();

    protected MavenProject getProject()
    {
        return project;
    }

    protected abstract Set<MavenProject> getDependencies();

    // ----------------------------------------------------------------------
    // Mojo Implementation
    // ----------------------------------------------------------------------

    protected SortedMap<String, MavenProject> getDependencies( MavenProject project )
    {
        return dependenciesTool.loadProjectDependencies(
                new ResolvedProjectDependencies( project.getArtifacts(), project.getDependencyArtifacts() ),
                this, localRepository, remoteRepositories, null );
    }

    protected java.util.Properties systemProperties;

    protected void storeProperties()
    {
        systemProperties = (java.util.Properties) System.getProperties().clone();
    }
    protected void restoreProperties()
    {
        if ( systemProperties != null )
        {
            System.setProperties( systemProperties );
            systemProperties = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {

        if ( isSkip() )
        {
            getLog().info( "skip flag is on, will skip goal." );
            return;
        }

        initDirectories();

        try
        {
            initProxy();

            Map<String, ProjectLicenseInfo> configuredDepLicensesMap = new HashMap<>();

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

            Set<MavenProject> dependencies = getDependencies();

            // The resulting list of licenses after dependency resolution
            List<ProjectLicenseInfo> depProjectLicenses = new ArrayList<>();

            for ( MavenProject project : dependencies )
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
                if ( !offline )
                {
                    downloadLicenses( depProject );
                }
                depProjectLicenses.add( depProject );
            }

            try
            {
                if ( sortByGroupIdAndArtifactId )
                {
                    depProjectLicenses = sortByGroupIdAndArtifactId( depProjectLicenses );
                }

                if ( licensesOutputFileEncoding == null )
                {
                    licensesOutputFileEncoding = System.getProperty( "file.encoding" );
                    getLog().warn( "Using the default system encoding for reading or writing licenses.xml file."
                            + " This makes your build platform dependent. You should set either"
                            + " project.build.sourceEncoding or licensesOutputFileEncoding" );
                }
                final Charset charset = Charset.forName( licensesOutputFileEncoding );
                if ( licensesOutputFileEol == Eol.AUTODETECT )
                {
                    final Path autodetectFromFile = licensesConfigFile.exists() ? licensesConfigFile.toPath()
                            : project.getBasedir().toPath().resolve( "pom.xml" );
                    licensesOutputFileEol = Eol.autodetect( autodetectFromFile, charset );
                }

                LicenseSummaryWriter.writeLicenseSummary( depProjectLicenses, licensesOutputFile,
                        charset, licensesOutputFileEol );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to write license summary file: " + licensesOutputFile, e );
            }
        }
        finally
        {
            //restore the system properties to what they where before the plugin execution
            restoreProperties();
        }
    }

    private List<ProjectLicenseInfo> sortByGroupIdAndArtifactId( List<ProjectLicenseInfo> depProjectLicenses )
    {
        List<ProjectLicenseInfo> sorted = new ArrayList<>( depProjectLicenses );
        Comparator<ProjectLicenseInfo> comparator = new Comparator<ProjectLicenseInfo>()
        {
            public int compare( ProjectLicenseInfo info1, ProjectLicenseInfo info2 )
            {
                //ProjectLicenseInfo::getId() can not be used because . is before : thus a:b.c would be after a.b:c
                return ( info1.getGroupId() + "+" + info1.getArtifactId() ).compareTo( info2.getGroupId()
                        + "+" + info2.getArtifactId() );
            }
        };
        Collections.sort( sorted, comparator );
        return sorted;
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementation
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

    /**
     * {@inheritDoc}
     */
    public List<String> getExcludedScopes()
    {
        return MojoHelper.getParams( excludedScopes );
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
        return MojoHelper.getParams( includedScopes );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getExcludedTypes()
    {
        return MojoHelper.getParams( excludedTypes );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getIncludedTypes()
    {
        return MojoHelper.getParams( includedTypes );
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedArtifacts()
    {
        return includedArtifacts;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getIncludedGroups()
    {
        return includedGroups;
    }

    // not used at the moment

    /**
     * {@inheritDoc}
     */
    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    // not used at the moment

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
    public boolean isVerbose()
    {
        return getLog().isDebugEnabled();
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

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
            //Save our system settings for restore after plugin run
            storeProperties();
            System.getProperties().put( "proxySet", "true" );
            System.setProperty( "proxyHost", proxyToUse.getHost() );
            System.setProperty( "proxyPort", String.valueOf( proxyToUse.getPort() ) );
            if ( proxyToUse.getNonProxyHosts() != null )
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
                    for ( ProjectLicense license : dep.getLicenses() )
                    {
                        final String fileName = license.getFile();
                        if ( fileName != null )
                        {
                            final File licenseFile = new File( licensesOutputDirectory, fileName );
                            if ( licenseFile.exists() )
                            {
                                // Save the URL so we don't download it again
                                downloadedLicenseURLs.put( license.getUrl(), licenseFile );
                            }
                        }
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
     * Create a simple DependencyProject object containing the GAV and license info from the Maven Artifact
     *
     * @param depMavenProject the dependency maven project
     * @return DependencyProject with artifact and license info
     */
    private ProjectLicenseInfo createDependencyProject( MavenProject depMavenProject )
    {
        ProjectLicenseInfo dependencyProject =
            new ProjectLicenseInfo( depMavenProject.getGroupId(), depMavenProject.getArtifactId(),
                                    depMavenProject.getVersion() );
        List<?> licenses = depMavenProject.getLicenses();
        for ( Object license : licenses )
        {
            dependencyProject.addLicense( new ProjectLicense( (License) license ) );
        }
        return dependencyProject;
    }

    /**
     * Determine filename to use for downloaded license file. The file name is based on the configured name of the
     * license (if available) and the remote filename of the license.
     *
     * @param depProject the project containing the license
     * @param licenseUrl the license url
     * @param licenseName the license name
     * @return A filename to be used for the downloaded license file
     */
    private String getLicenseFileName( ProjectLicenseInfo depProject, final URL licenseUrl, final String licenseName )
    {
        String defaultExtension = ".txt";

        File licenseUrlFile = new File( licenseUrl.getPath() );

        String licenseFileName;

        if ( organizeLicensesByDependencies )
        {
            licenseFileName = String.format( "%s.%s%s", depProject.getGroupId(), depProject.getArtifactId(),
                                             licenseName != null
                                                 ? "_" + licenseName
                                                 : "" ).toLowerCase().replaceAll( "[/\\s]+", "_" );
        }
        else
        {
            licenseFileName = licenseUrlFile.getName();

            if ( licenseName != null )
            {
                licenseFileName = licenseName.replaceAll( "/", "_" )
                                  + " - " + licenseUrlFile.getName();
            }

            // Check if the file has a valid file extention
            int extensionIndex = licenseFileName.lastIndexOf( "." );
            if ( extensionIndex == -1 || extensionIndex > ( licenseFileName.length() - 3 ) )
            {
                // This means it isn't a valid file extension, so append the default
                licenseFileName = licenseFileName + defaultExtension;
            }

            // Force lower case so we don't end up with multiple copies of the same license
            licenseFileName = licenseFileName.toLowerCase();
        }
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

        List<ProjectLicense> licenses = depProject.getLicenses();

        if ( depProject.getLicenses() == null || depProject.getLicenses().isEmpty() )
        {
            if ( !quiet )
            {
                getLog().warn( "No license information available for: " + depProject );
            }
            return;
        }

        for ( ProjectLicense license : licenses )
        {
            final String licenseUrl = rewriteLicenseUrlIfNecessary( license.getUrl() );
            try
            {

                File licenseOutputFile = downloadedLicenseURLs.get( licenseUrl );
                if ( licenseOutputFile == null )
                {
                    final String licenseFileName;
                    if ( license.getFile() != null )
                    {
                        licenseFileName = new File( license.getFile() ).getName();
                    }
                    else
                    {
                        licenseFileName = getLicenseFileName( depProject,
                                                              new URL( license.getUrl() ),
                                                              license.getName() );
                    }
                    licenseOutputFile = new File( licensesOutputDirectory, licenseFileName );
                }

                if ( !licenseOutputFile.exists() )
                {
                    if ( !downloadedLicenseURLs.containsKey( licenseUrl ) || organizeLicensesByDependencies )
                    {
                        licenseOutputFile = LicenseDownloader.downloadLicense( licenseUrl, proxyLoginPasswordEncoded,
                                licenseOutputFile );
                        downloadedLicenseURLs.put( licenseUrl, licenseOutputFile );
                    }
                }

                if ( licenseOutputFile != null )
                {
                    license.setFile( licenseOutputFile.getName() );
                }

            }
            catch ( MalformedURLException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString() + " has an invalid license URL: "
                                       + licenseUrl );
                }
            }
            catch ( LicenseNotFoundException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString()
                                       + " has a license URL that returns file not found: " + e.getLicenseUrl() );
                }
            }
            catch ( IOException e )
            {
                getLog().warn( "Unable to retrieve license for dependency: " + depProject.toString() );
                getLog().warn( licenseUrl );
                getLog().warn( e.getMessage() );
            }

        }

    }

    private String rewriteLicenseUrlIfNecessary( final String originalLicenseUrl )
    {
        String resultUrl = originalLicenseUrl;
        if ( licenseUrlReplacements != null )
        {
            for ( LicenseUrlReplacement urlReplacement : licenseUrlReplacements )
            {
                Pattern regexp = urlReplacement.getRegexp();
                String replacement = urlReplacement.getReplacement() == null ? "" : urlReplacement.getReplacement();
                if ( regexp != null )
                {
                    resultUrl = regexp.matcher( resultUrl ).replaceAll( replacement );
                }
            }

            if ( !resultUrl.equals( originalLicenseUrl ) )
            {
                getLog().debug( String.format( "Rewrote URL %s => %s", originalLicenseUrl, resultUrl ) );
            }
        }
        return resultUrl;
    }
}
