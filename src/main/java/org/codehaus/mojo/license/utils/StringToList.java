package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2013 CodeLutin, Codehaus, Tony Chemit
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

import java.util.ArrayList;
import java.util.List;

/**
 * Object to convert in mojo a parameter from a some simple String to a List.
 * <p/>
 * See (http://jira.codehaus.org/browse/MLICENSE-53).
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.4
 */
public class StringToList
{

    /**
     * List of data.
     */
    private final List<String> data;

    public StringToList()
    {
        data = new ArrayList<String>();
    }

    public StringToList( String data )
    {
        this();
        for ( String s : data.split( "\\s*\\|\\s*" ) )
        {
            addEntryToList( s );
        }
    }

    public List<String> getData()
    {
        return data;
    }

    protected void addEntryToList( String data )
    {
        this.data.add( data );
    }

}
