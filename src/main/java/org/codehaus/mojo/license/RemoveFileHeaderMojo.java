package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2012 CodeLutin, Codehaus, Tony Chemit
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

import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The goal to remove the header on project source files.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.11
 */
@Mojo( name = "remove-file-header", threadSafe = true )
public class RemoveFileHeaderMojo extends AbstractLicenseNameMojo
{
    private static final Logger LOG = LoggerFactory.getLogger( RemoveFileHeaderMojo.class );

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A flag to skip the goal.
     *
     * @since 1.11
     */
    @Parameter( property = "license.skipRemoveLicense", defaultValue = "false" )
    private boolean skipRemoveLicense;

    /**
     * A flag to test plugin but modify no file.
     *
     * @since 1.11
     */
    @Parameter( property = "dryRun", defaultValue = "false" )
    private boolean dryRun;

    /**
     * A flag to ignore no files to scan.
     * <p>
     * This flag will suppress the "No file to scan" warning. This will allow you to set the plug-in in the root pom of
     * your project without getting a lot of warnings for aggregation modules / artifacts.
     * </p>
     *
     * @since 1.11
     */
    @Parameter( property = "license.ignoreNoFileToScan", defaultValue = "false" )
    private boolean ignoreNoFileToScan;

    /**
     * To specify the base dir from which we apply the license.
     * <p>
     * Should be on form "root1,root2,rootn".
     * <p>
     * By default, the main roots are "src, target/generated-sources, target/processed-sources".
     * <p>
     * <b>Note:</b> If some of these roots do not exist, they will be simply
     * ignored.
     *
     * @since 1.11
     */
    @Parameter( property = "license.roots" )
    private String[] roots;

    /**
     * Specific files to includes, separated by a comma. By default, it is "** /*".
     *
     * @since 1.11
     */
    @Parameter( property = "license.includes" )
    private String[] includes;

    /**
     * Specific files to excludes, separated by a comma.
     * By default, those file types are excluded:
     * <ul>
     * <li>modelisation</li>
     * <li>images</li>
     * </ul>
     *
     * @since 1.11
     */
    @Parameter( property = "license.excludes" )
    private String[] excludes;

    /**
     * To associate extra extension files to an existing comment style.
     * <p>
     * Keys of the map are the extension of extra files to treat, and the value
     * is the comment style you want to associate.
     * <p>
     * For example, to treat file with extensions {@code java2} and {@code jdata}
     * as {@code java} files (says using the {@code java} comment style, declare this
     * in your plugin configuration :
     * <pre>
     * &lt;extraExtensions&gt;
     * &lt;java2&gt;java&lt;/java2&gt;
     * &lt;jdata&gt;java&lt;/jdata&gt;
     * &lt;/extraExtensions&gt;
     * </pre>
     *
     * @since 1.11
     */
    @Parameter
    private Map<String, String> extraExtensions;

    /**
     * To associate extra files to an existing comment style.
     * <p>
     * Keys of the map are the name of extra files to treat, and the value
     * is the comment style you want to associate.
     * <p>
     * For example, to treat a file named {@code DockerFile} as {@code properties} files
     * (says using the {@code properties} comment style, declare this in your plugin configuration :
     * <pre>
     * &lt;extraFiles&gt;
     * &lt;DockerFile&gt;properties&lt;/DockerFile&gt;
     * &lt;/extraFiles&gt;
     * </pre>
     *
     * @since 1.11
     */
    @Parameter
    private Map<String, String> extraFiles;

    /**
     * A tag to place on files that will be ignored by the plugin.
     * <p>
     * Sometimes, it is necessary to do this when file is under a specific license.
     * <p>
     * <b>Note:</b> If no sets, will use the default tag {@code %% Ignore-License}
     *
     * @since 1.0
     */
    @Parameter( property = "license.ignoreTag" )
    private String ignoreTag;

    // ----------------------------------------------------------------------
    // Plexus components
    // ----------------------------------------------------------------------

    /**
     * All available header transformers.
     *
     * @since 1.0
     */
    @Component( role = FileHeaderTransformer.class )
    private Map<String, FileHeaderTransformer> transformers;

    // ----------------------------------------------------------------------
    // Private fields
    // ----------------------------------------------------------------------

    /**
     * timestamp used for generation.
     */
    private long timestamp;

    /**
     * The freemarker template used to render the description section of the file header.
     *
     * @since 1.1
     */
    private Template descriptionTemplate0;

    /**
     * set of processed files
     */
    private Set<File> processedFiles;

    /**
     * Dictionary of treated files indexed by their state.
     */
    private EnumMap<FileState, Set<File>> result;

    /**
     * Dictionary of files to treat indexed by their CommentStyle.
     */
    private Map<String, List<File>> filesToTreatByCommentStyle;

    // ----------------------------------------------------------------------
    // AbstractLicenceMojo Implementation
    // ----------------------------------------------------------------------

    @Override
    public boolean isSkip()
    {
        return skipRemoveLicense;
    }

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    @Override
    public void init() throws Exception
    {

        if ( StringUtils.isEmpty( ignoreTag ) )
        {

            // use default value
            this.ignoreTag = "%" + "%Ignore-License";
        }

        if ( isVerbose() )
        {

            // print available comment styles (transformers)
            StringBuilder buffer = new StringBuilder();
            buffer.append( "config - available comment styles :" );
            String commentFormat = "\n  * %1$s (%2$s)";
            for ( String transformerName : transformers.keySet() )
            {
                FileHeaderTransformer aTransformer = getTransformer( transformers, transformerName );
                String str = String.format( commentFormat, aTransformer.getName(), aTransformer.getDescription() );
                buffer.append( str );
            }
            LOG.info( "{}", buffer );
        }

        // set timestamp used for temporary files
        this.timestamp = System.nanoTime();

        super.init();

        if ( roots == null || roots.length == 0 )
        {
            roots = DEFAULT_ROOTS;
            if ( isVerbose() )
            {
                LOG.info( "Will use default roots {}", ( Object ) roots );
            }
        }

        if ( includes == null || includes.length == 0 )
        {
            includes = DEFAULT_INCLUDES;
            if ( isVerbose() )
            {
                LOG.info( "Will use default includes {}", ( Object ) includes );
            }
        }

        if ( excludes == null || excludes.length == 0 )
        {
            excludes = DEFAULT_EXCLUDES;
            if ( isVerbose() )
            {
                LOG.info( "Will use default excludes {}", ( Object ) excludes );
            }
        }

        Map<String, String> extensionToCommentStyle = new TreeMap<>();

        // add default extensions from header transformers
        for ( Map.Entry<String, FileHeaderTransformer> entry : transformers.entrySet() )
        {
            String commentStyle = entry.getKey();
            FileHeaderTransformer aTransformer = entry.getValue();
            String[] extensions = aTransformer.getDefaultAcceptedExtensions();
            for ( String extension : extensions )
            {
                if ( isVerbose() )
                {
                    LOG.info( "Associate extension '{}' to comment style '{}'", extension, commentStyle );
                }
                extensionToCommentStyle.put( extension, commentStyle );
            }
        }

        if ( extraExtensions != null )
        {

            // fill extra extensions for each transformer
            for ( Map.Entry<String, String> entry : extraExtensions.entrySet() )
            {
                String extension = entry.getKey();
                if ( extensionToCommentStyle.containsKey( extension ) )
                {

                    // override existing extension mapping
                    LOG.warn( "The extension '{}' is already accepted for comment style '{}'",
                            extension, extensionToCommentStyle.get( extension ) );
                }
                String commentStyle = entry.getValue();

                // check transformer exists
                getTransformer( transformers, commentStyle );

                if ( isVerbose() )
                {
                    LOG.info( "Associate extension '{}' to comment style '{}'", extension, commentStyle );
                }
                extensionToCommentStyle.put( extension, commentStyle );
            }
        }

        if ( extraFiles == null )
        {
            extraFiles = Collections.emptyMap();
        }

        // get all files to treat indexed by their comment style
        filesToTreatByCommentStyle = obtainFilesToProcessByCommentStyle( extraFiles, roots, includes, excludes,
                extensionToCommentStyle, transformers );
    }

    @Override
    public void doAction() throws Exception
    {

        long t0 = System.nanoTime();

        processedFiles = new HashSet<>();
        result = new EnumMap<>( FileState.class );

        try
        {

            for ( Map.Entry<String, List<File>> commentStyleFiles : filesToTreatByCommentStyle.entrySet() )
            {

                String commentStyle = commentStyleFiles.getKey();
                List<File> files = commentStyleFiles.getValue();

                processCommentStyle( commentStyle, files );
            }

        }
        finally
        {
            int nbFiles = processedFiles.size();
            if ( nbFiles == 0 && !ignoreNoFileToScan )
            {
                LOG.warn( "No file to scan." );
            }
            else
            {
                String delay = MojoHelper.convertTime( System.nanoTime() - t0 );
                LOG.info( "Scanned {} file headers in {}.", nbFiles, delay );
            }
            Set<FileState> states = result.keySet();
            if ( states.size() == 1 && states.contains( FileState.uptodate ) )
            {
                // all files where up to date
                LOG.info( "All files are up-to-date." );
            }
            else
            {

                StringBuilder buffer = new StringBuilder();
                for ( FileState state : FileState.values() )
                {

                    reportType( result, state, buffer );
                }

                LOG.info( "{}", buffer );
            }

        }
    }

    private boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * Process a given comment style to all his detected files.
     *
     * @param commentStyle comment style to treat
     * @param filesToTreat files using this comment style to treat
     * @throws IOException if any IO error while processing files
     */
    private void processCommentStyle( String commentStyle, List<File> filesToTreat ) throws IOException
    {

        // obtain license from definition
        License license = getLicense( getLicenseName(), true );

        if ( isVerbose() )
        {
            LOG.info( "Process header '{}'", commentStyle );
            LOG.info( " - using {}", license.getDescription() );
        }

        // use header transformer according to comment style given in header
        FileHeaderTransformer transformer = getTransformer( transformers, commentStyle );


        for ( File file : filesToTreat )
        {
            processFile( transformer, file );
        }
        filesToTreat.clear();
    }

    private boolean processFile( FileHeaderTransformer transformer, File file, File processFile ) throws IOException
    {

        String content;

        try
        {
            content = FileUtil.readAsString( file, getEncoding() );
        }
        catch ( IOException e )
        {
            throw new IOException( "Could not obtain content of file " + file );
        }

        //check that file is not marked to be ignored
        if ( content.contains( ignoreTag ) )
        {
            LOG.info( " - ignore file (detected {}) {}", ignoreTag, file );

            FileState.ignore.addFile( file, result );

            return false;
        }

        String commentStartTag = transformer.getCommentStartTag();
        int firstIndex = content.indexOf( commentStartTag );
        if ( firstIndex == -1 )
        {

            FileState.uptodate.addFile( file, result );
            return false;
        }

        char lastchar = ' ';
        while ( lastchar != '\n' && firstIndex > 0 )
        {
            lastchar = content.charAt( ( --firstIndex ) );
        }
        String commentEndTag = transformer.getCommentEndTag();
        int lastIndex = content.indexOf( commentEndTag );
        if ( lastIndex == -1 )
        {
            FileState.uptodate.addFile( file, result );
            return false;
        }
        lastchar = ' ';
        while ( lastchar != '\n' )
        {
            lastchar = content.charAt( ( ++lastIndex ) );
        }

        if ( isVerbose() )
        {
            LOG.info( " - header was removed for {}", file );
        }

        String contentWithoutHeader = content.substring( 0, firstIndex ) + content.substring( lastIndex + 1 );

        FileUtils.fileWrite( processFile, contentWithoutHeader );
        FileState.remove.addFile( file, result );
        return true;
    }

    /**
     * Process the given file (will copy it, process the clone file and finally finalizeFile after process)...
     *
     * @param transformer current file header transformer
     * @param file        original file to process
     * @throws IOException if any IO error while processing this file
     */
    private void processFile( FileHeaderTransformer transformer, File file ) throws IOException
    {

        if ( processedFiles.contains( file ) )
        {
            LOG.info( " - skip already processed file {}", file );
            return;
        }

        // output file
        File processFile = new File( file.getAbsolutePath() + "_" + timestamp );
        boolean doFinalize = false;
        try
        {
            doFinalize = processFile( transformer, file, processFile );
        }
        catch ( Exception e )
        {
            LOG.warn( "skip failed file: {}{}",
                   e.getMessage(),
                   e.getCause() == null ? "" : " Cause : " + e.getCause().getMessage(),
                   e );
            FileState.fail.addFile( file, result );
            doFinalize = false;
        }
        finally
        {

            // whatever was the result, this file is treated.
            processedFiles.add( file );

            if ( doFinalize )
            {
                finalizeFile( file, processFile );
            }
            else
            {
                FileUtil.deleteFile( processFile );
            }
        }
    }

    /**
     * Finalize the process of a file.
     * <p>
     * If ad DryRun then just remove processed file, else use process file as original file.
     *
     * @param file        the original file
     * @param processFile the processed file
     * @throws IOException if any IO error while finalizing file
     */
    private void finalizeFile( File file, File processFile ) throws IOException
    {

        if ( isKeepBackup() && !isDryRun() )
        {
            File backupFile = FileUtil.getBackupFile( file );

            if ( backupFile.exists() )
            {

                // always delete backup file, before the renaming
                FileUtil.deleteFile( backupFile );
            }

            LOG.debug( " - backup original file {}", file );
            FileUtil.renameFile( file, backupFile );
        }

        if ( isDryRun() )
        {

            // dry run, delete temporary file
            FileUtil.deleteFile( processFile );
        }
        else
        {
            try
            {

                // replace file with the updated one
                FileUtil.renameFile( processFile, file );
            }
            catch ( IOException e )
            {
                LOG.warn( e.getMessage(), e );
            }
        }
    }

}
