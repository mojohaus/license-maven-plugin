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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link LicenseStore}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
class LicenseStoreTest {

    static final List<String> DEFAULT_LICENSES = Arrays.asList(
            "agpl_v3",
            "apache_v2",
            "bsd_2",
            "bsd_3",
            "cddl_v1",
            "epl_v1",
            "epl_v2",
            "epl_only_v1",
            "epl_only_v2",
            "eupl_v1_1",
            "fdl_v1_3",
            "gpl_v1",
            "gpl_v2",
            "gpl_v3",
            "lgpl_v2_1",
            "lgpl_v3",
            "mit");

    static final List<String> NEW_LICENSES = Arrays.asList("license1", "license2", "license3", "license4");

    protected LicenseStore store;

    @BeforeEach
    void setUp() {
        store = null;
    }

    @Test
    void testJarRepository() throws IOException {

        store = new LicenseStore();
        store.init();

        List<LicenseRepository> repositories = store.getRepositories();
        assertNotNull(repositories);
        assertEquals(1, repositories.size());
        LicenseRepository repository = repositories.get(0);

        License[] licenses1 = repository.getLicenses();
        License[] licenses = store.getLicenses();
        assertNotNull(licenses);
        assertNotNull(licenses1);
        assertEquals(DEFAULT_LICENSES.size(), licenses.length);
        assertEquals(DEFAULT_LICENSES.size(), licenses1.length);

        for (String licenseName : DEFAULT_LICENSES) {
            License license = repository.getLicense(licenseName);
            License license1 = store.getLicense(licenseName);
            assertNotNull(license);
            assertNotNull(license1);
            assertEquals(license, license1);
        }

        for (String licenseName : store.getLicenseNames()) {
            assertTrue(DEFAULT_LICENSES.contains(licenseName));
        }
    }

    @Test
    void testUserRepository() throws IOException {

        URL baseURL = getClass().getResource("/newRepository");
        LicenseRepository jarRepository = new LicenseRepository();
        jarRepository.setBaseURL(baseURL);

        store = new LicenseStore();
        store.addRepository(jarRepository);
        store.init();
        List<LicenseRepository> repositories = store.getRepositories();
        assertNotNull(repositories);
        assertEquals(1, repositories.size());
        LicenseRepository repository = repositories.get(0);

        License[] licenses1 = repository.getLicenses();
        License[] licenses = store.getLicenses();
        assertNotNull(licenses);
        assertNotNull(licenses1);
        assertEquals(licenses1.length, 4);
        assertEquals(licenses1.length, licenses.length);

        for (String licenseName : NEW_LICENSES) {
            License license = repository.getLicense(licenseName);
            License license1 = store.getLicense(licenseName);
            assertNotNull(license);
            assertNotNull(license1);
            assertEquals(license, license1);
        }

        for (String licenseName : store.getLicenseNames()) {
            assertTrue(NEW_LICENSES.contains(licenseName));
        }
    }
}
