/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

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
    implements MavenProjectDependenciesLoader
{

    /**
     * Local Repository.
     *
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     * @since 1.0.0
     */
    protected ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     * @since 1.0.0
     */
    protected List<?> remoteRepositories;

    /**
     * Maven Project Builder component.
     *
     * @component
     * @required
     * @readonly
     * @since 1.0.0
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    private boolean doGenerateMissing;

    @Override
    protected boolean checkPackaging()
    {
        return rejectPackaging( "pom" );
    }

    @Override
    protected SortedMap<String, MavenProject> loadDependencies()
    {
        return ArtifactHelper.loadProjectDependencies( this, getLog(), getArtifactCache() );
    }

    @Override
    protected SortedProperties createUnsafeMapping()
        throws ProjectBuildingException, IOException
    {

        SortedProperties unsafeMappings =
            getLicenseMap().loadUnsafeMapping( getArtifactCache(), getEncoding(), getMissingFile() );

        SortedSet<MavenProject> unsafeDependencies = getUnsafeDependencies();

        if ( isVerbose() )
        {
            getLog().info( "found " + unsafeMappings.size() + " unsafe mappings" );
        }

        // compute if missing file should be (re)-generate
        boolean generateMissingfile = computeDoGenerateMissingFile( unsafeMappings, unsafeDependencies );

        setDoGenerateMissing( generateMissingfile );

        if ( generateMissingfile && isVerbose() )
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

            // the mapping fro missing file is not empty, regenerate it
            return !CollectionUtils.isEmpty( unsafeMappings.keySet() );
        }
        else
        {

            if ( !CollectionUtils.isEmpty( unsafeDependencies ) )
            {

                // there is some unsafe dependencies from the project, must
                // regenerate missing file
                return true;
            }

            // check if the missing file has changed
            SortedProperties oldUnsafeMappings = new SortedProperties( getEncoding() );
            oldUnsafeMappings.load( getMissingFile() );
            return !unsafeMappings.equals( oldUnsafeMappings );
        }
    }

    @Override
    protected boolean checkSkip()
    {
        if ( !isDoGenerate() && !isDoGenerateBundle() && !isDoGenerateMissing() )
        {

            getLog().info( "All files are up to date, skip goal execution." );
            return false;
        }
        return true;
    }

    @Override
    protected void doAction()
        throws Exception
    {

        boolean unsafe = checkUnsafeDependencies();

        writeThirdPartyFile();

        if ( isDoGenerateMissing() )
        {

            writeMissingFile();
        }

        if ( unsafe && isFailIfWarning() )
        {
            throw new MojoFailureException(
                "There is some dependencies with no license, please fill the file " + getMissingFile() );
        }

        addResourceDir( getOutputDirectory(), "**/*.txt" );
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
            licenses.remove( LicenseMap.getUnknownLicenseMessage() );
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

    public boolean isDoGenerateMissing()
    {
        return doGenerateMissing;
    }

    public void setDoGenerateMissing( boolean doGenerateMissing )
    {
        this.doGenerateMissing = doGenerateMissing;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public List<?> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public MavenProjectBuilder getMavenProjectBuilder()
    {
        return mavenProjectBuilder;
    }

    public boolean isIncludeTransitiveDependencies()
    {
        return includeTransitiveDependencies;
    }

    public List<String> getExcludeScopes()
    {
        return Arrays.asList( Artifact.SCOPE_SYSTEM );
    }
}
