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
package org.codehaus.mojo.license;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
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

    private transient Log log;

    public static final String unknownLicenseMessage = "Unknown license";

    public LicenseMap()
    {
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public Log getLog()
    {
        return log;
    }

    public void addLicense( MavenProject project, List<?> licenses )
    {

        if ( Artifact.SCOPE_SYSTEM.equals( project.getArtifact().getScope() ) )
        {

            // do NOT treate system dependency
            return;
        }

        if ( CollectionUtils.isEmpty( licenses ) )
        {

            // no license found for the dependency
            put( getUnknownLicenseMessage(), project );
            return;
        }

        for ( Object o : licenses )
        {
            String id;
            id = ArtifactHelper.getArtifactId( project.getArtifact() );
            if ( o == null )
            {
                getLog().warn( "could not acquire the license for " + id );
                continue;
            }
            License license = (License) o;
            String licenseKey = license.getName();

            // tchemit 2010-08-29 Ano #816 Check if the License object is well formed

            if ( StringUtils.isEmpty( license.getName() ) )
            {
                getLog().warn( "No license name defined for " + id );
                licenseKey = license.getUrl();
            }

            if ( StringUtils.isEmpty( licenseKey ) )
            {
                getLog().warn( "No license url defined for " + id );
                licenseKey = getUnknownLicenseMessage();
            }
            put( licenseKey, project );
        }
    }

    public SortedSet<MavenProject> getUnsafeDependencies()
    {

        Log log = getLog();
        // get unsafe dependencies (says with no license)
        SortedSet<MavenProject> unsafeDependencies = get( getUnknownLicenseMessage() );

        if ( log.isDebugEnabled() )
        {
            if ( CollectionUtils.isEmpty( unsafeDependencies ) )
            {
                log.debug( "There is no dependency with no license from poms." );
            }
            else
            {
                log.debug( "There is " + unsafeDependencies.size() + " dependencies with no license from poms : " );
                for ( MavenProject dep : unsafeDependencies )
                {

                    // no license found for the dependency
                    log.debug( " - " + ArtifactHelper.getArtifactId( dep.getArtifact() ) );
                }
            }
        }

        return unsafeDependencies;
    }

    protected SortedProperties loadUnsafeMapping( SortedMap<String, MavenProject> artifactCache, String encoding,
                                                  File missingFile )
        throws IOException, ProjectBuildingException
    {

        SortedSet<MavenProject> unsafeDependencies = getUnsafeDependencies();

        SortedProperties unsafeMappings = new SortedProperties( encoding );

        // there is some unsafe dependencies
        if ( missingFile.exists() )
        {

            getLog().info( "Load missing file " + missingFile );

            // load the missing file
            unsafeMappings.load( missingFile );
        }

        // get from the missing file, all unknown dependencies
        List<String> unknownDependenciesId = new ArrayList<String>();

        // migrate unsafe mapping (before version 3.0 we do not have the type of
        // dependency in the missing file, now we must deal with it, so check it

        List<String> migrateId = new ArrayList<String>();
//        SortedMap<String, MavenProject> artifactCache = AbstractAddThirdPartyMojo.getArtifactCache();
        for ( Object o : unsafeMappings.keySet() )
        {
            String id = (String) o;
            MavenProject project = artifactCache.get( id );
            if ( project == null )
            {
                // try with the --jar type
                project = artifactCache.get( id + "--jar" );
                if ( project == null )
                {

                    // now we are sure this is a unknown dependency
                    unknownDependenciesId.add( id );
                }
                else
                {

                    // this dependency must be migrated
                    migrateId.add( id );
                }
            }
        }

        if ( !unknownDependenciesId.isEmpty() )
        {

            // there is some unknown dependencies in the missing file, remove them
            for ( String id : unknownDependenciesId )
            {
                getLog().warn( "dependency [" + id + "] does not exists in project, remove it from the missing file." );
                unsafeMappings.remove( id );
            }

            unknownDependenciesId.clear();
        }

        if ( !migrateId.isEmpty() )
        {

            // there is some dependencies to migrate in the missing file
            for ( String id : migrateId )
            {
                String newId = id + "--jar";
                getLog().info( "Migrate " + id + " to " + newId + " in the missing file." );
                Object value = unsafeMappings.get( id );
                unsafeMappings.remove( id );
                unsafeMappings.put( newId, value );
            }

            migrateId.clear();
        }

        // push back loaded dependencies
        for ( Object o : unsafeMappings.keySet() )
        {
            String id = (String) o;

            MavenProject project = artifactCache.get( id );
            if ( project == null )
            {
                getLog().warn( "dependency [" + id + "] does not exists in project." );
                continue;
            }

            String license = (String) unsafeMappings.get( id );
            if ( StringUtils.isEmpty( license ) )
            {

                // empty license means not fill, skip it
                continue;
            }

            // add license in map
            License l = new License();
            l.setName( license.trim() );
            l.setUrl( license.trim() );

            // add license
            addLicense( project, Arrays.asList( l ) );

            // remove unknown license
            unsafeDependencies.remove( project );
        }

        if ( unsafeDependencies.isEmpty() )
        {

            // no more unknown license in map
            remove( getUnknownLicenseMessage() );
        }
        else
        {

            // add a "with no value license" for missing dependencies
            for ( MavenProject project : unsafeDependencies )
            {
                String id = ArtifactHelper.getArtifactId( project.getArtifact() );
                unsafeMappings.setProperty( id, "" );
            }
        }
        return unsafeMappings;
    }

    public SortedSet<MavenProject> put( String key, MavenProject value )
    {

        // handle multiple values as a set to avoid duplicates
        SortedSet<MavenProject> valueList = get( key );
        if ( valueList == null )
        {

            valueList = new TreeSet<MavenProject>( ArtifactHelper.getProjectComparator() );
        }
        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "key:" + key + ",value: " + value );
        }
        valueList.add( value );
        return put( key, valueList );
    }

    public SortedMap<MavenProject, String[]> toDependencyMap()
    {
        SortedMap<MavenProject, Set<String>> tmp =
            new TreeMap<MavenProject, Set<String>>( ArtifactHelper.getProjectComparator() );

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

        SortedMap<MavenProject, String[]> result =
            new TreeMap<MavenProject, String[]>( ArtifactHelper.getProjectComparator() );
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

    public void mergeLicenses( String... licenses )
    {
        if ( licenses.length == 0 )
        {
            return;
        }

        String mainLicense = licenses[0].trim();
        SortedSet<MavenProject> mainSet = get( mainLicense );
        if ( mainSet == null )
        {
            getLog().warn( "No license [" + mainLicense + "] found, will create it." );
            mainSet = new TreeSet<MavenProject>( ArtifactHelper.getProjectComparator() );
            put( mainLicense, mainSet );
        }
        int size = licenses.length;
        for ( int i = 1; i < size; i++ )
        {
            String license = licenses[i].trim();
            SortedSet<MavenProject> set = get( license );
            if ( set == null )
            {
                getLog().warn( "No license [" + license + "] found, skip this merge." );
                continue;
            }
            getLog().info( "Merge license [" + license + "] (" + set.size() + " depedencies)." );
            mainSet.addAll( set );
            set.clear();
            remove( license );
        }
    }
}
