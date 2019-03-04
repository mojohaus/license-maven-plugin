package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 MojoHaus
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

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.20
 */
public class LicenseContentSanitizer
{
    private String id;

    private String urlRegexp;

    private String contentRegexp;

    private String contentReplacement;

    public String getId()
    {
        return id;
    }

    public String getUrlRegexp()
    {
        return urlRegexp;
    }

    public String getContentRegexp()
    {
        return contentRegexp;
    }

    public String getContentReplacement()
    {
        return contentReplacement;
    }
}
