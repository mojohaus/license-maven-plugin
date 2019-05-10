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

/** Do something when we encounter an unknown dependency.
 *
 *  <p>One source for unknown dependencies is the <code>missingFileUrl</code>
 *  configuration option. When a company-wide classpath resource is used
 *  here, then there will be many projects that don't use all dependencies
 *  listed.
 *
 *  <p>The same is true for the option <code>overrideUrl</code>.
 *
 * @since 1.21
 */
public interface UnknownDependencyStrategy
{
    void handleUnknownDependency( String id );
}
