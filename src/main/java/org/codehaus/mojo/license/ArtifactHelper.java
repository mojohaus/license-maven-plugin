/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2008 - 2010 CodeLutin
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
package org.codehaus.mojo.license;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.util.Comparator;

/**
 * A helper for artifacts.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class ArtifactHelper
{

    protected static Comparator<MavenProject> projectComparator;

    public static String getArtifactId( Artifact artifact )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( artifact.getGroupId() );
        sb.append( "--" );
        sb.append( artifact.getArtifactId() );
        sb.append( "--" );
        sb.append( artifact.getVersion() );
        String type = artifact.getType();
        if ( !StringUtils.isEmpty( type ) && !"pom".equals( type ) )
        {
            sb.append( "--" );
            sb.append( artifact.getType() );
        }
        if ( !StringUtils.isEmpty( artifact.getClassifier() ) )
        {
            sb.append( "--" );
            sb.append( artifact.getClassifier() );
        }
        return sb.toString();
    }

    public static String getArtifactName( MavenProject project )
    {
        StringBuilder sb = new StringBuilder();
        if ( project.getName().startsWith( "Unnamed -" ) )
        {

            // as in Maven 3, let's use the artifact id
            sb.append( project.getArtifactId() );
        }
        else
        {
            sb.append( project.getName() );
        }
        sb.append( " (" );
        sb.append( project.getGroupId() );
        sb.append( ":" );
        sb.append( project.getArtifactId() );
        sb.append( ":" );
        sb.append( project.getVersion() );
        sb.append( " - " );
        String url = project.getUrl();
        sb.append( url == null ? "no url defined" : url );
        sb.append( ")" );

        return sb.toString();
    }

    public static Comparator<MavenProject> getProjectComparator()
    {
        if ( projectComparator == null )
        {
            projectComparator = new Comparator<MavenProject>()
            {
                public int compare( MavenProject o1, MavenProject o2 )
                {

                    String id1 = getArtifactId( o1.getArtifact() );
                    String id2 = getArtifactId( o2.getArtifact() );
                    return id1.compareTo( id2 );
                }
            };
        }
        return projectComparator;
    }
}
