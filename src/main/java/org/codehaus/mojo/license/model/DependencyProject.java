package org.codehaus.mojo.license.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file 
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations 
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.License;

/**
 * Represents the license information for a given dependency project
 * 
 * @author pgier
 */
public class DependencyProject
{
    private String groupId;

    private String artifactId;

    private String version;

    private List<License> licenses;

    private String licenseResolutionResult;

    public String getLicenseResolutionResult()
    {
        return licenseResolutionResult;
    }

    public void setLicenseResolutionResult( String licenseResolutionResult )
    {
        this.licenseResolutionResult = licenseResolutionResult;
    }

    /**
     * Default constructor
     */
    public DependencyProject()
    {

    }

    public DependencyProject( String groupId, String artifactId, String version )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
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

    public List<License> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<License> licenses )
    {
        this.licenses = licenses;
    }

    public void addLicense( License license )
    {
        if ( licenses == null )
        {
            licenses = new ArrayList<License>();
        }
        licenses.add( license );
    }

    public String toString()
    {
        return getId();
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

    public boolean equals( Object compareTo )
    {
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

    public int hashCode()
    {
        return getId().hashCode();
    }

}
