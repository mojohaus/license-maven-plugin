package org.codehaus.mojo.license.extended;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2019 Jan-Hendrik Diederich
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
import org.apache.maven.model.Developer;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which contains extended licensing information which was found in other files in the JAR,
 * not only Mavens pom.xml's.
 */
public class ExtendedInfo
{
    private String name;
    private Artifact artifact;
    private List<InfoFile> infoFiles = new ArrayList<>();
    private String implementationVendor;
    private String bundleVendor;
    private String bundleLicense;

    private String inceptionYear;
    private Organization organization;
    private List<Developer> developers;
    private Scm scm;
    private String url;

    public Artifact getArtifact()
    {
        return artifact;
    }

    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public List<InfoFile> getInfoFiles()
    {
        return infoFiles;
    }

    public void setInfoFiles( List<InfoFile> infoFiles )
    {
        this.infoFiles = infoFiles;
    }

    public String getImplementationVendor()
    {
        return implementationVendor;
    }

    public void setImplementationVendor( String implementationVendor )
    {
        this.implementationVendor = implementationVendor;
    }

    public String getBundleVendor()
    {
        return bundleVendor;
    }

    public void setBundleVendor( String bundleVendor )
    {
        this.bundleVendor = bundleVendor;
    }

    public String getBundleLicense()
    {
        return bundleLicense;
    }

    public void setBundleLicense( String bundleLicense )
    {
        this.bundleLicense = bundleLicense;
    }

    public String getInceptionYear()
    {
        return inceptionYear;
    }

    public void setInceptionYear( String inceptionYear )
    {
        this.inceptionYear = inceptionYear;
    }

    public Organization getOrganization()
    {
        return organization;
    }

    public void setOrganization( Organization organization )
    {
        this.organization = organization;
    }

    public List<Developer> getDevelopers()
    {
        return developers;
    }

    public void setDevelopers( List<Developer> developers )
    {
        this.developers = developers;
    }

    public Scm getScm()
    {
        return scm;
    }

    public void setScm( Scm scm )
    {
        this.scm = scm;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }
}