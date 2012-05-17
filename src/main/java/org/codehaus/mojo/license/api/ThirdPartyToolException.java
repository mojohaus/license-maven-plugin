package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2011 CodeLutin, Codehaus, Tony Chemit
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
 * An exception occurring during the execution of this tool.
 *
 * @author <a href="mailto:tchemit@codelutin.com">tony chemit</a>
 * @version $Id$
 */
public class ThirdPartyToolException
    extends Exception
{
    /**
     * Construct a new <code>ThirdPartyToolException</code> exception wrapping an underlying <code>Exception</code>
     * and providing a <code>message</code>.
     *
     * @param message could be null
     * @param cause   could be null
     */
    public ThirdPartyToolException( String message, Exception cause )
    {
        super( message, cause );
    }

    /**
     * Construct a new <code>ThirdPartyToolException</code> exception wrapping an underlying <code>Throwable</code>
     * and providing a <code>message</code>.
     *
     * @param message could be null
     * @param cause   could be null
     */
    public ThirdPartyToolException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Construct a new <code>ThirdPartyToolException</code> exception providing a <code>message</code>.
     *
     * @param message could be null
     */
    public ThirdPartyToolException( String message )
    {
        super( message );
    }
}
