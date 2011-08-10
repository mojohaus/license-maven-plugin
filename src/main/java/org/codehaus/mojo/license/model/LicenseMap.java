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
package org.codehaus.mojo.license.model;

import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.MojoHelper;

import java.util.*;

/**
 * Map of artifacts (stub in mavenproject) group by their license.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class LicenseMap
    extends TreeMap<String, SortedSet<MavenProject>>
{

    private static final long serialVersionUID = 864199843545688069L;

    public static final String unknownLicenseMessage = "Unknown license";

    private final Comparator<MavenProject> projectComparator;

    public LicenseMap()
    {
        projectComparator = MojoHelper.newMavenProjectComparator();
    }

    public SortedSet<MavenProject> put( String key, MavenProject value )
    {

        // handle multiple values as a set to avoid duplicates
        SortedSet<MavenProject> valueList = get( key );
        if ( valueList == null )
        {

            valueList = new TreeSet<MavenProject>( projectComparator );
        }

        valueList.add( value );
        return put( key, valueList );
    }

    public SortedMap<MavenProject, String[]> toDependencyMap()
    {
        SortedMap<MavenProject, Set<String>> tmp = new TreeMap<MavenProject, Set<String>>( projectComparator );

        for ( Map.Entry<String, SortedSet<MavenProject>> entry : entrySet() )
        {
            String license = entry.getKey();
            SortedSet<MavenProject> set = entry.getValue();
            for ( MavenProject p : set )
            {
                Set<String> list = tmp.get( p );
                if ( list == null )
                {
                    list = new HashSet<String>();
                    tmp.put( p, list );
                }
                list.add( license );
            }
        }

        SortedMap<MavenProject, String[]> result = new TreeMap<MavenProject, String[]>( projectComparator );
        for ( Map.Entry<MavenProject, Set<String>> entry : tmp.entrySet() )
        {
            List<String> value = new ArrayList<String>( entry.getValue() );
            Collections.sort( value );
            result.put( entry.getKey(), value.toArray( new String[value.size()] ) );
        }
        tmp.clear();
        return result;
    }

    public static String getUnknownLicenseMessage()
    {
        return unknownLicenseMessage;
    }

}
