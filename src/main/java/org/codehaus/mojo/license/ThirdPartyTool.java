/*
 * #%L
 * License Maven Plugin
 *
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2011 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

/**
 * A tool to load third party files missing files.
 * <p/>
 * We should put here all the logic code written in some mojo and licenseMap...
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public interface ThirdPartyTool
{

    /**
     * Plexus Role
     */
    String ROLE = ThirdPartyTool.class.getName();

    SortedProperties loadThirdPartyDescriptorsForUnsafeMapping( MavenProject project, String encoding,
                                                                Collection<MavenProject> projects,
                                                                SortedSet<MavenProject> unsafeDependencies,
                                                                LicenseMap licenseMap,
                                                                ArtifactRepository localRepository,
                                                                List<ArtifactRepository> repositories )
        throws ThirdPartyToolException, IOException;

    void deployThirdPartyDescriptor( MavenProject project, File file );

    File getThirdPartyDescriptor( MavenProject project, ArtifactRepository localRepository,
                                  List<ArtifactRepository> repositories )
        throws ThirdPartyToolException;
}
