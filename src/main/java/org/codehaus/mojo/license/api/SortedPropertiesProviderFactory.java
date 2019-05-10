package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
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

import java.io.File;

import org.codehaus.mojo.license.api.impl.EmptySortedPropertiesProvider;
import org.codehaus.mojo.license.api.impl.SortedPropertiesFileProvider;
import org.codehaus.mojo.license.api.impl.SortedPropertiesUrlProvider;
import org.codehaus.mojo.license.utils.UrlRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide additional licenses, for example when projects
 * don't specify a license.
 *
 * @since 1.21
 */
public class SortedPropertiesProviderFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger( SortedPropertiesProviderFactory.class );

    private final String encoding;

    public SortedPropertiesProviderFactory( String encoding )
    {
        this.encoding = encoding;
    }

    /**
     * Returns a copy of the additional licenses.
     *
     * <p>You will get a new copy every time you call this
     * method.
     *
     * @return mutable copy of the additional licenses.
     */
    public SortedPropertiesProvider build( File file, String url )
    {
        if ( file != null )
        {
            if ( file.exists() )
            {
                return new SortedPropertiesFileProvider( file, encoding );
            }

            LOGGER.debug( "No such file: {}", file.getAbsolutePath() );
        }

        if ( UrlRequester.isStringUrl( url ) )
        {
            return new SortedPropertiesUrlProvider( url );
        }

        return new EmptySortedPropertiesProvider();

    }
}
