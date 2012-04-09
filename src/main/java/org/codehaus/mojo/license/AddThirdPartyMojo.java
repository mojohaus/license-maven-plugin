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

package org.codehaus.mojo.license;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.LicenseMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Goal to generate the third-party license file.
 * <p/>
 * This file contains a list of the dependencies and their licenses.  Each dependency and it's
 * license is displayed on a single line in the format <br/>
 * <pre>
 *   (&lt;license-name&gt;) &lt;project-name&gt; &lt;groupId&gt;:&lt;artifactId&gt;:&lt;version&gt; - &lt;project-url&gt;
 * </pre>
 * It will also copy it in the class-path (says add the generated directory as
 * a resource of the build).
 *
 * @author tchemit <chemit@codelutin.com>
 * @goal add-third-party
 * @phase generate-resources
 * @requiresDependencyResolution test
 * @requiresProject true
 * @since 1.0
 */
public class AddThirdPartyMojo
    extends AbstractAddThirdPartyMojo
    implements MavenProjectDependenciesConfigurator
{

    /**
     * Deploy the third party missing file in maven repository.
     *
     * @parameter expression="${license.deployMissingFile}"  default-value="true"
     * @since 1.0
     */
    private boolean deployMissingFile;

    /**
     * Load from repositories third party missing files.
     *
     * @parameter expression="${license.useRepositoryMissingFiles}"  default-value="true"
     * @since 1.0
     */
    private boolean useRepositoryMissingFiles;

    /**
     * To execute or not this mojo if project packaging is pom.
     * <p/>
     * <strong>Note:</strong> The default value is {@code false}.
     *
     * @parameter expression="${license.acceptPomPackaging}"  default-value="false"
     * @since 1.1
     */
    private boolean acceptPomPackaging;

    /**
     * A filter to exclude some scopes.
     *
     * @parameter expression="${license.excludedScopes}" default-value="system"
     * @since 1.1
     */
    private String excludedScopes;

    /**
     * A filter to include only some scopes, if let empty then all scopes will be used (no filter).
     *
     * @parameter expression="${license.includedScopes}" default-value=""
     * @since 1.1
     */
    private String includedScopes;

    /**
     * A filter to exclude some GroupIds
     *
     * @parameter expression="${license.excludedGroups}" default-value=""
     * @since 1.1
     */
    private String excludedGroups;

    /**
     * A filter to include only some GroupIds
     *
     * @parameter expression="${license.includedGroups}" default-value=""
     * @since 1.1
     */
    private String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     *
     * @parameter expression="${license.excludedArtifacts}" default-value=""
     * @since 1.1
     */
    private String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     *
     * @parameter expression="${license.includedArtifacts}" default-value=""
     * @since 1.1
     */
    private String includedArtifacts;

    /**
     * Include transitive dependencies when downloading license files.
     *
     * @parameter default-value="true"
     * @since 1.1
     */
    private boolean includeTransitiveDependencies;

    private boolean doGenerateMissing;

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
    protected SortedMap<String, MavenProject> loadDependencies()
    {
        return getHelper().loadDependencies( this );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedProperties createUnsafeMapping()
        throws ProjectBuildingException, IOException, ThirdPartyToolException
    {

        SortedSet<MavenProject> unsafeDependencies = getUnsafeDependencies();

        SortedProperties unsafeMappings =
            getHelper().createUnsafeMapping( getLicenseMap(), getMissingFile(), useRepositoryMissingFiles,
                                             unsafeDependencies, getProjectDependencies().values() );
        if ( isVerbose() )
        {
            getLog().info( "found " + unsafeMappings.size() + " unsafe mappings" );
        }

        // compute if missing file should be (re)-generate
        doGenerateMissing = computeDoGenerateMissingFile( unsafeMappings, unsafeDependencies );

        if ( doGenerateMissing && isVerbose() )
        {
            StringBuilder sb = new StringBuilder();
            sb.append( "Will use from missing file " );
            sb.append( unsafeMappings.size() );
            sb.append( " dependencies :" );
            for ( Map.Entry<Object, Object> entry : unsafeMappings.entrySet() )
            {
                String id = (String) entry.getKey();
                String license = (String) entry.getValue();
                sb.append( "\n - " ).append( id ).append( " - " ).append( license );
            }
            getLog().info( sb.toString() );
        }
        else
        {
            if ( isUseMissingFile() && !unsafeMappings.isEmpty() )
            {
                getLog().info( "Missing file " + getMissingFile() + " is up-to-date." );
            }
        }
        return unsafeMappings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkSkip()
    {
        if ( !isDoGenerate() && !isDoGenerateBundle() && !doGenerateMissing )
        {

            getLog().info( "All files are up to date, skip goal execution." );
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

        boolean unsafe = checkUnsafeDependencies();

        writeThirdPartyFile();

        if ( doGenerateMissing )
        {

            writeMissingFile();
        }

        if ( unsafe && isFailIfWarning() )
        {
            throw new MojoFailureException(
                "There is some dependencies with no license, please fill the file " + getMissingFile() );
        }

        if ( !unsafe && isUseMissingFile() && MapUtils.isEmpty( getUnsafeMappings() ) && getMissingFile().exists() )
        {

            // there is no missing dependencies, but still a missing file, delete it
            getLog().info( "There is no dependency to put in missing file, delete it at " + getMissingFile() );
            FileUtil.deleteFile( getMissingFile() );
        }

        if ( !unsafe && deployMissingFile && MapUtils.isNotEmpty( getUnsafeMappings() ) )
        {

            // can deploy missing file
            File file = getMissingFile();

            getLog().info( "Will deploy third party file from " + file );
            getHelper().attachThirdPartyDescriptor( file );
        }

        addResourceDir( getOutputDirectory(), "**/*.txt" );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getExcludedScopes()
    {
        String[] split = excludedScopes == null ? new String[0] : excludedScopes.split( "," );
        return Arrays.asList( split );
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getIncludedScopes()
    {
        String[] split = includedScopes == null ? new String[0] : includedScopes.split( "," );
        return Arrays.asList( split );
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

    /**
     * @param unsafeMappings     the unsafe mapping coming from the missing file
     * @param unsafeDependencies the unsafe dependencies from the project
     * @return {@code true} if missing ifle should be (re-)generated, {@code false} otherwise
     * @throws IOException if any IO problem
     * @since 1.0
     */
    protected boolean computeDoGenerateMissingFile( SortedProperties unsafeMappings,
                                                    SortedSet<MavenProject> unsafeDependencies )
        throws IOException
    {

        if ( !isUseMissingFile() )
        {

            // never use the missing file
            return false;
        }

        if ( isForce() )
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

        File missingFile = getMissingFile();

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

    protected void writeMissingFile()
        throws IOException
    {

        Log log = getLog();
        LicenseMap licenseMap = getLicenseMap();
        File file = getMissingFile();

        FileUtil.createDirectoryIfNecessary( file.getParentFile() );
        log.info( "Regenerate missing license file " + file );

        FileOutputStream writer = new FileOutputStream( file );
        try
        {
            StringBuilder sb = new StringBuilder( " Generated by " + getClass().getName() );
            List<String> licenses = new ArrayList<String>( licenseMap.keySet() );
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
            getUnsafeMappings().store( writer, sb.toString() );
        }
        finally
        {
            writer.close();
        }
    }
}
