package org.codehaus.mojo.license;

/* 
 * Codehaus License Maven Plugin
 *     
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/lgpl-3.0.html>.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Some basic file io utilities
 * 
 * @author pgier
 */
public class FileUtil
{

    public static void tryClose( InputStream is )
    {
        if ( is == null )
        {
            return;
        }
        try
        {
            is.close();
        }
        catch ( IOException e )
        {
            // do nothing
        }
    }

    public static void tryClose( OutputStream os )
    {
        if ( os == null )
        {
            return;
        }
        try
        {
            os.close();
        }
        catch ( IOException e )
        {
            // do nothing
        }
    }
}
