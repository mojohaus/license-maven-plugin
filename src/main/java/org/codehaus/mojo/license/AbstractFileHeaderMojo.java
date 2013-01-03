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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.api.FreeMarkerHelper;
import org.codehaus.mojo.license.header.FileHeader;
import org.codehaus.mojo.license.header.FileHeaderFilter;
import org.codehaus.mojo.license.header.FileHeaderProcessor;
import org.codehaus.mojo.license.header.FileHeaderProcessorConfiguration;
import org.codehaus.mojo.license.header.InvalideFileHeaderException;
import org.codehaus.mojo.license.header.UpdateFileHeaderFilter;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.JavaFileHeaderTransformer;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.nuiton.processor.Processor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstract mojo for file-header operations (chekc, update, report,...).
 * <p/>
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.2
 */
public abstract class AbstractFileHeaderMojo
    extends AbstractLicenseNameMojo
    implements FileHeaderProcessorConfiguration
{

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    public static final String[] DEFAULT_INCLUDES = new String[]{ "**/*" };

    public static final String[] DEFAULT_EXCLUDES =
        new String[]{ "**/*.zargo", "**/*.uml", "**/*.umldi", "**/*.xmi", /* modelisation */
            "**/*.img", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.gif", /* images */
            "**/*.zip", "**/*.jar", "**/*.war", "**/*.ear", "**/*.tgz", "**/*.gz" };

    public static final String[] DEFAULT_ROOTS =
        new String[]{ "src", "target/generated-sources", "target/processed-sources" };

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * Name of project (or module).
     * <p/>
     * Will be used as description section of new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.projectName", defaultValue = "${project.name}", required = true )
    protected String projectName;

    /**
     * Name of project's organization.
     * <p/>
     * Will be used as copyrigth's holder in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.organizationName", defaultValue = "${project.organization.name}", required = true )
    protected String organizationName;

    /**
     * Inception year of the project.
     * <p/>
     * Will be used as first year of copyright section in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.inceptionYear", defaultValue = "${project.inceptionYear}", required = true )
    protected String inceptionYear;

    /**
     * To overwrite the processStartTag used to build header model.
     * <p/>
     * See http://mojo.codehaus.org/license-maven-plugin/header.html#Configuration .
     *
     * @since 1.1
     */
    @Parameter( property = "license.processStartTag" )
    protected String processStartTag;

    /**
     * To overwrite the processEndTag used to build header model.
     * <p/>
     * See http://mojo.codehaus.org/license-maven-plugin/header.html#Configuration .
     *
     * @since 1.1
     */
    @Parameter( property = "license.processEndTag" )
    protected String processEndTag;

    /**
     * To overwrite the sectionDelimiter used to build header model.
     * <p/>
     * See http://mojo.codehaus.org/license-maven-plugin/header.html#Configuration .
     *
     * @since 1.1
     */
    @Parameter( property = "license.sectionDelimiter" )
    protected String sectionDelimiter;

    /**
     * A flag to add svn:keywords on new header.
     * <p/>
     * Will add svn keywords :
     * <pre>Id, HeadURL</pre>
     * <p/>
     * <strong>Note:</strong> This parameter is used by the {@link #descriptionTemplate}, so if you change this
     * template, the parameter could be no more used (depends what you put in your own template...).
     *
     * @since 1.0
     */
    @Parameter( property = "license.addSvnKeyWords", defaultValue = "false" )
    protected boolean addSvnKeyWords;

    /**
     * A flag to authorize update of the description part of the header.
     * <p/>
     * <b>Note:</b> By default, do NOT authorize it since description can change
     * on each file).
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateDescription", defaultValue = "false" )
    protected boolean canUpdateDescription;

    /**
     * A flag to authorize update of the copyright part of the header.
     * <p/>
     * <b>Note:</b> By default, do NOT authorize it since copyright part should be
     * handled by developpers (holder can change on each file for example).
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateCopyright", defaultValue = "false" )
    protected boolean canUpdateCopyright;

    /**
     * A flag to authorize update of the license part of the header.
     * <p/>
     * <b>Note:</b> By default, authorize it since license part should always be
     * generated by the plugin.
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateLicense", defaultValue = "true" )
    protected boolean canUpdateLicense;

    /**
     * A tag to place on files that will be ignored by the plugin.
     * <p/>
     * Sometimes, it is necessary to do this when file is under a specific license.
     * <p/>
     * <b>Note:</b> If no sets, will use the default tag {@code %% Ignore-License}
     *
     * @since 1.0
     */
    @Parameter( property = "license.ignoreTag" )
    protected String ignoreTag;

    /**
     * A flag to clear everything after execution.
     * <p/>
     * <b>Note:</b> This property should ONLY be used for test purpose.
     *
     * @since 1.0
     */
    @Parameter( property = "license.clearAfterOperation", defaultValue = "true" )
    protected boolean clearAfterOperation;

    /**
     * A flag to add the license header in java files after the package statement.
     * <p/>
     * This is a pratice used by many people (apache, codehaus, ...).
     * <p/>
     * <b>Note:</b> By default this property is then to {@code true} since it is a good pratice.
     *
     * @since 1.2
     */
    @Parameter( property = "license.addJavaLicenseAfterPackage", defaultValue = "true" )
    protected boolean addJavaLicenseAfterPackage;

    /**
     * To specify the base dir from which we apply the license.
     * <p/>
     * Should be on form "root1,root2,rootn".
     * <p/>
     * By default, the main roots are "src, target/generated-sources, target/processed-sources".
     * <p/>
     * <b>Note:</b> If some of these roots do not exist, they will be simply
     * ignored.
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @since 1.0
     */
    @Parameter( property = "license.roots" )
    protected String[] roots;

    /**
     * Specific files to includes, separated by a comma. By default, it is "** /*".
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @since 1.0
     */
    @Parameter( property = "license.includes" )
    protected String[] includes;

    /**
     * Specific files to excludes, separated by a comma.
     * By default, thoses file type are excluded:
     * <ul>
     * <li>modelisation</li>
     * <li>images</li>
     * </ul>
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @since 1.0
     */
    @Parameter( property = "license.excludes" )
    protected String[] excludes;

    /**
     * To associate extra extension files to an existing comment style.
     * <p/>
     * Keys of the map are the extension of extra files to treate, and the value
     * is the comment style you want to associate.
     * <p/>
     * For example, to treate file with extensions {@code java2} and {@code jdata}
     * as {@code java} files (says using the {@code java} comment style, declare this
     * in your plugin configuration :
     * <pre>
     * &lt;extraExtensions&gt;
     * &lt;java2&gt;java&lt;/java2&gt;
     * &lt;jdata&gt;java&lt;/jdata&gt;
     * &lt;/extraExtensions&gt;
     * </pre>
     * <p/>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @parameter
     * @since 1.0
     */
    @Parameter
    protected Map<String, String> extraExtensions;

    /**
     * Template used to build the description scetion of the license header.
     * <p/>
     * (This template use freemarker).
     *
     * @since 1.1
     */
    @Parameter( property = "license.descriptionTemplate",
                defaultValue = "/org/codehaus/mojo/license/default-file-header-description.ftl" )
    protected String descriptionTemplate;

    // ----------------------------------------------------------------------
    // Plexus components
    // ----------------------------------------------------------------------

    /**
     * @since 1.0
     */
    @Component( role = Processor.class, hint = "file-header" )
    private FileHeaderProcessor processor;

    /**
     * The processor filter used to change header content.
     *
     * @since 1.0
     */
    @Component( role = FileHeaderFilter.class, hint = "update-file-header" )
    private UpdateFileHeaderFilter filter;

    /**
     * All available header transformers.
     *
     * @since 1.0
     */
    @Component( role = FileHeaderTransformer.class )
    private Map<String, FileHeaderTransformer> transformers;

    /**
     * Freemarker helper component.
     *
     * @since 1.0
     */
    @Component( role = FreeMarkerHelper.class )
    private FreeMarkerHelper freeMarkerHelper;

    // ----------------------------------------------------------------------
    // Private fields
    // ----------------------------------------------------------------------

    /**
     * internal file header transformer.
     */
    private FileHeaderTransformer transformer;

    /**
     * internal default file header.
     */
    private FileHeader header;

    /**
     * timestamp used for generation.
     */
    private long timestamp;

    /**
     * The dictionnary of extension indexed by their associated comment style.
     *
     * @since 1.0
     */
    private Map<String, String> extensionToCommentStyle;

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
     * Dictionnary of treated files indexed by their state.
     */
    private EnumMap<FileState, Set<File>> result;

    /**
     * Dictonnary of files to treate indexed by their CommentStyle.
     */
    private Map<String, List<File>> filesToTreateByCommentStyle;

    // ----------------------------------------------------------------------
    // Abstract Methods
    // ----------------------------------------------------------------------

    /**
     * @return {@code true} if mojo must be a simple dry run (says do not modifiy any scanned files),
     *         {@code false} otherise.
     */
    protected abstract boolean isDryRun();

    /**
     * @return {@code true} if mojo should fails if dryRun and there is some missing license header, {@code false} otherwise.
     */
    protected abstract boolean isFailOnMissingHeader();

    /**
     * @return {@code true} if mojo should fails if dryRun and there is some obsolete license header, {@code false} otherwise.
     */
    protected abstract boolean isFailOnNotUptodateHeader();

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void init()
        throws Exception
    {

        if ( isSkip() )
        {
            return;
        }

        if ( StringUtils.isEmpty( ignoreTag ) )
        {

            // use default value
            this.ignoreTag = "%" + "%Ignore-License";
        }

        if ( !isDryRun() )
        {

            if ( isFailOnMissingHeader() )
            {

                getLog().warn( "The failOnMissingHeader has no effect if the property dryRun is not setted." );
            }

            if ( isFailOnNotUptodateHeader() )
            {

                getLog().warn( "The failOnNotUptodateHeader has no effect if the property dryRun is not setted." );
            }
        }

        if ( isVerbose() )
        {

            // print availables comment styles (transformers)
            StringBuilder buffer = new StringBuilder();
            buffer.append( "config - available comment styles :" );
            String commentFormat = "\n  * %1$s (%2$s)";
            for ( String transformerName : transformers.keySet() )
            {
                FileHeaderTransformer aTransformer = getTransformer( transformerName );
                String str = String.format( commentFormat, aTransformer.getName(), aTransformer.getDescription() );
                buffer.append( str );
            }
            getLog().info( buffer.toString() );
        }

        // set timestamp used for temporary files
        this.timestamp = System.nanoTime();

        // add flags to authorize or not updates of header
        filter.setUpdateCopyright( canUpdateCopyright );
        filter.setUpdateDescription( canUpdateDescription );
        filter.setUpdateLicense( canUpdateLicense );

        filter.setLog( getLog() );
        processor.setConfiguration( this );
        processor.setFilter( filter );

        super.init();

        if ( roots == null || roots.length == 0 )
        {
            roots = DEFAULT_ROOTS;
            if ( isVerbose() )
            {
                getLog().info( "Will use default roots " + Arrays.toString( roots ) );
            }
        }

        if ( includes == null || includes.length == 0 )
        {
            includes = DEFAULT_INCLUDES;
            if ( isVerbose() )
            {
                getLog().info( "Will use default includes " + Arrays.toString( includes ) );
            }
        }

        if ( excludes == null || excludes.length == 0 )
        {
            excludes = DEFAULT_EXCLUDES;
            if ( isVerbose() )
            {
                getLog().info( "Will use default excludes" + Arrays.toString( excludes ) );
            }
        }

        extensionToCommentStyle = new TreeMap<String, String>();

        processStartTag = cleanHeaderConfiguration( processStartTag, FileHeaderTransformer.DEFAULT_PROCESS_START_TAG );
        if ( isVerbose() )
        {
            getLog().info( "Will use processStartTag: " + processEndTag );
        }
        processEndTag = cleanHeaderConfiguration( processEndTag, FileHeaderTransformer.DEFAULT_PROCESS_END_TAG );
        if ( isVerbose() )
        {
            getLog().info( "Will use processEndTag: " + processEndTag );
        }
        sectionDelimiter =
            cleanHeaderConfiguration( sectionDelimiter, FileHeaderTransformer.DEFAULT_SECTION_DELIMITER );

        if ( isVerbose() )
        {
            getLog().info( "Will use sectionDelimiter: " + sectionDelimiter );
        }

        // add default extensions from header transformers
        for ( Map.Entry<String, FileHeaderTransformer> entry : transformers.entrySet() )
        {
            String commentStyle = entry.getKey();
            FileHeaderTransformer aTransformer = entry.getValue();

            aTransformer.setProcessStartTag( processStartTag );
            aTransformer.setProcessEndTag( processEndTag );
            aTransformer.setSectionDelimiter( sectionDelimiter );

            if ( aTransformer instanceof JavaFileHeaderTransformer )
            {
                ( (JavaFileHeaderTransformer) aTransformer ).setAddJavaLicenseAfterPackage(
                    addJavaLicenseAfterPackage );
            }

            String[] extensions = aTransformer.getDefaultAcceptedExtensions();
            for ( String extension : extensions )
            {
                if ( isVerbose() )
                {
                    getLog().info( "Associate extension " + extension + " to comment style " + commentStyle );
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
                    getLog().warn( "The extension " + extension + " is already accepted for comment style " +
                                       extensionToCommentStyle.get( extension ) );
                }
                String commentStyle = entry.getValue();

                // check transformer exists
                getTransformer( commentStyle );

                if ( isVerbose() )
                {
                    getLog().info( "Associate extension '" + extension + "' to comment style '" + commentStyle + "'" );
                }
                extensionToCommentStyle.put( extension, commentStyle );
            }
        }

        // get all files to treate indexed by their comment style
        filesToTreateByCommentStyle = obtainFilesToProcessByCommentStyle();

        // build the description template
        if ( isVerbose() )
        {
            getLog().info( "Use description template : " + descriptionTemplate );
        }
        descriptionTemplate0 = freeMarkerHelper.getTemplate( descriptionTemplate );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doAction()
        throws Exception
    {

        long t0 = System.nanoTime();

        clear();

        processedFiles = new HashSet<File>();
        result = new EnumMap<FileState, Set<File>>( FileState.class );

        try
        {

            for ( Map.Entry<String, List<File>> commentStyleFiles : filesToTreateByCommentStyle.entrySet() )
            {

                String commentStyle = commentStyleFiles.getKey();
                List<File> files = commentStyleFiles.getValue();

                processCommentStyle( commentStyle, files );
            }

        }
        finally
        {
            checkResults( result );

            int nbFiles = processedFiles.size();
            if ( nbFiles == 0 )
            {
                getLog().warn( "No file to scan." );
            }
            else
            {
                String delay = MojoHelper.convertTime( System.nanoTime() - t0 );
                String message =
                    String.format( "Scan %s file%s header done in %s.", nbFiles, nbFiles > 1 ? "s" : "", delay );
                getLog().info( message );
            }
            Set<FileState> states = result.keySet();
            if ( states.size() == 1 && states.contains( FileState.uptodate ) )
            {
                // all files where up to date
                getLog().info( "All files are up-to-date." );
            }
            else
            {

                StringBuilder buffer = new StringBuilder();
                for ( FileState state : FileState.values() )
                {

                    reportType( state, buffer );
                }

                getLog().info( buffer.toString() );
            }

            // clean internal states
            if ( clearAfterOperation )
            {
                clear();
            }
        }
    }

    // ----------------------------------------------------------------------
    // FileHeaderProcessorConfiguration Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public FileHeader getFileHeader()
    {
        return header;
    }

    /**
     * {@inheritDoc}
     */
    public FileHeaderTransformer getTransformer()
    {
        return transformer;
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    /**
     * Gets all files to process indexed by their comment style.
     *
     * @return for each comment style, list of files to process
     */
    protected Map<String, List<File>> obtainFilesToProcessByCommentStyle()
    {

        Map<String, List<File>> results = new HashMap<String, List<File>>();

        // add for all known comment style (says transformer) a empty list
        // this permits not to have to test if there is an already list each time
        // we wants to add a new file...
        for ( String commentStyle : transformers.keySet() )
        {
            results.put( commentStyle, new ArrayList<File>() );
        }

        List<String> rootsList = new ArrayList<String>( roots.length );
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
                getLog().info( "Will search files to update from root " + f );
                rootsList.add( f.getAbsolutePath() );
            }
            else
            {
                if ( isVerbose() )
                {
                    getLog().info( "Skip not found root " + f );
                }
            }
        }

        // Obtain all files to treate
        Map<File, String[]> allFiles = new HashMap<File, String[]>();
        getFilesToTreateForRoots( includes, excludes, rootsList, allFiles );

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

                    // unknown extension, do not treate this file
                    continue;
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
     * Checks the results of the mojo execution using the {@link #isFailOnMissingHeader()} and
     * {@link #isFailOnNotUptodateHeader()}.
     *
     * @param result processed files by their status
     * @throws MojoFailureException if check is not ok (some file with no header or to update)
     */
    protected void checkResults( EnumMap<FileState, Set<File>> result )
        throws MojoFailureException
    {
        Set<FileState> states = result.keySet();

        StringBuilder builder = new StringBuilder();
        if ( isDryRun() && isFailOnMissingHeader() && states.contains( FileState.add ) )
        {
            List<File> files = FileUtil.orderFiles( result.get( FileState.add ) );

            builder.append( "There is " ).append( files.size() ).append( " file(s) with no header :" );
            for ( File file : files )
            {
                builder.append( "\n" ).append( file );
            }
        }

        if ( isDryRun() && isFailOnNotUptodateHeader() && states.contains( FileState.update ) )
        {
            List<File> files = FileUtil.orderFiles( result.get( FileState.update ) );

            builder.append( "\nThere is " ).append( files.size() ).append( " file(s) with header to update:" );
            for ( File file : files )
            {
                builder.append( "\n" ).append( file );
            }
        }
        String message = builder.toString();

        if ( StringUtils.isNotBlank( message ) )
        {
            throw new MojoFailureException( builder.toString() );
        }
    }

    /**
     * Process a given comment styl to all his detected files.
     *
     * @param commentStyle comment style to treat
     * @param filesToTreat files using this comment style to treat
     * @throws IOException if any IO error while processing files
     */
    protected void processCommentStyle( String commentStyle, List<File> filesToTreat )
        throws IOException
    {

        // obtain license from definition
        License license = getLicense( getLicenseName(), true );

        if ( isVerbose() )
        {
            getLog().info( "Process header '" + commentStyle + "'" );
            getLog().info( " - using " + license.getDescription() );
        }

        // use header transformer according to comment style given in header
        this.transformer = getTransformer( commentStyle );

        // file header to use if no header is found on a file
        this.header = buildDefaultFileHeader( license, getEncoding() );

        // update processor filter
        processor.populateFilter();

        for ( File file : filesToTreat )
        {
            processFile( file );
        }
        filesToTreat.clear();
    }

    /**
     * Process the given file (will copy it, process the clone file and finally finalizeFile after process)...
     *
     * @param file original file to process
     * @throws IOException if any IO error while processing this file
     */
    protected void processFile( File file )
        throws IOException
    {

        if ( processedFiles.contains( file ) )
        {
            getLog().info( " - skip already processed file " + file );
            return;
        }

        // output file
        File processFile = new File( file.getAbsolutePath() + "_" + timestamp );
        boolean doFinalize = false;
        try
        {
            doFinalize = processFile( file, processFile );
        }
        catch ( Exception e )
        {
            getLog().warn( "skip failed file : " + e.getMessage() +
                               ( e.getCause() == null ? "" : " Cause : " + e.getCause().getMessage() ), e );
            FileState.fail.addFile( file, result );
            doFinalize = false;
        }
        finally
        {

            // always clean processor internal states
            processor.reset();

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
     * Process the given {@code file} and save the result in the given
     * {@code processFile}.
     *
     * @param file        the file to process
     * @param processFile the ouput processed file
     * @return {@code true} if prepareProcessFile can be finalize, otherwise need to be delete
     * @throws java.io.IOException if any pb while treatment
     */
    protected boolean processFile( File file, File processFile )
        throws IOException
    {

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( " - process file " + file );
            getLog().debug( " - will process into file " + processFile );
        }

        // update the file header description
        updateFileHeaderDescription( file );

        String content;

        try
        {

            // check before all that file should not be skip by the ignoreTag
            // this is a costy operation
            //TODO-TC-20100411 We should process always from the read content not reading again from file

            content = FileUtil.readAsString( file, getEncoding() );

        }
        catch ( IOException e )
        {
            throw new IOException( "Could not obtain content of file " + file );
        }

        //check that file is not marked to be ignored
        if ( content.contains( ignoreTag ) )
        {
            getLog().info( " - ignore file (detected " + ignoreTag + ") " + file );

            FileState.ignore.addFile( file, result );

            return false;
        }

        // process file to detect header

        try
        {
            processor.process( file, processFile );
        }
        catch ( IllegalStateException e )
        {
            // could not obtain existing header
            throw new InvalideFileHeaderException(
                "Could not extract header on file " + file + " for reason " + e.getMessage() );
        }
        catch ( Exception e )
        {
            if ( e instanceof InvalideFileHeaderException )
            {
                throw (InvalideFileHeaderException) e;
            }
            throw new IOException( "Could not process file " + file + " for reason " + e.getMessage() );
        }

        if ( processor.isTouched() )
        {

            if ( isVerbose() )
            {
                getLog().info( " - header was updated for " + file );
            }
            if ( processor.isModified() )
            {

                // header content has changed
                // must copy back process file to file (if not dry run)

                FileState.update.addFile( file, result );
                return true;

            }

            FileState.uptodate.addFile( file, result );
            return false;
        }

        // header was not fully (or not at all) detected in file

        if ( processor.isDetectHeader() )
        {

            // file has not a valid header (found a start process atg, but
            // not an ending one), can not do anything
            throw new InvalideFileHeaderException( "Could not find header end on file " + file );
        }

        // no header at all, add a new header

        getLog().info( " - adding license header on file " + file );

        //FIXME tchemit 20100409 xml files must add header after a xml prolog line
        content = getTransformer().addHeader( filter.getFullHeaderContent(), content );

        if ( !isDryRun() )
        {
            FileUtil.writeString( processFile, content, getEncoding() );
        }

        FileState.add.addFile( file, result );
        return true;
    }

    /**
     * Finalize the process of a file.
     * <p/>
     * If ad DryRun then just remove processed file, else use process file as original file.
     *
     * @param file        the original file
     * @param processFile the processed file
     * @throws IOException if any IO error while finalizing file
     */
    protected void finalizeFile( File file, File processFile )
        throws IOException
    {

        if ( isKeepBackup() && !isDryRun() )
        {
            File backupFile = FileUtil.getBackupFile( file );

            if ( backupFile.exists() )
            {

                // always delete backup file, before the renaming
                FileUtil.deleteFile( backupFile );
            }

            if ( isVerbose() )
            {
                getLog().debug( " - backup original file " + file );
            }

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

                // workaround windows problem to rename  files
                getLog().warn( e.getMessage() );

                // try to copy content (fail on windows xp...)
                FileUtils.copyFile( processFile, file );

                // then delete process file
                FileUtil.deleteFile( processFile );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        super.finalize();
        clear();
    }

    /**
     * Clear internal states of the mojo after execution. (will only invoked if {@link #clearAfterOperation} if on).
     */
    protected void clear()
    {
        if ( processedFiles != null )
        {
            processedFiles.clear();
        }
        if ( result != null )
        {
            for ( Set<File> fileSet : result.values() )
            {
                fileSet.clear();
            }
            result.clear();
        }
    }

    /**
     * Reports into the given {@code buffer} stats for the given {@code state}.
     *
     * @param state  state of file to report
     * @param buffer where to report
     */
    protected void reportType( FileState state, StringBuilder buffer )
    {
        String operation = state.name();

        Set<File> set = getFiles( state );
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
     * Build a default header given the parameters.
     *
     * @param license  the license type ot use in header
     * @param encoding encoding used to read or write files
     * @return the new file header
     * @throws java.io.IOException if any problem while creating file header
     */
    protected FileHeader buildDefaultFileHeader( License license, String encoding )
        throws IOException
    {
        FileHeader defaultFileHeader = new FileHeader();

        String licenseContent = license.getHeaderContent( encoding );
        defaultFileHeader.setLicense( licenseContent );

        Integer firstYear = Integer.valueOf( inceptionYear );
        defaultFileHeader.setCopyrightFirstYear( firstYear );

        Calendar cal = Calendar.getInstance();
        cal.setTime( new Date() );
        Integer lastYear = cal.get( Calendar.YEAR );
        if ( firstYear < lastYear )
        {
            defaultFileHeader.setCopyrightLastYear( lastYear );
        }
        defaultFileHeader.setCopyrightHolder( organizationName );
        return defaultFileHeader;
    }

    /**
     * Update in file header the description parts given the current file.
     *
     * @param file current file to treat
     * @throws java.io.IOException if any problem while creating file header
     */
    protected void updateFileHeaderDescription( File file )
        throws IOException
    {

        Map<String, Object> descriptionParameters = new HashMap<String, Object>();
        descriptionParameters.put( "project", getProject() );
        descriptionParameters.put( "addSvnKeyWords", addSvnKeyWords );
        descriptionParameters.put( "projectName", projectName );
        descriptionParameters.put( "inceptionYear", inceptionYear );
        descriptionParameters.put( "organizationName", organizationName );
        descriptionParameters.put( "file", file );

        String description = freeMarkerHelper.renderTemplate( descriptionTemplate0, descriptionParameters );
        header.setDescription( description );
        filter.resetContent();

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "header description : " + header.getDescription() );
        }
    }

    /**
     * Obtains the {@link FileHeaderTransformer} given his name.
     *
     * @param transformerName the name of the transformer to find
     * @return the transformer for the givne tramsformer name
     */
    protected FileHeaderTransformer getTransformer( String transformerName )
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
     * Obtain for a given value, a trim version of it. If value is empty then use the given default value
     *
     * @param value        the value to trim (if not empty)
     * @param defaultValue the default value to use if value is empty
     * @return the trim value (or default value if value is empty)
     */
    protected String cleanHeaderConfiguration( String value, String defaultValue )
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
     * Gets all files for the given {@code state}.
     *
     * @param state state of files to get
     * @return all files of the given state
     */
    protected Set<File> getFiles( FileState state )
    {
        return result.get( state );
    }

    /**
     * Collects some file.
     *
     * @param includes includes
     * @param excludes excludes
     * @param roots    root directories to treate
     * @param files    cache of file detected indexed by their root directory
     */
    protected void getFilesToTreateForRoots( String[] includes, String[] excludes, List<String> roots,
                                             Map<File, String[]> files )
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

            if ( getLog().isDebugEnabled() )
            {
                getLog().debug( "discovering source files in " + src );
            }

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

            List<String> toTreate = new ArrayList<String>();

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
     * Defines state of a file after process.
     *
     * @author tchemit <chemit@codelutin.com>
     * @since 1.0
     */
    public static enum FileState
    {

        /**
         * file was updated
         */
        update,

        /**
         * file was up to date
         */
        uptodate,

        /**
         * something was added on file
         */
        add,

        /**
         * file was ignored
         */
        ignore,

        /**
         * treatment failed for file
         */
        fail;

        /**
         * Register a file for this state on result dictionary.
         *
         * @param file    file to add
         * @param results dictionary to update
         */
        public void addFile( File file, EnumMap<FileState, Set<File>> results )
        {
            Set<File> fileSet = results.get( this );
            if ( fileSet == null )
            {
                fileSet = new HashSet<File>();
                results.put( this, fileSet );
            }
            fileSet.add( file );
        }
    }
}
