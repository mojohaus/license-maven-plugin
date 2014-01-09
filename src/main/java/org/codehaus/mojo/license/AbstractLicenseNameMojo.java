package org.codehaus.mojo.license;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.mojo.license.api.FreeMarkerHelper;
import org.codehaus.mojo.license.model.Copyright;
import org.codehaus.mojo.license.model.License;
import org.codehaus.mojo.license.model.LicenseStore;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract mojo which using a {@link #licenseName} and owns a
 * {@link #licenseStore}.
 *
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0
 */
public abstract class AbstractLicenseNameMojo
    extends AbstractLicenseMojo
{

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------

    /**
     * To specify an external extra licenses repository resolver (says the base
     * url of the repository where the {@code license.properties} is present).
     * <p/>
     * <p>
     * <strong>Note: </strong>If you want to refer to a file within this project, start the expression with <code>${project.baseUri}</code>
     * </p>
     *
     * @since 1.0
     */
    @Parameter( property = "license.licenseResolver" )
    private String licenseResolver;

    /**
     * A flag to keep a backup of every modified file.
     *
     * @since 1.0
     */
    @Parameter( property = "license.keepBackup", defaultValue = "false" )
    private boolean keepBackup;

    /**
     * Name of the license to use in the project.
     *
     * @parameter property="license.licenseName"
     * @since 1.0
     */
    @Parameter( property = "license.licenseName" )
    private String licenseName;

    /**
     * Name of project (or module).
     * <p/>
     * Will be used as description section of new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.projectName", defaultValue = "${project.name}", required = true )
    protected String projectName;

    /**
     * Name of project's organization.
     * <p/>
     * Will be used as copyrigth's holder in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.organizationName", defaultValue = "${project.organization.name}", required = true )
    protected String organizationName;

    /**
     * Inception year of the project.
     * <p/>
     * Will be used as first year of copyright section in new header.
     *
     * @since 1.0
     */
    @Parameter( property = "license.inceptionYear", defaultValue = "${project.inceptionYear}", required = true )
    protected Integer inceptionYear;

    /**
     * optional copyright owners.
     * <p/>
     * If not set, {@code organizationName} parameter will be used instead.
     *
     * @since 1.6
     */
    @Parameter( property = "license.copyrightOwners" )
    protected String copyrightOwners;

    /**
     * optional extra templates parameters.
     * <p/>
     * If filled, they are available with prefix extra_ to process license content
     * (says the header and license content).
     *
     * @since 1.6
     */
    @Parameter
    protected Map<String, String> extraTemplateParameters;

    // ----------------------------------------------------------------------
    // Private Fields
    // ----------------------------------------------------------------------

    /**
     * License loaded from the {@link #licenseName}.
     */
    private License license;

    /**
     * Store of available licenses.
     */
    private LicenseStore licenseStore;

    // ----------------------------------------------------------------------
    // AbstractLicenseMojo Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init()
        throws Exception
    {

        // init licenses store
        licenseStore = LicenseStore.createLicenseStore( getLog(), licenseResolver );

        // check licenseName exists
        license = getLicense( licenseName, true );

        if ( StringUtils.isBlank( copyrightOwners ) )
        {
            copyrightOwners = organizationName;
        }
    }

    // ----------------------------------------------------------------------
    // Protected Methods
    // ----------------------------------------------------------------------

    protected License getLicense( String licenseName, boolean checkIfExists )
    {
        if ( StringUtils.isEmpty( licenseName ) )
        {
            throw new IllegalArgumentException( "licenseName can not be null, nor empty" );
        }
        if ( licenseStore == null )
        {
            throw new IllegalStateException( "No license store initialized!" );
        }
        License result = licenseStore.getLicense( licenseName );
        if ( checkIfExists && result == null )
        {
            throw new IllegalArgumentException( "License named '" + licenseName + "' is unknown, use one of " +
                                                    Arrays.toString( licenseStore.getLicenseNames() ) );
        }
        return result;
    }

    protected boolean isKeepBackup()
    {
        return keepBackup;
    }

    protected String getLicenseName()
    {
        return licenseName;
    }

    protected License getLicense()
    {
        return license;
    }

    protected String getCopyrightOwners()
    {

        String holder = copyrightOwners;

        if ( holder == null )
        {
            holder = organizationName;
        }
        return holder;
    }

    protected String processLicenseContext( String licenseContent )
        throws IOException
    {
        FreeMarkerHelper licenseFreeMarkerHelper = FreeMarkerHelper.newHelperFromContent( licenseContent );

        Map<String, Object> templateParameters = new HashMap<String, Object>();

        addPropertiesToContext( System.getProperties(), "env_", templateParameters );
        addPropertiesToContext( getProject().getProperties(), "project_", templateParameters );

        templateParameters.put( "project", getProject().getModel() );

        templateParameters.put( "organizationName", organizationName );
        templateParameters.put( "inceptionYear", inceptionYear  );
        templateParameters.put( "copyright", getCopyright( getCopyrightOwners() ) );
        templateParameters.put( "projectName", projectName );

        addPropertiesToContext( extraTemplateParameters, "extra_", templateParameters );
        String result = licenseFreeMarkerHelper.renderTemplate( FreeMarkerHelper.TEMPLATE, templateParameters );
        return result;
    }

    protected Copyright getCopyright( String holder )
    {
        return Copyright.newCopyright( inceptionYear, holder );
    }

    // ----------------------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------------------

    private void addPropertiesToContext( Map properties, String prefix, Map<String, Object> context )
    {
        if ( properties != null )
        {
            for ( Object o : properties.keySet() )
            {
                String nextKey = (String) o;
                Object nextValue = properties.get( nextKey );
                context.put( prefix + nextKey, nextValue.toString() );
            }
        }
    }
}
