package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Codehaus
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

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

public class LicenseDownloaderTest
{

    @Test
    public void updateFileExtension() {
        assertExtension( "path/to/file.html", "path/to/file.php", "text/html" );
        assertExtension( "path/to/file.txt", "path/to/file", null );
    }

    private static void assertExtension( String expected, String input, String mimeType )
    {
        final File in = new File( input );
        final File result = LicenseDownloader.updateFileExtension( in, mimeType );
        Assert.assertEquals( expected, result.getPath().replace( '\\', '/' ) );
    }

}
