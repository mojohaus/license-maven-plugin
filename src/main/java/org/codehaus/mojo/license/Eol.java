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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * End of line values
 *
 * @since 1.17
 */
public enum Eol
{
    /** Unix {@code \n} */
    LF( "\n" ),

    /** Windows {@code \r\n} */
    CRLF ( "\r\n" ),

    /** Autodetect the end of line value based on some existing file using {@link #autodetect(Path, Charset)} */
    AUTODETECT ( null )
    {
        public String getEolString()
        {
            throw new IllegalStateException( "You need to autodetect the end of line value using "
                    + Eol.class.getName() + ".autodetect(Path, Charset)" );
        }
    },

    /**
     * The value of the {@code line.separator} system property in the current JVM.
     *
     * @deprecated kept for backwards compatibility reasons. Use other values of {@link Eol}. {@link #PLATFORM}
     * makes your build irreproducible on other platforms.
     */
    @Deprecated
    PLATFORM ( null )
    {
        public String getEolString()
        {
            return System.getProperty( "line.separator" );
        }
    };

    public static Eol autodetect( Path file, Charset charset ) throws IOException
    {
        final String content = new String( Files.readAllBytes( file ), charset );
        return content.indexOf( '\r' ) >= 0 ? Eol.CRLF : Eol.LF;
    }

    private final String eolString;

    private Eol( String eolString )
    {
        this.eolString = eolString;
    }

    public String getEolString()
    {
        return eolString;
    }
}
