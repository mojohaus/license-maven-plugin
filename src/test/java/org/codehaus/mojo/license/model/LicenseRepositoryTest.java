package org.codehaus.mojo.license.model;

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
    void jarRepository() throws Exception {

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
    void userRepository() throws Exception {

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
