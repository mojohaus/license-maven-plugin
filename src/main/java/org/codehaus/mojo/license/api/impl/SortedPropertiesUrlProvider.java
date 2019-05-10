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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.codehaus.mojo.license.api.SortedPropertiesProvider;
import org.codehaus.mojo.license.utils.SortedProperties;
import org.codehaus.mojo.license.utils.UrlRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load missing licenses from a URL.
 *
 * @since 1.21
 */
public class SortedPropertiesUrlProvider implements SortedPropertiesProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger( SortedPropertiesUrlProvider.class );

    private final String missingFileUrl;

    public SortedPropertiesUrlProvider( String missingFileUrl )
    {
        this.missingFileUrl = missingFileUrl;
    }

    @Override
    public SortedProperties get() throws IOException
    {
        String encoding = "UTF-8";
        SortedProperties result = new SortedProperties( encoding );

        LOGGER.info( "Load missing licenses from URL {}", missingFileUrl );

        String httpRequestResult = UrlRequester.getFromUrl( missingFileUrl );
        byte[] data = httpRequestResult.getBytes( encoding );
        result.load( new ByteArrayInputStream( data ) );

        return result;
    }
}
