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

import java.util.List;

import org.codehaus.mojo.license.download.LicenseMatchers.DependencyMatcher;
import org.codehaus.mojo.license.download.LicenseMatchers.LicenseMatcher;
import org.junit.Assert;
import org.junit.Test;

public class LicenseMatchersTest
{
    @Test
    public void licenseMatches()
    {
        final LicenseMatcher lm1 = new LicenseMatcher( "my license", "http://some.com", null, "important comment" );
        final ProjectLicense lic =
            new ProjectLicense( "my license", "http://some.com", null, "important comment", null );

        Assert.assertTrue( lm1.matches( lic ) );
        lic.setName( "other license" );
        Assert.assertFalse( lm1.matches( lic ) );

        final LicenseMatcher lm2 = new LicenseMatcher( "other.*", "http://some.com", null, "important comment" );
        Assert.assertTrue( lm2.matches( lic ) );
        lic.setUrl( "http://other.org" );
        Assert.assertFalse( lm2.matches( lic ) );

        final LicenseMatcher lm3 = new LicenseMatcher( "other.*", "http://other\\..*", null, "important comment" );
        Assert.assertTrue( lm3.matches( lic ) );
        lic.setComments( "other comment" );
        Assert.assertFalse( lm3.matches( lic ) );

        final LicenseMatcher lm4 = new LicenseMatcher( "other.*", "http://other\\..*", null, ".*comment" );
        Assert.assertTrue( lm4.matches( lic ) );


        lic.setComments( null );
        lic.setDistribution( null );
        lic.setFile( null );
        lic.setName( null );
        lic.setUrl( null );
        final LicenseMatcher lm5 = new LicenseMatcher( (String) null, (String) null, (String) null, (String) null );
        Assert.assertTrue( lm5.matches( lic ) );

        lic.setComments( "" );
        lic.setDistribution( "" );
        lic.setFile( "" );
        lic.setName( "" );
        lic.setUrl( "" );
        Assert.assertTrue( lm5.matches( lic ) );

    }

    @Test
    public void replaceMatchesLegacy()
    {
        final ProjectLicenseInfo dep = new ProjectLicenseInfo( "myGroup", "myArtifact", "1a2.3" );
        final ProjectLicenseInfo pli1 = new ProjectLicenseInfo( "myGroup", "myArtifact", "1.2.3", false );
        final ProjectLicense lic2 = new ProjectLicense("lic2", "http://other.org", null, "other comment", null);
        pli1.addLicense( lic2 );
        final DependencyMatcher m1 = DependencyMatcher.of( pli1 );
        Assert.assertTrue( m1.matches( dep ) ); // legacy mode disregards the version
        dep.setVersion( "1.2.3" );
        Assert.assertTrue( m1.matches( dep ) );

        dep.addLicense( new ProjectLicense( "lic1", "http://some.org", null, "comment", null ) );
        Assert.assertTrue( m1.matches( dep ) );
        final LicenseMatchers matchers1 = LicenseMatchers.builder().matcher( m1 ).build();
        matchers1.replaceMatches( dep );
        Assert.assertEquals( 1, dep.getLicenses().size() );
        Assert.assertEquals( lic2, dep.getLicenses().get( 0 ) );
        Assert.assertNotSame( lic2, dep.getLicenses().get( 0 ) );
    }

    @Test
    public void replaceMatches()
    {

        final ProjectLicenseInfo dep = new ProjectLicenseInfo( "myGroup", "myArtifact", "1.2.3" );
        final DependencyMatcher m0 =
                        DependencyMatcher.of( new ProjectLicenseInfo( "myGroup", "myArtifact", null, true ) );
                    Assert.assertTrue( m0.matches( dep ) );

        final DependencyMatcher m1 =
            DependencyMatcher.of( new ProjectLicenseInfo( "myGroup", "myArtifact", "1\\.2\\.3", true ) );
        Assert.assertTrue( m1.matches( dep ) );
        dep.setGroupId( "otherGroup" );
        Assert.assertFalse( m1.matches( dep ) );

        final DependencyMatcher m2 =
            DependencyMatcher.of( new ProjectLicenseInfo( "other.*", "myArtifact", "1\\.2\\.3", true ) );
        Assert.assertTrue( m2.matches( dep ) );
        dep.setArtifactId( "otherArtifact" );
        Assert.assertFalse( m2.matches( dep ) );

        final DependencyMatcher m3 =
            DependencyMatcher.of( new ProjectLicenseInfo( "other.*", ".*Artifact", "1\\.2\\.3", true ) );
        Assert.assertTrue( m3.matches( dep ) );
        dep.setVersion( "2.2.2" );
        Assert.assertFalse( m3.matches( dep ) );

        final DependencyMatcher m4 =
            DependencyMatcher.of( new ProjectLicenseInfo( "other.*", ".*Artifact", "2\\.2\\..*", true ) );
        Assert.assertTrue( m4.matches( dep ) );

        final LicenseMatchers matchers1 = LicenseMatchers.builder().matcher( m1 ).build();

        final List<ProjectLicense> oldLics = dep.cloneLicenses();
        matchers1.replaceMatches( dep );
        Assert.assertEquals( oldLics, dep.getLicenses() );

        dep.addLicense( new ProjectLicense( "lic1", "http://some.org", null, "comment", null ) );
        Assert.assertFalse( m1.matches( dep ) );

        final ProjectLicenseInfo dep11 = new ProjectLicenseInfo( "myGroup", "myArtifact", "1.2.3" );
        dep11.addLicense( new ProjectLicense( "lic1", "http://some.org", null, "comment", null ) );
        final List<ProjectLicense> oldLics11 = dep.cloneLicenses();
        final ProjectLicenseInfo pli11 = new ProjectLicenseInfo( "myGroup", "myArtifact", "1\\.2\\.3", true );
        pli11.addMatchLicense( new ProjectLicense( "lic1", "http://some\\.org", null, "comment", null ) );
        pli11.addLicense( new ProjectLicense( "lic2", "http://other.org", null, "other comment", null ) );
        final DependencyMatcher m11 = DependencyMatcher.of( pli11 );
        Assert.assertTrue( m11.matches( dep11 ) );

        Assert.assertEquals( oldLics11, dep11.getLicenses() );

        final LicenseMatchers matchers11 = LicenseMatchers.builder().matcher( m11 ).build();
        matchers11.replaceMatches( dep11 );

        Assert.assertNotEquals( oldLics11, dep11.getLicenses() );

        final List<ProjectLicense> newLics = dep11.getLicenses();
        Assert.assertEquals( 1, newLics.size() );
        Assert.assertEquals( 1, newLics.size() );

    }

}
