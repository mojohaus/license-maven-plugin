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
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.mojo.license.utils.StringToList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Abstract mojo for all third-party mojos.
 *
 * @author tchemit <chemit@codelutin.com>
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
     * @parameter property="license.outputDirectory" default-value="${project.build.directory}/generated-sources/license"
     * @required
     * @since 1.0
     */
    @Parameter( property = "license.outputDirectory",
                defaultValue = "${project.build.directory}/generated-sources/license", required = true )
    private File outputDirectory;

    /**
     * File where to write the third-party file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.thirdPartyFilename", defaultValue = "THIRD-PARTY.txt", required = true )
    private String thirdPartyFilename;

    /**
     * A flag to use the missing licenses file to consolidate the THID-PARTY file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.useMissingFile", defaultValue = "false" )
    private boolean useMissingFile;

    /**
     * The file to write with a license information template for dependencies with unknown license.
     *
     * @since 1.0
     */
    @Parameter( property = "license.missingFile", defaultValue = "src/license/THIRD-PARTY.properties" )
    private File missingFile;

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
     * @since 1.0
     */
    @Parameter
    private List<String> licenseMerges;

    /**
     * To specify some licenses to include.
     * <p/>
     * If this parameter is filled and a license is not in this {@code whitelist} then build will failed when property
     * {@link #failIfWarning} is <tt>true</tt>.
     * <p/>
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
    @Parameter( property = "license.includedLicenses", defaultValue = "" )
    private IncludedLicenses includedLicenses;

    /**
     * To specify some licenses to exclude.
     * <p/>
     * If a such license is found then build will failed when property
     * {@link #failIfWarning} is <tt>true</tt>.
     * <p/>
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
    @Parameter( property = "license.excludedLicenses", defaultValue = "" )
    private ExcludedLicenses excludedLicenses;

    /**
     * The path of the bundled third party file to produce when
     * {@link #generateBundle} is on.
     * <p/>
     * <b>Note:</b> This option is not available for {@code pom} module types.
     *
     * @since 1.0
     */
    @Parameter( property = "license.bundleThirdPartyPath",
                defaultValue = "META-INF/${project.artifactId}-THIRD-PARTY.txt" )
    private String bundleThirdPartyPath;

    /**
     * A flag to copy a bundled version of the third-party file. This is useful
     * to avoid for a final application collision name of third party file.
     * <p/>
     * The file will be copied at the {@link #bundleThirdPartyPath} location.
     *
     * @since 1.0
     */
    @Parameter( property = "license.generateBundle", defaultValue = "false" )
    private boolean generateBundle;

    /**
     * To force generation of the third-party file even if everything is up to date.
     *
     * @since 1.0
     */
    @Parameter( property = "license.force", defaultValue = "false" )
    private boolean force;

    /**
     * A flag to fail the build if at least one dependency was detected without a license.
     *
     * @since 1.0
     */
    @Parameter( property = "license.failIfWarning", defaultValue = "false" )
    private boolean failIfWarning;

    /**
     * A flag to sort artifact by name in the generated third-party file.
     *
     * If not then artifacts are sorted by <pre>groupId:artifactId:version</pre>t
     *
     * @since 1.6
     */
    @Parameter( property = "license.sortArtifactByName", defaultValue = "false" )
    private boolean sortArtifactByName;

    /**
     * Template used to build the third-party file.
     * <p/>
     * (This template use freemarker).
     *
     * @since 1.1
     */
    @Parameter( property = "license.fileTemplate", defaultValue = "/org/codehaus/mojo/license/third-party-file.ftl" )
    private String fileTemplate;

    /**
     * Local Repository.
     *
     * @since 1.0.0
     */
    @Parameter( property = "localRepository", required = true, readonly = true )
    private ArtifactRepository localRepository;

    /**
     * Remote repositories used for the project.
     *
     * @since 1.0.0
     */
    @Parameter( property = "project.remoteArtifactRepositories", required = true, readonly = true )
    private List<ArtifactRepository> remoteRepositories;

    /**
     * The set of dependencies for the current project, used to locate license databases.
     */
    @Parameter( property = "project.artifacts", required = true, readonly = true )
    private Set<Artifact> dependencies;

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
    private DependenciesTool dependenciesTool;

    // ----------------------------------------------------------------------
    // Private fields
    // ----------------------------------------------------------------------

    /**
     * Third-party helper (high level tool with common code for mojo and report).
     */
    private ThirdPartyHelper helper;

    private SortedMap<String, MavenProject> projectDependencies;

    private LicenseMap licenseMap;

    private SortedSet<MavenProject> unsafeDependencies;

    private File thirdPartyFile;

    private SortedProperties unsafeMappings;

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
     * <p/>
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

        thirdPartyFile = new File( getOutputDirectory(), thirdPartyFilename );

        long buildTimestamp = getBuildTimestamp();

        if ( isVerbose() )
        {
            log.info( "Build start   at : " + buildTimestamp );
            log.info( "third-party file : " + thirdPartyFile.lastModified() );
        }

        doGenerate = isForce() || !thirdPartyFile.exists() || buildTimestamp > thirdPartyFile.lastModified();

        if ( generateBundle )
        {

            File bundleFile = FileUtil.getFile( getOutputDirectory(), bundleThirdPartyPath );

            if ( isVerbose() )
            {
                log.info( "bundle third-party file : " + bundleFile.lastModified() );
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
    }

    // ----------------------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------------------

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean isFailIfWarning()
    {
        return failIfWarning;
    }

    public SortedMap<String, MavenProject> getProjectDependencies()
    {
        return projectDependencies;
    }

    public SortedSet<MavenProject> getUnsafeDependencies()
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

    public File getMissingFile()
    {
        return missingFile;
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

    public boolean isDoGenerateBundle()
    {
        return doGenerateBundle;
    }

    /**
     * @return list of license to exclude.
     */
    public List<String> getExcludedLicenses()
    {
        return excludedLicenses.getData();
    }

    /**
     * @return list of license to include.
     */
    public List<String> getIncludedLicenses()
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

    protected boolean checkUnsafeDependencies()
    {
        SortedSet<MavenProject> unsafeDeps = getUnsafeDependencies();
        boolean unsafe = !CollectionUtils.isEmpty( unsafeDeps );
        if ( unsafe )
        {
            Log log = getLog();
            log.warn( "There is " + unsafeDeps.size() + " dependencies with no license :" );
            for ( MavenProject dep : unsafeDeps )
            {

                // no license found for the dependency
                log.warn( " - " + MojoHelper.getArtifactId( dep.getArtifact() ) );
            }
        }
        return unsafe;
    }

    protected boolean checkForbiddenLicenses()
    {
        List<String> whiteLicenses = getIncludedLicenses();
        List<String> blackLicenses = getExcludedLicenses();
        Set<String> unsafeLicenses = new HashSet<String>();
        if ( CollectionUtils.isNotEmpty( blackLicenses ) )
        {
            Set<String> licenses = getLicenseMap().keySet();
            getLog().info( "Excluded licenses (blacklist): " + blackLicenses );

            for ( String excludeLicense : blackLicenses )
            {
                if ( licenses.contains( excludeLicense ) )
                {
                    //bad license found
                    unsafeLicenses.add( excludeLicense );
                }
            }
        }

        if ( CollectionUtils.isNotEmpty( whiteLicenses ) )
        {
            Set<String> licenses = getLicenseMap().keySet();
            getLog().info( "Included licenses (whitelist): " + whiteLicenses );

            for ( String license : licenses )
            {
                if ( !whiteLicenses.contains( license ) )
                {
                    //bad license found
                    unsafeLicenses.add( license );
                }
            }
        }

        boolean safe = CollectionUtils.isEmpty( unsafeLicenses );

        if ( !safe )
        {
            Log log = getLog();
            log.warn( "There is " + unsafeLicenses.size() + " forbidden licenses used:" );
            for ( String unsafeLicense : unsafeLicenses )
            {

                SortedSet<MavenProject> deps = getLicenseMap().get( unsafeLicense );
                StringBuilder sb = new StringBuilder();
                sb.append( "License " ).append( unsafeLicense ).append( "used by " ).append( deps.size() ).append(
                    " dependencies:" );
                for ( MavenProject dep : deps )
                {
                    sb.append( "\n -" ).append( MojoHelper.getArtifactName( dep ) );
                }

                log.warn( sb.toString() );
            }
        }
        return safe;
    }

    protected void writeThirdPartyFile()
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

            thirdPartyTool.writeBundleThirdPartyFile( thirdPartyFile, outputDirectory, bundleThirdPartyPath );
        }
    }

    /**
     * Class to fill the {@link #includedLicenses} parameter, from a simple string to split, or a list of string.
     * <p/>
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
        public IncludedLicenses( String data )
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
     * <p/>
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
        public ExcludedLicenses( String data )
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