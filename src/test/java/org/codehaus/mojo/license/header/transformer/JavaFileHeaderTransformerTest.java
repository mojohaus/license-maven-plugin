package org.codehaus.mojo.license.header.transformer;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link JavaFileHeaderTransformer}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.2
 */
class JavaFileHeaderTransformerTest {

    private static final String PACKAGE = "package org.codehaus.mojo.license.header.transformer;";

    private static final String CONTENT = "content";

    private static final String HEADER = "header";

    private static final String LINE_SEPARATOR = "<eol>";

    @Test
    void testAddHeader() {
        JavaFileHeaderTransformer transformer = new JavaFileHeaderTransformer();
        transformer.setLineSeparator(LINE_SEPARATOR);
        transformer.setEmptyLineAfterHeader(false);

        String content = PACKAGE + FileHeaderTransformer.LINE_SEPARATOR + CONTENT;

        transformer.setAddJavaLicenseAfterPackage(false);

        String result = transformer.addHeader(HEADER, content);
        assertEquals(HEADER + content, result);

        transformer.setAddJavaLicenseAfterPackage(true);

        result = transformer.addHeader(HEADER, content);
        assertEquals(
                PACKAGE + FileHeaderTransformer.LINE_SEPARATOR + transformer.getLineSeparator() + HEADER + CONTENT,
                result);
    }
}
