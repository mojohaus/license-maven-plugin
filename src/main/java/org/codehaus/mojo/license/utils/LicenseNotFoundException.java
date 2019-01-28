package org.codehaus.mojo.license.utils;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Exception to be thrown when a license url returns not found.
 */
public class LicenseNotFoundException extends IOException
{
    private static final long serialVersionUID = 1L;

    private final URL licenseUrl;

    LicenseNotFoundException( final URL licenseUrl, final FileNotFoundException e )
    {
        super( e );
        this.licenseUrl = licenseUrl;
    }

    public URL getLicenseUrl()
    {
        return licenseUrl;
    }
}
