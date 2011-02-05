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

package org.codehaus.mojo.license.header.transformer;


import org.codehaus.mojo.license.header.FileHeader;

/**
 * Contract to transform a file header to {@link FileHeader} in both way :
 * <p/>
 * <ul>
 * <li>Obtain a object representation of a file header from a existing file
 * (String to FileHeader).</li>
 * <li>Obtain the file header to inject in a file from a FileHeader (to update
 * or create a file header from the file header model).
 * </ul>
 * <p/>
 * Moreover the contract offers two methods to box and unbox a String
 * representation of a header content :
 * <ul>
 * <li>{@link #boxComment(String, boolean)}</li>
 * <li>{@link #unboxComent(String)}</li>
 * </ul>
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public interface FileHeaderTransformer
{

    /**
     * Plexus component role
     */
    String ROLE_NAME = FileHeaderTransformer.class.getName();

    /**
     * default section delimiter
     */
    String DEFAULT_SECTION_DELIMITER = "%" + "%";

    /**
     * default process start tag
     */
    String DEFAULT_PROCESS_START_TAG = "#" + "%" + "L";

    /**
     * default process end tag
     */
    String DEFAULT_PROCESS_END_TAG = "#" + "L" + "%";

    char LINE_SEPARATOR = '\n';

    /**
     * @return the name of the transformer
     */
    String getName();

    /**
     * @return the description of the transformer
     */
    String getDescription();

    /**
     * Get the default accepted extensions for this transformer.
     *
     * @return the default accepted extensions.
     */
    String[] getDefaultAcceptedExtensions();

    /**
     * Obtains the process tag which indicates the begin of the header content.
     * <p/>
     * By default, (says if you do not explicitly invoke the
     * {@link #setProcessStartTag(String)} method), will use the
     * {@link #DEFAULT_PROCESS_START_TAG}
     *
     * @return the starting header tag
     */
    String getProcessStartTag();

    /**
     * Obtain the process tag which indiciates the end of the header content.
     * <p/>
     * By default, (says if you do not explicitly invoke the
     * {@link #setProcessEndTag(String)} method), will use the
     * {@link #DEFAULT_PROCESS_END_TAG}.
     *
     * @return the ending header tag
     */
    String getProcessEndTag();

    /**
     * The pattern used to separate sections of the header.
     * <p/>
     * By default, (says if you do not explicitly invoke the
     * {@link #setSectionDelimiter(String)} method), will use the
     * {@link #DEFAULT_SECTION_DELIMITER}.
     *
     * @return the delimiter used to separate sections in the header.
     */
    String getSectionDelimiter();

    /**
     * @return the start tag of a comment
     */
    String getCommentStartTag();

    /**
     * @return the end tag of a comment
     */
    String getCommentEndTag();

    /**
     * @return the line prefix of every line insed the comment
     */
    String getCommentLinePrefix();

    /**
     * Adds the header.
     *
     * @param header  header to add
     * @param content content of original file
     * @return the new full file content beginning with header
     */
    String addHeader( String header, String content );

    /**
     * Box the given {@code header} in a comment.
     *
     * @param header   the header content WITHOUT any comment boxing
     * @param withTags flag to add start and end comment tags.
     * @return the header content WITH comment boxing
     */
    String boxComment( String header, boolean withTags );

    /**
     * Unbox the given boxed {@code boxedHeader} to obtain the header content.
     *
     * @param boxedHeader the boxed header
     * @return the unboxed header.
     */
    String unboxComent( String boxedHeader );

    /**
     * Box the given {@code header} between process tags.
     *
     * @param header the header content WITHOUT any comment boxing
     * @return the header content boxed between process tags
     * @see #getProcessStartTag()
     * @see #getProcessEndTag()
     */
    String boxProcessTag( String header );

    /**
     * Unbox the process tag on the given boxed {@code boxedHeader} to obtain
     * the brute header content.
     *
     * @param boxedHeader the boxed header
     * @return the brute header content.
     * @see #getProcessStartTag()
     * @see #getProcessEndTag()
     */
    String unboxProcessTag( String boxedHeader );

    /**
     * Build a {@link FileHeader} from an UNBOXED header content.
     *
     * @param header unboxed header content
     * @return The model of the header content
     */
    FileHeader toFileHeader( String header );

    /**
     * Build a UNBOXED header content from the given {@code model}.
     *
     * @param model the model of the file header
     * @return the UNBOXED header content
     * @throws NullPointerException if model is null
     */
    String toString( FileHeader model )
        throws NullPointerException;

    /**
     * Build a fully boxed header content from the given {@code model}.
     *
     * @param model the model of the file header
     * @return the fully boxed header content
     * @throws NullPointerException if model is null
     */
    String toHeaderContent( FileHeader model )
        throws NullPointerException;

    /**
     * Tests if the description of the two models are equals.
     *
     * @param header1 the first header
     * @param header2 the second header
     * @return {@code true} if headers description are stricly the same
     */
    boolean isDescriptionEquals( FileHeader header1, FileHeader header2 );

    /**
     * Tests if the copyright of the two models are equals.
     *
     * @param header1 the first header
     * @param header2 the second header
     * @return {@code true} if headers copyright are stricly the same
     */
    boolean isCopyrightEquals( FileHeader header1, FileHeader header2 );

    /**
     * Tests if the license of the two models are equals.
     *
     * @param header1 the first header
     * @param header2 the second header
     * @return {@code true} if headers license are stricly the same (WITHOUT ANY space)
     */
    boolean isLicenseEquals( FileHeader header1, FileHeader header2 );

    /**
     * Changes the name of the transformer.
     *
     * @param name the new name of the transformer
     */
    void setName( String name );

    /**
     * Chages the description of the transformer.
     *
     * @param description the new description of the transformer
     */
    void setDescription( String description );

    /**
     * Sets the header section delimiter.
     * <p/>
     * By default, will use the {@link #DEFAULT_SECTION_DELIMITER}.
     *
     * @param headerSectionDelimiter the new delimiter
     */
    void setSectionDelimiter( String headerSectionDelimiter );

    /**
     * Changes the process start tag.
     *
     * @param tag the new start tag
     */
    void setProcessStartTag( String tag );

    /**
     * Changes the process end tag.
     *
     * @param tag the new endtag
     */
    void setProcessEndTag( String tag );

    /**
     * Changes the comment start tag.
     *
     * @param commentStartTag the new comment start tag
     */
    void setCommentStartTag( String commentStartTag );

    /**
     * Changes the comment end tag.
     *
     * @param commentEndTag the new comment end tag
     */
    void setCommentEndTag( String commentEndTag );

    /**
     * Changes the comment prefix line.
     *
     * @param commentLinePrefix the new comment prefix line
     */
    void setCommentLinePrefix( String commentLinePrefix );
}
