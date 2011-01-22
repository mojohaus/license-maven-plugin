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

package org.codehaus.mojo.license.header;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.license.header.transformer.FileHeaderTransformer;
import org.nuiton.processor.filters.DefaultFilter;

/**
 * File Header filter.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 2.1
 */
public abstract class FileHeaderFilter
    extends DefaultFilter
{

    /**
     * flag set to {@code true} when a header was detected (says detects both
     * start and end process tags).
     */
    protected boolean touched;

    /**
     * flag set to {@code true} when a header was detected and was modified.
     */
    protected boolean modified;

    /**
     * flag set to {@code true} as soon as start process tag was detected.
     */
    protected boolean detectHeader;

    /**
     * incoming default file header model
     */
    protected FileHeader fileHeader;

    /**
     * header transformer
     */
    protected FileHeaderTransformer transformer;

    /**
     * cached header content
     */
    protected String headerContent;

    /**
     * cached full header content (with process tag + comment box)
     */
    protected String processTagHeaderContent;

    /**
     * cached full header content (with process tag + comment box)
     */
    protected String fullHeaderContent;

    /**
     * maven logger
     */
    protected Log log;

    /**
     * Obtains the new header to use according to the old one.
     * <p/>
     * <b>Note:</b> If the new header should not be updated, then the result is {@code null}.
     *
     * @param oldHeader the old header found in file.
     * @return {@code null} if header is still the same, otherwise the new header to apply
     * @since 2.3.2
     */
    protected abstract FileHeader getNewHeader( FileHeader oldHeader );

    /**
     * The logic to implements when a header was touched.
     *
     * @param ch the actual header content
     * @return the new header content (beginning by a
     *         {@link FileHeaderTransformer#getProcessStartTag()} and ending
     *         with a {@link FileHeaderTransformer#getProcessEndTag()}).
     * @deprecated since 2.3.2, use now the method {@link #getNewHeader(FileHeader)}.
     */
    @Deprecated
    protected abstract String treateHeader( String ch );

    public FileHeaderFilter()
    {
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    @Override
    protected String performInFilter( String ch )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "performInFilter - original header =\n" + ch );
        }
        if ( isTouched() )
        {
            // Can NOT authorize two header in a same source
            throw new IllegalStateException( "Can only have one file header start tag : " + getHeader() );
        }
        if ( getMatchIndexFor( ch, getHeader() ) == NOT_FOUND )
        {

            // the header was detected, mark file to be touched
            touched = true;

            // obtain old header model
            FileHeaderTransformer headerTransformer = getTransformer();
            String tmp = headerTransformer.unboxComent( ch );
            FileHeader oldHeader = headerTransformer.toFileHeader( tmp );

            // obtain the new header (according to what to update)
            FileHeader newFileHeader = getNewHeader( oldHeader );

            FileHeader header;

            if ( newFileHeader == null )
            {

                // keep the old header
                header = oldHeader;

            }
            else
            {

                // mark that the header was updated
                modified = true;

                header = newFileHeader;
            }

            String result = transformer.toHeaderContent( header );
            return result;

//            // update header
//            String newHeader = treateHeader(ch);
//            if (log.isDebugEnabled()) {
//                log.debug("old header =\n" + ch);
//                log.debug("new header =\n" + newHeader);
//            }
//            if (!newHeader.contains(ch.trim())) {
//                // this is a weak test, but should be more enough
//                // if the new header contains the old one, it would be
//                // bad luck to not have equality ?
//                //TODO-TC-20100415 : use an exact test (must remove process tag on new header)
////            if (!ch.equals(newHeader)) {
//
//                // header content was modified, mark it
//                modified = true;
//            }
//
//            return newHeader;
        }
        // Means we detects the process start tag but not the end one.
        // coming then here from the flush filter method... So changes nothing
        // just return the text as it comes.
        return ch;
    }

    @Override
    protected String performOutFilter( String ch )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( ch );
        }
        return ch;
    }

    @Override
    protected String getHeader()
    {
        return getTransformer().getProcessStartTag();
    }

    @Override
    protected String getFooter()
    {
        return getTransformer().getProcessEndTag();
    }

    @Override
    protected void changeState( State newState )
    {
        super.changeState( newState );
        if ( newState == State.SEARCH_FOOTER )
        {
            // on a decouvert un header
            detectHeader = true;
        }
    }

    public String getHeaderContent()
    {
        if ( headerContent == null )
        {
            headerContent = getTransformer().toString( getFileHeader() );
        }
        return headerContent;
    }

    public String getProcessTagHeaderContent()
    {
        if ( processTagHeaderContent == null )
        {

            // box with process tag
            processTagHeaderContent = getTransformer().boxProcessTag( getHeaderContent() );

        }
        return processTagHeaderContent;
    }

    public String getFullHeaderContent()
    {
        if ( fullHeaderContent == null )
        {

            // box with comment
            fullHeaderContent = getTransformer().boxComment( getProcessTagHeaderContent(), true );
        }
        return fullHeaderContent;
    }

    public boolean isTouched()
    {
        return touched;
    }

    public boolean isModified()
    {
        return modified;
    }

    public boolean isDetectHeader()
    {
        return detectHeader;
    }

    public FileHeader getFileHeader()
    {
        return fileHeader;
    }

    public FileHeaderTransformer getTransformer()
    {
        return transformer;
    }

    public void setFileHeader( FileHeader fileHeader )
    {
        this.fileHeader = fileHeader;
    }

    public void setTransformer( FileHeaderTransformer transformer )
    {
        this.transformer = transformer;
    }

    public void reset()
    {
        touched = false;
        modified = false;
        detectHeader = false;
        state = State.SEARCH_HEADER;
    }

    public void resetContent()
    {
        headerContent = null;
        processTagHeaderContent = null;
        fullHeaderContent = null;
    }
}
