package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Codehaus
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

import java.util.regex.Pattern;

/**
 * Defines a license url pattern and replacement pair.
 *
 * @since 1.17
 */
public class LicenseUrlReplacement
{
    /**
     * Regular expression used to identify license urls that are to be replaced.
     *
     * @since 1.17
     */
    private Pattern regexp;

    /**
     * Replacement license url.
     *
     * <p>Be aware that the replacement is passed to {@link java.util.regex.Matcher#replaceAll(String)
     * java.util.regex.Matcher#replaceAll(String)}, so be aware of the significance of backslashes (<tt>\</tt>)
     * and dollar signs (<tt>$</tt>) within the replacement string.
     *
     * @since 1.17
     */
    private String replacement;

    Pattern getRegexp()
    {
        return regexp;
    }

    @SuppressWarnings( "unused" )
    public void setRegexp( final String regexp )
    {
        this.regexp = Pattern.compile( regexp );
    }

    String getReplacement()
    {
        return replacement;
    }
}
