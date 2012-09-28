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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.model.ProjectLicenseInfo;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.LicenseDownloader;
import org.codehaus.mojo.license.utils.LicenseSummaryReader;
import org.codehaus.mojo.license.utils.LicenseSummaryWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;

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

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * Keeps a collection of the URLs of the licenses that have been downlaoded. This helps the plugin to avoid
     * downloading the same license multiple times.
     */
    private Set<String> downloadedLicenseURLs = new HashSet<String>();

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
        initDirectories();

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
            dependencyProject.addLicense( (License) license );
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
        if ( project != null )
        {
            Properties projectProperties = project.getProperties();

            if ( projectProperties != null )
            {
                String proxyHostKey = "proxyHost";
                String proxyPortKey = "proxyPort";
                String nonProxyHostsKey = "nonProxyHosts";

                String proxyHost = projectProperties.getProperty( proxyHostKey, "" );
                String proxyPort = projectProperties.getProperty( proxyPortKey, "" );
                String nonProxyHosts = projectProperties.getProperty( nonProxyHostsKey, "" );

                System.setProperty( proxyHostKey, proxyHost );
                System.setProperty( proxyPortKey, proxyPort );
                System.setProperty( nonProxyHostsKey, nonProxyHosts );
            }
        }

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
                    LicenseDownloader.downloadLicense( license.getUrl(), licenseOutputFile );
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
