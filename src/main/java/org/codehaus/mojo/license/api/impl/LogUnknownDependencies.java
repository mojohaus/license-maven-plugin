package org.codehaus.mojo.license.api.impl;

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

import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.license.api.UnknownDependencyStrategy;
import org.codehaus.plexus.logging.Logger;

/**
 * Log a warning for unknown dependencies.
 *
 * @since 1.21
 */
public class LogUnknownDependencies implements UnknownDependencyStrategy
{
    private final Logger logger;
    private final MavenProject project;
    private final String configOptionName;

    public LogUnknownDependencies( Logger logger,  MavenProject project, String configOptionName )
    {
        this.logger = logger;
        this.project = project;
        this.configOptionName = configOptionName;
    }

    @Override
    public void handleUnknownDependency( String id )
    {
        logger.warn( getMessage( id ) );
    }

    protected String getMessage( String id )
    {
        return "Dependency [" + id + "] is mentioned in " + configOptionName
                + " but isn't used in the project " + getProjectReference();
    }

    protected String getProjectReference()
    {
        return project.getGroupId() + ':' + project.getArtifactId();
    }
}
