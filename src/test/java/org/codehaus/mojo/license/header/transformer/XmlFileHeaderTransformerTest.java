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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link XmlFileHeaderTransformer}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class XmlFileHeaderTransformerTest
{

    protected XmlFileHeaderTransformer transformer;

    private static final String CONTENT = "content";

    private static final String HEADER = "header";

    @Before
    public void setUp()
    {
        transformer = new XmlFileHeaderTransformer();
    }

    @After
    public void tearDown()
    {
        transformer = null;
    }

    @Test
    public void testAddHeaderWithNoProlog()
    {
        String header = HEADER;
        String content = CONTENT;
        String result = transformer.addHeader( header, content );
        Assert.assertEquals( header + content, result );
    }

    @Test
    public void testAddHeaderWithProlog()
    {
        String header = HEADER;
        String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String content = prolog + CONTENT;
        String result = transformer.addHeader( header, content );
        Assert.assertEquals( prolog + '\n' + header + CONTENT, result );

        header = HEADER;
        content = "  " + prolog + CONTENT;
        result = transformer.addHeader( header, content );
        Assert.assertEquals( "  " + prolog + '\n' + header + CONTENT, result );
    }
}
