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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * This aggregator goal (will be executed only once and only on pom projects)
 * executed the {@code add-third-party} on all his modules (in a parellel build cycle)
 * then aggreates all the third-party files in final one in the pom project.
 *
 * @author tchemit <chemit@codelutin.com>
 * @goal aggregate-add-third-party
 * @phase generate-resources
 * @requiresProject true
 * @aggregator
 * @execute goal="add-third-party"
 * @since 1.0
 */
public class AggregatorAddThirdPartyMojo
    extends AbstractAddThirdPartyMojo
{

    /**
     * The projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     * @required
     * @since 1.0
     */
    protected List<?> reactorProjects;

    @Override
    protected boolean checkPackaging()
    {
        return acceptPackaging( "pom" );
    }

    @Override
    protected boolean checkSkip()
    {
        if ( !isDoGenerate() && !isDoGenerateBundle() )
        {

            getLog().info( "All files are up to date, skip goal execution." );
            return false;
        }
        return super.checkSkip();
    }

    @Override
    protected SortedMap<String, MavenProject> loadDependencies()
    {
        // use the cache filled by modules in reactor
        return getArtifactCache();
    }

    @Override
    protected SortedProperties createUnsafeMapping()
        throws ProjectBuildingException, IOException
    {

        String path =
            getMissingFile().getAbsolutePath().substring( getProject().getBasedir().getAbsolutePath().length() + 1 );

        if ( isVerbose() )
        {
            getLog().info( "Use missing file path : " + path );
        }

        SortedProperties unsafeMappings = new SortedProperties( getEncoding() );

        LicenseMap licenseMap = getLicenseMap();

        for ( Object o : reactorProjects )
        {
            MavenProject p = (MavenProject) o;

            File file = new File( p.getBasedir(), path );

            if ( file.exists() )
            {

                SortedProperties tmp = licenseMap.loadUnsafeMapping( getArtifactCache(), getEncoding(), file );
                unsafeMappings.putAll( tmp );
            }

            SortedSet<MavenProject> unsafes = licenseMap.getUnsafeDependencies();
            if ( CollectionUtils.isEmpty( unsafes ) )
            {

                // no more unsafe dependencies, can break
                break;
            }
        }
        return unsafeMappings;
    }

    @Override
    protected void doAction()
        throws Exception
    {
        Log log = getLog();

        if ( isVerbose() )
        {
            log.info( "After executing on " + reactorProjects.size() + " project(s)" );
        }
        SortedMap<String, MavenProject> artifacts = getArtifactCache();

        LicenseMap licenseMap = getLicenseMap();

        getLog().info( artifacts.size() + " detected artifact(s)." );
        if ( isVerbose() )
        {
            for ( String id : artifacts.keySet() )
            {
                getLog().info( " - " + id );
            }
        }
        getLog().info( licenseMap.size() + " detected license(s)." );
        if ( isVerbose() )
        {
            for ( String id : licenseMap.keySet() )
            {
                getLog().info( " - " + id );
            }
        }
        boolean unsafe = checkUnsafeDependencies();

        writeThirdPartyFile();

        if ( unsafe && isFailIfWarning() )
        {
            throw new MojoFailureException( "There is some dependencies with no license, please review the modules." );
        }
    }

}
