package org.codehaus.mojo.license;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;

/**
 * Download the license files of all the current project's dependencies, and generate a summary file containing a list
 * of all dependencies and their licenses.
 *
 * @author Paul Gier
 * @since 1.0
 */
@Mojo( name = "download-licenses", requiresDependencyResolution = ResolutionScope.TEST,
    defaultPhase = LifecyclePhase.PACKAGE )
public class DownloadLicensesMojo
    extends AbstractDownloadLicensesMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * A flag to skip the goal.
     *
     * @since 1.5
     */
    @Parameter( property = "license.skipDownloadLicenses", defaultValue = "false" )
    protected boolean skipDownloadLicenses;

    // ----------------------------------------------------------------------
    // AbstractDownloadLicensesMojo Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected boolean isSkip()
    {
        return skipDownloadLicenses;
    }

    protected SortedMap<String, MavenProject>  getDependencies()
    {
        return getDependencies( getProject() );
    }

}
