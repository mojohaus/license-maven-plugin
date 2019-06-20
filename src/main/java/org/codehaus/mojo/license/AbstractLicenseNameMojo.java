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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.api.FreeMarkerHelper;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.model.Copyright;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.model.LicenseStore;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Abstract mojo which using a {@link #licenseName} and owns a
 * {@link #licenseStore}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public abstract class AbstractLicenseNameMojo
        extends AbstractLicenseMojo
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractLicenseNameMojo.class );

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    protected static final String[] DEFAULT_INCLUDES = new String[]{"**/*"};

    protected static final String[] DEFAULT_EXCLUDES =
            new String[]{"**/*.zargo", "**/*.uml", "**/*.umldi", "**/*.xmi", /* modelisation */
                    "**/*.img", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.gif", /* images */
                    "**/*.zip", "**/*.jar", "**/*.war", "**/*.ear", "**/*.tgz", "**/*.gz"};

    protected static final String[] DEFAULT_ROOTS =
            new String[]{"src", "target/generated-sources", "target/processed-sources"};


    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * To specify an external extra licenses repository resolver (says the base
     * url of the repository where the {@code license.properties} is present).
     * <p>
     * <p>
     * <strong>Note: </strong>If you want to refer to a file within this project, start the expression with
     * <code>${project.baseUri}</code>
     * </p>
     *
     * @since 1.0
     */
    @Parameter( property = "license.licenseResolver" )
    private String licenseResolver;

    /**
     * A flag to keep a backup of every modified file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.keepBackup", defaultValue = "false" )
    private boolean keepBackup;

    /**
     * Name of the license to use in the project.
     *
     * @parameter property="license.licenseName"
     * @since 1.0
     */
    @Parameter( property = "license.licenseName" )
    private String licenseName;

    /**
     * Name of project (or module).
     * <p>
     * Will be used as description section of new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.projectName", defaultValue = "${project.name}", required = true )
    protected String projectName;

    /**
     * Name of project's organization.
     * <p>
     * Will be used as copyrigth's holder in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.organizationName", defaultValue = "${project.organization.name}", required = true )
    protected String organizationName;

    /**
     * Inception year of the project.
     * <p>
     * Will be used as first year of copyright section in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.inceptionYear", defaultValue = "${project.inceptionYear}", required = true )
    protected Integer inceptionYear;

    /**
     * optional copyright owners.
     * <p>
     * If not set, {@code organizationName} parameter will be used instead.
     *
     * @since 1.6
     */
    @Parameter( property = "license.copyrightOwners" )
    protected String copyrightOwners;

    /**
     * optional extra templates parameters.
     * <p>
     * If filled, they are available with prefix extra_ to process license content
     * (says the header and license content).
     *
     * @since 1.6
     */
    @Parameter
    protected Map<String, String> extraTemplateParameters;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * License loaded from the {@link #licenseName}.
     */
    private License license;

    /**
     * Store of available licenses.
     */
    private LicenseStore licenseStore;

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

        // init licenses store
        licenseStore = LicenseStore.createLicenseStore( licenseResolver );

        // check licenseName exists
        license = getLicense( licenseName, true );

        if ( StringUtils.isBlank( copyrightOwners ) )
        {
            copyrightOwners = organizationName;
        }
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    protected License getLicense( String licenseName, boolean checkIfExists )
    {
        if ( StringUtils.isEmpty( licenseName ) )
        {
            throw new IllegalArgumentException( "licenseName can not be null, nor empty" );
        }
        if ( licenseStore == null )
        {
            throw new IllegalStateException( "No license store initialized!" );
        }
        License result = licenseStore.getLicense( licenseName );
        if ( checkIfExists && result == null )
        {
            throw new IllegalArgumentException( "License named '" + licenseName + "' is unknown, use one of "
                    + Arrays.toString( licenseStore.getLicenseNames() ) );
        }
        return result;
    }

    protected boolean isKeepBackup()
    {
        return keepBackup;
    }

    protected String getLicenseName()
    {
        return licenseName;
    }

    protected License getLicense()
    {
        return license;
    }

    protected String getCopyrightOwners()
    {

        String holder = copyrightOwners;

        if ( holder == null )
        {
            holder = organizationName;
        }
        return holder;
    }

    protected String processLicenseContext( String licenseContent )
            throws IOException
    {
        FreeMarkerHelper licenseFreeMarkerHelper = FreeMarkerHelper.newHelperFromContent( licenseContent );

        Map<String, Object> templateParameters = new HashMap<>();

        addPropertiesToContext( System.getProperties(), "env_", templateParameters );
        addPropertiesToContext( getProject().getProperties(), "project_", templateParameters );

        templateParameters.put( "project", getProject().getModel() );

        templateParameters.put( "organizationName", organizationName );
        templateParameters.put( "inceptionYear", inceptionYear );
        templateParameters.put( "copyright", getCopyright( getCopyrightOwners() ) );
        templateParameters.put( "projectName", projectName );

        addPropertiesToContext( extraTemplateParameters, "extra_", templateParameters );
        return licenseFreeMarkerHelper.renderTemplate( FreeMarkerHelper.TEMPLATE, templateParameters );
    }

    Copyright getCopyright( String holder )
    {
        return Copyright.newCopyright( inceptionYear, holder );
    }

    /**
     * Collects some files.
     *
     * @param includes includes
     * @param excludes excludes
     * @param roots    root directories to treat
     * @param files    cache of file detected indexed by their root directory
     */
    void getFilesToTreatForRoots( String[] includes, String[] excludes, List<String> roots, Map<File, String[]> files )
    {

        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes( includes );
        if ( excludes != null )
        {
            ds.setExcludes( excludes );
        }
        for ( String src : roots )
        {

            File f = new File( src );
            if ( !f.exists() )
            {
                // do nothing on a non-existent
                continue;
            }

            LOG.debug( "discovering source files in {}", src );

            ds.setBasedir( f );
            // scan
            ds.scan();

            // get files
            String[] tmp = ds.getIncludedFiles();

            if ( tmp.length < 1 )
            {
                // no files found
                continue;
            }

            List<String> toTreate = new ArrayList<>();

            Collections.addAll( toTreate, tmp );

            if ( toTreate.isEmpty() )
            {
                // no file or all are up-to-date
                continue;
            }

            // register files
            files.put( f, toTreate.toArray( new String[toTreate.size()] ) );
        }
    }

    /**
     * Obtain for a given value, a trim version of it. If value is empty then use the given default value
     *
     * @param value        the value to trim (if not empty)
     * @param defaultValue the default value to use if value is empty
     * @return the trim value (or default value if value is empty)
     */
    String cleanHeaderConfiguration( String value, String defaultValue )
    {
        String resultHeader;
        if ( StringUtils.isEmpty( value ) )
        {

            // use default value
            resultHeader = defaultValue;
        }
        else
        {

            // clean all spaces of it
            resultHeader = value.replaceAll( "\\s", "" );
        }
        return resultHeader;
    }

    /**
     * Obtains the {@link FileHeaderTransformer} given his name.
     *
     * @param transformerName the name of the transformer to find
     * @return the transformer for the givne tramsformer name
     */
    FileHeaderTransformer getTransformer( Map<String, FileHeaderTransformer> transformers, String transformerName )
    {
        if ( StringUtils.isEmpty( transformerName ) )
        {
            throw new IllegalArgumentException( "transformerName can not be null, nor empty!" );
        }
        if ( transformers == null )
        {
            throw new IllegalStateException( "No transformers initialized!" );
        }
        FileHeaderTransformer transformer = transformers.get( transformerName );
        if ( transformer == null )
        {
            throw new IllegalArgumentException(
                    "transformerName " + transformerName + " is unknow, use one this one : " + transformers.keySet() );
        }
        return transformer;
    }

    /**
     * Reports into the given {@code buffer} stats for the given {@code state}.
     *
     * @param state  state of file to report
     * @param buffer where to report
     */
    void reportType( EnumMap<FileState, Set<File>> result, FileState state, StringBuilder buffer )
    {
        String operation = state.name();

        Set<File> set = getFiles( result, state );
        if ( set == null || set.isEmpty() )
        {
            if ( isVerbose() )
            {
                buffer.append( "\n * no header to " );
                buffer.append( operation );
                buffer.append( "." );
            }
            return;
        }
        buffer.append( "\n * " ).append( operation ).append( " header on " );
        buffer.append( set.size() );
        if ( set.size() == 1 )
        {
            buffer.append( " file." );
        }
        else
        {
            buffer.append( " files." );
        }
        if ( isVerbose() )
        {
            for ( File file : set )
            {
                buffer.append( "\n   - " ).append( file );
            }
        }
    }


    /**
     * Gets all files to process indexed by their comment style.
     *
     * @return for each comment style, list of files to process
     * @param extraFiles
     * @param roots
     * @param includes
     * @param excludes
     * @param extensionToCommentStyle
     * @param transformers
     */
    Map<String, List<File>> obtainFilesToProcessByCommentStyle( Map<String, String> extraFiles, String[] roots,
            String[] includes, String[] excludes, Map<String, String> extensionToCommentStyle, Map<String,
            FileHeaderTransformer> transformers )
    {

        Map<String, List<File>> results = new HashMap<>();

        // add for all known comment style (says transformer) a empty list
        // this permits not to have to test if there is an already list each time
        // we wants to add a new file...
        for ( String commentStyle : transformers.keySet() )
        {
            results.put( commentStyle, new ArrayList<File>() );
        }

        List<String> rootsList = new ArrayList<>( roots.length );
        for ( String root : roots )
        {
            File f = new File( root );
            if ( f.isAbsolute() )
            {
                rootsList.add( f.getAbsolutePath() );
            }
            else
            {
                f = new File( getProject().getBasedir(), root );
            }
            if ( f.exists() )
            {
                LOG.info( "Will search files to update from root {}", f );
                rootsList.add( f.getAbsolutePath() );
            }
            else
            {
                if ( isVerbose() )
                {
                    LOG.info( "Skip not found root {}", f );
                }
            }
        }

        // Obtain all files to treat
        Map<File, String[]> allFiles = new HashMap<>();
        getFilesToTreatForRoots( includes, excludes, rootsList, allFiles );

        // filter all these files according to their extension

        for ( Map.Entry<File, String[]> entry : allFiles.entrySet() )
        {
            File root = entry.getKey();
            String[] filesPath = entry.getValue();

            // sort them by the associated comment style to their extension
            for ( String path : filesPath )
            {
                String extension = FileUtils.extension( path );
                String commentStyle = extensionToCommentStyle.get( extension );
                if ( StringUtils.isEmpty( commentStyle ) )
                {

                    // unknown extension, try with extra files
                    File file = new File( root, path );
                    commentStyle = extraFiles.get( file.getName() );
                    if ( StringUtils.isEmpty( commentStyle ) )
                    {
                        // do not treat this file
                        continue;
                    }
                }
                //
                File file = new File( root, path );
                List<File> files = results.get( commentStyle );
                files.add( file );
            }
        }
        return results;
    }

    /**
     * Gets all files for the given {@code state}.
     *
     * @param state state of files to get
     * @return all files of the given state
     */
    private Set<File> getFiles( EnumMap<FileState, Set<File>> result, FileState state )
    {
        return result.get( state );
    }


    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private void addPropertiesToContext( Properties properties, String prefix, Map<String, Object> context )
    {
        @SuppressWarnings( { "rawtypes", "unchecked" } )
        Map<String, String> cast = (Map) properties;
        addPropertiesToContext( cast, prefix, context );
    }

    private void addPropertiesToContext( Map<String, String> properties, String prefix, Map<String, Object> context )
    {
        if ( properties != null )
        {
            for ( Object o : properties.keySet() )
            {
                String nextKey = (String) o;
                Object nextValue = properties.get( nextKey );
                context.put( prefix + nextKey, nextValue.toString() );
            }
        }
    }
}
