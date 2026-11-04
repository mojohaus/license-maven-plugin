package org.codehaus.mojo.license;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Defines a license name pattern and a URL to use when that name pattern matches.
 * Used by {@link AbstractDownloadLicensesMojo#licenseNameUrlOverrides} to map well-known license
 * names to canonical download URLs, regardless of which dependency declares the license and regardless
 * of whether the dependency's POM already contains a URL.
 *
 * @since 2.7.2
 */
public class LicenseNameUrlOverride {
    /**
     * A Java regular expression matched case-insensitively against the license name as declared in the dependency's
     * POM, i.e. the {@code <name>} element inside {@code <licenses><license>}. This is <b>not</b> the artifact ID.
     * Examples of values that this pattern is matched against: {@code "Apache License, Version 2.0"},
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
     * A {@link Pattern} compiled from {@link #licenseNamePattern}.
     */
    private Pattern pattern;

    public String getLicenseNamePattern() {
        return licenseNamePattern;
    }

    public void setLicenseNamePattern(String licenseNamePattern) {
        this.licenseNamePattern = licenseNamePattern;
        this.pattern =
                (licenseNamePattern != null) ? Pattern.compile(licenseNamePattern, Pattern.CASE_INSENSITIVE) : null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Pattern getPattern() {
        if (pattern == null && licenseNamePattern != null) {
            pattern = Pattern.compile(licenseNamePattern, Pattern.CASE_INSENSITIVE);
        }
        return pattern;
    }

    public void validate() throws MojoExecutionException {
        if (licenseNamePattern == null || licenseNamePattern.trim().isEmpty()) {
            throw new MojoExecutionException(
                    "The <licenseNamePattern> element in <licenseNameUrlOverride> cannot be blank");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new MojoExecutionException("The <url> element in <licenseNameUrlOverride> cannot be blank");
        }
        try {
            this.pattern = Pattern.compile(licenseNamePattern, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new MojoExecutionException(
                    "Invalid regular expression in <licenseNamePattern> '" + licenseNamePattern + "': "
                            + e.getMessage(),
                    e);
        }
    }
}
