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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.license.spdx.SpdxLicenseList;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.UrlReplacement;

/**
 * Applies manually configured license URL replacements as well as the default ones a necessary.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.20
 */
public class UrlReplacements
{

    public static Builder builder( Log log )
    {
        return new Builder( log );
    }

    private final List<UrlReplacement> replacements;

    private final Log log;

    UrlReplacements( List<UrlReplacement> replacements, Log log )
    {
        super();
        this.replacements = replacements;
        this.log = log;
    }

    public String rewriteIfNecessary( final String originalLicenseUrl )
    {
        String resultUrl = originalLicenseUrl;
        for ( UrlReplacement r : replacements )
        {
            resultUrl = r.getUrlPattern().matcher( resultUrl ).replaceAll( r.getReplacement() );
        }

        if ( log.isDebugEnabled() && !resultUrl.equals( originalLicenseUrl ) )
        {
            log.debug( String.format( "Rewrote URL %s => %s", originalLicenseUrl, resultUrl ) );
        }
        return resultUrl;
    }

    /**
     * A {@link UrlReplacements} builder.
     *
     * @since 1.20
     */
    public static class Builder
    {
        private List<UrlReplacement> replacements = new ArrayList<>();

        private final Log log;

        private boolean useDefaults;

        private Set<String> ids = new HashSet<>();

        public Builder( Log log )
        {
            this.log = log;
        }

        public Builder replacement( String id, String urlPattern, String replacement )
        {

            if ( id != null )
            {
                ids.add( id );
            }
            else
            {
                id = urlPattern + "_" + replacement;
            }
            replacements.add( UrlReplacement.compile( id, urlPattern, replacement ) );
            return this;
        }

        public Builder useDefaults( boolean useDefaults )
        {
            this.useDefaults = useDefaults;
            return this;
        }

        public UrlReplacements build()
        {
            if ( useDefaults )
            {
                final Collection<UrlReplacement> defaults =
                    SpdxLicenseList.getLatest().getAttachments().getUrlReplacements().values();
                int usedDefaults = 0;
                for ( UrlReplacement r : defaults )
                {
                    if ( !ids.contains( r.getId() ) )
                    {
                        replacements.add( r );
                        usedDefaults++;
                    }
                }

                if ( usedDefaults > 0 && log.isDebugEnabled() )
                {
                    final StringBuilder sb = new StringBuilder();
                    sb.append( "Appending " + usedDefaults + " default licenseUrlReplacements:\n" )
                    .append( "<licenseUrlReplacements>\n" );

                    for ( UrlReplacement r : defaults )
                    {
                        if ( !ids.contains( r.getId() ) )
                        {
                            sb
                            .append( "  <licenseUrlReplacement>\n" )
                            .append( "    <id>" ) //
                            .append( r.getId() ) //
                            .append( "</id>\n" ) //
                            .append( "    <regexp>" ) //
                            .append( r.getUrlPattern() ) //
                            .append( "</regexp>\n" ) //
                            .append( "    <replacement>" ) //
                            .append( r.getReplacement() ) //
                            .append( "</replacement>\n" ) //
                            .append( "  <licenseUrlReplacement>\n" );
                        }
                    }
                    sb
                    .append( "</licenseUrlReplacements>\n" );
                    log.debug( sb.toString() );
                }
            }
            List<UrlReplacement> rs = Collections.unmodifiableList( replacements );
            replacements = null;
            return new UrlReplacements( rs, log );
        }
    }
}
