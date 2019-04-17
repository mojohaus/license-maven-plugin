package org.codehaus.mojo.license.spdx;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) ${year} Codehaus
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.codehaus.mojo.license.spdx.SpdxLicenseInfo.Attachments.UrlInfo;

/**
 * A license item from <a href="https://raw.githubusercontent.com/spdx/license-list-data/master/json/licenses.json">
 * SPDX licenses.json</a>. {@link #attachments} is an enhancement done by us during processing of the JSON input.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class SpdxLicenseInfo
{
    private final String reference;

    private final boolean isDeprecatedLicenseId;

    private final String detailsUrl;

    private final String name;

    private final String licenseId;

    private final List<String> seeAlso;

    private final boolean isOsiApproved;

    private final boolean isFsfLibre;

    private final Attachments attachments;

    public static Builder builder()
    {
        return new Builder();
    }

    // CHECKSTYLE_OFF: ParameterNumber
    public SpdxLicenseInfo( String reference, boolean isDeprecatedLicenseId, boolean isFsfLibre, String detailsUrl,
                            String name, String licenseId, List<String> seeAlso,
                            boolean isOsiApproved, Attachments attachments )
    {
        super();
        this.reference = reference;
        this.isDeprecatedLicenseId = isDeprecatedLicenseId;
        this.isFsfLibre = isFsfLibre;
        this.detailsUrl = detailsUrl;
        this.name = name;
        this.licenseId = licenseId;
        this.seeAlso = seeAlso;
        this.isOsiApproved = isOsiApproved;
        this.attachments = attachments;
    }

    public boolean isFsfLibre()
    {
        return isFsfLibre;
    }

    public String getReference()
    {
        return reference;
    }

    public boolean isDeprecatedLicenseId()
    {
        return isDeprecatedLicenseId;
    }

    public String getDetailsUrl()
    {
        return detailsUrl;
    }

    public String getName()
    {
        return name;
    }

    public String getLicenseId()
    {
        return licenseId;
    }

    public List<String> getSeeAlso()
    {
        return seeAlso;
    }

    public boolean isOsiApproved()
    {
        return isOsiApproved;
    }

    /**
     * @return the {@link Attachments} - i.e. the data that does not come directly from spdx.org
     */
    public Attachments getAttachments()
    {
        return attachments;
    }

    /**
     * Data not coming directly from
     * <a href="https://raw.githubusercontent.com/spdx/license-list-data/master/json/licenses.json"> SPDX
     * licenses.json</a> but enhanced by us.
     *
     * @since 1.18
     */
    public static class Attachments
    {
        private final Map<String, UrlInfo> urlInfos;

        Attachments( Map<String, UrlInfo> urlInfos )
        {
            super();
            this.urlInfos = urlInfos;
        }

        /**
         * @return a {@link Map} from URLs to {@link UrlInfo}s
         */
        public Map<String, UrlInfo> getUrlInfos()
        {
            return urlInfos;
        }

        /**
         * A sha1 checksum and mime type associated with an URL.
         *
         * @since 1.18
         */
        public static class UrlInfo
        {
            private final String sha1;

            private final String mimeType;

            private final boolean stable;

            private final boolean sanitized;

            public UrlInfo( String sha1, String mimeType, boolean stable, boolean sanitized )
            {
                super();
                this.sha1 = sha1;
                this.mimeType = mimeType;
                this.stable = stable;
                this.sanitized = sanitized;
            }

            /**
             * @return {@code true} is the checksum returned by {@link #getSha1()} is likely to stay stable over time;
             *      {@code false} otherwise. The likeness of staying stable is (1) tested automatically when generatting
             *      {@link SpdxLicenseListData} and (2) for some sites it is set manually based on their historical
             *      behavior.
             */
            public boolean isStable()
            {
                return stable;
            }

            /**
             * @return the checksum computed after applying sanitizers at {@link SpdxLicenseListData} generation time.
             */
            public String getSha1()
            {
                return sha1;
            }

            public String getMimeType()
            {
                return mimeType;
            }

            public boolean isSanitized()
            {
                return sanitized;
            }
        }
    }

    /**
     * A {@link SpdxLicenseInfo} builder.
     *
     * @since 1.18
     */
    public static class Builder
    {

        private Boolean isDeprecatedLicenseId;

        private boolean isFsfLibre;

        private String detailsUrl;

        private String name;

        private String reference;

        private String licenseId;

        private List<String> seeAlso = new ArrayList<>();

        private Boolean isOsiApproved;

        private Map<String, UrlInfo> urlInfos = new LinkedHashMap<>();

        public Builder isDeprecatedLicenseId( boolean isDeprecatedLicenseId )
        {
            this.isDeprecatedLicenseId = isDeprecatedLicenseId;
            return this;
        }

        public Builder isFsfLibre( boolean isFsfLibre )
        {
            this.isFsfLibre = isFsfLibre;
            return this;
        }

        public Builder detailsUrl( String detailsUrl )
        {
            this.detailsUrl = detailsUrl;
            return this;
        }

        public Builder reference( String reference )
        {
            this.reference = reference;
            return this;
        }

        public Builder name( String name )
        {
            this.name = name;
            return this;
        }

        public Builder licenseId( String licenseId )
        {
            this.licenseId = licenseId;
            return this;
        }

        public Builder seeAlso( String seeAlso )
        {
            this.seeAlso.add( seeAlso );
            return this;
        }

        public Builder urlInfo( String url, String sha1, String mimeType, boolean stable, boolean sanitized )
        {
            this.urlInfos.put( url, new UrlInfo( sha1, mimeType, stable, sanitized ) );
            return this;
        }

        public Builder isOsiApproved( boolean isOsiApproved )
        {
            this.isOsiApproved = isOsiApproved;
            return this;
        }

        public SpdxLicenseInfo build()
        {
            Objects.requireNonNull( isDeprecatedLicenseId, "isDeprecatedLicenseId" );
            Objects.requireNonNull( detailsUrl, "detailsUrl" );
            Objects.requireNonNull( reference, "reference" );
            Objects.requireNonNull( name, "name" );
            Objects.requireNonNull( licenseId, "licenseId" );
            Objects.requireNonNull( isOsiApproved, "isOsiApproved" );
            if ( seeAlso.isEmpty() )
            {
                throw new IllegalStateException( "seeAlso cannot be empty" );
            }

            final List<String> sa = Collections.unmodifiableList( seeAlso );
            seeAlso = null;

            final Map<String, UrlInfo> uis = Collections.unmodifiableMap( urlInfos );
            urlInfos = null;

            return new SpdxLicenseInfo( reference, isDeprecatedLicenseId, isFsfLibre, detailsUrl, name,
                                        licenseId, sa, isOsiApproved, new Attachments( uis ) );
        }

    }
}
