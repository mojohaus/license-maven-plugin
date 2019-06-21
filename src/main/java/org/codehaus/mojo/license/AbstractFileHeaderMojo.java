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
import org.codehaus.mojo.license.header.FileHeaderProcessor;
import org.codehaus.mojo.license.header.InvalideFileHeaderException;
import org.codehaus.mojo.license.header.UpdateFileHeaderFilter;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.codehaus.mojo.license.header.transformer.JavaFileHeaderTransformer;
import org.codehaus.mojo.license.model.Copyright;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.utils.FileUtil;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstract mojo for file-header operations (check, update, report,...).
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.2
 */
public abstract class AbstractFileHeaderMojo extends AbstractLicenseNameMojo
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractFileHeaderMojo.class );

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * To overwrite the processStartTag used to build header model.
     * <p>
     * See <a href="http://mojohaus.org/license-maven-plugin/header.html#Configuration">File header configuration</a>.
     *
     * @since 1.1
     */
    @Parameter( property = "license.processStartTag" )
    private String processStartTag;

    /**
     * To overwrite the processEndTag used to build header model.
     * <p>
     * See <a href="http://mojohaus.org/license-maven-plugin/header.html#Configuration">File header configuration</a>.
     *
     * @since 1.1
     */
    @Parameter( property = "license.processEndTag" )
    private String processEndTag;

    /**
     * To overwrite the sectionDelimiter used to build header model.
     * <p>
     * See <a href="http://mojohaus.org/license-maven-plugin/header.html#Configuration">File header configuration</a>.
     *
     * @since 1.1
     */
    @Parameter( property = "license.sectionDelimiter" )
    private String sectionDelimiter;

    /**
     * To specify a line separator to use.
     *
     * If not set, will use system property {@code line.separator}.
     */
    @Parameter( property = "license.lineSeparator" )
    private String lineSeparator;

    /**
     * A flag to add svn:keywords on new header.
     * <p>
     * Will add svn keywords :
     * <pre>Id, HeadURL</pre>
     *
     * <strong>Note:</strong> This parameter is used by the {@link #descriptionTemplate}, so if you change this
     * template, the parameter could be no more used (depends what you put in your own template...).
     *
     * @since 1.0
     */
    @Parameter( property = "license.addSvnKeyWords", defaultValue = "false" )
    private boolean addSvnKeyWords;

    /**
     * A flag to authorize update of the description part of the header.
     * <p>
     * <b>Note:</b> By default, do NOT authorize it since description can change
     * on each file).
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateDescription", defaultValue = "false" )
    private boolean canUpdateDescription;

    /**
     * A flag to authorize update of the copyright part of the header.
     * <p>
     * <b>Note:</b> By default, do NOT authorize it since copyright part should be
     * handled by developpers (holder can change on each file for example).
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateCopyright", defaultValue = "false" )
    private boolean canUpdateCopyright;

    /**
     * A flag to authorize update of the license part of the header.
     * <p>
     * <b>Note:</b> By default, authorize it since license part should always be
     * generated by the plugin.
     *
     * @since 1.0
     */
    @Parameter( property = "license.canUpdateLicense", defaultValue = "true" )
    private boolean canUpdateLicense;

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

    /**
     * A flag to add the license header in java files after the package statement.
     * <p>
     * This is a practice used by many people (apache, codehaus, ...).
     * <p>
     * <b>Note:</b> By default this property is then to {@code true} since it is a good practice.
     *
     * @since 1.2
     */
    @Parameter( property = "license.addJavaLicenseAfterPackage", defaultValue = "true" )
    private boolean addJavaLicenseAfterPackage;

    /**
     * A flag to use for java comment start tag with no reformat syntax {@code /*-}.
     * <p>
     * See http://www.oracle.com/technetwork/java/javase/documentation/codeconventions-141999.html#350
     *
     * @since 1.9
     */
    @Parameter( property = "license.useJavaNoReformatCommentStartTag", defaultValue = "true" )
    private boolean useJavaNoReformatCommentStartTag;

    /**
     * A flag to indicate if there should be an empty line after the header.
     * <p>
     * Checkstyle requires empty line between license header and package statement.
     * If you are using addJavaLicenseAfterPackage=false it could make sense to set this to true.
     * </p>
     * <b>Note:</b> By default this property is set to {@code false} to keep old behavior.
     *
     * @since 1.9
     */
    @Parameter( property = "license.emptyLineAfterHeader", defaultValue = "false" )
    private boolean emptyLineAfterHeader;

    /**
     * A flag to indicate if there should be an empty line after the header.
     * <p>
     * Checkstyle usually requires no trailing whitespace.
     * If it is the case it could make sense to set this to true
     * </p>
     * <b>Note:</b> By default this property is set to {@code false} to keep old behavior.
     *
     * @since 1.14
     */
    @Parameter( property = "license.trimHeaderLine", defaultValue = "false" )
    private boolean trimHeaderLine;

    /**
     * A flag to ignore no files to scan.
     * <p>
     * This flag will suppress the "No file to scan" warning. This will allow you to set the plug-in in the root pom of
     * your project without getting a lot of warnings for aggregation modules / artifacts.
     * </p>
     * <b>Note:</b> By default this property is set to {@code false} to keep old behavior.
     *
     * @since 1.9
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
     * <p>
     * <b>Note:</b> This parameter is not useable if you are still using a project file descriptor.
     *
     * @since 1.0
     */
    @Parameter( property = "license.roots" )
    private String[] roots;

    /**
     * Specific files to includes, separated by a comma. By default, it is "** /*".
     * <p>
     * <b>Note:</b> This parameter is not usable if you are still using a project file descriptor.
     *
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * Template used to build the description section of the license header.
     * <p>
     * (This template use freemarker).
     *
     * @since 1.1
     */
    @Parameter( property = "license.descriptionTemplate",
            defaultValue = "/org/codehaus/mojo/license/default-file-header-description.ftl" )
    private String descriptionTemplate;

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
     * The dictionary of extension indexed by their associated comment style.
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
     * Dictionary of treated files indexed by their state.
     */
    EnumMap<FileState, Set<File>> result;

    /**
     * Dictionary of files to treat indexed by their CommentStyle.
     */
    private Map<String, List<File>> filesToTreatByCommentStyle;

    /**
     * Freemarker helper component.
     *
     * @since 1.0
     */
    private FreeMarkerHelper freeMarkerHelper = FreeMarkerHelper.newDefaultHelper();

    // ----------------------------------------------------------------------
    // Abstract Methods
    // ----------------------------------------------------------------------

    /**
     * @return {@code true} if mojo must be a simple dry run (says do not modifiy any scanned files), {@code false}
     *      otherise.
     */
    protected abstract boolean isDryRun();

    /**
     * @return {@code true} if mojo should fails if dryRun and there is some missing license header, {@code false}
     *      otherwise.
     */
    protected abstract boolean isFailOnMissingHeader();


    /**
     * @return {@code true} if mojo should fails if dryRun and there is some obsolete license header, {@code false}
     *      otherwise.
     */
    protected abstract boolean isFailOnNotUptodateHeader();

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
        if ( !isDryRun() )
        {
            if ( isFailOnMissingHeader() )
            {
                LOG.warn( "The failOnMissingHeader has no effect if the property dryRun is not set." );
            }

            if ( isFailOnNotUptodateHeader() )
            {
                LOG.warn( "The failOnNotUptodateHeader has no effect if the property dryRun is not set." );
            }
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

        extensionToCommentStyle = new TreeMap<>();

        processStartTag = cleanHeaderConfiguration( processStartTag, FileHeaderTransformer.DEFAULT_PROCESS_START_TAG );
        if ( isVerbose() )
        {
            LOG.info( "Will use processStartTag: {}", processStartTag );
        }
        processEndTag = cleanHeaderConfiguration( processEndTag, FileHeaderTransformer.DEFAULT_PROCESS_END_TAG );
        if ( isVerbose() )
        {
            LOG.info( "Will use processEndTag: {}", processEndTag );
        }
        sectionDelimiter = cleanHeaderConfiguration( sectionDelimiter,
                FileHeaderTransformer.DEFAULT_SECTION_DELIMITER );
        if ( isVerbose() )
        {
            LOG.info( "Will use sectionDelimiter: {}", sectionDelimiter );
        }

        // add default extensions from header transformers
        for ( Map.Entry<String, FileHeaderTransformer> entry : transformers.entrySet() )
        {
            String commentStyle = entry.getKey();
            FileHeaderTransformer aTransformer = entry.getValue();

            aTransformer.setProcessStartTag( processStartTag );
            aTransformer.setProcessEndTag( processEndTag );
            aTransformer.setSectionDelimiter( sectionDelimiter );
            aTransformer.setEmptyLineAfterHeader( emptyLineAfterHeader );
            aTransformer.setTrimHeaderLine( trimHeaderLine );
            aTransformer.setLineSeparator( lineSeparator );

            if ( aTransformer instanceof JavaFileHeaderTransformer )
            {
                JavaFileHeaderTransformer javaFileHeaderTransformer = (JavaFileHeaderTransformer) aTransformer;

                javaFileHeaderTransformer.setAddJavaLicenseAfterPackage( addJavaLicenseAfterPackage );
                javaFileHeaderTransformer.setUseNoReformatCommentStartTag( useJavaNoReformatCommentStartTag );
            }

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
        // build the description template
        if ( isVerbose() )
        {
            LOG.info( "Use description template: {}", descriptionTemplate );
        }
        descriptionTemplate0 = freeMarkerHelper.getTemplate( descriptionTemplate );
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
            checkResults( result );

            int nbFiles = processedFiles.size();
            if ( nbFiles == 0 && !ignoreNoFileToScan )
            {
                LOG.warn( "No file to scan." );
            }
            else
            {
                String delay = MojoHelper.convertTime( System.nanoTime() - t0 );
                String message =
                        String.format( "Scan %s file%s header done in %s.", nbFiles, nbFiles > 1 ? "s" : "", delay );
                LOG.info( message );
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

                LOG.info( buffer.toString() );
            }

        }
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    /**
     * Checks the results of the mojo execution using the {@link #isFailOnMissingHeader()} and
     * {@link #isFailOnNotUptodateHeader()}.
     *
     * @param result processed files by their status
     * @throws MojoFailureException if check is not ok (some file with no header or to update)
     */
    private void checkResults( EnumMap<FileState, Set<File>> result ) throws MojoFailureException
    {
        Set<FileState> states = result.keySet();

        StringBuilder builder = new StringBuilder();
        if ( isDryRun() && isFailOnMissingHeader() && states.contains( FileState.add ) )
        {
            List<File> files = FileUtil.orderFiles( result.get( FileState.add ) );

            builder.append( "There are " ).append( files.size() ).append( " file(s) with no header :" );
            for ( File file : files )
            {
                builder.append( "\n" ).append( file );
            }
        }

        if ( isDryRun() && isFailOnNotUptodateHeader() && states.contains( FileState.update ) )
        {
            List<File> files = FileUtil.orderFiles( result.get( FileState.update ) );

            builder.append( "\nThere are " ).append( files.size() ).append( " file(s) with header to update:" );
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
        FileHeaderProcessor processor = getFileHeaderProcessor( license, transformer );


        for ( File file : filesToTreat )
        {
            processFile( processor, file );
        }
        filesToTreat.clear();
    }

    private FileHeaderProcessor getFileHeaderProcessor( License license, FileHeaderTransformer transformer )
            throws IOException
    {
        // file header to use if no header is found on a file
        FileHeader header = new FileHeader();

        if ( inceptionYear == null )
        {
            LOG.warn( "No inceptionYear defined (will use current year)" );
        }

        Copyright copyright = getCopyright( getCopyrightOwners() );
        header.setCopyright( copyright );

        String licenseContent = license.getHeaderContent( getEncoding() );
        if ( license.isHeaderContentTemplateAware() )
        {
            licenseContent = processLicenseContext( licenseContent );
        }
        header.setLicense( licenseContent );

        UpdateFileHeaderFilter filter = new UpdateFileHeaderFilter();
        filter.setUpdateCopyright( canUpdateCopyright );
        filter.setUpdateDescription( canUpdateDescription );
        filter.setUpdateLicense( canUpdateLicense );

        // update processor filter
        return new FileHeaderProcessor( filter, header, transformer );
    }

    /**
     * Process the given file (will copy it, process the clone file and finally finalizeFile after process)...
     *
     * @param processor current file processor
     * @param file      original file to process
     * @throws IOException if any IO error while processing this file
     */
    private void processFile( FileHeaderProcessor processor, File file ) throws IOException
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
            doFinalize = processFile( processor, file, processFile );
        }
        catch ( Exception e )
        {
            LOG.warn( "skip failed file: " + e.getMessage()
                                   + ( e.getCause() == null ? "" : " Cause : " + e.getCause().getMessage() ), e );
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
     * Process the given {@code file} and save the result in the given {@code processFile}.
     *
     * @param processor   current file processor
     * @param file        the file to process
     * @param processFile the output processed file
     * @return {@code true} if prepareProcessFile can be finalize, otherwise need to be delete
     * @throws java.io.IOException if any pb while treatment
     */
    private boolean processFile( FileHeaderProcessor processor, File file, File processFile ) throws IOException
    {

        if ( getLog().isDebugEnabled() )
        {
            LOG.debug( " - process file {}", file );
            LOG.debug( " - will process into file {}", processFile );
        }

        // update the file header description
        Map<String, Object> descriptionParameters = new HashMap<>();
        descriptionParameters.put( "project", getProject() );
        descriptionParameters.put( "addSvnKeyWords", addSvnKeyWords );
        descriptionParameters.put( "projectName", projectName );
        descriptionParameters.put( "inceptionYear", inceptionYear );
        descriptionParameters.put( "organizationName", organizationName );
        descriptionParameters.put( "file", file );

        LOG.debug( "Description parameters: {}", descriptionParameters );

        String description = freeMarkerHelper.renderTemplate( descriptionTemplate0, descriptionParameters );
        processor.updateDescription( description );

        LOG.debug( "header description : " + processor.getFileHeaderDescription() );

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
            LOG.info( " - ignore file (detected {}) {}", ignoreTag, file );

            FileState.ignore.addFile( file, result );

            return false;
        }

        // process file to detect header

        try
        {
            processor.process( content, processFile, getEncoding() );
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
                LOG.info( " - header was updated for {}", file );
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
        if ( isVerbose() )
        {
            LOG.info( " - adding license header on file {}", file );
        }

        //FIXME tchemit 20100409 xml files must add header after a xml prolog line
        content = processor.addHeader( content );

        if ( !isDryRun() )
        {
            FileUtil.printString( processFile, content, getEncoding() );
        }

        FileState.add.addFile( file, result );
        return true;
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
            Files.copy( file.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES );
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
                String updatedContent = FileUtil.readAsString( processFile, getEncoding() );
                FileUtil.printString( file, updatedContent, getEncoding() );
                FileUtil.deleteFile( processFile );
            }
            catch ( IOException e )
            {
                LOG.warn( "Error updating {} -> {}", processFile, file, e );
            }
        }
    }

}
