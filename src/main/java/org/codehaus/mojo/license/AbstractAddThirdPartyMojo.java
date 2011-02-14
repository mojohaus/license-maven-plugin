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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Abstract mojo for all third-party mojos.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public abstract class AbstractAddThirdPartyMojo
    extends AbstractLicenseMojo
{

    /**
     * Directory where to generate files.
     *
     * @parameter expression="${license.outputDirectory}" default-value="${project.build.directory}/generated-sources/license"
     * @required
     * @since 1.0
     */
    protected File outputDirectory;

    /**
     * File where to wirte the third-party file.
     *
     * @parameter expression="${license.thirdPartyFilename}" default-value="THIRD-PARTY.txt"
     * @required
     * @since 1.0
     */
    protected String thirdPartyFilename;

    /**
     * A flag to use the missing licenses file to consolidate the THID-PARTY file.
     *
     * @parameter expression="${license.useMissingFile}"  default-value="false"
     * @since 1.0
     */
    protected boolean useMissingFile;

    /**
     * The file where to fill the license for dependencies with unknwon license.
     *
     * @parameter expression="${license.missingFile}"  default-value="src/license/THIRD-PARTY.properties"
     * @since 1.0
     */
    protected File missingFile;

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
     * @since 1.0
     */
    protected List<String> licenseMerges;

    /**
     * The path of the bundled third party file to produce when
     * {@link #generateBundle} is on.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @parameter expression="${license.bundleThirdPartyPath}"  default-value="META-INF/${project.artifactId}-THIRD-PARTY.txt"
     * @since 1.0
     */
    protected String bundleThirdPartyPath;

    /**
     * A flag to copy a bundled version of the third-party file. This is usefull
     * to avoid for a final application collision name of third party file.
     * <p/>
     * The file will be copied at the {@link #bundleThirdPartyPath} location.
     *
     * @parameter expression="${license.generateBundle}"  default-value="false"
     * @since 1.0
     */
    protected boolean generateBundle;

    /**
     * To force generation of the third-party file even if every thing is up to date.
     *
     * @parameter expression="${license.force}"  default-value="false"
     * @since 1.0
     */
    protected boolean force;

    /**
     * A flag to fail the build if at least one dependency was detected without a license.
     *
     * @parameter expression="${license.failIfWarning}"  default-value="false"
     * @since 1.0
     */
    protected boolean failIfWarning;

    /**
     * A flag to change the grouping of the generated THIRD-PARTY file.
     * <p/>
     * By default, group by dependecies.
     * <p/>
     * If sets to {@code true}, the it will group by license type.
     *
     * @parameter expression="${license.groupByLicense}"  default-value="false"
     * @since 1.0
     */
    protected boolean groupByLicense;

    /**
     * A filter to exclude some GroupIds
     *
     * @parameter expression="${license.excludedGroups}" default-value=""
     * @since 1.0
     */
    protected String excludedGroups;

    /**
     * A filter to include only some GroupIds
     *
     * @parameter expression="${license.includedGroups}" default-value=""
     * @since 1.0
     */
    protected String includedGroups;

    /**
     * A filter to exclude some ArtifactsIds
     *
     * @parameter expression="${license.excludedArtifacts}" default-value=""
     * @since 1.0
     */
    protected String excludedArtifacts;

    /**
     * A filter to include only some ArtifactsIds
     *
     * @parameter expression="${license.includedArtifacts}" default-value=""
     * @since 1.0
     */
    protected String includedArtifacts;

    /**
     * Include transitive dependencies when downloading license files.
     *
     * @parameter default-value="true"
     * @since 1.0
     */
    protected boolean includeTransitiveDependencies;

    private LicenseMap licenseMap;

    private SortedSet<MavenProject> unsafeDependencies;

    private File thirdPartyFile;

    private SortedProperties unsafeMappings;

    private boolean doGenerate;

    private boolean doGenerateBundle;

    public static final String NO_DEPENDENCIES_MESSAGE = "the project has no dependencies.";

    private static SortedMap<String, MavenProject> artifactCache;

    public static SortedMap<String, MavenProject> getArtifactCache()
    {
        if ( artifactCache == null )
        {
            artifactCache = new TreeMap<String, MavenProject>();
        }
        return artifactCache;
    }

    protected abstract SortedMap<String, MavenProject> loadDependencies();

    protected abstract SortedProperties createUnsafeMapping()
        throws ProjectBuildingException, IOException;

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

        File file = new File( getOutputDirectory(), getThirdPartyFilename() );

        setThirdPartyFile( file );

        long buildTimestamp = getBuildTimestamp();

        if ( isVerbose() )
        {
            log.info( "Build start   at : " + buildTimestamp );
            log.info( "third-party file : " + file.lastModified() );
        }

        setDoGenerate( isForce() || !file.exists() || buildTimestamp > file.lastModified() );

        if ( isGenerateBundle() )
        {

            File bundleFile = FileUtil.getFile( getOutputDirectory(), getBundleThirdPartyPath() );

            if ( isVerbose() )
            {
                log.info( "bundle third-party file : " + bundleFile.lastModified() );
            }
            setDoGenerateBundle( isForce() || !bundleFile.exists() || buildTimestamp > bundleFile.lastModified() );
        }
        else
        {

            // not generating bundled file
            setDoGenerateBundle( false );
        }

        SortedMap<String, MavenProject> projectDependencies = loadDependencies();

        licenseMap = createLicenseMap( projectDependencies );

        unsafeDependencies = licenseMap.getUnsafeDependencies();

        if ( !CollectionUtils.isEmpty( unsafeDependencies ) && isUseMissingFile() && isDoGenerate() )
        {

            // load unsafeMapping
            unsafeMappings = createUnsafeMapping();
        }

        if ( !CollectionUtils.isEmpty( licenseMerges ) )
        {

            // merge licenses in license map

            for ( String merge : licenseMerges )
            {
                merge = merge.trim();
                if ( isVerbose() )
                {
                    getLog().info( "Will merge [" + merge + "]" );
                }
                String[] split = merge.split( "\\|" );

                licenseMap.mergeLicenses( split );
            }
        }
    }

    protected LicenseMap createLicenseMap( SortedMap<String, MavenProject> dependencies )
    {

        LicenseMap licenseMap = new LicenseMap();
        licenseMap.setLog( getLog() );

        for ( MavenProject project : dependencies.values() )
        {
            licenseMap.addLicense( project, project.getLicenses() );
        }
        return licenseMap;
    }

    protected boolean checkUnsafeDependencies()
    {
        SortedSet<MavenProject> unsafeDependencies = getUnsafeDependencies();
        boolean unsafe = !CollectionUtils.isEmpty( unsafeDependencies );
        if ( unsafe )
        {
            Log log = getLog();
            log.warn( "There is " + unsafeDependencies.size() + " dependencies with no license :" );
            for ( MavenProject dep : unsafeDependencies )
            {

                // no license found for the dependency
                log.warn( " - " + ArtifactHelper.getArtifactId( dep.getArtifact() ) );
            }
        }
        return unsafe;
    }

    protected void writeThirdPartyFile()
        throws IOException
    {

        Log log = getLog();
        LicenseMap licenseMap = getLicenseMap();
        File target = getThirdPartyFile();

        if ( isDoGenerate() )
        {
            StringBuilder sb = new StringBuilder();
            if ( licenseMap.isEmpty() )
            {
                sb.append( NO_DEPENDENCIES_MESSAGE );
            }
            else
            {
                if ( isGroupByLicense() )
                {

                    // group by license
                    sb.append( "List of third-party dependencies grouped by " + "their license type." );
                    for ( String licenseName : licenseMap.keySet() )
                    {
                        SortedSet<MavenProject> projects = licenseMap.get( licenseName );
                        sb.append( "\n\n" ).append( licenseName ).append( " : " );

                        for ( MavenProject mavenProject : projects )
                        {
                            String s = ArtifactHelper.getArtifactName( mavenProject );
                            sb.append( "\n  * " ).append( s );
                        }
                    }

                }
                else
                {

                    // group by dependencies
                    SortedMap<MavenProject, String[]> map = licenseMap.toDependencyMap();

                    sb.append( "List of " ).append( map.size() ).append( " third-party dependencies.\n" );

                    List<String> lines = new ArrayList<String>();

                    for ( Map.Entry<MavenProject, String[]> entry : map.entrySet() )
                    {
                        String artifact = ArtifactHelper.getArtifactName( entry.getKey() );
                        StringBuilder buffer = new StringBuilder();
                        for ( String license : entry.getValue() )
                        {
                            buffer.append( " (" ).append( license ).append( ")" );
                        }
                        String licenses = buffer.toString();
                        String line = licenses + " " + artifact;
                        lines.add( line );
                    }

                    Collections.sort( lines );
                    for ( String line : lines )
                    {
                        sb.append( '\n' ).append( line );
                    }
                    lines.clear();
                }
            }
            String content = sb.toString();

            log.info( "Writing third-party file to " + target );
            if ( isVerbose() )
            {
                log.info( content );
            }

            FileUtil.writeString( target, content, getEncoding() );
        }

        if ( isDoGenerateBundle() )
        {

            // creates the bundled license file
            File bundleTarget = FileUtil.getFile( getOutputDirectory(), getBundleThirdPartyPath() );
            log.info( "Writing bundled third-party file to " + bundleTarget );
            FileUtil.copyFile( target, bundleTarget );
        }
    }

    public boolean isGroupByLicense()
    {
        return groupByLicense;
    }

    public void setGroupByLicense( boolean groupByLicense )
    {
        this.groupByLicense = groupByLicense;
    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public String getThirdPartyFilename()
    {
        return thirdPartyFilename;
    }

    public String getBundleThirdPartyPath()
    {
        return bundleThirdPartyPath;
    }

    public boolean isGenerateBundle()
    {
        return generateBundle;
    }

    public boolean isFailIfWarning()
    {
        return failIfWarning;
    }

    public SortedSet<MavenProject> getUnsafeDependencies()
    {
        return unsafeDependencies;
    }

    public File getThirdPartyFile()
    {
        return thirdPartyFile;
    }

    public LicenseMap getLicenseMap()
    {
        return licenseMap;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setThirdPartyFilename( String thirdPartyFilename )
    {
        this.thirdPartyFilename = thirdPartyFilename;
    }

    public void setBundleThirdPartyPath( String bundleThirdPartyPath )
    {
        this.bundleThirdPartyPath = bundleThirdPartyPath;
    }

    public void setGenerateBundle( boolean generateBundle )
    {
        this.generateBundle = generateBundle;
    }

    public void setThirdPartyFile( File thirdPartyFile )
    {
        this.thirdPartyFile = thirdPartyFile;
    }

    public boolean isUseMissingFile()
    {
        return useMissingFile;
    }

    public File getMissingFile()
    {
        return missingFile;
    }

    public void setUseMissingFile( boolean useMissingFile )
    {
        this.useMissingFile = useMissingFile;
    }

    public void setMissingFile( File missingFile )
    {
        this.missingFile = missingFile;
    }

    public void setFailIfWarning( boolean failIfWarning )
    {
        this.failIfWarning = failIfWarning;
    }

    public SortedProperties getUnsafeMappings()
    {
        return unsafeMappings;
    }

    public boolean isForce()
    {
        return force;
    }

    public boolean isDoGenerate()
    {
        return doGenerate;
    }

    public void setForce( boolean force )
    {
        this.force = force;
    }

    public void setDoGenerate( boolean doGenerate )
    {
        this.doGenerate = doGenerate;
    }

    public boolean isDoGenerateBundle()
    {
        return doGenerateBundle;
    }

    public void setDoGenerateBundle( boolean doGenerateBundle )
    {
        this.doGenerateBundle = doGenerateBundle;
    }

    public String getExcludedGroups()
    {
        return excludedGroups;
    }

    public void setExcludedGroups( String excludedGroups )
    {
        this.excludedGroups = excludedGroups;
    }

    public String getIncludedGroups()
    {
        return includedGroups;
    }

    public void setIncludedGroups( String includedGroups )
    {
        this.includedGroups = includedGroups;
    }

    public String getExcludedArtifacts()
    {
        return excludedArtifacts;
    }

    public void setExcludedArtifacts( String excludedArtifacts )
    {
        this.excludedArtifacts = excludedArtifacts;
    }

    public String getIncludedArtifacts()
    {
        return includedArtifacts;
    }

    public void setIncludedArtifacts( String includedArtifacts )
    {
        this.includedArtifacts = includedArtifacts;
    }
}