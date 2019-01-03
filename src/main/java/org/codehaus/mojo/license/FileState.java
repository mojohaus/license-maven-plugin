package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2017 Tony Chemit
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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines state of a file after process.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public enum FileState
{

    /**
     * file was updated
     */
    update,

    /**
     * file was up to date
     */
    uptodate,

    /**
     * something was added on file
     */
    add,
    /**
     * something was removed from file
     */
    remove,

    /**
     * file was ignored
     */
    ignore,

    /**
     * treatment failed for file
     */
    fail;

    /**
     * Register a file for this state on result dictionary.
     *
     * @param file    file to add
     * @param results dictionary to update
     */
    public void addFile( File file, EnumMap<FileState, Set<File>> results )
    {
        Set<File> fileSet = results.get( this );
        if ( fileSet == null )
        {
            fileSet = new HashSet<>();
            results.put( this, fileSet );
        }
        fileSet.add( file );
    }
}
