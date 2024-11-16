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
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.codehaus.mojo.license.utils.MojoHelper;

/**
 * The model of a license.
 *
 * @author tchemit dev@tchemit.fr
 * @since 1.0
 */
public class License {

    public static final String LICENSE_HEADER_FILE = "header.txt";

    public static final String LICENSE_CONTENT_FILE = "license.txt";

    public static final String TEMPLATE_SUFFIX = ".ftl";

    /**
     * base url of license (directory where to find license files).
     */
    protected URL baseURL;

    /**
     * the name of the licenses (ex lgpl-3.0).
     */
    protected String name;

    /**
     * the description of the license.
     */
    protected String description;

    /**
     * url of the license's content.
     */
    protected URL licenseURL;

    /**
     * url of the license header's content.
     */
    protected URL headerURL;

    public License() {}

    public String getName() {
        return name;
    }

    public URL getLicenseURL() {
        if (licenseURL == null) {
            licenseURL = MojoHelper.getUrl(getBaseURL(), LICENSE_CONTENT_FILE);
        }
        return licenseURL;
    }

    public URL getHeaderURL() {
        if (headerURL == null) {
            headerURL = MojoHelper.getUrl(getBaseURL(), LICENSE_HEADER_FILE);
        }
        return headerURL;
    }

    public String getDescription() {
        return description;
    }

    public URL getBaseURL() {
        return baseURL;
    }

    public boolean isHeaderContentTemplateAware() {
        return getHeaderURL().toString().endsWith(TEMPLATE_SUFFIX);
    }

    public boolean isLicenseContentTemplateAware() {
        return getLicenseURL().toString().endsWith(TEMPLATE_SUFFIX);
    }

    public String getLicenseContent(String encoding) throws IOException {
        if (baseURL == null) {
            throw new IllegalStateException("no baseURL defined, can not obtain license content in " + this);
        }

        try (InputStream in = getLicenseURL().openStream()) {
            return IOUtils.toString(in, encoding);
        }
    }

    public String getHeaderContent(String encoding) throws IOException {
        if (baseURL == null) {
            throw new IllegalStateException("no baseURL defined, can not obtain header content in " + this);
        }

        try (InputStream in = getHeaderURL().openStream()) {
            return IOUtils.toString(in, encoding);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBaseURL(URL baseURL) {
        this.baseURL = baseURL;
    }

    public void setLicenseURL(URL licenseURL) {
        this.licenseURL = licenseURL;
    }

    public void setHeaderURL(URL headerURL) {
        this.headerURL = headerURL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        builder.append("name", name);
        builder.append("description", description);
        builder.append("licenseURL", licenseURL);
        builder.append("headerURL", headerURL);
        return builder.toString();
    }
}
