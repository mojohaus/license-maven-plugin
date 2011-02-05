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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.regex.Matcher;

import static org.codehaus.mojo.license.header.transformer.AbstractFileHeaderTransformer.COPYRIGHT_PATTERN;
import static org.codehaus.mojo.license.header.transformer.FileHeaderTransformer.DEFAULT_PROCESS_END_TAG;
import static org.codehaus.mojo.license.header.transformer.FileHeaderTransformer.DEFAULT_PROCESS_START_TAG;
import static org.codehaus.mojo.license.header.transformer.FileHeaderTransformer.DEFAULT_SECTION_DELIMITER;
import static org.codehaus.mojo.license.header.transformer.FileHeaderTransformer.LINE_SEPARATOR;
import static org.codehaus.mojo.license.header.transformer.JavaFileHeaderTransformer.*;

/**
 * Tests the {@link AbstractFileHeaderTransformer}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class AbstractFileHeaderTransformerTest
{

    FileHeaderTransformer transformer;

    FileHeader model1, model2;

    @Before
    public void setUp()
        throws Exception
    {
        transformer = new JavaFileHeaderTransformer();

        model1 = new FileHeader();
        model1.setDescription( "Description" );
        model1.setCopyrightFirstYear( 2010 );
        model1.setCopyrightLastYear( null );
        model1.setCopyrightHolder( "Tony" );
        model1.setLicense( "License" );

        model2 = new FileHeader();
        model2.setDescription( "Description2" );
        model2.setCopyrightFirstYear( 2010 );
        model2.setCopyrightLastYear( 2012 );
        model2.setCopyrightHolder( "Tony2" );
        model2.setLicense( "License2" );
    }

    @After
    public void tearDown()
        throws Exception
    {
        transformer = null;
        model1 = null;
        model2 = null;
    }


    @Test( expected = IllegalStateException.class )
    public void testIllegalTransformer()
        throws Exception
    {

        new AbstractFileHeaderTransformer( "name", "description", "commentPrefix", "commentEndtag", "commentPrefix" )
        {

            public String[] getDefaultAcceptedExtensions()
            {
                return new String[]{ getName() };
            }
        };
    }

    @Test( expected = IllegalStateException.class )
    public void testIllegalTransformer2()
        throws Exception
    {

        new AbstractFileHeaderTransformer( "name", "description", "commentstartTag", "commentPrefix", "commentPrefix" )
        {

            public String[] getDefaultAcceptedExtensions()
            {
                return new String[]{ getName() };
            }
        };
    }

    @Test
    public void testCopyrightPattern()
        throws Exception
    {
        String actual;
        Matcher matcher;
        String prefix;
        String firstYear;
        String lastYear;
        String holder;

        actual = "Copyright (C) 2010 Tony";
        matcher = COPYRIGHT_PATTERN.matcher( actual );
        Assert.assertTrue( matcher.matches() );
        Assert.assertEquals( 5, matcher.groupCount() );
        prefix = matcher.group( 1 );
        Assert.assertEquals( "Copyright (C)", prefix );
        firstYear = matcher.group( 2 );
        Assert.assertEquals( "2010", firstYear );
        lastYear = matcher.group( 4 );
        Assert.assertEquals( null, lastYear );
        holder = matcher.group( 5 );
        Assert.assertEquals( "Tony", holder );

        actual = "Copyright (C) 2010 - 2012    Tony";
        matcher = COPYRIGHT_PATTERN.matcher( actual );
        Assert.assertTrue( matcher.matches() );
        Assert.assertEquals( 5, matcher.groupCount() );
        prefix = matcher.group( 1 );
        Assert.assertEquals( "Copyright (C)", prefix );
        firstYear = matcher.group( 2 );
        Assert.assertEquals( "2010", firstYear );
        lastYear = matcher.group( 4 );
        Assert.assertEquals( "2012", lastYear );
        holder = matcher.group( 5 );
        Assert.assertEquals( "Tony", holder );
    }

    @Test
    public void testToFileHeader()
        throws Exception
    {
        String header;
        FileHeader model;

        header =
            "Description" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + "Copyright (C) 2010 Tony" +
                LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + "License";
        model = transformer.toFileHeader( header );
        Assert.assertNotNull( model );
        assertEquals( model1, model );

        header = "Description2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR +
            "Copyright (C) 2010 - 2012 Tony2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR +
            "License2";
        model = transformer.toFileHeader( header );
        Assert.assertNotNull( model );
        assertEquals( model2, model );
    }

    @Test
    public void testToString()
        throws Exception
    {
        String header;

        header = transformer.toString( model1 );
        Assert.assertEquals(
            "Description" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + "Copyright (C) 2010 Tony" +
                LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + "License" + LINE_SEPARATOR, header );

        header = transformer.toString( model2 );
        Assert.assertEquals( "Description2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR +
                                 "Copyright (C) 2010 - 2012 Tony2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER +
                                 LINE_SEPARATOR + "License2" + LINE_SEPARATOR, header );
    }

    @Test
    public void testBoxProcessTag()
        throws Exception
    {
        String header;
        String boxedHeader;

        header = transformer.toString( model1 );
        boxedHeader = transformer.boxProcessTag( header );
        Assert.assertEquals(
            DEFAULT_PROCESS_START_TAG + LINE_SEPARATOR + "Description" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + "Copyright (C) 2010 Tony" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + "License" + LINE_SEPARATOR + DEFAULT_PROCESS_END_TAG + LINE_SEPARATOR, boxedHeader );

        header = transformer.toString( model2 );
        boxedHeader = transformer.boxProcessTag( header );
        Assert.assertEquals(
            DEFAULT_PROCESS_START_TAG + LINE_SEPARATOR + "Description2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + "Copyright (C) 2010 - 2012 Tony2" + LINE_SEPARATOR + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + "License2" + LINE_SEPARATOR + DEFAULT_PROCESS_END_TAG + LINE_SEPARATOR, boxedHeader );
    }


    @Test
    public void testUnboxProcessTag()
        throws Exception
    {
        String header;
        String boxedHeader;
        String unboxedHeader;

        header = transformer.toString( model1 );
        boxedHeader = transformer.boxProcessTag( header );
        unboxedHeader = transformer.unboxProcessTag( boxedHeader );

        Assert.assertEquals( header, unboxedHeader );

        header = transformer.toString( model2 );
        boxedHeader = transformer.boxProcessTag( header );
        unboxedHeader = transformer.unboxProcessTag( boxedHeader );

        Assert.assertEquals( header, unboxedHeader );
    }

    @Test
    public void testBoxComment()
        throws Exception
    {
        String header;
        String boxedHeader;

        header = transformer.toString( model1 );
        boxedHeader = transformer.boxComment( header, true );

        Assert.assertEquals( COMMENT_START_TAG + LINE_SEPARATOR + COMMENT_LINE_PREFIX + "Description" + LINE_SEPARATOR +
                                 COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR +
                                 COMMENT_LINE_PREFIX + "Copyright (C) 2010 Tony" + LINE_SEPARATOR +
                                 COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR +
                                 COMMENT_LINE_PREFIX + "License" + LINE_SEPARATOR + COMMENT_END_TAG + LINE_SEPARATOR,
                             boxedHeader );

        boxedHeader = transformer.boxComment( header, false );

        Assert.assertEquals(
            COMMENT_LINE_PREFIX + "Description" + LINE_SEPARATOR + COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + COMMENT_LINE_PREFIX + "Copyright (C) 2010 Tony" + LINE_SEPARATOR +
                COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + COMMENT_LINE_PREFIX + "License" +
                LINE_SEPARATOR, boxedHeader );

        header = transformer.toString( model2 );

        boxedHeader = transformer.boxComment( header, true );
        Assert.assertEquals(
            COMMENT_START_TAG + LINE_SEPARATOR + COMMENT_LINE_PREFIX + "Description2" + LINE_SEPARATOR +
                COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + COMMENT_LINE_PREFIX +
                "Copyright (C) 2010 - 2012 Tony2" + LINE_SEPARATOR + COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + COMMENT_LINE_PREFIX + "License2" + LINE_SEPARATOR + COMMENT_END_TAG + LINE_SEPARATOR,
            boxedHeader );

        boxedHeader = transformer.boxComment( header, false );
        Assert.assertEquals(
            COMMENT_LINE_PREFIX + "Description2" + LINE_SEPARATOR + COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER +
                LINE_SEPARATOR + COMMENT_LINE_PREFIX + "Copyright (C) 2010 - 2012 Tony2" + LINE_SEPARATOR +
                COMMENT_LINE_PREFIX + DEFAULT_SECTION_DELIMITER + LINE_SEPARATOR + COMMENT_LINE_PREFIX + "License2" +
                LINE_SEPARATOR, boxedHeader );
    }

    @Test
    public void testUnboxComment()
        throws Exception
    {
        String header;
        String boxedHeader;
        String unboxedHeader;

        header = transformer.toString( model1 );
        boxedHeader = transformer.boxComment( header, true );
        unboxedHeader = transformer.unboxComent( boxedHeader );

        Assert.assertEquals( header, unboxedHeader );

        header = transformer.toString( model2 );
        boxedHeader = transformer.boxComment( header, true );
        unboxedHeader = transformer.unboxComent( boxedHeader );

        Assert.assertEquals( header, unboxedHeader );
    }

    public static void assertEquals( FileHeader model, FileHeader model2 )
    {
        Assert.assertEquals( model.getDescription(), model2.getDescription() );
        Assert.assertEquals( model.getCopyrightFirstYear(), model2.getCopyrightFirstYear() );
        Assert.assertEquals( model.getCopyrightLastYear(), model2.getCopyrightLastYear() );
        Assert.assertEquals( model.getCopyrightHolder(), model2.getCopyrightHolder() );
        Assert.assertEquals( model.getLicense(), model2.getLicense() );
    }
}
