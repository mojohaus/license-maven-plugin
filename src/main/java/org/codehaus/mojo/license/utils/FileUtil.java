package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 Codehaus
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

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Some basic file io utilities
 *
 * @author pgier
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
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

    /**
     * Creates the directory (and his parents) if necessary.
     *
     * @param dir the directory to create if not exisiting
     * @return {@code true} if directory was created, {@code false} if was no
     *         need to create it
     * @throws IOException if could not create directory
     */
    public static boolean createDirectoryIfNecessary( File dir )
        throws IOException
    {
        if ( !dir.exists() )
        {
            boolean b = dir.mkdirs();
            if ( !b )
            {
                throw new IOException( "Could not create directory " + dir );
            }
            return true;
        }
        return false;
    }

    public static boolean createNewFile( File file )
        throws IOException
    {
        createDirectoryIfNecessary( file.getParentFile() );
        if ( !file.exists() )
        {
            boolean b = file.createNewFile();
            if ( !b )
            {
                throw new IOException( "Could not create new file " + file );
            }
            return true;
        }
        return false;
    }

    /**
     * Delete the given file.
     *
     * @param file the file to delete
     * @throws IOException if could not delete the file
     */
    public static void deleteFile( File file )
        throws IOException
    {
        if ( !file.exists() )
        {
            // file does not exist, can not delete it
            return;
        }
        boolean b = file.delete();
        if ( !b )
        {
            throw new IOException( "could not delete file " + file );
        }
    }

    /**
     * Rename the given file to a new destination.
     *
     * @param file        the file to rename
     * @param destination the destination file
     * @throws IOException if could not rename the file
     */
    public static void renameFile( File file, File destination )
        throws IOException
    {
        boolean b = file.renameTo( destination );
        if ( !b )
        {
            throw new IOException( "could not rename " + file + " to " + destination );
        }
    }

    /**
     * Copy a file to a given locationand logging.
     *
     * @param source represents the file to copy.
     * @param target file name of destination file.
     * @throws IOException if could not copy file.
     */
    public static void copyFile( File source, File target )
        throws IOException
    {
        createDirectoryIfNecessary( target.getParentFile() );
        FileUtils.copyFile( source, target );
    }

    public static File getFile( File base, String... paths )
    {
        StringBuilder buffer = new StringBuilder();
        for ( String path : paths )
        {
            buffer.append( File.separator ).append( path );
        }
        return new File( base, buffer.substring( 1 ) );
    }

    /**
     * @param file the source file
     * @return the backup file
     */
    public static File getBackupFile( File file )
    {
        return new File( file.getAbsolutePath() + "~" );
    }

    /**
     * Backups the given file using the {@link FileUtil#getBackupFile(File)} as
     * destination file.
     *
     * @param f the file to backup
     * @throws IOException if any pb while copying the file
     */
    public static void backupFile( File f )
        throws IOException
    {
        File dst = getBackupFile( f );
        copyFile( f, dst );
    }

    /**
     * Permet de lire un fichier et de retourner sont contenu sous forme d'une
     * chaine de carateres.
     *
     * @param file     le fichier a lire
     * @param encoding encoding to read file
     * @return the content of the file
     * @throws IOException if IO pb
     */
    public static String readAsString( File file, String encoding )
        throws IOException
    {
        FileInputStream inf = new FileInputStream( file );
        BufferedReader in = new BufferedReader( new InputStreamReader( inf, encoding ) );
        try
        {
            return IOUtil.toString( in );
        }
        finally
        {
            in.close();
        }
    }

    /**
     * Sauvegarde un contenu dans un fichier.
     *
     * @param file     le fichier a ecrire
     * @param content  le contenu du fichier
     * @param encoding l'encoding d'ecriture
     * @throws IOException if IO pb
     */
    public static void writeString( File file, String content, String encoding )
        throws IOException
    {
        createDirectoryIfNecessary( file.getParentFile() );
        BufferedWriter out;
        out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( file ), encoding ) );
        try
        {
            IOUtil.copy( content, out );
        }
        finally
        {
            out.close();
        }
    }

    public static List<File> orderFiles(Collection<File> files) {
        List<File> result = new ArrayList<File>( files );
        Collections.sort( result, new Comparator<File>()
        {
            public int compare( File o1, File o2 )
            {
                return o1.getAbsolutePath().compareTo( o2.getAbsolutePath() );
            }
        } );
        return result;
    }
}
