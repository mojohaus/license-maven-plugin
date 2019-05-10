package org.codehaus.mojo.license.api;

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
import org.codehaus.mojo.license.api.impl.IgnoreUnknownDependencies;
import org.codehaus.mojo.license.api.impl.LogUnknownDependencies;
import org.codehaus.plexus.logging.Logger;

/**
 * Factory for strategies to handle unknown dependencies.
 *
 * @since 1.21
 */
public class UnknownDependencyStrategyFactory
{
    /**
     * Possible strategies
     */
    public static enum Strategy
    {
        ignore, warn
    }

    public UnknownDependencyStrategy build( Strategy strategy, Logger logger, MavenProject project,
            String configOptionName )
    {
        switch ( strategy )
        {
        case ignore:
            return new IgnoreUnknownDependencies();

        case warn:
            return new LogUnknownDependencies( logger, project, configOptionName );

        default:
            throw new IllegalArgumentException( "Unsupported strategy: " + strategy );
        }
    }
}
