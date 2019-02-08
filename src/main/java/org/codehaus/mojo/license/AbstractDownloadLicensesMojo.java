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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.api.ResolvedProjectDependencies;
import org.codehaus.mojo.license.download.Cache;
import org.codehaus.mojo.license.download.FileNameEntry;
import org.codehaus.mojo.license.download.PreferredFileNames;
import org.codehaus.mojo.license.model.ProjectLicense;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.LicenseDownloader;
import org.codehaus.mojo.license.utils.LicenseDownloader.LicenseDownloadResult;
import org.codehaus.mojo.license.utils.LicenseSummaryReader;
import org.codehaus.mojo.license.utils.LicenseSummaryWriter;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

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
     * A file containing dependencies whose licenses could not be downloaded for some reason. The format is similar to
     * {@link #licensesOutputFile} but the entries in {@link #licensesErrorsFile} have {@code <downloaderMessage>}
     * elements attached to them. Those should explain what kind of error happened during the processing of the given
     * dependency.
     *
     * @since 1.18
     */
    @Parameter( property = "license.licensesErrorsFile",
        defaultValue = "${project.build.directory}/generated-resources/licenses-errors.xml" )
    private File licensesErrorsFile;

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
     * Before 1.18, {@link #quiet} having value {@code false} suppressed any license download related warnings in the
     * log. After 1.18 (incl.), the behavior depends on the value of {@link #errorRemedy}:
     * <table>
     *   <tr><th>quiet</th><th>errorRemedy</th><th>effective errorRemedy</th><tr>
     *   <tr><td>true</td><td>warn</td><td>ignore</td><tr>
     *   <tr><td>false</td><td>warn</td><td>warn</td><tr>
     *   <tr><td>true or false</td><td>ignore</td><td>ignore</td><tr>
     *   <tr><td>true or false</td><td>failFast</td><td>failFast</td><tr>
     *   <tr><td>true or false</td><td>xmlOutput</td><td>xmlOutput</td><tr>
     * </table>
     *
     * @since 1.0
     * @deprecated Use {@link #errorRemedy} instead
     */
    @Parameter( defaultValue = "false" )
    private boolean quiet;

    /**
     * What to do on any license download related error. The possible values are:
     * <li>
     *   <ul>{@link ErrorRemedy#ignore}: all errors are ignored</ul>
     *   <ul>{@link ErrorRemedy#warn}: all errors are output to the log as warnings</ul>
     *   <ul>{@link ErrorRemedy#failFast}: a {@link MojoFailureException} is thrown on the first download related
     *      error</ul>
     *   <ul>{@link ErrorRemedy#xmlOutput}: error messages are added as {@code <downloaderMessages>} to
     *   {@link AbstractDownloadLicensesMojo#licensesErrorsFile}; in case there are error messages, the build will
         * fail after processing all dependencies</ul>
     * </li>
     * @since 1.18
     */
    @Parameter( property = "license.errorRemedy", defaultValue = "warn" )
    private ErrorRemedy errorRemedy;

    /**
     * If {@code true}, all encountered dependency license URLs are downloaded, no matter what is there in
     * {@link #licensesConfigFile} and {@link #licensesOutputFile}; otherwise {@link #licensesConfigFile},
     * {@link #licensesOutputFile} (eventually persisted from a previous build) and the content of
     * {@link #licensesOutputDirectory} are considered sources of valid information - i.e. only URLs that do not appear
     * to have been downloaded in the past will be downloaded.
     *
     * @since 1.18
     */
    @Parameter( property = "license.forceDownload", defaultValue = "false" )
    private boolean forceDownload;

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

    /**
     * A map that helps to select names for the local files where the downloaded licenses are stored.
     * <p>
     * Keys in the map are local file names. These files will be created under {@link #licensesOutputDirectory}.
     * <p>
     * Values are white space ({@code " \t\n\r"}) separated lists of regular expressions that will be used to match
     * license URLs. The regular expressions are compiled using {@link Pattern#CASE_INSENSITIVE}. Note that various
     * characters that commonly occur in URLs have special meanings in regular extensions. Therefore, consider using
     * regex quoting as described in {@link Pattern}.
     * <p>
     * In addition to URL patterns, the list must also contain the sha1 checksum of the expected content. This is to
     * ensure that URLs returning distinct licenses cannot be saved under the same local path. Note that strict checking
     * of the checksum happens only when {@link #forceDownload} is {@code true}. Otherwise the mojo assumes the URL
     * -&gt; local name mapping is correct and downloads from the URL only if the local file does not exist.
     * <p>
     * Using {@link #licenseUrlFileNames} for URLs returning volatile content is not a good idea.
     * <p>An example:
     * <pre>
     * {@code
     * <licenseUrlFileNames>
     *   <bsd-antlr.html>
     *       sha1:81ffbd1712afe8cdf138b570c0fc9934742c33c1
     *       https?://(www\.)?antlr\.org/license\.html
     *   </bsd-antlr.html>
     *   <cddl-gplv2-ce.txt>
     *       sha1:534a3fc9ae1076409bb00d01127dbba1e2620e92
     *       \Qhttps://raw.githubusercontent.com/javaee/activation/master/LICENSE.txt\E
     *   </cddl-gplv2-ce.txt>
     * </licenseUrlFileNames>
     * }
     * </pre>
     * <p>
     * Relationship to other parameters:
     * <ul>
     * <li>{@link #licenseUrlReplacements} are applied before {@link #licenseUrlFileNames}</li>
     * <li>{@link #licenseUrlFileNames} have higher precedence than {@code <file>} elements in
     * {@link #licensesConfigFile}</li>
     * <li>{@link #licenseUrlFileNames} are ignored when {@link #organizeLicensesByDependencies} is {@code true}</li>
     * </ul>
     *
     * @since 1.18
     */
    @Parameter
    private Map<String, String> licenseUrlFileNames;

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
    private PreferredFileNames preferredFileNames;
    /**
     * A map from the license URLs to file names (without path) where the
     * licenses were downloaded. This helps the plugin to avoid downloading
     * the same license multiple times.
     */
    private Cache cache;

    /**
     * Proxy Login/Password encoded(only if usgin a proxy with authentication).
     *
     * @since 1.4
     */
    private String proxyLoginPasswordEncoded;

    private int downloadErrorCount = 0;

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
     * @throws MojoFailureException
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {

        if ( isSkip() )
        {
            getLog().info( "skip flag is on, will skip goal." );
            return;
        }

        this.errorRemedy = getEffectiveErrorRemedy( this.quiet, this.errorRemedy );
        this.preferredFileNames = PreferredFileNames.build( licensesOutputDirectory, licenseUrlFileNames, getLog() );
        this.cache = new Cache( licenseUrlFileNames != null && !licenseUrlFileNames.isEmpty() );

        initDirectories();

        try
        {
            initProxy();

            Map<String, ProjectLicenseInfo> configuredDepLicensesMap = new HashMap<>();

            // License info from previous build
            if ( !forceDownload && licensesOutputFile.exists() )
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

            try ( LicenseDownloader licenseDownloader = new LicenseDownloader() )
            {
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
                    depProjectLicenses.add( depProject );
                }
                if ( !offline )
                {
                    /* First save the matching URLs into the cache */
                    for ( ProjectLicenseInfo depProject : depProjectLicenses )
                    {
                        downloadLicenses( licenseDownloader, depProject, true );
                    }
                    getLog().debug( "Finished populating cache" );
                    /*
                     * Then attempt to download the rest of the URLs using the available cache entries to select local
                     * file names based on file content sha1
                     */
                    for ( ProjectLicenseInfo depProject : depProjectLicenses )
                    {
                        downloadLicenses( licenseDownloader, depProject, false );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
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

                List<ProjectLicenseInfo> depProjectLicensesWithErrors = filterErrors( depProjectLicenses );
                LicenseSummaryWriter.writeLicenseSummary( depProjectLicenses, licensesOutputFile, charset,
                                                          licensesOutputFileEol );
                if ( depProjectLicensesWithErrors != null && !depProjectLicensesWithErrors.isEmpty() )
                {
                    LicenseSummaryWriter.writeLicenseSummary( depProjectLicensesWithErrors, licensesErrorsFile, charset,
                                                              licensesOutputFileEol );
                }
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

        switch ( errorRemedy )
        {
            case ignore:
            case failFast:
                /* do nothing */
                break;
            case warn:
                getLog().warn( "There were " + downloadErrorCount + " download errors - check the warnings above" );
                break;
            case xmlOutput:
                if ( downloadErrorCount > 0 )
                {
                    throw new MojoFailureException( "There were " + downloadErrorCount + " download errors - check "
                        + licensesErrorsFile.getAbsolutePath() );
                }
                break;
            default:
                throw new IllegalStateException( "Unexpected value of " + ErrorRemedy.class.getName() + ": "
                    + errorRemedy );
        }
    }

    /**
     * Removes from the given {@code depProjectLicenses} those elements which have non-empty
     * {@link ProjectLicenseInfo#getDownloaderMessages()} and adds those to the resulting {@link List}.
     *
     * @param depProjectLicenses the list of {@link ProjectLicenseInfo}s to filter
     * @return a new {@link List} of {@link ProjectLicenseInfo}s containing only elements with non-empty
     *         {@link ProjectLicenseInfo#getDownloaderMessages()}
     */
    private List<ProjectLicenseInfo> filterErrors( List<ProjectLicenseInfo> depProjectLicenses )
    {
        final List<ProjectLicenseInfo> result = new ArrayList<>();
        final Iterator<ProjectLicenseInfo> it = depProjectLicenses.iterator();
        while ( it.hasNext() )
        {
            final ProjectLicenseInfo dep = it.next();
            final List<String> messages = dep.getDownloaderMessages();
            if ( messages != null && !messages.isEmpty() )
            {
                it.remove();
                result.add( dep );
            }
        }
        return result;
    }

    private static ErrorRemedy getEffectiveErrorRemedy( boolean quiet, ErrorRemedy errorRemedy )
    {
        switch ( errorRemedy )
        {
            case warn:
                return quiet ? ErrorRemedy.ignore : ErrorRemedy.warn;
            default:
                return errorRemedy;
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

            FileUtil.createDirectoryIfNecessary( licensesErrorsFile.getParentFile() );
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
                        final String url = license.getUrl();
                        if ( url != null )
                        {
                            final String licenseUrl = rewriteLicenseUrlIfNecessary( url );
                            final FileNameEntry fileNameEntry =
                                getLicenseFileName( dep, licenseUrl, license.getName(), license.getFile() );
                            final File licenseFile = fileNameEntry.getFile();
                            if ( !forceDownload && licenseFile.exists() )
                            {
                                final String actualSha1 = FileUtil.sha1( licenseFile.toPath() );
                                if ( fileNameEntry.getSha1() != null && !actualSha1.equals( fileNameEntry.getSha1() ) )
                                {
                                    throw new MojoFailureException( "Unexpected sha1 checksum for file '"
                                            + licenseFile.getAbsolutePath() + "': '" + actualSha1 + "'; expected '"
                                            + fileNameEntry.getSha1() + "'. You may want to (a) re-run the current mojo"
                                            + " with -Dlicense.forceDownload=true or (b) change the expected sha1 in"
                                            + " the licenseUrlFileNames entry '"
                                            + fileNameEntry.getFile().getName() + "' or (c) split the entry so that"
                                            + " its URLs return content with different sha1 sums." );
                                }
                                // Save the URL so we don't download it again
                                cache.put( license.getUrl(), LicenseDownloadResult.success( licenseFile,
                                        actualSha1,
                                        fileNameEntry.isPreferred() ) );
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
     * @param string
     * @return A filename to be used for the downloaded license file
     * @throws URISyntaxException
     */
    private FileNameEntry getLicenseFileName( ProjectLicenseInfo depProject, final String url,
                                                           final String licenseName, String licenseFileName )
        throws URISyntaxException
    {

        final URI licenseUrl = new URI( url );
        File licenseUrlFile = new File( licenseUrl.getPath() );

        if ( organizeLicensesByDependencies )
        {
            if ( licenseFileName != null && !licenseFileName.isEmpty() )
            {
                return new FileNameEntry( new File( licensesOutputDirectory, new File( licenseFileName ).getName() ),
                                          false, null );
            }
            licenseFileName = String.format( "%s.%s%s", depProject.getGroupId(), depProject.getArtifactId(),
                                             licenseName != null
                                                 ? "_" + licenseName
                                                 : "" ).replaceAll( "\\s+", "_" );
        }
        else
        {
            final FileNameEntry preferredFileNameEntry = preferredFileNames.getEntryByUrl( url );
            if ( preferredFileNameEntry != null )
            {
                return preferredFileNameEntry;
            }

            if ( licenseFileName != null && !licenseFileName.isEmpty() )
            {
                return new FileNameEntry( new File( licensesOutputDirectory,
                                                                         new File( licenseFileName ).getName() ),
                                                               false, null );
            }

            licenseFileName = licenseUrlFile.getName();

            if ( licenseName != null )
            {
                licenseFileName = licenseName + " - " + licenseUrlFile.getName();
            }

            // Normalize whitespace
            licenseFileName = licenseFileName.replaceAll( "\\s+", " " );
        }

        // lower case and (back)slash removal
        licenseFileName = licenseFileName.toLowerCase( Locale.US ).replaceAll( "[\\\\/]+", "_" );

        return new FileNameEntry( new File( licensesOutputDirectory, licenseFileName ), false, null );
    }

    /**
     * Download the licenses associated with this project
     *
     * @param depProject The project which generated the dependency
     * @param matchingUrlsOnly
     * @throws MojoFailureException
     */
    private void downloadLicenses( LicenseDownloader licenseDownloader, ProjectLicenseInfo depProject,
                                   boolean matchingUrlsOnly )
        throws MojoFailureException
    {
        getLog().debug( "Downloading license(s) for project " + depProject );

        List<ProjectLicense> licenses = depProject.getLicenses();

        if ( matchingUrlsOnly && ( depProject.getLicenses() == null || depProject.getLicenses().isEmpty() ) )
        {
            handleError( depProject, "No license information available for: " + depProject );
            return;
        }

        int licenseIndex = 0;
        for ( ProjectLicense license : licenses )
        {
            if ( matchingUrlsOnly && license.getUrl() == null )
            {
                handleError( depProject, "No URL for license at index " + licenseIndex + " in dependency "
                    + depProject.toString() );
            }
            else if ( license.getUrl() != null )
            {
                final String licenseUrl = rewriteLicenseUrlIfNecessary( license.getUrl() );

                final LicenseDownloadResult cachedResult = cache.get( licenseUrl );
                try
                {

                    if ( cachedResult != null )
                    {
                        if ( cachedResult.isPreferredFileName() == matchingUrlsOnly )
                        {
                            if ( organizeLicensesByDependencies )
                            {
                                final FileNameEntry fileNameEntry =
                                    getLicenseFileName( depProject, licenseUrl, license.getName(), license.getFile() );
                                final File cachedFile = cachedResult.getFile();
                                final LicenseDownloadResult byDepsResult;
                                final File byDepsFile = fileNameEntry.getFile();
                                if ( cachedResult.isSuccess() && !cachedFile.equals( byDepsFile ) )
                                {
                                    Files.copy( cachedFile.toPath(), byDepsFile.toPath() );
                                    byDepsResult = cachedResult.withFile( byDepsFile );
                                }
                                else
                                {
                                    byDepsResult = cachedResult;
                                }
                                handleResult( licenseUrl, byDepsResult, depProject, license );
                            }
                            else
                            {
                                handleResult( licenseUrl, cachedResult, depProject, license );
                            }
                        }
                        return;
                    }
                    else
                    {
                        /* No cache entry for the current URL */
                        final FileNameEntry fileNameEntry =
                            getLicenseFileName( depProject, licenseUrl, license.getName(), license.getFile() );

                        final File licenseOutputFile = fileNameEntry.getFile();
                        if ( matchingUrlsOnly == fileNameEntry.isPreferred() )
                        {
                            if ( !licenseOutputFile.exists() || forceDownload )
                            {
                                LicenseDownloadResult result =
                                    licenseDownloader.downloadLicense( licenseUrl, proxyLoginPasswordEncoded,
                                                                       fileNameEntry, getLog() );
                                if ( !organizeLicensesByDependencies && result.isSuccess() )
                                {
                                    /* check if we can re-use an existing file that has the same content */
                                    final String name = preferredFileNames.getFileNameBySha1( result.getSha1() );
                                    if ( name != null )
                                    {
                                        final File oldFile = result.getFile();
                                        if ( !oldFile.getName().equals( name ) )
                                        {
                                            final File newFile = new File( licensesOutputDirectory, name );
                                            result = result.withFile( newFile );
                                            oldFile.delete();
                                        }
                                    }
                                }
                                handleResult( licenseUrl, result, depProject, license );
                                cache.put( licenseUrl, result );
                            }
                            else if ( licenseOutputFile.exists() )
                            {
                                final LicenseDownloadResult result =
                                    LicenseDownloadResult.success( licenseOutputFile,
                                                                   FileUtil.sha1( licenseOutputFile.toPath() ),
                                                                   fileNameEntry.isPreferred() );
                                handleResult( licenseUrl, result, depProject, license );
                                cache.put( licenseUrl, result );
                            }
                        }
                    }
                }
                catch ( URISyntaxException e )
                {
                    handleError( depProject, "POM for dependency " + depProject.toString()
                        + " has an invalid license URL: " + licenseUrl );
                    getLog().debug( e );
                }
                catch ( FileNotFoundException e )
                {
                    handleError( depProject, "POM for dependency " + depProject.toString()
                        + " has a license URL that returns file not found: " + licenseUrl );
                    getLog().debug( e );
                }
                catch ( IOException e )
                {
                    handleError( depProject, "Unable to retrieve license from URL '" + licenseUrl + "' for dependency '"
                        + depProject.toString() + "': " + e.getMessage() );
                    getLog().debug( e );
                }
            }
            licenseIndex++;
        }

    }

    private void handleResult( String licenseUrl, LicenseDownloadResult result, ProjectLicenseInfo depProject,
                               ProjectLicense license )
        throws MojoFailureException
    {
        if ( result.isSuccess() )
        {
            license.setFile( result.getFile().getName() );
        }
        else
        {
            handleError( depProject, result.getErrorMessage() );
        }
    }

    private void handleError( ProjectLicenseInfo depProject, String msg ) throws MojoFailureException
    {
        switch ( errorRemedy )
        {
            case ignore:
                /* do nothing */
                break;
            case warn:
                getLog().warn( msg );
                break;
            case failFast:
                throw new MojoFailureException( msg );
            case xmlOutput:
                getLog().debug( msg );
                depProject.addDownloaderMessage( msg );
                break;
            default:
                throw new IllegalStateException( "Unexpected value of " + ErrorRemedy.class.getName() + ": "
                    + errorRemedy );
        }
        downloadErrorCount++;
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

    /**
     * What to do in case of a license download error.
     *
     * @since 1.18
     */
    public enum ErrorRemedy
    {
        /** All errors are ignored */
        ignore,
        /** All errors are output to the log as warnings */
        warn,
        /**
         * The first encountered error is logged and a {@link MojoFailureException} is thrown
         */
        failFast,
        /**
         * Error messages are added as {@code <downloaderMessages>} to
         * {@link AbstractDownloadLicensesMojo#licensesErrorsFile}; in case there are error messages, the build will
         * fail after processing all dependencies.
         */
        xmlOutput
    }
}
