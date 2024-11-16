package org.codehaus.mojo.license.header.transformer;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link XmlFileHeaderTransformer}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class XmlFileHeaderTransformerTest {

    protected XmlFileHeaderTransformer transformer;

    private static final String CONTENT = "content";

    private static final String HEADER = "header";

    @BeforeEach
    void setUp() {
        transformer = new XmlFileHeaderTransformer();
    }

    @AfterEach
    void tearDown() {
        transformer = null;
    }

    @Test
    void testAddHeaderWithNoProlog() {
        String header = HEADER;
        String content = CONTENT;
        String result = transformer.addHeader(header, content);
        assertEquals(header + content, result);
    }

    @Test
    void testAddHeaderWithProlog() {
        String header = HEADER;
        String prolog = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        String content = prolog + CONTENT;
        String result = transformer.addHeader(header, content);
        assertEquals(prolog + FileHeaderTransformer.LINE_SEPARATOR + header + CONTENT, result);

        header = HEADER;
        content = "  " + prolog + CONTENT;
        result = transformer.addHeader(header, content);
        assertEquals("  " + prolog + FileHeaderTransformer.LINE_SEPARATOR + header + CONTENT, result);
    }
}
