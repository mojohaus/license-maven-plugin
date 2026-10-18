package org.codehaus.mojo.license;

import java.util.regex.Pattern;

/**
 * Defines a license name pattern and a URL to use when that name pattern matches.
 * Used by {@link AbstractDownloadLicensesMojo#licenseNameUrlOverridesFile} to map well-known license
 * names to canonical download URLs, regardless of which dependency declares the license and regardless
 * of whether the dependency's POM already contains a URL.
 */
public class LicenseNameUrlOverride {
    /**
     * A Java regular expression matched against the license name as declared in the dependency's POM, i.e. the
     * {@code <name>} element inside {@code <licenses><license>}. This is <b>not</b> the artifact ID. Examples of
     * values that this pattern is matched against: {@code "Apache License, Version 2.0"},
     * {@code "Eclipse Public License 1.0"}, {@code "MIT License"}.
     *
     * <p>When this pattern matches, the {@link #url} is used instead of any URL that was declared in the POM.</p>
     */
    private String licenseNamePattern;

    /**
     * The URL from which the license text should be downloaded when {@link #licenseNamePattern} matches.
     */
    private String url;

    /**
     * A {@link Pattern} eagerly compiled from {@link #licenseNamePattern}.
     */
    private Pattern pattern;

    public String getLicenseNamePattern() {
        return licenseNamePattern;
    }

    public void setLicenseNamePattern(String licenseNamePattern) {
        this.licenseNamePattern = licenseNamePattern;
        this.pattern = (licenseNamePattern != null) ? Pattern.compile(licenseNamePattern) : null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
