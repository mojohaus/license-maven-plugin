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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.ContentSanitizer;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.UrlReplacement;

/**
 * A Java representation of
 * <a href="https://raw.githubusercontent.com/spdx/license-list-data/master/json/licenses.json"> SPDX licenses.json</a>
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class SpdxLicenseList
{
    private static volatile SpdxLicenseList latest;

    private static final Object LOCK = new Object();

    public static SpdxLicenseList getLatest()
    {
        if ( latest == null )
        {
            synchronized ( LOCK )
            {
                if ( latest == null )
                {
                    latest = SpdxLicenseListData.createList();
                }
            }
        }
        return latest;
    }

    private final String licenseListVersion;

    private final String releaseDate;

    private final Map<String, SpdxLicenseInfo> licenses;

    private final Attachments attachments;

    public static Builder builder()
    {
        return new Builder();
    }

    SpdxLicenseList( String licenseListVersion, Map<String, SpdxLicenseInfo> licenses, String releaseDate,
                            Attachments attachments )
    {
        super();
        this.licenseListVersion = licenseListVersion;
        this.licenses = licenses;
        this.releaseDate = releaseDate;
        this.attachments = attachments;
    }

    public String getLicenseListVersion()
    {
        return licenseListVersion;
    }

    /**
     * @return an unmodifiable {@link Map} from license IDs to {@link SpdxLicenseInfo}.
     */
    public Map<String, SpdxLicenseInfo> getLicenses()
    {
        return licenses;
    }

    public String getReleaseDate()
    {
        return releaseDate;
    }

    public Attachments getAttachments()
    {
        return attachments;
    }

    /**
     *
     * @since 1.20
     */
    public static class Attachments
    {
        private final Map<String, ContentSanitizer> contentSanitizers;
        private final Map<String, UrlReplacement> urlReplacements;

        Attachments( Map<String, ContentSanitizer> contentSanitizers, Map<String, UrlReplacement> urlReplacements )
        {
            super();
            this.contentSanitizers = contentSanitizers;
            this.urlReplacements = urlReplacements;
        }

        public Map<String, ContentSanitizer> getContentSanitizers()
        {
            return contentSanitizers;
        }

        public Map<String, UrlReplacement> getUrlReplacements()
        {
            return urlReplacements;
        }

        /**
         * @since 1.20
         */
        public static class UrlReplacement
        {
            public static UrlReplacement compile( String id, String urlPattern, String replacement )
            {
                Objects.requireNonNull( id, "id" );
                Objects.requireNonNull( urlPattern, "urlPattern" );
                replacement = replacement == null ? "" : replacement;
                return new UrlReplacement( id, Pattern.compile( urlPattern, Pattern.CASE_INSENSITIVE ), replacement );
            }
            private final String id;
            private final Pattern urlPattern;
            private final String replacement;
            public UrlReplacement( String id, Pattern urlPattern, String replacement )
            {
                super();
                this.id = id;
                this.urlPattern = urlPattern;
                this.replacement = replacement;
            }
            public String getId()
            {
                return id;
            }
            public Pattern getUrlPattern()
            {
                return urlPattern;
            }
            public String getReplacement()
            {
                return replacement;
            }

        }

        /**
         * @since 1.20
         */
        public static class ContentSanitizer
        {
            public static ContentSanitizer compile( String id, String urlPattern, String contentPattern,
                                                    String contentReplacement )
            {
                Objects.requireNonNull( id, "id" );
                Objects.requireNonNull( urlPattern, "urlPattern" );
                Objects.requireNonNull( contentPattern, "contentPattern" );
                contentReplacement = contentReplacement == null ? "" : contentReplacement;
                contentReplacement = StringEscapeUtils.unescapeJava( contentReplacement );
                return new ContentSanitizer( id,
                                             Pattern.compile( urlPattern, Pattern.CASE_INSENSITIVE ),
                                             Pattern.compile( contentPattern,
                                                              Pattern.CASE_INSENSITIVE ),
                                             contentReplacement );
            }

            private final String id;
            private final Pattern urlPattern;
            private final Pattern contentPattern;
            private final String contentReplacement;

            public ContentSanitizer( String id, Pattern urlPattern, Pattern contentPattern, String contentReplacement )
            {
                super();
                Objects.requireNonNull( id, "id" );
                Objects.requireNonNull( urlPattern, "urlPattern" );
                Objects.requireNonNull( contentPattern, "contentPattern" );
                Objects.requireNonNull( contentReplacement, "contentReplacement" );
                this.id = id;
                this.urlPattern = urlPattern;
                this.contentPattern = contentPattern;
                this.contentReplacement = contentReplacement;
            }

            public boolean applies( String url )
            {
                return urlPattern.matcher( url ).matches();
            }

            public String sanitize( String content )
            {
                if ( content == null )
                {
                    return null;
                }
                return contentPattern.matcher( content ).replaceAll( contentReplacement );
            }

            public String getId()
            {
                return id;
            }

            public Pattern getUrlPattern()
            {
                return urlPattern;
            }

            public Pattern getContentPattern()
            {
                return contentPattern;
            }

            public String getContentReplacement()
            {
                return contentReplacement;
            }
        }
    }

    /**
     * A {@link SpdxLicenseList} builder.
     *
     * @since 1.18
     */
    public static class Builder
    {
        private String licenseListVersion;

        private String releaseDate;

        private Map<String, SpdxLicenseInfo> licenses = new LinkedHashMap<>();

        private Map<String, ContentSanitizer> contentSanitizers = new TreeMap<>();
        private Map<String, UrlReplacement> urlReplacements = new TreeMap<>();

        public SpdxLicenseList build()
        {
            Objects.requireNonNull( licenseListVersion, "isDeprecatedLicenseId" );
            Objects.requireNonNull( releaseDate, "detailsUrl" );
            if ( licenses.isEmpty() )
            {
                throw new IllegalStateException( "licenses cannot be empty" );
            }
            final Map<String, SpdxLicenseInfo> lics = Collections.unmodifiableMap( licenses );
            this.licenses = null;

            final Map<String, ContentSanitizer> sanitizers = Collections.unmodifiableMap( contentSanitizers );
            this.contentSanitizers = null;

            final Map<String, UrlReplacement> replacements = Collections.unmodifiableMap( urlReplacements );
            this.urlReplacements = null;

            return new SpdxLicenseList( licenseListVersion, lics, releaseDate,
                                        new Attachments( sanitizers, replacements ) );
        }

        public Builder licenseListVersion( String licenseListVersion )
        {
            this.licenseListVersion = licenseListVersion;
            return this;
        }

        public Builder releaseDate( String releaseDate )
        {
            this.releaseDate = releaseDate;
            return this;
        }

        public Builder license( SpdxLicenseInfo license )
        {
            this.licenses.put( license.getLicenseId(), license );
            return this;
        }

        public Builder contentSanitizer( String id, String urlPattern, String contentPattern,
                                         String contentReplacement )
        {
            this.contentSanitizers.put( id, ContentSanitizer.compile( id, urlPattern, contentPattern,
                                                                      contentReplacement ) );
            return this;
        }

        public Builder urlReplacement( String id, String urlPattern, String replacement )
        {
            this.urlReplacements.put( id, UrlReplacement.compile( id, urlPattern, replacement ) );
            return this;
        }
    }
}
