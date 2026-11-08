package org.codehaus.mojo.license.header.transformer;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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

import static org.codehaus.mojo.license.header.transformer.FileHeaderTransformer.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeMarkerFileHeaderTransformerTest {

    private final FreeMarkerFileHeaderTransformer transformer = new FreeMarkerFileHeaderTransformer();

    private final String header = transformer.boxComment("license" + LINE_SEPARATOR, true);

    @Test
    void headerGoesAfterAngleBracketFtlDirective() {
        final String directive = "<#ftl output_format=\"HTML\" strip_whitespace=true>";

        final String result = transformer.addHeader(header, directive + LINE_SEPARATOR + "<p>${name}</p>");

        assertEquals(directive + LINE_SEPARATOR + header + LINE_SEPARATOR + "<p>${name}</p>", result);
    }

    @Test
    void headerGoesAfterSquareBracketFtlDirectiveInSquareBracketComment() {
        final String result = transformer.addHeader(header, "[#ftl]" + LINE_SEPARATOR + "[#if x]y[/#if]");

        assertTrue(result.startsWith("[#ftl]" + LINE_SEPARATOR + "[#--"), result);
        assertTrue(result.contains("--]" + LINE_SEPARATOR), result);
        assertFalse(result.contains("<#--"), result);
        assertFalse(result.contains("-->"), result);
        assertTrue(result.endsWith("[#if x]y[/#if]"), result);
    }

    @Test
    void headerStaysFirstWithoutFtlDirective() {
        final String result = transformer.addHeader(header, "<p>${name}</p>");

        assertTrue(result.startsWith(header), result);
    }

    @Test
    void squareBracketHeaderIsUnboxed() {
        final String squareHeader = header.replace("<#--", "[#--").replace("-->", "--]");

        assertEquals(transformer.unboxComent(header), transformer.unboxComent(squareHeader));
    }
}
