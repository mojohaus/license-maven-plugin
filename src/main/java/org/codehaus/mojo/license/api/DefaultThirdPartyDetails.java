package org.codehaus.mojo.license.api;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2012 CodeLutin, Codehaus, Tony Chemit
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

import org.apache.maven.project.MavenProject;

/**
 * Default implementation of {@link ThirdPartyDetails}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.1
 */
public class DefaultThirdPartyDetails
    implements ThirdPartyDetails
{

    private String[] pomLicenses;

    private String[] thirdPartyLicenses;

    private final MavenProject project;

    public DefaultThirdPartyDetails( MavenProject project )
    {
        this.project = project;
    }

    /**
     * {@inheritDoc}
     */
    public String getGroupId()
    {
        return project.getArtifact().getGroupId();
    }

    /**
     * {@inheritDoc}
     */
    public String getArtifactId()
    {
        return project.getArtifact().getArtifactId();
    }

    /**
     * {@inheritDoc}
     */
    public String getVersion()
    {
        return project.getArtifact().getVersion();
    }

    /**
     * {@inheritDoc}
     */
    public String getType()
    {
        return project.getArtifact().getType();
    }

    /**
     * {@inheritDoc}
     */
    public String getClassifier()
    {
        return project.getArtifact().getClassifier();
    }

    /**
     * {@inheritDoc}
     */
    public String getScope()
    {
        return project.getArtifact().getScope();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPomLicenses()
    {
        return pomLicenses != null && pomLicenses.length > 0;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLicenses()
    {
        String[] result = null;
        if ( hasPomLicenses() )
        {
            result = getPomLicenses();
        }
        else if ( hasThirdPartyLicenses() )
        {
            result = getThirdPartyLicenses();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLicenses()
    {
        return hasPomLicenses() || hasThirdPartyLicenses();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPomLicenses()
    {
        return pomLicenses;
    }

    /**
     * {@inheritDoc}
     */
    public void setPomLicenses( String[] pomLicenses )
    {
        this.pomLicenses = pomLicenses;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getThirdPartyLicenses()
    {
        return thirdPartyLicenses;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasThirdPartyLicenses()
    {
        return thirdPartyLicenses != null && thirdPartyLicenses.length > 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setThirdPartyLicenses( String[] thirdPartyLicenses )
    {
        this.thirdPartyLicenses = thirdPartyLicenses;
    }
}
