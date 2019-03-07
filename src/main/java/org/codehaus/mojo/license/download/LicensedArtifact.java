package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 MojoHaus
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
import java.util.List;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.20
 */
public class LicensedArtifact
{

    public static Builder builder( String groupId, String artifactId, String version )
    {
        return new Builder( groupId, artifactId, version );
    }

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final List<License> licenses;

    private final List<String> errorMessages;

    LicensedArtifact( String groupId, String artifactId, String version, List<License> licenses,
                      List<String> errorMessages )
    {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.licenses = licenses;
        this.errorMessages = errorMessages;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = prime * result + ( ( licenses == null ) ? 0 : licenses.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        // CHECKSTYLE_OFF: NeedBraces
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        LicensedArtifact other = (LicensedArtifact) obj;
        if ( artifactId == null )
        {
            if ( other.artifactId != null )
                return false;
        }
        else if ( !artifactId.equals( other.artifactId ) )
            return false;
        if ( groupId == null )
        {
            if ( other.groupId != null )
                return false;
        }
        else if ( !groupId.equals( other.groupId ) )
            return false;
        if ( licenses == null )
        {
            if ( other.licenses != null )
                return false;
        }
        else if ( !licenses.equals( other.licenses ) )
            return false;
        if ( version == null )
        {
            if ( other.version != null )
                return false;
        }
        else if ( !version.equals( other.version ) )
            return false;
        return true;
        // CHECKSTYLE_ON: NeedBraces
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public List<License> getLicenses()
    {
        return licenses;
    }

    public List<String> getErrorMessages()
    {
        return errorMessages;
    }

    /**
     * A {@link LicensedArtifact} builder.
     */
    public static class Builder
    {

        public Builder( String groupId, String artifactId, String version )
        {
            super();
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        private final String groupId;

        private final String artifactId;

        private final String version;

        private List<License> licenses = new ArrayList<>();

        private List<String> errorMessages = new ArrayList<>();

        public Builder errorMessage( String errorMessage )
        {
            this.errorMessages.add( errorMessage );
            return this;
        }

        public Builder license( License license )
        {
            this.licenses.add( license );
            return this;
        }

        public LicensedArtifact build()
        {
            final List<License> lics = Collections.unmodifiableList( licenses );
            licenses = null;
            final List<String> msgs = Collections.unmodifiableList( errorMessages );
            errorMessages = null;
            return new LicensedArtifact( groupId, artifactId, version, lics, msgs );
        }
    }

}
