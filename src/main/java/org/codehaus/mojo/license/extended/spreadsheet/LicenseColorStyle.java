package org.codehaus.mojo.license.extended.spreadsheet;

import org.codehaus.mojo.license.AbstractAddThirdPartyMojo;
import org.codehaus.mojo.license.AbstractDownloadLicensesMojo;
import org.codehaus.mojo.license.download.ProjectLicense;

/**
 * In which color, according to a license, the cell is styled.
 */
enum LicenseColorStyle {
    /**
     * Unknown license highlight.
     */
    UNKNOWN,
    /**
     * Forbidden license highlight.
     */
    FORBIDDEN,
    /**
     * Problematic license highlight.
     */
    PROBLEMATIC,
    /**
     * OK license highlight.
     */
    OK,
    /**
     * No highlighting at all.
     */
    NONE;

    static LicenseColorStyle getLicenseColorStyle(
            ProjectLicense license,
            AbstractDownloadLicensesMojo.DataFormatting dataFormatting,
            AbstractAddThirdPartyMojo.ExcludedLicenses excludedLicenses) {
        final LicenseColorStyle licenseColorStyle;
        if (excludedLicenses != null && excludedLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.FORBIDDEN;
        } else if (dataFormatting.problematicLicenses != null
                && dataFormatting.problematicLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.PROBLEMATIC;
        } else if (dataFormatting.okLicenses != null && dataFormatting.okLicenses.contains(license.getName())) {
            licenseColorStyle = LicenseColorStyle.OK;
        } else if (dataFormatting.highlightUnknownLicenses) {
            licenseColorStyle = LicenseColorStyle.UNKNOWN;
        } else {
            licenseColorStyle = LicenseColorStyle.NONE;
        }
        return licenseColorStyle;
    }
}
