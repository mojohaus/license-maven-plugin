package org.codehaus.mojo.license.model;

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

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link LicenseRepository}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
class LicenseRepositoryTest {

    protected LicenseRepository repository;

    @BeforeEach
    void setUp() {
        repository = null;
    }

    @Test
    void testJarRepository() throws IOException {

        repository = new LicenseRepository();
        URL baseURL = getClass().getResource(LicenseStore.JAR_LICENSE_REPOSITORY);
        repository.setBaseURL(baseURL);
        repository.load();

        License[] licenses = repository.getLicenses();
        assertNotNull(licenses);
        assertEquals(LicenseStoreTest.DEFAULT_LICENSES.size(), licenses.length);

        for (String licenseName : LicenseStoreTest.DEFAULT_LICENSES) {
            License license = repository.getLicense(licenseName);
            assertNotNull(license);
            assertNotNull(license.getHeaderURL());
            assertNotNull(license.getLicenseURL());
        }

        for (String licenseName : repository.getLicenseNames()) {
            assertTrue(LicenseStoreTest.DEFAULT_LICENSES.contains(licenseName));
        }
    }

    @Test
    void testUserRepository() throws IOException {

        repository = new LicenseRepository();
        URL baseURL = getClass().getResource("/newRepository");
        repository.setBaseURL(baseURL);
        repository.load();

        License[] licenses = repository.getLicenses();
        assertNotNull(licenses);
        assertEquals(LicenseStoreTest.NEW_LICENSES.size(), licenses.length);

        for (String licenseName : LicenseStoreTest.NEW_LICENSES) {
            License license = repository.getLicense(licenseName);
            assertNotNull(license);
            assertNotNull(license.getHeaderURL());
            assertNotNull(license.getLicenseURL());
        }

        for (String licenseName : repository.getLicenseNames()) {
            assertTrue(LicenseStoreTest.NEW_LICENSES.contains(licenseName));
        }
    }
}
