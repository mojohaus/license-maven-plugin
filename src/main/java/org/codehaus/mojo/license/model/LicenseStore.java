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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code LicenseStore} offers {@link License} coming from different {@link
 * LicenseRepository}.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class LicenseStore implements Iterable<LicenseRepository> {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(LicenseStore.class);

    /**
     * class-path directory where is the licenses repository.
     */
    public static final String JAR_LICENSE_REPOSITORY = "/META-INF/licenses";

    /**
     * Classpath protocol prefix for extra license resolver to seek in classpath.
     */
    public static final String CLASSPATH_PROTOCOL = "classpath://";

    /**
     * list of available license repositories.
     */
    protected List<LicenseRepository> repositories;

    /**
     * flag to know if store was init.
     */
    protected boolean init;

    public static LicenseStore createLicenseStore(String... extraResolver) throws MojoExecutionException {
        LicenseStore store;
        try {
            store = new LicenseStore();
            store.addJarRepository();
            if (extraResolver != null) {
                for (String s : extraResolver) {
                    if (StringUtils.isNotEmpty(s)) {
                        LOG.info("adding extra resolver {}", s);
                        store.addRepository(s);
                    }
                }
            }
            store.init();
        } catch (IllegalArgumentException ex) {
            throw new MojoExecutionException("could not obtain the license repository", ex);
        } catch (IOException ex) {
            throw new MojoExecutionException("could not obtain the license repository", ex);
        }
        return store;
    }

    public void init() throws IOException {
        checkNotInit("init");
        try {
            if (repositories == null) {
                // adding the default class-path repository
                addJarRepository();
            }
            for (LicenseRepository r : this) {
                r.load();
            }
        } finally {
            init = true;
        }
    }

    public List<LicenseRepository> getRepositories() {
        return repositories;
    }

    public String[] getLicenseNames() {
        checkInit("getLicenseNames");
        List<String> result = new ArrayList<>();
        for (LicenseRepository repository : this) {
            for (License license : repository) {
                result.add(license.getName());
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public License[] getLicenses() {
        checkInit("getLicenses");
        List<License> result = new ArrayList<>();
        if (repositories != null) {
            for (LicenseRepository repository : this) {
                for (License license : repository) {
                    result.add(license);
                }
            }
        }
        return result.toArray(new License[result.size()]);
    }

    public License getLicense(String licenseName) {
        checkInit("getLicense");
        Iterator<LicenseRepository> itr = iterator();
        License result = null;
        while (itr.hasNext()) {
            LicenseRepository licenseRepository = itr.next();
            License license = licenseRepository.getLicense(licenseName);
            if (license != null) {
                result = license;
                break;
            }
        }
        if (result == null) {
            LOG.debug("could not find license named '{}'", licenseName);
        }
        return result;
    }

    public void addRepository(String extraResolver) throws IOException {

        if (extraResolver.equals(CLASSPATH_PROTOCOL)) {
            addRootPackageClassPathRepository();
        } else if (extraResolver.startsWith(CLASSPATH_PROTOCOL)) {
            extraResolver = extraResolver.substring(CLASSPATH_PROTOCOL.length());
            LOG.debug("Using classpath extraresolver: {}", extraResolver);
            URL baseURL = getClass().getClassLoader().getResource(extraResolver);
            addRepository(baseURL);
        } else {
            URL baseURL = URI.create(extraResolver).toURL();
            addRepository(baseURL);
        }
    }

    public void addRepository(URL baseURL) throws IOException {
        checkNotInit("addRepository");
        LicenseRepository repository = new LicenseRepository();
        repository.setBaseURL(baseURL);
        LOG.debug("Adding a license repository {}", repository);
        addRepository(repository);
    }

    public void addJarRepository() throws IOException {
        checkNotInit("addJarRepository");
        URL baseURL = getClass().getResource(JAR_LICENSE_REPOSITORY);
        LicenseRepository repository = new LicenseRepository();
        repository.setBaseURL(baseURL);
        LOG.debug("Adding a jar license repository {}", repository);
        addRepository(repository);
    }

    public void addRootPackageClassPathRepository() throws IOException {
        checkNotInit("addRootPackageClassPathRepository");
        LOG.debug("Adding a no package class path license repository ");
        addRepository(new RootPackageClassPathLicenseRepository());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<LicenseRepository> iterator() {
        return getRepositories().iterator();
    }

    protected void addRepository(LicenseRepository repository) {
        checkNotInit("addRepository");
        if (repositories == null) {
            repositories = new ArrayList<>();
        }
        LOG.info("Adding a license repository {}", repository.getBaseURL());
        repositories.add(repository);
    }

    protected void checkInit(String operation) {
        if (!init) {
            throw new IllegalStateException("store was not init, operation [" + operation + "] not possible.");
        }
    }

    protected void checkNotInit(String operation) {
        if (init) {
            throw new IllegalStateException("store was init, operation [" + operation + "+] not possible.");
        }
    }
}
