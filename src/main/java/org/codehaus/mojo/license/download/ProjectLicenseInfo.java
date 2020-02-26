package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2010 - 2011 Codehaus
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

import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains the license information for a single project/dependency
 *
 * @author pgier
 * @since 1.0
 */
public class ProjectLicenseInfo
{
    private String groupId;

    private String artifactId;

    private String version;

    private List<ProjectLicense> licenses = new ArrayList<>();

    private List<ProjectLicense> matchLicenses = new ArrayList<>();
    private boolean hasMatchLicenses = false;

    private List<String> downloaderMessages = new ArrayList<>();

    private boolean approved;

    /**
     * Default constructor.
     */
    public ProjectLicenseInfo()
    {

    }

    public ProjectLicenseInfo( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public ProjectLicenseInfo( String groupId, String artifactId, String version, boolean hasMatchLicenses )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hasMatchLicenses = hasMatchLicenses;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List<ProjectLicense> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<ProjectLicense> licenses )
    {
        this.licenses = licenses;
    }

    public void addLicense( ProjectLicense license )
    {
        licenses.add( license );
    }

    public List<ProjectLicense> getMatchLicenses()
    {
        return matchLicenses;
    }

    public void setMatchLicenses( List<ProjectLicense> matchLicenses )
    {
        this.matchLicenses = matchLicenses;
    }

    public void addMatchLicense( ProjectLicense license )
    {
        matchLicenses.add( license );
    }

    public boolean hasMatchLicenses()
    {
        return hasMatchLicenses;
    }

    public void setHasMatchLicenses( boolean hasMatchLicenses )
    {
        this.hasMatchLicenses = hasMatchLicenses;
    }

    /**
     * The unique ID for the project
     *
     * @return String containing "groupId:artifactId"
     */
    public String getId()
    {
        return groupId + ":" + artifactId;
    }

    public List<String> getDownloaderMessages()
    {
        return downloaderMessages;
    }

    public void addDownloaderMessage( String message )
    {
        downloaderMessages.add( message );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getId();
    }

    public String toGavString()
    {
        return groupId + ":" + artifactId + ( version == null ? "" : ( ":" + version ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object compareTo )
    {
        if ( compareTo instanceof ProjectLicenseInfo )
        {
            ProjectLicenseInfo compare = (ProjectLicenseInfo) compareTo;
            if ( groupId.equals( compare.getGroupId() ) && artifactId.equals( compare.getArtifactId() ) )
            {
                return true;
            }
        }
        if ( compareTo instanceof Artifact )
        {
            Artifact compare = (Artifact) compareTo;
            if ( groupId.equals( compare.getGroupId() ) && artifactId.equals( compare.getArtifactId() ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean deepEquals( ProjectLicenseInfo other )
    {
        return Objects.equals( groupId, other.groupId ) && Objects.equals( artifactId, other.artifactId )
            && Objects.equals( version, other.version ) && Objects.equals( licenses, other.licenses )
            && Objects.equals( matchLicenses, other.matchLicenses )
            && Objects.equals( downloaderMessages, other.downloaderMessages );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return getId().hashCode();
    }


    /**
     * @return a deep clone of {@link #licenses}
     */
    public List<ProjectLicense> cloneLicenses()
    {
        try
        {
            final ArrayList<ProjectLicense> result = new ArrayList<>( licenses != null ? licenses.size() : 0 );
            if ( licenses != null )
            {
                for ( ProjectLicense license : licenses )
                {
                    result.add( license.clone() );
                }
            }
            return result;
        }
        catch ( CloneNotSupportedException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void setApproved( boolean approved )
    {
        this.approved = approved;
    }

    public boolean isApproved()
    {
        return approved;
    }

}
