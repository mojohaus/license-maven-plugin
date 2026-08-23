package org.codehaus.mojo.license.extended.spreadsheet;

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

import org.codehaus.mojo.license.download.LicenseClassifier;
import org.codehaus.mojo.license.download.LicenseClassifier.LicenseMatch;
import org.codehaus.mojo.license.download.ProjectLicense;

/**
 * How the Excel and Calc writers should mark up the licenses they write.
 *
 * <p>This lives in {@code src/main/java} rather than next to the writer that reads it, because
 * {@link CalcFileWriter} exists twice: the Java 11 implementation and the stub that replaces it on Java 8.
 *
 * @since 2.8.0
 */
public class SpreadsheetFormatting {

    /** Marks up nothing, the behaviour before the license lists existed. */
    public static final SpreadsheetFormatting NONE =
            new SpreadsheetFormatting(new LicenseClassifier(null, null, null), false, false);

    private final LicenseClassifier classifier;
    private final boolean highlightUnknownLicenses;
    private final boolean matchedLicensesHaveBorder;

    public SpreadsheetFormatting(
            LicenseClassifier classifier, boolean highlightUnknownLicenses, boolean matchedLicensesHaveBorder) {
        this.classifier = classifier;
        this.highlightUnknownLicenses = highlightUnknownLicenses;
        this.matchedLicensesHaveBorder = matchedLicensesHaveBorder;
    }

    /**
     * Whether any license can be highlighted at all, so that a writer can skip creating the styles.
     *
     * @return whether highlighting is configured
     */
    public boolean highlights() {
        return highlightUnknownLicenses || !classifier.isEmpty();
    }

    /**
     * How a license should be highlighted.
     *
     * @param license the license
     * @return the category to highlight it as, or {@code null} to leave it alone
     */
    public LicenseMatch highlight(ProjectLicense license) {
        final LicenseMatch match = classifier.classify(license);
        return match == LicenseMatch.unknown && !highlightUnknownLicenses ? null : match;
    }

    /**
     * Whether a highlighted license is also boxed in. A border tells the categories apart at a glance, at the cost
     * of making one license of a multi-license dependency look like the only one.
     *
     * @return whether to draw a border around a highlighted license
     */
    public boolean hasBorder() {
        return matchedLicensesHaveBorder;
    }
}
