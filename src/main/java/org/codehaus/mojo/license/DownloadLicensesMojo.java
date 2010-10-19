package org.codehaus.mojo.license;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.DependencyProject;

/**
 * Maven goal for downloading the license files of all the current project's dependencies.
 * 
 * @phase generate-resources
 * @goal download-licenses
 * @requiresDependencyResolution test
 * @author Paul Gier
 * @version $Revision$
 */
public class DownloadLicensesMojo
    extends AbstractMojo
{

    /**
     * The Maven Project Object
     * 
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * Used to build a maven projects from artifacts in the remote repository.
     * 
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @readonly
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     */
    private org.apache.maven.artifact.repository.ArtifactRepository localRepository;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     */
    private java.util.List remoteRepositories;

    /**
     * File contains a mapping between each dependency and it's license information.
     * 
     * @parameter default-value="${project.build.directory}/licenses.xml"
     * @since 2.0.0
     */
    private File licensesSummaryFile;

    /**
     * The directory to which the dependency licenses should be written.
     * 
     * @parameter default-value="${project.build.directory}/licenses"
     */
    private File licensesOutputDirectory;

    /**
     * File contains a mapping between each dependency and it's license information.
     * 
     * @parameter default-value="${project.build.directory}/licenses.xml"
     */
    private File licensesSummaryOutputFile;

    /**
     * Don't show warnings about bad or missing license files.
     * 
     * @parameter default-value="false"
     */
    private boolean quiet;

    /**
     * Include transitive dependencies when downloading license files.
     * 
     * @parameter default-value="true"
     * @since 2.0.0
     */
    private boolean includeTransitiveDependencies;

    /**
     * Main Maven plugin execution
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( !licensesOutputDirectory.exists() )
        {
            licensesOutputDirectory.mkdirs();
        }

        if ( !licensesSummaryOutputFile.getParentFile().exists() )
        {
            licensesSummaryOutputFile.getParentFile().mkdirs();
        }

        // Load pre-configured license information
        HashMap<String, DependencyProject> configuredDepLicensesMap = new HashMap<String, DependencyProject>();
        if ( licensesSummaryFile.exists() )
        {
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream( licensesSummaryFile );
                List<DependencyProject> licensesList = LicenseSummaryReader.parseLicenseSummary( fis );
                for ( DependencyProject dep : licensesList )
                {
                    configuredDepLicensesMap.put( dep.toString(), dep );
                }
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to parse license summary file.", e );
            }
            finally
            {
                FileUtil.tryClose( fis );
            }
        }

        // Load license information from previous build so we don't have to download the licenses again
        HashMap<String, DependencyProject> cachedDepLicensesMap = new HashMap<String, DependencyProject>();
        if ( licensesSummaryOutputFile.exists() )
        {
            FileInputStream fis = null;
            try
            {
                fis = new FileInputStream( licensesSummaryOutputFile );
                List<DependencyProject> licensesList = LicenseSummaryReader.parseLicenseSummary( fis );
                for ( DependencyProject dep : licensesList )
                {
                    cachedDepLicensesMap.put( dep.toString(), dep );
                }
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to parse license summary file.", e );
            }
            finally
            {
                FileUtil.tryClose( fis );
            }
        }

        // Get the list of build dependencies
        Set<Artifact> depArtifacts = null;

        if ( includeTransitiveDependencies )
        {
            depArtifacts = project.getArtifacts();
        }
        else
        {
            depArtifacts = project.getDependencyArtifacts();
        }

        // The resulting list of licenses after dependency resolution
        List<DependencyProject> depProjectLicenses = new ArrayList<DependencyProject>();

        for ( Artifact artifact : depArtifacts )
        {
            MavenProject depMavenProject = null;
            try
            {
                depMavenProject = projectBuilder.buildFromRepository( artifact, remoteRepositories, localRepository );
            }
            catch ( ProjectBuildingException e )
            {
                getLog().warn( "Unable to build project: " + artifact.getDependencyConflictId() );
                getLog().warn( e );
            }

            DependencyProject depProject = createDependencyProject( depMavenProject );

            getLog().debug( "Downloading licenses..." );
            if ( configuredDepLicensesMap.containsKey( depProject.getId() ) )
            {
                List<License> licenses = configuredDepLicensesMap.get( depProject.getId() ).getLicenses();
                depProject.setLicenses( licenses );
            }

            // Don't try to download the license again if we don't need to
            if ( !cachedDepLicensesMap.containsKey( depProject.getId() ) )
            {
                this.downloadLicenses( depProject );
            }
            depProjectLicenses.add( depProject );

        }

        try
        {
            LicenseSummaryWriter.writeLicenseSummary( depProjectLicenses, licensesSummaryOutputFile );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Unable to write license summary file.", e );
        }

    }

    /**
     * Create a simple DependencyProject object containing the GAV and license info from the MavenProject
     * 
     * @param project
     * @return
     */
    public DependencyProject createDependencyProject( MavenProject project )
    {
        DependencyProject dependencyProject =
            new DependencyProject( project.getGroupId(), project.getArtifactId(), project.getVersion() );
        List<License> licenses = project.getLicenses();
        for ( License license : licenses )
        {
            dependencyProject.addLicense( license );
        }
        return dependencyProject;
    }

    /**
     * Tries to determine what the name of the downloaded license file should be based on the information in the license
     * object.
     * 
     * @param license
     * @return
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
        final String DEFAULT_EXTENSION = ".txt";
        int extensionIndex = licenseFileName.lastIndexOf( "." );
        if ( extensionIndex == -1 || extensionIndex > ( licenseFileName.length() - 3 ) )
        {
            // This means it isn't a valid file extension, so append the default
            licenseFileName = licenseFileName + DEFAULT_EXTENSION;
        }

        return licenseFileName;
    }

    /**
     * Download the licenses associated with this project
     * 
     * @param depProject The project which generated the dependency
     */
    private void downloadLicenses( DependencyProject depProject )
    {
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
                String licenseFileName = this.getLicenseFileName( license );

                File licenseOutputFile = new File( licensesOutputDirectory, licenseFileName );
                if ( licenseOutputFile.exists() )
                {
                    continue;
                }

                LicenseDownloader.downloadLicense( license.getUrl(), licenseOutputFile );

            }
            catch ( MalformedURLException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString() + " has an invalid license URL: "
                                       + license.getUrl() );
                }
            }
            catch ( FileNotFoundException e )
            {
                if ( !quiet )
                {
                    getLog().warn( "POM for dependency " + depProject.toString()
                                       + " has a license URL that returns file not found: " + license.getUrl() );
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
