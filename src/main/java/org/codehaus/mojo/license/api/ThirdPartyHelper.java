/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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

package org.codehaus.mojo.license.api;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Helper class that provides common functionality required by both the mojos and the reports.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public interface ThirdPartyHelper
{

    SortedMap<String, MavenProject> loadDependencies( MavenProjectDependenciesConfigurator configuration );

    SortedProperties loadThirdPartyDescriptorForUnsafeMapping( SortedSet<MavenProject> unsafeDependencies,
                                                               Collection<MavenProject> projects,
                                                               LicenseMap licenseMap )
        throws ThirdPartyToolException, IOException;

    SortedProperties loadUnsafeMapping( LicenseMap licenseMap, File missingFile )
        throws IOException;

    LicenseMap createLicenseMap( SortedMap<String, MavenProject> dependencies );

    void attachThirdPartyDescriptor( File file );

    SortedSet<MavenProject> getProjectsWithNoLicense( LicenseMap licenseMap );

    SortedMap<String, MavenProject> getArtifactCache();

    SortedProperties createUnsafeMapping( LicenseMap licenseMap, File missingFile, boolean useRepositoryMissingFiles,
                                          SortedSet<MavenProject> unsafeDependencies,
                                          Collection<MavenProject> projectDependencies )
        throws ProjectBuildingException, IOException, ThirdPartyToolException;

    void mergeLicenses( List<String> licenseMerges, LicenseMap licenseMap )
        throws MojoFailureException;
}
