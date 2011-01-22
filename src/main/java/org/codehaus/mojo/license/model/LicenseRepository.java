/*
 * #%L
 * License Maven Plugin
 * 
 * $Id$
 * $HeadURL$
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

package org.codehaus.mojo.license.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.plugin.PluginHelper;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author tchemit <chemit@codelutin.com>
 * @since 1.0.3
 */
public class LicenseRepository
    implements Iterable<License>
{

    /**
     * Logger
     */
    private static final Log log = LogFactory.getLog( LicenseRepository.class );

    public static final String REPOSITORY_DEFINITION_FILE = "licenses.properties";

    /**
     * the base url of the licenses repository
     */
    protected URL baseURL;

    /**
     * licenses of this repository
     */
    protected List<License> licenses;

    /**
     * flag to known if repository was init (pass to {@code true} when invoking
     * the method {@link #load()}).
     */
    protected boolean init;

    public LicenseRepository()
    {
    }

    public URL getBaseURL()
    {
        return baseURL;
    }

    public void setBaseURL( URL baseURL )
    {
        checkNotInit( "setBaseURL" );
        this.baseURL = baseURL;
    }

    public void load()
        throws IOException
    {
        checkNotInit( "load" );
        try
        {
            if ( baseURL == null || baseURL.toString().trim().isEmpty() )
            {
                throw new IllegalStateException( "no baseURL defined in " + this );
            }

            URL definitionURL = PluginHelper.getUrl( getBaseURL(), REPOSITORY_DEFINITION_FILE );
            if ( licenses != null )
            {
                licenses.clear();
            }
            else
            {
                licenses = new ArrayList<License>();
            }

            if ( !checkExists( definitionURL ) )
            {
                throw new IllegalArgumentException(
                    "no licenses.properties found with url [" + definitionURL + "] for resolver " + this );
            }
            Properties p = new Properties();
            p.load( definitionURL.openStream() );

            for ( Entry<Object, Object> entry : p.entrySet() )
            {
                String licenseName = (String) entry.getKey();
                licenseName = licenseName.trim().toLowerCase();
                String licenseDescription = (String) entry.getValue();
                URL licenseURL = PluginHelper.getUrl( baseURL, licenseName );

                License license = new License();
                license.setName( licenseName );
                license.setDescription( licenseDescription );
                license.setBaseURL( licenseURL );
                if ( log.isInfoEnabled() )
                {
                    log.info( "register " + license.getDescription() );
                }
                if ( log.isDebugEnabled() )
                {
                    log.debug( license );
                }
                licenses.add( license );
            }
            licenses = Collections.unmodifiableList( licenses );
        }
        finally
        {
            // mark repository as available
            init = true;
        }
    }

    public String[] getLicenseNames()
    {
        checkInit( "getLicenseNames" );
        List<String> result = new ArrayList<String>( licenses.size() );
        for ( License license : this )
        {
            result.add( license.getName() );
        }
        return result.toArray( new String[result.size()] );
    }

    public License[] getLicenses()
    {
        checkInit( "getLicenses" );
        return licenses.toArray( new License[licenses.size()] );
    }

    public License getLicense( String licenseName )
    {
        checkInit( "getLicense" );
        if ( licenseName == null || licenseName.trim().isEmpty() )
        {
            throw new IllegalArgumentException( "licenceName can not be null, nor empty" );
        }

        License license = null;
        for ( License l : this )
        {
            if ( licenseName.equals( l.getName() ) )
            {
                // got it
                license = l;
                break;
            }
        }
        return license;
    }

    @Override
    public Iterator<License> iterator()
    {
        checkInit( "iterator" );
        return licenses.iterator();
    }

    protected boolean checkExists( URL url )
        throws IOException
    {
        URLConnection openConnection = url.openConnection();
        return openConnection.getContentLength() > 0;
    }

    protected void checkInit( String operation )
        throws IllegalStateException
    {
        if ( !init )
        {
            throw new IllegalStateException(
                "repository " + this + " was not init, operation [" + operation + "] not possible." );
        }
    }

    protected void checkNotInit( String operation )
        throws IllegalStateException
    {
        if ( init )
        {
            throw new IllegalStateException(
                "repository " + this + "was init, operation [" + operation + "+] not possible." );
        }
    }

//    /** next repository (can be {@code null}). */
//    @Deprecated
//    protected LicenseRepository next;
//
//    @Deprecated
//    protected List<LicenseDefinition> definitions;
//
//    @Deprecated
//    protected final Map<LicenseDefinition, License> cache;
//
//    @Deprecated
//    public void reload() throws IOException {
//        init = false;
//        load();
//    }

//    @Deprecated
//    public static URL getUrl(URL baseUrl,
//                             String suffix) throws IllegalArgumentException {
//        return PluginHelper.getUrl(baseUrl, suffix);
//    }
//
//    @Deprecated
//    public LicenseRepository appendRepository(LicenseRepository next) {
//        LicenseRepository lastRepository = getLastRepository();
//        lastRepository.next = next;
//        return next;
//    }
//
//    @Deprecated
//    public List<LicenseDefinition> getAllDefinitions() {
//        LicenseRepository[] repos = getAllRepositories();
//        List<LicenseDefinition> result =
//                new ArrayList<LicenseDefinition>(repos.length);
//        for (LicenseRepository repo : repos) {
//            result.addAll(repo.definitions);
//        }
//        return result;
//    }
//
//    public List<LicenseDefinition> getDefinitions() {
//        return definitions;
//    }
//
//    public LicenseDefinition getDefinition(String licenseName) {
//        checkInit("getDefinition");
//        if (licenseName == null || licenseName.trim().isEmpty()) {
//            throw new IllegalArgumentException(
//                    "licenceName can not be null, nor empty");
//        }
//        licenseName = licenseName.trim().toLowerCase();
//        LicenseDefinition definition = null;
//        for (LicenseDefinition d : definitions) {
//            if (licenseName.equals(d.getName())) {
//                definition = d;
//                break;
//            }
//        }
//        if (definition == null && next != null) {
//            definition = next.getDefinition(licenseName);
//        }
//        return definition;
//    }
//
//    @Deprecated
//    protected LicenseRepository getLastRepository() {
//        LicenseRepository last = next == null ? this : next.getLastRepository();
//        return last;
//    }
//
//    @Deprecated
//    protected LicenseRepository[] getAllRepositories() {
//        List<LicenseRepository> list = new ArrayList<LicenseRepository>();
//        LicenseRepository repo = this;
//        while (repo != null) {
//            list.add(repo);
//            repo = repo.next;
//        }
//        return list.toArray(new LicenseRepository[list.size()]);
//    }
//
//    @Deprecated
//    protected void checkNotInit() throws IllegalStateException {
//        if (init) {
//            throw new IllegalStateException(
//                    "license repository " + this +
//                            " was already initialize...");
//        }
//    }
//
//    @Deprecated
//    protected void checkInit() throws IllegalStateException {
//        if (!init) {
//            throw new IllegalStateException(
//                    "repository " + this + " is not init, use the load " +
//                            "method before any license request");
//        }
//    }
}
