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
import java.util.Arrays;
import java.util.List;

/**
 * Tests {@link LicenseStore}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public class LicenseStoreTest
{

    public static final List<String> DEFAULT_LICENSES =
        Arrays.asList( "agpl_v3", "apache_v2", "cddl_v1", "fdl_v1_3", "gpl_v1", "gpl_v2", "gpl_v3", "lgpl_v2_1",
                       "lgpl_v3", "mit" );

    protected LicenseStore store;

    @Before
    public void setUp()
    {
        store = null;
    }

    @Test
    public void testJarRepository()
        throws IOException
    {

        store = new LicenseStore();
        store.init();

        List<LicenseRepository> repositories = store.getRepositories();
        Assert.assertNotNull( repositories );
        Assert.assertEquals( 1, repositories.size() );
        LicenseRepository repository = repositories.get( 0 );

        License[] licenses1 = repository.getLicenses();
        License[] licenses = store.getLicenses();
        Assert.assertNotNull( licenses );
        Assert.assertNotNull( licenses1 );
        Assert.assertEquals( DEFAULT_LICENSES.size(), licenses.length );
        Assert.assertEquals( DEFAULT_LICENSES.size(), licenses1.length );

        for ( String licenseName : DEFAULT_LICENSES )
        {
            License license = repository.getLicense( licenseName );
            License license1 = store.getLicense( licenseName );
            Assert.assertNotNull( license );
            Assert.assertNotNull( license1 );
            Assert.assertEquals( license, license1 );
        }

        for ( String licenseName : store.getLicenseNames() )
        {
            Assert.assertTrue( LicenseStoreTest.DEFAULT_LICENSES.contains( licenseName ) );
        }
    }
}
