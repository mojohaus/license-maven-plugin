package org.codehaus.mojo.license.model;

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
    void jarRepository() throws Exception {

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
    void userRepository() throws Exception {

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
        assertEquals(4, licenses1.length);
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
