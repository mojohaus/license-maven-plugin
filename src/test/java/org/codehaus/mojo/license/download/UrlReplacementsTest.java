package org.codehaus.mojo.license.download;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlReplacementsTest {

    private static final String OLD_URL = "http://opensource.org/licenses/mit-license";

    private static final String NEW_URL = "https://opensource.org/license/mit";

    @Test
    void quotesAroundTheUrlDoNotSurviveAReplacement() {
        UrlReplacements replacements = UrlReplacements.builder()
                .replacement("mit", "\\Q" + OLD_URL + "\\E", NEW_URL)
                .build();

        assertEquals(NEW_URL, replacements.rewriteIfNecessary("'" + OLD_URL + "'"));
        assertEquals(NEW_URL, replacements.rewriteIfNecessary('"' + OLD_URL + '"'));
    }

    @Test
    void quoteAnchoredReplacementStillMatchesThePublishedUrl() {
        UrlReplacements replacements = UrlReplacements.builder()
                .replacement("mit-anchored", "^'\\Q" + OLD_URL + "\\E'$", NEW_URL)
                .build();

        assertEquals(NEW_URL, replacements.rewriteIfNecessary("'" + OLD_URL + "'"));
    }

    @Test
    void quotesAreStrippedEvenWhenNoReplacementMatches() {
        UrlReplacements replacements = UrlReplacements.builder().build();

        assertEquals(OLD_URL, replacements.rewriteIfNecessary("'" + OLD_URL + "'"));
    }

    @Test
    void unmatchedQuotesAreLeftAlone() {
        UrlReplacements replacements = UrlReplacements.builder().build();

        assertEquals("'" + OLD_URL, replacements.rewriteIfNecessary("'" + OLD_URL));
    }
}
