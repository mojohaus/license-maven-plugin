package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2008 - 2012 CodeLutin, Codehaus, Tony Chemit
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.artifact.Artifact;

/**
 * Copies of the project's dependency sets. AddThirdParty needs to load dependencies only for the single project it is
 * run for, while AggregateAddThirdParty needs to load dependencies for the parent project, as well as all child
 * projects in the reactor.
 *
 * The aggregator goal replaces all reactor projects with their direct dependencies, to avoid trying to load artifacts
 * for projects that haven't been built/published yet. This is necessary in cases where one child project A in a reactor
 * depends on another project B in the same reactor. Since B is not necessarily built/published, the plugin needs to
 * replace B with its dependencies when processing A. This field stores that modified view of the project's
 * dependencies.
 */
public class ResolvedProjectDependencies
{

    private final Set<Artifact> allDependencies;
    private final Set<Artifact> directDependencies;

    public ResolvedProjectDependencies( Set<Artifact> allDependencies, Set<Artifact> directDependencies )
    {
        this.allDependencies = Collections.unmodifiableSet( new HashSet<>( allDependencies ) );
        this.directDependencies = Collections.unmodifiableSet( new HashSet<>( directDependencies ) );
    }

    public Set<Artifact> getAllDependencies()
    {
        return allDependencies;
    }

    public Set<Artifact> getDirectDependencies()
    {
        return directDependencies;
    }

}
