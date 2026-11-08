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

class HtmlFileHeaderTransformerTest {

    private static final String HEADER =
            "first line" + LINE_SEPARATOR + LINE_SEPARATOR + "second line" + LINE_SEPARATOR;

    @Test
    void unboxKeepsBlankLinesOfTrimmedHeader() {
        final FileHeaderTransformer transformer = new HtmlFileHeaderTransformer();
        transformer.setTrimHeaderLine(true);

        final String boxed = transformer.boxComment(HEADER, true);

        assertEquals(HEADER, transformer.unboxComent(boxed));
    }

    @Test
    void unboxKeepsBlankLinesOfUntrimmedHeader() {
        final FileHeaderTransformer transformer = new HtmlFileHeaderTransformer();

        final String boxed = transformer.boxComment(HEADER, true);

        assertEquals(HEADER, transformer.unboxComent(boxed));
    }
}
