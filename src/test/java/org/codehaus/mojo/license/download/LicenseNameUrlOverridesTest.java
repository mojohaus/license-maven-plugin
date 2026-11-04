package org.codehaus.mojo.license.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.license.LicenseNameUrlOverride;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LicenseNameUrlOverridesTest {

    private static LicenseNameUrlOverride getSingleLicenseOverride(String licenseNamePattern, String url) {
        LicenseNameUrlOverride o = new LicenseNameUrlOverride();
        o.setLicenseNamePattern(licenseNamePattern);
        o.setUrl(url);
        return o;
    }

    private static LicenseNameUrlOverrides getLicenseNameUrlOverrides(LicenseNameUrlOverride... entries) {
        return new LicenseNameUrlOverrides(Arrays.asList(entries));
    }

    @Test
    void matchingNamePatternReturnsOverrideUrl() {
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(
                getSingleLicenseOverride(".*Apache License, Version 2\\.0.*", "https://internal/apache-2.0.txt"));
        assertEquals(
                "https://internal/apache-2.0.txt",
                subject.overrideIfNecessary(
                        "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    @Test
    void nonMatchingNamePatternReturnsOriginalUrl() {
        LicenseNameUrlOverrides subject =
                getLicenseNameUrlOverrides(getSingleLicenseOverride("MIT License", "https://internal/mit.txt"));
        assertEquals(
                "https://opensource.org/licenses/MIT",
                subject.overrideIfNecessary("Apache License, Version 2.0", "https://opensource.org/licenses/MIT"));
    }

    @Test
    void firstMatchWins() {
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(
                getSingleLicenseOverride("Apache.*", "https://internal/first.txt"),
                getSingleLicenseOverride("Apache.*", "https://internal/second.txt"));
        assertEquals(
                "https://internal/first.txt",
                subject.overrideIfNecessary(
                        "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    @Test
    void caseInsensitivePatternMatchWorksByDefault() {
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(
                getSingleLicenseOverride("apache.*2\\.0", "https://internal/apache-2.0.txt"));
        assertEquals(
                "https://internal/apache-2.0.txt",
                subject.overrideIfNecessary("APACHE LICENSE, VERSION 2.0", "https://old.url/"));
    }

    @Test
    void wholeStringMatchWithWildcardsMatchesDeclaredAsfName() {
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(
                getSingleLicenseOverride(".*Apache.*2\\.0.*", "https://internal/apache-2.0.txt"));
        assertEquals(
                "https://internal/apache-2.0.txt",
                subject.overrideIfNecessary("The Apache Software License, Version 2.0", "https://old.url/"));
    }

    @Test
    void nullLicenseNameReturnsCurrentUrl() {
        LicenseNameUrlOverrides subject =
                getLicenseNameUrlOverrides(getSingleLicenseOverride("Apache.*", "https://internal/apache-2.0.txt"));
        assertEquals("https://old.url/", subject.overrideIfNecessary(null, "https://old.url/"));
    }

    @Test
    void blankLicenseNameReturnsCurrentUrl() {
        LicenseNameUrlOverrides subject =
                getLicenseNameUrlOverrides(getSingleLicenseOverride("Apache.*", "https://internal/apache-2.0.txt"));
        assertEquals("https://old.url/", subject.overrideIfNecessary("", "https://old.url/"));
        assertEquals("https://old.url/", subject.overrideIfNecessary("   ", "https://old.url/"));
    }

    @Test
    void nullCurrentUrlCanBeFilledInByOverride() {
        LicenseNameUrlOverrides subject =
                getLicenseNameUrlOverrides(getSingleLicenseOverride("MIT License", "https://internal/mit.txt"));
        assertEquals("https://internal/mit.txt", subject.overrideIfNecessary("MIT License", null));
    }

    @Test
    void emptyOverrideListReturnsCurrentUrl() {
        LicenseNameUrlOverrides subject = new LicenseNameUrlOverrides(Collections.emptyList());
        assertEquals(
                "https://old.url/", subject.overrideIfNecessary("Apache License, Version 2.0", "https://old.url/"));
    }

    @Test
    void nullOverrideListReturnsCurrentUrl() {
        LicenseNameUrlOverrides subject = new LicenseNameUrlOverrides(null);
        assertEquals(
                "https://old.url/", subject.overrideIfNecessary("Apache License, Version 2.0", "https://old.url/"));
    }

    @Test
    void emptyInstanceConstantBehavesLikeEmptyList() {
        assertEquals(
                "https://old.url/",
                LicenseNameUrlOverrides.EMPTY.overrideIfNecessary("Some License", "https://old.url/"));
    }

    @Test
    void noMatchAndNullCurrentUrlReturnsNull() {
        LicenseNameUrlOverrides subject =
                getLicenseNameUrlOverrides(getSingleLicenseOverride("MIT License", "https://internal/mit.txt"));
        assertNull(subject.overrideIfNecessary("Apache License", null));
    }

    @Test
    void findMatchReturnsMatchingOverride() {
        LicenseNameUrlOverride override = getSingleLicenseOverride("MIT License", "https://internal/mit.txt");
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(override);

        assertSame(override, subject.findMatch("MIT License"));
        assertNull(subject.findMatch("Apache License"));
        assertNull(subject.findMatch(null));
        assertNull(subject.findMatch("   "));
    }

    @Test
    void constructorDefensivelyCopiesList() {
        List<LicenseNameUrlOverride> mutableList = new ArrayList<>();
        mutableList.add(getSingleLicenseOverride("MIT License", "https://internal/mit.txt"));

        LicenseNameUrlOverrides subject = new LicenseNameUrlOverrides(mutableList);
        mutableList.clear();

        assertNotNull(subject.findMatch("MIT License"));
    }

    @Test
    void overrideWithBlankUrlFallsBackToCurrentUrl() {
        LicenseNameUrlOverride blankUrlOverride = getSingleLicenseOverride("MIT License", "   ");
        LicenseNameUrlOverrides subject = getLicenseNameUrlOverrides(blankUrlOverride);

        assertEquals("https://old.url/", subject.overrideIfNecessary("MIT License", "https://old.url/"));
    }

    @Test
    void malformedRegexFailsEagerlyAtConstruction() {
        LicenseNameUrlOverride o = new LicenseNameUrlOverride();
        assertThrows(PatternSyntaxException.class, () -> o.setLicenseNamePattern("[invalid"));
    }

    @Test
    void validationFailsOnBlankPattern() {
        LicenseNameUrlOverride o = new LicenseNameUrlOverride();
        o.setUrl("https://example.com");

        assertThrows(MojoExecutionException.class, o::validate);

        o.setLicenseNamePattern("   ");
        assertThrows(MojoExecutionException.class, o::validate);
    }

    @Test
    void validationFailsOnBlankUrl() {
        LicenseNameUrlOverride o = new LicenseNameUrlOverride();
        o.setLicenseNamePattern(".*MIT.*");

        assertThrows(MojoExecutionException.class, o::validate);

        o.setUrl("   ");
        assertThrows(MojoExecutionException.class, o::validate);
    }

    @Test
    void validationPassesOnValidOverride() throws MojoExecutionException {
        LicenseNameUrlOverride o = new LicenseNameUrlOverride();
        o.setLicenseNamePattern(".*MIT.*");
        o.setUrl("https://example.com/mit.txt");

        o.validate();
        assertNotNull(o.getPattern());
    }
}
