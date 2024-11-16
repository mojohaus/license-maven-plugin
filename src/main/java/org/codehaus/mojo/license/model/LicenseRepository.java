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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.mojo.license.utils.MojoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class LicenseRepository implements Iterable<License> {
    private static final Logger LOG = LoggerFactory.getLogger(LicenseRepository.class);

    public static final String REPOSITORY_DEFINITION_FILE = "licenses.properties";

    public static final Pattern LICENSE_DESCRIPTION_PATTERN =
            Pattern.compile("(.*)\\s*~~\\s*license\\s*:\\s*(.*)\\s*~~\\s*header\\s*:\\s*(.*)\\s*");

    /**
     * the base url of the licenses repository.
     */
    protected URL baseURL;

    /**
     * licenses of this repository.
     */
    protected List<License> licenses;

    /**
     * flag to known if repository was init (pass to {@code true} when invoking
     * the method {@link #load()}).
     */
    protected boolean init;

    public URL getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(URL baseURL) {
        checkNotInit("setBaseURL");
        this.baseURL = baseURL;
    }

    protected URL getDefinitionURL() {
        if (baseURL == null || StringUtils.isEmpty(baseURL.toString())) {
            throw new IllegalStateException("no baseURL defined in " + this);
        }

        URL definitionURL = MojoHelper.getUrl(getBaseURL(), REPOSITORY_DEFINITION_FILE);
        return definitionURL;
    }

    protected URL getLicenseBaseURL(String licenseName) {
        URL licenseBaseURL = MojoHelper.getUrl(baseURL, licenseName);
        return licenseBaseURL;
    }

    public void load() throws IOException {
        checkNotInit("load");
        try {

            URL definitionURL = getDefinitionURL();
            if (licenses != null) {
                licenses.clear();
            } else {
                licenses = new ArrayList<>();
            }

            if (!checkExists(definitionURL)) {
                throw new IllegalArgumentException(
                        "no licenses.properties found with url [" + definitionURL + "] for resolver " + this);
            }
            Properties p = new Properties();
            p.load(definitionURL.openStream());

            for (Entry<Object, Object> entry : p.entrySet()) {
                String licenseName = (String) entry.getKey();
                licenseName = licenseName.trim().toLowerCase();
                URL licenseBaseURL = getLicenseBaseURL(licenseName);

                License license = new License();
                license.setName(licenseName);
                license.setBaseURL(licenseBaseURL);

                String licenseDescription = (String) entry.getValue();
                Matcher matcher = LICENSE_DESCRIPTION_PATTERN.matcher(licenseDescription);
                String licenseFile;
                String headerFile;

                if (matcher.matches()) {
                    licenseDescription = matcher.group(1).trim();
                    licenseFile = matcher.group(2).trim();
                    headerFile = matcher.group(3).trim();
                } else {
                    licenseFile = License.LICENSE_CONTENT_FILE;
                    headerFile = License.LICENSE_HEADER_FILE;
                }

                URL licenseFileURL = getFileURL(license, licenseFile);
                license.setLicenseURL(licenseFileURL);
                URL headerFileURL = getFileURL(license, headerFile);
                license.setHeaderURL(headerFileURL);

                license.setDescription(licenseDescription);

                LOG.debug("register {}", license.getDescription());
                LOG.debug("{}", license);
                licenses.add(license);
            }
            licenses = Collections.unmodifiableList(licenses);
        } finally {
            // mark repository as available
            init = true;
        }
    }

    public String[] getLicenseNames() {
        checkInit("getLicenseNames");
        List<String> result = new ArrayList<>(licenses.size());
        for (License license : this) {
            result.add(license.getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public License[] getLicenses() {
        checkInit("getLicenses");
        return licenses.toArray(new License[licenses.size()]);
    }

    public License getLicense(String licenseName) {
        checkInit("getLicense");

        License license = null;
        for (License l : this) {
            if (licenseName.equals(l.getName())) {
                // got it
                license = l;
                break;
            }
        }
        return license;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<License> iterator() {
        checkInit("iterator");
        return licenses.iterator();
    }

    protected boolean checkExists(URL url) throws IOException {
        URLConnection openConnection = url.openConnection();
        return openConnection.getContentLength() > 0;
    }

    protected void checkInit(String operation) {
        if (!init) {
            throw new IllegalStateException(
                    "repository " + this + " was not init, operation [" + operation + "] not possible.");
        }
    }

    protected void checkNotInit(String operation) {
        if (init) {
            throw new IllegalStateException(
                    "repository " + this + "was init, operation [" + operation + "+] not possible.");
        }
    }

    protected URL getFileURL(License license, String filename) throws IOException {

        URL licenseBaseURL = license.getBaseURL();
        URL result = MojoHelper.getUrl(licenseBaseURL, filename);
        if (!checkExists(result)) {
            // let's try with a .ftl suffix
            URL resultWithFtlSuffix = MojoHelper.getUrl(licenseBaseURL, filename + License.TEMPLATE_SUFFIX);

            if (checkExists(resultWithFtlSuffix)) {
                result = resultWithFtlSuffix;
            } else {
                throw new IllegalArgumentException("Could not find license (" + license + ") content file at [" + result
                        + "], nor at [" + resultWithFtlSuffix + "] for resolver " + this);
            }
        }
        return result;
    }
}
