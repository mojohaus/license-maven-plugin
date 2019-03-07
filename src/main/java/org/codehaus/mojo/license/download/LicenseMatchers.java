package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2018 Codehaus
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A collection of {@link DependencyMatcher}s to match and replace licenses in {@link ProjectLicenseInfo} instances.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 1.18
 */
public class LicenseMatchers
{

    private static final Pattern MATCH_EMPTY_PATTERN = Pattern.compile( "" );
    private static final Pattern MATCH_ALL_PATTERN = Pattern.compile( ".*" );

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * @param licenseMatchersFile
     * @return new {@link LicenseMatchers} configured from the given {@code licenseMatchersFile}
     * @throws MojoExecutionException
     */
    public static LicenseMatchers load( File licenseMatchersFile )
        throws MojoExecutionException
    {
        final List<DependencyMatcher> matchers = new ArrayList<>();
        try
        {
            if ( licenseMatchersFile != null && licenseMatchersFile.exists() )
            {
                final List<ProjectLicenseInfo> replacements =
                    LicenseSummaryReader.parseLicenseSummary( licenseMatchersFile );

                for ( ProjectLicenseInfo dependency : replacements )
                {
                    matchers.add( DependencyMatcher.of( dependency ) );
                }
            }
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not parse licensesReplacementsFile " + licenseMatchersFile, e );
        }
        return new LicenseMatchers( matchers );
    }

    private static boolean match( Pattern pattern, String string )
    {
        return string == null ? pattern.matcher( "" ).matches() : pattern.matcher( string ).matches();
    }

    private static Pattern pattern( String string, boolean isPre118Match )
    {
        return string == null || string.isEmpty() ? MATCH_EMPTY_PATTERN
                        : isPre118Match ? Pattern.compile( Pattern.quote( string ) )
                                        : Pattern.compile( string, Pattern.CASE_INSENSITIVE );
    }

    private final List<DependencyMatcher> matchers;

    private LicenseMatchers( List<DependencyMatcher> matchers )
    {
        super();
        this.matchers = matchers;
    }

    /**
     * Replace matching licenses in the given {@code dependency}
     *
     * @param dependency
     */
    public void replaceMatches( ProjectLicenseInfo dependency )
    {
        for ( DependencyMatcher matcher : matchers )
        {
            if ( matcher.matches( dependency ) )
            {
                if ( matcher.isApproved() )
                {
                    /* do nothing */
                }
                else
                {
                    dependency.setLicenses( matcher.cloneLicenses() );
                }
                dependency.setApproved( true );
                dependency.getDownloaderMessages().clear();
            }
        }
    }

    /**
     * A {@link LicenseMatchers} builder
     */
    public static class Builder
    {
        private List<DependencyMatcher> matchers = new ArrayList<>();

        public Builder matcher( DependencyMatcher matcher )
        {
            matchers.add( matcher );
            return this;
        }

        public LicenseMatchers build()
        {
            final List<DependencyMatcher> ms = matchers;
            matchers = null;
            return new LicenseMatchers( ms );
        }
    }

    /**
     * A matcher for dependency nodes in a licenses.xml file
     *
     * @since 1.18
     */
    static class DependencyMatcher
    {

        public static DependencyMatcher of( ProjectLicenseInfo dependency )
        {
            final String version = dependency.getVersion();
            final boolean isPre118Match = !dependency.hasMatchLicenses();
            return new DependencyMatcher( pattern( dependency.getGroupId(), isPre118Match ),
                                          pattern( dependency.getArtifactId(), isPre118Match ),
                                          isPre118Match || version == null || version.isEmpty() ? MATCH_ALL_PATTERN
                                                          : Pattern.compile( version, Pattern.CASE_INSENSITIVE ),
                                          LicenseListMatcher.of( dependency ), dependency.cloneLicenses(),
                                          dependency.isApproved() );
        }

        private final Pattern artifactId;

        private final Pattern groupId;

        private final LicenseListMatcher licenseListMatcher;

        private final boolean approved;
        private final List<ProjectLicense> licenses;

        private final Pattern version;

        DependencyMatcher( Pattern groupId, Pattern artifactId, Pattern version, LicenseListMatcher licenseListMatcher,
                           List<ProjectLicense> licenses, boolean approved )
        {
            super();
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.licenseListMatcher = licenseListMatcher;
            this.licenses = licenses;
            this.approved = approved;
        }

        public List<ProjectLicense> getLicenseMatchers()
        {
            return null;
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

        public boolean matches( ProjectLicenseInfo dependency )
        {
            return match( groupId, dependency.getGroupId() ) && match( artifactId, dependency.getArtifactId() )
                && match( version, dependency.getVersion() ) && licenseListMatcher.matches( dependency.getLicenses() );
        }

        public boolean isApproved()
        {
            return approved;
        }

    }

    /**
     * A matcher for lists of {@link ProjectLicense}s.
     *
     * @since 1.18
     */
    static class LicenseListMatcher
    {
        private final List<LicenseMatcher> licenseMatchers;

        private static final LicenseListMatcher MATCHES_ALL_LICENSE_LIST_MATCHER = new LicenseListMatcher( null )
        {
            @Override
            public boolean matches( List<ProjectLicense> licenses )
            {
                return true;
            }
        };

        public LicenseListMatcher( List<LicenseMatcher> licenseMatchers )
        {
            this.licenseMatchers = licenseMatchers;
        }

        public boolean matches( List<ProjectLicense> licenses )
        {
            final int licsSize = licenses == null ? 0 : licenses.size();
            if ( licenseMatchers.size() != licsSize )
            {
                return false;
            }
            final Iterator<LicenseMatcher> matchersIt = licenseMatchers.iterator();
            final Iterator<ProjectLicense> licsIt = licenses.iterator();
            while ( matchersIt.hasNext() )
            {
                if ( !matchersIt.next().matches( licsIt.next() ) )
                {
                    return false;
                }
            }
            return true;
        }

        public static LicenseListMatcher of( ProjectLicenseInfo dependency )
        {
            if ( !dependency.hasMatchLicenses() )
            {
                return MATCHES_ALL_LICENSE_LIST_MATCHER;
            }

            final List<LicenseMatcher> licenseMatchers;
            final List<ProjectLicense> rawMatchers = dependency.getMatchLicenses();
            if ( rawMatchers == null || rawMatchers.isEmpty() )
            {
                licenseMatchers = Collections.emptyList();
            }
            else
            {
                licenseMatchers = new ArrayList<>();
                for ( ProjectLicense lic : rawMatchers )
                {
                    licenseMatchers.add( new LicenseMatcher( lic.getName(), lic.getUrl(), lic.getDistribution(),
                                                             lic.getComments() ) );
                }
            }
            return new LicenseListMatcher( licenseMatchers );
        }
    }

    /**
     * A matcher for single {@link ProjectLicense}s.
     *
     * @since 1.18
     */
    static class LicenseMatcher
    {

        private final Pattern comments;
        private final Pattern distribution;
        private final Pattern name;
        private final Pattern url;

        LicenseMatcher( Pattern name, Pattern url, Pattern distribution, Pattern comments )
        {
            super();
            this.name = name;
            this.url = url;
            this.distribution = distribution;
            this.comments = comments;
        }

        LicenseMatcher( String name, String url, String distribution, String comments )
        {
            super();
            this.name = pattern( name, false );
            this.url = pattern( url, false );
            this.distribution = pattern( distribution, false );
            this.comments = pattern( comments, false );
        }

        public boolean matches( ProjectLicense license )
        {
            return match( name, license.getName() ) && match( url, license.getUrl() )
                && match( distribution, license.getDistribution() ) && match( comments, license.getComments() );
        }

    }

}
