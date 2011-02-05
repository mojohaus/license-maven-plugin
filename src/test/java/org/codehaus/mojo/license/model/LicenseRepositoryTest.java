/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

package org.codehaus.mojo.license.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * Tests {@link LicenseRepository}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class LicenseRepositoryTest
{

    protected LicenseRepository repository;

    @Before
    public void setUp()
    {
        repository = null;
    }

    @Test
    public void testJarRepository()
        throws IOException
    {

        repository = new LicenseRepository();
        URL baseURL = getClass().getResource( LicenseStore.JAR_LICENSE_REPOSITORY );
        repository.setBaseURL( baseURL );
        repository.load();

        License[] licenses = repository.getLicenses();
        Assert.assertNotNull( licenses );
        Assert.assertEquals( LicenseStoreTest.DEFAULT_LICENSES.size(), licenses.length );

        for ( String licenseName : LicenseStoreTest.DEFAULT_LICENSES )
        {
            License license = repository.getLicense( licenseName );
            Assert.assertNotNull( license );
        }

        for ( String licenseName : repository.getLicenseNames() )
        {
            Assert.assertTrue( LicenseStoreTest.DEFAULT_LICENSES.contains( licenseName ) );
        }
    }
}
