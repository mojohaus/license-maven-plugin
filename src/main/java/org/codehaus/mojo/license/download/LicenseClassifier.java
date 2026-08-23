package org.codehaus.mojo.license.download;

/*
 * #%L
 * License Maven Plugin
 * %%
 * Copyright (C) 2026 Codehaus
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sorts license names into the categories a reader of a license report cares about: the ones that were signed off,
 * the ones that need a second look, the ones that must not appear at all, and everything nobody has classified yet.
 *
 * <p>Names are matched ignoring case and surrounding whitespace. Use {@code licenseMerges} to bring the spelling
 * variants of one license together before they reach this point.
 *
 * @since 2.8.0
 */
public class LicenseClassifier {

    /**
     * How a license compares against the configured lists, from the most to the least worrying. The order of the
     * constants is the order dependencies appear in when ordered by license match.
     */
    public enum LicenseMatch {
        /** The license is listed in {@code forbiddenLicenses}. */
        forbidden,
        /** The license is listed in {@code problematicLicenses}. */
        problematic,
        /** The license is listed in {@code okLicenses}. */
        ok,
        /** The license is in none of the lists, or the dependency declares no license at all. */
        unknown
    }

    private final Set<String> forbidden;
    private final Set<String> problematic;
    private final Set<String> ok;

    public LicenseClassifier(
            Collection<String> forbiddenLicenses,
            Collection<String> problematicLicenses,
            Collection<String> okLicenses) {
        this.forbidden = names(forbiddenLicenses);
        this.problematic = names(problematicLicenses);
        this.ok = names(okLicenses);
    }

    /**
     * Whether any license was classified at all. A classifier without a single configured name leaves every license
     * {@link LicenseMatch#unknown}, so callers can skip the work entirely.
     *
     * @return whether at least one license name is configured
     */
    public boolean isEmpty() {
        return forbidden.isEmpty() && problematic.isEmpty() && ok.isEmpty();
    }

    /**
     * Classifies a single license.
     *
     * @param license the license, may be {@code null}
     * @return the category the license falls into, never {@code null}
     */
    public LicenseMatch classify(ProjectLicense license) {
        final String name = license != null ? license.getName() : null;
        if (name == null) {
            return LicenseMatch.unknown;
        }
        final String key = name.trim();
        if (forbidden.contains(key)) {
            return LicenseMatch.forbidden;
        }
        if (problematic.contains(key)) {
            return LicenseMatch.problematic;
        }
        if (ok.contains(key)) {
            return LicenseMatch.ok;
        }
        return LicenseMatch.unknown;
    }

    /**
     * Classifies a dependency by the most worrying of its licenses, so that a dependency offering both a forbidden
     * and an approved license is not filed away as approved.
     *
     * @param info the dependency
     * @return the category the dependency falls into, never {@code null}
     */
    public LicenseMatch classify(ProjectLicenseInfo info) {
        LicenseMatch worst = LicenseMatch.unknown;
        for (ProjectLicense license : info.getLicenses()) {
            final LicenseMatch match = classify(license);
            if (match.ordinal() < worst.ordinal()) {
                worst = match;
            }
        }
        return worst;
    }

    private static Set<String> names(Collection<String> licenseNames) {
        if (licenseNames == null || licenseNames.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String licenseName : licenseNames) {
            if (licenseName != null && !licenseName.trim().isEmpty()) {
                result.add(licenseName.trim());
            }
        }
        return result;
    }
}
