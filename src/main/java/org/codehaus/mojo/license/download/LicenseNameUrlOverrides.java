package org.codehaus.mojo.license.download;

import java.util.Collections;
import java.util.List;

import org.codehaus.mojo.license.LicenseNameUrlOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies license-name-based URL overrides. When a {@link ProjectLicense#getName() license name} matches one of the
 * configured {@link LicenseNameUrlOverride#getLicenseNamePattern() license name patterns}, the URL from the matching override is used
 * instead of the URL originally declared in the dependency's POM. This is useful for redirecting downloads to an
 * internal mirror or for providing a canonical plain-text URL for well-known licenses that are otherwise only
 * available as HTML pages.
 *
 * <p>The overrides are applied in the order they are configured; the first matching override wins.</p>
 */
public class LicenseNameUrlOverrides {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseNameUrlOverrides.class);

    /** An instance with an empty override list, used as a null-safe default. */
    public static final LicenseNameUrlOverrides EMPTY = new LicenseNameUrlOverrides(Collections.emptyList());

    private final List<LicenseNameUrlOverride> overrides;

    public LicenseNameUrlOverrides(List<LicenseNameUrlOverride> overrides) {
        this.overrides = overrides == null ? Collections.emptyList() : Collections.unmodifiableList(overrides);
    }

    /**
     * If {@code licenseName} matches one of the configured name patterns, returns the URL configured for that
     * override; otherwise returns {@code currentUrl} unchanged (which may be {@code null} or blank).
     *
     * @param licenseName the license name as declared in the dependency's POM; may be {@code null} or blank
     * @param currentUrl  the URL currently associated with the license; may be {@code null} or blank
     * @return the override URL when the name matches, or {@code currentUrl} when there is no match
     */
    public String overrideIfNecessary(final String licenseName, final String currentUrl) {
        if (licenseName == null || licenseName.isEmpty()) {
            return currentUrl;
        }
        for (LicenseNameUrlOverride override : overrides) {
            if (override.getPattern() != null
                    && override.getPattern().matcher(licenseName).matches()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Overriding URL for license '{}': {} => {}", licenseName, currentUrl, override.getUrl());
                }
                return override.getUrl();
            }
        }
        return currentUrl;
    }
}
