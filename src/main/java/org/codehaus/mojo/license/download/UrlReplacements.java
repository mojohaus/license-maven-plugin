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

import org.codehaus.mojo.license.spdx.SpdxLicenseList;
import org.codehaus.mojo.license.spdx.SpdxLicenseList.Attachments.UrlReplacement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies manually configured license URL replacements as well as the default ones a necessary.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.20
 */
public class UrlReplacements
{
    private static final Logger LOG = LoggerFactory.getLogger( UrlReplacements.class );

    public static Builder builder()
    {
        return new Builder();
    }

    private final List<UrlReplacement> replacements;

    UrlReplacements( List<UrlReplacement> replacements )
    {
        super();
        this.replacements = replacements;
    }

    public String rewriteIfNecessary( final String originalLicenseUrl )
    {
        String resultUrl = originalLicenseUrl;
        for ( UrlReplacement r : replacements )
        {
            resultUrl = r.getUrlPattern().matcher( resultUrl ).replaceAll( r.getReplacement() );
        }

        if ( LOG.isDebugEnabled() && !resultUrl.equals( originalLicenseUrl ) )
        {
            LOG.debug( "Rewrote URL {} => {}", originalLicenseUrl, resultUrl );
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

        private boolean useDefaults;

        private Set<String> ids = new HashSet<>();

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

                if ( usedDefaults > 0 && LOG.isDebugEnabled() )
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
                    LOG.debug( sb.toString() );
                }
            }
            List<UrlReplacement> rs = Collections.unmodifiableList( replacements );
            replacements = null;
            return new UrlReplacements( rs );
        }
    }
}
