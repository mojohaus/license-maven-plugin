package org.codehaus.mojo.license;

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
