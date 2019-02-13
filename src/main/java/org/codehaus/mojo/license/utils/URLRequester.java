package org.codehaus.mojo.license.utils;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 - Falco Nikolas
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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * The implementation of this interface must provide the way to read the content
 * of the given URL as String.
 *
 * @author Nikolas Falco
 */
public interface URLRequester
{

    /**
     * This method retrieve the content of the URL as a string.
     *
     * @param url
     *            the resource destination that is expected to contain pure text
     * @return the string representation of the resource at the given URL
     */
    String getFromUrl( String url ) throws MojoExecutionException;

}