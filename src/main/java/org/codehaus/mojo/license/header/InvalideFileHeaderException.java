/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2011 CodeLutin, Codehaus, Tony Chemit
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

package org.codehaus.mojo.license.header;

import java.io.IOException;

/**
 * Exception to be thrown when a file header could not be read or transformed
 * back to a {@link FileHeader}
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class InvalideFileHeaderException
    extends IOException
{
    private static final long serialVersionUID = 1L;

    public InvalideFileHeaderException()
    {
    }

    public InvalideFileHeaderException( String message )
    {
        super( message );
    }
}
