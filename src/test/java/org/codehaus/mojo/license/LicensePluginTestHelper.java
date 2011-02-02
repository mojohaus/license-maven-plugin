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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.IOUtil;

/**
 * Helper methods for the Maven plugin test harness
 */
public class LicensePluginTestHelper
{
    /**
     * Initialize the project of the mojo.
     * 
     * @param container the plexus container
     * @param pomFile the pom file used to configure the mojo
     * @throws Exception
     */
    public static MavenProject buildProject( PlexusContainer container, File pomFile )
        throws Exception
    {
        MavenProjectBuilder projectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.ROLE );
        ProjectBuilderConfiguration projectBuilderConfiguration = new DefaultProjectBuilderConfiguration();
        return projectBuilder.build( pomFile, projectBuilderConfiguration );
    }

    /**
     * Try to update the lastModified date of a file
     * 
     * @param file
     * @param lastModified
     * @throws IOException If the lastModified date was not changed
     */
    public static void setLastModified( File file, long lastModified )
        throws IOException
    {
        boolean ok = file.setLastModified( lastModified );
        if ( !ok )
        {
            throw new IOException( "could not changed lastModified [" + lastModified + "] for " + file );
        }
    }

    /**
     * Read a file as a string with the given encoding
     * 
     * @param file The file to read
     * @param encoding encoding to read file
     * @return the content of the file
     * @throws IOException if IO problem
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

}
