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


import org.apache.commons.lang.StringUtils;
import org.codehaus.mojo.license.header.FileHeader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract implementation of {@link FileHeaderTransformer}.
 * <p/>
 * Concrete implementation should only have to give comment configuration.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public abstract class AbstractFileHeaderTransformer
    implements FileHeaderTransformer
{

    /**
     * pattern of the copyright string representation :
     * <ul>
     * <li>group(1) is Copyright prefix</li>
     * <li>group(2) is Copyright first year</li>
     * <li>group(3) is Copyright last year with prefix (can be null)</li>
     * <li>group(4) is Copyright last year (can be null)</li>
     * <li>group(5) is Copyright holder</li>
     * </ul>
     */
    protected static final Pattern COPYRIGHT_PATTERN =
        Pattern.compile( "(.[^\\d]+)?\\s(\\d{4})?(\\s+-\\s+(\\d{4})?){0,1}\\s+(.+)?" );

    /**
     * name of transformer
     */
    protected String name;

    /**
     * description of transfomer
     */
    protected String description;

    /**
     * section delimiter
     */
    protected String sectionDelimiter = DEFAULT_SECTION_DELIMITER;

    /**
     * start process tag
     */
    protected String processStartTag = DEFAULT_PROCESS_START_TAG;

    /**
     * end process tag
     */
    protected String processEndTag = DEFAULT_PROCESS_END_TAG;

    /**
     * comment start tag
     */
    protected String commentStartTag;

    /**
     * comment end tag
     */
    protected String commentEndTag;

    /**
     * comment line prefix (to add for header content)
     */
    protected String commentLinePrefix;

    protected AbstractFileHeaderTransformer( String name, String description, String commentStartTag,
                                             String commentEndTag, String commentLinePrefix )
    {
        this.name = name;
        this.description = description;

        // checks comment start tag is different from comment prefix
        if ( commentStartTag.equals( commentLinePrefix ) )
        {
            throw new IllegalStateException(
                "commentStartTag can not be equals to commentPrefixLine, " + "but was [" + commentStartTag + "]" );
        }

        // checks comment end tag is different from comment prefix
        if ( commentEndTag.equals( commentLinePrefix ) )
        {
            throw new IllegalStateException(
                "commentEndTag can not be equals to commentPrefixLine, " + "but was [" + commentEndTag + "]" );
        }

        this.commentStartTag = commentStartTag;
        this.commentEndTag = commentEndTag;
        this.commentLinePrefix = commentLinePrefix;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getSectionDelimiter()
    {
        return sectionDelimiter;
    }

    public void setSectionDelimiter( String sectionDelimiter )
    {
        this.sectionDelimiter = sectionDelimiter;
    }

    public String getProcessStartTag()
    {
        return processStartTag;
    }

    public void setProcessStartTag( String processStartTag )
    {
        this.processStartTag = processStartTag;
    }

    public String getProcessEndTag()
    {
        return processEndTag;
    }

    public void setProcessEndTag( String processEndTag )
    {
        this.processEndTag = processEndTag;
    }

    public String getCommentStartTag()
    {
        return commentStartTag;
    }

    public void setCommentStartTag( String commentStartTag )
    {
        this.commentStartTag = commentStartTag;
    }

    public String getCommentEndTag()
    {
        return commentEndTag;
    }

    public void setCommentEndTag( String commentEndTag )
    {
        this.commentEndTag = commentEndTag;
    }

    public String getCommentLinePrefix()
    {
        return commentLinePrefix;
    }

    public String addHeader( String header, String content )
    {
        return header + content;
    }

    public void setCommentLinePrefix( String commentLinePrefix )
    {
        this.commentLinePrefix = commentLinePrefix;
    }

    public FileHeader toFileHeader( String header )
    {
        FileHeader model = new FileHeader();

        String[] sections = header.split( getSectionDelimiter() );
        if ( sections.length != 3 )
        {
            throw new IllegalStateException( "could not find 3 sections in\n" + header );
        }

        // first section is the description
        String description = sections[0].trim();
        model.setDescription( description );

        // second section is the copyright
        String copyright = sections[1].trim();
        Matcher matcher = COPYRIGHT_PATTERN.matcher( copyright );
        if ( !matcher.matches() )
        {
            throw new IllegalStateException( "copyright [" + copyright + "] is not valid" );
        }
        String firstYear = matcher.group( 2 );
        String lastYear = matcher.group( 4 );
        String holder = matcher.group( 5 );
        model.setCopyrightFirstYear( Integer.valueOf( firstYear.trim() ) );
        if ( lastYear != null )
        {
            model.setCopyrightLastYear( Integer.valueOf( lastYear.trim() ) );
        }
        model.setCopyrightHolder( holder.trim() );

        // third section is the license
        String license = sections[2].trim();
        model.setLicense( license );
        return model;
    }

    public String toString( FileHeader model )
        throws NullPointerException
    {
        if ( model == null )
        {
            throw new NullPointerException( "model can not be null!" );
        }
        StringBuilder buffer = new StringBuilder();

        String sectionDelimiter = LINE_SEPARATOR + getSectionDelimiter() + LINE_SEPARATOR;

        // add description section
        buffer.append( model.getDescription().trim() );
        buffer.append( sectionDelimiter );

        // add copyright section
        buffer.append( model.getCopyright().trim() );
        buffer.append( sectionDelimiter );

        // add license section
        buffer.append( model.getLicense().trim() ).append( LINE_SEPARATOR );
        return buffer.toString();
    }

    public String toHeaderContent( FileHeader model )
        throws NullPointerException
    {

        String result;

        // model to text
        result = toString( model );

        // box with process tag
        result = boxProcessTag( result );

        // box header with comment prefix
        result = boxComment( result, false );

        // remove all before process start tag
        // remove all after process end tag
        // this is a requirement for processor to respect involution.
        int index = result.indexOf( getProcessStartTag() );
        int lastIndex = result.lastIndexOf( getProcessEndTag() ) + getProcessEndTag().length();

        result = result.substring( index, lastIndex );
        return result;
    }

    public String boxComment( String header, boolean withTags )
    {
        StringBuilder buffer = new StringBuilder();
        if ( withTags )
        {
            buffer.append( getCommentStartTag() ).append( LINE_SEPARATOR );
        }
        for ( String line : header.split( LINE_SEPARATOR + "" ) )
        {
            buffer.append( getCommentLinePrefix() );
            buffer.append( line );
            buffer.append( LINE_SEPARATOR );
        }
        if ( withTags )
        {
            buffer.append( getCommentEndTag() ).append( LINE_SEPARATOR );
        }
        return buffer.toString();
    }

    public String unboxComent( String header )
    {
        StringBuilder buffer = new StringBuilder();
        int prefixLength = getCommentLinePrefix().length();
        for ( String line : header.split( LINE_SEPARATOR + "" ) )
        {
            if ( StringUtils.isEmpty( line ) || line.contains( getCommentStartTag() ) ||
                line.contains( getCommentEndTag() ) )
            {

                // not be unboxed, but just skipped
                continue;
            }
            int index = line.indexOf( getCommentLinePrefix() );
            if ( index > -1 )
            {

                // remove comment prefix
                line = line.substring( index + prefixLength );
            }
            else
            {
                String s = getCommentLinePrefix().trim();
                if ( line.startsWith( s ) )
                {
                    if ( line.length() <= s.length() )
                    {
                        line = "";
                    }
                }
                else
                {
                    line = line.substring( s.length() );
                }
            }
            buffer.append( line ).append( LINE_SEPARATOR );
        }
        return buffer.toString();
    }

    public String boxProcessTag( String header )
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( getProcessStartTag() ).append( LINE_SEPARATOR );
        buffer.append( header.trim() ).append( LINE_SEPARATOR );
        buffer.append( getProcessEndTag() ).append( LINE_SEPARATOR );
        return buffer.toString();
    }

    public String unboxProcessTag( String boxedHeader )
    {
        StringBuilder buffer = new StringBuilder();
        for ( String line : boxedHeader.split( LINE_SEPARATOR + "" ) )
        {
            if ( StringUtils.isEmpty( line ) || line.contains( getProcessStartTag() ) ||
                line.contains( getProcessEndTag() ) )
            {

                // not be unboxed, but just skipped
                continue;
            }
            buffer.append( line ).append( LINE_SEPARATOR );
        }
        return buffer.toString();
    }

    public boolean isDescriptionEquals( FileHeader header1, FileHeader header2 )
    {
        return header1.getDescription().equals( header2.getDescription() );
    }

    public boolean isCopyrightEquals( FileHeader header1, FileHeader header2 )
    {
        return header1.getCopyright().equals( header2.getCopyright() );
    }

    public boolean isLicenseEquals( FileHeader header1, FileHeader header2 )
    {
        String license1 = removeSpaces( header1.getLicense() );
        String license2 = removeSpaces( header2.getLicense() );
        return license1.equals( license2 );
    }

    protected static final Pattern REMOVE_SPACE_PATTERN = Pattern.compile( "(\\s+)" );

    protected String removeSpaces( String str )
    {
        Matcher matcher = REMOVE_SPACE_PATTERN.matcher( str );
        String result;
        if ( matcher.find() )
        {
            result = matcher.replaceAll( "" );
        }
        else
        {
            result = str;
        }
        return result;
    }
}
