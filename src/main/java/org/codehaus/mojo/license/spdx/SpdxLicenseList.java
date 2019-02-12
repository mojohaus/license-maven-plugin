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

    public static Builder builder()
    {
        return new Builder();
    }

    public SpdxLicenseList( String licenseListVersion, Map<String, SpdxLicenseInfo> licenses, String releaseDate )
    {
        super();
        this.licenseListVersion = licenseListVersion;
        this.licenses = licenses;
        this.releaseDate = releaseDate;
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

        public SpdxLicenseList build()
        {
            Objects.requireNonNull( licenseListVersion, "isDeprecatedLicenseId" );
            Objects.requireNonNull( releaseDate, "detailsUrl" );
            if ( licenses.isEmpty() )
            {
                throw new IllegalStateException( "licenses cannot be empty" );
            }
            Map<String, SpdxLicenseInfo> lics = Collections.unmodifiableMap( licenses );
            this.licenses = null;
            return new SpdxLicenseList( licenseListVersion, lics, releaseDate );
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
    }
}
